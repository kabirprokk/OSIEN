package com.example.data.repository

import com.example.BuildConfig
import com.example.data.remote.SupabaseHttpClient
import com.example.data.remote.SupabaseSession
import com.example.data.remote.SupabaseSessionStore
import android.net.Uri
import com.example.domain.model.AuthState
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SupabaseUserRepository(
  private val api: SupabaseHttpClient = SupabaseHttpClient(
    BuildConfig.SUPABASE_URL,
    BuildConfig.SUPABASE_PUBLISHABLE_KEY
  ),
  private val session: SupabaseSession = SupabaseSession(),
  private val sessionStore: SupabaseSessionStore? = null
) : UserRepository {
  private val _currentUser = MutableStateFlow<User?>(null)
  override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
  private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()
  private val accessToken: String?
    get() = session.accessToken

  override suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
    _authState.value = AuthState.Authenticating
    val payload = JSONObject().put("email", email.trim()).put("password", password)
    requestUser("POST", "auth/v1/token?grant_type=password", payload.toString(), email)
  }

  override suspend fun register(
    name: String,
    email: String,
    phone: String,
    role: UserRole,
    companyOrSpecialty: String?,
    password: String
  ): Result<User> = withContext(Dispatchers.IO) {
    if (role != UserRole.CLIENT) return@withContext failure("New accounts are created as clients.")
    val payload = JSONObject()
      .put("email", email.trim())
      .put("password", password)
      .put("data", JSONObject().put("full_name", name.trim()))
    runCatching {
      val redirect = Uri.encode("famebook://auth/callback")
      api.request("POST", "auth/v1/signup?redirect_to=$redirect", payload.toString()).use { response ->
        if (!response.isSuccessful) error(response.errorMessage())
        val json = JSONObject(response.body?.string().orEmpty())
        val session = json.optJSONObject("session")
        val user = json.optJSONObject("user") ?: json
        val token = session?.optString("access_token").orEmpty()
        if (token.isBlank()) error("Account created. Confirm your email before signing in.")
        this@SupabaseUserRepository.session.accessToken = token
        this@SupabaseUserRepository.session.refreshToken =
          session?.optString("refresh_token")?.takeIf { it.isNotBlank() }
        sessionStore?.save(
          this@SupabaseUserRepository.session.accessToken,
          this@SupabaseUserRepository.session.refreshToken ?: sessionStore?.refreshToken()
        )
        loadProfile(user.getString("id"), name, email, phone)
      }
    }.fold({ user -> setAuthenticated(user) }, { failure(it.message ?: "Account creation failed.") })
  }

  override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
    val token = accessToken
    // Best-effort remote sign-out; local session always clears so the user
    // is signed out on this device even if the network call fails.
    runCatching { token?.let { api.request("POST", "auth/v1/logout", accessToken = it).use { } } }
    session.accessToken = null
    session.refreshToken = null
    session.currentUser = null
    sessionStore?.clear()
    _currentUser.value = null
    _authState.value = AuthState.Unauthenticated
    Result.success(Unit)
  }

  override suspend fun restoreSession(): Result<User> = withContext(Dispatchers.IO) {
    if (_currentUser.value != null) return@withContext Result.success(_currentUser.value!!)
    val storedRefresh = session.refreshToken ?: sessionStore?.refreshToken()
    val storedAccess = session.accessToken ?: sessionStore?.accessToken()
    if (storedRefresh.isNullOrBlank() && storedAccess.isNullOrBlank()) {
      return@withContext Result.failure(IllegalStateException("No saved session"))
    }
    // Prefer refresh flow so expired access tokens still restore.
    if (!storedRefresh.isNullOrBlank()) {
      val refreshed = tryRefresh(storedRefresh)
      if (refreshed.isSuccess) return@withContext refreshed
    }
    // Fall back to stored access token (may still be valid).
    if (!storedAccess.isNullOrBlank()) {
      session.accessToken = storedAccess
      runCatching {
        api.request("GET", "auth/v1/user", accessToken = storedAccess).use { response ->
          if (!response.isSuccessful) error("Saved session expired. Please sign in again.")
          val authUser = JSONObject(response.body?.string().orEmpty())
          loadProfile(authUser.getString("id"), "", authUser.optString("email"), "")
        }
      }.fold(
        { setAuthenticated(it) },
        {
          session.accessToken = null
          sessionStore?.clear()
          Result.failure(it)
        }
      )
    } else {
      Result.failure(IllegalStateException("No saved session"))
    }
  }

  override suspend fun updateProfile(name: String, email: String, phone: String, bio: String, company: String?): Result<User> = withContext(Dispatchers.IO) {
    val current = _currentUser.value ?: return@withContext failure("No active session.")
    val payload = JSONObject().put("full_name", name.trim()).put("email", email.trim())
      .put("phone", phone.trim()).put("bio", bio.trim()).put("company_name", company)
    runCatching {
      api.request("PATCH", "rest/v1/profiles?id=eq.${current.id}", payload.toString(), accessToken, "return=representation").use { response ->
        if (!response.isSuccessful) error(response.errorMessage())
        val rows = JSONArray(response.body?.string().orEmpty())
        if (rows.length() == 0) error("Profile was not returned by Supabase.")
        profileFromJson(rows.getJSONObject(0))
      }
    }.fold({ setAuthenticated(it) }, { failure(it.message ?: "Profile update failed.") })
  }

  override suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
    runCatching {
      api.request("GET", "rest/v1/profiles?id=eq.$userId&select=*", accessToken = accessToken).use { response ->
        if (!response.isSuccessful) return@runCatching null
        val rows = JSONArray(response.body?.string().orEmpty())
        if (rows.length() == 0) null else profileFromJson(rows.getJSONObject(0))
      }
    }.getOrNull()
  }

  override suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
    runCatching {
      api.request("GET", "rest/v1/profiles?select=*&order=created_at.desc&limit=100", accessToken = accessToken).use { response ->
        if (!response.isSuccessful) return@runCatching emptyList()
        val rows = JSONArray(response.body?.string().orEmpty())
        List(rows.length()) { profileFromJson(rows.getJSONObject(it)) }
      }
    }.getOrDefault(emptyList())
  }

  override suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      return@withContext Result.failure(IllegalArgumentException("Enter a valid email address."))
    }
    runCatching {
      val payload = JSONObject().put("email", cleanEmail)
      api.request("POST", "auth/v1/recover", payload.toString()).use { response ->
        if (!response.isSuccessful) error(response.errorMessage())
      }
    }.fold(
      { Result.success(Unit) },
      { Result.failure(IllegalArgumentException(it.message ?: "Could not send reset email.")) }
    )
  }

  override suspend fun signInWithGoogle(idToken: String, nonce: String): Result<User> = withContext(Dispatchers.IO) {
    _authState.value = AuthState.Authenticating
    val payload = JSONObject()
      .put("provider", "google")
      .put("token", idToken)
      .put("nonce", nonce)
    runCatching {
      api.request("POST", "auth/v1/verify", payload.toString()).use { response ->
        if (!response.isSuccessful) error(response.errorMessage())
        val json = JSONObject(response.body?.string().orEmpty())
        persistTokens(json)
        val authUser = json.getJSONObject("user")
        val metadata = authUser.optJSONObject("user_metadata")
        val fallbackName = metadata?.optString("full_name", "")
          .orEmpty().ifBlank { metadata?.optString("name").orEmpty() }
        loadProfile(authUser.getString("id"), fallbackName, authUser.optString("email"), "")
      }
    }.fold({ setAuthenticated(it) }, { failure(it.message ?: "Google sign-in failed.") })
  }

  override suspend fun updateUserRole(userId: String, role: UserRole): Result<User> = withContext(Dispatchers.IO) {
    runCatching {
      val payload = JSONObject().put("p_user_id", userId).put("p_role", role.name)
      api.request("POST", "rest/v1/rpc/assign_user_role", payload.toString(), accessToken).use { response ->
        if (!response.isSuccessful) error(response.errorMessage())
        val body = response.body?.string().orEmpty().trim()
        val json = if (body.startsWith("[")) JSONArray(body).getJSONObject(0) else JSONObject(body)
        profileFromJson(json)
      }
    }.fold(
      { updated ->
        if (_currentUser.value?.id == updated.id) setAuthenticated(updated) else Result.success(updated)
      },
      { Result.failure(it) }
    )
  }

  private fun requestUser(method: String, path: String, body: String, email: String): Result<User> = runCatching {
    api.request(method, path, body).use { response ->
      if (!response.isSuccessful) error(response.errorMessage())
      val json = JSONObject(response.body?.string().orEmpty())
      persistTokens(json)
      val authUser = json.getJSONObject("user")
      val metadata = authUser.optJSONObject("user_metadata")
      loadProfile(authUser.getString("id"), metadata?.optString("full_name").orEmpty(), email, "")
    }
  }.fold({ setAuthenticated(it) }, { failure(it.message ?: "Sign in failed.") })

  private fun tryRefresh(refreshToken: String): Result<User> = runCatching {
    val payload = JSONObject().put("refresh_token", refreshToken)
    api.request("POST", "auth/v1/token?grant_type=refresh_token", payload.toString()).use { response ->
      if (!response.isSuccessful) error("Saved session expired. Please sign in again.")
      val json = JSONObject(response.body?.string().orEmpty())
      persistTokens(json)
      val authUser = json.getJSONObject("user")
      loadProfile(authUser.getString("id"), "", authUser.optString("email"), "")
    }
  }.fold(
    { setAuthenticated(it) },
    {
      session.accessToken = null
      session.refreshToken = null
      sessionStore?.clear()
      Result.failure(it)
    }
  )

  private fun persistTokens(json: JSONObject) {
    session.accessToken = json.optString("access_token").takeIf { it.isNotBlank() }
    session.refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() } ?: session.refreshToken
    sessionStore?.save(session.accessToken, session.refreshToken ?: sessionStore?.refreshToken())
  }

  private fun loadProfile(id: String, fallbackName: String, email: String, phone: String): User {
    api.request("GET", "rest/v1/profiles?id=eq.$id&select=*", accessToken = accessToken).use { response ->
      if (!response.isSuccessful) error(response.errorMessage())
      val rows = JSONArray(response.body?.string().orEmpty())
      return if (rows.length() == 0) User(id, fallbackName.ifBlank { email.substringBefore('@') }, email, phone, UserRole.CLIENT)
      else profileFromJson(rows.getJSONObject(0))
    }
  }

  private fun profileFromJson(json: JSONObject) = User(
    id = json.getString("id"), name = json.optString("full_name", "FameBook User"),
    email = json.optString("email"), phone = json.optString("phone"),
    role = runCatching { UserRole.valueOf(json.optString("role", "CLIENT")) }.getOrDefault(UserRole.CLIENT),
    avatarUrl = json.optString("avatar_url").takeIf { it.isNotBlank() },
    companyName = json.optString("company_name").takeIf { it.isNotBlank() }, bio = json.optString("bio")
  )

  private fun setAuthenticated(user: User): Result<User> {
    session.currentUser = user
    _currentUser.value = user
    _authState.value = AuthState.Authenticated(user)
    return Result.success(user)
  }

  private fun failure(message: String): Result<Nothing> {
    _authState.value = AuthState.AuthenticationError(message)
    return Result.failure(IllegalArgumentException(message))
  }

  private fun okhttp3.Response.errorMessage(): String {
    val bodyText = body?.string().orEmpty()
    return runCatching {
      val json = JSONObject(bodyText)
      json.optString("msg").ifBlank { json.optString("message") }
    }.getOrDefault(bodyText).ifBlank { "Supabase request failed ($code)." }
  }
}
