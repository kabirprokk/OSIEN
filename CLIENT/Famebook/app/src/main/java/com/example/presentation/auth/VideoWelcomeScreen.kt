package com.example.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.presentation.components.PrimaryGoldButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay

/**
 * Full-screen looping video welcome. Audio plays, no player chrome is shown.
 * The sign-up CTA rises in with a soft slide+fade once the first loop ends
 * (with a time fallback so slow decodes never trap the user).
 */
@Composable
fun VideoWelcomeScreen(
  onSignUpClick: () -> Unit,
  onSignInClick: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var showCta by remember { mutableStateOf(false) }

  val player = remember {
    ExoPlayer.Builder(context).build().apply {
      val uri = android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.famebook_bg}")
      setMediaItem(MediaItem.fromUri(uri))
      repeatMode = Player.REPEAT_MODE_OFF
      volume = 1f
      playWhenReady = true
      addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_ENDED) {
            showCta = true
            seekTo(0)
            play()
          }
        }
      })
      prepare()
    }
  }

  // Fallback: never leave the user staring at video with no action.
  LaunchedEffect(Unit) {
    delay(15000)
    showCta = true
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> player.pause()
        Lifecycle.Event.ON_RESUME -> if (!showCta || player.playbackState != Player.STATE_IDLE) player.play()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      player.release()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) {
        // Tapping anywhere skips the intro and reveals the CTA immediately.
        showCta = true
      }
  ) {
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          this.player = player
          useController = false
          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
      },
      modifier = Modifier.fillMaxSize()
    )

    // Readability scrim: light on top, heavy at the bottom behind the CTA.
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0x55000000),
              Color(0x00000000),
              Color(0xCC000000)
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 28.dp, vertical = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.weight(1f))

      AnimatedVisibility(
        visible = !showCta,
        enter = fadeIn(animationSpec = tween(durationMillis = 400)),
        exit = fadeOut(animationSpec = tween(durationMillis = 250))
      ) {
        Text(
          text = "TAP TO SKIP",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          ),
          color = PureWhite.copy(alpha = 0.7f),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 24.dp)
        )
      }

      AnimatedVisibility(
        visible = showCta,
        enter = slideInVertically(
          initialOffsetY = { it / 2 },
          animationSpec = tween(durationMillis = 550)
        ) + fadeIn(animationSpec = tween(durationMillis = 450)),
        exit = fadeOut()
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          PrimaryGoldButton(
            text = "CREATE ACCOUNT",
            onClick = onSignUpClick,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("welcome_signup_button")
          )
          TextButton(onClick = onSignInClick) {
            Text(
              text = "Already have an account? Sign in",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = PureWhite
            )
          }
          Text(
            text = "Book verified FameBros crew in seconds",
            style = MaterialTheme.typography.bodySmall,
            color = AmberGold,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
