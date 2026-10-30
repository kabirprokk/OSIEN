package com.example

import com.example.data.repository.LocalBookingRepository
import com.example.data.repository.LocalCrewRepository
import com.example.data.repository.LocalMessageRepository
import com.example.data.repository.LocalNotificationRepository
import com.example.data.repository.LocalUserRepository
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.CrewProfile
import com.example.domain.model.CrewRequirement
import com.example.domain.model.CrewRole
import com.example.domain.model.LocationInfo
import com.example.domain.model.ShootType
import com.example.domain.model.User
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FameBookMarketplaceTest {
  private lateinit var userRepository: LocalUserRepository
  private lateinit var crewRepository: LocalCrewRepository
  private lateinit var bookingRepository: LocalBookingRepository
  private lateinit var messageRepository: LocalMessageRepository

  private val client = User("client", "Client User", "client@example.com", "", UserRole.CLIENT)
  private val crew = User("crew", "Crew User", "crew@example.com", "", UserRole.CREW)

  @Before
  fun setup() {
    userRepository = LocalUserRepository()
    crewRepository = LocalCrewRepository()
    val notifications = LocalNotificationRepository()
    bookingRepository = LocalBookingRepository(crewRepository, notifications)
    messageRepository = LocalMessageRepository()
    runBlocking {
      crewRepository.updateCrewProfile(
        CrewProfile(
          id = "profile-crew",
          userId = crew.id,
          name = crew.name,
          primaryRole = CrewRole.CINEMATOGRAPHER,
          experienceYears = 3,
          gearSummary = "Production kit",
          phone = "",
          email = crew.email
        )
      )
    }
  }

  private fun booking(id: String) = Booking(
    id = id,
    clientId = client.id,
    clientName = client.name,
    clientPhone = client.phone,
    clientEmail = client.email,
    shootType = ShootType.VIDEOGRAPHY,
    title = "Production shoot",
    description = "A production brief",
    date = "25 Oct 2026",
    startTime = "08:00 PM",
    durationHours = 4,
    location = LocationInfo("Studio", "Mumbai"),
    requirements = listOf(CrewRequirement(CrewRole.CINEMATOGRAPHER)),
    status = BookingStatus.SEARCHING_CREW
  )

  @Test
  fun `client booking request creation transitions to searching crew`() = runBlocking {
    val created = bookingRepository.createBooking(booking("TEST-01")).getOrThrow()
    assertEquals(BookingStatus.SEARCHING_CREW, created.status)
    assertTrue(bookingRepository.getIncomingRequestsForCrew(crew.id).first().any { it.id == created.id })
  }

  @Test
  fun `only one crew member can accept a shoot request`() = runBlocking {
    val created = bookingRepository.createBooking(booking("TEST-02")).getOrThrow()
    val accepted = bookingRepository.acceptBooking(created.id, crew.id).getOrThrow()
    assertEquals(BookingStatus.CONFIRMED, accepted.status)
    assertEquals(crew.id, accepted.assignedCrewId)
    assertFalse(bookingRepository.acceptBooking(created.id, "another-crew").isSuccess)
  }

  @Test
  fun `chat messaging sends and retrieves messages`() = runBlocking {
    val result = messageRepository.sendMessage("BK-7892", client.id, client.name, client.role, "Hello")
    assertTrue(result.isSuccess)
    assertEquals("Hello", messageRepository.getMessages("BK-7892").first().last().text)
  }

  @Test
  fun `new accounts default to client and require the registered password`() = runBlocking {
    assertTrue(userRepository.register(client.name, client.email, password = "password123").isSuccess)
    assertTrue(userRepository.login(client.email, "password123").isSuccess)
    assertEquals(UserRole.CLIENT, userRepository.currentUser.value?.role)
    assertFalse(userRepository.login(client.email, "wrong-password").isSuccess)
    userRepository.logout()
    assertEquals(null, userRepository.currentUser.value)
  }
}
