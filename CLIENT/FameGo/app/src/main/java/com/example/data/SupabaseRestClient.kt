package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object SupabaseRestClient {
  private val client = SupabaseNetwork.http
  private val json = "application/json".toMediaType()

  suspend fun get(path: String): Result<String> = call("GET", path)
  suspend fun post(path: String, body: String): Result<String> = call("POST", path, body)
  suspend fun upsert(path: String, body: String): Result<String> = call("POST", path, body, upsert = true)
  suspend fun patch(path: String, body: String): Result<String> = call("PATCH", path, body)
  suspend fun delete(path: String): Result<String> = call("DELETE", path)

  private suspend fun call(method: String, path: String, body: String? = null, upsert: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured) return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
    val builder = Request.Builder().url("${SupabaseConfig.baseUrl}/rest/v1/$path")
      .header("apikey", SupabaseConfig.publishableKey)
      .header("Authorization", "Bearer ${SupabaseSession.accessToken ?: SupabaseConfig.publishableKey}")
      .header("Accept", "application/json")
    if (body != null) builder.header("Content-Type", "application/json")
    // Ask PostgREST to return the row so callers can reconcile IDs/codes.
    if (method == "POST" || method == "PATCH") {
      builder.header("Prefer", if (upsert) "resolution=merge-duplicates,return=representation" else "return=representation")
    }
    val request = when (method) {
      "POST" -> builder.post(body.orEmpty().toRequestBody(json)).build()
      "PATCH" -> builder.patch(body.orEmpty().toRequestBody(json)).build()
      "DELETE" -> builder.delete().build()
      else -> builder.get().build()
    }
    runCatching {
      client.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) error("Supabase ${response.code}: ${raw.take(240)}")
        raw
      }
    }
  }
}
