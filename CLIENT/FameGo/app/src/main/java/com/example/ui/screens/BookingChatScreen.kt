package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.example.ui.components.fameGoTapBounce
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.model.ChatMessage
import com.example.model.Role
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoSurface
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite

@Composable
fun BookingChatScreen(
  bookingId: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bookings by FameGoRepository.bookings.collectAsState()
  val currentUser by FameGoRepository.currentUser.collectAsState()
  val allMessages by FameGoRepository.chatMessages.collectAsState()

  val booking = bookings.find { it.id == bookingId }
  val messages = allMessages[bookingId] ?: emptyList()

  LaunchedEffect(bookingId) {
    FameGoRepository.loadChatMessages(bookingId)
    FameGoRepository.startChatRealtime(bookingId)
  }
  DisposableEffect(bookingId) {
    onDispose { FameGoRepository.stopChatRealtime(bookingId) }
  }
  // Safety net: realtime sockets can silently drop (expired token, doze).
  // A slow poll guarantees messages land even when the socket is dead.
  // The dedicated socket + push-event reload carry the live path, so this
  // stays at 8s (not 3s) to save radio/battery.
  LaunchedEffect(bookingId) {
    while (true) {
      kotlinx.coroutines.delay(8000)
      FameGoRepository.loadChatMessages(bookingId)
    }
  }
  // Foreground return: always re-read, never show a stale thread.
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, bookingId) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        FameGoRepository.loadChatMessages(bookingId)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  var messageInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val chatError by FameGoRepository.chatErrors.collectAsState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      // animateScroll can throw when the list isn't laid out yet (rotation,
      // fast open); fall back to an instant jump instead of crashing chat.
      runCatching { listState.animateScrollToItem(messages.size - 1) }
        .onFailure { runCatching { listState.scrollToItem(messages.size - 1) } }
    }
  }

  // Read receipts: once incoming messages are on screen, mark them read.
  LaunchedEffect(messages) {
    if (messages.any { !it.isFromMe && !it.isRead }) {
      kotlinx.coroutines.delay(800)
      FameGoRepository.markChatRead(bookingId)
    }
  }

  val quickReplies = listOf(
    "Arriving at set in 15 mins",
    "Parking spot confirmed",
    "Ready with primary 4K kit",
    "Can you share entrance code?"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Bar with shoot context
      Surface(
        color = FameGoSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = FameGoTextPrimary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = booking?.shootTitle ?: "Message your crew",
                color = FameGoWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.ui.components.LiveOrb(
                  color = com.example.ui.theme.FameGoSuccessGreen,
                  size = 6.dp
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = if (booking?.status == com.example.model.BookingStatus.COMPLETED) "Chat history" else "Online",
                  color = FameGoGold,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = FameGoCardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder)
            ) {
              Text(
                text = "FameGo Verified",
                color = FameGoTextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      // Messages list
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        item {
          Spacer(modifier = Modifier.height(10.dp))
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
              text = "Shoot chat",
              color = FameGoTextMuted,
              fontSize = 11.sp
            )
          }
        }

        items(messages, key = { it.id }) { msg ->
          ChatBubble(message = msg, isMe = msg.isFromMe)
        }

        item {
          Spacer(modifier = Modifier.height(8.dp))
        }
      }

      // Send failures used to vanish silently (ghost "Sent" bubble).
      // Surface the reason inline so the user can retry.
      chatError?.let { error ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = FameGoCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, FameGoGold.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = error,
              color = FameGoGold,
              fontSize = 12.sp,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = "Dismiss",
              color = FameGoTextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.clickable { FameGoRepository.consumeChatError() }
            )
          }
        }
      }

      // Quick template replies
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        quickReplies.forEach { text ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = FameGoCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
            modifier = Modifier.fameGoTapBounce {
              FameGoRepository.sendChatMessage(
                bookingId = bookingId,
                text = text,
                senderRole = currentUser.role,
                senderName = currentUser.name
              )
            }
          ) {
            Text(
              text = text,
              color = FameGoTextSecondary,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }

      // Input Field Row
      Surface(
        color = FameGoSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = messageInput,
            onValueChange = { messageInput = it },
            placeholder = { Text("Write a message...", color = FameGoTextMuted, fontSize = 13.sp) },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = FameGoTextPrimary,
              unfocusedTextColor = FameGoTextPrimary,
              focusedContainerColor = FameGoCard,
              unfocusedContainerColor = FameGoCard,
              focusedBorderColor = FameGoGold,
              unfocusedBorderColor = FameGoBorder
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("chat_input_field")
          )

          Spacer(modifier = Modifier.width(8.dp))

          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(if (messageInput.isNotBlank()) FameGoGold else FameGoCardElevated)
              .clickable(enabled = messageInput.isNotBlank()) {
                val toSend = messageInput.trim()
                messageInput = ""
                FameGoRepository.sendChatMessage(
                  bookingId = bookingId,
                  text = toSend,
                  senderRole = currentUser.role,
                  senderName = currentUser.name
                )
              },
            contentAlignment = Alignment.Center
          ) {
            // Send arrow pops alive the moment there's text to send.
            val ready = messageInput.isNotBlank()
            val pop = com.example.ui.components.fameGoToggleScale(ready)
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint = if (ready) FameGoBg else FameGoTextMuted,
              modifier = Modifier.size(20.dp).scale(pop)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChatBubble(message: ChatMessage, isMe: Boolean) {
  val alignEnd = isMe
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
  ) {
    if (!alignEnd) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = message.senderName,
          color = FameGoGold,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = message.senderRole.name,
          color = FameGoTextMuted,
          fontSize = 9.sp
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
    }

    Surface(
      shape = RoundedCornerShape(
        topStart = 14.dp,
        topEnd = 14.dp,
        bottomStart = if (alignEnd) 14.dp else 2.dp,
        bottomEnd = if (alignEnd) 2.dp else 14.dp
      ),
      color = if (alignEnd) FameGoGoldContainer else FameGoCard,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (alignEnd) FameGoGold.copy(alpha = 0.5f) else FameGoBorder
      ),
      modifier = Modifier.fillMaxWidth(0.82f)
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
          text = message.message,
          color = if (alignEnd) FameGoTextPrimary else FameGoWhite,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (alignEnd) {
            "${message.timeText} • ${if (message.isRead) "Seen" else "Sent"}"
          } else {
            message.timeText
          },
          color = if (alignEnd && message.isRead) FameGoGold else FameGoTextMuted,
          fontSize = 10.sp,
          modifier = Modifier.align(Alignment.End)
        )
      }
    }
  }
}
