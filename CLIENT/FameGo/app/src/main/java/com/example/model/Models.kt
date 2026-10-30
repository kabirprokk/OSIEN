package com.example.model

import java.util.UUID

enum class Role {
  CLIENT,
  CREW,
  ADMIN
}

enum class VerificationStatus {
  VERIFIED,
  PENDING_VERIFICATION,
  SUSPENDED
}

enum class BookingStatus(val label: String) {
  DRAFT("Draft"),
  SEARCHING_CREW("Finding Crew"),
  CREW_RESPONDED("Finding Crew"),
  CONFIRMED("Confirmed"),
  UPCOMING("Upcoming"),
  IN_PROGRESS("In Progress"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled");

  companion object {
    val PENDING = SEARCHING_CREW
  }
}

enum class PaymentStatus { PENDING, PAID, FAILED, REFUNDED }

enum class ShootPlan(
  val title: String,
  val durationLabel: String,
  val durationHours: Int,
  val priceRupees: Int,
  val description: String
) {
  BRONZE_90(
    "Bronze Plan",
    "1 hour + 30 min buffer",
    1,
    1_999,
    "Fast and focused for small shoots."
  ),
  BRONZE_3H(
    "Silver Plan",
    "3 hours + 30 min buffer",
    3,
    2_999,
    "For product shoots, reels and small campaigns."
  ),
  BRONZE_6H(
    "Gold Plan",
    "6 hours + 30 min buffer",
    6,
    8_999,
    "Reliable coverage for larger productions."
  )
}

enum class ShootCategory(
  val title: String,
  val subtitle: String,
  val iconName: String
) {
  VIDEO("Video", "Commercials, reels and digital", "movie"),
  FASHION("Fashion", "Editorial and lookbooks", "style"),
  FOOD("Food", "Restaurants and culinary", "restaurant"),
  PRODUCT("Product", "E-commerce and showcase", "shopping_bag"),
  EVENT("Event", "Shows and live moments", "event"),
  PHOTOGRAPHY("Photography", "Portraits and studio", "camera_alt"),
  CORPORATE("Corporate", "Interviews and brand", "business");

  companion object {
    val CINEMA_VIDEO = VIDEO
    val LIVE_EVENTS = EVENT
    val PHOTO = PHOTOGRAPHY
  }

  val description: String get() = subtitle
}

enum class CrewRoleType(
  val title: String,
  val gearDescription: String
) {
  CINEMATOGRAPHER("Cinematographer", "Sony FX6 / FX3, prime lenses"),
  VIDEOGRAPHER("Videographer", "Sony A7S III, gimbal"),
  PHOTOGRAPHER("Photographer", "Canon R5 / Sony A7R V, lights"),
  DRONE_OPERATOR("Drone Operator", "DJI Mavic 3 Pro Cine"),
  EDITOR("Editor", "MacBook Pro, DaVinci Resolve"),
  ASSISTANT("Assistant", "Grip, boom audio and logistics");

  companion object {
    val SOUND_ENGINEER = ASSISTANT
    val LIGHTING_TECH = CINEMATOGRAPHER
    val PRODUCTION_ASSISTANT = ASSISTANT
  }

  val ratePerHour: Int get() = when (this) {
    CINEMATOGRAPHER -> 2500
    PHOTOGRAPHER -> 1800
    VIDEOGRAPHER -> 2000
    DRONE_OPERATOR -> 3000
    EDITOR -> 1500
    ASSISTANT -> 800
  }

  val recommendedGear: String get() = gearDescription
  val description: String get() = gearDescription
}

typealias ShootLocation = ShootLocationData

data class CrewRequirement(
  val role: CrewRoleType,
  val quantity: Int
) {
  val count: Int get() = quantity
}

data class AssignedCrewMember(
  val crewId: String,
  val name: String,
  val role: CrewRoleType,
  val phone: String,
  val gear: String,
  val rating: Double,
  val isVerified: Boolean = true
) {
  val crewName: String get() = name
  val gearList: List<String> get() = listOf(gear)
}

data class User(
  val id: String,
  val name: String,
  val email: String,
  val phone: String,
  val companyName: String = "",
  val role: Role = Role.CLIENT,
  val avatarInitials: String = "FG",
  /** ISO date of birth (yyyy-MM-dd), required at signup. */
  val dob: String = ""
) {
  val company: String get() = companyName
}

data class CrewProfile(
  val id: String,
  val userId: String,
  val fullName: String,
  val phone: String,
  val email: String,
  val city: String = "Mumbai",
  val primaryRole: CrewRoleType,
  val secondaryRoles: List<CrewRoleType> = emptyList(),
  val experienceYears: Int,
  val bio: String,
  val gearSummary: String,
  val portfolioUrl: String,
  val instagramHandle: String,
  val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
  val isAvailable: Boolean = true,
  val rating: Double = 4.9,
  val totalShootsCompleted: Int = 24,
  val avatarInitials: String = "CR"
)

data class ShootLocationData(
  val venueName: String,
  val address: String
)

data class Booking(
  val id: String = UUID.randomUUID().toString(),
  val bookingCode: String = "FG-" + (1000..9999).random(),
  val shootTitle: String,
  val clientName: String = "",
  val clientCompany: String = "",
  val category: ShootCategory,
  val dateText: String,
  val timeText: String,
  val durationHours: Int,
  val venueName: String,
  val fullAddress: String,
  val locationInstructions: String = "",
  val crewRequirements: List<CrewRequirement>,
  val shootDescription: String,
  val specialInstructions: String = "",
  val brandName: String = "",
  val referenceLink: String = "",
  val plan: ShootPlan = ShootPlan.BRONZE_3H,
  val priceRupees: Int = plan.priceRupees,
  val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
  val paymentReference: String = "",
  val status: BookingStatus = BookingStatus.SEARCHING_CREW,
  val assignedCrew: List<AssignedCrewMember> = emptyList(),
  val createdAtMillis: Long = System.currentTimeMillis()
) {
  val title: String get() = shootTitle
  val date: String get() = dateText
  val time: String get() = timeText
  val brief: String get() = shootDescription
  val estimatedBudget: String get() = priceRupees.toString()
  val location: ShootLocationData get() = ShootLocationData(venueName, fullAddress)
  val requiredCrew: List<CrewRequirement> get() = crewRequirements
}

data class NotificationItem(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val message: String,
  val timestampText: String,
  val targetRole: Role,
  val isRead: Boolean = false,
  val bookingId: String? = null
)

data class ChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val bookingId: String,
  val senderName: String,
  val senderRole: Role,
  val message: String,
  val timeText: String,
  val isFromMe: Boolean,
  val isRead: Boolean = false
)

data class CrewRating(
  val bookingId: String,
  val crewId: String,
  val stars: Int,
  val review: String = ""
)

data class SupportMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val userId: String,
  val message: String,
  val isFromSupport: Boolean = false,
  val createdAt: String = ""
)

data class LiveCrewPoint(
  val latitude: Double,
  val longitude: Double,
  val label: String = ""
)

data class SavedLocation(
  val id: String,
  val label: String,
  val venueName: String,
  val address: String
)

enum class CrewApplicationStatus { UNDER_REVIEW, APPROVED, REJECTED }

data class CrewApplication(
  val id: String,
  val fullName: String,
  val phone: String,
  val email: String,
  val city: String,
  val iphoneModel: String,
  val gearSummary: String,
  val portfolioUrl: String,
  val instagramHandle: String,
  val experienceYears: Int,
  val bestShoot: String,
  val status: CrewApplicationStatus = CrewApplicationStatus.UNDER_REVIEW,
  val createdAt: String = ""
)
