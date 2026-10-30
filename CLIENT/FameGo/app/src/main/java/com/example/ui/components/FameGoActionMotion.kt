package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Shared action motion: every tap in the app should feel alive.
 * Buttons already spring via VengeanceAnimatedButton; these cover the rows,
 * chips, icons and state flips that plain clickable() leaves dead.
 */

/** Press-down bounce for rows/chips/cards. Replaces bare clickable(). */
@Composable
fun Modifier.fameGoTapBounce(
  enabled: Boolean = true,
  pressedScale: Float = 0.94f,
  onTap: () -> Unit
): Modifier {
  val scale = androidx.compose.runtime.remember { Animatable(1f) }
  return this
    .scale(scale.value)
    .pointerInput(enabled, onTap) {
      detectTapGestures(
        onPress = {
          if (!enabled) return@detectTapGestures
          try {
            scale.animateTo(pressedScale, tween(90, easing = FastOutSlowInEasing))
            tryAwaitRelease()
          } finally {
            scale.animateTo(
              1f,
              spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            )
          }
        },
        onTap = { if (enabled) onTap() }
      )
    }
}

/**
 * Pop transition for state flips (favorite heart, send-button enable,
 * countdown ticks): scales through 1.25 on every target change.
 */
@Composable
fun <T> FameGoPop(
  target: T,
  modifier: Modifier = Modifier,
  contentAlignment: Alignment = Alignment.Center,
  content: @Composable (T) -> Unit
) {
  AnimatedContent(
    targetState = target,
    transitionSpec = {
      (fadeIn(tween(140)) + scaleIn(
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        initialScale = 0.6f
      )).togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.8f))
    },
    contentAlignment = contentAlignment,
    label = "fameGoPop",
    modifier = modifier
  ) { content(it) }
}

/** Gentle infinite breathing for in-progress states (searching, waiting). */
@Composable
fun FameGoPulse(
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  if (!enabled) {
    Box(modifier = modifier) { content() }
    return
  }
  val s = remember { Animatable(1f) }
  LaunchedEffect(Unit) {
    while (true) {
      s.animateTo(1.06f, tween(700, easing = FastOutSlowInEasing))
      s.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
  }
  Box(modifier = modifier.scale(s.value)) { content() }
}

/** Error shake: jolts horizontally whenever [key] changes to non-null. */
@Composable
fun FameGoShake(
  key: Any?,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val offset = remember { Animatable(0f) }
  LaunchedEffect(key) {
    if (key != null) {
      repeat(2) {
        offset.animateTo(10f, tween(55))
        offset.animateTo(-10f, tween(55))
      }
      offset.animateTo(0f, tween(60))
    }
  }
  Box(modifier = modifier.graphicsLayer { translationX = offset.value }) { content() }
}

/** Smooth number/icon scale for toggles driven by a boolean. */
@Composable
fun fameGoToggleScale(active: Boolean): Float {
  val s by animateFloatAsState(
    targetValue = if (active) 1.15f else 1f,
    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
    label = "toggleScale"
  )
  return s
}
