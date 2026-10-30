package com.example.presentation.crew.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.CrewDispatchService
import com.example.domain.model.Booking
import com.example.domain.model.User
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.CrewRepository
import com.example.presentation.components.SectionHeader
import com.example.presentation.components.SimpleStatusBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CrewHomeScreen(
  currentUser: User,
  bookingRepository: BookingRepository,
  crewRepository: CrewRepository,
  snackbarHostState: SnackbarHostState,
  onBookingClick: (String) -> Unit,
  onOpenChat: (String) -> Unit
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  val crewProfile by crewRepository.getCrewProfileByUserId(currentUser.id).collectAsState(initial = null)
  val isAvailable = crewProfile?.isAvailable ?: false
  var isUpdatingAvailability by remember { mutableStateOf(false) }

  val incomingRequests by bookingRepository.getIncomingRequestsForCrew(currentUser.id).collectAsState(initial = emptyList())
  val assignedBookings by bookingRepository.getCrewBookings(currentUser.id).collectAsState(initial = emptyList())

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 20.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
  ) {
    // 1. TOP: Large Availability Toggle (Section 9)
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("crew_availability_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isAvailable) "AVAILABLE FOR SHOOTS" else "OFF-DUTY",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isAvailable) EmeraldSuccess else TextSecondary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = if (isAvailable) "Ready for instant dispatch in Mumbai" else "Tap toggle to receive requests",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }

            Switch(
              checked = isAvailable,
              enabled = !isUpdatingAvailability,
              onCheckedChange = { newStatus ->
                if (isUpdatingAvailability) return@Switch
                isUpdatingAvailability = true
                scope.launch {
                  val result = try {
                    crewRepository.setAvailability(currentUser.id, newStatus)
                  } catch (e: Exception) {
                    Result.failure(e)
                  }
                  if (result.isFailure) {
                    val reason = result.exceptionOrNull()?.message?.take(120)
                    snackbarHostState.showSnackbar(
                      if (reason.isNullOrBlank()) "Could not update availability. Check connection and retry."
                      else "Availability failed: $reason"
                    )
                  }
                  isUpdatingAvailability = false
                }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = EmeraldSuccess,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkElevated
              ),
              modifier = Modifier.testTag("crew_availability_toggle")
            )
          }

          if (isAvailable) {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DarkBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(EmeraldSuccess)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = AmberGold,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Dispatch Active • Real-time alerts enabled",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                  color = AmberGold
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }

    // 2. NEW REQUEST (Cinematic incoming request card, never expires)
    if (incomingRequests.isNotEmpty()) {
      item {
        SectionHeader(title = "NEW REQUEST")
        Spacer(modifier = Modifier.height(8.dp))
      }

      items(incomingRequests) { req ->
        CinematicIncomingRequestCard(
          booking = req,
          onAccept = {
            scope.launch {
              val result = bookingRepository.acceptBooking(req.id, currentUser.id)
              if (result.isSuccess) {
                snackbarHostState.showSnackbar("Shoot Claimed! Confirmed with ${req.clientName}.")
              } else {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Unable to accept request.")
              }
            }
          },
          onDecline = {
            scope.launch {
              bookingRepository.declineBooking(req.id, currentUser.id)
              snackbarHostState.showSnackbar("Request declined.")
            }
          }
        )
        Spacer(modifier = Modifier.height(18.dp))
      }
    } else {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = DarkCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (isAvailable) "Standing by for requests" else "You are currently off-duty",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PureWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isAvailable) "Incoming FameBros shoot requests will alert you here." else "Turn on your availability above to get alerted.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }

    // 3. SCHEDULE / ASSIGNED SHOOTS
    item {
      SectionHeader(title = "ASSIGNED SHOOTS")
      Spacer(modifier = Modifier.height(8.dp))
    }

    if (assignedBookings.isEmpty()) {
      item {
        Text(
          text = "No confirmed shoots assigned yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          modifier = Modifier.padding(vertical = 12.dp)
        )
      }
    } else {
      items(assignedBookings) { booking ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = DarkCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onBookingClick(booking.id) }
            .testTag("crew_assigned_booking_${booking.id}")
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              SimpleStatusBadge(status = booking.status)
              Text(
                text = booking.date.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AmberGold
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = booking.title,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = PureWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "${booking.startTime} (${booking.durationHours}h) • ${booking.location.name}",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary
            )

            Text(
              text = "Client: ${booking.clientName} (${booking.clientCompany ?: "Direct"})",
              style = MaterialTheme.typography.bodySmall,
              color = AmberGold
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DarkBorder)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = {
                  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking.clientPhone}"))
                  context.startActivity(intent)
                },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Call,
                  contentDescription = null,
                  tint = PureWhite,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Call Client", color = PureWhite, style = MaterialTheme.typography.labelMedium)
              }

              Spacer(modifier = Modifier.width(10.dp))

              Button(
                onClick = { onOpenChat(booking.id) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = AmberGoldContainer,
                  contentColor = AmberGold
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Chat,
                  contentDescription = null,
                  tint = AmberGold,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Chat", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Cinematic Incoming Request Card (No timer, no countdown)
 */
@Composable
private fun CinematicIncomingRequestCard(
  booking: Booking,
  onAccept: () -> Unit,
  onDecline: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = DarkElevated,
    border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberGold.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("cinematic_incoming_request_card")
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
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
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = AmberGold
          )
        }

        Text(
          text = "AVAILABLE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = EmeraldSuccess
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = booking.title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
        color = PureWhite
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "${booking.date} • ${booking.startTime} • ${booking.durationHours} hours",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = PureWhite
      )

      Text(
        text = "${booking.location.name} (${booking.location.address})",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Role: Lead Cinematographer (4K Camera Package)",
        style = MaterialTheme.typography.bodySmall,
        color = AmberGold
      )

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onDecline,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
          modifier = Modifier
            .weight(0.4f)
            .height(50.dp)
            .testTag("incoming_request_decline_button")
        ) {
          Text(
            text = "DECLINE",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
          )
        }

        Button(
          onClick = onAccept,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = AmberGold,
            contentColor = ObsidianBlack
          ),
          modifier = Modifier
            .weight(0.6f)
            .height(50.dp)
            .testTag("incoming_request_accept_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "ACCEPT",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
            )
          }
        }
      }
    }
  }
}
