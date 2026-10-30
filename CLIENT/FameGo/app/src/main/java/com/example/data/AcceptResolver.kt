package com.example.data

import org.json.JSONArray

/**
 * Shared verdict for a failed shoot claim. A dead RPC alone says nothing —
 * the booking is re-read so the crew sees the truth: claimed by me
 * (confirmed), claimed by someone else, off duty, closed, or a transient
 * failure worth retrying. Used by the notification action, the full-screen
 * popup, and the in-app accept path so all three agree.
 */
sealed interface AcceptOutcome {
  data class Confirmed(val title: String, val details: String) : AcceptOutcome
  data class Taken(val name: String) : AcceptOutcome
  data object OffDuty : AcceptOutcome
  data object Gone : AcceptOutcome
  data object Retry : AcceptOutcome
}

object AcceptResolver {
  suspend fun resolve(bookingId: String): AcceptOutcome {
    val me = SupabaseSession.cachedProfile()?.id
    if (bookingId.isBlank() || me.isNullOrBlank()) return AcceptOutcome.Retry
    // My crew row (id for assignment match, availability for duty verdict).
    var myCrewId: String? = null
    var available: Boolean? = null
    SupabaseRestClient.get("crew_profiles?select=id,is_available&user_id=eq.$me&limit=1")
      .onSuccess { raw ->
        val row = runCatching { JSONArray(raw).optJSONObject(0) }.getOrNull()
        myCrewId = row?.optString("id")?.ifBlank { null }
        if (row?.has("is_available") == true) available = row.optBoolean("is_available")
      }
    // Live booking with its assignment snapshot.
    val raw = SupabaseRestClient.get(
      "bookings?select=id,shoot_title,venue_name,shoot_date,shoot_time,plan_price_paise,status,payment_status,booking_assignments(crew_id,name_snapshot)&id=eq.$bookingId&limit=1"
    ).getOrNull() ?: return AcceptOutcome.Retry
    val o = runCatching { JSONArray(raw).optJSONObject(0) }.getOrNull()
      ?: return AcceptOutcome.Gone
    val assignments = o.optJSONArray("booking_assignments")
    val first = if (assignments != null && assignments.length() > 0) assignments.optJSONObject(0) else null
    val assignedId = first?.optString("crew_id")?.ifBlank { null }
    val assignedName = first?.optString("name_snapshot").orEmpty().ifBlank { "another crew member" }
    if (assignedId != null) {
      return if (assignedId == myCrewId) {
        AcceptOutcome.Confirmed(prettyTitle(o), prettyDetails(o))
      } else {
        AcceptOutcome.Taken(assignedName)
      }
    }
    if (o.optString("status") != "SEARCHING_CREW" || o.optString("payment_status") != "PAID") {
      return AcceptOutcome.Gone
    }
    if (available == false) return AcceptOutcome.OffDuty
    // Still open and (as far as we know) on duty: the RPC failure was
    // transient or auth-related — retry, don't declare it taken.
    return AcceptOutcome.Retry
  }

  fun prettyTitle(o: org.json.JSONObject): String =
    o.optString("shoot_title").ifBlank { "Shoot" }

  fun prettyDetails(o: org.json.JSONObject): String {
    val venue = o.optString("venue_name").ifBlank { "Venue TBA" }
    val date = o.optString("shoot_date").ifBlank { "" }
    val time = o.optString("shoot_time").ifBlank { "" }
    val price = (o.optInt("plan_price_paise", 0) / 100).coerceAtLeast(0)
    return listOf(venue, listOf(date, time).filter { it.isNotBlank() }.joinToString(" • "))
      .filter { it.isNotBlank() }.joinToString(" • ") + " • ₹${"%,d".format(price)}"
  }
}
