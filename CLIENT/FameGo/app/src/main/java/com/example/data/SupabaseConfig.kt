package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** Public Supabase client settings. The database password must never be shipped in the app. */
object SupabaseConfig {
  val baseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/')
  val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
  val mapTilerKey: String = BuildConfig.MAPTILER_KEY

  val isConfigured: Boolean
    get() = baseUrl.startsWith("https://") &&
      !baseUrl.contains("your-project") &&
      publishableKey.startsWith("sb_publishable_")
}

object SupabaseSession {
  private const val PREFS = "famego_session"
  private const val ACCESS = "access_token"
  private const val REFRESH = "refresh_token"
  private const val PROFILE = "profile_json"
  private var context: Context? = null
  @Volatile var accessToken: String? = null
    private set
  @Volatile var refreshToken: String? = null
    private set

  fun initialize(appContext: Context) {
    context = appContext.applicationContext
    val prefs = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    accessToken = prefs?.getString(ACCESS, null)
    refreshToken = prefs?.getString(REFRESH, null)
  }

  /** Never let a blank token wipe a good saved session (e.g. fragment flow). */
  fun save(access: String, refresh: String) {
    if (access.isBlank()) return
    accessToken = access
    if (refresh.isNotBlank()) refreshToken = refresh
    context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.let { editor ->
      editor.putString(ACCESS, access)
      if (refresh.isNotBlank()) editor.putString(REFRESH, refresh)
      editor.apply()
    }
  }

  /** Cached profile so a valid session survives offline/slow launches. */
  fun saveProfile(user: com.example.model.User) {
    if (user.id.isBlank()) return
    val json = org.json.JSONObject().apply {
      put("id", user.id)
      put("name", user.name)
      put("email", user.email)
      put("phone", user.phone)
      put("company_name", user.companyName)
      put("role", user.role.name)
      put("avatar_initials", user.avatarInitials)
      put("dob", user.dob)
    }.toString()
    context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
      ?.putString(PROFILE, json)?.apply()
  }

  fun cachedProfile(): com.example.model.User? = runCatching {
    val raw = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      ?.getString(PROFILE, null) ?: return null
    val o = org.json.JSONObject(raw)
    if (o.optString("id").isBlank()) return null
    com.example.model.User(
      id = o.optString("id"),
      name = o.optString("name"),
      email = o.optString("email"),
      phone = o.optString("phone"),
      companyName = o.optString("company_name"),
      role = runCatching { com.example.model.Role.valueOf(o.optString("role")) }
        .getOrDefault(com.example.model.Role.CLIENT),
      avatarInitials = o.optString("avatar_initials").ifBlank { "FG" },
      dob = o.optString("dob")
    )
  }.getOrNull()

  fun clear() {
    accessToken = null
    refreshToken = null
    context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
  }
}

/**
 * Shared transport helpers so every Supabase call fails fast with a clear,
 * diagnosable error instead of a generic "no connection".
 */
object SupabaseNetwork {
  val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

  /** True only for real transport problems (DNS, refused, timeout) — never for server text. */
  fun isNetworkFailure(e: Throwable): Boolean {
    var cause: Throwable? = e
    while (cause != null) {
      if (cause is UnknownHostException || cause is SocketTimeoutException || cause is ConnectException) return true
      if (cause is IOException &&
        cause.message?.contains("Unable to resolve host", ignoreCase = true) == true
      ) return true
      cause = cause.cause
    }
    return false
  }

  /** False when the device itself has no usable network (airplane mode, no Wi-Fi/data). */
  fun isDeviceOnline(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return true
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
