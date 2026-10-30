package com.example.data.service

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists crew dispatch state so the foreground dispatch service can restart
 * itself after the app is swiped away or the device reboots — even with no
 * activity alive. Cleared the moment crew goes off-duty or signs out.
 */
class CrewDispatchStore(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("famebook_dispatch", Context.MODE_PRIVATE)

  fun setDispatch(crewUserId: String, crewName: String) {
    prefs.edit {
      putString(KEY_CREW_ID, crewUserId)
      putString(KEY_CREW_NAME, crewName)
    }
  }

  fun crewUserId(): String? = prefs.getString(KEY_CREW_ID, null)

  fun crewName(): String? = prefs.getString(KEY_CREW_NAME, null)

  fun isDispatchOn(): Boolean = !crewUserId().isNullOrBlank()

  fun clearDispatch() {
    prefs.edit { clear() }
  }

  companion object {
    private const val KEY_CREW_ID = "dispatch_crew_id"
    private const val KEY_CREW_NAME = "dispatch_crew_name"
  }
}
