package com.example

import com.example.data.FameGoRepository
import com.example.model.AssignedCrewMember
import com.example.model.Booking
import com.example.model.BookingStatus
import com.example.model.ChatMessage
import com.example.model.CrewRoleType
import com.example.model.Role
import com.example.model.ShootCategory
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClientAftercareTest {

  private fun testBooking(): Booking {
    val booking = Booking(
      id = "aftercare-${UUID.randomUUID()}",
      shootTitle = "Aftercare shoot",
      category = ShootCategory.VIDEO,
      dateText = "Today",
      timeText = "09:00 AM",
      durationHours = 2,
      venueName = "Studio",
      fullAddress = "Bandra West",
      crewRequirements = emptyList(),
      shootDescription = "Brief"
    )
    return FameGoRepository.createBooking(booking)
  }

  private fun testCrew(id: String) = AssignedCrewMember(
    crewId = id,
    name = "Test Crew",
    role = CrewRoleType.CINEMATOGRAPHER,
    phone = "+91 90000 00000",
    gear = "FX6",
    rating = 4.8,
    isVerified = true
  )

  @Test
  fun `rating is stored and stars are clamped`() {
    val booking = testBooking()
    val crewId = "rate-${UUID.randomUUID()}"
    FameGoRepository.submitRating(booking.id, crewId, 9, "Great work")
    assertEquals(5, FameGoRepository.ratingFor(booking.id, crewId)?.stars)
    assertNull(FameGoRepository.ratingFor(booking.id, "missing"))
  }

  @Test
  fun `live sharing does not invent a point before the device reports one`() {
    val booking = testBooking()
    FameGoRepository.setLiveSharing(booking.id, true)
    assertTrue(booking.id in FameGoRepository.liveSharing.value)
    assertNull(FameGoRepository.livePoints.value[booking.id])
    FameGoRepository.setLiveSharing(booking.id, false)
    assertTrue(booking.id !in FameGoRepository.liveSharing.value)
  }

  @Test
  fun `completing a shoot notifies and stops sharing`() {
    val booking = testBooking()
    FameGoRepository.setLiveSharing(booking.id, true)
    val before = FameGoRepository.notifications.value.size
    FameGoRepository.completeShoot(booking.id)
    val updated = FameGoRepository.bookings.value.first { it.id == booking.id }
    assertEquals(BookingStatus.COMPLETED, updated.status)
    assertTrue(booking.id !in FameGoRepository.liveSharing.value)
    assertTrue(FameGoRepository.notifications.value.size > before)
  }

  @Test
  fun `rebooking copies details without crew or payment`() {
    val booking = testBooking()
    val crewId = "rebook-${UUID.randomUUID()}"
    FameGoRepository.acceptShootRequest(booking.id, testCrew(crewId))
    val rebook = FameGoRepository.prepareRebooking(booking.id)
    assertNotNull(rebook)
    assertTrue(rebook!!.id != booking.id)
    assertEquals(booking.shootTitle, rebook.shootTitle)
    assertEquals(booking.plan, rebook.plan)
    assertTrue(rebook.assignedCrew.isEmpty())
    assertEquals(BookingStatus.SEARCHING_CREW, rebook.status)
  }

  @Test
  fun `marking chat read only affects incoming messages`() {
    val booking = testBooking()
    FameGoRepository.sendChatMessage(booking.id, "hello", Role.CLIENT, "Me")
    val mine = FameGoRepository.chatMessages.value[booking.id]!!.last()
    val incoming = mine.copy(id = "in-${UUID.randomUUID()}", isFromMe = false, isRead = false)
    val field = FameGoRepository::class.java.getDeclaredField("_chatMessages")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val state = field.get(FameGoRepository) as kotlinx.coroutines.flow.MutableStateFlow<Map<String, List<ChatMessage>>>
    state.value = state.value + (booking.id to (state.value[booking.id]!! + incoming))
    FameGoRepository.markChatRead(booking.id)
    val after = FameGoRepository.chatMessages.value[booking.id]!!
    assertTrue(after.first { it.id == incoming.id }.isRead)
    assertTrue(after.any { it.isFromMe })
  }
}
