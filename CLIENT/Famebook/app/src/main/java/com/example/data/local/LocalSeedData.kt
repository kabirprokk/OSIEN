package com.example.data.local

import com.example.domain.model.AppNotification
import com.example.domain.model.Booking
import com.example.domain.model.CrewProfile
import com.example.domain.model.Message

/** Empty local defaults. A remote data source can provide users, crew, and bookings later. */
object LocalSeedData {
  val initialUsers = emptyList<com.example.domain.model.User>()
  val initialCrewProfiles = emptyList<CrewProfile>()
  val initialBookings: List<Booking> = emptyList()
  val initialMessages: List<Message> = emptyList()
  val initialNotifications: List<AppNotification> = emptyList()
}
