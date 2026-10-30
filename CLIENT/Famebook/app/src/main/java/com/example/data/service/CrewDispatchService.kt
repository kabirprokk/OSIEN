package com.example.data.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.data.notification.IncomingRequestNotificationHelper
import com.example.di.ServiceLocator
import com.example.domain.model.Booking
import com.example.domain.model.IncomingShootRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps crew dispatch alive when the app is closed.
 *
 * How it survives app death:
 * - Crew identity is persisted in [CrewDispatchStore], so the service can
 *   restart from a cold process (swipe-away, reboot) with no activity alive.
 * - On every start it restores the Supabase session from local storage, so
 *   network calls stay authenticated after process death.
 * - Instead of same-process events it polls the backend for open requests,
 *   so a booking made on any other device surfaces here as a heads-up
 *   notification with Accept / Decline actions.
 */
class CrewDispatchService : Service() {

  companion object {
    const val ACTION_START_DISPATCH = "com.example.ACTION_START_DISPATCH"
    const val ACTION_STOP_DISPATCH = "com.example.ACTION_STOP_DISPATCH"

    const val EXTRA_CREW_USER_ID = "extra_crew_user_id"
    const val EXTRA_CREW_NAME = "extra_crew_name"

    private const val POLL_INTERVAL_MS = 20_000L

    fun startService(context: Context, crewUserId: String, crewName: String) {
      CrewDispatchStore(context).setDispatch(crewUserId, crewName)
      val intent = Intent(context, CrewDispatchService::class.java).apply {
        action = ACTION_START_DISPATCH
        putExtra(EXTRA_CREW_USER_ID, crewUserId)
        putExtra(EXTRA_CREW_NAME, crewName)
      }
      try {
        ContextCompat.startForegroundService(context, intent)
      } catch (e: Exception) {
        // Fallback if background restrictions apply
      }
    }

    fun stopService(context: Context) {
      CrewDispatchStore(context).clearDispatch()
      val intent = Intent(context, CrewDispatchService::class.java).apply {
        action = ACTION_STOP_DISPATCH
      }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        // Handled
      }
    }
  }

  private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
  private var pollJob: Job? = null
  private var crewUserId: String = ""
  private var crewName: String = "Crew Member"
  private val notifiedIds = mutableSetOf<String>()

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP_DISPATCH -> {
        pollJob?.cancel()
        stopForegroundCompat()
        stopSelf()
        return START_NOT_STICKY
      }

      ACTION_START_DISPATCH, null -> {
        val store = CrewDispatchStore(this)
        intent?.getStringExtra(EXTRA_CREW_USER_ID)?.let { crewUserId = it }
          ?: store.crewUserId()?.let { crewUserId = it }
        intent?.getStringExtra(EXTRA_CREW_NAME)?.let { crewName = it }
          ?: store.crewName()?.let { crewName = it }

        if (crewUserId.isBlank()) {
          stopSelf()
          return START_NOT_STICKY
        }

        val notification = IncomingRequestNotificationHelper.buildDispatchForegroundNotification(
          this,
          crewName
        )
        try {
          startForeground(IncomingRequestNotificationHelper.NOTIFICATION_ID_DISPATCH_SERVICE, notification)
        } catch (e: Exception) {
          // Handled
        }

        startPolling()
      }
    }

    return START_STICKY
  }

  private fun startPolling() {
    pollJob?.cancel()
    pollJob = serviceScope.launch {
      // Re-authenticate: after process death the in-memory token is gone.
      ServiceLocator.init(applicationContext)
      runCatching { ServiceLocator.userRepository.restoreSession() }

      while (isActive) {
        try {
          pollOnce()
        } catch (_: Exception) {
          // Transient network failure: keep the loop alive for the next round.
        }
        delay(POLL_INTERVAL_MS)
      }
    }
  }

  private suspend fun pollOnce() {
    // Off-duty or signed out: stand down and clear persisted dispatch.
    val profile = runCatching {
      ServiceLocator.crewRepository.getCrewProfileByUserId(crewUserId).first()
    }.getOrNull()
    if (profile?.isAvailable != true) {
      CrewDispatchStore(this).clearDispatch()
      pollJob?.cancel()
      stopForegroundCompat()
      stopSelf()
      return
    }

    val open = runCatching {
      ServiceLocator.bookingRepository.getIncomingRequestsForCrew(crewUserId).first()
    }.getOrElse { return }

    val openIds = open.map { it.id }.toSet()

    // Requests that closed (accepted elsewhere / cancelled): clear the banner.
    (notifiedIds - openIds).forEach { staleId ->
      IncomingRequestNotificationHelper.cancelIncomingNotification(this, staleId)
      notifiedIds.remove(staleId)
    }

    // New requests: heads-up banner with Accept / Decline, whatever app is open.
    open.forEach { booking ->
      if (notifiedIds.add(booking.id)) {
        IncomingRequestNotificationHelper.showIncomingShootNotification(
          context = this,
          request = booking.toIncomingRequest(),
          crewUserId = crewUserId
        )
      }
    }
  }

  private fun Booking.toIncomingRequest() = IncomingShootRequest(
    id = id,
    shootTypeTitle = shootType.title,
    shootTitle = title,
    dateFormatted = date.uppercase(),
    timeAndDuration = "$startTime • $durationHours HOURS",
    locationName = location.name,
    requiredRole = requirements.firstOrNull()?.role?.title ?: "Lead Cinematographer",
    rawBooking = this
  )

  /**
   * App swiped away: schedule an immediate restart while dispatch is on.
   * Force-stop and battery-restricted devices are the only case this cannot
   * cover — a tap on the app restores dispatch instantly.
   */
  override fun onTaskRemoved(rootIntent: Intent?) {
    if (CrewDispatchStore(this).isDispatchOn()) {
      val restart = Intent(this, CrewDispatchService::class.java).apply {
        action = ACTION_START_DISPATCH
      }
      val pending = PendingIntent.getService(
        this, 0, restart,
        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
      )
      val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
      try {
        alarmManager.set(
          AlarmManager.ELAPSED_REALTIME_WAKEUP,
          SystemClock.elapsedRealtime() + 1_000,
          pending
        )
      } catch (_: Exception) {
        // Handled
      }
    }
  }

  private fun stopForegroundCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }
}
