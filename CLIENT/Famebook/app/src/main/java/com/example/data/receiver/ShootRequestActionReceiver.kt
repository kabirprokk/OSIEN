package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.notification.IncomingRequestNotificationHelper
import com.example.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShootRequestActionReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION_ACCEPT_SHOOT = "com.example.ACTION_ACCEPT_SHOOT"
    const val ACTION_DECLINE_SHOOT = "com.example.ACTION_DECLINE_SHOOT"

    const val EXTRA_BOOKING_ID = "extra_booking_id"
    const val EXTRA_CREW_ID = "extra_crew_id"
    const val EXTRA_SHOOT_TITLE = "extra_shoot_title"
    const val EXTRA_SHOOT_DATE = "extra_shoot_date"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID) ?: return
    val crewId = intent.getStringExtra(EXTRA_CREW_ID) ?: return
    val shootTitle = intent.getStringExtra(EXTRA_SHOOT_TITLE) ?: "Shoot"
    val shootDate = intent.getStringExtra(EXTRA_SHOOT_DATE) ?: ""

    val pendingResult = goAsync()
    val scope = CoroutineScope(Dispatchers.IO)

    scope.launch {
      // Notification taps can arrive with a dead process: re-authenticate
      // from local storage before touching the backend.
      ServiceLocator.init(context.applicationContext)
      runCatching { ServiceLocator.userRepository.restoreSession() }
      try {
        when (action) {
          ACTION_ACCEPT_SHOOT -> {
            val result = ServiceLocator.bookingRepository.acceptBooking(bookingId, crewId)
            if (result.isSuccess) {
              val booking = result.getOrThrow()
              IncomingRequestNotificationHelper.showShootConfirmedNotification(
                context = context,
                bookingId = booking.id,
                shootTitle = booking.title.ifBlank { shootTitle },
                shootDate = "${booking.date} • ${booking.startTime}"
              )
            } else {
              val errorMsg = result.exceptionOrNull()?.message ?: ""
              val claimedBy = if (errorMsg.contains("claimed by", ignoreCase = true)) {
                errorMsg.substringAfter("claimed by ")
              } else {
                "another crew member"
              }
              IncomingRequestNotificationHelper.showAlreadyAssignedNotification(
                context = context,
                bookingId = bookingId,
                assignedTo = claimedBy
              )
            }
          }

          ACTION_DECLINE_SHOOT -> {
            ServiceLocator.bookingRepository.declineBooking(bookingId, crewId)
            IncomingRequestNotificationHelper.cancelIncomingNotification(context, bookingId)
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
