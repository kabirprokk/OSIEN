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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.model.NotificationItem
import com.example.ui.components.SoftCard
import com.example.ui.components.rubberBand
import com.example.ui.theme.fameGoRise
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  onOpenBooking: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by FameGoRepository.currentUser.collectAsState()
  val repositoryNotifications by FameGoRepository.notifications.collectAsState()
  val notifications = repositoryNotifications.filter { it.targetRole == currentUser.role }
  val unreadCount = notifications.count { !it.isRead }

  val scrollState = rememberScrollState()
  var isRefreshing by remember { mutableStateOf(false) }
  val refreshScope = rememberCoroutineScope()
  val notifSfx = LocalContext.current.applicationContext

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
  ) {
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        isRefreshing = true
        com.example.data.FameGoSfx.pop(notifSfx)
        refreshScope.launch {
          FameGoRepository.refreshNow()
          isRefreshing = false
        }
      },
      modifier = Modifier.fillMaxSize()
    ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .rubberBand()
        .padding(horizontal = 20.dp)
        .fameGoRise()
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Notifications",
        color = FameGoWhite,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Updates about your shoots",
          color = FameGoTextMuted,
          fontSize = 13.sp,
          modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
        )
        if (unreadCount > 0) {
          Text(
            text = "Mark all read",
            color = FameGoGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
              .clickable { FameGoRepository.markAllNotificationsRead(currentUser.role) }
              .padding(bottom = 20.dp)
              .testTag("notifications_mark_all_read")
          )
        }
      }

      if (notifications.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "All caught up",
              color = FameGoWhite,
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "You will see shoot updates here.",
              color = FameGoTextMuted,
              fontSize = 13.sp,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      } else {
        val groups = listOf("Just now", "Today", "Earlier")
        groups.forEach { grp ->
          val itemsInGroup = notifications.filter { notificationGroup(it) == grp }
          if (itemsInGroup.isNotEmpty()) {
            Text(
              text = grp,
              color = FameGoTextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(vertical = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              itemsInGroup.forEach { item ->
                SoftCard(
                  onClick = {
                    FameGoRepository.markNotificationRead(item.id)
                    item.bookingId?.let { onOpenBooking(it) }
                  },
                  testTag = "notification_${item.id}"
                ) {
                  Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Box(
                      modifier = Modifier
                        .padding(top = 5.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (!item.isRead) FameGoGold else FameGoTextMuted.copy(alpha = 0.4f))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Text(
                        text = item.title,
                          color = FameGoWhite,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.SemiBold
                        )
                        Text(
                          text = item.timestampText,
                          color = FameGoTextMuted,
                          fontSize = 11.sp
                        )
                      }

                      Text(
                        text = item.message,
                        color = FameGoTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 3.dp)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(110.dp))
    }
    }
  }
}

private fun notificationGroup(item: NotificationItem): String = when {
  item.timestampText.contains("now", ignoreCase = true) ||
    item.timestampText.contains("just", ignoreCase = true) -> "Just now"
  item.timestampText.contains("yesterday", ignoreCase = true) -> "Earlier"
  else -> "Today"
}
