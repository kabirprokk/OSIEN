package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.MapPlace
import com.example.data.MapTilerGeocoding
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** One-tap popular shoot localities — works with zero network. */
private val PopularLocalities = listOf(
  "Bandra West", "Andheri West", "Juhu", "Lower Parel",
  "Powai", "Colaba", "Versova", "Goregaon"
)

/**
 * Real movable map (osmdroid + free OpenStreetMap tiles, no API key):
 * drag to pan, pinch to zoom, tap to drop a pin, "Use this spot" fills
 * the address via free reverse-geocoding. Recenters on search picks.
 */
@Composable
fun MapPreviewCard(
  centerLng: Double,
  centerLat: Double,
  zoom: Int,
  pinLabel: String?,
  onPick: (MapPlace) -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var mapRef by remember { mutableStateOf<MapView?>(null) }
  // Last center WE applied — user pans never trigger a recenter fight.
  val lastApplied = remember { doubleArrayOf(centerLat, centerLng, zoom.toDouble()) }
  val markerHolder = remember { arrayOfNulls<Marker>(1) }
  // User-tapped pin: survives recomposition so the marker never snaps back
  // to the prop center when reverse-geocode (or any state) recomposes us.
  var userPin by remember { mutableStateOf<GeoPoint?>(null) }
  var pendingFull by remember { mutableStateOf<MapPlace?>(null) }
  // Guards concurrent reverse-geocodes: only the latest tap refines the chip.
  var reverseSeq by remember { mutableIntStateOf(0) }

  // osmdroid keeps rendering/loading tiles unless paused: tie it to the
  // compose lifecycle so backgrounding the app parks the map.
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, mapRef) {
    val map = mapRef
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      when (event) {
        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> runCatching { map?.onResume() }
        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> runCatching { map?.onPause() }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(240.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(FameGoCard)
      .border(1.dp, FameGoBorder, RoundedCornerShape(16.dp))
      .testTag("map_preview")
  ) {
    AndroidView(
      factory = { ctx ->
        MapView(ctx).apply {
          // OSM Mapnik: key-free, reliable everywhere. CARTO's free
          // endpoint renders "API key required" tiles on some networks.
          setTileSource(TileSourceFactory.MAPNIK)
          setMultiTouchControls(true)
          zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
          controller.setZoom(zoom.toDouble())
          controller.setCenter(GeoPoint(centerLat, centerLng))
          overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
              override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                // Instant: drop the pin + confirm chip immediately with coords,
                // then refine the label in the background. Never block on network.
                val tapped = GeoPoint(p.latitude, p.longitude)
                userPin = tapped
                val instant = MapPlace(
                  full = "${p.latitude}, ${p.longitude}",
                  short = "Selected spot",
                  latitude = p.latitude,
                  longitude = p.longitude
                )
                pendingFull = instant
                markerHolder[0]?.position = tapped
                val seq = ++reverseSeq
                scope.launch {
                  MapTilerGeocoding.reverse(p.latitude, p.longitude).onSuccess { place ->
                    // Stale responses (rapid taps) must not overwrite the latest pin.
                    if (seq == reverseSeq) pendingFull = place
                  }
                }
                return true
              }

              override fun longPressHelper(p: GeoPoint): Boolean = false
            })
          )
          val marker = Marker(this).apply {
            position = GeoPoint(centerLat, centerLng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
          }
          overlays.add(marker)
          markerHolder[0] = marker
          mapRef = this
        }
      },
      update = { map ->
        if (lastApplied[0] != centerLat || lastApplied[1] != centerLng || lastApplied[2] != zoom.toDouble()) {
          val dLat = kotlin.math.abs(lastApplied[0] - centerLat)
          val dLng = kotlin.math.abs(lastApplied[1] - centerLng)
          lastApplied[0] = centerLat
          lastApplied[1] = centerLng
          lastApplied[2] = zoom.toDouble()
          // A new prop center (search pick / locality chip) wins: drop the
          // old tap pin and glide the marker + camera to it.
          userPin = null
          pendingFull = null
          markerHolder[0]?.let {
            it.position = GeoPoint(centerLat, centerLng)
            it.title = pinLabel
          }
          // Far jumps snap instantly; nearby moves glide smoothly.
          if (dLat + dLng > 0.5) {
            map.controller.setCenter(GeoPoint(centerLat, centerLng))
          } else {
            map.controller.animateTo(GeoPoint(centerLat, centerLng))
          }
          map.controller.setZoom(zoom.toDouble())
        } else {
          // No prop change: the marker belongs to the user's tap pin.
          // (The old code reset it to center on EVERY recomposition, so the
          // pin visibly snapped back the moment reverse-geocode resolved.)
          val want = userPin ?: GeoPoint(centerLat, centerLng)
          markerHolder[0]?.let {
            if (it.position.latitude != want.latitude || it.position.longitude != want.longitude) {
              it.position = want
            }
            it.title = pendingFull?.short ?: pinLabel
          }
        }
        map.invalidate()
      },
      modifier = Modifier.matchParentSize(),
      onRelease = { it.onDetach() }
    )

    // Zoom + recenter controls (minimal, gold).
    Column(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(
        Icons.Default.Add to ({ mapRef?.controller?.zoomIn() } to "Zoom in"),
        Icons.Default.Remove to ({ mapRef?.controller?.zoomOut() } to "Zoom out"),
        Icons.Default.MyLocation to ({
          // Back to the searched/selected center, drop the stray tap pin.
          userPin = null
          pendingFull = null
          mapRef?.controller?.animateTo(GeoPoint(centerLat, centerLng))
        } to "Recenter")
      ).forEach { (icon, action) ->
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, FameGoGold.copy(alpha = 0.5f), CircleShape)
            .clickable { action.first() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = action.second,
            tint = FameGoGold,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // Confirm chip for the tapped pin.
    pendingFull?.let { full ->
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 26.dp, start = 16.dp, end = 16.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(FameGoGold)
          .clickable {
            onPick(full)
            pendingFull = null
            // The parent recenters the map to this spot via props; keep the
            // pin there instead of dropping it on the prop switch.
            userPin = full.latitude?.let { lat ->
              full.longitude?.let { lng -> GeoPoint(lat, lng) }
            }
          }
          .padding(horizontal = 16.dp, vertical = 10.dp)
          .testTag("map_use_spot"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF1A1408),
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Use this spot${if (full.short != "Selected spot") " • ${full.short}" else ""}",
            color = Color(0xFF1A1408),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Text(
      text = "© OpenStreetMap contributors",
      color = Color.Black.copy(alpha = 0.7f),
      fontSize = 9.sp,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .background(Color.White.copy(alpha = 0.7f))
        .padding(start = 8.dp, bottom = 6.dp, end = 4.dp)
    )
  }
}

@Composable
fun SavedLocationChips(
  onPick: (com.example.model.SavedLocation) -> Unit,
  modifier: Modifier = Modifier
) {
  val saved by com.example.data.FameGoRepository.savedLocations.collectAsState()
  if (saved.isEmpty()) return
  Column(modifier = modifier) {
    Text(
      text = "Your past shoot spots — tap to refill",
      color = FameGoTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .testTag("saved_location_chips"),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      saved.forEach { loc ->
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FameGoGold.copy(alpha = 0.12f))
            .border(1.dp, FameGoGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .fameGoTapBounce { onPick(loc) }
            .padding(horizontal = 14.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(FameGoGold)
          )
          Spacer(modifier = Modifier.width(7.dp))
          Text(
            text = loc.label.ifBlank { loc.venueName.ifBlank { "Saved spot" } },
            color = FameGoGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
fun LocalityChips(
  onPick: (MapPlace) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Text(
      text = "Popular shoot areas — tap to fill",
      color = FameGoTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .testTag("locality_chips"),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      PopularLocalities.forEach { loc ->
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FameGoCard)
            .border(1.dp, FameGoBorder, RoundedCornerShape(16.dp))
            .fameGoTapBounce { onPick(MapPlace(full = "$loc, Mumbai, Maharashtra, India", short = loc)) }
            .padding(horizontal = 14.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(FameGoGold)
          )
          Spacer(modifier = Modifier.width(7.dp))
          Text(
            text = loc,
            color = FameGoWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}
