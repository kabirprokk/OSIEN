package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.model.Booking
import com.example.model.BookingStatus
import com.example.model.ShootCategory
import com.example.ui.components.FlowPill
import com.example.ui.components.FlowPillState
import com.example.ui.components.LiveOrb
import com.example.ui.components.SoftCard
import com.example.ui.components.rubberBand
import com.example.ui.components.swipeToGoBack
import com.example.ui.theme.fameGoRise
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoSurface
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.launch

@Composable
fun BookingDetailsScreen(
  bookingId: String,
  onBack: () -> Unit,
  onOpenChat: (String) -> Unit,
  onRebook: (ShootCategory) -> Unit,
  onBookSameCrew: (Booking) -> Unit,
  onContactSupport: () -> Unit,
  onPayBooking: (Booking) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val bookings by FameGoRepository.bookings.collectAsState()
  val ratings by FameGoRepository.ratings.collectAsState()
  val liveSharing by FameGoRepository.liveSharing.collectAsState()
  val livePoints by FameGoRepository.livePoints.collectAsState()
  val currentUser by FameGoRepository.currentUser.collectAsState()
  // A stale deep link must not silently display a different booking.
  val booking = bookings.firstOrNull { it.id == bookingId }

  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .statusBarsPadding()
      .navigationBarsPadding()
      .swipeToGoBack(onBack = onBack)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .rubberBand()
        .padding(horizontal = 20.dp)
        .fameGoRise()
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Top bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = FameGoTextSecondary
          )
        }
        Text(
          text = "Shoot details",
          color = FameGoWhite,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold
        )
        // Chat shortcut
        IconButton(
          onClick = { booking?.id?.let(onOpenChat) },
          enabled = booking != null,
          modifier = Modifier.testTag("call_sheet_chat_icon")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = "Message",
            tint = FameGoGold
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      if (booking == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Booking not found", color = FameGoWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "It may have been cancelled or removed.",
              color = FameGoTextMuted, fontSize = 13.sp,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }

      if (booking != null) {
        // Main Live Shoot Card
        SoftCard(
          shape = RoundedCornerShape(22.dp),
          isElevated = true,
          testTag = "call_sheet_main_card"
        ) {
          Column(modifier = Modifier.padding(22.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                LiveOrb(
                  color = when (booking.status) {
                    BookingStatus.IN_PROGRESS -> FameGoGold
                    BookingStatus.CONFIRMED -> FameGoSuccessGreen
                    BookingStatus.PENDING -> FameGoAccentCyan
                    else -> FameGoTextMuted
                  },
                  size = 8.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = when (booking.status) {
                    BookingStatus.IN_PROGRESS -> "Shoot in progress"
                    BookingStatus.CONFIRMED -> "Crew confirmed"
                    BookingStatus.COMPLETED -> "Wrap completed"
                    else -> "Finding crew"
                  },
                  color = when (booking.status) {
                    BookingStatus.IN_PROGRESS -> FameGoGold
                    BookingStatus.CONFIRMED -> FameGoSuccessGreen
                    else -> FameGoTextSecondary
                  },
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Text(
                text = "₹${booking.estimatedBudget}",
                color = FameGoWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = booking.title,
              color = FameGoWhite,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )

            Text(
              text = "${booking.date} • Call time: ${booking.time} (${booking.durationHours} hours)",
              color = FameGoTextSecondary,
              fontSize = 13.sp,
              modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Venue
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = FameGoGold,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${booking.location.venueName}, ${booking.location.address}",
                color = FameGoTextPrimary,
                fontSize = 13.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Assigned Crew Roster
        if (booking.assignedCrew.isNotEmpty()) {
          Text(
            text = "Assigned crew",
            color = FameGoTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            booking.assignedCrew.forEach { crew ->
              SoftCard(
                onClick = { onOpenChat(booking.id) },
                testTag = "assigned_crew_item_${crew.crewId}"
              ) {
                Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(FameGoCardElevated)
                      .border(1.dp, FameGoGold.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = crew.crewName.take(1),
                      color = FameGoGold,
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  Spacer(modifier = Modifier.width(14.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = crew.crewName,
                        color = FameGoWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "${crew.rating} ★",
                        color = FameGoGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                    Text(
                      text = "${crew.role.title} • Verified kit",
                      color = FameGoTextSecondary,
                      fontSize = 12.sp,
                      modifier = Modifier.padding(top = 1.dp)
                    )
                    Text(
                      text = crew.gearList.joinToString(" • "),
                      color = FameGoTextMuted,
                      fontSize = 11.sp,
                      modifier = Modifier.padding(top = 2.dp)
                    )
                    if (crew.phone.isNotBlank()) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Phone,
                          contentDescription = null,
                          tint = FameGoTextMuted,
                          modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = crew.phone,
                          color = FameGoTextSecondary,
                          fontSize = 12.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FameGoSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle),
                    modifier = Modifier.clickable { onOpenChat(booking.id) }
                  ) {
                    Text(
                      text = "Message",
                      color = FameGoGold,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(20.dp))
        }

        // Pay to lock in: crew accepted, money moves only now.
        if (booking.status == BookingStatus.CONFIRMED &&
          booking.paymentStatus == com.example.model.PaymentStatus.PENDING &&
          currentUser.role == com.example.model.Role.CLIENT
        ) {
          val crewName = booking.assignedCrew.firstOrNull()?.name ?: "Your crew"
          SoftCard(isElevated = true, testTag = "pay_to_lock_card") {
            Column(modifier = Modifier.padding(18.dp)) {
              Text(
                text = "$crewName accepted your shoot",
                color = FameGoWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Pay now to lock the date. Nothing was charged before this.",
                color = FameGoTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              com.example.ui.components.FameGoButton(
                text = "Pay ₹${"%,d".format(booking.priceRupees)}",
                onClick = { onPayBooking(booking) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "pay_to_lock_button"
              )
            }
          }
          Spacer(modifier = Modifier.height(20.dp))
        }

        // Live crew location (visible once crew is assigned and shoot is active)
        if (booking.assignedCrew.isNotEmpty() &&
          (booking.status == BookingStatus.CONFIRMED || booking.status == BookingStatus.IN_PROGRESS)
        ) {
          val sharing = booking.id in liveSharing
          Text(
            text = "Live location",
            color = FameGoTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))
          LiveLocationMap(
            venueName = booking.location.venueName,
            sharing = sharing,
            latitude = livePoints[booking.id]?.latitude,
            longitude = livePoints[booking.id]?.longitude,
            statusLabel = livePoints[booking.id]?.label
              ?: if (sharing) "Locating crew…" else "Waiting for crew to share location"
          )
          Spacer(modifier = Modifier.height(20.dp))
        }

        // Bluetooth proximity Shoot-Done: no more one-tap "it's done".
        // Client + crew each press below, standing together with Bluetooth
        // on. Discovery proves the wrap happened at the venue.
        if (booking.assignedCrew.isNotEmpty() &&
          (booking.status == BookingStatus.CONFIRMED || booking.status == BookingStatus.IN_PROGRESS) &&
          currentUser.role != com.example.model.Role.ADMIN
        ) {
          ShootDonePanel(
            bookingId = booking.id,
            bookingCompleted = booking.status == BookingStatus.COMPLETED,
            onContactSupport = onContactSupport
          )
          Spacer(modifier = Modifier.height(20.dp))
        }

        // Brief & Scope
        Text(
          text = "Shoot brief",
          color = FameGoTextMuted,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        SoftCard {
          Text(
            text = booking.brief.ifEmpty { "Standard production coverage as requested." },
            color = FameGoTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(16.dp)
          )
        }

        // Rating + book-same-crew (completed shoots with assigned crew)
        if (booking.status == BookingStatus.COMPLETED && booking.assignedCrew.isNotEmpty()) {
          Spacer(modifier = Modifier.height(20.dp))
          Text(
            text = "Rate your crew",
            color = FameGoTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))
          val rateable = booking.assignedCrew.first()
          val existingRating = ratings["${booking.id}:${rateable.crewId}"]
          val favoriteIds by FameGoRepository.favoriteCrewIds.collectAsState()
          val isFav = rateable.crewId in favoriteIds
          SoftCard(
            isElevated = true,
            testTag = "rate_crew_card"
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = rateable.crewName,
                  color = FameGoWhite,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = { FameGoRepository.toggleFavoriteCrew(rateable.crewId) },
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("favorite_crew_toggle")
                ) {
                  com.example.ui.components.FameGoPop(target = isFav) { fav ->
                    Icon(
                      imageVector = if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                      contentDescription = if (fav) "Remove from favorites" else "Save crew to favorites",
                      tint = if (fav) FameGoGold else FameGoTextMuted,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }
              if (existingRating != null) {
                Spacer(modifier = Modifier.height(8.dp))
                RatingStars(stars = existingRating.stars, onSelect = {}, enabled = false)
                if (existingRating.review.isNotBlank()) {
                  Text(
                    text = "\"${existingRating.review}\"",
                    color = FameGoTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                  )
                }
              } else {
                var stars by remember(booking.id) { mutableIntStateOf(0) }
                var review by remember(booking.id) { mutableStateOf("") }
                Spacer(modifier = Modifier.height(8.dp))
                RatingStars(stars = stars, onSelect = { stars = it }, enabled = true)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                  value = review,
                  onValueChange = { review = it },
                  placeholder = { Text("Add a note for the crew (optional)", color = FameGoTextMuted, fontSize = 12.sp) },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FameGoWhite,
                    unfocusedTextColor = FameGoWhite,
                    focusedContainerColor = FameGoCardElevated,
                    unfocusedContainerColor = FameGoCardElevated,
                    focusedBorderColor = FameGoGold,
                    unfocusedBorderColor = FameGoBorderSubtle
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rate_crew_review")
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowPill(
                  state = FlowPillState.CUSTOM,
                  customText = "Submit rating",
                  enabled = stars > 0,
                  onClick = {
                    FameGoRepository.submitRating(booking.id, rateable.crewId, stars, review)
                  },
                  modifier = Modifier.fillMaxWidth(),
                  testTag = "rate_crew_submit"
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          FlowPill(
            state = FlowPillState.CUSTOM,
            customText = "Book same crew again",
            onClick = {
              FameGoRepository.prepareRebooking(booking.id)?.let(onBookSameCrew)
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "book_same_crew_button"
          )
        }
      }

      Spacer(modifier = Modifier.height(110.dp))
    }

    // Fixed Bottom Action Flow Pill
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 24.dp)
    ) {
      if (booking?.status == BookingStatus.COMPLETED) {
        FlowPill(
          state = FlowPillState.CUSTOM,
          customText = "Book again",
          onClick = { onRebook(booking.category) },
          testTag = "call_sheet_rebook_pill"
        )
      } else {
        FlowPill(
          state = FlowPillState.CUSTOM,
          customText = "Message crew",
          customIcon = Icons.AutoMirrored.Filled.Chat,
          onClick = { booking?.id?.let(onOpenChat) },
          testTag = "call_sheet_chat_pill"
        )
      }
    }
  }
}

@Composable
private fun RatingStars(
  stars: Int,
  onSelect: (Int) -> Unit,
  enabled: Boolean,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.testTag("rate_crew_stars"),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    (1..5).forEach { value ->
      val lit = value <= stars
      IconButton(
        onClick = { onSelect(value) },
        enabled = enabled,
        modifier = Modifier
          .size(40.dp)
          .testTag("rate_star_$value")
      ) {
        // Lit stars pop in with a spring the moment they're tapped.
        val pop = com.example.ui.components.fameGoToggleScale(lit)
        Icon(
          imageVector = if (lit) Icons.Default.Star else Icons.Default.StarBorder,
          contentDescription = "$value star",
          tint = if (lit) FameGoGold else FameGoTextMuted,
          modifier = Modifier
            .size(28.dp)
            .scale(pop)
        )
      }
    }
  }
}

@Composable
private fun LiveLocationMap(
  venueName: String,
  sharing: Boolean,
  latitude: Double?,
  longitude: Double?,
  statusLabel: String,
  modifier: Modifier = Modifier
) {
  SoftCard(
    shape = RoundedCornerShape(18.dp),
    isElevated = true,
    modifier = modifier.fillMaxWidth(),
    testTag = "live_location_map"
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
          .background(Color(0xFF101216)),
        contentAlignment = Alignment.Center
      ) {
        if (sharing && latitude != null && longitude != null) {
          // Key-free live map (osmdroid + OSM tiles): follows the crew point.
          AndroidView(
            factory = { context ->
              org.osmdroid.views.MapView(context).apply {
                setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(
                  org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                )
                controller.setZoom(15.0)
                controller.setCenter(org.osmdroid.util.GeoPoint(latitude, longitude))
                val marker = org.osmdroid.views.overlay.Marker(this).apply {
                  position = org.osmdroid.util.GeoPoint(latitude, longitude)
                  setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                  title = "Crew"
                }
                overlays.add(marker)
                tag = marker
              }
            },
            update = { map ->
              (map.tag as? org.osmdroid.views.overlay.Marker)?.position =
                org.osmdroid.util.GeoPoint(latitude, longitude)
              map.controller.animateTo(org.osmdroid.util.GeoPoint(latitude, longitude))
              map.invalidate()
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.onDetach() }
          )
        } else {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LocationOn, null, tint = FameGoGold, modifier = Modifier.size(28.dp))
            Text(
              if (sharing) "Waiting for the crew's location" else "Location sharing off",
              color = FameGoTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
            )
          }
        }
        if (!sharing) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC141518),
            border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle)
          ) {
            Text(
              text = "Location sharing off",
              color = FameGoTextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
          }
        }
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        LiveOrb(
          color = if (sharing) FameGoSuccessGreen else FameGoTextMuted,
          size = 7.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = venueName.ifBlank { "Shoot venue" },
            color = FameGoWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (sharing && latitude != null && longitude != null) {
              "$statusLabel • ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}"
            } else {
              statusLabel
            },
            color = FameGoTextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

private enum class ShootDonePhase { IDLE, SEARCHING, WAITING, DONE, ERROR }

/**
 * Proximity-verified wrap: both phones broadcast + scan a booking-specific
 * BLE id. Discovery (only possible ~10-30m apart) records this side's tap;
 * when both sides have tapped, the booking completes and both get
 * "Shoot is Done". No pairing, no personal data over the air.
 */
@Composable
private fun ShootDonePanel(
  bookingId: String,
  bookingCompleted: Boolean,
  onContactSupport: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var phase by remember(bookingId) { mutableStateOf(ShootDonePhase.IDLE) }
  var statusText by remember(bookingId) { mutableStateOf("") }
  var session by remember { mutableStateOf<com.example.data.ShootDoneProximity.Session?>(null) }
  var worker by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

  fun cleanup() {
    session?.stop()
    session = null
    worker?.cancel()
    worker = null
  }
  DisposableEffect(bookingId) { onDispose { cleanup() } }
  // Remote completion (peer tapped second): stop searching, show done.
  LaunchedEffect(bookingCompleted) {
    if (bookingCompleted) {
      cleanup()
      phase = ShootDonePhase.DONE
      statusText = "Shoot is Done — wrap confirmed by both sides."
    }
  }

  fun fail(message: String) {
    cleanup()
    phase = ShootDonePhase.ERROR
    statusText = message
  }

  fun beginSearch() {
    cleanup()
    phase = ShootDonePhase.SEARCHING
    statusText = "Broadcasting… keep both phones close, Bluetooth on."
    worker = scope.launch {
      try {
        session = com.example.data.ShootDoneProximity.start(
          context = context,
          bookingId = bookingId,
          onPeerFound = {
            scope.launch {
              phase = ShootDonePhase.WAITING
              statusText = "Other phone found — recording your confirmation…"
              FameGoRepository.signalShootDone(bookingId)
                .onSuccess { result ->
                  if (result == FameGoRepository.ShootDoneResult.COMPLETED) {
                    cleanup()
                    phase = ShootDonePhase.DONE
                    statusText = "Shoot is Done — wrap confirmed by both sides."
                  } else {
                    // Other side hasn't tapped yet: poll until they do.
                    statusText = "You confirmed. Waiting for the other side…"
                    worker = scope.launch {
                      repeat(15) {
                        kotlinx.coroutines.delay(4000)
                        if (FameGoRepository.countShootDoneSignals(bookingId) >= 2) {
                          FameGoRepository.completeShoot(bookingId)
                          cleanup()
                          phase = ShootDonePhase.DONE
                          statusText = "Shoot is Done — wrap confirmed by both sides."
                          return@launch
                        }
                      }
                      fail("The other side hasn't confirmed yet. Ask them to press Shoot Done nearby, then retry.")
                    }
                  }
                }
                .onFailure { fail(FameGoRepository.friendlyMessage(it)) }
            }
          },
          onError = { fail(it) }
        )
        // Give up searching after 60s so we never drain battery silently.
        kotlinx.coroutines.delay(60_000)
        if (phase == ShootDonePhase.SEARCHING) {
          fail("Couldn't find the other phone nearby. Stand together with Bluetooth on and retry.")
        }
      } catch (e: Exception) {
        fail(e.message ?: "Couldn't start nearby search.")
      }
    }
  }

  val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    if (grants.values.all { it }) beginSearch()
    else fail("Nearby-devices permission denied — allow it so the phones can find each other.")
  }
  val btEnableLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == android.app.Activity.RESULT_OK) beginSearch()
    else fail("Bluetooth stayed off — turn it on so the phones can find each other.")
  }

  fun press() {
    if (phase == ShootDonePhase.SEARCHING || phase == ShootDonePhase.WAITING) {
      cleanup()
      phase = ShootDonePhase.IDLE
      statusText = ""
      return
    }
    if (phase == ShootDonePhase.DONE) return
    val missing = com.example.data.ShootDoneProximity.missingPermissions(context)
    if (missing.isNotEmpty()) {
      permissionLauncher.launch(missing.toTypedArray())
      return
    }
    if (!com.example.data.ShootDoneProximity.isBluetoothOn(context)) {
      runCatching {
        btEnableLauncher.launch(
          android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
        )
      }.onFailure { fail("Couldn't open Bluetooth settings — enable Bluetooth manually and retry.") }
      return
    }
    beginSearch()
  }

  SoftCard(isElevated = true, testTag = "shoot_done_card") {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Bluetooth,
          contentDescription = null,
          tint = FameGoGold,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Shoot Done — together",
          color = FameGoWhite,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = "Both of you press below, standing together with Bluetooth on. Your phones find each other only when truly nearby — that's the proof the shoot really wrapped.",
        color = FameGoTextSecondary,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(top = 8.dp)
      )
      if (phase == ShootDonePhase.SEARCHING || phase == ShootDonePhase.WAITING) {
        com.example.ui.components.FameGoPulse(enabled = true) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp)
          ) {
            androidx.compose.material3.CircularProgressIndicator(
              color = FameGoGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = statusText, color = FameGoGold, fontSize = 13.sp)
          }
        }
      } else if (statusText.isNotBlank()) {
        Text(
          text = statusText,
          color = if (phase == ShootDonePhase.DONE) FameGoSuccessGreen else FameGoTextSecondary,
          fontSize = 13.sp,
          modifier = Modifier.padding(top = 12.dp)
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      com.example.ui.components.FameGoButton(
        text = when (phase) {
          ShootDonePhase.IDLE -> "Shoot Done"
          ShootDonePhase.SEARCHING -> "Searching… tap to stop"
          ShootDonePhase.WAITING -> "Waiting… tap to stop"
          ShootDonePhase.DONE -> "Shoot is Done ✓"
          ShootDonePhase.ERROR -> "Retry Shoot Done"
        },
        onClick = { press() },
        enabled = phase != ShootDonePhase.DONE,
        modifier = Modifier.fillMaxWidth(),
        testTag = "shoot_done_button"
      )
      if (phase == ShootDonePhase.ERROR) {
        Text(
          text = "Stuck? Contact helpline",
          color = FameGoGold,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier
            .padding(top = 10.dp)
            .clickable { onContactSupport() }
        )
      }
    }
  }
}
