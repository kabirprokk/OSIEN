package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.ui.components.SoftCard
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoLiveRed
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import com.example.ui.theme.fameGoRise

/**
 * Producer profile — every row works. Dead placeholders (billing, storage,
 * saved lists, insurance) were removed; what remains navigates or explains.
 */
@Composable
fun ClientProfileScreen(
  onOpenSupport: () -> Unit,
  onOpenBookings: () -> Unit,
  onLogout: () -> Unit,
  onEditProfile: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val currentUser by FameGoRepository.currentUser.collectAsState()
  val bookings by FameGoRepository.bookings.collectAsState()
  val scrollState = rememberScrollState()
  var showRules by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp)
        .fameGoRise()
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Producer Profile",
        color = FameGoWhite,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Avatar & Identity Card
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(FameGoCard)
            .border(1.5.dp, FameGoGold.copy(alpha = 0.6f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = currentUser.avatarInitials.ifBlank {
              currentUser.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                .take(2).joinToString("").ifBlank { "FG" }
            },
            color = FameGoGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = currentUser.name.ifBlank { "Producer" },
              color = FameGoWhite,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(FameGoGoldContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "PRODUCER",
                color = FameGoGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Text(
            text = currentUser.companyName.ifBlank { "Independent Creator" },
            color = FameGoTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
          )

          Text(
            text = "Account Verified",
            color = FameGoSuccessGreen,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Stats strip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ProfileStatCard(
          value = bookings.size.toString(),
          label = "Total shoots",
          modifier = Modifier.weight(1f)
        )
        ProfileStatCard(
          value = bookings.count {
            it.status == com.example.model.BookingStatus.COMPLETED
          }.toString(),
          label = "Completed",
          modifier = Modifier.weight(1f)
        )
        ProfileStatCard(
          value = bookings.count {
            it.status == com.example.model.BookingStatus.SEARCHING_CREW ||
              it.status == com.example.model.BookingStatus.CONFIRMED
          }.toString(),
          label = "Active",
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Account
      Text(
        text = "Account",
        color = FameGoTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(8.dp))

      SoftCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
          ProfileRow(
            label = "Edit profile",
            value = currentUser.name.ifBlank { "Set your details" },
            onClick = onEditProfile
          )
          ProfileRow(
            label = "Email",
            value = currentUser.email.ifBlank { "Not set" },
            onClick = null
          )
          ProfileRow(
            label = "Phone",
            value = currentUser.phone.ifBlank { "Not set" },
            onClick = null
          )
          ProfileRow(
            label = "My bookings",
            value = if (bookings.isEmpty()) "No shoots yet" else "${bookings.size} shoots",
            onClick = onOpenBookings
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Help and safety
      Text(
        text = "Help and safety",
        color = FameGoTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(8.dp))

      SoftCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
          ProfileRow(
            label = "Help desk",
            value = "Talk to us",
            onClick = onOpenSupport
          )
          ProfileRow(
            label = "House rules",
            value = "Read before you book",
            onClick = { showRules = true }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Sign out — asks for confirmation in MainActivity.
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = FameGoCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, FameGoLiveRed.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onLogout() }
          .testTag("profile_logout_button")
      ) {
        Text(
          text = "Sign out",
          color = FameGoLiveRed,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(vertical = 15.dp),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(130.dp))
    }

    if (showRules) {
      com.example.ui.components.HouseRulesDialog(onDismiss = { showRules = false })
    }
  }
}

@Composable
private fun ProfileStatCard(value: String, label: String, modifier: Modifier = Modifier) {
  SoftCard(modifier = modifier) {
    Column(
      modifier = Modifier.padding(vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = value, color = FameGoGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      Text(text = label, color = FameGoTextMuted, fontSize = 11.sp)
    }
  }
}

@Composable
private fun ProfileRow(
  label: String,
  value: String,
  onClick: (() -> Unit)?
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = onClick != null) { onClick?.invoke() }
      .padding(vertical = 13.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      color = FameGoWhite,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = value,
        color = FameGoTextMuted,
        fontSize = 13.sp
      )
      if (onClick != null) {
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = null,
          tint = FameGoTextMuted,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
