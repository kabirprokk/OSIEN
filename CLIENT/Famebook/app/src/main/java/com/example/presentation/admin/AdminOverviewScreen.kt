package com.example.presentation.admin

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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.CrewRepository
import com.example.domain.repository.UserRepository
import com.example.presentation.components.FameBookTopBar
import com.example.presentation.components.alivePulse
import com.example.presentation.components.rememberAlivePulse
import kotlinx.coroutines.launch
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOverviewScreen(
  currentUser: User,
  bookingRepository: BookingRepository,
  crewRepository: CrewRepository,
  userRepository: UserRepository,
  onBookingClick: (String) -> Unit
) {
  val allBookings by bookingRepository.allBookings.collectAsState()
  val crewProfiles by crewRepository.crewProfiles.collectAsState()

  val activeShoots = allBookings.filter {
    it.status == BookingStatus.CONFIRMED ||
        it.status == BookingStatus.IN_PROGRESS ||
        it.status == BookingStatus.SEARCHING_CREW
  }

  val scope = rememberCoroutineScope()
  var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
  var userQuery by remember { mutableStateOf("") }
  var roleMessage by remember { mutableStateOf<String?>(null) }
  var assigningUserId by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    allUsers = userRepository.getAllUsers()
  }

  val pulse by rememberAlivePulse()

  val filteredUsers = remember(allUsers, userQuery) {
    if (userQuery.isBlank()) allUsers
    else allUsers.filter {
      it.email.contains(userQuery, ignoreCase = true) ||
        it.name.contains(userQuery, ignoreCase = true)
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 20.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "FAMEBROS HQ OPERATIONS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = AmberGold
          )
          Text(
            text = "Studio Control Room",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = PureWhite
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AmberGoldContainer)
            .alivePulse(pulse)
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "LIVE DISPATCH",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = AmberGold
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Production Metrics
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          AdminMetric(label = "ACTIVE PIPELINE", value = "${activeShoots.size}")
          AdminMetric(label = "CREW ROSTER", value = "${crewProfiles.size}")
          AdminMetric(label = "CREW AVAILABLE", value = "${crewProfiles.count { it.isAvailable }}")
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }

    // Admin-only role management
    item {
      Text(
        text = "USER ROLE MANAGEMENT",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        ),
        color = TextMuted
      )
      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth().testTag("admin_role_management")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          OutlinedTextField(
            value = userQuery,
            onValueChange = { userQuery = it },
            label = { Text("Search by name or email", color = TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("admin_user_search"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = AmberGold,
              unfocusedBorderColor = DarkBorder
            )
          )
          if (roleMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = roleMessage!!, style = MaterialTheme.typography.bodySmall, color = AmberGold)
          }
          Spacer(modifier = Modifier.height(8.dp))
          filteredUsers.take(20).forEach { user ->
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("admin_user_${user.id}")
            ) {
              Text(text = user.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)
              Text(text = "${user.email} • ${user.role.name}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.entries.forEach { role ->
                  val selected = user.role == role
                  Button(
                    onClick = {
                      assigningUserId = user.id
                      roleMessage = null
                      scope.launch {
                        val result = userRepository.updateUserRole(user.id, role)
                        assigningUserId = null
                        if (result.isSuccess) {
                          allUsers = userRepository.getAllUsers()
                          roleMessage = "${user.email} is now ${role.name}"
                        } else {
                          roleMessage = result.exceptionOrNull()?.message ?: "Role update failed"
                        }
                      }
                    },
                    enabled = !selected && assigningUserId != user.id,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (selected) AmberGold else DarkElevated,
                      contentColor = if (selected) ObsidianBlack else PureWhite
                    )
                  ) {
                    Text(text = role.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                  }
                }
              }
              HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(top = 8.dp))
            }
          }
          if (filteredUsers.isEmpty()) {
            Text(text = "No users found. They appear after creating an account.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }

    // Live Bookings Stream
    item {
      Text(
        text = "LIVE PRODUCTION PIPELINE",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        ),
        color = TextMuted
      )
      Spacer(modifier = Modifier.height(10.dp))
    }

    if (allBookings.isEmpty()) {
      item {
        Text(text = "No bookings found", color = TextSecondary)
      }
    } else {
      items(allBookings) { booking ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = DarkCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onBookingClick(booking.id) }
            .testTag("admin_booking_${booking.id}")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              StatusChip(status = booking.status)
              Text(
                text = booking.id,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AmberGold
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = booking.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PureWhite
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
              text = "Client: ${booking.clientName} • Crew: ${booking.assignedCrewName ?: "Pending Assignment"}",
              style = MaterialTheme.typography.bodySmall,
              color = if (booking.assignedCrewName != null) LensCyan else AmberGold
            )

            Text(
              text = "${booking.date} at ${booking.startTime} (${booking.durationHours}h) • ${booking.location.name}",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
          }
        }
      }
    }

    // Crew Roster
    item {
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = "FAMEBROS VERIFIED CREW ROSTER",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        ),
        color = TextMuted
      )
      Spacer(modifier = Modifier.height(10.dp))
    }

    items(crewProfiles) { crew ->
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(DarkSurface),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = AmberGold,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = crew.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "• ${crew.primaryRole.title}",
                style = MaterialTheme.typography.bodySmall,
                color = AmberGold
              )
            }
            Text(
              text = crew.gearSummary,
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              maxLines = 1
            )
          }

          Box(
            modifier = Modifier
              .clip(CircleShape)
              .background(if (crew.isAvailable) EmeraldSuccess.copy(alpha = 0.2f) else DarkSurface)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = if (crew.isAvailable) "ONLINE" else "OFFLINE",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (crew.isAvailable) EmeraldSuccess else TextMuted
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminMetric(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = TextMuted
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
      color = AmberGold
    )
  }
}
