package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.ui.theme.fameGoRise
import com.example.ui.components.FameGoButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.VengeanceFaqAccordion
import com.example.ui.components.VengeanceFaqItem
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoLiveRed
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.launch

@Composable
fun CustomerSupportScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var disputeReason by remember { mutableStateOf("Call Time Reschedule") }
  var disputeText by remember { mutableStateOf("") }
  var isSubmitted by remember { mutableStateOf(false) }
  var activeChannelDialog by remember { mutableStateOf<String?>(null) }
  var followUp by remember { mutableStateOf("") }
  val thread by com.example.data.FameGoRepository.supportThread.collectAsState()

  LaunchedEffect(Unit) { com.example.data.FameGoRepository.loadSupportThread() }

  val reasons = listOf("Call Time Reschedule", "Crew Equipment Question", "Shoot Location Change", "Billing / Tax Invoice", "Other Production Inquiry")

  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp)
        .fameGoRise()
    ) {
      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = FameGoTextPrimary)
        }
        Text(
          text = "Support",
          color = FameGoWhite,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(36.dp))
      }

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "How can we help?",
        color = FameGoWhite,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Our team is here to help with your shoots",
        color = FameGoTextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
      )

      // Direct Contact Channels
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SupportChannelCard(
          icon = Icons.Default.Phone,
          title = "Call support",
          subtitle = "+91 98200 FAMEGO",
          accentColor = FameGoGold,
          onClick = { activeChannelDialog = "phone" }
        )

        SupportChannelCard(
          icon = Icons.AutoMirrored.Filled.Chat,
          title = "Chat on WhatsApp",
          subtitle = "Fast crew coordination and shoot updates",
          accentColor = FameGoSuccessGreen,
          onClick = { activeChannelDialog = "whatsapp" }
        )

        SupportChannelCard(
          icon = Icons.Default.Headphones,
          title = "Priority concierge",
          subtitle = "Dedicated help for large shoots and gear packages",
          accentColor = FameGoAccentCyan,
          onClick = { activeChannelDialog = "concierge" }
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Report an Issue / Booking Dispute Card
      SectionHeader(
        title = "Report an issue",
        subtitle = "Send a note to our operations team"
      )

      Surface(
        color = FameGoCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          if (isSubmitted) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D2818)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = FameGoSuccessGreen)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "Message received",
                  color = FameGoWhite,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Our team will reach out within 5 minutes.",
                  color = FameGoTextSecondary,
                  fontSize = 13.sp,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
            }
            // Two-way thread: admin replies land here live.
            if (thread.isNotEmpty()) {
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "Conversation",
                color = FameGoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(8.dp))
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                thread.takeLast(20).forEach { msg ->
                  SupportBubble(message = msg.message, isSupport = msg.isFromSupport)
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                  value = followUp,
                  onValueChange = { followUp = it },
                  placeholder = { Text("Reply to support…", color = FameGoTextMuted, fontSize = 12.sp) },
                  shape = RoundedCornerShape(14.dp),
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FameGoTextPrimary,
                    unfocusedTextColor = FameGoTextPrimary,
                    focusedContainerColor = FameGoCardElevated,
                    unfocusedContainerColor = FameGoCardElevated,
                    focusedBorderColor = FameGoGold,
                    unfocusedBorderColor = FameGoBorder
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("support_reply_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (followUp.isNotBlank()) FameGoGold else FameGoCardElevated)
                    .clickable(enabled = followUp.isNotBlank()) {
                      val text = followUp.trim()
                      followUp = ""
                      com.example.data.FameGoRepository.submitSupportMessage(text)
                    }
                    .testTag("support_reply_send"),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send reply",
                    tint = if (followUp.isNotBlank()) FameGoBg else FameGoTextMuted,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          } else {
            Text(
              text = "Topic",
              color = FameGoTextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              reasons.forEach { reason ->
                val isSelected = disputeReason == reason
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) FameGoGoldContainer else FameGoCardElevated,
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) FameGoGold else FameGoBorder
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { disputeReason = reason }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = reason,
                      color = if (isSelected) FameGoGold else FameGoTextPrimary,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                      Icon(Icons.Default.Check, contentDescription = null, tint = FameGoGold, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "Tell us what happened",
              color = FameGoTextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
              value = disputeText,
              onValueChange = { disputeText = it },
              placeholder = { Text("Details about timing, location, or crew...", color = FameGoTextMuted, fontSize = 12.sp) },
              minLines = 3,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = FameGoTextPrimary,
                unfocusedTextColor = FameGoTextPrimary,
                focusedContainerColor = FameGoCardElevated,
                unfocusedContainerColor = FameGoCardElevated,
                focusedBorderColor = FameGoGold,
                unfocusedBorderColor = FameGoBorder
              ),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            FameGoButton(
              text = "Send message",
              onClick = {
                if (disputeText.isNotBlank()) {
                  val ok = com.example.data.FameGoRepository.submitSupportMessage("[$disputeReason] ${disputeText.trim()}")
                  if (ok) isSubmitted = true
                }
              },
              icon = Icons.AutoMirrored.Filled.Send,
              modifier = Modifier.fillMaxWidth(),
              testTag = "submit_support_ticket"
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Production FAQ
      SectionHeader(
        title = "Frequently asked questions",
        subtitle = "Policies and setup on set"
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VengeanceFaqAccordion(
          title = null,
          items = listOf(
            VengeanceFaqItem(
              question = "What happens if call time is delayed by weather?",
              answer = "Message the crew in chat and agree a new time. If the shoot can't happen, cancel free within 10 minutes of booking — after that, talk to support."
            ),
            VengeanceFaqItem(
              question = "What gear does the crew bring?",
              answer = "Each crew member lists their own kit on their profile, and the confirmed booking shows a gear snapshot. Check it before you pay."
            ),
            VengeanceFaqItem(
              question = "How do cancellations and refunds work?",
              answer = "Cancel free within 10 minutes of booking. After that there is no refund — contact support and we'll review your case."
            )
          )
        )
      }

      Spacer(modifier = Modifier.height(32.dp))
    }

    // Active Channel Dialog
    activeChannelDialog?.let { channel ->
      AlertDialog(
        onDismissRequest = { activeChannelDialog = null },
        title = {
          Text(
            text = when (channel) {
              "phone" -> "Direct Hotline Support"
              "whatsapp" -> "WhatsApp Production Desk"
              "concierge" -> "Priority Concierge"
              else -> "Support Channel"
            },
            color = FameGoWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
        },
        text = {
          Text(
            text = when (channel) {
              "phone" -> "Call us at +91 98200 FAMEGO (326346) for help during a live shoot."
              "whatsapp" -> "Message us on WhatsApp with location pins, call-sheet changes, or gear issues."
              "concierge" -> "For multi-camera events and multi-day shoots, write to us here and we'll plan it with you."
              else -> ""
            },
            color = FameGoTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
          )
        },
        confirmButton = {
          TextButton(onClick = { activeChannelDialog = null }) {
            Text("Got it", color = FameGoGold, fontWeight = FontWeight.Bold)
          }
        },
        containerColor = FameGoCard,
        shape = RoundedCornerShape(20.dp)
      )
    }
  }
}
@Composable
private fun SupportBubble(message: String, isSupport: Boolean) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isSupport) Alignment.Start else Alignment.End
  ) {
    if (isSupport) {
      Text(
        text = "FameGo Support",
        color = FameGoGold,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(2.dp))
    }
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = if (isSupport) FameGoCardElevated else FameGoGoldContainer,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isSupport) FameGoBorder else FameGoGold.copy(alpha = 0.5f)
      ),
      modifier = Modifier.fillMaxWidth(0.85f)
    ) {
      Text(
        text = message,
        color = FameGoWhite,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
      )
    }
  }
}

@Composable
fun SupportChannelCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accentColor: Color,
  onClick: (() -> Unit)? = null
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = FameGoCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = onClick != null) { onClick?.invoke() }
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accentColor.copy(alpha = 0.15f))
          .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = FameGoWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = FameGoTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
      }

      Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FameGoTextMuted, modifier = Modifier.size(20.dp))
    }
  }
}

// =============================================================================
// ADMIN SUPPORT INBOX — every user thread grouped, reply inline as support.
// Requires supabase/008_support_replies.sql.
// =============================================================================

@Composable
fun AdminSupportScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val allSupport by com.example.data.FameGoRepository.allSupport.collectAsState()
  val allUsers by com.example.data.FameGoRepository.allUsers.collectAsState()
  var openUserId by remember { mutableStateOf<String?>(null) }
  var reply by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  val names = remember(allUsers) { allUsers.associate { it.id to it.name } }

  LaunchedEffect(Unit) {
    com.example.data.FameGoRepository.loadAllSupport()
    com.example.data.FameGoRepository.loadAllUsers()
  }

  val threads = remember(allSupport) {
    allSupport.groupBy { it.userId }.toList()
      .sortedByDescending { (_, msgs) -> msgs.maxOfOrNull { it.createdAt }.orEmpty() }
  }
  val openThread = threads.firstOrNull { it.first == openUserId }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
          if (openUserId != null) {
            openUserId = null
            reply = ""
          } else onBack()
        }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = FameGoTextPrimary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (openUserId != null) names[openUserId].orEmpty().ifBlank { "Conversation" } else "Support inbox",
          color = FameGoWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(16.dp))

      if (openThread == null) {
        if (threads.isEmpty()) {
          Text(
            text = "No support messages yet.",
            color = FameGoTextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 24.dp)
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            threads.forEach { (userId, msgs) ->
              val last = msgs.maxByOrNull { it.createdAt }
              val needsReply = last != null && !last.isFromSupport
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = FameGoCard,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (needsReply) FameGoGold.copy(alpha = 0.5f) else FameGoBorder
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { openUserId = userId }
                  .testTag("admin_support_thread_$userId")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (needsReply) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FameGoGold)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = names[userId].orEmpty().ifBlank { "User" },
                      color = FameGoWhite,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                    Text(
                      text = last?.message.orEmpty(),
                      color = FameGoTextSecondary,
                      fontSize = 12.sp,
                      maxLines = 2
                    )
                  }
                  Text(
                    text = "${msgs.size}",
                    color = FameGoTextMuted,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
        }
      } else {
        val (_, msgs) = openThread
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          msgs.sortedBy { it.createdAt }.takeLast(50).forEach { msg ->
            SupportBubble(message = msg.message, isSupport = msg.isFromSupport)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = reply,
            onValueChange = { reply = it },
            placeholder = { Text("Reply as support…", color = FameGoTextMuted, fontSize = 12.sp) },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = FameGoTextPrimary,
              unfocusedTextColor = FameGoTextPrimary,
              focusedContainerColor = FameGoCardElevated,
              unfocusedContainerColor = FameGoCardElevated,
              focusedBorderColor = FameGoGold,
              unfocusedBorderColor = FameGoBorder
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("admin_support_reply_field")
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(if (reply.isNotBlank() && !busy) FameGoGold else FameGoCardElevated)
              .clickable(enabled = reply.isNotBlank() && !busy) {
                val text = reply.trim()
                reply = ""
                busy = true
                error = null
                scope.launch {
                  com.example.data.FameGoRepository.replySupport(openUserId.orEmpty(), text)
                    .onFailure { e ->
                      reply = text
                      error = com.example.data.FameGoRepository.friendlyMessage(e)
                    }
                  busy = false
                }
              }
              .testTag("admin_support_reply_send"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send reply",
              tint = if (reply.isNotBlank() && !busy) FameGoBg else FameGoTextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }
        error?.let {
          Text(it, color = FameGoLiveRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
      }
      Spacer(modifier = Modifier.height(110.dp))
    }
  }
}
