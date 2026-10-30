package com.example.data.repository

import com.example.data.local.LocalSeedData
import com.example.domain.model.AppNotification
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.IncomingShootRequest
import com.example.domain.model.RequestEvent
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.CrewRepository
import com.example.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-ready local implementation of BookingRepository.
 * Manages bookings, request dispatching, state transitions, and
 * atomic single-crew assignment in memory.
 *
 * Designed as a clean repository pattern: in the future,
 * RemoteBookingRepository can replace this class without
 * modifying any ViewModels or UI composables.
 */
class LocalBookingRepository(
  private val crewRepository: CrewRepository,
  private val notificationRepository: NotificationRepository
) : BookingRepository {

  private val _bookings = MutableStateFlow<List<Booking>>(LocalSeedData.initialBookings)
  override val allBookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

  // Concurrency lock ensuring atomic single-crew assignment without race conditions
  private val mutex = Mutex()

  // Track declined bookings per crew member to avoid re-prompting
  private val declinedByCrew = mutableMapOf<String, MutableSet<String>>()

  override fun getBooking(bookingId: String): Flow<Booking?> {
    return _bookings.map { list -> list.find { it.id == bookingId } }
  }

  override fun getClientBookings(clientId: String): Flow<List<Booking>> {
    return _bookings.map { list ->
      list.filter { it.clientId == clientId }
        .sortedByDescending { it.createdAtTimestamp }
    }
  }

  override fun getCrewBookings(crewId: String): Flow<List<Booking>> {
    return _bookings.map { list ->
      list.filter { it.assignedCrewId == crewId }
        .sortedByDescending { it.createdAtTimestamp }
    }
  }

  override fun getActiveRequestForClient(clientId: String): Flow<Booking?> {
    return _bookings.map { list ->
      list.find {
        it.clientId == clientId &&
            (it.status == BookingStatus.SEARCHING_CREW || it.status == BookingStatus.OFFERED)
      }
    }
  }

  override fun getIncomingRequestsForCrew(crewId: String): Flow<List<Booking>> {
    return combine(_bookings, crewRepository.crewProfiles) { list, profiles ->
      val crewProfile = profiles.find { it.id == crewId || it.userId == crewId }
      val isAvailable = crewProfile?.isAvailable == true

      if (!isAvailable) {
        emptyList()
      } else {
        val declinedSet = declinedByCrew[crewId] ?: emptySet()
        list.filter { booking ->
          (booking.status == BookingStatus.SEARCHING_CREW || booking.status == BookingStatus.OFFERED) &&
              !declinedSet.contains(booking.id)
        }
      }
    }
  }

  override suspend fun createBooking(booking: Booking): Result<Booking> = mutex.withLock {
    val newBooking = booking.copy(
      id = if (booking.id.isBlank()) "FB-${UUID.randomUUID().toString().take(6).uppercase()}" else booking.id,
      status = BookingStatus.SEARCHING_CREW,
      createdAtTimestamp = System.currentTimeMillis()
    )
    _bookings.value = listOf(newBooking) + _bookings.value

    // Notify available crew via notification repository
    notificationRepository.sendNotification(
      AppNotification(
        id = UUID.randomUUID().toString(),
        recipientUserId = "crew_broadcast",
        title = "New Shoot Request!",
        message = "${newBooking.shootType.title} in ${newBooking.location.city}",
        bookingId = newBooking.id
      )
    )

    // Emit typed RequestEvent for incoming request display
    val incomingReq = IncomingShootRequest(
      id = newBooking.id,
      shootTypeTitle = newBooking.shootType.title,
      shootTitle = newBooking.title,
      dateFormatted = newBooking.date.uppercase(),
      timeAndDuration = "${newBooking.startTime} • ${newBooking.durationHours} HOURS",
      locationName = newBooking.location.name,
      requiredRole = newBooking.requirements.firstOrNull()?.role?.title ?: "Lead Cinematographer",
      rawBooking = newBooking
    )
    notificationRepository.emitRequestEvent(RequestEvent.NewShootRequest(incomingReq))

    return Result.success(newBooking)
  }

  override suspend fun cancelBooking(bookingId: String): Result<Unit> = mutex.withLock {
    val currentList = _bookings.value
    val target = currentList.find { it.id == bookingId }
      ?: return Result.failure(IllegalArgumentException("Booking not found"))

    _bookings.value = currentList.map {
      if (it.id == bookingId) it.copy(status = BookingStatus.CANCELLED) else it
    }

    notificationRepository.sendNotification(
      AppNotification(
        id = UUID.randomUUID().toString(),
        recipientUserId = target.clientId,
        title = "Booking Cancelled",
        message = "Shoot '${target.title}' has been cancelled.",
        bookingId = bookingId
      )
    )

    notificationRepository.emitRequestEvent(RequestEvent.RequestCancelled(bookingId))
    return Result.success(Unit)
  }

  override suspend fun acceptBooking(bookingId: String, crewId: String): Result<Booking> = mutex.withLock {
    val currentList = _bookings.value
    val target = currentList.find { it.id == bookingId }
      ?: return Result.failure(IllegalArgumentException("Booking request not found."))

    // Atomic race-condition prevention: Ensure request is not already accepted by another crew
    if (target.status == BookingStatus.CONFIRMED || target.assignedCrewId != null) {
      return Result.failure(IllegalStateException("This shoot has already been claimed by another crew member."))
    }
    if (target.status == BookingStatus.CANCELLED) {
      return Result.failure(IllegalStateException("This shoot request was cancelled by the client."))
    }

    val crew = crewRepository.crewProfiles.value.find { it.id == crewId || it.userId == crewId }
      ?: return Result.failure(IllegalArgumentException("Crew profile not found."))

    val updatedBooking = target.copy(
      status = BookingStatus.CONFIRMED,
      assignedCrewId = crew.userId,
      assignedCrewName = crew.name,
      assignedCrewRole = crew.primaryRole.title,
      assignedCrewPhone = crew.phone,
      assignedCrewEmail = crew.email
    )

    _bookings.value = currentList.map {
      if (it.id == bookingId) updatedBooking else it
    }

    // Notify Client
    notificationRepository.sendNotification(
      AppNotification(
        id = UUID.randomUUID().toString(),
        recipientUserId = target.clientId,
        title = "Crew Confirmed!",
        message = "${crew.name} has been assigned to your shoot '${target.title}'.",
        bookingId = bookingId
      )
    )

    // Notify Crew
    notificationRepository.sendNotification(
      AppNotification(
        id = UUID.randomUUID().toString(),
        recipientUserId = crew.userId,
        title = "Shoot Assigned",
        message = "You are confirmed for '${target.title}' on ${target.date}.",
        bookingId = bookingId
      )
    )

    // Emit assigned event so other crew devices/screens clear the request
    notificationRepository.emitRequestEvent(
      RequestEvent.RequestAssigned(
        bookingId = bookingId,
        assignedCrewId = crew.userId,
        assignedCrewName = crew.name
      )
    )

    return Result.success(updatedBooking)
  }

  override suspend fun declineBooking(bookingId: String, crewId: String): Result<Unit> = mutex.withLock {
    val set = declinedByCrew.getOrPut(crewId) { mutableSetOf() }
    set.add(bookingId)
    _bookings.value = _bookings.value.toList()
    notificationRepository.emitRequestEvent(
      RequestEvent.RequestDeclined(
        bookingId = bookingId,
        crewId = crewId
      )
    )
    return Result.success(Unit)
  }

  override suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Result<Unit> = mutex.withLock {
    _bookings.value = _bookings.value.map {
      if (it.id == bookingId) it.copy(status = status) else it
    }
    return Result.success(Unit)
  }
}
