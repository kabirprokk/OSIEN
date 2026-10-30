package com.example.domain.model

enum class ShootType(
  val title: String,
  val subtitle: String,
  val iconName: String,
  val defaultDurationHours: Int
) {
  PHOTOGRAPHY("Photography", "High-res stills, portraits & lookbooks", "camera", 2),
  VIDEOGRAPHY("Videography", "Cinematic 4K reels, interviews & promos", "videocam", 4),
  EVENT_COVERAGE("Event Coverage", "Conferences, premieres, parties & live ops", "celebration", 6),
  PRODUCT_SHOOT("Product Shoot", "E-commerce, studio table-top & macro lighting", "inventory", 4),
  FASHION_SHOOT("Fashion Shoot", "Editorial, apparel models & runway aesthetics", "styler", 4),
  CORPORATE_SHOOT("Corporate Shoot", "Executive headshots, brand storytelling & offices", "business_center", 3),
  SOCIAL_MEDIA_CONTENT("Social Media Content", "Vertical shorts, viral TikTok/Reels packs", "phone_iphone", 2),
  MUSIC_CREATIVE("Music & Creative", "Music videos, conceptual art & narrative pieces", "music_video", 8),
  OTHER("Custom Production", "Bespoke studio or multi-day setup", "movie", 4)
}

enum class CrewRole(val title: String, val defaultEquipment: String) {
  PHOTOGRAPHER("Photographer", "Sony A7R V / Canon R5 + GM Lenses + Profoto strobe"),
  VIDEOGRAPHER("Videographer", "Sony FX3 / FX6 + Gimbal + Wireless Mics"),
  CINEMATOGRAPHER("Cinematographer", "RED / ARRI Cinema Camera + Anamorphic Glass"),
  DRONE_OPERATOR("Drone Operator", "DJI Inspire 3 / Mavic 3 Pro Cine + ND Filters"),
  EDITOR("On-Site Editor", "M3 Max MacBook Pro + Color Calibrated Monitor"),
  ASSISTANT("Gaffer / Assistant", "Aputure 600d / Nova Panels + C-Stands + Modifiers")
}

data class CrewRequirement(
  val role: CrewRole,
  val count: Int = 1,
  val equipmentNotes: String = ""
)

data class LocationInfo(
  val name: String,
  val address: String,
  val city: String = "Mumbai",
  val notes: String = "",
  val latitude: Double = 19.0760,
  val longitude: Double = 72.8777
)

enum class BookingStatus(val label: String) {
  PENDING("Pending"),
  SEARCHING_CREW("Finding Crew..."),
  OFFERED("Crew Alerted"),
  CONFIRMED("Crew Confirmed"),
  IN_PROGRESS("In Production"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled")
}
