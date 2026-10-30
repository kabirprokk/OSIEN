package com.example.presentation.crew.request

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.notification.IncomingRequestNotificationHelper
import com.example.data.receiver.ShootRequestActionReceiver
import com.example.di.ServiceLocator
import com.example.domain.model.Booking
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Outside-the-app incoming shoot popup. Launched by the notification's
 * full-screen intent when a request arrives while FameBook is closed, so crew
 * get the same rounded Accept / Decline card over any app or the lock screen.
 */
class CrewIncomingActivity : ComponentActivity() {

  companion object {
    const val EXTRA_BOOKING_ID = "extra_booking_id"
    const val EXTRA_CREW_ID = "extra_crew_id"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    // Visible over the lock screen like an incoming call.
    setShowWhenLocked(true)
    setTurnScreenOn(true)

    ServiceLocator.init(applicationContext)
    lifecycleScope.launch {
      runCatching { ServiceLocator.userRepository.restoreSession() }
    }

    val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID).orEmpty()
    val crewId = intent.getStringExtra(EXTRA_CREW_ID).orEmpty()

    setContent {
      com.example.ui.theme.FameBookTheme {
        IncomingPopupScreen(
          bookingId = bookingId,
          crewId = crewId,
          onOpenApp = { id ->
            startActivity(
              Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ShootRequestActionReceiver.EXTRA_BOOKING_ID, id)
              }
            )
            finish()
          },
          onFinish = { finish() }
        )
      }
    }
  }
}

private sealed interface PopupState {
  object Loading : PopupState
  data class Active(val booking: Booking) : PopupState
  data class Confirmed(val booking: Booking) : PopupState
  data class Taken(val name: String) : PopupState
  object Gone : PopupState
}

@Composable
private fun IncomingPopupScreen(
  bookingId: String,
  crewId: String,
  onOpenApp: (String) -> Unit,
  onFinish: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var state by remember { mutableStateOf<PopupState>(PopupState.Loading) }
  var working by remember { mutableStateOf(false) }

  LaunchedEffect(bookingId) {
    val booking = runCatching {
      ServiceLocator.bookingRepository.getBooking(bookingId).first()
    }.getOrNull()
    state = when {
      booking == null -> PopupState.Gone
      booking.assignedCrewId != null && booking.assignedCrewId != crewId ->
        PopupState.Taken(booking.assignedCrewName ?: "Another crew member")
      booking.assignedCrewId == crewId -> PopupState.Confirmed(booking)
      else -> PopupState.Active(booking)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xCC000000)),
    contentAlignment = Alignment.Center
  ) {
    when (val s = state) {
      PopupState.Loading -> CircularProgressIndicator(color = AmberGold)
      PopupState.Gone -> {
        LaunchedEffect(Unit) {
          IncomingRequestNotificationHelper.cancelIncomingNotification(bookingId)
          onFinish()
        }
      }
      is PopupState.Taken -> TakenCard(name = s.name, onFinish = onFinish)
      is PopupState.Confirmed -> ConfirmedCard(
        title = s.booking.title,
        onOpenApp = { onOpenApp(s.booking.id) },
        onFinish = onFinish
      )
      is PopupState.Active -> RequestCard(
        booking = s.booking,
        working = working,
        onAccept = {
          if (working) return@RequestCard
          working = true
          scope.launch {
            val result = ServiceLocator.bookingRepository.acceptBooking(bookingId, crewId)
            working = false
            if (result.isSuccess) {
              IncomingRequestNotificationHelper.cancelIncomingNotification(bookingId)
              state = PopupState.Confirmed(result.getOrThrow())
            } else {
              state = PopupState.Taken("Another crew member")
            }
          }
        },
        onDecline = {
          if (working) return@RequestCard
          working = true
          scope.launch {
            runCatching { ServiceLocator.bookingRepository.declineBooking(bookingId, crewId) }
            IncomingRequestNotificationHelper.cancelIncomingNotification(bookingId)
            onFinish()
          }
        }
      )
    }
  }
}

@Composable
private fun RequestCard(
  booking: Booking,
  working: Boolean,
  onAccept: () -> Unit,
  onDecline: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color(0xF2141720),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x33FFFFFF)),
    shadowElevation = 22.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .widthIn(max = 460.dp)
      .testTag("incoming_popup_outside_app")
  ) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(AmberGold)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "NEW SHOOT REQUEST",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
          ),
          color = AmberGold
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = booking.shootType.title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
        color = PureWhite
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "${booking.date} • ${booking.startTime} • ${booking.durationHours} HOURS",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = AmberGold
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "${booking.location.name}, ${booking.location.address}",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = booking.requirements.firstOrNull()?.role?.title ?: "Lead Cinematographer",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = PureWhite
      )

      Spacer(modifier = Modifier.height(24.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onDecline,
          enabled = !working,
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
          modifier = Modifier
            .weight(0.38f)
            .height(52.dp)
            .testTag("incoming_popup_decline"),
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            text = "DECLINE",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary
          )
        }

        Button(
          onClick = onAccept,
          enabled = !working,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = AmberGold,
            contentColor = ObsidianBlack
          ),
          modifier = Modifier
            .weight(0.62f)
            .height(52.dp)
            .testTag("incoming_popup_accept"),
          contentPadding = PaddingValues(0.dp)
        ) {
          if (working) {
            CircularProgressIndicator(
              color = ObsidianBlack,
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp
            )
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              androidx.compose.material3.Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ObsidianBlack,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "ACCEPT",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp
                ),
                color = ObsidianBlack
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ConfirmedCard(title: String, onOpenApp: () -> Unit, onFinish: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color(0xF2121A16),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, EmeraldSuccess.copy(alpha = 0.7f)),
    shadowElevation = 24.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .widthIn(max = 460.dp)
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Text(
        text = "SHOOT CONFIRMED",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 1.2.sp
        ),
        color = EmeraldSuccess
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
        color = PureWhite
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onOpenApp,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBlack),
        modifier = Modifier.fillMaxWidth().height(52.dp)
      ) {
        Text(
          text = "VIEW SHOOT",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
          color = ObsidianBlack
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedButton(
        onClick = onFinish,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
        modifier = Modifier.fillMaxWidth().height(52.dp)
      ) {
        Text(text = "DONE", color = TextSecondary)
      }
    }
  }
}

@Composable
private fun TakenCard(name: String, onFinish: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color(0xF2141720),
    border = androidx.compose.foundation.BorderStroke(1.dp, RecRed.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .widthIn(max = 460.dp)
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      androidx.compose.material3.Icon(
        imageVector = Icons.Default.Close,
        contentDescription = null,
        tint = RecRed,
        modifier = Modifier.size(28.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Claimed by $name",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = PureWhite
      )
      Spacer(modifier = Modifier.height(16.dp))
      OutlinedButton(
        onClick = onFinish,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
        modifier = Modifier.fillMaxWidth().height(52.dp)
      ) {
        Text(text = "CLOSE", color = TextSecondary)
      }
    }
  }
}
