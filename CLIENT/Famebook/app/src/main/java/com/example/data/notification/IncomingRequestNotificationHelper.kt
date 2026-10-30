package com.example.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.receiver.ShootRequestActionReceiver
import com.example.domain.model.IncomingShootRequest

object IncomingRequestNotificationHelper {

  private var appContext: Context? = null

  const val CHANNEL_INCOMING_SHOOTS = "famebook_incoming_shoots"
  const val CHANNEL_CREW_SERVICE = "famebook_crew_service"

  const val NOTIFICATION_ID_DISPATCH_SERVICE = 1001
  const val NOTIFICATION_ID_INCOMING_BASE = 2000

  fun createNotificationChannels(context: Context) {
    appContext = context.applicationContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      // High-Priority Heads-Up Channel for Incoming Shoot Requests
      val incomingChannel = NotificationChannel(
        CHANNEL_INCOMING_SHOOTS,
        "Incoming Shoot Requests",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Urgent alerts when a client requests you for a shoot"
        enableLights(true)
        lightColor = Color.parseColor("#FFB800")
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 350, 150, 350)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      notificationManager.createNotificationChannel(incomingChannel)

      // Low-priority Channel for Foreground Crew Dispatch Service
      val dispatchChannel = NotificationChannel(
        CHANNEL_CREW_SERVICE,
        "Crew Dispatch Status",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Keeps FameBook active to receive real-time shoot requests while on duty"
        setShowBadge(false)
      }
      notificationManager.createNotificationChannel(dispatchChannel)
    }
  }

  fun buildDispatchForegroundNotification(context: Context, crewName: String): Notification {
    createNotificationChannels(context)

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingOpen = PendingIntent.getActivity(
      context,
      100,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(context, CHANNEL_CREW_SERVICE)
      .setSmallIcon(R.drawable.ic_stat_shoot)
      .setContentTitle("FameBook Dispatch • Available")
      .setContentText("$crewName is online and listening for shoot requests")
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(pendingOpen)
      .setColor(Color.parseColor("#FFB800"))
      .build()
  }

  fun showIncomingShootNotification(
    context: Context,
    request: IncomingShootRequest,
    crewUserId: String
  ) {
    createNotificationChannels(context)

    val notificationId = getNotificationId(request.id)

    // Tap to open app directly to shoot
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(ShootRequestActionReceiver.EXTRA_BOOKING_ID, request.id)
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Full-screen takeover: the same rounded Accept / Decline card, launched
    // over any app (or the lock screen) when FameBook itself is closed.
    val popupIntent = Intent(context, com.example.presentation.crew.request.CrewIncomingActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(
        com.example.presentation.crew.request.CrewIncomingActivity.EXTRA_BOOKING_ID,
        request.id
      )
      putExtra(
        com.example.presentation.crew.request.CrewIncomingActivity.EXTRA_CREW_ID,
        crewUserId
      )
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(
      context,
      notificationId + 100,
      popupIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Action 1: ACCEPT
    val acceptIntent = Intent(context, ShootRequestActionReceiver::class.java).apply {
      action = ShootRequestActionReceiver.ACTION_ACCEPT_SHOOT
      putExtra(ShootRequestActionReceiver.EXTRA_BOOKING_ID, request.id)
      putExtra(ShootRequestActionReceiver.EXTRA_CREW_ID, crewUserId)
      putExtra(ShootRequestActionReceiver.EXTRA_SHOOT_TITLE, request.shootTitle.ifBlank { request.shootTypeTitle })
      putExtra(ShootRequestActionReceiver.EXTRA_SHOOT_DATE, request.dateFormatted)
    }
    val acceptPendingIntent = PendingIntent.getBroadcast(
      context,
      notificationId + 1,
      acceptIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Action 2: DECLINE
    val declineIntent = Intent(context, ShootRequestActionReceiver::class.java).apply {
      action = ShootRequestActionReceiver.ACTION_DECLINE_SHOOT
      putExtra(ShootRequestActionReceiver.EXTRA_BOOKING_ID, request.id)
      putExtra(ShootRequestActionReceiver.EXTRA_CREW_ID, crewUserId)
    }
    val declinePendingIntent = PendingIntent.getBroadcast(
      context,
      notificationId + 2,
      declineIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_INCOMING_SHOOTS)
      .setSmallIcon(R.drawable.ic_stat_shoot)
      .setContentTitle("🎬 NEW SHOOT REQUEST: ${request.shootTypeTitle}")
      .setContentText("${request.dateFormatted} • ${request.timeAndDuration} at ${request.locationName}")
      .setSubText(request.requiredRole)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText(
            "📍 ${request.locationName}\n" +
            "📅 ${request.dateFormatted} • ${request.timeAndDuration}\n" +
            "🎬 Role: ${request.requiredRole}\n" +
            "⚡ Tap ACCEPT to claim this booking."
          )
      )
      .setColor(Color.parseColor("#FFB800"))
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setOnlyAlertOnce(false)
      .setContentIntent(contentPendingIntent)
      .setFullScreenIntent(fullScreenPendingIntent, true)
      .addAction(0, "DECLINE", declinePendingIntent)
      .addAction(0, "ACCEPT", acceptPendingIntent)
      .setVibrate(longArrayOf(0, 350, 150, 350))

    try {
      NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    } catch (e: SecurityException) {
      // Permission not yet granted
    }
  }

  fun showShootConfirmedNotification(
    context: Context,
    bookingId: String,
    shootTitle: String,
    shootDate: String
  ) {
    createNotificationChannels(context)
    val notificationId = getNotificationId(bookingId)

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(ShootRequestActionReceiver.EXTRA_BOOKING_ID, bookingId)
    }
    val pendingOpen = PendingIntent.getActivity(
      context,
      notificationId + 10,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_INCOMING_SHOOTS)
      .setSmallIcon(R.drawable.ic_stat_shoot)
      .setContentTitle("✅ SHOOT CONFIRMED")
      .setContentText("You are assigned to $shootTitle ($shootDate)")
      .setColor(Color.parseColor("#10B981"))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingOpen)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (e: SecurityException) {
      // Ignored
    }
  }

  fun showAlreadyAssignedNotification(
    context: Context,
    bookingId: String,
    assignedTo: String
  ) {
    createNotificationChannels(context)
    val notificationId = getNotificationId(bookingId)

    val notification = NotificationCompat.Builder(context, CHANNEL_INCOMING_SHOOTS)
      .setSmallIcon(R.drawable.ic_stat_shoot)
      .setContentTitle("REQUEST ALREADY ASSIGNED")
      .setContentText("Claimed by $assignedTo")
      .setColor(Color.parseColor("#EF4444"))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (e: SecurityException) {
      // Ignored
    }
  }

  fun cancelIncomingNotification(context: Context, bookingId: String) {
    val notificationId = getNotificationId(bookingId)
    NotificationManagerCompat.from(context).cancel(notificationId)
  }

  fun cancelIncomingNotification(bookingId: String) {
    appContext?.let { cancelIncomingNotification(it, bookingId) }
  }

  private fun getNotificationId(bookingId: String): Int {
    return NOTIFICATION_ID_INCOMING_BASE + (bookingId.hashCode() and 0x7FFF)
  }
}
