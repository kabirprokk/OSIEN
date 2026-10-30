package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.notification.IncomingRequestNotificationHelper
import com.example.data.receiver.ShootRequestActionReceiver
import com.example.di.ServiceLocator
import com.example.presentation.navigation.AppNavigation
import com.example.ui.theme.FameBookTheme
import com.example.ui.theme.ObsidianBlack

class MainActivity : ComponentActivity() {

  private var targetBookingId by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    ServiceLocator.init(this)
    IncomingRequestNotificationHelper.createNotificationChannels(this)
    targetBookingId = intent?.getStringExtra(ShootRequestActionReceiver.EXTRA_BOOKING_ID)

    setContent {
      FameBookTheme {
        // Request POST_NOTIFICATIONS runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
          ) { /* granted or denied handled gracefully */ }

          LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS
              ) != PackageManager.PERMISSION_GRANTED
            ) {
              permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
        }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = ObsidianBlack
        ) {
          AppNavigation(
            initialBookingId = targetBookingId,
            onBookingIdHandled = { targetBookingId = null }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.getStringExtra(ShootRequestActionReceiver.EXTRA_BOOKING_ID)?.let {
      targetBookingId = it
    }
  }
}

