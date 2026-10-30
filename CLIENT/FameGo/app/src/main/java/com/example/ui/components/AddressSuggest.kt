package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MapPlace
import com.example.data.MapTilerGeocoding
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.delay

/**
 * Address options powered by free OpenStreetMap search (no API key): as the
 * address query changes, matching places appear here so one tap fills it.
 * Renders nothing while idle (short query / offline / no results).
 */
@Composable
fun AddressSuggestList(
  query: String,
  onPick: (MapPlace) -> Unit,
  modifier: Modifier = Modifier,
  debounceMs: Long = 400,
  onResults: (List<MapPlace>) -> Unit = {}
) {
  var places by remember { mutableStateOf(emptyList<MapPlace>()) }
  var loading by remember { mutableStateOf(false) }
  var failed by remember { mutableStateOf(false) }

  LaunchedEffect(query) {
    if (query.trim().length < 3) {
      places = emptyList()
      loading = false
      failed = false
      return@LaunchedEffect
    }
    delay(debounceMs)
    loading = true
    failed = false
    MapTilerGeocoding.suggest(query)
      .onSuccess {
        places = it
        onResults(it)
      }
      .onFailure { failed = true }
    loading = false
  }

  AnimatedVisibility(
    visible = loading || places.isNotEmpty(),
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut(),
    modifier = modifier.testTag("address_suggest_list")
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = if (loading) "Finding places…" else "Tap to fill address",
          color = FameGoTextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f)
        )
        if (loading) {
          CircularProgressIndicator(
            color = FameGoGold,
            strokeWidth = 2.dp,
            modifier = Modifier.size(14.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      places.forEachIndexed { index, place ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = FameGoCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onPick(place) }
            .testTag("address_option_$index")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = FameGoGold,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = place.short,
                color = FameGoWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = place.full,
                color = FameGoTextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
      if (!failed) {
        Text(
          text = "Suggestions by OpenStreetMap",
          color = FameGoTextMuted.copy(alpha = 0.7f),
          fontSize = 10.sp,
          modifier = Modifier.padding(top = 6.dp)
        )
      }
    }
  }
}
