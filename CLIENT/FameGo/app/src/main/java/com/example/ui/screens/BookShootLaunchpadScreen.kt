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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShootCategory
import com.example.model.ShootPlan
import com.example.ui.components.CharacterState
import com.example.ui.components.FameGoCharacterIllustration
import com.example.ui.components.SoftCard
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoSurface
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import com.example.ui.theme.fameGoBreathe
import com.example.ui.theme.fameGoRise

/**
 * Reels-only launchpad. FameGo shoots vertical reels and nothing else,
 * so this screen goes straight to plans instead of picking a production type.
 */
@Composable
fun BookShootLaunchpadScreen(
  onStartBooking: (ShootCategory?) -> Unit,
  onPlanClick: (ShootPlan) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

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
        text = "Book a Reel Shoot",
        color = FameGoWhite,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )
      Text(
        text = "Vertical reel crew for brands and creators",
        color = FameGoTextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 2.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Availability badge — always a single line.
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(FameGoGoldContainer)
          .border(1.dp, FameGoGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(FameGoSuccessGreen)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Availability checked after booking",
            color = FameGoGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Hero: reels only
      SoftCard(
        modifier = Modifier.fillMaxWidth(),
        isElevated = true
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FameGoCharacterIllustration(
            state = CharacterState.FINDING_CREW,
            size = 72.dp,
            modifier = Modifier.fameGoBreathe()
          )
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "We only shoot reels",
              color = FameGoWhite,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Pro iPhone videographer, gimbal, mic and light. Raw vertical files, no editing.",
              color = FameGoTextSecondary,
              fontSize = 12.sp,
              lineHeight = 17.sp,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Plans at a glance — tap a plan to jump straight into booking with it.
      ShootPlan.values().forEach { plan ->
        PlanGlanceRow(plan = plan, onClick = { onPlanClick(plan) })
        Spacer(modifier = Modifier.height(8.dp))
      }

      Spacer(modifier = Modifier.height(8.dp))

      // How it works
      Text(
        text = "How it works",
        color = FameGoTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(10.dp))
      HowItWorksRow(index = "1", title = "Pick a plan", subtitle = "Bronze, Silver or Gold")
      HowItWorksRow(index = "2", title = "Share shoot details", subtitle = "Date, time and venue")
      HowItWorksRow(index = "3", title = "Crew arrives", subtitle = "Videographer locked in after payment")

      Spacer(modifier = Modifier.height(20.dp))

      // Primary CTA
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = FameGoGold,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onStartBooking(ShootCategory.VIDEO) }
          .testTag("launchpad_start_booking_btn")
      ) {
        Row(
          modifier = Modifier.padding(vertical = 16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color(0xFF141518),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Book a Reel Shoot",
            color = Color(0xFF141518),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = FameGoSuccessGreen,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Verified reel shooters with pro iPhone kits",
          color = FameGoTextMuted,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(145.dp))
    }
  }
}

@Composable
private fun PlanGlanceRow(plan: ShootPlan, onClick: () -> Unit) {
  SoftCard(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .testTag("launchpad_plan_${plan.name.lowercase()}")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(FameGoGoldContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when (plan) {
            ShootPlan.BRONZE_90 -> Icons.Default.Movie
            ShootPlan.BRONZE_3H -> Icons.Default.Videocam
            ShootPlan.BRONZE_6H -> Icons.Default.Schedule
          },
          contentDescription = null,
          tint = FameGoGold,
          modifier = Modifier.size(19.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = plan.title,
          color = FameGoWhite,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = plan.durationLabel,
          color = FameGoTextMuted,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = "₹${"%,d".format(plan.priceRupees)}",
        color = FameGoGold,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = "Book ${plan.title}",
        tint = FameGoGold,
        modifier = Modifier.size(18.dp).padding(start = 4.dp)
      )
    }
  }
}

@Composable
private fun HowItWorksRow(index: String, title: String, subtitle: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 5.dp)
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(FameGoSurface)
        .border(1.dp, FameGoBorderSubtle, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(text = index, color = FameGoGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(text = title, color = FameGoWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      Text(text = subtitle, color = FameGoTextMuted, fontSize = 11.sp)
    }
    Spacer(modifier = Modifier.weight(1f))
    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = null,
      tint = FameGoSuccessGreen.copy(alpha = 0.7f),
      modifier = Modifier.size(16.dp)
    )
  }
}

private data class CategoryLaunchpadItem(
  val category: ShootCategory,
  val title: String,
  val price: String,
  val kit: String,
  val icon: ImageVector
)
