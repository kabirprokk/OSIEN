package com.example.presentation.crew.request

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.alivePulse
import com.example.presentation.components.rememberAlivePulse
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Premium Liquid-Glass Floating Incoming Shoot Request Overlay
 * Absolutely NO timers or countdowns.
 */
@Composable
fun IncomingShootRequestOverlay(
  viewModel: CrewIncomingRequestViewModel,
  onViewShoot: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val pulse by rememberAlivePulse()

  // Subtle background dim overlay when request is active
  AnimatedVisibility(
    visible = uiState !is IncomingRequestUiState.Idle,
    enter = fadeIn(animationSpec = tween(280)),
    exit = fadeOut(animationSpec = tween(200))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0x99000000))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) {
          // Tap scrim to dismiss
          viewModel.dismiss()
        }
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .padding(horizontal = 20.dp, vertical = 24.dp),
    contentAlignment = Alignment.TopCenter
  ) {
    AnimatedVisibility(
      visible = uiState !is IncomingRequestUiState.Idle,
      enter = slideInVertically(
        initialOffsetY = { -it / 2 },
        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing)
      ) + fadeIn(
        animationSpec = tween(durationMillis = 280)
      ) + scaleIn(
        initialScale = 0.97f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
      ),
      exit = slideOutVertically(
        targetOffsetY = { -it / 2 },
        animationSpec = tween(durationMillis = 240)
      ) + fadeOut(
        animationSpec = tween(durationMillis = 200)
      ) + scaleOut(
        targetScale = 0.97f,
        animationSpec = tween(durationMillis = 200)
      )
    ) {
      when (val state = uiState) {
        is IncomingRequestUiState.ActiveRequest -> {
          ActiveLiquidGlassRequestCard(
            state = state,
            pulse = pulse,
            onAccept = { viewModel.acceptRequest(state.request.id) },
            onDecline = { viewModel.declineRequest(state.request.id) },
            onDismiss = { viewModel.dismiss() }
          )
        }

        is IncomingRequestUiState.ShootConfirmed -> {
          ShootConfirmedLiquidCard(
            state = state,
            onViewShoot = { onViewShoot(state.bookingId) },
            onDismiss = { viewModel.dismiss() }
          )
        }

        IncomingRequestUiState.Idle -> {
          // Hidden
        }
      }
    }
  }
}

/**
 * Premium Liquid Glass Surface for Incoming Shoot Request (Level 3 Floating Layer)
 * Clean, restrained, NO timers or countdowns.
 */
@Composable
private fun ActiveLiquidGlassRequestCard(
  state: IncomingRequestUiState.ActiveRequest,
  pulse: Float,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  onDismiss: () -> Unit
) {
  val isAssigned = state.isAlreadyAssigned
  val accentColor = if (isAssigned) RecRed else AmberGold

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color(0xF2141720), // Translucent liquid glass base
    border = BorderStroke(1.2.dp, if (isAssigned) RecRed.copy(alpha = 0.6f) else Color(0x33FFFFFF)),
    shadowElevation = 22.dp,
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 460.dp)
      .shadow(
        elevation = 24.dp,
        shape = RoundedCornerShape(24.dp),
        spotColor = Color(0x66000000),
        ambientColor = Color(0x40000000)
      )
      .testTag("floating_incoming_shoot_request_popup")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0x18FFFFFF), // Subtle top refraction highlight
              Color(0x00FFFFFF),
              Color(0x08000000)
            )
          )
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 22.dp)
      ) {
        // 1. TOP HEADER: Status badge + subtle close affordance
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
                .alivePulse(pulse)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isAssigned) "REQUEST ALREADY ASSIGNED" else "NEW SHOOT REQUEST",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              ),
              color = accentColor
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Dismiss",
              tint = TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. SHOOT TYPE / TITLE
        Text(
          text = state.request.shootTypeTitle,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.3).sp
          ),
          color = PureWhite
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 3. DATE & TIME • DURATION
        Text(
          text = "${state.request.dateFormatted} • ${state.request.timeAndDuration.substringBefore(" •")}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = AmberGold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. LOCATION
        Text(
          text = state.request.locationName,
          style = MaterialTheme.typography.bodyLarge,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 5. ROLE
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x26FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Text(
            text = state.request.requiredRole,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = PureWhite
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. ACTION BUTTONS OR ASSIGNED LOCK STATE
        if (isAssigned) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0x26FF453A),
            border = BorderStroke(1.dp, RecRed.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = RecRed,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Claimed by ${state.assignedToName ?: "another crew member"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = PureWhite
              )
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // DECLINE BUTTON (Subtle liquid glass)
            OutlinedButton(
              onClick = onDecline,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, Color(0x33FFFFFF)),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSecondary
              ),
              modifier = Modifier
                .weight(0.38f)
                .height(52.dp)
                .testTag("incoming_request_decline_button"),
              contentPadding = PaddingValues(0.dp)
            ) {
              Text(
                text = "DECLINE",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = TextSecondary
              )
            }

            // ACCEPT BUTTON (Liquid Glass Neumorphic Accent)
            LiquidAcceptButton(
              onClick = onAccept,
              modifier = Modifier
                .weight(0.62f)
                .height(52.dp)
                .testTag("incoming_request_accept_button")
            )
          }
        }
      }
    }
  }
}

/**
 * Premium Liquid Accept Button with subtle depth and smooth tactile compression
 */
@Composable
private fun LiquidAcceptButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "accept_btn_scale"
  )

  Button(
    onClick = onClick,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = AmberGold,
      contentColor = ObsidianBlack
    ),
    modifier = modifier
      .scale(scale)
      .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(14.dp),
        spotColor = AmberGold.copy(alpha = 0.6f)
      ),
    contentPadding = PaddingValues(0.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = ObsidianBlack,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "ACCEPT",
        style = MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 0.8.sp
        ),
        color = ObsidianBlack
      )
    }
  }
}

/**
 * Premium Confirmation Card shown immediately when accepted
 */
@Composable
private fun ShootConfirmedLiquidCard(
  state: IncomingRequestUiState.ShootConfirmed,
  onViewShoot: () -> Unit,
  onDismiss: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color(0xF2121A16), // Subtle emerald liquid tone
    border = BorderStroke(1.2.dp, EmeraldSuccess.copy(alpha = 0.7f)),
    shadowElevation = 24.dp,
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 460.dp)
      .shadow(
        elevation = 24.dp,
        shape = RoundedCornerShape(24.dp),
        spotColor = EmeraldSuccess.copy(alpha = 0.4f)
      )
      .testTag("shoot_confirmed_floating_popup")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              EmeraldSuccess.copy(alpha = 0.15f),
              Color(0x00000000)
            )
          )
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(EmeraldSuccess),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ObsidianBlack,
                modifier = Modifier.size(14.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "SHOOT CONFIRMED",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
              ),
              color = EmeraldSuccess
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Dismiss",
              tint = TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = state.shootTypeTitle,
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
          color = PureWhite
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = state.dateAndTime,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = AmberGold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "You are assigned to this shoot.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onViewShoot,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = AmberGold,
            contentColor = ObsidianBlack
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("view_confirmed_shoot_button")
        ) {
          Text(
            text = "VIEW SHOOT",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 0.8.sp
            ),
            color = ObsidianBlack
          )
        }
      }
    }
  }
}
