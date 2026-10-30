package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.repository.FavoriteRepository
import com.example.domain.model.ShootType
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LensCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecRed
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Top App Bar with clean, cinematic branding and discreet user identity badge
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FameBookTopBar(
  title: String? = null,
  subtitle: String? = null,
  currentUser: User? = null,
  onBackClick: (() -> Unit)? = null,
  onNotificationClick: (() -> Unit)? = null
) {
  TopAppBar(
    title = {
      if (title != null) {
        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = PureWhite,
            maxLines = 1
          )
          if (subtitle != null) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
          }
        }
      } else {
        Image(
          painter = painterResource(id = R.drawable.famebook_logo),
          contentDescription = "FameBook",
          modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
        )
      }
    },
    navigationIcon = {
      if (onBackClick != null) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.testTag("top_bar_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = PureWhite
          )
        }
      }
    },
    actions = {
      if (onNotificationClick != null) {
        IconButton(
          onClick = onNotificationClick,
          modifier = Modifier.testTag("top_bar_notification_button")
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      if (currentUser != null) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = DarkCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.testTag("top_bar_user_pill")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                  when (currentUser.role) {
                    UserRole.CLIENT -> LensCyan
                    UserRole.CREW -> EmeraldSuccess
                    UserRole.ADMIN -> AmberGold
                  }
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = currentUser.name.split(" ").first(),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = PureWhite
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = ObsidianBlack,
      titleContentColor = PureWhite
    )
  )
}

/**
 * Cinematic Hero Section (Section 8)
 * Confident visual statement with restrained neumorphic [ BOOK A SHOOT ] action
 */
@Composable
fun HeroSection(
  onBookShootClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = Color(0xFF141720),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x26FFFFFF)),
    shadowElevation = 14.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("cinematic_hero_section")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF1D212E),
              Color(0xFF141720),
              Color(0xFF0D0F15)
            )
          )
        )
        .padding(26.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        // Dispatch status pill
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1AFFB800))
            .border(1.dp, AmberGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(EmeraldSuccess)
          )
          Spacer(modifier = Modifier.width(7.dp))
          Text(
            text = "VERIFIED DISPATCH ACTIVE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = AmberGold
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "READY FOR YOUR\nNEXT SHOOT?",
          style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Black,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
          ),
          color = PureWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Book professional FameBros crew in seconds.",
          style = MaterialTheme.typography.bodyLarge,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryGoldButton(
          text = "BOOK A SHOOT",
          onClick = onBookShootClick,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_book_shoot_button")
        )
      }
    }
  }
}

@Composable
private fun SpecTag(
  icon: ImageVector,
  label: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = AmberGold,
      modifier = Modifier.size(13.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
      color = TextPrimary
    )
  }
}

/**
 * Section Header with confident typography and optional action
 */
@Composable
fun SectionHeader(
  title: String,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = PureWhite
    )

    if (actionText != null && onActionClick != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onActionClick() }
      ) {
        Text(
          text = actionText,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = AmberGold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          tint = AmberGold,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

/**
 * Visual Shoot Category Card for horizontal discovery (Production Deck / Streaming App style)
 */
@Composable
fun ShootCategoryCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "cat_card_scale"
  )

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color(0xFF141720),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EFFFFFF)),
    shadowElevation = 8.dp,
    modifier = modifier
      .width(172.dp)
      .height(132.dp)
      .scale(scale)
      .clickable(
        interactionSource = interactionSource,
        indication = null
      ) { onClick() }
      .testTag("category_card_${title.lowercase()}")
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF202534),
              Color(0xFF141722),
              Color(0xFF0D0F16)
            )
          )
        )
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x26FFB800))
              .border(1.dp, AmberGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = title,
              tint = AmberGold,
              modifier = Modifier.size(18.dp)
            )
          }

          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp)
          )
        }

        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PureWhite
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1
          )
        }
      }
    }
  }
}

/**
 * Cinematic Call Sheet / Upcoming Booking Card (Section 10)
 */
@Composable
fun UpcomingBookingCard(
  booking: Booking,
  onClick: () -> Unit,
  onOpenChat: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "upcoming_scale"
  )

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = Color(0xFF141722),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x33FFFFFF)),
    shadowElevation = 10.dp,
    modifier = modifier
      .fillMaxWidth()
      .scale(scale)
      .clickable(
        interactionSource = interactionSource,
        indication = null
      ) { onClick() }
      .testTag("upcoming_booking_card_${booking.id}")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF1C212E),
              Color(0xFF131620),
              Color(0xFF0C0E14)
            )
          )
        )
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          SimpleStatusBadge(status = booking.status)

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0x26FFB800))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = booking.date.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = AmberGold
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = booking.title,
          style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Black,
            lineHeight = 28.sp
          ),
          color = PureWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "${booking.startTime} (${booking.durationHours} hrs) • ${booking.location.name}",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )

        if (booking.assignedCrewName != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(EmeraldSuccess)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Assigned Crew: ${booking.assignedCrewName}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = PureWhite
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0x26FFB800),
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
            modifier = Modifier
              .clickable { onOpenChat(booking.id) }
              .testTag("upcoming_card_chat_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Chat with Crew",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AmberGold
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Clean Boarding Pass / Cinema Ticket Booking Item (Section 10)
 */
@Composable
fun CleanBookingItem(
  booking: Booking,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "recent_scale"
  )

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF141722),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)),
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .scale(scale)
      .clickable(
        interactionSource = interactionSource,
        indication = null
      ) { onClick() }
      .testTag("recent_booking_${booking.id}")
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left accent bar
      Box(
        modifier = Modifier
          .width(3.dp)
          .height(38.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(
            when (booking.status) {
              BookingStatus.CONFIRMED, BookingStatus.COMPLETED -> EmeraldSuccess
              BookingStatus.SEARCHING_CREW, BookingStatus.OFFERED -> AmberGold
              BookingStatus.IN_PROGRESS -> LensCyan
              else -> TextMuted
            }
          )
      )

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = booking.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = PureWhite
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "${booking.date} • ${booking.location.name}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
        if (booking.assignedCrewName != null) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Crew: ${booking.assignedCrewName}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = AmberGold
          )
        }
      }

      SimpleStatusBadge(status = booking.status)

      Spacer(modifier = Modifier.width(10.dp))

      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = TextMuted,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

/**
 * Minimal Status Badge without excess noise
 */
@Composable
fun SimpleStatusBadge(status: BookingStatus) {
  val (color, label) = when (status) {
    BookingStatus.PENDING -> TextSecondary to "PENDING"
    BookingStatus.SEARCHING_CREW -> AmberGold to "FINDING CREW"
    BookingStatus.OFFERED -> AmberGold to "CREW ALERTED"
    BookingStatus.CONFIRMED -> EmeraldSuccess to "CONFIRMED"
    BookingStatus.IN_PROGRESS -> LensCyan to "IN PRODUCTION"
    BookingStatus.COMPLETED -> EmeraldSuccess to "COMPLETED"
    BookingStatus.CANCELLED -> RecRed to "CANCELLED"
  }

  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = color
    )
  }
}

/**
 * Primary Amber-Gold Button with restrained neumorphic depth and tactile compression
 */
@Composable
fun PrimaryGoldButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "button_scale"
  )

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = AmberGold,
    shadowElevation = if (enabled) 10.dp else 0.dp,
    modifier = modifier
      .scale(scale)
      .height(52.dp)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled
      ) { onClick() }
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFFFFD54F), // Subtle top highlight
              AmberGold,
              Color(0xFFE5A100) // Rich golden bottom base
            )
          )
        )
        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
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
 * Secondary Dark Button with tactile feel and crisp border
 */
@Composable
fun SecondaryDarkButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "sec_button_scale"
  )

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF181B26),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x33FFFFFF)),
    shadowElevation = 6.dp,
    modifier = modifier
      .scale(scale)
      .height(52.dp)
      .clickable(
        interactionSource = interactionSource,
        indication = null
      ) { onClick() }
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF222634),
              Color(0xFF161822)
            )
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.6.sp
        ),
        color = PureWhite
      )
    }
  }
}

/**
 * Contextual Empty State
 */
@Composable
fun EmptyState(
  title: String,
  message: String,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = DarkCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = PureWhite,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      if (actionText != null && onActionClick != null) {
        Spacer(modifier = Modifier.height(18.dp))
        PrimaryGoldButton(
          text = actionText,
          onClick = onActionClick
        )
      }
    }
  }
}

// Backwards compatibility alias for components used elsewhere
@Composable
fun StatusChip(status: BookingStatus) = SimpleStatusBadge(status)

/**
 * Heart toggle that saves a crew member to the client's favorites list.
 * Client-side only; nothing renders without a crew id.
 */
@Composable
fun FavoriteHeartButton(
  clientId: String,
  crewUserId: String?,
  favoriteRepository: FavoriteRepository,
  modifier: Modifier = Modifier
) {
  if (crewUserId.isNullOrBlank()) return
  val scope = rememberCoroutineScope()
  var isFavorite by remember(clientId, crewUserId) { mutableStateOf(false) }
  LaunchedEffect(clientId, crewUserId) {
    isFavorite = runCatching { favoriteRepository.isFavorite(clientId, crewUserId) }.getOrDefault(false)
  }
  IconButton(
    onClick = {
      scope.launch {
        favoriteRepository.toggleFavorite(clientId, crewUserId).onSuccess { isFavorite = it }
      }
    },
    modifier = modifier.testTag("favorite_heart_$crewUserId")
  ) {
    FavoriteHeartIcon(isFavorite = isFavorite)
  }
}

@Composable
private fun FavoriteHeartIcon(isFavorite: Boolean) {
  Icon(
    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
    contentDescription = if (isFavorite) "Remove from favorites" else "Save to favorites",
    tint = if (isFavorite) RecRed else TextSecondary
  )
}

/**
 * Single shared slow pulse per screen for tiny accent glows (dots, LIVE
 * badges). Applied through graphicsLayer so it redraws without recomposing.
 * One transition drives every accent on the screen: effectively free.
 */
@Composable
fun rememberAlivePulse(): State<Float> {
  val transition = rememberInfiniteTransition(label = "alive")
  return transition.animateFloat(
    initialValue = 0.55f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(2600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alive_pulse"
  )
}

fun Modifier.alivePulse(pulse: Float): Modifier = graphicsLayer {
  alpha = 0.55f + 0.45f * pulse
}

/**
 * Light one-shot entrance for list cards: a subtle rise + fade, staggered by
 * index so lists feel alive. Runs once per composition with no ongoing cost.
 */
@Composable
fun CardEntrance(
  index: Int = 0,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { visible = true }
  val stagger = (index * 70).coerceAtMost(420)
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(300, delayMillis = stagger)) +
      slideInVertically(animationSpec = tween(300, delayMillis = stagger)) { it / 10 },
    modifier = modifier
  ) {
    content()
  }
}
