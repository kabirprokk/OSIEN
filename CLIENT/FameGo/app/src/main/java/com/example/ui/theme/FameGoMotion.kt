package com.example.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * FameGo Motion System
 *
 * Core Motion Rules:
 * - CREATED = RISE: Gently rises upward into position (+16dp to 0dp, alpha 0 to 1).
 * - CONFIRMED = SETTLE: Physically settles downward into place (-6dp to 0dp, scale 1.03 to 1.0).
 * - SEARCHING = BREATHE: Subtle repeating scale (1.0 to 1.03) and soft opacity/glow over 2.4 - 3.2s.
 * - DISMISSED = SINK: Gently moves downward (+16dp) and fades out (scale 1.0 to 0.95, alpha 1 to 0).
 * - LIVE = ALIVE: Tiny continuous signal (breathing dot, soft status orb, waveform ring).
 */
object FameGoMotion {
  // Speed standards (milliseconds)
  const val TAP_FEEDBACK = 120
  const val STATE_CHANGE = 200
  const val COMPONENT_TRANSITION = 300
  const val SCREEN_TRANSITION = 380
  const val BREATHE_CYCLE = 2600
}

/**
 * CREATED = RISE:
 * Gently rises upward into position.
 * The item begins slightly below its final position and settles naturally.
 */
@Composable
fun Modifier.fameGoRise(
  enabled: Boolean = true,
  delayMs: Long = 0,
  durationMs: Int = FameGoMotion.COMPONENT_TRANSITION,
  offsetY: Float = 20f
): Modifier {
  if (!enabled) return this

  val animOffset = remember { Animatable(offsetY) }
  val animAlpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    if (delayMs > 0) delay(delayMs)
    animOffset.animateTo(
      targetValue = 0f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
      )
    )
  }

  LaunchedEffect(Unit) {
    if (delayMs > 0) delay(delayMs)
    animAlpha.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
    )
  }

  return this.graphicsLayer {
    translationY = animOffset.value
    alpha = animAlpha.value
  }
}

/**
 * CONFIRMED = SETTLE:
 * Physically feels like it has locked into place.
 * Small downward settle, very subtle scale reduction from 1.03 to 1.0.
 */
@Composable
fun Modifier.fameGoSettle(
  trigger: Any? = Unit,
  durationMs: Int = FameGoMotion.STATE_CHANGE
): Modifier {
  val animScale = remember(trigger) { Animatable(1.035f) }
  val animOffsetY = remember(trigger) { Animatable(-6f) }

  LaunchedEffect(trigger) {
    animScale.animateTo(
      targetValue = 1.0f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
      )
    )
  }

  LaunchedEffect(trigger) {
    animOffsetY.animateTo(
      targetValue = 0f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
      )
    )
  }

  return this.graphicsLayer {
    scaleX = animScale.value
    scaleY = animScale.value
    translationY = animOffsetY.value
  }
}

/**
 * SEARCHING = BREATHE:
 * Whenever FameGo is actively searching or processing:
 * Gently breathes with subtle scale (1.0 to 1.025) and opacity (0.85 to 1.0).
 */
@Composable
fun Modifier.fameGoBreathe(
  enabled: Boolean = true,
  cycleDurationMs: Int = FameGoMotion.BREATHE_CYCLE
): Modifier {
  if (!enabled) return this

  val infiniteTransition = rememberInfiniteTransition(label = "fameGoBreathe")
  val breatheScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = cycleDurationMs / 2, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "breatheScale"
  )
  val breatheAlpha by infiniteTransition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = cycleDurationMs / 2, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "breatheAlpha"
  )

  return this.graphicsLayer {
    scaleX = breatheScale
    scaleY = breatheScale
    alpha = breatheAlpha
  }
}

/**
 * DISMISSED = SINK:
 * Whenever something is removed, dismissed, or cancelled:
 * Gently moves backward/downward and fades.
 */
@Composable
fun Modifier.fameGoSink(
  isDismissing: Boolean,
  onFinished: () -> Unit = {}
): Modifier {
  val animOffset = remember { Animatable(0f) }
  val animScale = remember { Animatable(1.0f) }
  val animAlpha = remember { Animatable(1.0f) }

  LaunchedEffect(isDismissing) {
    if (isDismissing) {
      animOffset.animateTo(
        targetValue = 18f,
        animationSpec = tween(durationMillis = FameGoMotion.STATE_CHANGE, easing = FastOutSlowInEasing)
      )
      animScale.animateTo(
        targetValue = 0.94f,
        animationSpec = tween(durationMillis = FameGoMotion.STATE_CHANGE, easing = FastOutSlowInEasing)
      )
      animAlpha.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = FameGoMotion.STATE_CHANGE, easing = FastOutSlowInEasing)
      )
      onFinished()
    }
  }

  return this.graphicsLayer {
    translationY = animOffset.value
    scaleX = animScale.value
    scaleY = animScale.value
    alpha = animAlpha.value
  }
}

/**
 * LIVE = ALIVE:
 * Tiny continuous signal for ongoing shoots or crew en route.
 */
@Composable
fun FameGoLiveSignal(
  color: Color = FameGoGold,
  size: Dp = 8.dp,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "liveSignal")
  val ringScale by transition.animateFloat(
    initialValue = 1.0f,
    targetValue = 2.4f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "liveRingScale"
  )
  val ringAlpha by transition.animateFloat(
    initialValue = 0.65f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "liveRingAlpha"
  )

  Box(
    modifier = modifier.size(size * 2.5f),
    contentAlignment = Alignment.Center
  ) {
    // Expanding soft aura ring
    Box(
      modifier = Modifier
        .size(size)
        .scale(ringScale)
        .alpha(ringAlpha)
        .clip(CircleShape)
        .border(1.dp, color, CircleShape)
    )
    // Core solid orb
    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(color)
    )
  }
}
