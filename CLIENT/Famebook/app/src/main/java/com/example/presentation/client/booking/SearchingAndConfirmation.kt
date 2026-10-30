package com.example.presentation.client.booking

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.User
import com.example.data.remote.RealtimeHub
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.FavoriteRepository
import com.example.presentation.components.FameBookTopBar
import com.example.presentation.components.FavoriteHeartButton
import com.example.presentation.components.PrimaryGoldButton
import com.example.presentation.components.SecondaryDarkButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LensCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Liquid-Glass Radar Finding Crew Screen.
 * Atmospheric orbital wave sweep, soft moving light, glowing core, zero countdown timeout.
 * Operates cleanly with real-time background dispatch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchingCrewScreen(
  bookingId: String,
  currentUser: User,
  bookingRepository: BookingRepository,
  onConfirmed: (Booking) -> Unit,
  onCancelled: () -> Unit,
  onDismissToHome: () -> Unit
) {
  // Push-first refresh: a crew accept on another device flips this screen
  // to CONFIRMED within a second; the timer is only a socket fallback.
  var refreshTick by remember { mutableIntStateOf(0) }
  LaunchedEffect(bookingId) {
    launch {
      RealtimeHub.bookingsChanged.collect { refreshTick++ }
    }
    while (true) {
      delay(15000)
      refreshTick++
    }
  }
  val bookingFlow = remember(bookingId, refreshTick) { bookingRepository.getBooking(bookingId) }
  val booking by bookingFlow.collectAsState(initial = null)

  var statusMessageIndex by remember { mutableIntStateOf(0) }
  val statusMessages = listOf(
    "Finding free crew near you...",
    "Sending your shoot to the crew...",
    "Matching the right gear for your brief...",
    "Waiting for a crew member to pick this up..."
  )

  LaunchedEffect(Unit) {
    while (true) {
      delay(4000)
      statusMessageIndex = (statusMessageIndex + 1) % statusMessages.size
    }
  }

  // Seamless automatic transition when booking is claimed by any crew member
  LaunchedEffect(booking?.status) {
    if (booking?.status == BookingStatus.CONFIRMED) {
      onConfirmed(booking!!)
    }
  }

  Scaffold(
    containerColor = ObsidianBlack,
    topBar = {
      FameBookTopBar(
        title = "Production Dispatch",
        currentUser = currentUser,
        onBackClick = onDismissToHome
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Center Atmospheric Radar Animation & Status
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          CinematicOrbitalPulse()

          Spacer(modifier = Modifier.height(32.dp))

          Text(
            text = "FINDING YOUR CREW",
            style = MaterialTheme.typography.displaySmall.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = (-0.5).sp
            ),
            color = PureWhite
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = statusMessages[statusMessageIndex],
            style = MaterialTheme.typography.bodyLarge,
            color = AmberGold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )

          if (booking != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x1AFFFFFF),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
              Text(
                text = "${booking!!.title} • ${booking!!.location.name}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }
        }

        // Production Actions: Return to Dashboard & Cancel Option
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF2141722),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x33FFFFFF)),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(
                      Color(0x1FFFFFFF),
                      Color(0x00000000)
                    )
                  )
                )
                .padding(20.dp)
            ) {
              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(AmberGold)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "DISPATCH ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.2.sp
                    ),
                    color = AmberGold
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                  text = "Your shoot brief is currently broadcast to available crew in Mumbai. You can return to home while the search continues.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryGoldButton(
                  text = "RETURN TO DASHBOARD",
                  onClick = onDismissToHome,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("searching_return_home_btn")
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          TextButton(
            onClick = onCancelled,
            modifier = Modifier.testTag("cancel_search_button")
          ) {
            Text(
              text = "Cancel Shoot Request",
              color = TextSecondary,
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
      }
    }
  }
}

/**
 * Liquid-Glass Radar Orbital Field with soft moving light & glowing center core
 */
@Composable
fun CinematicOrbitalPulse() {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")

  // One lightweight pulse keeps the radar alive without driving four animated
  // values and several expensive glow layers on lower-end devices.
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(2600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "radar_pulse"
  )

  Box(
    modifier = Modifier.size(210.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = this.center
      val maxRadius = size.minDimension / 2

      drawCircle(
        color = AmberGold.copy(alpha = (1f - pulse) * 0.4f),
        radius = maxRadius * pulse,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      drawCircle(
        color = LensCyan.copy(alpha = 0.12f),
        radius = maxRadius * 0.67f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
      )

      drawCircle(
        color = AmberGold.copy(alpha = 0.08f),
        radius = maxRadius * 0.38f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
      )
    }

    // Glowing Core Shutter in Liquid Glass
    Surface(
      shape = CircleShape,
      color = Color(0xF2161924),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberGold.copy(alpha = 0.8f)),
      shadowElevation = 16.dp,
      modifier = Modifier.size(86.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(
                AmberGold.copy(alpha = 0.2f),
                Color(0x00000000)
              )
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Videocam,
          contentDescription = null,
          tint = AmberGold,
          modifier = Modifier.size(38.dp)
        )
      }
    }
  }
}

/**
 * Editorial Confirmation Screen
 */
@Composable
fun ConfirmationScreen(
  booking: Booking,
  currentUser: User,
  favoriteRepository: FavoriteRepository,
  onOpenChat: () -> Unit,
  onViewBookingDetails: () -> Unit,
  onBackToHome: () -> Unit
) {
  val context = LocalContext.current
  val crewName = booking.assignedCrewName ?: "Assigned Specialist"
  val crewRole = booking.assignedCrewRole ?: "Production Professional"

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 24.dp),
    contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Confirmation Header
    item {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(EmeraldSuccess.copy(alpha = 0.15f))
          .border(2.dp, EmeraldSuccess, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = EmeraldSuccess,
          modifier = Modifier.size(38.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "CREW CONFIRMED",
        style = MaterialTheme.typography.displaySmall.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = (-0.5).sp
        ),
        color = PureWhite
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Your crew is booked. Say hello and lock the plan.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Spacer(modifier = Modifier.height(30.dp))
    }

    // Editorial Crew Card in Liquid Glass styling
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF2141722),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x33FFFFFF)),
        shadowElevation = 14.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("editorial_crew_card")
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color(0xFF1E2332),
                  Color(0xFF141722),
                  Color(0xFF0C0E15)
                )
              )
            )
            .padding(22.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(66.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF1A1D2A))
                  .border(1.5.dp, AmberGold, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = AmberGold,
                  modifier = Modifier.size(36.dp)
                )
              }

              Spacer(modifier = Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = crewName.uppercase(),
                  style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                  color = PureWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = crewRole,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = AmberGold
                )
                Text(
                  text = "Verified FameBros Crew • Mumbai",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary
                )
              }

              FavoriteHeartButton(
                clientId = currentUser.id,
                crewUserId = booking.assignedCrewId,
                favoriteRepository = favoriteRepository
              )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color(0x22FFFFFF))
            Spacer(modifier = Modifier.height(18.dp))

            // Production details
            Row(verticalAlignment = Alignment.Top) {
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "${booking.date} • Call Time ${booking.startTime}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = PureWhite
                )
                Text(
                  text = "${booking.location.name}, ${booking.location.address}",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary
                )
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Instant Actions (Call & Chat) with tactile styling
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              SecondaryDarkButton(
                text = "CALL CREW",
                onClick = {
                  val phone = booking.assignedCrewPhone
                  if (!phone.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                  }
                },
                modifier = Modifier.weight(1f)
              )

              PrimaryGoldButton(
                text = "CHAT",
                onClick = onOpenChat,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }

    // View Specs & Back to Home buttons
    item {
      PrimaryGoldButton(
        text = "VIEW SHOOT SPECS",
        onClick = onViewBookingDetails,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(10.dp))

      TextButton(
        onClick = onBackToHome,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Return to Home",
          color = TextSecondary,
          style = MaterialTheme.typography.bodyLarge
        )
      }
    }
  }
}
