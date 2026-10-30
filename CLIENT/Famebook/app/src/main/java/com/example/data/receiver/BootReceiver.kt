package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.service.CrewDispatchService
import com.example.data.service.CrewDispatchStore

/**
 * Restarts crew dispatch after a device reboot when dispatch was left on.
 * The service restores its own session and resumes backend polling.
 */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    if (!CrewDispatchStore(context).isDispatchOn()) return
    try {
      ContextCompat.startForegroundService(
        context,
        Intent(context, CrewDispatchService::class.java).apply {
          action = CrewDispatchService.ACTION_START_DISPATCH
        }
      )
    } catch (_: Exception) {
      // System not ready yet; next app open restores dispatch.
    }
  }
}
