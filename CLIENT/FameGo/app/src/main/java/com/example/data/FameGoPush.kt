package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import com.example.IncomingShootActivity
import com.example.MainActivity
import com.example.R
import com.example.model.Booking
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Push pipeline (old-app parity + killed-app wake).
 *
 * - Foreground/in-app: realtime subscriptions + notifications fan-out
 *   (supabase/002 + 006) drive the bell + full-screen Compose dialog.
 * - Background/killed: FCM high-priority data messages (push-send Edge
 *   Function) render a heads-up notification with a fullScreenIntent that
 *   launches IncomingShootActivity over the lock screen.
 * - No Firebase configured: realtime path still fires a local full-screen
 *   notification via notifyLocalIncoming() so crew phones buzz in-app.
 */
object FameGoPush {
  const val CHANNEL_ID = "famego_alerts"
  const val CHANNEL_SHOOTS = "famego_shoots"
  const val CHANNEL_DISPATCH = "famego_dispatch"
  const val NOTIF_DISPATCH_SERVICE = 1001
  private const val NOTIF_INCOMING_BASE = 2000
  private const val PREFS = "famego_push"
  private const val KEY_TOKEN = "fcm_token"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  @Volatile private var appContext: Context? = null

  /** Stable notification id per booking so updates collapse, never stack. */
  fun notifIdFor(bookingId: String?): Int =
    NOTIF_INCOMING_BASE + ((bookingId?.hashCode() ?: 0) and 0x7FFF)

  val isConfigured: Boolean
    get() = BuildConfig.FCM_SENDER_ID.isNotBlank() &&
      BuildConfig.FCM_API_KEY.isNotBlank() &&
      BuildConfig.FCM_PROJECT_ID.isNotBlank() &&
      BuildConfig.FCM_APP_ID.isNotBlank()

  /**
   * Manual Firebase init so the project builds with NO google-services.json
   * until the team creates the Firebase project. Returns false when disabled.
   */
  fun ensureInitialized(context: Context): Boolean {
    appContext = context.applicationContext
    if (!isConfigured) return false
    return runCatching {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
            .setGcmSenderId(BuildConfig.FCM_SENDER_ID)
            .setApiKey(BuildConfig.FCM_API_KEY)
            .setProjectId(BuildConfig.FCM_PROJECT_ID)
            .setApplicationId(BuildConfig.FCM_APP_ID)
            .build()
        )
      }
      true
    }.getOrDefault(false)
  }

  /** Uploads the FCM token so Edge Functions / triggers can target this phone. */
  fun registerToken(context: Context, userId: String) {
    if (userId.isBlank() || !SupabaseConfig.isConfigured || !ensureInitialized(context)) {
      appContext = context.applicationContext
      return
    }
    runCatching {
      FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
        if (t.isBlank()) return@addOnSuccessListener
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
          ?.putString(KEY_TOKEN, t)?.apply()
        scope.launch {
          SupabaseRestClient.upsert(
            "device_tokens?on_conflict=token",
            JSONObject().apply {
              put("user_id", userId)
              put("token", t)
              put("platform", "android")
            }.toString()
          )
        }
      }
    }
  }

  /** Removes this phone from push targets (called on sign-out). */
  fun unregisterToken() {
    appContext?.let {
      setCrewTopic(it, false)
      // Off-duty by definition: stop dispatch + forget persisted crew state.
      runCatching { FameGoDispatchService.stop(it) }
    }
    val t = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      ?.getString(KEY_TOKEN, null)
    appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    if (t.isNullOrBlank() || !SupabaseConfig.isConfigured) return
    scope.launch {
      runCatching { SupabaseRestClient.delete("device_tokens?token=eq.$t") }
    }
  }

  /**
   * Broadcast channel for new paid requests: every signed-in crew device
   * subscribes, so one topic send reaches all shooters (FCM background).
   */
  fun setCrewTopic(context: Context, subscribed: Boolean) {
    if (!ensureInitialized(context)) return
    runCatching {
      val messaging = FirebaseMessaging.getInstance()
      if (subscribed) messaging.subscribeToTopic("crew_requests")
      else messaging.unsubscribeFromTopic("crew_requests")
    }
  }

  private fun manager(context: Context): NotificationManager? =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

  private fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = manager(context) ?: return
    runCatching {
      manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "FameGo alerts", NotificationManager.IMPORTANCE_HIGH)
      )
      // Alarm-style channel: heads-up + full-screen intent + lock-screen show.
      val shoots = NotificationChannel(
        CHANNEL_SHOOTS, "Shoot requests", NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "New paid shoot requests — pops up even over lock screen"
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 350, 150, 350)
        enableLights(true)
        // Banner sound for closed-app arrivals (the full-screen popup
        // plays the custom chime when it opens).
        setSound(
          android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
          null
        )
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        if (Build.VERSION.SDK_INT >= 29) setBypassDnd(true)
      }
      manager.createNotificationChannel(shoots)
      // Quiet persistent channel for the crew dispatch service.
      val dispatch = NotificationChannel(
        CHANNEL_DISPATCH, "Crew dispatch", NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Keeps FameGo listening for shoot requests while you are on duty"
        setShowBadge(false)
      }
      manager.createNotificationChannel(dispatch)
    }
  }

  /**
   * Local fallback when FCM is unavailable: realtime found a fresh paid
   * request while the app runs — buzz with the same full-screen popup.
   */
  fun notifyLocalIncoming(context: Context, booking: Booking) {
    showIncomingShoot(
      context = context,
      title = booking.shootTitle.ifBlank { "New shoot request" },
      body = "${booking.venueName} • ${booking.dateText} • ${booking.timeText}",
      bookingId = booking.id
    )
  }

  /** Alarm-style popup: heads-up + full-screen over lock, even if app is closed. */
  fun showIncomingShoot(context: Context, title: String, body: String, bookingId: String?) {
    ensureChannels(context)
    val manager = manager(context) ?: return
    val fullIntent = Intent(context, IncomingShootActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(IncomingShootActivity.EXTRA_BOOKING_ID, bookingId.orEmpty())
      putExtra(IncomingShootActivity.EXTRA_TITLE, title)
      putExtra(IncomingShootActivity.EXTRA_BODY, body)
    }
    val fullPending = PendingIntent.getActivity(
      context, (bookingId?.hashCode() ?: title.hashCode()) + 1000, fullIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    // Tapping the notification body opens the app at the request detail.
    val tapIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      bookingId?.let { putExtra(IncomingShootActivity.EXTRA_BOOKING_ID, it) }
    }
    val tapPending = PendingIntent.getActivity(
      context, (bookingId?.hashCode() ?: 0), tapIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_SHOOTS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setColor(0xFFF6B941.toInt())
      .setAutoCancel(true)
      .setOngoing(false)
      .setVibrate(longArrayOf(0, 400, 200, 400))
      .setLights(0xFFF6B941.toInt(), 800, 800)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setContentIntent(tapPending)
      .setFullScreenIntent(fullPending, true)
      .build()
    runCatching { manager.notify(notifIdFor(bookingId), notification) }
  }

  /**
   * Dispatch-service banner: the same full-screen popup, plus inline
   * ACCEPT / DECLINE actions that work even with the app closed.
   */
  fun showIncomingShootRequest(
    context: Context,
    bookingId: String,
    shootTitle: String,
    detailsLine: String,
    roleLine: String,
    crewUserId: String
  ) {
    ensureChannels(context)
    val manager = manager(context) ?: return
    val notificationId = notifIdFor(bookingId)

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(IncomingShootActivity.EXTRA_BOOKING_ID, bookingId)
    }
    val contentPending = PendingIntent.getActivity(
      context, notificationId, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val popupIntent = Intent(context, IncomingShootActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(IncomingShootActivity.EXTRA_BOOKING_ID, bookingId)
      putExtra(IncomingShootActivity.EXTRA_TITLE, shootTitle)
      putExtra(IncomingShootActivity.EXTRA_BODY, detailsLine)
    }
    val fullPending = PendingIntent.getActivity(
      context, notificationId + 100, popupIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val acceptIntent = Intent(context, ShootActionReceiver::class.java).apply {
      action = ShootActionReceiver.ACTION_ACCEPT
      putExtra(ShootActionReceiver.EXTRA_BOOKING_ID, bookingId)
      putExtra(ShootActionReceiver.EXTRA_CREW_USER_ID, crewUserId)
      putExtra(ShootActionReceiver.EXTRA_TITLE, shootTitle)
      putExtra(ShootActionReceiver.EXTRA_DETAILS, detailsLine)
    }
    val acceptPending = PendingIntent.getBroadcast(
      context, notificationId + 1, acceptIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val declineIntent = Intent(context, ShootActionReceiver::class.java).apply {
      action = ShootActionReceiver.ACTION_DECLINE
      putExtra(ShootActionReceiver.EXTRA_BOOKING_ID, bookingId)
      putExtra(ShootActionReceiver.EXTRA_CREW_USER_ID, crewUserId)
    }
    val declinePending = PendingIntent.getBroadcast(
      context, notificationId + 2, declineIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_SHOOTS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("NEW SHOOT REQUEST: $shootTitle")
      .setContentText(detailsLine)
      .setSubText(roleLine)
      .setStyle(
        NotificationCompat.BigTextStyle().bigText(
          "$detailsLine\nRole: $roleLine\nTap ACCEPT to claim this booking."
        )
      )
      .setColor(0xFFF6B941.toInt())
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setOnlyAlertOnce(false)
      .setContentIntent(contentPending)
      .setFullScreenIntent(fullPending, true)
      .addAction(0, "DECLINE", declinePending)
      .addAction(0, "ACCEPT", acceptPending)
      .setVibrate(longArrayOf(0, 350, 150, 350))
      .build()
    runCatching { manager.notify(notificationId, notification) }
  }

  fun showShootConfirmed(context: Context, bookingId: String, shootTitle: String, details: String) {
    ensureChannels(context)
    val manager = manager(context) ?: return
    val notificationId = notifIdFor(bookingId)
    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(IncomingShootActivity.EXTRA_BOOKING_ID, bookingId)
    }
    val pending = PendingIntent.getActivity(
      context, notificationId + 10, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_SHOOTS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("SHOOT CONFIRMED")
      .setContentText("You are assigned to $shootTitle ($details)")
      .setColor(0xFF43D19E.toInt())
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pending)
      .build()
    runCatching { manager.notify(notificationId, notification) }
  }

  fun showDutyOff(context: Context, bookingId: String) {
    ensureChannels(context)
    val manager = manager(context) ?: return
    val notificationId = notifIdFor(bookingId)
    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(IncomingShootActivity.EXTRA_BOOKING_ID, bookingId)
    }
    val pending = PendingIntent.getActivity(
      context, notificationId + 20, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_SHOOTS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("You're off duty")
      .setContentText("Go on duty in the app to claim this shoot before someone else does.")
      .setColor(0xFFF6B941.toInt())
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pending)
      .build()
    runCatching { manager.notify(notificationId, notification) }
  }

  fun showAlreadyAssigned(context: Context, bookingId: String, assignedTo: String) {    ensureChannels(context)
    val manager = manager(context) ?: return
    val notification = NotificationCompat.Builder(context, CHANNEL_SHOOTS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("REQUEST ALREADY ASSIGNED")
      .setContentText("Claimed by $assignedTo")
      .setColor(0xFFFF5A5F.toInt())
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .build()
    runCatching { manager.notify(notifIdFor(bookingId), notification) }
  }

  fun cancelIncoming(context: Context, bookingId: String) {
    runCatching { manager(context)?.cancel(notifIdFor(bookingId)) }
  }

  /** Quiet persistent notification under which the dispatch service runs. */
  fun buildDispatchForegroundNotification(context: Context, crewName: String): android.app.Notification {
    ensureChannels(context)
    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pending = PendingIntent.getActivity(
      context, 100, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Builder(context, CHANNEL_DISPATCH)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("FameGo Dispatch • Available")
      .setContentText("$crewName is online and listening for shoot requests")
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(pending)
      .setColor(0xFFF6B941.toInt())
      .build()
  }

  fun showAlert(context: Context, title: String, body: String, bookingId: String?) {
    ensureChannels(context)
    val manager = manager(context) ?: return
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      bookingId?.let { putExtra(IncomingShootActivity.EXTRA_BOOKING_ID, it) }
    }
    val pending = PendingIntent.getActivity(
      context, (bookingId?.hashCode() ?: 0), intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setColor(0xFFF6B941.toInt())
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pending)
      .build()
    runCatching { manager.notify((bookingId?.hashCode() ?: title.hashCode()), notification) }
  }
}

/** Receives FCM messages when Firebase is configured; inert otherwise. */
class FameGoMessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(message: RemoteMessage) {
    val title = message.notification?.title ?: message.data["title"] ?: "FameGo"
    val body = message.notification?.body ?: message.data["body"]
      ?: message.data["message"] ?: return
    val bookingId = message.data["booking_id"]
    val type = message.data["type"].orEmpty()
    if (type == "shoot_request" || message.data.containsKey("booking_id")) {
      FameGoPush.showIncomingShoot(this, title, body, bookingId)
    } else {
      FameGoPush.showAlert(this, title, body, bookingId)
    }
  }

  override fun onNewToken(token: String) {
    val userId = SupabaseSession.cachedProfile()?.id.orEmpty()
    if (userId.isNotBlank()) FameGoPush.registerToken(this, userId)
  }
}
