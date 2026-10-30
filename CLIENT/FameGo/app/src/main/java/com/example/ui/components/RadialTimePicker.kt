package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class DialMode { HOUR, MINUTE }

/** 0° = 12 o'clock, clockwise, 0..360. */
private fun clockAngle(center: Offset, pos: Offset): Float {
  val dx = pos.x - center.x
  val dy = pos.y - center.y
  var deg = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat()
  if (deg < 0f) deg += 360f
  return deg
}

private fun posOnDial(center: Offset, radiusPx: Float, clockDeg: Float): Offset {
  val rad = Math.toRadians(clockDeg.toDouble())
  return Offset(
    center.x + radiusPx * sin(rad).toFloat(),
    center.y - radiusPx * cos(rad).toFloat()
  )
}

/**
 * FameGo radial clock picker — tap the call-time field and a clock dial opens.
 * Slide a finger around the ring to set hour/minute, exactly like the classic
 * analog picker, reskinned in FameGo gold-on-black with a spring needle and
 * haptic ticks.
 */
@Composable
fun FameGoRadialTimePickerDialog(
  initialHour12: Int,
  initialMinute: Int,
  initialAmPm: String,
  onDismiss: () -> Unit,
  onConfirm: (hour12: Int, minute: Int, amPm: String) -> Unit,
  modifier: Modifier = Modifier,
  // Same-day gate: slots before this are greyed and unpickable (1-hour buffer).
  minHour12: Int? = null,
  minMinute: Int = 0,
  minAmPm: String? = null
) {
  val haptic = LocalHapticFeedback.current

  fun toMin24(h12: Int, m: Int, ap: String): Int {
    val h24 = if (ap == "PM") h12 % 12 + 12 else h12 % 12
    return h24 * 60 + m
  }
  val minTotal: Int? =
    if (minHour12 != null && minAmPm != null) toMin24(minHour12, minMinute, minAmPm) else null
  fun isValid(h12: Int, m: Int, ap: String): Boolean =
    minTotal == null || toMin24(h12, m, ap) >= minTotal
  // An hour stays selectable while any minute inside it is still bookable.
  fun hourValid(h12: Int, ap: String): Boolean =
    minTotal == null || toMin24(h12, 59, ap) >= minTotal

  // Coerce the opening selection up to the minimum (never open on a dead time).
  val openTriple: Triple<Int, Int, String> = run {
    var h = initialHour12.coerceIn(1, 12)
    var m = initialMinute.coerceIn(0, 59)
    var ap = if (initialAmPm == "PM") "PM" else "AM"
    if (minHour12 != null && minAmPm != null && !isValid(h, m, ap)) {
      ap = minAmPm
      h = minHour12
      m = minMinute
    }
    Triple(h, m, ap)
  }

  var hour12 by remember(openTriple) { mutableIntStateOf(openTriple.first) }
  var minute by remember(openTriple) { mutableIntStateOf(openTriple.second) }
  var amPm by remember(openTriple) { mutableStateOf(openTriple.third) }
  var mode by remember { mutableStateOf(DialMode.HOUR) }
  // Continuous needle angle — shortest-path stepping avoids wrap-around spins.
  var needleDeg by remember { mutableStateOf((hour12 % 12) * 30f) }
  val smoothNeedle by animateFloatAsState(
    targetValue = needleDeg,
    animationSpec = spring(stiffness = 260f, dampingRatio = 0.82f),
    label = "clockNeedle"
  )

  fun angleForState(): Float = if (mode == DialMode.HOUR) (hour12 % 12) * 30f else minute * 6f

  fun moveNeedleTo(target: Float) {
    val delta = ((target - needleDeg + 540f) % 360f) - 180f
    needleDeg += delta
  }

  fun snapToMin() {
    if (minHour12 != null && minAmPm != null) {
      amPm = minAmPm
      hour12 = minHour12
      minute = minMinute
      moveNeedleTo(angleForState())
    }
  }

  fun applyAngle(angle: Float) {
    if (mode == DialMode.HOUR) {
      val h = ((angle + 15f) / 30f).toInt() % 12
      val next = if (h == 0) 12 else h
      if (!hourValid(next, amPm)) {
        snapToMin()
        return
      }
      if (next != hour12) {
        hour12 = next
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      moveNeedleTo((hour12 % 12) * 30f)
    } else {
      // Snap to 5-minute steps like the classic picker.
      val next = ((angle / 30f).roundToInt() * 5) % 60
      if (!isValid(hour12, next, amPm)) {
        if (minHour12 != null) {
          minute = minMinute
          moveNeedleTo(minute * 6f)
        }
        return
      }
      if (next != minute) {
        minute = next
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      moveNeedleTo(minute * 6f)
    }
  }

  fun pickAmPm(option: String) {
    // Refuse to flip into a half-day that is entirely in the past.
    if (minTotal != null) {
      val order = mapOf("AM" to 0, "PM" to 1)
      val minOrder = if (minAmPm == "PM") 1 else 0
      if ((order[option] ?: 0) < minOrder) return
    }
    amPm = option
    if (!isValid(hour12, minute, amPm)) snapToMin()
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = FameGoCardElevated,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoGold.copy(alpha = 0.35f)),
      shadowElevation = 16.dp,
      modifier = modifier.testTag("radial_time_dialog")
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Set call time",
          color = FameGoWhite,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Slide your finger around the dial",
          color = FameGoTextMuted,
          fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (minTotal != null && minHour12 != null && minAmPm != null) {
          Text(
            text = "Today: from %02d:%02d %s (1-hour buffer)".format(minHour12, minMinute, minAmPm),
            color = FameGoGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))
        }

        // Digital readout — tap either half to switch dial mode.
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "%02d".format(hour12),
            color = if (mode == DialMode.HOUR) FameGoGold else FameGoTextMuted,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { mode = DialMode.HOUR }
              .padding(horizontal = 6.dp)
              .testTag("clock_readout_hour")
          )
          Text(text = ":", color = FameGoGold, fontSize = 36.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "%02d".format(minute),
            color = if (mode == DialMode.MINUTE) FameGoGold else FameGoTextMuted,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { mode = DialMode.MINUTE }
              .padding(horizontal = 6.dp)
              .testTag("clock_readout_minute")
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("AM", "PM").forEach { option ->
              val selected = amPm == option
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (selected) FameGoGold else Color.Transparent)
                  .border(
                    1.dp,
                    if (selected) FameGoGold else FameGoBorder,
                    RoundedCornerShape(10.dp)
                  )
                  .clickable { pickAmPm(option) }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
                  .testTag("clock_ampm_$option"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = option,
                  color = if (selected) Color(0xFF1A1408) else FameGoTextSecondary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // The dial — responsive to small phones, capped on tablets.
        BoxWithConstraints(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          val dialSize: Dp = (maxWidth - 48.dp).coerceIn(220.dp, 300.dp)
          ClockDial(
            dialSize = dialSize,
            mode = mode,
            hour12 = hour12,
            minute = minute,
            needleDeg = smoothNeedle,
            onAngle = ::applyAngle,
            isHourEnabled = { h -> hourValid(h, amPm) },
            isMinuteEnabled = { m -> isValid(hour12, m, amPm) },
            onPickHour = { h ->
              hour12 = h
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              moveNeedleTo((h % 12) * 30f)
              mode = DialMode.MINUTE
            },
            onPickMinute = { m ->
              minute = m
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              moveNeedleTo(m * 6f)
            },
            onModeChange = { mode = it }
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mode tabs.
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(DialMode.HOUR, DialMode.MINUTE).forEach { m ->
              val selected = mode == m
              Text(
                text = if (m == DialMode.HOUR) "Hour" else "Minute",
                color = if (selected) FameGoGold else FameGoTextMuted,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { mode = m; moveNeedleTo(angleForState()) }
                  .background(
                    if (selected) FameGoGold.copy(alpha = 0.12f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                  )
                  .padding(horizontal = 14.dp, vertical = 8.dp)
              )
            }
          }
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = FameGoTextSecondary, fontSize = 14.sp)
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        VengeanceAnimatedButton(
          text = "Set %02d:%02d %s".format(hour12, minute, amPm),
          onClick = { onConfirm(hour12, minute, amPm) },
          style = VengeanceButtonStyle.GOLD,
          modifier = Modifier.fillMaxWidth(),
          testTag = "clock_confirm"
        )
      }
    }
  }
}

@Composable
private fun ClockDial(
  dialSize: Dp,
  mode: DialMode,
  hour12: Int,
  minute: Int,
  needleDeg: Float,
  onAngle: (Float) -> Unit,
  isHourEnabled: (Int) -> Boolean = { true },
  isMinuteEnabled: (Int) -> Boolean = { true },
  onPickHour: (Int) -> Unit,
  onPickMinute: (Int) -> Unit,
  onModeChange: (DialMode) -> Unit,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val dialPx = remember(dialSize) { with(density) { dialSize.toPx() } }
  val labels = remember(mode) {
    // Index 0 sits at 12 o'clock: hours run 12,1,…,11; minutes 00,05,…,55.
    if (mode == DialMode.HOUR) (0..11).map { if (it == 0) "12" else "$it" }
    else (0..11).map { "%02d".format(it * 5) }
  }
  // Fractional selection for the highlight ring.
  val selectedFraction = if (mode == DialMode.HOUR) (hour12 % 12) else minute / 5

  Box(
    modifier = modifier
      .size(dialSize)
      .pointerInput(mode) {
        detectTapGestures { pos ->
          val angle = clockAngle(Offset(dialPx / 2f, dialPx / 2f), pos)
          onAngle(angle)
          if (mode == DialMode.HOUR) onModeChange(DialMode.MINUTE)
        }
      }
      .pointerInput(mode) {
        detectDragGestures(
          onDrag = { change, _ ->
            change.consume()
            onAngle(clockAngle(Offset(dialPx / 2f, dialPx / 2f), change.position))
          },
          onDragEnd = { if (mode == DialMode.HOUR) onModeChange(DialMode.MINUTE) }
        )
      }
      .semantics {
        contentDescription = "Clock dial, ${mode.name.lowercase()} selection"
      }
      .testTag("clock_dial"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val center = Offset(cx, cy)
      val ringR = size.minDimension / 2f - 14.dp.toPx()
      val numR = size.minDimension / 2f - 44.dp.toPx()

      // Outer track ring.
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(FameGoGold.copy(alpha = 0.16f), Color.Transparent),
          center = center,
          radius = size.minDimension / 2f
        ),
        radius = size.minDimension / 2f,
        center = center
      )
      drawCircle(
        color = FameGoBorder,
        radius = ringR,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
      )
      // 60 minute ticks.
      for (i in 0 until 60) {
        val a = i * 6f
        val major = i % 5 == 0
        val outer = posOnDial(center, ringR - 4.dp.toPx(), a)
        val inner = posOnDial(center, ringR - (if (major) 12.dp.toPx() else 8.dp.toPx()), a)
        drawLine(
          color = if (major) FameGoTextMuted.copy(alpha = 0.8f) else FameGoTextMuted.copy(alpha = 0.3f),
          start = inner,
          end = outer,
          strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
          cap = StrokeCap.Round
        )
      }
      // Needle from hub to selection.
      val needleEnd = posOnDial(center, numR, ((needleDeg % 360f) + 360f) % 360f)
      drawLine(
        color = FameGoGold,
        start = center,
        end = needleEnd,
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
      )
      // Hub.
      drawCircle(color = FameGoGold, radius = 7.dp.toPx(), center = center)
      drawCircle(color = Color(0xFF1A1408), radius = 3.dp.toPx(), center = center)
    }

    // Number touch targets around the ring.
    val numRadiusDp = dialSize / 2f - 44.dp
    labels.forEachIndexed { index, label ->
      val clockDeg = index * 30f
      val rad = Math.toRadians(clockDeg.toDouble())
      val dx = with(density) { (numRadiusDp * sin(rad).toFloat()).toPx() }
      val dy = with(density) { (numRadiusDp * -cos(rad).toFloat()).toPx() }
      val isSelected = index == selectedFraction
      // Past slots render grey and ignore taps.
      val valueEnabled = if (mode == DialMode.HOUR) {
        isHourEnabled(if (index == 0) 12 else index)
      } else {
        isMinuteEnabled((index * 5) % 60)
      }
      Box(
        modifier = Modifier
          .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
          .size(44.dp)
          .clip(CircleShape)
          .background(
            if (isSelected) FameGoGold else Color.Transparent,
            CircleShape
          )
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = valueEnabled,
            onClick = {
              if (mode == DialMode.HOUR) onPickHour(if (index == 0) 12 else index)
              else onPickMinute((index * 5) % 60)
            }
          )
          .semantics {
            contentDescription = label
            role = Role.RadioButton
          }
          .testTag("clock_num_$label"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = label,
          color = when {
            isSelected -> Color(0xFF1A1408)
            !valueEnabled -> FameGoTextMuted.copy(alpha = 0.3f)
            else -> FameGoTextSecondary
          },
          fontSize = 15.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
