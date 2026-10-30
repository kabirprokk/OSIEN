package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.model.BookingStatus
import com.example.model.ShootCategory
import com.example.ui.components.CharacterState
import com.example.ui.components.FameGoLivingHeroCard
import com.example.ui.components.SoftCard
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoMotion
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import com.example.ui.theme.fameGoBreathe
import com.example.ui.theme.fameGoRise
import com.example.ui.theme.fameGoSettle
import java.util.Calendar

@Composable
fun ClientHomeScreen(
  onBookAShoot: (ShootCategory?) -> Unit,
  onOpenBooking: (String) -> Unit,
  onOpenLiveSearch: (String) -> Unit,
  onOpenChat: (String) -> Unit,
  onViewAllBookings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by FameGoRepository.currentUser.collectAsState()
  val bookings by FameGoRepository.bookings.collectAsState()

  // Smart Context Filter
  val searchingBooking = bookings.firstOrNull { it.status == BookingStatus.SEARCHING_CREW }
  val activeBooking = bookings.firstOrNull {
    it.status == BookingStatus.IN_PROGRESS || it.status == BookingStatus.CONFIRMED
  }
  val recentBooking = bookings.firstOrNull { it.status == BookingStatus.COMPLETED }

  val scrollState = rememberScrollState()

  // Dynamic greeting based on current time
  val greeting = remember {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    when {
      hour < 12 -> "Good morning"
      hour < 17 -> "Good afternoon"
      else -> "Good evening"
    }
  }

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
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // 1. Dynamic Greeting & Producer Context
      val firstName = currentUser.name.split(" ").firstOrNull() ?: "there"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "$greeting, $firstName",
            color = FameGoTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "Producer • Mumbai",
            color = FameGoTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 1.dp)
          )
        }

        // Live status orb indicator
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(FameGoGold)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "What are we shooting today?",
        color = FameGoWhite,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )

      Spacer(modifier = Modifier.height(20.dp))

      // 2. THE ONE LIVING SHOOT OBJECT (Situational Production Character + Motion)
      when {
        searchingBooking != null -> {
          // SEARCHING = BREATHE
          FameGoLivingHeroCard(
            state = CharacterState.FINDING_CREW,
            title = searchingBooking.shootTitle,
            subtitle = "${searchingBooking.venueName} • Checking availability",
            primaryActionLabel = "Open Radar",
            onPrimaryAction = { onOpenLiveSearch(searchingBooking.id) },
            modifier = Modifier
              .fillMaxWidth()
              .fameGoRise()
              .fameGoBreathe()
              .clickable { onOpenLiveSearch(searchingBooking.id) }
              .testTag("home_living_searching_card")
          )
        }

        activeBooking != null -> {
          val isInProgress = activeBooking.status == BookingStatus.IN_PROGRESS
          // IN_PROGRESS = ALIVE / CONFIRMED = SETTLE
          FameGoLivingHeroCard(
            state = if (isInProgress) CharacterState.SHOOT_IN_PROGRESS else CharacterState.CREW_CONFIRMED,
            title = activeBooking.shootTitle,
            subtitle = if (isInProgress)
              "Live on set • ${activeBooking.venueName} • Camera rolling"
            else
              "Call sheet locked • ${activeBooking.dateText} at ${activeBooking.timeText}",
            primaryActionLabel = if (isInProgress) "View Live Set" else "Call Sheet",
            onPrimaryAction = { onOpenBooking(activeBooking.id) },
            secondaryActionLabel = "Message",
            onSecondaryAction = { onOpenChat(activeBooking.id) },
            modifier = Modifier
              .fillMaxWidth()
              .fameGoRise()
              .fameGoSettle(trigger = activeBooking.status)
              .clickable { onOpenBooking(activeBooking.id) }
              .testTag("home_living_active_card")
          )
        }

        recentBooking != null -> {
          // SHOOT_COMPLETED
          FameGoLivingHeroCard(
            state = CharacterState.SHOOT_COMPLETED,
            title = recentBooking.shootTitle,
            subtitle = "Shoot done • Rate your crew",
            primaryActionLabel = "Book Again",
            onPrimaryAction = { onBookAShoot(recentBooking.category) },
            secondaryActionLabel = "View Summary",
            onSecondaryAction = { onOpenBooking(recentBooking.id) },
            modifier = Modifier
              .fillMaxWidth()
              .fameGoRise()
              .clickable { onOpenBooking(recentBooking.id) }
              .testTag("home_living_completed_card")
          )
        }

        else -> {
          // NOTHING_BOOKED
          FameGoLivingHeroCard(
            state = CharacterState.NOTHING_BOOKED,
            title = "Ready for your next shoot",
            subtitle = "Checked crew with their own gear. You pay only after someone accepts.",
            primaryActionLabel = "Book a Shoot",
            onPrimaryAction = { onBookAShoot(null) },
            modifier = Modifier
              .fillMaxWidth()
              .fameGoRise()
              .clickable { onBookAShoot(null) }
              .testTag("home_living_empty_card")
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 4. Activity Shortcut (if past bookings exist)
      if (bookings.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Shoot Archive",
            color = FameGoTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "View all (${bookings.size})",
            color = FameGoGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onViewAllBookings() }
          )
        }
      }

      // Generous bottom clearance for the FameGo rotating wheel
      Spacer(modifier = Modifier.height(130.dp))
    }
  }
}
