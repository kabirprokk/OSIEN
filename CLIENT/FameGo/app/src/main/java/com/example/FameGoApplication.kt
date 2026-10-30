package com.example

import android.app.Application
import com.example.data.SupabaseConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit

/**
 * App entry: preloads UI sounds and installs the crash reporter.
 * Uncaught crashes are posted to Supabase crash_reports (009) on a spare
 * thread with a hard timeout, then the previous handler finishes the kill.
 */
class FameGoApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    com.example.data.FameGoSfx.ensure(this)
    installCrashReporter()
  }

  private fun installCrashReporter() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
      runCatching { reportCrash(error) }
      previous?.uncaughtException(thread, error)
    }
  }

  private fun reportCrash(error: Throwable) {
    if (!SupabaseConfig.isConfigured) return
    val trace = StringWriter().apply {
      error.printStackTrace(PrintWriter(this))
    }.toString().take(8000)
    val body = JSONObject().apply {
      put("app_version", try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
      } catch (_: Exception) { "?" })
      put("android_version", android.os.Build.VERSION.RELEASE ?: "?")
      put("device_model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim())
      put("stacktrace", "${error::class.java.name}: ${error.message}\n$trace")
    }.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
      .url("${SupabaseConfig.baseUrl}/rest/v1/crash_reports")
      .header("apikey", SupabaseConfig.publishableKey)
      .header("Authorization", "Bearer ${SupabaseConfig.publishableKey}")
      .header("Content-Type", "application/json")
      .post(body)
      .build()
    // Best-effort with a hard cap: reporting must never delay the kill.
    val worker = Thread {
      runCatching {
        OkHttpClient.Builder()
          .connectTimeout(3, TimeUnit.SECONDS)
          .writeTimeout(3, TimeUnit.SECONDS)
          .readTimeout(3, TimeUnit.SECONDS)
          .build()
          .newCall(request).execute().close()
      }
    }
    worker.isDaemon = true
    worker.start()
    runCatching { worker.join(4000) }
  }
}
