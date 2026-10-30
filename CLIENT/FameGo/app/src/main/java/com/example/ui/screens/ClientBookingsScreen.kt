package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.model.BookingStatus
import com.example.model.ShootCategory
import com.example.ui.components.CharacterState
import com.example.ui.components.FameGoCharacterIllustration
import com.example.ui.components.FlowPill
import com.example.ui.components.FlowPillState
import com.example.ui.components.FameGoPressable
import com.example.ui.components.FameGoSheet
import com.example.ui.components.rubberBand
import com.example.ui.components.LiveOrb
import com.example.ui.components.SoftCard
import com.example.ui.components.VengeanceGooeySearch
import com.example.ui.theme.fameGoBreathe
import com.example.ui.theme.fameGoRise
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoLiveRed
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoSurface
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientBookingsScreen(
  onOpenBooking: (String) -> Unit,
  onBookAgain: (ShootCategory) -> Unit,
  onNewBooking: () -> Unit,
  onOpenChat: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val bookings by FameGoRepository.bookings.collectAsState()
  var selectedTab by remember { mutableStateOf("All") }
  var searchQuery by remember { mutableStateOf("") }
  var pendingDeleteId by remember { mutableStateOf<String?>(null) }
  var sheetBookingId by remember { mutableStateOf<String?>(null) }
  var isRefreshing by remember { mutableStateOf(false) }
  val refreshScope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val sfx = LocalContext.current.applicationContext

  val tabFiltered = when (selectedTab) {
    "Active" -> bookings.filter { it.status != BookingStatus.COMPLETED && it.status != BookingStatus.CANCELLED }
    "Completed" -> bookings.filter { it.status == BookingStatus.COMPLETED }
    else -> bookings
  }
  val filteredBookings = if (searchQuery.isBlank()) tabFiltered
    else tabFiltered.filter {
      it.title.contains(searchQuery, ignoreCase = true) ||
        it.location.address.contains(searchQuery, ignoreCase = true)
    }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp)
        .fameGoRise()
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Your Shoots",
        color = FameGoWhite,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Minimal Tab Filter
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All", "Active", "Completed").forEach { tab ->
          val isSelected = selectedTab == tab
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) FameGoGoldContainer else FameGoSurface,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSelected) FameGoGold else FameGoBorderSubtle
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .clickable { selectedTab = tab }
              .testTag("bookings_tab_${tab.lowercase()}")
          ) {
            Text(
              text = tab,
              color = if (isSelected) FameGoGold else FameGoTextSecondary,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Morphing search — filters shoots by title or location.
      VengeanceGooeySearch(
        items = bookings.map { it.title },
        buttonLabel = "Search shoots",
        placeholder = "Shoot title or place...",
        onSelect = { title ->
          bookings.firstOrNull { it.title == title }?.let { onOpenBooking(it.id) }
        },
        onSearch = { q ->
          bookings
            .filter {
              it.title.contains(q, ignoreCase = true) ||
                it.location.address.contains(q, ignoreCase = true)
            }
            .map { it.title }
        },
        onQueryChange = { searchQuery = it },
        modifier = Modifier.fillMaxWidth(),
        testTag = "bookings_search"
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Content or Smart Empty State
      if (filteredBookings.isEmpty()) {
        // 24. SMART EMPTY STATE
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FameGoCharacterIllustration(
              state = CharacterState.NOTHING_BOOKED,
              size = 100.dp,
              modifier = Modifier.fameGoBreathe()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "No shoots booked yet",
              color = FameGoWhite,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Book your first crew in a few taps.",
              color = FameGoTextMuted,
              fontSize = 14.sp,
              modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            FlowPill(
              state = FlowPillState.CUSTOM,
              customText = "Book a shoot",
              onClick = onNewBooking,
              testTag = "empty_bookings_flow_pill"
            )
          }
        }
      } else {
        // Pull down anywhere on the list for a fresh server sync.
        PullToRefreshBox(
          isRefreshing = isRefreshing,
          onRefresh = {
            isRefreshing = true
            com.example.data.FameGoSfx.pop(sfx)
            refreshScope.launch {
              FameGoRepository.refreshNow()
              isRefreshing = false
            }
          },
          modifier = Modifier.weight(1f)
        ) {
        LazyColumn(
          modifier = Modifier.fillMaxSize().rubberBand(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredBookings, key = { it.id }) { booking ->
            // Swipe right → message crew · swipe left → delete (with confirm).
            val dismissState = rememberSwipeToDismissBoxState(
              confirmValueChange = { value ->
                when (value) {
                  SwipeToDismissBoxValue.StartToEnd -> {
                    com.example.data.FameGoSfx.tap(sfx)
                    onOpenChat(booking.id)
                    false
                  }
                  SwipeToDismissBoxValue.EndToStart -> {
                    com.example.data.FameGoSfx.tap(sfx)
                    pendingDeleteId = booking.id
                    false
                  }
                  SwipeToDismissBoxValue.Settled -> false
                }
              }
            )
            SwipeToDismissBox(
              state = dismissState,
              enableDismissFromStartToEnd = true,
              enableDismissFromEndToStart = true,
              backgroundContent = {
                val direction = dismissState.dismissDirection
                val bg = when (direction) {
                  SwipeToDismissBoxValue.StartToEnd -> FameGoAccentCyan.copy(alpha = 0.22f)
                  SwipeToDismissBoxValue.EndToStart -> FameGoLiveRed.copy(alpha = 0.22f)
                  SwipeToDismissBoxValue.Settled -> Color.Transparent
                }
                val label = when (direction) {
                  SwipeToDismissBoxValue.StartToEnd -> "Message"
                  SwipeToDismissBoxValue.EndToStart -> "Delete"
                  SwipeToDismissBoxValue.Settled -> ""
                }
                val tint = when (direction) {
                  SwipeToDismissBoxValue.StartToEnd -> FameGoAccentCyan
                  SwipeToDismissBoxValue.EndToStart -> FameGoLiveRed
                  SwipeToDismissBoxValue.Settled -> Color.Transparent
                }
                val icon = when (direction) {
                  SwipeToDismissBoxValue.StartToEnd -> Icons.AutoMirrored.Filled.Chat
                  else -> Icons.Default.Delete
                }
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = bg,
                  modifier = Modifier.fillMaxSize()
                ) {
                  if (direction != SwipeToDismissBoxValue.Settled) {
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                      contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                        Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = icon,
                          contentDescription = label,
                          tint = tint,
                          modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = label,
                          color = tint,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }
                    }
                  }
                }
              },
              content = {
            // Double-tap jumps straight to chat; long-press opens every
            // action at once. Single tap (card) still opens details.
            Box(
              modifier = Modifier.pointerInput(booking.id) {
                detectTapGestures(
                  onDoubleTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenChat(booking.id)
                  },
                  onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sheetBookingId = booking.id
                  }
                )
              }
            ) {
            SoftCard(
              onClick = { onOpenBooking(booking.id) },
              testTag = "booking_card_${booking.id}"
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveOrb(
                      color = when (booking.status) {
                        BookingStatus.IN_PROGRESS -> FameGoGold
                        BookingStatus.CONFIRMED -> FameGoSuccessGreen
                        BookingStatus.SEARCHING_CREW -> FameGoAccentCyan
                        else -> FameGoTextMuted
                      },
                      size = 7.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = when (booking.status) {
                        BookingStatus.IN_PROGRESS -> "IN PROGRESS"
                        BookingStatus.CONFIRMED -> "CONFIRMED"
                        BookingStatus.UPCOMING -> "UPCOMING"
                        BookingStatus.SEARCHING_CREW -> "SEARCHING"
                        BookingStatus.CREW_RESPONDED -> "RESPONDED"
                        BookingStatus.COMPLETED -> "COMPLETED"
                        BookingStatus.CANCELLED -> "CANCELLED"
                        BookingStatus.DRAFT -> "DRAFT"
                      },
                      color = when (booking.status) {
                        BookingStatus.IN_PROGRESS -> FameGoGold
                        BookingStatus.CONFIRMED -> FameGoSuccessGreen
                        else -> FameGoTextMuted
                      },
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.8.sp
                    )
                  }

                  Text(
                    text = "₹${booking.estimatedBudget}",
                    color = FameGoGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = booking.title,
                  color = FameGoWhite,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )

                Text(
                  text = "${booking.date} • ${booking.time} • ${booking.location.address.substringBefore(",")}",
                  color = FameGoTextSecondary,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(top = 2.dp)
                )

                if (booking.status == BookingStatus.COMPLETED) {
                  Spacer(modifier = Modifier.height(12.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FameGoSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle),
                    modifier = Modifier.clickable { onBookAgain(booking.category) }
                  ) {
                    Text(
                      text = "Book Again ↻",
                      color = FameGoGold,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            }
              } // tap-gesture Box
              } // SwipeToDismissBox content
            )
          }

          item {
            Spacer(modifier = Modifier.height(110.dp))
          }
        }
        }
      }
    }
    // Long-press action sheet — every row action in one place.
    sheetBookingId?.let { sheetId ->
      val sheetBooking = bookings.firstOrNull { it.id == sheetId }
      FameGoSheet(onDismiss = { sheetBookingId = null }) {
        Text(
          sheetBooking?.title ?: "Shoot options",
          color = FameGoWhite,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          modifier = Modifier.padding(bottom = 12.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SheetActionRow(label = "Open details") {
            sheetBookingId = null
            onOpenBooking(sheetId)
          }
          SheetActionRow(label = "Message crew") {
            sheetBookingId = null
            onOpenChat(sheetId)
          }
          if (sheetBooking != null) {
            SheetActionRow(label = "Book again") {
              sheetBookingId = null
              onBookAgain(sheetBooking.category)
            }
          }
          SheetActionRow(label = "Delete shoot", destructive = true) {
            sheetBookingId = null
            pendingDeleteId = sheetId
          }
        }
      }
    }
    // Delete needs an explicit tap — swipes never destroy data alone.
    pendingDeleteId?.let { doomedId ->      val doomed = bookings.firstOrNull { it.id == doomedId }
      AlertDialog(
        onDismissRequest = { pendingDeleteId = null },
        title = {
          Text(
            "Delete this shoot?",
            color = FameGoWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        text = {
          Text(
            text = "“${doomed?.title ?: "This shoot"}” will be removed from your shoots.",
            color = FameGoTextSecondary,
            fontSize = 14.sp
          )
        },
        confirmButton = {
          TextButton(
            onClick = {
              pendingDeleteId = null
              FameGoRepository.deleteBooking(doomedId)
            }
          ) { Text("Delete", color = FameGoLiveRed, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
          TextButton(onClick = { pendingDeleteId = null }) {
            Text("Keep", color = FameGoTextSecondary, fontWeight = FontWeight.Medium)
          }
        },
        containerColor = FameGoCard,
        shape = RoundedCornerShape(20.dp)
      )
    }
  }
}

@Composable
private fun SheetActionRow(
  label: String,
  destructive: Boolean = false,
  onClick: () -> Unit
) {
  FameGoPressable(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = FameGoSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle),
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = label,
      color = if (destructive) FameGoLiveRed else FameGoTextPrimary,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
    )
  }
}
