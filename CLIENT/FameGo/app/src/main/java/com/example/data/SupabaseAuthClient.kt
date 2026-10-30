package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class SupabaseAuthResult(
  val id: String,
  val email: String,
  val accessToken: String,
  val refreshToken: String
)

/** Small REST auth adapter kept independent from the app's UI and local state. */
object SupabaseAuthClient {
  private val http = SupabaseNetwork.http
  private val jsonType = "application/json".toMediaType()

  /**
   * Where Supabase sends the user after they tap "Yes, it's me" in the
   * confirmation email. This is the hosted "You're verified" page.
   * The same URL must ALSO be allow-listed in Supabase Dashboard >
   * Authentication > URL Configuration > Redirect URLs.
   */
  const val EMAIL_REDIRECT_URI = "famego://auth/callback"
  const val WEB_VERIFY_URL = "https://famebrosstudio.github.io/FameGo/verified.html"

  suspend fun authenticate(
    email: String,
    password: String,
    signUp: Boolean,
    name: String,
    phone: String,
    role: String,
    companyName: String,
    dobIso: String = ""
  ): Result<SupabaseAuthResult> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured) return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
    // redirect_to MUST be a URL query param (GoTrue rejects it in the body
    // on some versions with 400). Keep the JSON body minimal.
    // The web "You're verified" page is the landing target; the app's
    // deep link stays registered for the in-app "Open FameGo" button.
    val endpoint = if (signUp) {
      "${SupabaseConfig.baseUrl}/auth/v1/signup?redirect_to=${java.net.URLEncoder.encode(WEB_VERIFY_URL, "UTF-8")}"
    } else {
      "${SupabaseConfig.baseUrl}/auth/v1/token?grant_type=password"
    }
    val body = JSONObject().apply {
      put("email", email)
      put("password", password)
      if (signUp) {
        put("data", JSONObject().apply {
          put("full_name", name)
          put("phone", phone)
          put("role", role)
          put("company_name", companyName)
          if (dobIso.isNotBlank()) put("dob", dobIso)
        })
      }
    }.toString().toRequestBody(jsonType)
    val request = Request.Builder()
      .url(endpoint)
      .header("apikey", SupabaseConfig.publishableKey)
      .header("Authorization", "Bearer ${SupabaseConfig.publishableKey}")
      .header("Content-Type", "application/json")
      .post(body)
      .build()
    runCatching {
      http.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          val parsed = runCatching { JSONObject(raw) }.getOrNull()
          val message = parsed?.optString("msg").orEmpty()
            .ifBlank { parsed?.optString("message").orEmpty() }
            .ifBlank { parsed?.optString("error_description").orEmpty() }
            .ifBlank { parsed?.optString("error").orEmpty() }
            .ifBlank { raw.take(300) }
            .ifBlank { "Authentication failed (${response.code})" }
          android.util.Log.e("FameGoAuth", "signup/signin ${response.code}: $raw")
          error("$message (code ${response.code})")
        }
        val root = JSONObject(raw)
        val user = root.optJSONObject("user") ?: root
        val accessToken = root.optString("access_token")
        if (accessToken.isBlank()) {
          // Email-confirmation flow: Supabase created the user but issued no
          // session. The email link now opens the app (famego://auth/callback)
          // and auto-confirms — the user just taps "Yes, it's me".
          error("CHECK_EMAIL:Account created. Open your email and tap \"Yes, it's me\" — the FameGo app will confirm you automatically, then sign in.")
        }
        SupabaseAuthResult(
          id = user.optString("id").ifBlank { error("Supabase did not return a user id") },
          email = user.optString("email", email),
          accessToken = accessToken,
          refreshToken = root.optString("refresh_token")
        ).also { SupabaseSession.save(it.accessToken, it.refreshToken) }
      }
    }
  }

  /**
   * Starts "forgot password": Supabase mails a reset link that opens the app
   * (redirect_to famego://auth/callback, allowlisted) for a fresh password.
   */
  suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured) {
      return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
    }
    val endpoint = "${SupabaseConfig.baseUrl}/auth/v1/recover" +
      "?redirect_to=${java.net.URLEncoder.encode(EMAIL_REDIRECT_URI, "UTF-8")}"
    val body = JSONObject().apply { put("email", email.trim()) }
      .toString().toRequestBody(jsonType)
    val request = Request.Builder()
      .url(endpoint)
      .header("apikey", SupabaseConfig.publishableKey)
      .header("Authorization", "Bearer ${SupabaseConfig.publishableKey}")
      .header("Content-Type", "application/json")
      .post(body).build()
    runCatching {
      http.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) error(raw.take(240).ifBlank { "Reset failed (${response.code})" })
      }
    }
  }

  /**
   * Sets a new password for the current recovery session (user tapped the
   * reset link and the app holds a fresh access token).
   */
  suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
    val token = SupabaseSession.accessToken
    if (!SupabaseConfig.isConfigured || token.isNullOrBlank()) {
      return@withContext Result.failure(IllegalStateException("That reset link expired. Request a fresh one."))
    }
    val body = JSONObject().apply { put("password", newPassword) }
      .toString().toRequestBody(jsonType)
    val request = Request.Builder()
      .url("${SupabaseConfig.baseUrl}/auth/v1/user")
      .header("apikey", SupabaseConfig.publishableKey)
      .header("Authorization", "Bearer $token")
      .header("Content-Type", "application/json")
      .put(body).build()
    runCatching {
      http.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) error(raw.take(240).ifBlank { "Update failed (${response.code})" })
      }
    }
  }
  suspend fun resendConfirmation(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured) {
      return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
    }
    val body = JSONObject().apply {
      put("type", "signup")
      put("email", email.trim())
    }.toString().toRequestBody(jsonType)
    val request = Request.Builder()
      .url("${SupabaseConfig.baseUrl}/auth/v1/resend")
      .header("apikey", SupabaseConfig.publishableKey)
      .header("Authorization", "Bearer ${SupabaseConfig.publishableKey}")
      .header("Content-Type", "application/json")
      .post(body).build()
    runCatching {
      http.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) error(raw.take(240).ifBlank { "Resend failed (${response.code})" })
      }
    }
  }

  suspend fun restoreSession(): Result<SupabaseAuthResult?> = withContext(Dispatchers.IO) {
    val refresh = SupabaseSession.refreshToken
    if (!SupabaseConfig.isConfigured || refresh.isNullOrBlank()) return@withContext Result.success(null)
    val body = JSONObject().put("refresh_token", refresh).toString().toRequestBody(jsonType)
    val request = Request.Builder()
      .url("${SupabaseConfig.baseUrl}/auth/v1/token?grant_type=refresh_token")
      .header("apikey", SupabaseConfig.publishableKey)
      .header("Authorization", "Bearer ${SupabaseConfig.publishableKey}")
      .header("Content-Type", "application/json")
      .post(body).build()
    runCatching {
      http.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          // Only drop the local session when the server says the refresh
          // token itself is invalid. Transient 5xx / network errors keep it.
          if (response.code == 400 || response.code == 401) SupabaseSession.clear()
          else android.util.Log.w("FameGoAuth", "restoreSession HTTP ${response.code}: ${raw.take(200)}")
          return@use null
        }
        val root = JSONObject(raw)
        val user = root.optJSONObject("user") ?: return@use null
        val access = root.optString("access_token")
        val newRefresh = root.optString("refresh_token")
        if (access.isBlank() || newRefresh.isBlank() || user.optString("id").isBlank()) return@use null
        SupabaseAuthResult(
          id = user.getString("id"),
          email = user.optString("email"),
          accessToken = access,
          refreshToken = newRefresh
        ).also { SupabaseSession.save(it.accessToken, it.refreshToken) }
      }
    }
  }

  fun friendlyMessage(error: Throwable, signingUp: Boolean): String {
    val raw = error.message.orEmpty()
    val value = raw.lowercase()
    // Real transport failures first — these never carry server text, so check
    // the exception type (not a substring) to avoid mislabeling server errors.
    if (SupabaseNetwork.isNetworkFailure(error)) {
      return when {
        "unable to resolve host" in value || "unknownhost" in value || "no address" in value ->
          "Can't reach FameGo servers (DNS). Check your connection or VPN/DNS settings and try again."
        "timeout" in value || "timed out" in value ->
          "Server is taking too long to respond. Check your connection and try again."
        else -> "No connection to server. Check internet and try again."
      }
    }
    return when {
      "already registered" in value || "already exists" in value || "user already" in value ->
        "An account already exists for this email. Try Sign in."
      "invalid login" in value || "invalid credentials" in value -> "Email or password is incorrect."
      "invalid api key" in value || "no api key" in value ->
        "Server config error. Please update the app and try again."
      "redirect" in value && ("not allowed" in value || "url" in value) ->
        "Server rejected signup (email redirect not allowed). Contact support."
      "weak password" in value || "password too short" in value || "password should" in value ->
        "Password is too weak. Use at least 6 characters with letters and numbers."
      "rate limit" in value || "too many" in value || "code 429" in value ->
        "Too many attempts. Wait a minute and try again."
      "check_email" in value -> "Account created. Open your email and tap \"Yes, it's me\" — the app confirms you automatically."
      "email not confirmed" in value -> "Tap \"Yes, it's me\" in your email, then sign in."
      "expired" in value && ("link" in value || "token" in value || "otp" in value) -> "That email link expired. Sign in to get a fresh one."
      "code 400" in value || "code 422" in value -> "Server rejected signup: ${raw.take(220)}"
      "code 401" in value || "code 403" in value -> "Server refused the request (${raw.take(120)}). Update the app and retry."
      "code 500" in value || "code 502" in value || "code 503" in value ->
        "Server error (${raw.take(120)}). Check Supabase Auth logs, then retry."
      signingUp -> "Unable to create account ($raw).".take(300)
      else -> "Unable to sign in ($raw).".take(300)
    }
  }

  /**
   * Confirms the email link the user tapped ("Yes, it's me").
   * Handles both flows Supabase may send:
   *  - PKCE:  ?token_hash=...&type=signup|email|recovery  -> POST /auth/v1/verify
   *  - Implicit: #access_token=...&refresh_token=...      -> saved directly
   */
  suspend fun confirmEmailLink(
    queryParams: Map<String, String>,
    fragmentParams: Map<String, String>
  ): Result<SupabaseAuthResult> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured) {
      return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
    }
    // 1) Implicit flow — tokens arrive in the URL fragment.
    val implicitAccess = fragmentParams["access_token"].orEmpty()
    val implicitRefresh = fragmentParams["refresh_token"].orEmpty()
    if (implicitAccess.isNotBlank()) {
      val id = parseJwtSub(implicitAccess).orEmpty()
      SupabaseSession.save(implicitAccess, implicitRefresh)
      return@withContext Result.success(
        SupabaseAuthResult(
          id = id,
          email = fragmentParams["email"].orEmpty(),
          accessToken = implicitAccess,
          refreshToken = implicitRefresh
        )
      )
    }
    // 2) PKCE / token-hash flow — exchange the one-time hash for a session.
    val tokenHash = queryParams["token_hash"].orEmpty()
      .ifBlank { fragmentParams["token_hash"].orEmpty() }
    val type = queryParams["type"].orEmpty()
      .ifBlank { fragmentParams["type"].orEmpty() }
      .ifBlank { "signup" }
    if (tokenHash.isBlank()) {
      val err = queryParams["error_description"].orEmpty()
        .ifBlank { fragmentParams["error_description"].orEmpty() }
      return@withContext Result.failure(
        IllegalStateException(err.ifBlank { "That email link is invalid or expired. Please sign in again." })
      )
    }
    val body = JSONObject().apply {
      put("type", type)
      put("token_hash", tokenHash)
    }.toString().toRequestBody(jsonType)
    val request = Request.Builder()
      .url("${SupabaseConfig.baseUrl}/auth/v1/verify")
      .header("apikey", SupabaseConfig.publishableKey)
      .header("Authorization", "Bearer ${SupabaseConfig.publishableKey}")
      .header("Content-Type", "application/json")
      .post(body)
      .build()
    runCatching {
      http.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          val message = runCatching {
            JSONObject(raw).optString("msg").ifBlank { JSONObject(raw).optString("message") }
          }.getOrNull()
          error(message?.ifBlank { "This link expired. Please sign in again." }
            ?: "This link expired. Please sign in again.")
        }
        val root = JSONObject(raw)
        val user = root.optJSONObject("user") ?: JSONObject()
        val access = root.optString("access_token")
        val refresh = root.optString("refresh_token")
        if (access.isBlank()) error("Confirmation worked — please sign in now.")
        SupabaseAuthResult(
          id = user.optString("id").ifBlank { parseJwtSub(access).orEmpty() },
          email = user.optString("email"),
          accessToken = access,
          refreshToken = refresh
        ).also { SupabaseSession.save(it.accessToken, it.refreshToken) }
      }
    }
  }

  /** Reads the `sub` (user id) claim out of a JWT without extra deps. */
  private fun parseJwtSub(jwt: String): String? = runCatching {
    val parts = jwt.split(".")
    if (parts.size < 2) return null
    var payload = parts[1].replace('-', '+').replace('_', '/')
    repeat((4 - payload.length % 4) % 4) { payload += "=" }
    val json = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
      .toString(Charsets.UTF_8)
    JSONObject(json).optString("sub").ifBlank { null }
  }.getOrNull()
}
