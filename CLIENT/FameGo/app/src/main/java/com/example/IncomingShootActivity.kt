package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.FameGoDispatchStore
import com.example.data.FameGoPush
import com.example.data.SupabaseAuthClient
import com.example.data.SupabaseRestClient
import com.example.data.SupabaseSession
import com.example.ui.components.FameGoAmbientBackground
import com.example.ui.components.FameGoButton
import com.example.ui.components.FameGoSnakeLoader
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoLiveRed
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoTheme
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * Alarm-style full-screen popup for a new paid shoot request.
 * Launched via fullScreenIntent so it appears even when the app is
 * closed, in background, or the phone is locked (Famebook pattern,
 * FameGo styling: black + pulsing gold).
 *
 * Works from a dead process: restores the session, loads the live booking,
 * and Accept / Decline hit the backend directly.
 */
class IncomingShootActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
      )
    }
    SupabaseSession.initialize(applicationContext)
    lifecycleScope.launch { runCatching { SupabaseAuthClient.restoreSession() } }
    com.example.data.FameGoSfx.ensure(applicationContext)
    com.example.data.FameGoSfx.notify(applicationContext)

    val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID).orEmpty()
    val fallbackTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    val fallbackBody = intent.getStringExtra(EXTRA_BODY).orEmpty()
    setContent {
      FameGoTheme {
        FameGoAmbientBackground(modifier = Modifier.fillMaxSize()) {
          IncomingShootContent(
            bookingId = bookingId,
            fallbackTitle = fallbackTitle,
            fallbackBody = fallbackBody,
            onOpenApp = {
              val open = Intent(this@IncomingShootActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_BOOKING_ID, bookingId)
              }
              startActivity(open)
              finish()
            },
            onDone = { finish() }
          )
        }
      }
    }
  }

  companion object {
    const val EXTRA_BOOKING_ID = "booking_id"
    const val EXTRA_TITLE = "incoming_title"
    const val EXTRA_BODY = "incoming_body"
  }
}

private data class PopupBooking(
  val id: String,
  val title: String,
  val details: String,
  val roleLine: String,
  val assignedCrewProfileId: String?,
  val assignedName: String
)

private sealed interface PopupState {
  data object Loading : PopupState
  data class Active(val booking: PopupBooking) : PopupState
  data class Confirmed(val booking: PopupBooking) : PopupState
  data class Taken(val name: String) : PopupState
  data object OffDuty : PopupState
  data object Gone : PopupState
}

@Composable
private fun IncomingShootContent(
  bookingId: String,
  fallbackTitle: String,
  fallbackBody: String,
  onOpenApp: () -> Unit,
  onDone: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var state by remember { mutableStateOf<PopupState>(PopupState.Loading) }
  var working by remember { mutableStateOf(false) }
  var notice by remember { mutableStateOf<String?>(null) }
  val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext

  LaunchedEffect(bookingId) {
    if (bookingId.isBlank()) {
      state = PopupState.Gone
      return@LaunchedEffect
    }
    state = loadPopupBooking(bookingId) ?: PopupState.Gone
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.weight(1f))
    when (val s = state) {
      PopupState.Loading -> {
        FameGoSnakeLoader(modifier = Modifier.size(width = 150.dp, height = 88.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = fallbackTitle.ifBlank { "New shoot request" },
          color = FameGoWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
        if (fallbackBody.isNotBlank()) {
          Text(
            text = fallbackBody, color = FameGoTextSecondary, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
      PopupState.Gone -> {
        LaunchedEffect(Unit) { onDone() }
      }
      is PopupState.Taken -> TakenCard(name = s.name, onClose = onDone)
      is PopupState.OffDuty -> OffDutyCard(onOpenApp = onOpenApp, onClose = onDone)
      is PopupState.Confirmed -> ConfirmedCard(
        bookingId = s.booking.id,
        title = s.booking.title,
        details = s.booking.details,
        onView = onOpenApp,
        onDone = onDone
      )
      is PopupState.Active -> RequestCard(
        booking = s.booking,
        working = working,
        notice = notice,
        onAccept = {
          if (working) return@RequestCard
          working = true
          notice = null
          scope.launch {
            val result = SupabaseRestClient.post(
              "rpc/accept_booking",
              org.json.JSONObject().apply { put("p_booking_id", bookingId) }.toString()
            )
            working = false
            if (result.isSuccess) {
              state = PopupState.Confirmed(s.booking)
            } else {
              // Same verdict engine as the notification action: say why.
              when (val verdict = com.example.data.AcceptResolver.resolve(bookingId)) {
                is com.example.data.AcceptOutcome.Confirmed ->
                  state = PopupState.Confirmed(s.booking)
                is com.example.data.AcceptOutcome.Taken ->
                  state = PopupState.Taken(verdict.name)
                com.example.data.AcceptOutcome.OffDuty ->
                  state = PopupState.OffDuty
                com.example.data.AcceptOutcome.Gone ->
                  state = PopupState.Gone
                com.example.data.AcceptOutcome.Retry -> {
                  notice = "Couldn't reach the server — check connection and retry."
                  state = PopupState.Active(s.booking)
                }
              }
            }
          }
        },
        onDecline = {
          if (working) return@RequestCard
          // Dead-process-safe decline: persist + clear the banner, then close.
          FameGoDispatchStore(appContext).markDeclined(bookingId)
          FameGoDispatchStore(appContext).unmarkNotified(bookingId)
          FameGoPush.cancelIncoming(appContext, bookingId)
          onDone()
        }
      )
    }
    Spacer(modifier = Modifier.weight(1f))
    Text(
      text = "FameGo • Famebros Studio",
      color = Color.White.copy(alpha = 0.4f),
      fontSize = 11.sp,
      modifier = Modifier.padding(bottom = 32.dp)
    )
  }
}

/** Loads the live booking + figures out Taken / Confirmed / Active. */
private suspend fun loadPopupBooking(bookingId: String): PopupState? {
  val raw = SupabaseRestClient.get(
    "bookings?select=id,shoot_title,venue_name,shoot_date,shoot_time,plan_price_paise,status,payment_status,booking_assignments(crew_id,name_snapshot)&id=eq.$bookingId&limit=1"
  ).getOrNull() ?: return null
  val o = runCatching { JSONArray(raw).optJSONObject(0) }.getOrNull() ?: return null
  if (o.optString("status") != "SEARCHING_CREW" || o.optString("payment_status") != "PAID") {
    // Still show Gone only when claimed/cancelled AND assigned elsewhere;
    // a cancelled booking has no assignment — treat as gone too.
    val assignments = o.optJSONArray("booking_assignments")
    if (assignments == null || assignments.length() == 0) return PopupState.Gone
  }
  val title = o.optString("shoot_title").ifBlank { "New shoot request" }
  val venue = o.optString("venue_name").ifBlank { "Venue TBA" }
  val date = o.optString("shoot_date").ifBlank { "" }
  val time = o.optString("shoot_time").ifBlank { "" }
  val price = (o.optInt("plan_price_paise", 0) / 100).coerceAtLeast(0)
  val details = listOf(venue, listOf(date, time).filter { it.isNotBlank() }.joinToString(" • "))
    .filter { it.isNotBlank() }.joinToString(" • ") + " • ₹${"%,d".format(price)}"
  val assignments = o.optJSONArray("booking_assignments")
  val first = if (assignments != null && assignments.length() > 0) assignments.optJSONObject(0) else null
  val assignedCrewId = first?.optString("crew_id")?.ifBlank { null }
  val assignedName = first?.optString("name_snapshot").orEmpty().ifBlank { "another crew member" }
  val booking = PopupBooking(
    id = bookingId, title = title, details = details,
    roleLine = "FameGo verified crew call sheet",
    assignedCrewProfileId = assignedCrewId, assignedName = assignedName
  )
  if (assignedCrewId != null) {
    return if (assignedCrewId == myCrewProfileId()) PopupState.Confirmed(booking)
    else PopupState.Taken(assignedName)
  }
  return PopupState.Active(booking)
}

private suspend fun myCrewProfileId(): String? {
  val me = SupabaseSession.cachedProfile()?.id ?: return null
  val raw = SupabaseRestClient.get("crew_profiles?select=id&user_id=eq.$me&limit=1").getOrNull()
    ?: return null
  return runCatching { JSONArray(raw).optJSONObject(0)?.optString("id") }.getOrNull()
    ?.ifBlank { null }
}

@Composable
private fun PulsingBadge() {
  val pulseLoop = rememberInfiniteTransition(label = "incomingFullPulse")
  val pulse by pulseLoop.animateFloat(
    initialValue = 0.5f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1100, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "incomingFullBadge"
  )
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = FameGoGold.copy(alpha = 0.16f * pulse + 0.08f)
  ) {
    Text(
      text = "● NEW SHOOT REQUEST",
      color = FameGoGold.copy(alpha = 0.6f * pulse + 0.4f),
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.2.sp,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
    )
  }
}

@Composable
private fun RequestCard(
  booking: PopupBooking,
  working: Boolean,
  notice: String?,
  onAccept: () -> Unit,
  onDecline: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(26.dp),
    color = FameGoCard,
    border = androidx.compose.foundation.BorderStroke(1.5.dp, FameGoGold),
    shadowElevation = 24.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      PulsingBadge()
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = booking.title, color = FameGoWhite, fontSize = 22.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
      )
      Text(
        text = booking.details, color = FameGoTextSecondary, fontSize = 14.sp,
        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
      )
      Text(
        text = booking.roleLine, color = FameGoTextMuted, fontSize = 12.sp,
        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
      )
      if (!notice.isNullOrBlank()) {
        Text(
          text = notice, color = FameGoGold, fontSize = 12.sp,
          textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        androidx.compose.material3.OutlinedButton(
          onClick = onDecline,
          enabled = !working,
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, FameGoTextMuted.copy(alpha = 0.4f)),
          modifier = Modifier.weight(0.9f).height(52.dp)
        ) {
          Text("DECLINE", color = FameGoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        FameGoButton(
          text = if (working) "Claiming…" else "ACCEPT",
          onClick = onAccept,
          enabled = !working,
          modifier = Modifier.weight(1.2f)
        )
      }
    }
  }
}

@Composable
private fun ConfirmedCard(
  bookingId: String,
  title: String,
  details: String,
  onView: () -> Unit,
  onDone: () -> Unit
) {
  // Clear the banner now that the shooter has seen the confirmation.
  val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
  LaunchedEffect(bookingId) {
    FameGoDispatchStore(appContext).unmarkNotified(bookingId)
    FameGoPush.cancelIncoming(appContext, bookingId)
  }
  Surface(
    shape = RoundedCornerShape(26.dp),
    color = FameGoCard,
    border = androidx.compose.foundation.BorderStroke(1.5.dp, FameGoSuccessGreen),
    shadowElevation = 24.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Default.Check, contentDescription = null,
        tint = FameGoSuccessGreen, modifier = Modifier.size(40.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "SHOOT CONFIRMED", color = FameGoSuccessGreen, fontSize = 13.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title, color = FameGoWhite, fontSize = 20.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
      )
      Text(
        text = details, color = FameGoTextSecondary, fontSize = 13.sp,
        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
      )
      Spacer(modifier = Modifier.height(20.dp))
      FameGoButton(text = "View shoot", onClick = onView, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(8.dp))
      TextButton(onClick = onDone) {
        Text("Done", color = FameGoTextSecondary, fontSize = 14.sp)
      }
    }
  }
}

@Composable
private fun TakenCard(name: String, onClose: () -> Unit) {  Surface(
    shape = RoundedCornerShape(26.dp),
    color = FameGoCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, FameGoLiveRed.copy(alpha = 0.5f)),
    shadowElevation = 24.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Default.Close, contentDescription = null,
        tint = FameGoLiveRed, modifier = Modifier.size(32.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Claimed by $name", color = FameGoWhite, fontSize = 17.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(16.dp))
      TextButton(onClick = onClose) {
        Text("Close", color = FameGoTextSecondary, fontSize = 14.sp)
      }
    }
  }
}

@Composable
private fun OffDutyCard(onOpenApp: () -> Unit, onClose: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(26.dp),
    color = FameGoCard,
    border = androidx.compose.foundation.BorderStroke(1.5.dp, FameGoGold),
    shadowElevation = 24.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      PulsingBadge()
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "You're off duty",
        color = FameGoWhite, fontSize = 20.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
      )
      Text(
        text = "Go on duty in the app to claim this shoot before someone else does.",
        color = FameGoTextSecondary, fontSize = 13.sp,
        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
      )
      Spacer(modifier = Modifier.height(20.dp))
      FameGoButton(text = "Open app", onClick = onOpenApp, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(8.dp))
      TextButton(onClick = onClose) {
        Text("Close", color = FameGoTextSecondary, fontSize = 14.sp)
      }
    }
  }
}
