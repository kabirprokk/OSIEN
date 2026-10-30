package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShootCategory
import com.example.model.ShootPlan
import com.example.ui.components.FameGoButton
import com.example.ui.components.FameGoOutlinedButton
import com.example.ui.components.SoftCard
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun ShootPlanScreen(
  preselectedCategory: ShootCategory?,
  onContinue: (ShootPlan, ShootCategory?) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selected by remember { mutableStateOf(ShootPlan.BRONZE_3H) }
  val context = LocalContext.current
  Column(
    modifier = modifier.fillMaxSize().background(Color.Transparent)
      .statusBarsPadding().navigationBarsPadding()
      .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FameGoTextSecondary)
      }
      Text("Choose your shoot plan", color = FameGoWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
    Text(
      "Pick the time that fits your shoot.", color = FameGoTextSecondary,
      fontSize = 14.sp, modifier = Modifier.padding(start = 48.dp, bottom = 24.dp)
    )
    Text("BRONZE PLAN", color = FameGoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    Spacer(Modifier.height(10.dp))
    ShootPlan.entries.forEach { plan ->
      val active = selected == plan
      SoftCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp)
          // Static gold glow for the selected plan — no rotating layers, so
          // cards stay fully visible and cheap to draw on low-end phones.
          .then(
            if (active) Modifier.shadow(
              elevation = 14.dp,
              shape = RoundedCornerShape(20.dp),
              spotColor = FameGoGold.copy(alpha = 0.55f)
            ) else Modifier
          )
          .border(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) FameGoGold else FameGoBorderSubtle,
            shape = RoundedCornerShape(20.dp)
          )
          .clickable { selected = plan }.testTag("plan_${plan.name.lowercase()}")
      ) {
        Column(Modifier.padding(18.dp)) {
          Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Column(Modifier.weight(1f)) {
              Text(plan.durationLabel, color = FameGoWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
              Text(plan.description, color = FameGoTextMuted, fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 6.dp))
            }
            Text("₹${"%,d".format(plan.priceRupees)}", color = FameGoGold, fontSize = 18.sp,
              fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
          }
          if (active) Text("SELECTED", color = FameGoGold, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, modifier = Modifier.padding(top = 12.dp).background(FameGoGoldContainer, RoundedCornerShape(8.dp)).padding(8.dp, 4.dp))
        }
      }
    }
    Spacer(Modifier.height(18.dp))
    SoftCard(modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(18.dp)) {
        Text("Custom shoot duration", color = FameGoWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Contact Famebros Studio for a tailored call time.", color = FameGoTextMuted, fontSize = 12.sp,
          modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
        FameGoOutlinedButton(
          text = "Contact on WhatsApp",
          onClick = {
            val message = URLEncoder.encode(
              "Hi Famebros Studio, I want a custom FameGo shoot plan.", StandardCharsets.UTF_8.toString()
            )
            runCatching {
              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919892384424?text=$message")))
            }
          },
          modifier = Modifier.fillMaxWidth(), testTag = "custom_plan_whatsapp"
        )
      }
    }
    Spacer(Modifier.height(22.dp))
    FameGoButton(
      text = "Continue with ₹${"%,d".format(selected.priceRupees)}",
      onClick = { onContinue(selected, preselectedCategory) },
      modifier = Modifier.fillMaxWidth(), testTag = "plan_continue"
    )
    Spacer(Modifier.height(28.dp))
  }
}
