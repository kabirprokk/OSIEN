package com.example.data.repository

import com.example.BuildConfig
import com.example.data.remote.SupabaseHttpClient
import com.example.data.remote.SupabaseSession
import com.example.domain.model.AppNotification
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.CrewProfile
import com.example.domain.model.CrewRequirement
import com.example.domain.model.CrewRole
import com.example.domain.model.LocationInfo
import com.example.domain.model.Message
import com.example.domain.model.RequestEvent
import com.example.domain.model.ShootType
import com.example.domain.model.UserRole
import com.example.domain.repository.BookingRepository
import com.example.domain.repository.CrewRepository
import com.example.domain.repository.FavoriteRepository
import com.example.domain.repository.MessageRepository
import com.example.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private fun defaultApi() = SupabaseHttpClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)

abstract class SupabaseRepository(protected val api: SupabaseHttpClient, protected val session: SupabaseSession) {
  protected fun token() = session.accessToken

  protected fun JSONObject.string(name: String, fallback: String = "") = optString(name, fallback).takeUnless { it == "null" } ?: fallback
  protected fun JSONObject.nullableString(name: String) = if (isNull(name)) null else string(name).takeIf { it.isNotBlank() }
  protected fun JSONObject.rows(name: String) = optJSONArray(name) ?: JSONArray()
  protected fun JSONObject.errorMessage(code: Int): String {
    val text = optString("message").ifBlank { optString("msg") }
    return text.ifBlank { "Supabase request failed ($code)." }
  }
  protected fun responseError(response: okhttp3.Response): String {
    val body = response.body?.string().orEmpty()
    return runCatching { JSONObject(body).errorMessage(response.code) }.getOrDefault(body).ifBlank { "Supabase request failed (${response.code})." }
  }
  protected fun timestamp(value: String): Long = runCatching {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US).parse(value)?.time
  }.getOrNull() ?: System.currentTimeMillis()
}

class SupabaseCrewRepository(
  api: SupabaseHttpClient = defaultApi(),
  session: SupabaseSession = SupabaseSession()
) : SupabaseRepository(api, session), CrewRepository {
  private val _profiles = MutableStateFlow<List<CrewProfile>>(emptyList())
  override val crewProfiles: StateFlow<List<CrewProfile>> = _profiles.asStateFlow()

  override fun getCrewProfile(crewId: String): Flow<CrewProfile?> =
    _profiles.map { list -> list.find { p -> p.id == crewId || p.userId == crewId } }
      .onStart { runCatching { refreshProfiles() } }

  override fun getCrewProfileByUserId(userId: String): Flow<CrewProfile?> =
    _profiles.map { list -> list.find { p -> p.userId == userId } }
      .onStart { runCatching { refreshProfiles() } }

  override suspend fun setAvailability(crewId: String, isAvailable: Boolean): Result<Unit> {
    return update(crewId, JSONObject().put("is_available", isAvailable))
  }

  override fun getAvailableCrew(role: CrewRole?): List<CrewProfile> = _profiles.value.filter {
    it.isAvailable && (role == null || it.primaryRole == role || role in it.secondaryRoles)
  }

  override suspend fun updateCrewProfile(profile: CrewProfile): Result<CrewProfile> = withContext(Dispatchers.IO) {
    runCatching {
      val body = JSONObject().put("primary_role", profile.primaryRole.name)
        .put("secondary_roles", JSONArray(profile.secondaryRoles.map { it.name }))
        .put("skills", JSONArray(profile.skills)).put("experience_years", profile.experienceYears)
        .put("gear_summary", profile.gearSummary).put("rating", profile.rating)
        .put("total_completed_shoots", profile.totalCompletedShoots).put("is_available", profile.isAvailable)
      api.request("PATCH", "rest/v1/crew_profiles?user_id=eq.${profile.userId}", body.toString(), token(), "return=representation").use { response ->
        if (!response.isSuccessful) error(responseError(response))
        val rows = JSONArray(response.body?.string().orEmpty())
        if (rows.length() == 0) error("Crew profile was not returned by Supabase.")
      }
      val refreshed = refreshProfiles()
      refreshed.find { it.userId == profile.userId } ?: error("Crew profile was not returned by Supabase.")
    }
  }

  private fun profiles(): Flow<List<CrewProfile>> = _profiles
    .onStart { runCatching { refreshProfiles() } }
    .flowOn(Dispatchers.IO)

  private suspend fun refreshProfiles(): List<CrewProfile> = withContext(Dispatchers.IO) {
    val result = runCatching {
      val crewRows = api.request(
        "GET",
        "rest/v1/crew_profiles?select=*&order=created_at.desc",
        accessToken = token()
      ).use { response ->
        if (!response.isSuccessful) error(responseError(response))
        JSONArray(response.body?.string().orEmpty())
      }
      if (crewRows.length() == 0) {
        _profiles.value = emptyList()
        return@withContext emptyList()
      }
      val ids = List(crewRows.length()) { crewRows.getJSONObject(it).optString("user_id") }
        .filter { it.isNotBlank() }
      val usersById = if (ids.isEmpty()) emptyMap() else {
        api.request(
          "GET",
          "rest/v1/profiles?id=in.(${ids.joinToString(",")})&select=*",
          accessToken = token()
        ).use { response ->
          if (!response.isSuccessful) emptyMap()
          else {
            val rows = JSONArray(response.body?.string().orEmpty())
            buildMap {
              for (i in 0 until rows.length()) {
                val user = rows.getJSONObject(i)
                put(user.optString("id"), user)
              }
            }
          }
        }
      }
      List(crewRows.length()) { i ->
        val crew = crewRows.getJSONObject(i)
        crewFromJson(crew, usersById[crew.optString("user_id")] ?: JSONObject())
      }
    }.getOrElse { emptyList() }
    _profiles.value = result
    result
  }

  private suspend fun update(id: String, body: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
    val result = runCatching {
      val patchedRows = api.request(
        "PATCH",
        "rest/v1/crew_profiles?user_id=eq.$id",
        body.toString(),
        token(),
        "return=representation"
      ).use { response ->
        if (!response.isSuccessful) error(responseError(response))
        JSONArray(response.body?.string().orEmpty()).length()
      }
      if (patchedRows == 0) {
        // Profile missing (e.g. crew promoted before auto-creation): create it.
        val insertBody = JSONObject()
          .put("user_id", id)
          .put("primary_role", CrewRole.PHOTOGRAPHER.name)
          .put("is_available", body.optBoolean("is_available", false))
        api.request("POST", "rest/v1/crew_profiles", insertBody.toString(), token(), "return=representation").use { response ->
          if (!response.isSuccessful) error(responseError(response))
        }
      }
    }
    runCatching { refreshProfiles() }
    if (result.isSuccess) {
      Result.success(Unit)
    } else {
      val error = result.exceptionOrNull() ?: IllegalStateException("Availability update failed")
      android.util.Log.e("SupabaseCrew", "setAvailability failed for $id", error)
      Result.failure(error)
    }
  }

  private fun crewFromJson(json: JSONObject, user: JSONObject = JSONObject()): CrewProfile {
    val embedded = json.optJSONObject("profile")
    val profileUser = embedded ?: user
    return CrewProfile(json.string("user_id"), json.string("user_id"), profileUser.string("full_name", "FameBook Crew"),
      enumValue(json.string("primary_role"), CrewRole.PHOTOGRAPHER), json.arrayStrings("secondary_roles").mapNotNull { enumValueOrNull<CrewRole>(it) },
      json.arrayStrings("skills"), json.optInt("experience_years"), json.string("gear_summary"), json.optDouble("rating"),
      json.optInt("total_completed_shoots"), json.optBoolean("is_available"), profileUser.string("phone"), profileUser.string("email"), profileUser.nullableString("avatar_url"))
  }
}

class SupabaseBookingRepository(
  api: SupabaseHttpClient = defaultApi(), session: SupabaseSession = SupabaseSession()
) : SupabaseRepository(api, session), BookingRepository {
  private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
  override val allBookings: StateFlow<List<Booking>> = _bookings.asStateFlow()
  override fun getBooking(bookingId: String): Flow<Booking?> = bookings().map { it.find { b -> b.id == bookingId } }
  override fun getClientBookings(clientId: String): Flow<List<Booking>> = bookings().map { it.filter { b -> b.clientId == clientId } }
  override fun getCrewBookings(crewId: String): Flow<List<Booking>> = bookings().map { it.filter { b -> b.assignedCrewId == crewId } }
  override fun getActiveRequestForClient(clientId: String): Flow<Booking?> = getClientBookings(clientId).map { it.find { b -> b.status == BookingStatus.SEARCHING_CREW || b.status == BookingStatus.OFFERED } }
  override fun getIncomingRequestsForCrew(crewId: String): Flow<List<Booking>> = flow {
    // RPC returns privacy-safe rows without requirements; enrich them in one
    // batch so request cards show the required crew role.
    val result = runCatching {
      val rows = api.request("POST", "rest/v1/rpc/get_available_booking_requests", "{}", token()).use { response ->
        if (!response.isSuccessful) error(responseError(response))
        JSONArray(response.body?.string().orEmpty())
      }
      val ids = List(rows.length()) { rows.getJSONObject(it).optString("id") }.filter { it.isNotBlank() }
      val reqsByBooking = if (ids.isEmpty()) emptyMap() else {
        api.request(
          "GET",
          "rest/v1/booking_requirements?booking_id=in.(${ids.joinToString(",")})&select=*",
          accessToken = token()
        ).use { response ->
          if (!response.isSuccessful) emptyMap()
          else {
            val reqs = JSONArray(response.body?.string().orEmpty())
            buildMap<String, MutableList<JSONObject>> {
              for (i in 0 until reqs.length()) {
                val r = reqs.getJSONObject(i)
                getOrPut(r.optString("booking_id")) { mutableListOf() }.add(r)
              }
            }
          }
        }
      }
      List(rows.length()) { i ->
        val row = rows.getJSONObject(i)
        row.put("booking_requirements", JSONArray(reqsByBooking[row.optString("id")].orEmpty()))
        bookingFromJson(row)
      }
    }.getOrElse { emptyList() }
    emit(result)
  }.flowOn(Dispatchers.IO)

  override suspend fun createBooking(booking: Booking): Result<Booking> = withContext(Dispatchers.IO) {
    runCatching {
    val body = JSONObject().put("client_id", booking.clientId).put("shoot_type", booking.shootType.name).put("title", booking.title)
      .put("description", booking.description).put("shoot_date", databaseDate(booking.date)).put("start_time", databaseTime(booking.startTime))
      .put("duration_hours", booking.durationHours).put("location_name", booking.location.name).put("location_address", booking.location.address)
      .put("location_city", booking.location.city).put("location_notes", booking.location.notes).put("special_instructions", booking.specialInstructions)
      .put("status", booking.status.name)
    val created = api.request("POST", "rest/v1/bookings", body.toString(), token(), "return=representation").use { response ->
      if (!response.isSuccessful) error(responseError(response)); JSONArray(response.body?.string().orEmpty()).getJSONObject(0)
    }
    val id = created.getString("id")
    if (booking.requirements.isNotEmpty()) {
      val requirements = JSONArray(booking.requirements.map { JSONObject().put("booking_id", id).put("crew_role", it.role.name).put("quantity", it.count).put("equipment_notes", it.equipmentNotes) })
      api.request("POST", "rest/v1/booking_requirements", requirements.toString(), token()).use { if (!it.isSuccessful) error(responseError(it)) }
    }
    val result = loadBooking(id)
    notifyCrewOfNewBooking(result)
    result
    }
  }
  override suspend fun cancelBooking(bookingId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val booking = runCatching { loadBooking(bookingId) }.getOrNull()
    updateStatus(bookingId, BookingStatus.CANCELLED).onSuccess {
      booking?.assignedCrewId?.let { crewId ->
        notifyUser(crewId, bookingId, "Booking Cancelled", "Shoot '${booking.title}' was cancelled by the client.")
      }
    }
  }
  override suspend fun acceptBooking(bookingId: String, crewId: String): Result<Booking> = withContext(Dispatchers.IO) {
    runCatching {
      val confirmed = api.request("POST", "rest/v1/rpc/accept_booking", JSONObject().put("p_booking_id", bookingId).toString(), token()).use { response ->
        if (!response.isSuccessful) error(responseError(response)); bookingFromJson(response.body?.string()?.let(::JSONObject) ?: error("Empty booking response"))
      }
      notifyUser(confirmed.clientId, bookingId, "Crew Confirmed!", "Your shoot '${confirmed.title}' has been accepted.")
      confirmed
    }
  }
  override suspend fun declineBooking(bookingId: String, crewId: String): Result<Unit> = Result.success(Unit)
  override suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Result<Unit> = updateStatus(bookingId, status)

  private suspend fun updateStatus(id: String, status: BookingStatus): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      api.request("PATCH", "rest/v1/bookings?id=eq.$id", JSONObject().put("status", status.name).toString(), token()).use { if (!it.isSuccessful) error(responseError(it)) }
    }
  }
  private fun bookings(): Flow<List<Booking>> = flow {
    // Rich join first; plain + batched fallback if the embed ever fails,
    // so bookings can never silently vanish because of a join name.
    val result = runCatching { fetchBookingsEmbedded() }
      .getOrElse { fetchBookingsPlain() }
    _bookings.value = result; emit(result)
  }.flowOn(Dispatchers.IO)

  private fun fetchBookingsEmbedded(): List<Booking> {
    api.request("GET", "rest/v1/bookings?select=*,client:profiles!bookings_client_id_fkey(*),crew:profiles!bookings_assigned_crew_id_fkey(*),booking_requirements(*)&order=created_at.desc", accessToken = token()).use { response ->
      if (!response.isSuccessful) error(responseError(response))
      val rows = JSONArray(response.body?.string().orEmpty())
      return List(rows.length()) { bookingFromJson(rows.getJSONObject(it)) }
    }
  }

  private fun fetchBookingsPlain(): List<Booking> {
    return runCatching {
      val rows = api.request(
        "GET",
        "rest/v1/bookings?select=*&order=created_at.desc",
        accessToken = token()
      ).use { response ->
        if (!response.isSuccessful) error(responseError(response))
        JSONArray(response.body?.string().orEmpty())
      }
      if (rows.length() == 0) return emptyList()
      val bookingIds = List(rows.length()) { rows.getJSONObject(it).optString("id") }.filter { it.isNotBlank() }
      val userIds = List(rows.length()) {
        val row = rows.getJSONObject(it)
        listOf(row.optString("client_id"), row.optString("assigned_crew_id"))
      }.flatten().filter { it.isNotBlank() }.distinct()
      val usersById = if (userIds.isEmpty()) emptyMap() else {
        api.request(
          "GET",
          "rest/v1/profiles?id=in.(${userIds.joinToString(",")})&select=*",
          accessToken = token()
        ).use { response ->
          if (!response.isSuccessful) emptyMap()
          else {
            val profiles = JSONArray(response.body?.string().orEmpty())
            buildMap {
              for (i in 0 until profiles.length()) {
                val p = profiles.getJSONObject(i)
                put(p.optString("id"), p)
              }
            }
          }
        }
      }
      val reqsByBooking = if (bookingIds.isEmpty()) emptyMap() else {
        api.request(
          "GET",
          "rest/v1/booking_requirements?booking_id=in.(${bookingIds.joinToString(",")})&select=*",
          accessToken = token()
        ).use { response ->
          if (!response.isSuccessful) emptyMap()
          else {
            val reqs = JSONArray(response.body?.string().orEmpty())
            buildMap<String, MutableList<JSONObject>> {
              for (i in 0 until reqs.length()) {
                val r = reqs.getJSONObject(i)
                getOrPut(r.optString("booking_id")) { mutableListOf() }.add(r)
              }
            }
          }
        }
      }
      List(rows.length()) { i ->
        val row = rows.getJSONObject(i)
        row.put("client", usersById[row.optString("client_id")] ?: JSONObject())
        row.optString("assigned_crew_id").takeIf { it.isNotBlank() }?.let { crewId ->
          usersById[crewId]?.let { row.put("crew", it) }
        }
        row.put("booking_requirements", JSONArray(reqsByBooking[row.optString("id")].orEmpty()))
        bookingFromJson(row)
      }
    }.getOrElse { emptyList() }
  }
  private suspend fun loadBooking(id: String): Booking = withContext(Dispatchers.IO) {
    var value: Booking? = null; bookings().collect { value = it.find { b -> b.id == id } }; value ?: error("Booking was not returned by Supabase.")
  }

  // Cross-device fan-out: push rows into notifications so the other party's
  // device shows them. Best-effort; booking state never depends on this.
  private fun notifyUser(recipientId: String, bookingId: String?, title: String, body: String) {
    if (recipientId.isBlank()) return
    runCatching {
      val payload = JSONObject()
        .put("recipient_id", recipientId)
        .put("booking_id", bookingId)
        .put("title", title)
        .put("body", body)
      api.request("POST", "rest/v1/notifications", payload.toString(), token()).use { }
    }
  }

  private fun notifyCrewOfNewBooking(booking: Booking) {
    runCatching {
      val crewIds = api.request(
        "GET",
        "rest/v1/profiles?select=id&role=eq.CREW",
        accessToken = token()
      ).use { response ->
        if (!response.isSuccessful) return
        val rows = JSONArray(response.body?.string().orEmpty())
        List(rows.length()) { rows.getJSONObject(it).optString("id") }.filter { it.isNotBlank() }
      }
      if (crewIds.isEmpty()) return
      val rows = JSONArray(crewIds.map { id ->
        JSONObject()
          .put("recipient_id", id)
          .put("booking_id", booking.id)
          .put("title", "New Shoot Request!")
          .put("body", "${booking.title} in ${booking.location.city}")
      })
      api.request("POST", "rest/v1/notifications", rows.toString(), token()).use { }
    }
  }

  private fun bookingFromJson(j: JSONObject): Booking {
    val client = j.optJSONObject("client") ?: JSONObject(); val crew = j.optJSONObject("crew")
    return Booking(j.string("id"), j.string("client_id"), client.string("full_name"), client.string("phone"), client.string("email"), client.nullableString("company_name"),
      enumValue(j.string("shoot_type"), ShootType.OTHER), j.string("title"), j.string("description"), j.string("shoot_date"), j.string("start_time"), j.optInt("duration_hours"),
      LocationInfo(j.string("location_name"), j.string("location_address"), j.string("location_city", "Mumbai"), j.string("location_notes")),
      j.rows("booking_requirements").let { a -> List(a.length()) { val r = a.getJSONObject(it); CrewRequirement(enumValue(r.string("crew_role"), CrewRole.ASSISTANT), r.optInt("quantity", 1), r.string("equipment_notes")) } },
      j.string("special_instructions"), status = enumValue(j.string("status"), BookingStatus.SEARCHING_CREW), assignedCrewId = j.nullableString("assigned_crew_id"), assignedCrewName = crew?.nullableString("full_name"), assignedCrewPhone = crew?.nullableString("phone"), assignedCrewEmail = crew?.nullableString("email"), createdAtTimestamp = timestamp(j.string("created_at")))
  }
}

class SupabaseMessageRepository(api: SupabaseHttpClient = defaultApi(), session: SupabaseSession = SupabaseSession()) : SupabaseRepository(api, session), MessageRepository {
  override fun getMessages(bookingId: String): Flow<List<Message>> = flow {
    // Two-step fetch (no fragile FK-embed): messages first, then sender
    // profiles in one batch so names/roles always resolve.
    val result = runCatching {
      val rows = api.request(
        "GET",
        "rest/v1/messages?booking_id=eq.$bookingId&select=*&order=created_at.asc",
        accessToken = token()
      ).use { r ->
        if (!r.isSuccessful) error(responseError(r))
        JSONArray(r.body?.string().orEmpty())
      }
      val senderIds = List(rows.length()) { rows.getJSONObject(it).optString("sender_id") }
        .filter { it.isNotBlank() }.distinct()
      val senders = if (senderIds.isEmpty()) emptyMap() else {
        api.request(
          "GET",
          "rest/v1/profiles?id=in.(${senderIds.joinToString(",")})&select=id,full_name,role",
          accessToken = token()
        ).use { r ->
          if (!r.isSuccessful) emptyMap()
          else {
            val profiles = JSONArray(r.body?.string().orEmpty())
            buildMap {
              for (i in 0 until profiles.length()) {
                val p = profiles.getJSONObject(i)
                put(p.optString("id"), p)
              }
            }
          }
        }
      }
      List(rows.length()) { i ->
        val j = rows.getJSONObject(i)
        val s = senders[j.optString("sender_id")] ?: JSONObject()
        Message(
          j.string("id"), bookingId, j.string("sender_id"),
          s.optString("full_name").ifBlank { "FameBook User" },
          enumValue(s.optString("role", "CLIENT"), UserRole.CLIENT),
          j.string("body"), timestamp(j.string("created_at"))
        )
      }
    }.getOrElse { emptyList() }
    emit(result)
  }.flowOn(Dispatchers.IO)
  override suspend fun sendMessage(bookingId: String, senderId: String, senderName: String, senderRole: UserRole, text: String): Result<Message> = withContext(Dispatchers.IO) {
    runCatching {
      require(text.isNotBlank()) { "Message cannot be empty." }; api.request("POST", "rest/v1/messages", JSONObject().put("booking_id", bookingId).put("sender_id", senderId).put("body", text.trim()).toString(), token(), "return=representation").use { r -> if (!r.isSuccessful) error(responseError(r)); val j=JSONArray(r.body?.string().orEmpty()).getJSONObject(0); Message(j.string("id"), bookingId, senderId, senderName, senderRole, j.string("body"), timestamp(j.string("created_at"))) }
    }
  }
}

class SupabaseNotificationRepository(api: SupabaseHttpClient = defaultApi(), session: SupabaseSession = SupabaseSession()) : SupabaseRepository(api, session), NotificationRepository {
  private val _events = MutableSharedFlow<RequestEvent>(replay = 5, extraBufferCapacity = 20)
  override val requestEvents: SharedFlow<RequestEvent> = _events.asSharedFlow()
  override fun getNotifications(userId: String): Flow<List<AppNotification>> = flow {
    val result = runCatching { api.request("GET", "rest/v1/notifications?recipient_id=eq.$userId&select=*&order=created_at.desc", accessToken=token()).use { r -> if (!r.isSuccessful) error(responseError(r)); JSONArray(r.body?.string().orEmpty()).let { a -> List(a.length()) { val j=a.getJSONObject(it); AppNotification(j.string("id"), userId, j.string("title"), j.string("body"), j.nullableString("booking_id"), timestamp(j.string("created_at")), j.optBoolean("is_read")) } } } }.getOrElse { emptyList() }; emit(result)
  }.flowOn(Dispatchers.IO)
  override suspend fun sendNotification(notification: AppNotification) = withContext(Dispatchers.IO) { api.request("POST", "rest/v1/notifications", JSONObject().put("recipient_id", notification.recipientUserId).put("booking_id", notification.bookingId).put("title", notification.title).put("body", notification.message).put("is_read", notification.isRead).toString(), token()).use { if (!it.isSuccessful) error(responseError(it)) } }
  override suspend fun markAsRead(notificationId: String) = withContext(Dispatchers.IO) { api.request("PATCH", "rest/v1/notifications?id=eq.$notificationId", JSONObject().put("is_read", true).toString(), token()).use { if (!it.isSuccessful) error(responseError(it)) } }
  override suspend fun emitRequestEvent(event: RequestEvent) { _events.emit(event) }
}

class SupabaseFavoriteRepository(
  api: SupabaseHttpClient = defaultApi(),
  session: SupabaseSession = SupabaseSession()
) : SupabaseRepository(api, session), FavoriteRepository {
  override fun getFavoriteCrewIds(clientId: String): Flow<List<String>> = flow {
    val result = runCatching {
      api.request(
        "GET",
        "rest/v1/favorite_crew?client_id=eq.$clientId&select=crew_user_id&order=created_at.desc",
        accessToken = token()
      ).use { response ->
        if (!response.isSuccessful) error(responseError(response))
        val rows = JSONArray(response.body?.string().orEmpty())
        List(rows.length()) { rows.getJSONObject(it).optString("crew_user_id") }.filter { it.isNotBlank() }
      }
    }.getOrElse { emptyList() }
    emit(result)
  }.flowOn(Dispatchers.IO)

  override suspend fun isFavorite(clientId: String, crewUserId: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
      api.request(
        "GET",
        "rest/v1/favorite_crew?client_id=eq.$clientId&crew_user_id=eq.$crewUserId&select=crew_user_id",
        accessToken = token()
      ).use { response ->
        if (!response.isSuccessful) return@runCatching false
        JSONArray(response.body?.string().orEmpty()).length() > 0
      }
    }.getOrDefault(false)
  }

  override suspend fun toggleFavorite(clientId: String, crewUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
    runCatching {
      if (isFavorite(clientId, crewUserId)) {
        api.request(
          "DELETE",
          "rest/v1/favorite_crew?client_id=eq.$clientId&crew_user_id=eq.$crewUserId",
          accessToken = token()
        ).use { response ->
          if (!response.isSuccessful) error(responseError(response))
        }
        false
      } else {
        val payload = JSONObject().put("client_id", clientId).put("crew_user_id", crewUserId)
        api.request("POST", "rest/v1/favorite_crew", payload.toString(), token()).use { response ->
          if (!response.isSuccessful) error(responseError(response))
        }
        true
      }
    }
  }
}

private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T = runCatching { enumValueOf<T>(value.uppercase().replace(' ', '_')) }.getOrDefault(fallback)
private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? = runCatching { enumValueOf<T>(value.uppercase().replace(' ', '_')) }.getOrNull()
private fun JSONObject.arrayStrings(name: String): List<String> = (optJSONArray(name) ?: JSONArray()).let { a -> List(a.length()) { a.optString(it) } }

private fun databaseDate(value: String): String {
  val today = Calendar.getInstance()
  when {
    value.startsWith("Tomorrow", ignoreCase = true) -> today.add(Calendar.DAY_OF_YEAR, 1)
    value.startsWith("Next Monday", ignoreCase = true) -> {
      val days = (Calendar.MONDAY - today.get(Calendar.DAY_OF_WEEK) + 7) % 7
      today.add(Calendar.DAY_OF_YEAR, if (days == 0) 7 else days)
    }
    value.startsWith("This Weekend", ignoreCase = true) -> {
      val days = (Calendar.SATURDAY - today.get(Calendar.DAY_OF_WEEK) + 7) % 7
      today.add(Calendar.DAY_OF_YEAR, days)
    }
  }
  return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(today.time)
}

private fun databaseTime(value: String): String {
  val display = value.substringAfterLast(",").trim()
  val parsed = runCatching { SimpleDateFormat("hh:mm a", Locale.US).parse(display) }.getOrNull()
  return if (parsed == null) "10:00:00" else SimpleDateFormat("HH:mm:ss", Locale.US).format(parsed)
}
