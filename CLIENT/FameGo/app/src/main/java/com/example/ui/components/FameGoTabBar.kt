package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FameGoGlassBg
import com.example.ui.theme.FameGoGlassBorder
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoWhite

/**
 * A bottom tab entry: outline icon at rest, filled icon when selected —
 * the classic minimal tab bar, in FameGo gold.
 */
data class FameGoTab(
  val route: String,
  val label: String,
  val outlineIcon: ImageVector,
  val filledIcon: ImageVector,
  /** Signature accent (the center Book tab stays gold even at rest). */
  val accent: Boolean = false,
  val testTag: String = "tab_$route"
)

fun fameGoClientTabs() = listOf(
  FameGoTab("home", "Home", Icons.Outlined.Home, Icons.Filled.Home, testTag = "tab_home"),
  FameGoTab(
    "bookings", "Bookings",
    Icons.Outlined.ConfirmationNumber, Icons.Filled.ConfirmationNumber,
    testTag = "tab_bookings"
  ),
  FameGoTab("book", "Book", Icons.Filled.Add, Icons.Filled.Add, accent = true, testTag = "tab_book"),
  FameGoTab(
    "notifications", "Alerts",
    Icons.Outlined.Notifications, Icons.Filled.Notifications,
    testTag = "tab_notifications"
  ),
  FameGoTab(
    "profile", "Profile",
    Icons.Outlined.Person, Icons.Filled.Person,
    testTag = "tab_profile"
  )
)

fun fameGoCrewTabs() = listOf(
  FameGoTab("requests", "Requests", Icons.Outlined.ConfirmationNumber, Icons.Filled.ConfirmationNumber, testTag = "tab_requests"),
  FameGoTab(
    "bookings", "Shoots",
    Icons.Outlined.Home, Icons.Filled.Home,
    testTag = "tab_bookings"
  ),
  FameGoTab(
    "notifications", "Alerts",
    Icons.Outlined.Notifications, Icons.Filled.Notifications,
    testTag = "tab_notifications"
  ),
  FameGoTab(
    "profile", "Profile",
    Icons.Outlined.Person, Icons.Filled.Person,
    testTag = "tab_profile"
  )
)

fun fameGoAdminTabs() = listOf(
  FameGoTab("dashboard", "Dashboard", Icons.Outlined.Home, Icons.Filled.Home, testTag = "tab_dashboard"),
  FameGoTab(
    "users", "People",
    Icons.Outlined.Person, Icons.Filled.Person,
    testTag = "tab_users"
  ),
  FameGoTab(
    "notifications", "Alerts",
    Icons.Outlined.Notifications, Icons.Filled.Notifications,
    testTag = "tab_notifications"
  ),
  FameGoTab(
    "profile", "Profile",
    Icons.Outlined.Person, Icons.Outlined.Person,
    testTag = "tab_profile"
  )
)

/**
 * FameGo bottom tab bar — dark floating glass pill with a hairline top
 * highlight. Unselected tabs are muted outline icons; the active tab is a
 * filled gold icon with a bold white label. Tap a tab, or swipe left/right
 * anywhere on the tab content to move between tabs.
 */
@Composable
fun FameGoTabBar(
  tabs: List<FameGoTab>,
  selectedRoute: String,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current

  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag("famego_tab_bar"),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = FameGoGlassBg,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoGlassBorder),
      shadowElevation = 10.dp
    ) {
      Box {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          tabs.forEach { tab ->
            val selected = tab.route == selectedRoute ||
              // "book" also covers the launchpad sub-flow visually.
              (tab.route == "book" && selectedRoute == "book")
            FameGoTabItem(
              tab = tab,
              selected = selected,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(tab.route)
              },
              modifier = Modifier.weight(1f)
            )
          }
        }
        // Hairline glass highlight along the top edge (reference look).
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .size(height = 1.dp, width = 10.dp)
            .align(Alignment.TopCenter)
        ) {
          drawLine(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.35f),
                Color.Transparent
              )
            ),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height
          )
        }
      }
    }
  }
}

@Composable
private fun FameGoTabItem(
  tab: FameGoTab,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.92f else 1f,
    animationSpec = spring(
      stiffness = Spring.StiffnessMedium,
      dampingRatio = Spring.DampingRatioMediumBouncy
    ),
    label = "tabPress"
  )
  val iconTint = when {
    selected -> FameGoGold
    tab.accent -> FameGoGold.copy(alpha = 0.75f)
    else -> FameGoTextMuted
  }

  Column(
    modifier = modifier
      .scale(scale)
      .clip(RoundedCornerShape(18.dp))
      .clickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick
      )
      .semantics {
        contentDescription = tab.label
        role = Role.Tab
        this.selected = selected
      }
      .testTag(tab.testTag)
      .padding(vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = if (selected) tab.filledIcon else tab.outlineIcon,
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier.size(24.dp)
    )
    Text(
      text = tab.label,
      color = if (selected) FameGoWhite else FameGoTextMuted,
      fontSize = 11.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      letterSpacing = 0.1.sp,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(top = 3.dp)
    )
  }
}
