package com.example.domain.model

data class IncomingShootRequest(
  val id: String, // bookingId
  val shootTypeTitle: String,
  val shootTitle: String,
  val dateFormatted: String,
  val timeAndDuration: String,
  val locationName: String,
  val requiredRole: String,
  val rawBooking: Booking
)

sealed interface RequestEvent {
  data class NewShootRequest(val request: IncomingShootRequest) : RequestEvent
  data class RequestAssigned(
    val bookingId: String,
    val assignedCrewId: String,
    val assignedCrewName: String
  ) : RequestEvent
  data class RequestDeclined(val bookingId: String, val crewId: String) : RequestEvent
  data class RequestCancelled(val bookingId: String) : RequestEvent
}
