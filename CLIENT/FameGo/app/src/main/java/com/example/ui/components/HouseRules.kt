package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite

/**
 * The house rules, in plain words. Shown once after signup (blocking) and
 * on demand from every profile tab. These match what the app enforces:
 * crew-first payment, the 10-minute free window, together-only wrap.
 */
val FameGoHouseRules: List<Pair<String, String>> = listOf(
  "Pay after accept" to
    "Your money moves only after a crew accepts your shoot. If nobody accepts, nothing is ever charged.",
  "10-minute free cancel" to
    "Cancel free within 10 minutes of booking. After that there is no refund — only the support team can review your case.",
  "Be on time" to
    "Be ready at the call time you picked. The crew waits up to 30 minutes; if you never show, the shoot counts as done with no refund.",
  "Real details only" to
    "Right address, right time, a phone that picks up. Wrong details that kill the shoot count as a no-show.",
  "Finish it together" to
    "The shoot ends only when both sides press Shoot Done standing together with Bluetooth on. One-sided taps don't count.",
  "Play fair" to
    "Fake bookings, abuse or harassment get the account removed without refund. Crew no-shows get reported the same way.",
  "Rescheduling" to
    "Need a new slot? Cancel free inside 10 minutes and book again. Later than that, talk to support before the call time."
)

@Composable
fun HouseRulesDialog(
  onDismiss: () -> Unit,
  confirmText: String = "Got it",
  modifier: Modifier = Modifier
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("House rules", color = FameGoWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    },
    text = {
      Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(
          "Short, strict, and enforced. Breaking these can cost you the booking.",
          color = FameGoTextSecondary, fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        FameGoHouseRules.forEach { (title, body) ->
          Text(
            text = title,
            color = FameGoGold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
          )
          Text(
            text = body,
            color = FameGoTextSecondary, fontSize = 13.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("house_rules_dismiss")) {
        Text(confirmText, color = FameGoGold, fontWeight = FontWeight.Bold)
      }
    },
    containerColor = FameGoCard,
    shape = RoundedCornerShape(20.dp)
  )
}
