package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * Foreground service that keeps crew dispatch alive when the app is closed.
 *
 * How it survives app death (Famebook pattern, FameGo style):
 * - Crew identity is persisted in [FameGoDispatchStore], so the service
 *   restarts from a cold process (swipe-away, reboot) with no activity alive.
 * - On every start it restores the Supabase session from local storage, so
 *   network calls stay authenticated after process death.
 * - Instead of same-process events it polls the backend for open paid
 *   requests, so a booking made on any other device surfaces here as a
 *   heads-up notification with Accept / Decline actions + full-screen popup.
 */
class FameGoDispatchService : Service() {

  companion object {
    const val ACTION_START = "com.famebros.famego.ACTION_START_DISPATCH"
    const val ACTION_STOP = "com.famebros.famego.ACTION_STOP_DISPATCH"

    const val EXTRA_CREW_USER_ID = "extra_crew_user_id"
    const val EXTRA_CREW_NAME = "extra_crew_name"

    private const val POLL_INTERVAL_MS = 20_000L
    private const val OPEN_POOL_SELECT =
      "bookings?select=id,booking_code,shoot_title,venue_name,shoot_date,shoot_time,plan_price_paise,payment_status" +
        "&status=eq.SEARCHING_CREW&payment_status=in.(PENDING,PAID)&order=created_at.desc&limit=25"

    fun start(context: Context, crewUserId: String, crewName: String) {
      FameGoDispatchStore(context).setDispatch(crewUserId, crewName)
      val intent = Intent(context, FameGoDispatchService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_CREW_USER_ID, crewUserId)
        putExtra(EXTRA_CREW_NAME, crewName)
      }
      runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    fun stop(context: Context) {
      FameGoDispatchStore(context).clearDispatch()
      val intent = Intent(context, FameGoDispatchService::class.java).apply {
        action = ACTION_STOP
      }
      runCatching { context.startService(intent) }
    }
  }

  private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
  private var pollJob: Job? = null
  private var crewUserId: String = ""
  private var crewName: String = "FameGo Crew"

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        pollJob?.cancel()
        stopForegroundCompat()
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_START, null -> {
        val store = FameGoDispatchStore(this)
        intent?.getStringExtra(EXTRA_CREW_USER_ID)?.let { crewUserId = it }
          ?: store.crewUserId()?.let { crewUserId = it }
        intent?.getStringExtra(EXTRA_CREW_NAME)?.let { crewName = it }
          ?: store.crewName()?.let { crewName = it }

        if (crewUserId.isBlank()) {
          stopSelf()
          return START_NOT_STICKY
        }
        val notification = FameGoPush.buildDispatchForegroundNotification(this, crewName)
        // Explicit service type: on Android 14+ the 2-arg startForeground
        // throws when the type is only in the manifest, killing dispatch.
        var foregrounded = false
        runCatching {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.core.app.ServiceCompat.startForeground(
              this,
              FameGoPush.NOTIF_DISPATCH_SERVICE,
              notification,
              android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
          } else {
            startForeground(FameGoPush.NOTIF_DISPATCH_SERVICE, notification)
          }
          foregrounded = true
        }
        // A denied foreground start (e.g. cold boot on Android 12+) must
        // stop, not limp along unforegrounded until the system kills us.
        if (!foregrounded) {
          stopSelf()
          return START_NOT_STICKY
        }
        startPolling()
      }
    }
    return START_STICKY
  }

  private fun startPolling() {
    pollJob?.cancel()
    pollJob = serviceScope.launch {
      // Cold process: tokens live in prefs; re-authenticate before polling.
      SupabaseSession.initialize(applicationContext)
      runCatching { SupabaseAuthClient.restoreSession() }
      while (isActive) {
        runCatching { pollOnce() }
        delay(POLL_INTERVAL_MS)
      }
    }
  }

  private suspend fun pollOnce() {
    val store = FameGoDispatchStore(this)
    // Off-duty stops dispatch — but only on a confirmed server read; a
    // failed fetch (offline) must never stand us down.
    when (fetchAvailability()) {
      false -> {
        store.clearDispatch()
        pollJob?.cancel()
        stopForegroundCompat()
        stopSelf()
        return
      }
      else -> Unit
    }
    val open = SupabaseRestClient.get(OPEN_POOL_SELECT).getOrElse { return }
    val rows = runCatching { JSONArray(open) }.getOrElse { return }
    val openIds = buildSet {
      for (i in 0 until rows.length()) {
        rows.optJSONObject(i)?.optString("id")?.ifBlank { null }?.let { add(it) }
      }
    }
    // Requests that closed (claimed elsewhere / cancelled): clear the banner.
    (store.notifiedIds() - openIds).forEach { staleId ->
      FameGoPush.cancelIncoming(this, staleId)
      store.unmarkNotified(staleId)
    }
    // New paid requests: heads-up + full-screen popup, app closed or not.
    for (i in 0 until rows.length()) {
      val o = rows.optJSONObject(i) ?: continue
      val id = o.optString("id")
      if (id.isBlank() || store.isNotified(id) || store.isDeclined(id)) continue
      store.markNotified(id)
      val title = o.optString("shoot_title").ifBlank { "New shoot request" }
      val venue = o.optString("venue_name").ifBlank { "Venue TBA" }
      val date = o.optString("shoot_date").ifBlank { "" }
      val time = FameGoTime.toDisplay(o.optString("shoot_time"))
      val price = (o.optInt("plan_price_paise", 0) / 100).coerceAtLeast(0)
      val payNote = if (o.optString("payment_status") == "PENDING") "pay on accept" else "paid"
      val details = listOf(venue, listOf(date, time).filter { it.isNotBlank() }.joinToString(" • "))
        .filter { it.isNotBlank() }.joinToString(" • ") + " • ₹${"%,d".format(price)} • $payNote"
      FameGoPush.showIncomingShootRequest(
        context = this,
        bookingId = id,
        shootTitle = title,
        detailsLine = details,
        roleLine = "FameGo verified crew call sheet",
        crewUserId = crewUserId
      )
    }
  }

  /** Tri-state: true available / false off-duty / null unknown (offline). */
  private suspend fun fetchAvailability(): Boolean? {
    val raw = SupabaseRestClient.get(
      "crew_profiles?select=is_available&user_id=eq.$crewUserId&limit=1"
    ).getOrElse { return null }
    return runCatching {
      val row = JSONArray(raw).optJSONObject(0) ?: return null
      if (!row.has("is_available")) null else row.optBoolean("is_available")
    }.getOrNull()
  }

  /**
   * App swiped away: schedule an immediate restart while dispatch is on.
   * Force-stop and battery-restricted devices are the only case this cannot
   * cover — a tap on the app restores dispatch instantly.
   */
  override fun onTaskRemoved(rootIntent: Intent?) {
    if (FameGoDispatchStore(this).isDispatchOn()) {
      val restart = Intent(this, FameGoDispatchService::class.java).apply {
        action = ACTION_START
      }
      // Foreground-service intent: a plain getService() restart is denied a
      // background FGS start on Android 12+, which silently kills dispatch.
      // getForegroundService needs API 26; older devices use getService.
      val pending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        PendingIntent.getForegroundService(
          this, 0, restart,
          PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
      } else {
        PendingIntent.getService(
          this, 0, restart,
          PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
      }
      runCatching {
        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        alarm.set(
          AlarmManager.ELAPSED_REALTIME_WAKEUP,
          SystemClock.elapsedRealtime() + 1_000,
          pending
        )
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

/** "09:00:00" -> "09:00 AM" for dispatch banners without the repository. */
private object FameGoTime {
  fun toDisplay(supabaseTime: String): String {
    val match = Regex("""(\d{1,2}):(\d{2})""").find(supabaseTime) ?: return ""
    val hour24 = match.groupValues[1].toIntOrNull() ?: return ""
    val minute = match.groupValues[2]
    val ampm = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
    return "%02d:%s %s".format(hour12, minute, ampm)
  }
}
