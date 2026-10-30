package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Persists Supabase auth tokens locally so sign-in survives app restarts. */
class SupabaseSessionStore(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("famebook_session", Context.MODE_PRIVATE)

  fun save(accessToken: String?, refreshToken: String?) {
    prefs.edit {
      if (accessToken.isNullOrBlank()) remove(KEY_ACCESS) else putString(KEY_ACCESS, accessToken)
      if (refreshToken.isNullOrBlank()) remove(KEY_REFRESH) else putString(KEY_REFRESH, refreshToken)
    }
  }

  fun hasSession(): Boolean = !accessToken().isNullOrBlank() || !refreshToken().isNullOrBlank()

  fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)

  fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)

  fun clear() {
    prefs.edit { clear() }
  }

  companion object {
    private const val KEY_ACCESS = "supabase_access_token"
    private const val KEY_REFRESH = "supabase_refresh_token"
  }
}
