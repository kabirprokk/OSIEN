package com.example.presentation.common

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.remote.RealtimeHub
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.FavoriteRepository
import com.example.presentation.components.FameBookTopBar
import com.example.presentation.components.FavoriteHeartButton
import com.example.presentation.components.PrimaryGoldButton
import com.example.presentation.components.SecondaryDarkButton
import com.example.presentation.components.StatusChip
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LensCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
  bookingId: String,
  currentUser: User,
  bookingRepository: BookingRepository,
  favoriteRepository: FavoriteRepository,
  onBackClick: () -> Unit,
  onOpenChat: (String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  // Push-first refresh; the timer is only a socket fallback.
  var refreshTick by remember { mutableIntStateOf(0) }
  LaunchedEffect(bookingId) {
    launch {
      RealtimeHub.bookingsChanged.collect { refreshTick++ }
    }
    while (true) {
      delay(30000)
      refreshTick++
    }
  }
  val bookingFlow = remember(bookingId, refreshTick) { bookingRepository.getBooking(bookingId) }
  val booking by bookingFlow.collectAsState(initial = null)

  Scaffold(
    containerColor = ObsidianBlack,
    topBar = {
      FameBookTopBar(
        title = booking?.title ?: "Booking Specs",
        subtitle = "ID: $bookingId",
        currentUser = currentUser,
        onBackClick = onBackClick
      )
    }
  ) { innerPadding ->
    if (booking == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "Loading booking...", color = TextSecondary)
      }
    } else {
      val b = booking!!
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
      ) {
        // Status Bar & Booking ID
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            StatusChip(status = b.status)
            Text(
              text = b.id,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = AmberGold
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = b.title,
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.5).sp
            ),
            color = PureWhite
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "${b.shootType.title} • ${b.shootType.subtitle}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(20.dp))
        }

        // Assigned Crew or Client Party Card
        item {
          if (b.status == BookingStatus.CONFIRMED || b.status == BookingStatus.COMPLETED || b.status == BookingStatus.IN_PROGRESS) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = DarkCard,
              border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = if (currentUser.role == UserRole.CREW) "CLIENT INFORMATION" else "ASSIGNED CREW MEMBER",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp
                    ),
                    color = AmberGold
                  )
                  if (currentUser.role == UserRole.CLIENT) {
                    FavoriteHeartButton(
                      clientId = currentUser.id,
                      crewUserId = b.assignedCrewId,
                      favoriteRepository = favoriteRepository
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(48.dp)
                      .clip(CircleShape)
                      .background(DarkElevated)
                      .border(1.dp, AmberGold, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      tint = AmberGold,
                      modifier = Modifier.size(26.dp)
                    )
                  }

                  Spacer(modifier = Modifier.width(14.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = if (currentUser.role == UserRole.CREW) b.clientName else (b.assignedCrewName ?: "Pending Assignment"),
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = PureWhite
                    )
                    Text(
                      text = if (currentUser.role == UserRole.CREW) (b.clientCompany ?: "Client") else (b.assignedCrewRole ?: "FameBros Specialist"),
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Quick Communication Row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  val targetPhone = if (currentUser.role == UserRole.CREW) b.clientPhone else b.assignedCrewPhone

                  OutlinedButton(
                    onClick = {
                      targetPhone?.takeIf { it.isNotBlank() }?.let {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it")))
                      }
                    },
                    enabled = !targetPhone.isNullOrBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Call,
                      contentDescription = null,
                      tint = PureWhite,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Call", color = PureWhite, style = MaterialTheme.typography.labelMedium)
                  }

                  Button(
                    onClick = { onOpenChat(b.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = AmberGoldContainer,
                      contentColor = AmberGold
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Chat,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Chat", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
          }
        }

        // Schedule & Location Card
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Text(
                text = "PRODUCTION SCHEDULE & VENUE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = TextMuted
              )
              Spacer(modifier = Modifier.height(12.dp))

              Row(verticalAlignment = Alignment.Top) {
                Icon(
                  imageVector = Icons.Default.Schedule,
                  contentDescription = null,
                  tint = AmberGold,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "${b.date} • Call Time ${b.startTime}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = PureWhite
                  )
                  Text(
                    text = "Planned Duration: ${b.durationHours} hours on set",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Row(verticalAlignment = Alignment.Top) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = AmberGold,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = b.location.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = PureWhite
                  )
                  Text(
                    text = b.location.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                  )
                  if (b.location.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "Note: ${b.location.notes}",
                      style = MaterialTheme.typography.bodySmall,
                      color = LensCyan
                    )
                  }
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Creative Brief & Requirements
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Text(
                text = "CREATIVE BRIEF & CREW SPECS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = TextMuted
              )
              Spacer(modifier = Modifier.height(10.dp))

              if (b.description.isNotBlank()) {
                Text(
                  text = b.description,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextPrimary,
                  lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
              }

              if (b.requirements.isNotEmpty()) {
                Text(
                  text = "Assigned Crew Roles Required:",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = AmberGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                b.requirements.forEach { req ->
                  Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AmberGold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "${req.count}x ${req.role.title} (${req.role.defaultEquipment})",
                      style = MaterialTheme.typography.bodySmall,
                      color = PureWhite
                    )
                  }
                }
                Spacer(modifier = Modifier.height(12.dp))
              }

              if (b.specialInstructions.isNotBlank()) {
                Text(
                  text = "Special Instructions:",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = AmberGold
                )
                Text(
                  text = b.specialInstructions,
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(24.dp))
        }

        // Production Lifecycle Controls
        item {
          if (b.status == BookingStatus.SEARCHING_CREW) {
            SecondaryDarkButton(
              text = "CANCEL SHOOT REQUEST",
              onClick = {
                scope.launch {
                  bookingRepository.cancelBooking(b.id)
                  onBackClick()
                }
              }
            )
          } else if (b.status == BookingStatus.CONFIRMED && currentUser.role == UserRole.CREW) {
            PrimaryGoldButton(
              text = "START PRODUCTION",
              onClick = {
                scope.launch {
                  bookingRepository.updateBookingStatus(b.id, BookingStatus.IN_PROGRESS)
                }
              }
            )
          } else if (b.status == BookingStatus.IN_PROGRESS) {
            PrimaryGoldButton(
              text = "MARK SHOOT COMPLETE",
              onClick = {
                scope.launch {
                  bookingRepository.updateBookingStatus(b.id, BookingStatus.COMPLETED)
                }
              }
            )
          }
        }
      }
    }
  }
}
