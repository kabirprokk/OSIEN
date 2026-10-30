package com.example.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.PrimaryGoldButton
import com.example.presentation.components.SecondaryDarkButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.NebulaCyan
import com.example.ui.theme.NebulaDeep
import com.example.ui.theme.NebulaViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
  onFinish: () -> Unit
) {
  var line1Visible by remember { mutableStateOf(false) }
  var line2Visible by remember { mutableStateOf(false) }
  var showSteller by remember { mutableStateOf(false) }
  var stellerVisible by remember { mutableStateOf(false) }
  var dimmed by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    // Act 1: FameBros presents FameBook, fading up out of black.
    delay(250)
    line1Visible = true
    delay(800)
    line2Visible = true
    delay(1300)
    dimmed = true
    // Act 2: crossfade into the Steller-Nebula developer card.
    delay(600)
    showSteller = true
    dimmed = false
    delay(150)
    stellerVisible = true
    delay(1600)
    dimmed = true
    delay(600)
    onFinish()
  }

  val screenAlpha by animateFloatAsState(
    targetValue = if (dimmed) 0f else 1f,
    animationSpec = tween(durationMillis = 550),
    label = "intro_fade"
  )
  val background by animateColorAsState(
    targetValue = if (showSteller) NebulaDeep else ObsidianBlack,
    animationSpec = tween(durationMillis = 800),
    label = "intro_bg"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(background)
      .graphicsLayer { alpha = screenAlpha },
    contentAlignment = Alignment.Center
  ) {
    if (!showSteller) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        AnimatedVisibility(
          visible = line1Visible,
          enter = fadeIn(animationSpec = tween(600)) +
            slideInVertically(animationSpec = tween(600)) { it / 3 }
        ) {
          Text(
            text = "FAMEBROS STUDIO PRESENTS",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 4.sp
            ),
            color = AmberGold
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(
          visible = line2Visible,
          enter = fadeIn(animationSpec = tween(700)) +
            slideInVertically(animationSpec = tween(700)) { it / 3 } +
            scaleIn(initialScale = 0.94f, animationSpec = tween(700))
        ) {
          Text(
            text = "FameBook",
            style = MaterialTheme.typography.displayLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = (-1).sp
            ),
            color = PureWhite
          )
        }
      }
    } else {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        AnimatedVisibility(
          visible = stellerVisible,
          enter = fadeIn(animationSpec = tween(600)) +
            slideInVertically(animationSpec = tween(600)) { it / 3 }
        ) {
          Text(
            text = "DEVELOPED BY",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 5.sp
            ),
            color = TextSecondary
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
          visible = stellerVisible,
          enter = fadeIn(animationSpec = tween(700)) +
            slideInVertically(animationSpec = tween(700)) { it / 4 } +
            scaleIn(initialScale = 0.94f, animationSpec = tween(700))
        ) {
          Text(
            text = "Steller-Nebula",
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = (-0.5).sp,
              brush = Brush.horizontalGradient(
                colors = listOf(NebulaCyan, NebulaViolet)
              )
            )
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        AnimatedVisibility(
          visible = stellerVisible,
          enter = fadeIn(animationSpec = tween(800)) +
            scaleIn(initialScale = 0.9f, animationSpec = tween(800))
        ) {
          Image(
            painter = painterResource(id = R.drawable.steller_nebula_logo),
            contentDescription = "Steller-Nebula Logo",
            modifier = Modifier
              .size(128.dp)
              .clip(CircleShape)
          )
        }
      }
    }
  }
}

data class OnboardingSlide(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val tag: String
)

@Composable
fun OnboardingScreen(
  onComplete: () -> Unit
) {
  val slides = listOf(
    OnboardingSlide(
      title = "Book Professional Production Crew",
      subtitle = "Instant access to verified FameBros cinematographers, photographers, sound technicians & editors.",
      icon = Icons.Default.Camera,
      tag = "ON-DEMAND TALENT"
    ),
    OnboardingSlide(
      title = "Lightning-Fast Dispatch",
      subtitle = "Submit your shoot specs. Available crew members receive immediate alerts and claim your shoot in seconds.",
      icon = Icons.Default.FlashOn,
      tag = "REAL-TIME MARKETPLACE"
    ),
    OnboardingSlide(
      title = "Direct Prep & Collaboration",
      subtitle = "Connect directly with your confirmed crew via live chat, call, or WhatsApp with clear call sheets.",
      icon = Icons.Default.Videocam,
      tag = "CREATIVE CONFIRMATION"
    )
  )

  val pagerState = rememberPagerState { slides.size }
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 24.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Skip Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End
    ) {
      TextButton(
        onClick = onComplete,
        modifier = Modifier.testTag("onboarding_skip_button")
      ) {
        Text(
          text = "SKIP",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = TextSecondary
        )
      }
    }

    // Pager Content
    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) { page ->
      val slide = slides[page]
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(DarkCard)
            .border(1.5.dp, DarkBorder, RoundedCornerShape(32.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = slide.icon,
            contentDescription = null,
            tint = AmberGold,
            modifier = Modifier.size(54.dp)
          )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AmberGoldContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = slide.tag,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = AmberGold
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = slide.title,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
          ),
          color = PureWhite,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = slide.subtitle,
          style = MaterialTheme.typography.bodyLarge,
          color = TextSecondary,
          textAlign = TextAlign.Center,
          lineHeight = 22.sp
        )
      }
    }

    // Indicator & Action Buttons
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Dots
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 28.dp)
      ) {
        repeat(slides.size) { index ->
          val isSelected = pagerState.currentPage == index
          Box(
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .height(6.dp)
              .width(if (isSelected) 24.dp else 6.dp)
              .clip(CircleShape)
              .background(if (isSelected) AmberGold else DarkBorder)
          )
        }
      }

      if (pagerState.currentPage < slides.size - 1) {
        PrimaryGoldButton(
          text = "NEXT",
          onClick = {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
          },
          modifier = Modifier.testTag("onboarding_next_button")
        )
      } else {
        PrimaryGoldButton(
          text = "GET STARTED",
          onClick = onComplete,
          modifier = Modifier.testTag("onboarding_get_started_button")
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
