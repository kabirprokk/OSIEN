package com.example.presentation.common

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.RealtimeHub
import com.example.domain.model.Message
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.MessageRepository
import com.example.presentation.components.FameBookTopBar
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingChatScreen(
  bookingId: String,
  currentUser: User,
  messageRepository: MessageRepository,
  bookingRepository: BookingRepository,
  onBackClick: () -> Unit
) {
  // Push-first refresh: incoming messages land within a second;
  // the timer is only a socket fallback.
  var refreshTick by remember { mutableIntStateOf(0) }
  LaunchedEffect(bookingId) {
    launch {
      RealtimeHub.messagesChanged.collect { refreshTick++ }
    }
    launch {
      RealtimeHub.bookingsChanged.collect { refreshTick++ }
    }
    while (true) {
      delay(15000)
      refreshTick++
    }
  }
  val messages by remember(bookingId, refreshTick) {
    messageRepository.getMessages(bookingId)
  }.collectAsState(initial = emptyList())
  val booking by remember(bookingId, refreshTick) {
    bookingRepository.getBooking(bookingId)
  }.collectAsState(initial = null)
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  var inputText by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }

  val quickReplies = listOf(
    "Location permit secured 👍",
    "Call time confirmed!",
    "Bringing extra lighting modifiers",
    "Will be on site 20 mins early"
  )

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Scaffold(
    containerColor = ObsidianBlack,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      FameBookTopBar(
        title = booking?.title ?: "Production Chat",
        subtitle = "FameBook Direct Prep",
        currentUser = currentUser,
        onBackClick = onBackClick
      )
    },
    bottomBar = {
      Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          // Quick suggestions chips
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(quickReplies) { reply ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.clickable {
                  inputText = reply
                }
              ) {
                Text(
                  text = reply,
                  style = MaterialTheme.typography.labelSmall,
                  color = AmberGold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = inputText,
              onValueChange = { inputText = it },
              placeholder = { Text("Message shoot crew...", color = TextMuted) },
              modifier = Modifier
                .weight(1f)
                .testTag("chat_input_field"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = DarkBorder,
                cursorColor = AmberGold
              ),
              shape = RoundedCornerShape(24.dp),
              singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
              onClick = {
                if (inputText.isNotBlank()) {
                  val textToSend = inputText
                  inputText = ""
                  scope.launch {
                    val result = try {
                      messageRepository.sendMessage(
                        bookingId = bookingId,
                        senderId = currentUser.id,
                        senderName = currentUser.name,
                        senderRole = currentUser.role,
                        text = textToSend
                      )
                    } catch (e: Exception) {
                      Result.failure(e)
                    }
                    if (result.isFailure) {
                      inputText = textToSend
                      snackbarHostState.showSnackbar("Message failed. Check connection and retry.")
                    } else {
                      refreshTick++
                    }
                  }
                }
              },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AmberGold)
                .testTag("chat_send_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = ObsidianBlack,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "END-TO-END CREATIVE PRODUCTION CHAT",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
          )
        }
      }

      items(messages, key = { it.id }) { msg ->
        val isMe = msg.senderId == currentUser.id
        MessageBubble(message = msg, isMe = isMe)
      }
    }
  }
}

@Composable
private fun MessageBubble(message: Message, isMe: Boolean) {
  val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
  val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
      Text(
        text = message.senderName,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = if (isMe) AmberGold else PureWhite
      )
      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(if (message.senderRole == UserRole.CREW) EmeraldSuccess.copy(alpha = 0.2f) else LensCyan.copy(alpha = 0.2f))
          .padding(horizontal = 4.dp, vertical = 1.dp)
      ) {
        Text(
          text = message.senderRole.name,
          style = MaterialTheme.typography.labelSmall,
          color = if (message.senderRole == UserRole.CREW) EmeraldSuccess else LensCyan
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 16.dp
      ),
      color = if (isMe) AmberGoldContainer else DarkCard,
      border = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = if (isMe) AmberGold.copy(alpha = 0.6f) else DarkBorder
      ),
      modifier = Modifier.widthIn(max = 290.dp)
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium,
          color = if (isMe) PureWhite else TextPrimary,
          lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = formattedTime,
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted,
          modifier = Modifier.align(Alignment.End)
        )
      }
    }
  }
}
