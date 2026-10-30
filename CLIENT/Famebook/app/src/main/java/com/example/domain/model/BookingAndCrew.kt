package com.example.domain.model

data class CrewProfile(
  val id: String,
  val userId: String,
  val name: String,
  val primaryRole: CrewRole,
  val secondaryRoles: List<CrewRole> = emptyList(),
  val skills: List<String> = emptyList(),
  val experienceYears: Int,
  val gearSummary: String,
  val rating: Double = 4.9,
  val totalCompletedShoots: Int = 42,
  val isAvailable: Boolean = true,
  val phone: String,
  val email: String,
  val avatarUrl: String? = null,
  val instagramHandle: String = "@famebros.crew"
)

data class Booking(
  val id: String,
  val clientId: String,
  val clientName: String,
  val clientPhone: String,
  val clientEmail: String,
  val clientCompany: String? = null,
  val shootType: ShootType,
  val title: String,
  val description: String,
  val date: String,
  val startTime: String,
  val durationHours: Int,
  val location: LocationInfo,
  val requirements: List<CrewRequirement>,
  val specialInstructions: String = "",
  val referenceNotes: String = "",
  val status: BookingStatus = BookingStatus.SEARCHING_CREW,
  val assignedCrewId: String? = null,
  val assignedCrewName: String? = null,
  val assignedCrewRole: String? = null,
  val assignedCrewPhone: String? = null,
  val assignedCrewEmail: String? = null,
  val createdAtTimestamp: Long = System.currentTimeMillis()
)

data class Message(
  val id: String,
  val bookingId: String,
  val senderId: String,
  val senderName: String,
  val senderRole: UserRole,
  val text: String,
  val timestamp: Long = System.currentTimeMillis()
)

data class AppNotification(
  val id: String,
  val recipientUserId: String,
  val title: String,
  val message: String,
  val bookingId: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val isRead: Boolean = false
)
