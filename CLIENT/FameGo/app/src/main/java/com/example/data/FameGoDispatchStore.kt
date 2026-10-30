package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists crew dispatch state so [FameGoDispatchService] can restart after
 * the app is swiped away or the device reboots — even with no activity alive.
 * Cleared the moment crew goes off-duty or signs out. Also remembers which
 * requests already buzzed / were declined so a cold start never double-pings.
 */
class FameGoDispatchStore(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

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

  fun isNotified(bookingId: String): Boolean = notifiedIds().contains(bookingId)

  fun notifiedIds(): Set<String> =
    prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().toSet()

  fun markNotified(bookingId: String) {
    val next = notifiedIds().toMutableSet().apply { add(bookingId) }
    // Cap the set so prefs never grow without bound.
    prefs.edit { putStringSet(KEY_NOTIFIED, next.toList().takeLast(MAX_IDS).toSet()) }
  }

  fun unmarkNotified(bookingId: String) {
    val next = prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()
      .toMutableSet().apply { remove(bookingId) }
    prefs.edit { putStringSet(KEY_NOTIFIED, next) }
  }

  fun isDeclined(bookingId: String): Boolean =
    prefs.getStringSet(KEY_DECLINED, emptySet()).orEmpty().contains(bookingId)

  fun markDeclined(bookingId: String) {
    val next = prefs.getStringSet(KEY_DECLINED, emptySet()).orEmpty()
      .toMutableSet().apply { add(bookingId) }
    prefs.edit { putStringSet(KEY_DECLINED, next.toList().takeLast(MAX_IDS).toSet()) }
  }

  companion object {
    private const val PREFS = "famego_dispatch"
    private const val KEY_CREW_ID = "dispatch_crew_id"
    private const val KEY_CREW_NAME = "dispatch_crew_name"
    private const val KEY_NOTIFIED = "dispatch_notified_ids"
    private const val KEY_DECLINED = "dispatch_declined_ids"
    private const val MAX_IDS = 100
  }
}
