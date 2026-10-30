package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FameGoBrightGold
import com.example.ui.theme.FameGoDarkerGold
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite

/**
 * FameGo snake loader — port of the web 5-square loadingspinner, restyled:
 * flat darkorange -> gold gradient + soft glow, 2px corners -> 7dp rounded,
 * tighter grid, trailing opacity (head bright, tail dim) and a gentle
 * per-square breathe so it feels alive on black.
 *
 * Grid paths replicate the original CSS keyframes exactly (2.4s loop).
 */
@Composable
fun FameGoSnakeLoader(
  modifier: Modifier = Modifier,
  square: Dp = 20.dp,
  gap: Dp = 8.dp,
  color: Color = FameGoGold,
  testTag: String = "famego_snake_loader"
) {
  val loop = rememberInfiniteTransition(label = "snakeLoop")
  val progress by loop.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
    label = "snakeProgress"
  )
  // Staggered entrance: squares pop in one after another on first show.
  val entrance = remember { Animatable(0f) }
  LaunchedEffect(Unit) { entrance.animateTo(1f, tween(900, easing = LinearEasing)) }
  val paths = remember { snakePaths() }
  Box(modifier = modifier.testTag(testTag), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val cell = square.toPx() + gap.toPx()
      val gridW = 4 * cell - gap.toPx()
      val gridH = 3 * cell - gap.toPx()
      val originX = (size.width - gridW) / 2f
      val originY = (size.height - gridH) / 2f
      // Soft gold pool under the snake.
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(color.copy(alpha = 0.14f), Color.Transparent),
          center = center,
          radius = size.minDimension * 0.55f
        ),
        radius = size.minDimension * 0.55f,
        center = center
      )
      paths.forEachIndexed { index, stops ->
        val (col, row) = samplePath(stops, progress)
        val x = originX + col * cell
        val y = originY + row * cell
        val sPx = square.toPx()
        // Entrance: staggered fade/scale like squarefadein (0.4s, 0.1s apart).
        val local = ((entrance.value * 0.9f) - index * 0.1f) / 0.4f
        val fade = local.coerceIn(0f, 1f)
        if (fade <= 0f) return@forEachIndexed
        // Trailing opacity: head (square5) brightest, tail dimmest.
        val trail = 0.55f + 0.45f * (index / 4f)
        val breathe = 1f + 0.06f * kotlin.math.sin((progress * 2 * Math.PI + index * 0.9).toFloat())
        val scale = (0.75f + 0.25f * fade) * breathe
        val side = sPx * scale
        val cx = x + (sPx - side) / 2f
        val cy = y + (sPx - side) / 2f
        // Glow halo.
        drawRoundRect(
          color = color.copy(alpha = 0.25f * fade * trail),
          topLeft = Offset(cx - 3.dp.toPx(), cy - 3.dp.toPx()),
          size = Size(side + 6.dp.toPx(), side + 6.dp.toPx()),
          cornerRadius = CornerRadius(10.dp.toPx())
        )
        // Gold gradient body.
        withTransform({ translate(left = cx, top = cy) }) {
          drawRoundRect(
            brush = Brush.linearGradient(
              colors = listOf(FameGoBrightGold, color, FameGoDarkerGold),
              start = Offset.Zero,
              end = Offset(side, side)
            ),
            size = Size(side, side),
            cornerRadius = CornerRadius(7.dp.toPx()),
            alpha = fade * trail
          )
        }
      }
    }
  }
}

/** Full-screen loading state: snake + wordmark + optional caption. */
@Composable
fun FameGoLoadingScreen(
  text: String = "FameGo",
  caption: String? = null,
  modifier: Modifier = Modifier,
  loaderModifier: Modifier = Modifier.size(width = 190.dp, height = 110.dp)
) {
  Column(
    modifier = modifier.testTag("famego_loading_screen"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    FameGoSnakeLoader(modifier = loaderModifier)
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text = text,
      color = FameGoWhite,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 2.sp,
      textAlign = TextAlign.Center
    )
    if (!caption.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = caption,
        color = FameGoTextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
      )
    } else {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Setting up your shoot…",
        color = FameGoTextMuted,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
      )
    }
  }
}

private typealias PathStops = List<Pair<Float, Pair<Float, Float>>>

/** Exact port of the CSS @keyframes square1..square5 (fraction, col, row). */
private fun snakePaths(): List<PathStops> = listOf(
  // square1
  listOf(0f to (0f to 0f), 0.08333f to (0f to 1f), 1f to (0f to 1f)),
  // square2
  listOf(
    0f to (0f to 1f), 0.08333f to (0f to 2f), 0.1667f to (1f to 2f),
    0.25f to (1f to 1f), 0.8333f to (1f to 1f), 0.9167f to (1f to 0f), 1f to (0f to 0f)
  ),
  // square3
  listOf(
    0f to (1f to 1f), 0.1667f to (1f to 1f), 0.25f to (1f to 0f),
    0.3333f to (2f to 0f), 0.4167f to (2f to 1f), 0.6667f to (2f to 1f),
    0.75f to (2f to 2f), 0.8333f to (1f to 2f), 0.9167f to (1f to 1f), 1f to (1f to 1f)
  ),
  // square4
  listOf(
    0f to (2f to 1f), 0.3333f to (2f to 1f), 0.4167f to (2f to 2f),
    0.5f to (3f to 2f), 0.5833f to (3f to 1f), 1f to (3f to 1f)
  ),
  // square5
  listOf(
    0f to (3f to 1f), 0.5f to (3f to 1f), 0.5833f to (3f to 0f),
    0.6667f to (2f to 0f), 0.75f to (2f to 1f), 1f to (2f to 1f)
  )
)

private fun samplePath(stops: PathStops, t: Float): Pair<Float, Float> {
  if (t <= stops.first().first) return stops.first().second
  for (i in 1 until stops.size) {
    val (t1, p1) = stops[i - 1]
    val (t2, p2) = stops[i]
    if (t <= t2) {
      val span = (t2 - t1).takeIf { it > 0f } ?: 1f
      val f = ((t - t1) / span).coerceIn(0f, 1f)
      return (p1.first + (p2.first - p1.first) * f) to (p1.second + (p2.second - p1.second) * f)
    }
  }
  return stops.last().second
}
