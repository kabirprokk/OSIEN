package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssignedCrewMember
import com.example.model.Booking
import com.example.model.BookingStatus
import com.example.model.CrewProfile
import com.example.model.Role
import com.example.model.VerificationStatus
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoBrightGold
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoDarkerGold
import com.example.ui.theme.FameGoGlassBg
import com.example.ui.theme.FameGoGlassBorder
import com.example.ui.theme.FameGoGlassHighlight
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoLiveRed
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoSurface
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import com.example.R
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// =============================================================================
// 1. THE FLOW PILL (Signature interactive pill morphing near bottom of screen)
// =============================================================================

enum class FlowPillState {
  BOOK_A_SHOOT,
  CONTINUE,
  FINDING_CREW,
  CREW_CONFIRMED,
  SHOOT_IN_PROGRESS,
  BOOK_AGAIN,
  CUSTOM
}

@Composable
fun FlowPill(
  state: FlowPillState = FlowPillState.BOOK_A_SHOOT,
  customText: String? = null,
  customIcon: ImageVector? = null,
  onClick: () -> Unit,
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
  testTag: String = "flow_pill"
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1.0f,
    animationSpec = spring(stiffness = 500f),
    label = "flowPillScale"
  )

  val (text, icon, isGold, isLoading) = when (state) {
    FlowPillState.BOOK_A_SHOOT -> Quadruple(customText ?: "Book a shoot", Icons.Default.Add, true, false)
    FlowPillState.CONTINUE -> Quadruple(customText ?: "Continue", Icons.AutoMirrored.Filled.ArrowForward, true, false)
    FlowPillState.FINDING_CREW -> Quadruple(customText ?: "Finding crew...", null, false, true)
    FlowPillState.CREW_CONFIRMED -> Quadruple(customText ?: "Crew confirmed", Icons.Default.Check, false, false)
    FlowPillState.SHOOT_IN_PROGRESS -> Quadruple(customText ?: "Shoot in progress", null, false, false)
    FlowPillState.BOOK_AGAIN -> Quadruple(customText ?: "Book again", Icons.Default.Refresh, true, false)
    FlowPillState.CUSTOM -> Quadruple(customText ?: "Confirm", customIcon, true, false)
  }

  val backgroundColor by animateColorAsState(
    targetValue = when {
      !enabled -> FameGoCard
      isGold -> FameGoGold
      state == FlowPillState.CREW_CONFIRMED -> Color(0xFF142E20)
      state == FlowPillState.SHOOT_IN_PROGRESS -> Color(0xFF1F1B12)
      else -> FameGoCardElevated
    },
    animationSpec = tween(250),
    label = "pillBgColor"
  )

  val contentColor by animateColorAsState(
    targetValue = when {
      !enabled -> FameGoTextMuted
      isGold -> FameGoBg
      state == FlowPillState.CREW_CONFIRMED -> FameGoSuccessGreen
      state == FlowPillState.SHOOT_IN_PROGRESS -> FameGoGold
      else -> FameGoTextPrimary
    },
    animationSpec = tween(250),
    label = "pillContentColor"
  )

  Surface(
    modifier = modifier
      .scale(scale)
      .clip(RoundedCornerShape(32.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled && !isLoading,
        onClick = onClick
      )
      .testTag(testTag),
    shape = RoundedCornerShape(32.dp),
    color = backgroundColor,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      when {
        isGold -> FameGoBrightGold.copy(alpha = 0.5f)
        state == FlowPillState.CREW_CONFIRMED -> FameGoSuccessGreen.copy(alpha = 0.4f)
        state == FlowPillState.SHOOT_IN_PROGRESS -> FameGoGold.copy(alpha = 0.4f)
        else -> FameGoBorderSubtle
      }
    ),
    shadowElevation = if (isGold) 6.dp else 2.dp
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 24.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (isLoading) {
        LiveOrb(color = FameGoGold, size = 10.dp)
        Spacer(modifier = Modifier.width(10.dp))
      } else if (state == FlowPillState.SHOOT_IN_PROGRESS) {
        LiveOrb(color = FameGoGold, size = 8.dp)
        Spacer(modifier = Modifier.width(10.dp))
      } else if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
      }

      Text(
        text = text,
        color = contentColor,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp
      )
    }
  }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// =============================================================================
// 2. THE FLOW DOCK (Floating Soft-Glass Navigation Dock)
// =============================================================================

@Composable
fun FlowDock(
  currentRoute: String,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    // Level 3 Floating Glass Dock
    Surface(
      shape = RoundedCornerShape(36.dp),
      color = FameGoGlassBg,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoGlassBorder),
      shadowElevation = 8.dp
    ) {
      Row(
        modifier = Modifier
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Home
        DockTabItem(
          selected = currentRoute == "home",
          icon = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
          label = "Home",
          testTag = "nav_home",
          onClick = { onNavigate("home") }
        )

        // Bookings
        DockTabItem(
          selected = currentRoute == "bookings",
          icon = Icons.Outlined.ConfirmationNumber,
          label = "Bookings",
          testTag = "nav_bookings",
          onClick = { onNavigate("bookings") }
        )

        // Center + Flow Action Button (FameGo Gold)
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(FameGoGold)
            .clickable { onNavigate("book") }
            .testTag("nav_book_a_shoot"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Book a shoot",
            tint = FameGoBg,
            modifier = Modifier.size(22.dp)
          )
        }

        // Alerts / Notifications
        DockTabItem(
          selected = currentRoute == "notifications",
          icon = if (currentRoute == "notifications") Icons.Filled.Notifications else Icons.Outlined.Notifications,
          label = "Alerts",
          testTag = "nav_notifications",
          onClick = { onNavigate("notifications") }
        )

        // Profile
        DockTabItem(
          selected = currentRoute == "profile",
          icon = if (currentRoute == "profile") Icons.Filled.Person else Icons.Outlined.Person,
          label = "Profile",
          testTag = "nav_profile",
          onClick = { onNavigate("profile") }
        )
      }
    }
  }
}

@Composable
private fun DockTabItem(
  selected: Boolean,
  icon: ImageVector,
  label: String,
  testTag: String,
  onClick: () -> Unit
) {
  val iconColor by animateColorAsState(
    targetValue = if (selected) FameGoWhite else FameGoTextMuted,
    animationSpec = tween(150),
    label = "tabIconColor"
  )

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = iconColor,
      modifier = Modifier.size(20.dp)
    )
  }
}

// Backward-compatible alias
@Composable
fun FameGoBottomNav(
  currentRoute: String,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  FlowDock(currentRoute = currentRoute, onNavigate = onNavigate, modifier = modifier)
}

// =============================================================================
// 3. ADAPTIVE HEADER (Clean, Minimalist Top Area)
// =============================================================================

@Composable
fun FameGoLogo(
  modifier: Modifier = Modifier,
  contentDescription: String = "FameGo"
) {
  androidx.compose.foundation.Image(
    painter = painterResource(R.drawable.famego_logo),
    contentDescription = contentDescription,
    contentScale = ContentScale.Fit,
    modifier = modifier.heightIn(min = 28.dp)
  )
}

@Composable
fun FameGoWordmark(modifier: Modifier = Modifier) {
  Text(
    text = "FameGo",
    color = FameGoWhite,
    fontFamily = FontFamily(Font(R.font.megrim_regular)),
    fontSize = 20.sp,
    letterSpacing = 0.4.sp,
    modifier = modifier
  )
}

@Composable
fun AdaptiveHeader(
  unreadNotifications: Int = 0,
  onNotificationsClick: () -> Unit,
  onProfileClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    FameGoWordmark(modifier = Modifier.width(92.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Notifications button with unread badge
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(FameGoCard)
          .border(1.dp, FameGoBorderSubtle, CircleShape)
          .clickable { onNotificationsClick() }
          .testTag("top_bar_notifications_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Notifications,
          contentDescription = "Notifications",
          tint = FameGoTextSecondary,
          modifier = Modifier.size(16.dp)
        )
        if (unreadNotifications > 0) {
          // Gentle breathing halo — the only motion in the header, so
          // new alerts catch the eye without animating the whole bar.
          val badgePulse = rememberInfiniteTransition(label = "badgePulse")
          val badgeGlow by badgePulse.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
              animation = tween(1600, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse
            ),
            label = "badgeGlow"
          )
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(14.dp)
              .clip(CircleShape)
              .background(FameGoGold.copy(alpha = badgeGlow)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (unreadNotifications > 9) "9+" else "$unreadNotifications",
              color = FameGoBg,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Profile Avatar button
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(FameGoCard)
          .border(1.dp, FameGoBorderSubtle, CircleShape)
          .clickable { onProfileClick() }
          .testTag("top_bar_profile_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = "Profile",
          tint = FameGoTextSecondary,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

// Backward-compatible alias
@Composable
fun FameGoTopBar(
  title: String = "FameGo",
  subtitle: String = "by Famebros Studio",
  unreadNotifications: Int = 0,
  onNotificationsClick: () -> Unit,
  onProfileClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  AdaptiveHeader(
    unreadNotifications = unreadNotifications,
    onNotificationsClick = onNotificationsClick,
    onProfileClick = onProfileClick,
    modifier = modifier
  )
}

// =============================================================================
// 4. SOFT CARD (Level 2 Neumorphic Surface)
// =============================================================================

@Composable
fun SoftCard(
  modifier: Modifier = Modifier,
  shape: RoundedCornerShape = RoundedCornerShape(20.dp),
  isElevated: Boolean = false,
  onClick: (() -> Unit)? = null,
  testTag: String? = null,
  content: @Composable () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val cardHaptic = LocalHapticFeedback.current
  val cardSfx = LocalContext.current.applicationContext
  val scale by animateFloatAsState(
    targetValue = if (isPressed && onClick != null) 0.985f else 1.0f,
    animationSpec = spring(stiffness = 500f),
    label = "softCardScale"
  )

  Surface(
    modifier = modifier
      .scale(scale)
      .clip(shape)
      .then(
        if (onClick != null) {
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
              FameGoHaptics.micro(cardHaptic)
              com.example.data.FameGoSfx.tap(cardSfx)
              onClick()
            }
          )
        } else Modifier
      )
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    shape = shape,
    color = if (isElevated) FameGoCardElevated else FameGoCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle),
    shadowElevation = if (isElevated) 3.dp else 1.dp
  ) {
    content()
  }
}

// =============================================================================
// 5. LIVE ORB (Gentle Pulsing Status Indicator)
// =============================================================================

@Composable
fun LiveOrb(
  color: Color = FameGoSuccessGreen,
  size: Dp = 8.dp,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
  val alpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "orbAlpha"
  )
  val haloScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "orbHaloScale"
  )

  Box(
    modifier = modifier.size(size * 2),
    contentAlignment = Alignment.Center
  ) {
    // Subtle outer halo
    Box(
      modifier = Modifier
        .size(size)
        .scale(haloScale)
        .clip(CircleShape)
        .background(color.copy(alpha = alpha * 0.25f))
    )
    // Core orb
    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(color)
    )
  }
}

// =============================================================================
// 6. ROLE COUNTER (Stacked Tactile Neumorphic Controls)
// =============================================================================

@Composable
fun RoleCounter(
  roleTitle: String,
  roleSubtitle: String,
  ratePerHour: Int,
  count: Int,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  modifier: Modifier = Modifier,
  testTagPrefix: String = "counter"
) {
  val isSelected = count > 0

  val animatedBg by animateColorAsState(
    targetValue = if (isSelected) FameGoCardElevated else FameGoCard,
    animationSpec = tween(200),
    label = "roleCounterBg"
  )
  val animatedBorder by animateColorAsState(
    targetValue = if (isSelected) FameGoGold.copy(alpha = 0.35f) else FameGoBorderSubtle,
    animationSpec = tween(200),
    label = "roleCounterBorder"
  )

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = animatedBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, animatedBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = roleTitle,
            color = if (isSelected) FameGoWhite else FameGoTextPrimary,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
          )
          if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(FameGoGold)
            )
          }
        }
        Text(
          text = "₹$ratePerHour/hr • $roleSubtitle",
          color = FameGoTextMuted,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 2.dp)
        )
      }

      // Tactile [ - ] [ Count ] [ + ] Controls
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Minus
        TactileCircleButton(
          icon = Icons.Default.Remove,
          enabled = count > 0,
          onClick = onDecrement,
          testTag = "${testTagPrefix}_decrement"
        )

        // Count number
        Text(
          text = count.toString(),
          color = if (isSelected) FameGoGold else FameGoTextMuted,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.width(24.dp),
          textAlign = TextAlign.Center
        )

        // Plus
        TactileCircleButton(
          icon = Icons.Default.Add,
          enabled = count < 5,
          isHighlight = isSelected,
          onClick = onIncrement,
          testTag = "${testTagPrefix}_increment"
        )
      }
    }
  }
}

@Composable
private fun TactileCircleButton(
  icon: ImageVector,
  enabled: Boolean,
  isHighlight: Boolean = false,
  onClick: () -> Unit,
  testTag: String
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.9f else 1.0f,
    animationSpec = spring(stiffness = 600f),
    label = "tactileBtnScale"
  )

  Box(
    modifier = Modifier
      .scale(scale)
      .size(34.dp)
      .clip(CircleShape)
      .background(
        when {
          !enabled -> FameGoSurface
          isHighlight -> FameGoGoldContainer
          else -> FameGoCardElevated
        }
      )
      .border(
        1.dp,
        if (isHighlight && enabled) FameGoGold.copy(alpha = 0.4f) else FameGoBorderSubtle,
        CircleShape
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = when {
        !enabled -> FameGoTextMuted.copy(alpha = 0.4f)
        isHighlight -> FameGoGold
        else -> FameGoTextPrimary
      },
      modifier = Modifier.size(16.dp)
    )
  }
}

// =============================================================================
// 7. STATUS CAPSULE (Crew Availability Control: OFF DUTY <-> READY FOR SHOOTS)
// =============================================================================

@Composable
fun StatusCapsule(
  isAvailable: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val haptic = LocalHapticFeedback.current
  val capsuleSfx = LocalContext.current.applicationContext
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1.0f,
    animationSpec = spring(stiffness = 500f),
    label = "capsuleScale"
  )

  Surface(
    modifier = modifier
      .scale(scale)
      .clip(RoundedCornerShape(32.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {
          FameGoHaptics.micro(haptic)
          com.example.data.FameGoSfx.pop(capsuleSfx)
          onToggle()
        }
      )
      .testTag("crew_duty_toggle"),
    shape = RoundedCornerShape(32.dp),
    color = if (isAvailable) Color(0xFF0F2618) else FameGoSurface,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isAvailable) FameGoSuccessGreen.copy(alpha = 0.4f) else FameGoBorderSubtle
    ),
    shadowElevation = if (isAvailable) 4.dp else 0.dp
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 18.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (isAvailable) {
        LiveOrb(color = FameGoSuccessGreen, size = 7.dp)
      } else {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(FameGoTextMuted)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Text(
        text = if (isAvailable) "Ready for shoots" else "Off duty",
        color = if (isAvailable) FameGoSuccessGreen else FameGoTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp
      )
    }
  }
}

// =============================================================================
// 8. LIVE CARD (Active / Upcoming Shoot Card with Live Status Orb)
// =============================================================================

@Composable
fun LiveCard(
  booking: Booking,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  SoftCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    isElevated = true,
    onClick = onClick,
    testTag = "live_shoot_card"
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      // Top status row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          LiveOrb(
            color = when (booking.status) {
              BookingStatus.IN_PROGRESS -> FameGoGold
              BookingStatus.CONFIRMED, BookingStatus.UPCOMING -> FameGoSuccessGreen
              BookingStatus.SEARCHING_CREW, BookingStatus.CREW_RESPONDED -> FameGoAccentCyan
              BookingStatus.CANCELLED -> FameGoLiveRed
              else -> FameGoTextMuted
            },
            size = 8.dp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = when (booking.status) {
              BookingStatus.IN_PROGRESS -> "Shoot in progress"
              BookingStatus.CONFIRMED -> "Confirmed"
              BookingStatus.UPCOMING -> "Upcoming"
              BookingStatus.SEARCHING_CREW -> "Finding crew"
              BookingStatus.CREW_RESPONDED -> "Crew responded"
              BookingStatus.COMPLETED -> "Completed"
              BookingStatus.CANCELLED -> "Cancelled"
              BookingStatus.DRAFT -> "Draft"
            },
            color = when (booking.status) {
              BookingStatus.IN_PROGRESS -> FameGoGold
              BookingStatus.CONFIRMED -> FameGoSuccessGreen
              else -> FameGoTextSecondary
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
          )
        }

        Text(
          text = booking.date,
          color = FameGoTextMuted,
          fontSize = 12.sp
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Title
      Text(
        text = booking.title,
        color = FameGoWhite,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )

      // Time & Location
      Text(
        text = "${booking.time} • ${booking.durationHours} hours • ${booking.location.address.substringBefore(",")}",
        color = FameGoTextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp)
      )

      // Crew brief preview
      if (booking.assignedCrew.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          booking.assignedCrew.take(3).forEach { crew ->
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(FameGoCardElevated)
                .border(1.dp, FameGoGold.copy(alpha = 0.3f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = crew.crewName.take(1),
                color = FameGoGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Text(
            text = booking.assignedCrew.joinToString(", ") { it.crewName },
            color = FameGoTextMuted,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

// =============================================================================
// 9. GESTURE CARD (Swipe Action Priority Card with Accessible Buttons)
// =============================================================================

@Composable
fun GestureRequestCard(
  title: String,
  subtitle: String,
  timeText: String,
  requestedRole: String,
  payoutText: String,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  modifier: Modifier = Modifier
) {
  SoftCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    isElevated = true,
    testTag = "crew_request_card"
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          LiveOrb(color = FameGoGold, size = 7.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "New shoot request",
            color = FameGoGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
          )
        }
        Text(
          text = payoutText,
          color = FameGoWhite,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = title,
        color = FameGoWhite,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = "$timeText • $subtitle",
        color = FameGoTextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 3.dp)
      )

      Text(
        text = "Role: $requestedRole",
        color = FameGoTextMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp)
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Clean Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Decline button
        FameGoPressable(
          onClick = onDecline,
          shape = RoundedCornerShape(16.dp),
          color = FameGoSurface,
          border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle),
          modifier = Modifier.weight(1f),
          testTag = "decline_request_button"
        ) {
          Text(
            text = "Decline",
            color = FameGoTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 12.dp)
          )
        }

        // Accept button (FameGo Gold)
        FameGoPressable(
          onClick = onAccept,
          shape = RoundedCornerShape(16.dp),
          color = FameGoGold,
          modifier = Modifier.weight(1.5f),
          testTag = "accept_request_button"
        ) {
          Text(
            text = "Accept shoot",
            color = FameGoBg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
          )
        }
      }
    }
  }
}

// =============================================================================
// 10. REUSABLE UTILITIES & BACKWARD-COMPATIBLE WRAPPERS
// =============================================================================

/**
 * Shared spring language (Apple-style fluid physics): one bounce vocabulary
 * across presses, sheets, dismissals and entrances. Stiffness in the
 * 400–600 range with slightly under-damped ratios gives the natural settle.
 */
object FameGoSprings {
  fun press() = spring<Float>(stiffness = 500f, dampingRatio = 0.7f)
  fun sheet() = spring<Float>(stiffness = 380f, dampingRatio = 0.82f)
  fun settle() = spring<Float>(stiffness = 420f, dampingRatio = 0.6f)
  fun pop() = spring<Float>(stiffness = 320f, dampingRatio = 0.65f)
}

/**
 * Haptic language (Taptic-style): micro ticks for selections and threshold
 * crossings, a deep thud for success, a rapid triple-tick stutter for
 * errors. Three distinct rhythms so fingers learn what happened.
 */
object FameGoHaptics {
  fun micro(feedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
  }

  fun success(feedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    feedback.performHapticFeedback(HapticFeedbackType.LongPress)
  }

  fun error(
    feedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    scope: kotlinx.coroutines.CoroutineScope
  ) {
    scope.launch {
      repeat(3) {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        kotlinx.coroutines.delay(70)
      }
    }
  }
}

/**
 * Horizontal swipe between bottom tabs, anywhere on the tab content.
 * Swipe left → next tab, swipe right → previous tab. Vertical scrolls and
 * taps are untouched: only a deliberate horizontal drag past the threshold
 * fires, so lists keep scrolling normally.
 */
@Composable
fun Modifier.swipeToSwitchTabs(
  onSwipeLeft: () -> Unit,
  onSwipeRight: () -> Unit,
  thresholdDp: Float = 90f
): Modifier {
  val density = LocalDensity.current
  val haptic = LocalHapticFeedback.current
  val thresholdPx = remember(density, thresholdDp) {
    with(density) { thresholdDp.dp.toPx() }
  }
  val latestLeft by rememberUpdatedState(onSwipeLeft)
  val latestRight by rememberUpdatedState(onSwipeRight)
  return pointerInput(thresholdPx) {
    var totalX = 0f
    detectHorizontalDragGestures(
      onDragStart = { totalX = 0f },
      onHorizontalDrag = { _, dragAmount -> totalX += dragAmount },
      onDragEnd = {
        if (totalX <= -thresholdPx) {
          FameGoHaptics.micro(haptic)
          latestLeft()
        } else if (totalX >= thresholdPx) {
          FameGoHaptics.micro(haptic)
          latestRight()
        }
      }
    )
  }
}

/**
 * Swipe right to go back on detail screens. Higher threshold than tab
 * switching so it never fires by accident; the on-screen back button stays
 * as the visible alternative.
 */
@Composable
fun Modifier.swipeToGoBack(
  onBack: () -> Unit,
  thresholdDp: Float = 130f
): Modifier {
  val density = LocalDensity.current
  val haptic = LocalHapticFeedback.current
  val thresholdPx = remember(density, thresholdDp) {
    with(density) { thresholdDp.dp.toPx() }
  }
  val latestBack by rememberUpdatedState(onBack)
  return pointerInput(thresholdPx) {
    var totalX = 0f
    detectHorizontalDragGestures(
      onDragStart = { totalX = 0f },
      onHorizontalDrag = { _, dragAmount -> totalX += dragAmount },
      onDragEnd = {
        if (totalX >= thresholdPx) {
          FameGoHaptics.micro(haptic)
          latestBack()
        }
      }
    )
  }
}

/**
 * Drag a full-screen sheet/card downward to dismiss it, with a live
 * follow-finger offset. The visible dismiss button stays as backup.
 */
@Composable
fun Modifier.swipeDownToDismiss(
  onDismiss: () -> Unit,
  thresholdDp: Float = 110f
): Modifier {
  val density = LocalDensity.current
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val thresholdPx = remember(density, thresholdDp) {
    with(density) { thresholdDp.dp.toPx() }
  }
  val latestDismiss by rememberUpdatedState(onDismiss)
  val offsetY = remember { Animatable(0f) }
  return this
    .offset { IntOffset(0, offsetY.value.roundToInt()) }
    .pointerInput(thresholdPx) {
      val tracker = VelocityTracker()
      detectVerticalDragGestures(
        // Velocity transfers from finger to sheet: a fast downward fling
        // dismisses even before crossing the distance threshold.
        onDragStart = { tracker.resetTracking() },
        onDragEnd = {
          val velocity = runCatching { tracker.calculateVelocity().y }.getOrDefault(0f)
          if (offsetY.value >= thresholdPx || velocity > 1400f) {
            FameGoHaptics.micro(haptic)
            latestDismiss()
          }
          scope.launch { offsetY.animateTo(0f, FameGoSprings.press()) }
        },
        onDragCancel = { scope.launch { offsetY.animateTo(0f, FameGoSprings.press()) } },
        onVerticalDrag = { change, dragAmount ->
          runCatching { tracker.addPosition(change.uptimeMillis, change.position) }
          if (dragAmount > 0) scope.launch { offsetY.snapTo((offsetY.value + dragAmount).coerceAtMost(thresholdPx * 1.6f)) }
          else scope.launch { offsetY.snapTo((offsetY.value + dragAmount).coerceAtLeast(0f)) }
        }
      )
    }
}

/**
 * Rubber-band overscroll (Apple-style elastic edges): at the very top or
 * bottom of a scrollable, content stretches with resistance and springs
 * back instead of hitting a hard wall. Wrap the scrollable's content.
 */
@Composable
fun Modifier.rubberBand(maxStretchPx: Float = 220f): Modifier {
  val scope = rememberCoroutineScope()
  val stretch = remember { Animatable(0f) }
  val connection = remember {
    object : NestedScrollConnection {
      override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
      ): Offset {
        if (available.y != 0f) {
          scope.launch {
            stretch.snapTo((stretch.value + available.y * 0.35f).coerceIn(-maxStretchPx, maxStretchPx))
          }
        }
        return Offset.Zero
      }

      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        stretch.animateTo(0f, FameGoSprings.settle())
        return super.onPostFling(consumed, available)
      }
    }
  }
  return this
    .nestedScroll(connection)
    .offset { IntOffset(0, stretch.value.roundToInt()) }
}

/**
 * One pressable primitive for every tappable surface: shrink slightly under
 * the finger, spring back on release. Replaces ad-hoc clickables so the
 * whole app shares a single tactile vocabulary.
 */
@Composable
fun FameGoPressable(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: RoundedCornerShape = RoundedCornerShape(16.dp),
  color: Color = FameGoSurface,
  border: androidx.compose.foundation.BorderStroke? = null,
  testTag: String? = null,
  content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressHaptic = LocalHapticFeedback.current
  val pressSfx = LocalContext.current.applicationContext
  val scale by animateFloatAsState(
    targetValue = if (pressed && enabled) 0.96f else 1f,
    animationSpec = FameGoSprings.press(),
    label = "pressableScale"
  )
  Surface(
    modifier = modifier
      .scale(scale)
      .clip(shape)
      .clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onClick = {
          FameGoHaptics.micro(pressHaptic)
          com.example.data.FameGoSfx.click(pressSfx)
          onClick()
        }
      )
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    shape = shape,
    color = color,
    border = border,
    content = { androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, content = content) }
  )
}

/**
 * System-style bottom sheet: slides up on a spring, dims the page behind,
 * drag handle on top, swipe-down or scrim tap to dismiss. Destructive
 * confirms stay in AlertDialogs; everything contextual lives here.
 */
@Composable
fun FameGoSheet(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
  val slide = remember { Animatable(600f) }
  val sheetSfx = LocalContext.current.applicationContext
  LaunchedEffect(Unit) {
    com.example.data.FameGoSfx.pop(sheetSfx)
    slide.animateTo(0f, FameGoSprings.sheet())
  }
  androidx.compose.ui.window.Dialog(
    onDismissRequest = onDismiss,
    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.6f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismiss
        ),
      contentAlignment = Alignment.BottomCenter
    ) {
      Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = FameGoCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle),
        shadowElevation = 24.dp,
        modifier = modifier
          .fillMaxWidth()
          .offset { IntOffset(0, slide.value.roundToInt()) }
          .swipeDownToDismiss(onDismiss = onDismiss)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {}
          )
      ) {
        Column(
          modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .width(44.dp)
              .height(4.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(FameGoTextMuted.copy(alpha = 0.5f))
          )
          Spacer(modifier = Modifier.height(12.dp))
          content()
          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }
}

/**
 * Credits & inspiration: the open motion studies and data sources this
 * app's interface is built on, restyled into the FameGo black-and-gold
 * language.
 */
@Composable
fun CreditsCard(modifier: Modifier = Modifier) {
  val credits = remember {
    listOf(
      "Snake loader" to "CSS loading-spinner study, rebuilt in gold",
      "Kinetic type loader" to "Letter-wave page transition study",
      "Spring button" to "Shine-sweep + border-beam button study",
      "Glow border card" to "Rotating conic-glow card study",
      "Gooey search" to "Morphing search-pill study",
      "Ambient aura" to "Aurora-rays canvas study, calmed down",
      "Fluid motion" to "Spring physics, haptics, rubber-band, sheets",
      "Maps & places" to "OpenStreetMap tiles + Nominatim search",
      "System sounds" to "AOSP UI sounds, Apache 2.0 license",
    )
  }
  SoftCard(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "CREDITS & INSPIRATION",
        color = FameGoTextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(10.dp))
      credits.forEach { (title, subtitle) ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(FameGoGold)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              color = FameGoWhite,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = subtitle,
              color = FameGoTextMuted,
              fontSize = 11.sp
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "OSEM A.1.1 • Famebros Studio",
        color = FameGoTextMuted,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
/**
 * Fully-offline takeover: FameGo is online-only, so a lost connection gets
 * a full screen with a retry instead of half-broken pages.
 */
@Composable
fun NoInternetScreen(
  onRetry: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FameGoBg.copy(alpha = 0.97f)),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 32.dp)
    ) {
      FameGoSnakeLoader(
        modifier = Modifier.size(width = 150.dp, height = 88.dp)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "You're offline",
        color = FameGoWhite,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
      Text(
        text = "FameGo needs the internet for bookings, chats and crew alerts. Check Wi-Fi or mobile data and retry.",
        color = FameGoTextSecondary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
      )
      Spacer(modifier = Modifier.height(24.dp))
      FameGoButton(
        text = "Retry connection",
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
        testTag = "no_internet_retry"
      )
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.padding(vertical = 8.dp)) {
    Text(
      text = title,
      color = FameGoWhite,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold
    )
    if (subtitle != null) {
      Text(
        text = subtitle,
        color = FameGoTextMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
}

@Composable
fun FameGoButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String = "primary_button"
) {
  VengeanceAnimatedButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    icon = icon,
    style = VengeanceButtonStyle.GOLD,
    testTag = testTag
  )
}

@Composable
fun FameGoOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  borderColor: Color = FameGoBorderSubtle,
  textColor: Color = FameGoTextPrimary,
  testTag: String = "outlined_button"
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = FameGoSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    modifier = modifier
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
        color = textColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
