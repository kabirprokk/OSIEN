package com.example.presentation.client.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.ShootType
import com.example.domain.model.User
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.CrewRepository
import com.example.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.first
import com.example.presentation.components.CardEntrance
import com.example.presentation.components.alivePulse
import com.example.presentation.components.rememberAlivePulse
import com.example.presentation.components.CleanBookingItem
import com.example.presentation.components.HeroSection
import com.example.presentation.components.PrimaryGoldButton
import com.example.presentation.components.SectionHeader
import com.example.presentation.components.ShootCategoryCard
import com.example.presentation.components.SimpleStatusBadge
import com.example.presentation.components.UpcomingBookingCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class VisualCategory(
  val type: ShootType,
  val title: String,
  val subtitle: String,
  val icon: ImageVector
)

@Composable
fun ClientHomeScreen(
  currentUser: User,
  bookingRepository: BookingRepository,
  crewRepository: CrewRepository,
  favoriteRepository: FavoriteRepository,
  onBookShootClick: () -> Unit,
  onActiveRequestClick: (String) -> Unit,
  onBookingClick: (String) -> Unit,
  onOpenChat: (String) -> Unit,
  onRebookClick: (String) -> Unit
) {
  val clientBookings by bookingRepository.getClientBookings(currentUser.id).collectAsState(initial = emptyList())
  val favoriteCrewIds by favoriteRepository.getFavoriteCrewIds(currentUser.id).collectAsState(initial = emptyList())
  val allCrewProfiles by crewRepository.crewProfiles.collectAsState()
  val pulse by rememberAlivePulse()

  // Warm the crew roster once so favorites resolve to names.
  LaunchedEffect(currentUser.id) {
    runCatching { crewRepository.getCrewProfileByUserId(currentUser.id).first() }
  }

  // Categories with expressive cinema and studio icons (Zero Images)
  val categories = listOf(
    VisualCategory(ShootType.FASHION_SHOOT, "Fashion", "Editorial & Lookbooks", Icons.Default.CameraAlt),
    VisualCategory(ShootType.VIDEOGRAPHY, "Videography", "4K Cinema & Commercials", Icons.Default.Videocam),
    VisualCategory(ShootType.EVENT_COVERAGE, "Live Events", "Concerts & Galas", Icons.Default.Celebration),
    VisualCategory(ShootType.PRODUCT_SHOOT, "Products", "High-End Commercials", Icons.Default.CenterFocusStrong),
    VisualCategory(ShootType.PHOTOGRAPHY, "Photography", "Portraits & Studio", Icons.Default.PhotoCamera),
    VisualCategory(ShootType.CORPORATE_SHOOT, "Corporate", "Brand & Keynotes", Icons.Default.BusinessCenter)
  )

  // Next upcoming booking (CONFIRMED, IN_PROGRESS, or active SEARCHING)
  val activeSearchBooking = clientBookings.firstOrNull { it.status == BookingStatus.SEARCHING_CREW }
  val nextUpcomingBooking = clientBookings.firstOrNull {
    it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.IN_PROGRESS
  }

  // Recent completed or historical bookings (max 3)
  val recentBookings = clientBookings.filter {
    it.id != activeSearchBooking?.id && it.id != nextUpcomingBooking?.id
  }.take(3)

  // Favorited specialists resolved against the crew roster
  val favoriteCrew = allCrewProfiles.filter { it.userId in favoriteCrewIds }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 20.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
  ) {
    // ACTIVE RADAR BANNER (Only when client has a live searching request)
    if (activeSearchBooking != null) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = androidx.compose.ui.graphics.Color(0xF2161A26),
          border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberGold.copy(alpha = 0.55f)),
          shadowElevation = 10.dp,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onActiveRequestClick(activeSearchBooking.id) }
            .testTag("active_search_alert_banner")
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                  colors = listOf(
                    AmberGold.copy(alpha = 0.12f),
                    androidx.compose.ui.graphics.Color(0x00000000)
                  )
                )
              )
              .padding(16.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(AmberGold)
                  .alivePulse(pulse)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "SEARCHING AVAILABLE CREW",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                  ),
                  color = AmberGold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = activeSearchBooking.title,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = PureWhite
                )
              }
              Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
      }
    }

    // 1. HERO SECTION
    item {
      HeroSection(
        onBookShootClick = onBookShootClick
      )
      Spacer(modifier = Modifier.height(28.dp))
    }

    // 2. UPCOMING (Single large horizontal booking card)
    if (nextUpcomingBooking != null) {
      item {
        SectionHeader(title = "UPCOMING")
        Spacer(modifier = Modifier.height(8.dp))
        UpcomingBookingCard(
          booking = nextUpcomingBooking,
          onClick = { onBookingClick(nextUpcomingBooking.id) },
          onOpenChat = onOpenChat
        )
        Spacer(modifier = Modifier.height(28.dp))
      }
    }

    // 3. YOUR CREW (Favorited specialists with one-tap rebooking)
    if (favoriteCrew.isNotEmpty()) {
      item {
        SectionHeader(title = "YOUR CREW")
        Spacer(modifier = Modifier.height(8.dp))
      }
      itemsIndexed(favoriteCrew, key = { _, crew -> crew.userId }) { index, crew ->
        val lastBookingId = remember(clientBookings, crew.userId) {
          clientBookings.firstOrNull { it.assignedCrewId == crew.userId }?.id
        }
        CardEntrance(index = index) {
          FavoriteCrewRow(
            crewName = crew.name,
            crewRole = crew.primaryRole.title,
            isAvailable = crew.isAvailable,
            onRebookClick = { lastBookingId?.let(onRebookClick) }
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
      item {
        Spacer(modifier = Modifier.height(18.dp))
      }
    }

    // 4. EXPLORE SHOOTS (Horizontal scrolling visual cards taking inspiration from streaming apps)
    item {
      SectionHeader(title = "EXPLORE SHOOTS")
      Spacer(modifier = Modifier.height(8.dp))
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
      ) {
        items(categories) { category ->
          ShootCategoryCard(
            title = category.title,
            subtitle = category.subtitle,
            icon = category.icon,
            onClick = onBookShootClick
          )
        }
      }
      Spacer(modifier = Modifier.height(28.dp))
    }

    // 5. RECENT (Clean list with high visual hierarchy without clutter)
    if (recentBookings.isNotEmpty()) {
      item {
        SectionHeader(title = "RECENT")
        Spacer(modifier = Modifier.height(8.dp))
      }
      itemsIndexed(recentBookings, key = { _, booking -> booking.id }) { index, booking ->
        CardEntrance(index = index) {
          CleanBookingItem(
            booking = booking,
            onClick = { onBookingClick(booking.id) }
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}
@Composable
private fun FavoriteCrewRow(
  crewName: String,
  crewRole: String,
  isAvailable: Boolean,
  onRebookClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = DarkCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(DarkElevated),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = AmberGold,
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = crewName,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = PureWhite
        )
        Text(
          text = crewRole,
          style = MaterialTheme.typography.bodySmall,
          color = if (isAvailable) EmeraldSuccess else TextSecondary
        )
      }
      PrimaryGoldButton(
        text = "BOOK AGAIN",
        onClick = onRebookClick
      )
    }
  }
}
