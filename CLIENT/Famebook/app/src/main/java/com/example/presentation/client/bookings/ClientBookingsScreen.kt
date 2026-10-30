package com.example.presentation.client.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.BookingStatus
import com.example.domain.model.User
import com.example.domain.repository.BookingRepository
import com.example.presentation.components.CardEntrance
import com.example.presentation.components.CleanBookingItem
import com.example.presentation.components.EmptyState
import com.example.presentation.components.SectionHeader
import com.example.presentation.components.UpcomingBookingCard
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientBookingsScreen(
  currentUser: User,
  bookingRepository: BookingRepository,
  onBookingClick: (String) -> Unit,
  onOpenChat: (String) -> Unit,
  onBookShootClick: () -> Unit
) {
  // Poll while visible so a crew accept on another device updates statuses.
  // Pull-to-refresh triggers the same reload on demand.
  var refreshTick by remember { mutableIntStateOf(0) }
  var isRefreshing by remember { mutableStateOf(false) }
  LaunchedEffect(currentUser.id) {
    while (true) {
      delay(20000)
      refreshTick++
    }
  }
  LaunchedEffect(refreshTick) {
    delay(1200)
    isRefreshing = false
  }
  val clientBookings by remember(currentUser.id, refreshTick) {
    bookingRepository.getClientBookings(currentUser.id)
  }.collectAsState(initial = emptyList())

  val activeShoots = clientBookings.filter {
    it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.IN_PROGRESS || it.status == BookingStatus.SEARCHING_CREW
  }

  val pastShoots = clientBookings.filter {
    it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED
  }

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
      isRefreshing = true
      refreshTick++
    },
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
  ) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
  ) {
    item {
      SectionHeader(title = "MY SHOOTS")
      Spacer(modifier = Modifier.height(8.dp))
    }

    if (clientBookings.isEmpty()) {
      item {
        EmptyState(
          title = "NO SHOOTS YET",
          message = "Tell us what you're shooting and we'll find the right crew for it.",
          actionText = "BOOK A SHOOT",
          onActionClick = onBookShootClick
        )
      }
    } else {
      if (activeShoots.isNotEmpty()) {
        item {
          Text(
            text = "ACTIVE & UPCOMING",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = com.example.ui.theme.AmberGold,
            modifier = Modifier.padding(vertical = 6.dp)
          )
        }

        itemsIndexed(activeShoots, key = { _, booking -> booking.id }) { index, booking ->
          CardEntrance(index = index) {
            UpcomingBookingCard(
              booking = booking,
              onClick = { onBookingClick(booking.id) },
              onOpenChat = onOpenChat
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
        }
      }

      if (pastShoots.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "PAST PRODUCTIONS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = com.example.ui.theme.TextMuted,
            modifier = Modifier.padding(vertical = 6.dp)
          )
        }

        itemsIndexed(pastShoots, key = { _, booking -> booking.id }) { index, booking ->
          CardEntrance(index = index) {
            CleanBookingItem(
              booking = booking,
              onClick = { onBookingClick(booking.id) }
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
  }
}
