package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

/**
 * Restarts crew dispatch after a device reboot when dispatch was left on.
 * The service restores its own session and resumes backend polling.
 */
class FameGoBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    if (!FameGoDispatchStore(context).isDispatchOn()) return
    runCatching {
      ContextCompat.startForegroundService(
        context,
        Intent(context, FameGoDispatchService::class.java).apply {
          action = FameGoDispatchService.ACTION_START
        }
      )
    }
  }
}

/**
 * Notification actions for shoot requests. Taps can arrive with a dead
 * process, so the session is restored from local storage before touching
 * the backend — then accept runs the same rpc/accept_booking the app uses.
 */
class ShootActionReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION_ACCEPT = "com.famebros.famego.ACTION_ACCEPT_SHOOT"
    const val ACTION_DECLINE = "com.famebros.famego.ACTION_DECLINE_SHOOT"

    const val EXTRA_BOOKING_ID = "extra_booking_id"
    const val EXTRA_CREW_USER_ID = "extra_crew_user_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_DETAILS = "extra_details"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID).orEmpty()
    if (bookingId.isBlank()) return
    val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Shoot" }
    val details = intent.getStringExtra(EXTRA_DETAILS).orEmpty()

    val pending = goAsync()
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    scope.launch {
      SupabaseSession.initialize(context.applicationContext)
      runCatching { SupabaseAuthClient.restoreSession() }
      try {
        when (action) {
          ACTION_ACCEPT -> {
            val rpc = SupabaseRestClient.post(
              "rpc/accept_booking",
              org.json.JSONObject().apply { put("p_booking_id", bookingId) }.toString()
            )
            val store = FameGoDispatchStore(context)
            if (rpc.isSuccess) {
              store.unmarkNotified(bookingId)
              FameGoPush.showShootConfirmed(context, bookingId, title, details)
            } else {
              // Never blanket-claim "taken": re-read the booking and say
              // what actually happened (mine / taken / off-duty / retry).
              when (val verdict = AcceptResolver.resolve(bookingId)) {
                is AcceptOutcome.Confirmed -> {
                  store.unmarkNotified(bookingId)
                  FameGoPush.showShootConfirmed(
                    context, bookingId,
                    verdict.title.ifBlank { title }, verdict.details.ifBlank { details }
                  )
                }
                is AcceptOutcome.Taken -> {
                  store.unmarkNotified(bookingId)
                  FameGoPush.showAlreadyAssigned(context, bookingId, verdict.name)
                }
                AcceptOutcome.OffDuty -> {
                  FameGoPush.showDutyOff(context, bookingId)
                }
                AcceptOutcome.Gone -> {
                  store.unmarkNotified(bookingId)
                  FameGoPush.cancelIncoming(context, bookingId)
                }
                AcceptOutcome.Retry -> {
                  FameGoPush.showAlert(
                    context,
                    "Couldn't claim yet",
                    "$title — tap to open the request and retry.",
                    bookingId
                  )
                }
              }
            }
          }
          ACTION_DECLINE -> {
            FameGoDispatchStore(context).markDeclined(bookingId)
            FameGoDispatchStore(context).unmarkNotified(bookingId)
            FameGoPush.cancelIncoming(context, bookingId)
          }
        }
      } finally {
        pending.finish()
      }
    }
  }
}
