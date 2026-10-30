package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoDarkerGold
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// =============================================================================
// 1. VENGEANCE ANIMATED BUTTON  (port of animated-button: spring press + shine)
// Web: framer-motion whileHover 1.01 / whileTap 0.97 spring + infinite text
// shine-mask sweep + border beam (1s anim, 1s pause).
// Mobile: press spring scale + diagonal shimmer band + animated border beam.
// =============================================================================

enum class VengeanceButtonStyle {
  DARK,
  GOLD
}

@Composable
fun VengeanceAnimatedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  style: VengeanceButtonStyle = VengeanceButtonStyle.DARK,
  testTag: String = "vengeance_button"
) {
  val shape = RoundedCornerShape(12.dp)
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (pressed && enabled) 0.97f else 1f,
    animationSpec = spring(stiffness = 500f, dampingRatio = 0.7f),
    label = "vengeancePress"
  )
  // 2s loop: 1s sweep + 1s rest — mirrors the web repeatDelay.
  val shineLoop = rememberInfiniteTransition(label = "vengeanceShine")
  val shine by shineLoop.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
    label = "shineProgress"
  )
  // Sweep occupies the first half of the loop, then rests.
  val sweep = (shine * 2f).coerceIn(0f, 1f)
  val isGold = style == VengeanceButtonStyle.GOLD
  val haptic = LocalHapticFeedback.current
  val sfxContext = LocalContext.current.applicationContext
  val container = when {
    !enabled -> FameGoCard.copy(alpha = 0.6f)
    isGold -> FameGoGold
    else -> FameGoCardElevated
  }
  val content = if (!enabled) FameGoTextMuted else if (isGold) Color(0xFF1A1408) else FameGoWhite

  Box(
    modifier = modifier
      .scale(pressScale)
      .clip(shape)
      .background(container)
      .border(1.dp, if (isGold) FameGoGold else FameGoBorder, shape)
      .clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onClick = {
          FameGoHaptics.micro(haptic)
          com.example.data.FameGoSfx.click(sfxContext)
          onClick()
        }
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isGold) content else FameGoGold,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = text,
        color = content,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
        textAlign = TextAlign.Center
      )
    }
    // Traveling shimmer band clipped to the button shape.
    if (enabled) {
      BoxWithConstraints(modifier = Modifier.matchParentSize()) {
        val bandWidthPx = with(LocalDensity.current) { 64.dp.toPx() }
        val bandTall = maxHeight * 3f
        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              translationX = -bandWidthPx + sweep * (size.width + bandWidthPx * 2f)
              // Fade the band out during the rest half of the loop.
              alpha = if (shine < 0.5f) 1f else 0f
            }
        ) {
          Box(
            modifier = Modifier
              .width(64.dp)
              .height(bandTall)
              .align(Alignment.Center)
              .graphicsLayer { rotationZ = -20f }
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = if (isGold) 0.3f else 0.14f),
                    Color.Transparent
                  )
                )
              )
          )
        }
      }
      // Animated border beam sweeping with the same phase.
      Canvas(modifier = Modifier.matchParentSize()) {
        val w = size.width
        val h = size.height
        val beamX = -w * 0.2f + sweep * w * 1.4f
        drawRoundRect(
          brush = Brush.linearGradient(
            colors = listOf(
              Color.Transparent,
              (if (isGold) Color.White else FameGoGold).copy(alpha = 0.85f),
              Color.Transparent
            ),
            start = Offset(beamX - w * 0.25f, 0f),
            end = Offset(beamX + w * 0.25f, h)
          ),
          cornerRadius = CornerRadius(12.dp.toPx()),
          style = Stroke(width = 1.5.dp.toPx())
        )
      }
    }
  }
}

// =============================================================================
// 2. VENGEANCE GLOW BORDER CARD  (port of glow-border-card: rotating conic glow)
// Web: rotating conic-gradient glow ring (blurred, oversized, clipped) under a
// glass card. Mobile: oversized rotating sweep-gradient disc under an inner
// cover — the rim shows a traveling aurora glow. No API-31 blur dependency.
// =============================================================================

enum class VengeanceGlowPreset(val colors: List<Color>) {
  NATURE(
    listOf(
      Color(0xFF669900), Color(0xFF99CC33), Color(0xFFCCEE66),
      Color(0xFF006699), Color(0xFF3399CC)
    )
  ),
  OCEAN(
    listOf(
      Color(0xFF006699), Color(0xFF3399CC), Color(0xFF55BBEE),
      Color(0xFF66CCFF), Color(0xFF2299CC)
    )
  ),
  SUNSET(
    listOf(
      Color(0xFFFF6600), Color(0xFFFF9900), Color(0xFFFFCC00),
      Color(0xFFFF9933), Color(0xFFFF7722)
    )
  ),
  AURORA(
    listOf(
      Color(0xFF00FF87), Color(0xFF60EFFF), Color(0xFFBB99FF),
      Color(0xFFFF68F0), Color(0xFFFF55CC)
    )
  ),
  GOLD(
    listOf(
      FameGoGold, Color(0xFFFFC857), FameGoAccentCyan,
      FameGoGold, Color(0xFFD4972B)
    )
  );
}

@Composable
fun VengeanceGlowCard(
  modifier: Modifier = Modifier,
  preset: VengeanceGlowPreset = VengeanceGlowPreset.GOLD,
  gradientColors: List<Color>? = null,
  cornerRadius: Dp = 12.dp,
  animationDurationMs: Int = 4000,
  glowWidth: Dp = 2.dp,
  glowAlpha: Float = 0.75f,
  paused: Boolean = false,
  content: @Composable BoxScope.() -> Unit
) {
  val shape = RoundedCornerShape(cornerRadius)
  val base = gradientColors?.takeIf { it.size >= 2 } ?: preset.colors
  // Sweep needs first == last for a seamless loop.
  val loop = remember(base) { base + base.first() }
  val spin = rememberInfiniteTransition(label = "glowSpin")
  val angle by spin.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(animationDurationMs, easing = LinearEasing)),
    label = "glowAngle"
  )
  val rotation = if (paused) 0f else angle

  Box(
    modifier = modifier
      .clip(shape)
      .background(FameGoCard)
      .testTag("vengeance_glow_card"),
    contentAlignment = Alignment.Center
  ) {
    // Oversized rotating aurora disc; the inner cover leaves only the rim visible.
    Box(
      modifier = Modifier
        .matchParentSize()
        .fillMaxSize(1.7f)
        .graphicsLayer { rotationZ = rotation }
        .background(Brush.sweepGradient(loop), alpha = glowAlpha)
    )
    Box(
      modifier = Modifier
        .matchParentSize()
        .padding(glowWidth)
        .clip(RoundedCornerShape((cornerRadius.value - glowWidth.value).coerceAtLeast(0f).dp))
        .background(FameGoCard.copy(alpha = 0.94f)),
      contentAlignment = Alignment.Center,
      content = content
    )
  }
}

// =============================================================================
// 3. VENGEANCE FAQ ACCORDION  (port of faq-accordion: single-expand list)
// Web: plus/minus, left rail highlight, grid-rows expand. Mobile: gold rail
// when open + AnimatedVisibility expand.
// =============================================================================

data class VengeanceFaqItem(
  val question: String,
  val answer: String
)

@Composable
fun VengeanceFaqAccordion(
  items: List<VengeanceFaqItem>,
  modifier: Modifier = Modifier,
  title: String? = null,
  testTag: String = "vengeance_faq"
) {
  var expandedIndex by remember { mutableStateOf<Int?>(null) }

  Column(modifier = modifier.testTag(testTag)) {
    if (!title.isNullOrBlank()) {
      Text(
        text = title,
        color = FameGoTextSecondary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
      )
    }
    items.forEachIndexed { index, item ->
      val expanded = expandedIndex == index
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = if (expanded) 0.dp else 0.dp)
          .border(
            width = 1.dp,
            color = if (expanded) FameGoGold.copy(alpha = 0.45f) else FameGoBorder,
            shape = RoundedCornerShape(14.dp)
          )
          .clip(RoundedCornerShape(14.dp))
          .background(if (expanded) FameGoGold.copy(alpha = 0.06f) else FameGoCard)
          .clickable(
            role = Role.Tab,
            onClick = { expandedIndex = if (expanded) null else index }
          )
          .semantics {
            contentDescription = "FAQ: ${item.question}"
            role = Role.Tab
          }
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (expanded) "−" else "+",
            color = if (expanded) FameGoGold else FameGoTextMuted,
            fontSize = if (expanded) 26.sp else 22.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.width(28.dp)
          )
          Text(
            text = item.question,
            color = if (expanded) FameGoWhite else FameGoTextPrimary,
            fontSize = 15.sp,
            fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
          )
          Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = if (expanded) FameGoGold else FameGoTextMuted,
            modifier = Modifier
              .size(20.dp)
              .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
          )
        }
        AnimatedVisibility(
          visible = expanded,
          enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(240)),
          exit = shrinkVertically(tween(240)) + fadeOut(tween(180))
        ) {
          Text(
            text = item.answer,
            color = FameGoTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 28.dp, end = 4.dp, top = 8.dp)
          )
        }
      }
      if (index != items.lastIndex) Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

// =============================================================================
// 4. VENGEANCE KINETIC LOADER  (port of kinetic-text-loader + page transition)
// Web: letters squash/stretch on an 1800ms wave + orbiting dot. Mobile: one
// shared 1800ms progress drives per-letter phase-shifted scaleY + dot orbit.
// Plus a full-screen transition overlay for mid-page loading states.
// =============================================================================

@Composable
fun VengeanceKineticLoader(
  text: String = "Loading",
  modifier: Modifier = Modifier,
  color: Color = FameGoWhite,
  dotColor: Color = FameGoGold,
  testTag: String = "vengeance_loader"
) {
  val loop = rememberInfiniteTransition(label = "kineticLoop")
  val progress by loop.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
    label = "kineticProgress"
  )
  val letters = remember(text) { text.toList() }

  Box(
    modifier = modifier.testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    // Orbiting dot tracing an ellipse above the word.
    val dotR = with(LocalDensity.current) { 26.dp.toPx() }
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f + cos((progress * 2 * PI).toFloat()) * dotR * 2.2f
      val cy = size.height * 0.18f + sin((progress * 2 * PI).toFloat()) * dotR * 0.5f
      drawCircle(color = dotColor, radius = 3.dp.toPx(), center = Offset(cx, cy))
    }
    Row(verticalAlignment = Alignment.Bottom) {
      letters.forEachIndexed { index, ch ->
        // Phase-shifted wave: letters squash & stretch in sequence.
        val phase = (progress * 2f - index * 0.09f) % 1f
        val wave = sin(phase * 2f * PI.toFloat())
        val scaleY = 1f + 0.32f * wave
        Text(
          text = ch.toString(),
          color = color,
          fontSize = 34.sp,
          fontWeight = FontWeight.Light,
          letterSpacing = 4.sp,
          modifier = Modifier.graphicsLayer {
            this.scaleY = scaleY
            transformOrigin = TransformOrigin(0.5f, 1f)
          }
        )
      }
    }
  }
}

@Composable
fun VengeanceTransitionOverlay(
  visible: Boolean,
  text: String = "Loading",
  subText: String? = null,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(240)) + scaleIn(
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
      initialScale = 0.94f
    ),
    exit = fadeOut(tween(200)),
    modifier = modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(FameGoBg.copy(alpha = 0.92f)),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Signature snake above the kinetic word — same overlay, new rhythm.
        FameGoSnakeLoader(
          modifier = Modifier.size(width = 150.dp, height = 88.dp)
        )
        VengeanceKineticLoader(
          text = text,
          modifier = Modifier.size(width = 260.dp, height = 96.dp)
        )
        // Gold hairline under the wordmark.
        Box(
          modifier = Modifier
            .width(64.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
              Brush.horizontalGradient(
                listOf(Color.Transparent, FameGoGold, Color.Transparent)
              )
            )
        )
        if (!subText.isNullOrBlank()) {
          Text(
            text = subText,
            color = FameGoTextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
          )
        }
      }
    }
  }
}

// =============================================================================
// 5. VENGEANCE GOOEY SEARCH  (port of gooey-search: morphing pill + results)
// Web: collapsed pill morphs into a field; results spring out with a gooey
// blur filter. Mobile: spring width morph + staggered spring-in result pills
// (SVG goo filters don't exist on Android — springs give the same feel).
// =============================================================================

@Composable
fun VengeanceGooeySearch(
  modifier: Modifier = Modifier,
  items: List<String> = emptyList(),
  onSearch: (suspend (String) -> List<String>)? = null,
  placeholder: String = "Type to search...",
  buttonLabel: String = "Search",
  onSelect: (String) -> Unit = {},
  /** Live query stream (including "") so hosts can filter their own lists. */
  onQueryChange: (String) -> Unit = {},
  debounceMs: Long = 500,
  maxResults: Int = 5,
  testTag: String = "vengeance_search"
) {
  var expanded by remember { mutableStateOf(false) }
  var query by remember { mutableStateOf("") }
  var results by remember { mutableStateOf(emptyList<String>()) }
  var loading by remember { mutableStateOf(false) }

  // Debounced search — LaunchedEffect auto-cancels the previous run.
  LaunchedEffect(query) {
    onQueryChange(query)
    if (query.isBlank()) {
      results = emptyList()
      loading = false
      return@LaunchedEffect
    }
    delay(debounceMs)
    loading = true
    results = try {
      val found = onSearch?.invoke(query.trim())
        ?: items.filter { it.contains(query.trim(), ignoreCase = true) }
      found.take(maxResults)
    } catch (_: Exception) {
      emptyList()
    }
    loading = false
  }

  // Responsive: never wider than the screen (small phones, split-screen,
  // large font scales). Expanded pill fills available width, capped.
  BoxWithConstraints(
    modifier = modifier.testTag(testTag),
    contentAlignment = Alignment.TopCenter
  ) {
    val wide = (maxWidth - 32.dp).coerceAtLeast(200.dp).coerceAtMost(420.dp)
    val collapsed = 124.dp.coerceAtMost(wide)
    val targetWidth = if (expanded) wide else collapsed
    val responsiveWidth by animateDpAsState(
      targetValue = targetWidth,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.7f),
      label = "gooeyWidth"
    )
    GooeySearchBody(
      expanded = expanded,
      onExpand = { expanded = true },
      query = query,
      onQueryChange = { query = it },
      onClear = { query = ""; results = emptyList() },
      results = results,
      loading = loading,
      placeholder = placeholder,
      buttonLabel = buttonLabel,
      onSelect = onSelect,
      pillWidth = responsiveWidth,
      resultsWidth = wide
    )
  }
}

@Composable
private fun GooeySearchBody(
  expanded: Boolean,
  onExpand: () -> Unit,
  query: String,
  onQueryChange: (String) -> Unit,
  onClear: () -> Unit,
  results: List<String>,
  loading: Boolean,
  placeholder: String,
  buttonLabel: String,
  onSelect: (String) -> Unit,
  pillWidth: Dp,
  resultsWidth: Dp
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Surface(
      shape = RoundedCornerShape(999.dp),
      color = FameGoGold,
      shadowElevation = 8.dp,
      modifier = Modifier
        .width(pillWidth)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = { if (!expanded) onExpand() }
        )
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!expanded) {
          Text(
            text = buttonLabel,
            color = Color(0xFF1A1408),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
          )
        } else {
          BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
              color = Color(0xFF1A1408),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            ),
            decorationBox = { inner ->
              if (query.isEmpty()) {
                Text(text = placeholder, color = Color(0xFF1A1408).copy(alpha = 0.55f), fontSize = 14.sp)
              }
              inner()
            },
            modifier = Modifier
              .weight(1f)
              .testTag("vengeance_search_input")
          )
          if (query.isNotEmpty()) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear search",
              tint = Color(0xFF1A1408),
              modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onClear)
                .padding(5.dp)
            )
          }
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(androidx.compose.foundation.shape.CircleShape)
              .background(Color(0xFF1A1408)),
            contentAlignment = Alignment.Center
          ) {
            if (loading) {
              CircularProgressIndicator(
                color = FameGoGold,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
              )
            } else {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Searching",
                tint = FameGoGold,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }

    // Staggered spring-in results.
    results.forEachIndexed { index, item ->
      AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(280, delayMillis = index * 90)) +
          expandVertically(
            animationSpec = tween(380, delayMillis = index * 90, easing = FastOutSlowInEasing)
          ) +
          scaleIn(
            animationSpec = spring(
              stiffness = Spring.StiffnessMediumLow,
              dampingRatio = 0.6f
            )
          ),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(160))
      ) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = FameGoCardElevated,
          border = androidx.compose.foundation.BorderStroke(1.dp, FameGoGold.copy(alpha = 0.35f)),
          shadowElevation = 4.dp,
          modifier = Modifier
            .padding(top = 8.dp)
            .width(resultsWidth)
            .clickable { onSelect(item) }
            .testTag("vengeance_search_result_$index")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = FameGoGold,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = item,
              color = FameGoTextPrimary,
              fontSize = 14.sp,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

// =============================================================================
// 6. VENGEANCE RAYS BACKGROUND  (port of animated-rays: aurora stripes bg)
// Web: repeating-linear-gradient stripes + rainbow, blurred, radial-masked at
// top-right, animated background-position. Mobile: Canvas diagonal rainbow
// bands + stripe overlay with slow drift, faded to the FameGo canvas.
// Use as the root background of any screen: content goes in the slot.
// =============================================================================

@Composable
fun VengeanceRaysBackground(
  modifier: Modifier = Modifier,
  driftDurationMs: Int = 14000,
  intensity: Float = 0.5f,
  content: @Composable BoxScope.() -> Unit
) {
  val driftLoop = rememberInfiniteTransition(label = "raysDrift")
  val drift by driftLoop.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(driftDurationMs, easing = LinearEasing)),
    label = "raysProgress"
  )

  Box(modifier = modifier.background(FameGoBg)) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      // Slow horizontal drift of the whole aurora field.
      val shift = (drift - 0.5f) * w * 0.35f
      withTransform({
        translate(left = shift, top = 0f)
        rotate(degrees = 25f, pivot = Offset(w / 2f, h / 2f))
      }) {
        // Rainbow bands (web: #60a5fa → #e879f9 → #60a5fa → #5eead4 → #60a5fa).
        val rainbow = listOf(
          Color(0xFF60A5FA), Color(0xFFE879F9), Color(0xFF60A5FA),
          Color(0xFF5EEAD4), Color(0xFF60A5FA)
        )
        val bandW = w * 0.28f
        var x = -w
        var band = 0
        while (x < w * 2f) {
          drawRect(
            color = rainbow[band % rainbow.size].copy(alpha = 0.16f * intensity),
            topLeft = Offset(x, -h),
            size = androidx.compose.ui.geometry.Size(bandW, h * 3f)
          )
          x += bandW
          band++
        }
        // Thin bright stripes over the rainbow (web repeating stripes).
        val stripeW = w * 0.045f
        var sx = -w
        while (sx < w * 2f) {
          drawRect(
            color = Color.White.copy(alpha = 0.05f * intensity),
            topLeft = Offset(sx, -h),
            size = androidx.compose.ui.geometry.Size(stripeW, h * 3f)
          )
          sx += stripeW * 3.2f
        }
      }
      // Radial mask fading to the canvas (web: ellipse at 100% 0%).
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(Color.Transparent, FameGoBg.copy(alpha = 0.55f), FameGoBg),
          center = Offset(w, 0f),
          radius = maxOf(w, h) * 1.05f
        )
      )
      // Bottom fade so foreground content stays readable.
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(Color.Transparent, FameGoBg.copy(alpha = 0.85f)),
          startY = h * 0.35f,
          endY = h
        )
      )
    }
    content()
  }
}

private val AmbientRest = Offset(0.82f, 0.10f)

// =============================================================================
// 7. FAMEGO AMBIENT BACKGROUND — signature app canvas (replaces the rainbow
// rays). Deep black with a slow-breathing gold aura, a faint ember glow, and
// a soft light pool that eases toward every tap, then settles back to rest.
// Interactive, but calm and minimalist — one instance at the app root.
// =============================================================================

@Composable
fun FameGoAmbientBackground(
  modifier: Modifier = Modifier,
  intensity: Float = 1f,
  content: @Composable BoxScope.() -> Unit
) {
  BoxWithConstraints(
    modifier = modifier.background(FameGoBg)
  ) {
    val density = LocalDensity.current
    val wPx = remember(maxWidth) { with(density) { maxWidth.toPx() } }
    val hPx = remember(maxHeight) { with(density) { maxHeight.toPx() } }

    // Light-pool target (fractional). Rests top-right; glides toward taps.
    var target by remember { mutableStateOf(AmbientRest) }
    val poolX = remember { Animatable(AmbientRest.x) }
    val poolY = remember { Animatable(AmbientRest.y) }
    LaunchedEffect(target) {
      launch { poolX.animateTo(target.x, spring(stiffness = 120f, dampingRatio = 0.85f)) }
      launch { poolY.animateTo(target.y, spring(stiffness = 120f, dampingRatio = 0.85f)) }
    }
    // Settle back to rest a few seconds after a touch.
    LaunchedEffect(target) {
      if (target != AmbientRest) {
        delay(3500)
        target = AmbientRest
      }
    }

    val driftLoop = rememberInfiniteTransition(label = "ambientDrift")
    val drift by driftLoop.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
      label = "ambientProgress"
    )

    // Tap observer — never consumes, so every button/scroll keeps working.
    Box(
      modifier = Modifier
        .matchParentSize()
        .pointerInput(wPx, hPx) {
          detectTapGestures(onPress = { pos ->
            if (wPx > 0f && hPx > 0f) {
              target = Offset(
                (pos.x / wPx).coerceIn(0f, 1f),
                (pos.y / hPx).coerceIn(0f, 1f)
              )
            }
            tryAwaitRelease()
          })
        }
    )
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      val sway = sin(drift * 2f * PI.toFloat())
      // 1. Resting gold aura, top-right, breathing almost imperceptibly.
      val auraX = w * (0.85f + sway * 0.03f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(FameGoGold.copy(alpha = 0.11f * intensity), Color.Transparent),
          center = Offset(auraX, h * 0.05f),
          radius = w * 0.8f
        ),
        radius = w * 0.8f,
        center = Offset(auraX, h * 0.05f)
      )
      // 2. Faint ember glow, bottom-left, for depth.
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(FameGoDarkerGold.copy(alpha = 0.08f * intensity), Color.Transparent),
          center = Offset(w * 0.08f, h * 0.95f),
          radius = w * 0.65f
        ),
        radius = w * 0.65f,
        center = Offset(w * 0.08f, h * 0.95f)
      )
      // 3. Touch light pool gliding on its spring.
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(FameGoGold.copy(alpha = 0.13f * intensity), Color.Transparent),
          center = Offset(poolX.value * w, poolY.value * h),
          radius = size.minDimension * 0.6f
        ),
        radius = size.minDimension * 0.6f,
        center = Offset(poolX.value * w, poolY.value * h)
      )
      // 4. Vignette — keeps focus centre-stage, edges fall to black.
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
          center = Offset(w / 2f, h * 0.45f),
          radius = maxOf(w, h) * 0.75f
        )
      )
    }
    content()
  }
}
