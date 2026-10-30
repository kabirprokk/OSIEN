package com.example.presentation.crew.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.notification.IncomingRequestNotificationHelper
import com.example.data.remote.RealtimeHub
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.IncomingShootRequest
import com.example.domain.model.RequestEvent
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.CrewRepository
import com.example.domain.repository.NotificationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface IncomingRequestUiState {
  object Idle : IncomingRequestUiState

  data class ActiveRequest(
    val request: IncomingShootRequest,
    val isAlreadyAssigned: Boolean = false,
    val assignedToName: String? = null
  ) : IncomingRequestUiState

  data class ShootConfirmed(
    val bookingId: String,
    val shootTitle: String,
    val shootTypeTitle: String,
    val dateAndTime: String,
    val locationName: String
  ) : IncomingRequestUiState
}

class CrewIncomingRequestViewModel(
  private val crewUserId: String,
  private val bookingRepository: BookingRepository,
  private val crewRepository: CrewRepository,
  private val notificationRepository: NotificationRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow<IncomingRequestUiState>(IncomingRequestUiState.Idle)
  val uiState: StateFlow<IncomingRequestUiState> = _uiState.asStateFlow()

  private var isCrewAvailable: Boolean = true

  init {
    observeAvailability()
    observeIncomingRequests()
    observeRequestEvents()
    observeBookingsForAssignmentChanges()
    observeRealtimePush()
  }

  /**
   * Live push hook: the repository flow below only fetches once per
   * collection, so without this the popup would never appear for requests
   * created while the app is already open. Every bookings push re-checks
   * the backend and raises the floating card for the newest open request.
   */
  private fun observeRealtimePush() {
    viewModelScope.launch {
      RealtimeHub.bookingsChanged.collect {
        if (!isCrewAvailable) return@collect
        if (_uiState.value is IncomingRequestUiState.ShootConfirmed) return@collect
        val active = runCatching {
          bookingRepository.getIncomingRequestsForCrew(crewUserId).first()
        }.getOrNull()?.firstOrNull() ?: return@collect
        val current = _uiState.value as? IncomingRequestUiState.ActiveRequest
        if (current == null || current.request.id != active.id) {
          showActiveRequest(active.toIncomingRequest())
        }
      }
    }
  }

  private fun Booking.toIncomingRequest() = IncomingShootRequest(
    id = id,
    shootTypeTitle = shootType.title,
    shootTitle = title,
    dateFormatted = date.uppercase(),
    timeAndDuration = "$startTime • $durationHours HOURS",
    locationName = location.name,
    requiredRole = requirements.firstOrNull()?.role?.title ?: "Lead Cinematographer",
    rawBooking = this
  )

  private fun observeAvailability() {
    viewModelScope.launch {
      crewRepository.getCrewProfileByUserId(crewUserId).collectLatest { profile ->
        val available = profile?.isAvailable ?: true
        isCrewAvailable = available
        if (!available) {
          // Crew is OFFLINE: dismiss any incoming popup immediately
          if (_uiState.value !is IncomingRequestUiState.ShootConfirmed) {
            _uiState.value = IncomingRequestUiState.Idle
          }
        }
      }
    }
  }

  private fun observeIncomingRequests() {
    viewModelScope.launch {
      bookingRepository.getIncomingRequestsForCrew(crewUserId).collectLatest { requests ->
        if (!isCrewAvailable) {
          return@collectLatest
        }

        // If we are already displaying confirmed state, keep it
        if (_uiState.value is IncomingRequestUiState.ShootConfirmed) return@collectLatest

        val activeBooking = requests.firstOrNull()
        if (activeBooking != null) {
          val currentActive = _uiState.value as? IncomingRequestUiState.ActiveRequest
          if (currentActive == null || currentActive.request.id != activeBooking.id) {
            val incoming = IncomingShootRequest(
              id = activeBooking.id,
              shootTypeTitle = activeBooking.shootType.title,
              shootTitle = activeBooking.title,
              dateFormatted = activeBooking.date.uppercase(),
              timeAndDuration = "${activeBooking.startTime} • ${activeBooking.durationHours} HOURS",
              locationName = activeBooking.location.name,
              requiredRole = activeBooking.requirements.firstOrNull()?.role?.title ?: "Lead Cinematographer",
              rawBooking = activeBooking
            )
            showActiveRequest(incoming)
          }
        } else if (_uiState.value is IncomingRequestUiState.ActiveRequest) {
          val current = _uiState.value as IncomingRequestUiState.ActiveRequest
          if (!current.isAlreadyAssigned) {
            // Check if it was claimed by another crew or cancelled
            val bookingInRepo = bookingRepository.allBookings.value.find { it.id == current.request.id }
            if (bookingInRepo?.status == BookingStatus.CONFIRMED && bookingInRepo.assignedCrewId != crewUserId) {
              _uiState.value = current.copy(
                isAlreadyAssigned = true,
                assignedToName = bookingInRepo.assignedCrewName ?: "Another Crew Member"
              )
              scheduleAutoDismiss(3500)
            } else if (bookingInRepo?.status == BookingStatus.CANCELLED) {
              _uiState.value = IncomingRequestUiState.Idle
            }
          }
        }
      }
    }
  }

  private fun observeRequestEvents() {
    viewModelScope.launch {
      notificationRepository.requestEvents.collectLatest { event ->
        if (!isCrewAvailable) return@collectLatest

        when (event) {
          is RequestEvent.NewShootRequest -> {
            if (_uiState.value !is IncomingRequestUiState.ShootConfirmed) {
              showActiveRequest(event.request)
            }
          }

          is RequestEvent.RequestAssigned -> {
            val current = _uiState.value as? IncomingRequestUiState.ActiveRequest
            if (current != null && current.request.id == event.bookingId) {
              if (event.assignedCrewId == crewUserId) {
                _uiState.value = IncomingRequestUiState.ShootConfirmed(
                  bookingId = current.request.id,
                  shootTitle = current.request.shootTitle,
                  shootTypeTitle = current.request.shootTypeTitle,
                  dateAndTime = "${current.request.dateFormatted} • ${current.request.timeAndDuration.substringBefore(" •")}",
                  locationName = current.request.locationName
                )
              } else {
                _uiState.value = current.copy(
                  isAlreadyAssigned = true,
                  assignedToName = event.assignedCrewName
                )
                scheduleAutoDismiss(3500)
              }
            }
          }

          is RequestEvent.RequestCancelled -> {
            val current = _uiState.value as? IncomingRequestUiState.ActiveRequest
            if (current != null && current.request.id == event.bookingId) {
              _uiState.value = IncomingRequestUiState.Idle
            }
          }

          is RequestEvent.RequestDeclined -> {
            if (event.crewId == crewUserId) {
              val current = _uiState.value as? IncomingRequestUiState.ActiveRequest
              if (current != null && current.request.id == event.bookingId) {
                _uiState.value = IncomingRequestUiState.Idle
              }
            }
          }
        }
      }
    }
  }

  private fun observeBookingsForAssignmentChanges() {
    viewModelScope.launch {
      bookingRepository.allBookings.collectLatest { bookings ->
        val current = _uiState.value as? IncomingRequestUiState.ActiveRequest ?: return@collectLatest
        val matched = bookings.find { it.id == current.request.id } ?: return@collectLatest

        if (matched.status == BookingStatus.CONFIRMED) {
          if (matched.assignedCrewId == crewUserId) {
            _uiState.value = IncomingRequestUiState.ShootConfirmed(
              bookingId = matched.id,
              shootTitle = matched.title,
              shootTypeTitle = matched.shootType.title,
              dateAndTime = "${matched.date} • ${matched.startTime}",
              locationName = matched.location.name
            )
          } else {
            // Already claimed by another crew member!
            _uiState.value = current.copy(
              isAlreadyAssigned = true,
              assignedToName = matched.assignedCrewName ?: "Another Crew Member"
            )
            scheduleAutoDismiss(3500)
          }
        } else if (matched.status == BookingStatus.CANCELLED) {
          _uiState.value = IncomingRequestUiState.Idle
        }
      }
    }
  }

  private fun showActiveRequest(request: IncomingShootRequest) {
    if (!isCrewAvailable) return
    // Request stays available with NO countdown. Crew can take their time to accept.
    _uiState.value = IncomingRequestUiState.ActiveRequest(
      request = request,
      isAlreadyAssigned = false,
      assignedToName = null
    )
  }

  fun acceptRequest(bookingId: String) {
    IncomingRequestNotificationHelper.cancelIncomingNotification(bookingId)
    viewModelScope.launch {
      val result = bookingRepository.acceptBooking(bookingId, crewUserId)
      if (result.isSuccess) {
        val booking = result.getOrThrow()
        _uiState.value = IncomingRequestUiState.ShootConfirmed(
          bookingId = booking.id,
          shootTitle = booking.title,
          shootTypeTitle = booking.shootType.title,
          dateAndTime = "${booking.date} • ${booking.startTime}",
          locationName = booking.location.name
        )
      } else {
        val errorMsg = result.exceptionOrNull()?.message ?: ""
        if (errorMsg.contains("claimed", ignoreCase = true) || errorMsg.contains("already", ignoreCase = true)) {
          val current = _uiState.value as? IncomingRequestUiState.ActiveRequest
          if (current != null) {
            _uiState.value = current.copy(isAlreadyAssigned = true)
            scheduleAutoDismiss(3500)
          }
        } else {
          _uiState.value = IncomingRequestUiState.Idle
        }
      }
    }
  }

  fun declineRequest(bookingId: String) {
    IncomingRequestNotificationHelper.cancelIncomingNotification(bookingId)
    viewModelScope.launch {
      _uiState.value = IncomingRequestUiState.Idle
      bookingRepository.declineBooking(bookingId, crewUserId)
    }
  }

  fun dismiss() {
    val current = _uiState.value as? IncomingRequestUiState.ActiveRequest
    if (current != null) {
      IncomingRequestNotificationHelper.cancelIncomingNotification(current.request.id)
    }
    _uiState.value = IncomingRequestUiState.Idle
  }

  private fun scheduleAutoDismiss(millis: Long) {
    viewModelScope.launch {
      delay(millis)
      if (_uiState.value !is IncomingRequestUiState.ShootConfirmed) {
        _uiState.value = IncomingRequestUiState.Idle
      }
    }
  }
}
