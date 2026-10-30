package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoLiveRed
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite

/**
 * FameGo People — Situational Production Characters
 *
 * Minimal editorial silhouettes and linework crafted strictly within FameGo's
 * aesthetic palette: dark grey (#1E1F23, #2A2B30), off-white (#F5F5F7), muted grey (#8E8E93),
 * with subtle FameGo Gold accents.
 */
enum class CharacterState {
  FINDING_CREW,      // Checking watch / waiting / processing
  REQUEST_SENT,      // Standing ready with equipment
  CREW_CONFIRMED,    // Confident, camera ready on tripod/shoulder
  SHOOT_TOMORROW,    // Packing/prepping gear case
  CREW_EN_ROUTE,     // Walking with production backpack
  SHOOT_IN_PROGRESS, // Cameraman actively filming, tally light active
  SHOOT_COMPLETED,   // Calmly heading out with gear, job done
  NOTHING_BOOKED,    // Relaxed, leaning/sitting with camera
  NO_NEW_REQUESTS,   // Waiting calmly on duty
  ERROR              // Checking cable/device
}

/**
 * Composable character display occupying approximately 15-30% of visual card area.
 */
@Composable
fun FameGoCharacterIllustration(
  state: CharacterState,
  modifier: Modifier = Modifier,
  size: Dp = 88.dp,
  showBadge: Boolean = true
) {
  val infiniteTransition = rememberInfiniteTransition(label = "characterMotion")

  // Subtle breathing shoulder rise (2.6s cycle)
  val shoulderBreath by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = -1.6f,
    animationSpec = infiniteRepeatable(
      animation = tween(1300, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shoulderBreath"
  )

  // Watch checking tilt / subtle arm movement
  val watchShift by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 2.5f,
    animationSpec = infiniteRepeatable(
      animation = tween(1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "watchShift"
  )

  // En-route walking subtle horizontal shift
  val walkStep by infiniteTransition.animateFloat(
    initialValue = -2f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "walkStep"
  )

  // Tally light blink for live shooting
  val tallyAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(700, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "tallyAlpha"
  )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val w = this.size.width
      val h = this.size.height
      val darkChar = Color(0xFF26282E)
      val mediumChar = Color(0xFF3F424A)
      val lightChar = Color(0xFFE6E7EB)
      val accentGold = FameGoGold
      val mutedGray = Color(0xFF7B8089)

      // Soft ground line
      drawLine(
        color = Color(0x22FFFFFF),
        start = Offset(w * 0.15f, h * 0.90f),
        end = Offset(w * 0.85f, h * 0.90f),
        strokeWidth = 1.5f
      )

      when (state) {
        CharacterState.FINDING_CREW -> {
          // Character checking wrist watch + breathing
          drawFindingCrew(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = accentGold,
            breathY = shoulderBreath,
            watchAngle = watchShift
          )
        }

        CharacterState.REQUEST_SENT -> {
          // Crew standing ready next to gear rig
          drawRequestSent(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = accentGold,
            breathY = shoulderBreath
          )
        }

        CharacterState.CREW_CONFIRMED -> {
          // Confident DP with cinema camera on tripod/rig
          drawCrewConfirmed(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = accentGold,
            breathY = shoulderBreath
          )
        }

        CharacterState.SHOOT_TOMORROW -> {
          // Production member checking hardcase / packing
          drawShootTomorrow(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = accentGold
          )
        }

        CharacterState.CREW_EN_ROUTE -> {
          // Walking with production backpack
          drawCrewEnRoute(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = accentGold,
            walkOffset = walkStep
          )
        }

        CharacterState.SHOOT_IN_PROGRESS -> {
          // Cameraman aiming cinema camera, recording with tally light
          drawShootInProgress(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = accentGold,
            tallyAlpha = tallyAlpha
          )
        }

        CharacterState.SHOOT_COMPLETED -> {
          // Calmly walking away with camera bag, job done
          drawShootCompleted(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = accentGold,
            walkOffset = walkStep
          )
        }

        CharacterState.NOTHING_BOOKED -> {
          // Relaxed character standing/leaning near resting camera
          drawNothingBooked(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = accentGold,
            breathY = shoulderBreath
          )
        }

        CharacterState.NO_NEW_REQUESTS -> {
          // Calm crew member standing on set
          drawNoNewRequests(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = mutedGray,
            breathY = shoulderBreath
          )
        }

        CharacterState.ERROR -> {
          // Checking cable / tech setup
          drawErrorCharacter(
            w = w,
            h = h,
            darkColor = darkChar,
            mediumColor = mediumChar,
            lightColor = lightChar,
            accentColor = FameGoGold
          )
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// Individual Character Renderers (Minimal Editorial Linework & Silhouettes)
// -----------------------------------------------------------------------------

private fun DrawScope.drawFindingCrew(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color,
  breathY: Float,
  watchAngle: Float
) {
  val centerX = w * 0.48f
  val headY = h * 0.22f + breathY

  // Head & Beanie
  drawCircle(color = lightColor, radius = w * 0.085f, center = Offset(centerX, headY))
  drawArc(
    color = darkColor,
    startAngle = 180f,
    sweepAngle = 180f,
    useCenter = true,
    topLeft = Offset(centerX - w * 0.088f, headY - h * 0.092f),
    size = Size(w * 0.176f, h * 0.12f)
  )

  // Torso / Jacket
  val torsoY = headY + h * 0.09f
  drawRoundRect(
    color = darkColor,
    topLeft = Offset(centerX - w * 0.14f, torsoY),
    size = Size(w * 0.28f, h * 0.36f),
    cornerRadius = CornerRadius(8f, 8f)
  )

  // Left arm checking watch
  val shoulderL = Offset(centerX - w * 0.13f, torsoY + h * 0.04f)
  val elbowL = Offset(centerX - w * 0.22f, torsoY + h * 0.18f)
  val wristL = Offset(centerX - w * 0.05f, torsoY + h * 0.14f + watchAngle)

  drawLine(color = mediumColor, start = shoulderL, end = elbowL, strokeWidth = 5.5f)
  drawLine(color = mediumColor, start = elbowL, end = wristL, strokeWidth = 5.5f)

  // Watch on wrist (Gold accent dot/dial)
  drawCircle(color = accentColor, radius = 3.5f, center = wristL)

  // Right arm relaxed down
  val shoulderR = Offset(centerX + w * 0.13f, torsoY + h * 0.04f)
  val handR = Offset(centerX + w * 0.17f, torsoY + h * 0.32f)
  drawLine(color = mediumColor, start = shoulderR, end = handR, strokeWidth = 5f)

  // Legs / Cargo pants
  val hipY = torsoY + h * 0.34f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.07f, hipY), end = Offset(centerX - w * 0.07f, h * 0.88f), strokeWidth = 6.5f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.07f, hipY), end = Offset(centerX + w * 0.07f, h * 0.88f), strokeWidth = 6.5f)

  // Shoes
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.12f, h * 0.86f), size = Size(w * 0.08f, h * 0.04f), cornerRadius = CornerRadius(3f, 3f))
  drawRoundRect(color = darkColor, topLeft = Offset(centerX + w * 0.04f, h * 0.86f), size = Size(w * 0.08f, h * 0.04f), cornerRadius = CornerRadius(3f, 3f))
}

private fun DrawScope.drawRequestSent(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color,
  breathY: Float
) {
  val centerX = w * 0.38f
  val headY = h * 0.22f + breathY

  // Head
  drawCircle(color = lightColor, radius = w * 0.08f, center = Offset(centerX, headY))
  // Cap facing right
  drawArc(
    color = darkColor,
    startAngle = 180f,
    sweepAngle = 180f,
    useCenter = true,
    topLeft = Offset(centerX - w * 0.08f, headY - h * 0.085f),
    size = Size(w * 0.16f, h * 0.10f)
  )
  drawRoundRect(color = darkColor, topLeft = Offset(centerX + w * 0.02f, headY - h * 0.04f), size = Size(w * 0.10f, h * 0.02f), cornerRadius = CornerRadius(2f, 2f))

  // Torso
  val torsoY = headY + h * 0.085f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.12f, torsoY), size = Size(w * 0.24f, h * 0.36f), cornerRadius = CornerRadius(6f, 6f))

  // Legs
  val hipY = torsoY + h * 0.34f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.06f, hipY), end = Offset(centerX - w * 0.06f, h * 0.88f), strokeWidth = 6f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.06f, hipY), end = Offset(centerX + w * 0.06f, h * 0.88f), strokeWidth = 6f)

  // Camera rig standing beside on heavy duty C-stand/tripod
  val rigX = w * 0.74f
  // Stand column
  drawLine(color = Color(0xFF555964), start = Offset(rigX, h * 0.30f), end = Offset(rigX, h * 0.88f), strokeWidth = 3f)
  // Legs of stand
  drawLine(color = Color(0xFF555964), start = Offset(rigX, h * 0.76f), end = Offset(rigX - w * 0.12f, h * 0.88f), strokeWidth = 2.5f)
  drawLine(color = Color(0xFF555964), start = Offset(rigX, h * 0.76f), end = Offset(rigX + w * 0.12f, h * 0.88f), strokeWidth = 2.5f)

  // Camera Body
  drawRoundRect(color = darkColor, topLeft = Offset(rigX - w * 0.08f, h * 0.25f), size = Size(w * 0.16f, h * 0.10f), cornerRadius = CornerRadius(3f, 3f))
  // Cinema Lens (with gold rim)
  drawRoundRect(color = mediumColor, topLeft = Offset(rigX + w * 0.08f, h * 0.27f), size = Size(w * 0.09f, h * 0.06f), cornerRadius = CornerRadius(2f, 2f))
  drawCircle(color = accentColor, radius = 2.5f, center = Offset(rigX + w * 0.16f, h * 0.30f))
}

private fun DrawScope.drawCrewConfirmed(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color,
  breathY: Float
) {
  val centerX = w * 0.44f
  val headY = h * 0.20f + breathY

  // Head
  drawCircle(color = lightColor, radius = w * 0.085f, center = Offset(centerX, headY))
  // Headset / Comms
  drawArc(
    color = accentColor,
    startAngle = 170f,
    sweepAngle = 100f,
    useCenter = false,
    topLeft = Offset(centerX - w * 0.09f, headY - h * 0.09f),
    size = Size(w * 0.18f, h * 0.14f),
    style = Stroke(width = 2.5f)
  )

  // Body
  val torsoY = headY + h * 0.09f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.13f, torsoY), size = Size(w * 0.26f, h * 0.36f), cornerRadius = CornerRadius(6f, 6f))

  // Production vest badge / gold stripe
  drawLine(color = accentColor, start = Offset(centerX - w * 0.08f, torsoY + h * 0.12f), end = Offset(centerX - w * 0.03f, torsoY + h * 0.12f), strokeWidth = 2.5f)

  // Shouldering camera
  val camX = centerX + w * 0.10f
  val camY = torsoY + h * 0.02f
  drawRoundRect(color = mediumColor, topLeft = Offset(camX, camY), size = Size(w * 0.22f, h * 0.12f), cornerRadius = CornerRadius(4f, 4f))
  // Top handle
  drawLine(color = darkColor, start = Offset(camX + w * 0.04f, camY - 4f), end = Offset(camX + w * 0.18f, camY - 4f), strokeWidth = 3f)
  // Lens
  drawRoundRect(color = darkColor, topLeft = Offset(camX + w * 0.22f, camY + h * 0.03f), size = Size(w * 0.08f, h * 0.06f), cornerRadius = CornerRadius(2f, 2f))
  // Gold tally ring
  drawCircle(color = accentColor, radius = 3.5f, center = Offset(camX + w * 0.29f, camY + h * 0.06f))

  // Right arm supporting camera
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.10f, torsoY + h * 0.06f), end = Offset(camX + w * 0.12f, camY + h * 0.14f), strokeWidth = 5f)

  // Legs
  val hipY = torsoY + h * 0.34f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.06f, hipY), end = Offset(centerX - w * 0.06f, h * 0.88f), strokeWidth = 6f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.06f, hipY), end = Offset(centerX + w * 0.06f, h * 0.88f), strokeWidth = 6f)
}

private fun DrawScope.drawShootTomorrow(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color
) {
  val centerX = w * 0.35f
  val headY = h * 0.30f

  // Leaning slightly over case
  drawCircle(color = lightColor, radius = w * 0.08f, center = Offset(centerX, headY))
  val torsoY = headY + h * 0.08f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.11f, torsoY), size = Size(w * 0.22f, h * 0.30f), cornerRadius = CornerRadius(5f, 5f))

  // Arms reaching down towards case
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.08f, torsoY + h * 0.06f), end = Offset(w * 0.58f, h * 0.58f), strokeWidth = 5f)

  // Legs bent
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.05f, torsoY + h * 0.28f), end = Offset(centerX - w * 0.09f, h * 0.88f), strokeWidth = 5.5f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.04f, torsoY + h * 0.28f), end = Offset(centerX + w * 0.02f, h * 0.88f), strokeWidth = 5.5f)

  // Heavy duty flight case / Pelican case open
  val caseX = w * 0.52f
  val caseY = h * 0.56f
  drawRoundRect(color = Color(0xFF1E2024), topLeft = Offset(caseX, caseY), size = Size(w * 0.36f, h * 0.28f), cornerRadius = CornerRadius(4f, 4f))
  // Lid open upward
  drawLine(color = Color(0xFF33363F), start = Offset(caseX + w * 0.36f, caseY), end = Offset(caseX + w * 0.44f, caseY - h * 0.20f), strokeWidth = 4f)
  // Latch accents (Gold)
  drawRoundRect(color = accentColor, topLeft = Offset(caseX + w * 0.08f, caseY + h * 0.08f), size = Size(6f, 10f), cornerRadius = CornerRadius(1f, 1f))
  drawRoundRect(color = accentColor, topLeft = Offset(caseX + w * 0.24f, caseY + h * 0.08f), size = Size(6f, 10f), cornerRadius = CornerRadius(1f, 1f))
}

private fun DrawScope.drawCrewEnRoute(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color,
  walkOffset: Float
) {
  val centerX = w * 0.46f + walkOffset
  val headY = h * 0.22f

  // Head
  drawCircle(color = lightColor, radius = w * 0.08f, center = Offset(centerX, headY))

  // Torso
  val torsoY = headY + h * 0.085f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.11f, torsoY), size = Size(w * 0.22f, h * 0.34f), cornerRadius = CornerRadius(6f, 6f))

  // Heavy production backpack on back
  val bagX = centerX - w * 0.20f
  drawRoundRect(color = Color(0xFF1A1C20), topLeft = Offset(bagX, torsoY + h * 0.04f), size = Size(w * 0.12f, h * 0.26f), cornerRadius = CornerRadius(5f, 5f))
  // Tripod strapped to backpack with gold buckle
  drawLine(color = Color(0xFF555964), start = Offset(bagX - 2f, torsoY), end = Offset(bagX - 2f, torsoY + h * 0.32f), strokeWidth = 3f)
  drawCircle(color = accentColor, radius = 2.5f, center = Offset(bagX + w * 0.06f, torsoY + h * 0.16f))

  // Legs in walking stride
  val hipY = torsoY + h * 0.32f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.03f, hipY), end = Offset(centerX - w * 0.12f, h * 0.88f), strokeWidth = 5.5f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.03f, hipY), end = Offset(centerX + w * 0.14f, h * 0.86f), strokeWidth = 5.5f)
}

private fun DrawScope.drawShootInProgress(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color,
  tallyAlpha: Float
) {
  val centerX = w * 0.34f
  val headY = h * 0.24f

  // Head tilted into eyepiece
  drawCircle(color = lightColor, radius = w * 0.08f, center = Offset(centerX, headY))

  // Torso
  val torsoY = headY + h * 0.085f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.11f, torsoY), size = Size(w * 0.22f, h * 0.34f), cornerRadius = CornerRadius(6f, 6f))

  // Large camera on shoulder
  val camX = centerX + w * 0.06f
  val camY = headY - h * 0.02f
  drawRoundRect(color = mediumColor, topLeft = Offset(camX, camY), size = Size(w * 0.32f, h * 0.15f), cornerRadius = CornerRadius(4f, 4f))

  // Eyepiece connecting to operator's eye
  drawLine(color = darkColor, start = Offset(camX + w * 0.04f, camY + h * 0.08f), end = Offset(centerX + w * 0.04f, headY), strokeWidth = 4f)

  // Matte box / lens hood
  val hoodX = camX + w * 0.32f
  val hoodY = camY + h * 0.02f
  drawRoundRect(color = darkColor, topLeft = Offset(hoodX, hoodY), size = Size(w * 0.12f, h * 0.11f), cornerRadius = CornerRadius(2f, 2f))

  // Red LIVE Tally Light on top of matte box
  drawCircle(
    color = FameGoLiveRed.copy(alpha = tallyAlpha),
    radius = 4f,
    center = Offset(camX + w * 0.24f, camY - 2f)
  )

  // Arms supporting rig
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.08f, torsoY + h * 0.08f), end = Offset(camX + w * 0.20f, camY + h * 0.18f), strokeWidth = 5f)

  // Legs in athletic shooting stance
  val hipY = torsoY + h * 0.32f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.04f, hipY), end = Offset(centerX - w * 0.12f, h * 0.88f), strokeWidth = 6f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.04f, hipY), end = Offset(centerX + w * 0.12f, h * 0.88f), strokeWidth = 6f)
}

private fun DrawScope.drawShootCompleted(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color,
  walkOffset: Float
) {
  // Calm production person walking away, back visible, job done
  val centerX = w * 0.50f + (walkOffset * 0.5f)
  val headY = h * 0.20f

  // Head from back (beanie/cap)
  drawCircle(color = darkColor, radius = w * 0.085f, center = Offset(centerX, headY))

  // Torso
  val torsoY = headY + h * 0.09f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.13f, torsoY), size = Size(w * 0.26f, h * 0.35f), cornerRadius = CornerRadius(7f, 7f))

  // Production sling bag across back
  drawLine(color = accentColor, start = Offset(centerX - w * 0.12f, torsoY + 4f), end = Offset(centerX + w * 0.12f, torsoY + h * 0.30f), strokeWidth = 3f)

  // Carrying camera case in right hand
  val caseX = centerX + w * 0.18f
  val caseY = torsoY + h * 0.24f
  drawRoundRect(color = Color(0xFF1E2024), topLeft = Offset(caseX, caseY), size = Size(w * 0.18f, h * 0.24f), cornerRadius = CornerRadius(3f, 3f))
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.10f, torsoY + h * 0.06f), end = Offset(caseX + w * 0.08f, caseY), strokeWidth = 4.5f)

  // Legs walking away
  val hipY = torsoY + h * 0.33f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.06f, hipY), end = Offset(centerX - w * 0.04f, h * 0.88f), strokeWidth = 6f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.06f, hipY), end = Offset(centerX + w * 0.09f, h * 0.86f), strokeWidth = 6f)
}

private fun DrawScope.drawNothingBooked(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color,
  breathY: Float
) {
  val centerX = w * 0.42f
  val headY = h * 0.24f + breathY

  // Head
  drawCircle(color = lightColor, radius = w * 0.085f, center = Offset(centerX, headY))

  // Torso (relaxed posture, hands in pockets)
  val torsoY = headY + h * 0.09f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.12f, torsoY), size = Size(w * 0.24f, h * 0.36f), cornerRadius = CornerRadius(6f, 6f))

  // Arms into jacket pockets
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.11f, torsoY + h * 0.06f), end = Offset(centerX - w * 0.06f, torsoY + h * 0.28f), strokeWidth = 5f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.11f, torsoY + h * 0.06f), end = Offset(centerX + w * 0.06f, torsoY + h * 0.28f), strokeWidth = 5f)

  // Camera resting securely on table / riser nearby
  val riserX = w * 0.72f
  val riserY = h * 0.58f
  drawRoundRect(color = Color(0xFF26282E), topLeft = Offset(riserX - w * 0.08f, riserY), size = Size(w * 0.22f, h * 0.30f), cornerRadius = CornerRadius(3f, 3f))
  // Camera sitting on top
  drawRoundRect(color = mediumColor, topLeft = Offset(riserX - w * 0.06f, riserY - h * 0.10f), size = Size(w * 0.14f, h * 0.10f), cornerRadius = CornerRadius(3f, 3f))
  drawCircle(color = accentColor, radius = 2.5f, center = Offset(riserX + w * 0.06f, riserY - h * 0.05f))

  // Legs
  val hipY = torsoY + h * 0.34f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.06f, hipY), end = Offset(centerX - w * 0.06f, h * 0.88f), strokeWidth = 6f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.06f, hipY), end = Offset(centerX + w * 0.06f, h * 0.88f), strokeWidth = 6f)
}

private fun DrawScope.drawNoNewRequests(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color,
  breathY: Float
) {
  val centerX = w * 0.50f
  val headY = h * 0.24f + breathY

  // Head
  drawCircle(color = lightColor, radius = w * 0.08f, center = Offset(centerX, headY))

  // Torso
  val torsoY = headY + h * 0.09f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.12f, torsoY), size = Size(w * 0.24f, h * 0.36f), cornerRadius = CornerRadius(6f, 6f))

  // Slung radio / walkie talkie
  drawLine(color = accentColor, start = Offset(centerX - w * 0.06f, torsoY + h * 0.08f), end = Offset(centerX - w * 0.06f, torsoY + h * 0.04f), strokeWidth = 2f)

  // Legs
  val hipY = torsoY + h * 0.34f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.06f, hipY), end = Offset(centerX - w * 0.06f, h * 0.88f), strokeWidth = 6f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.06f, hipY), end = Offset(centerX + w * 0.06f, h * 0.88f), strokeWidth = 6f)
}

private fun DrawScope.drawErrorCharacter(
  w: Float,
  h: Float,
  darkColor: Color,
  mediumColor: Color,
  lightColor: Color,
  accentColor: Color
) {
  val centerX = w * 0.45f
  val headY = h * 0.25f

  // Head inspecting device
  drawCircle(color = lightColor, radius = w * 0.08f, center = Offset(centerX, headY))

  // Torso
  val torsoY = headY + h * 0.085f
  drawRoundRect(color = darkColor, topLeft = Offset(centerX - w * 0.11f, torsoY), size = Size(w * 0.22f, h * 0.34f), cornerRadius = CornerRadius(5f, 5f))

  // Holding diagnostic tool / monitor device in hands
  val devX = centerX + w * 0.08f
  val devY = torsoY + h * 0.12f
  drawRoundRect(color = accentColor, topLeft = Offset(devX, devY), size = Size(w * 0.16f, h * 0.12f), cornerRadius = CornerRadius(2f, 2f))
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.08f, torsoY + h * 0.08f), end = Offset(devX + 2f, devY + h * 0.06f), strokeWidth = 4.5f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.08f, torsoY + h * 0.08f), end = Offset(devX + w * 0.14f, devY + h * 0.06f), strokeWidth = 4.5f)

  // Legs
  val hipY = torsoY + h * 0.32f
  drawLine(color = mediumColor, start = Offset(centerX - w * 0.05f, hipY), end = Offset(centerX - w * 0.05f, h * 0.88f), strokeWidth = 5.5f)
  drawLine(color = mediumColor, start = Offset(centerX + w * 0.05f, hipY), end = Offset(centerX + w * 0.05f, h * 0.88f), strokeWidth = 5.5f)
}

/**
 * Editorial Living Card Hero Component with Situational Character
 */
@Composable
fun FameGoLivingHeroCard(
  state: CharacterState,
  title: String,
  subtitle: String,
  primaryActionLabel: String? = null,
  onPrimaryAction: (() -> Unit)? = null,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  SoftCard(
    isElevated = true,
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 18.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 15-30% Visual Character Area
      FameGoCharacterIllustration(
        state = state,
        size = 84.dp,
        modifier = Modifier.padding(end = 14.dp)
      )

      // Content & Meaning
      Column(modifier = Modifier.weight(1f)) {
        // Status pill indicator
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val dotColor = when (state) {
            CharacterState.SHOOT_IN_PROGRESS -> FameGoLiveRed
            CharacterState.CREW_CONFIRMED, CharacterState.SHOOT_COMPLETED -> FameGoSuccessGreen
            CharacterState.FINDING_CREW, CharacterState.REQUEST_SENT, CharacterState.CREW_EN_ROUTE -> FameGoGold
            else -> FameGoTextMuted
          }
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(dotColor)
          )
          Text(
            text = when (state) {
              CharacterState.FINDING_CREW -> "Finding your crew"
              CharacterState.REQUEST_SENT -> "Request active"
              CharacterState.CREW_CONFIRMED -> "Crew confirmed"
              CharacterState.SHOOT_TOMORROW -> "Shoot tomorrow"
              CharacterState.CREW_EN_ROUTE -> "On the way"
              CharacterState.SHOOT_IN_PROGRESS -> "Shoot in progress"
              CharacterState.SHOOT_COMPLETED -> "Shoot completed"
              CharacterState.NOTHING_BOOKED -> "Nothing scheduled"
              CharacterState.NO_NEW_REQUESTS -> "On standby"
              CharacterState.ERROR -> "Action needed"
            },
            color = dotColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = title,
          color = FameGoWhite,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          lineHeight = 20.sp
        )

        Text(
          text = subtitle,
          color = FameGoTextSecondary,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          modifier = Modifier.padding(top = 2.dp)
        )

        // Actions
        if (primaryActionLabel != null && onPrimaryAction != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(FameGoGold)
                .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Text(
                text = primaryActionLabel,
                color = Color(0xFF141518),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
              Text(
                text = secondaryActionLabel,
                color = FameGoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }
  }
}
