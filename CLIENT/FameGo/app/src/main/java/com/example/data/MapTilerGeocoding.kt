package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class MapPlace(
  /** Full display address (fills the address field). */
  val full: String,
  /** Short name (fills venue when empty). */
  val short: String,
  val latitude: Double? = null,
  val longitude: Double? = null
)
/** Map geocoding: MapTiler when the key works, free OSM Nominatim otherwise. */
object MapTilerGeocoding {
  const val MUMBAI_LNG = 72.8777
  const val MUMBAI_LAT = 19.0760
  private const val UA = "FameGo-Android/1.0"

  /** Short-timeout client for interactive map work: fail fast, never stall. */
  private val fastHttp: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
      .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
      .writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }

  suspend fun suggest(query: String, limit: Int = 6): Result<List<MapPlace>> =
    withContext(Dispatchers.IO) {
      if (query.trim().length < 3) return@withContext Result.success(emptyList())
      // 1. Free OpenStreetMap Nominatim first (no key, always available).
      val osm = runCatching { queryNominatim(query.trim(), limit) }.getOrNull()
      if (!osm.isNullOrEmpty()) return@withContext Result.success(osm)
      // 2. MapTiler backup when a valid key is configured.
      val key = BuildConfig.MAPTILER_KEY
      if (key.isNotBlank()) {
        runCatching { queryMapTiler(query.trim(), key, limit) }
          .onSuccess { if (it.isNotEmpty()) return@withContext Result.success(it) }
      }
      runCatching { queryNominatim(query.trim(), limit) }
    }

  /** Free reverse-geocode (BigDataCloud, no key) for map taps. */
  suspend fun reverse(latitude: Double, longitude: Double): Result<MapPlace> =
    withContext(Dispatchers.IO) {
      runCatching {
        val url = "https://api.bigdatacloud.net/data/reverse-geocode-client" +
          "?latitude=$latitude&longitude=$longitude&localityLanguage=en"
        fastHttp.newCall(Request.Builder().url(url).get().build())
          .execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("reverse ${response.code}")
            val o = JSONObject(raw)
            val locality = o.optString("locality").ifBlank { o.optString("city") }
            val city = o.optString("city").ifBlank { locality }
            val parts = listOf(
              locality.ifBlank { city },
              city, o.optString("principalSubdivision"),
              o.optString("postcode"), o.optString("countryName")
            ).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val full = parts.joinToString(", ").ifBlank { "$latitude, $longitude" }
            MapPlace(
              full = full,
              short = locality.ifBlank { city }.ifBlank { "Selected spot" },
              latitude = latitude,
              longitude = longitude
            )
          }
      }
    }

  private fun queryMapTiler(query: String, key: String, limit: Int): List<MapPlace> {
    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
    val url = "https://api.maptiler.com/geocoding/$encoded.json" +
      "?key=$key&country=in&limit=$limit" +
      "&types=address,place,locality,neighborhood,poi" +
      "&language=en&proximity=72.8777,19.0760"
    fastHttp.newCall(Request.Builder().url(url).get().build())
      .execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) error("MapTiler ${response.code}")
        return parseMapTilerFeatures(JSONObject(raw).optJSONArray("features") ?: JSONArray())
      }
  }

  private fun parseMapTilerFeatures(features: JSONArray): List<MapPlace> = buildList {
    for (i in 0 until features.length()) {
      val f = features.optJSONObject(i) ?: continue
      val full = f.optString("place_name").ifBlank { f.optString("text") }
      if (full.isBlank()) continue
      val short = f.optString("text").ifBlank { full.substringBefore(",") }.trim()
      val center = f.optJSONArray("center")
      add(
        MapPlace(
          full = full,
          short = short,
          longitude = center?.optDouble(0)?.takeIf { !it.isNaN() },
          latitude = center?.optDouble(1)?.takeIf { !it.isNaN() }
        )
      )
    }
  }

  private fun queryNominatim(query: String, limit: Int): List<MapPlace> {
    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
    val url = "https://nominatim.openstreetmap.org/search" +
      "?format=jsonv2&q=$encoded&countrycodes=in&limit=$limit&addressdetails=1"
    fastHttp.newCall(
      Request.Builder().url(url).header("User-Agent", UA).get().build()
    ).execute().use { response ->
      val raw = response.body?.string().orEmpty()
      if (!response.isSuccessful) error("Nominatim ${response.code}")
      return buildList {
        val array = JSONArray(raw)
        for (i in 0 until array.length()) {
          val o = array.optJSONObject(i) ?: continue
          val full = o.optString("display_name")
          if (full.isBlank()) continue
          val addr = o.optJSONObject("address")
          val short = o.optString("name").ifBlank {
            (addr?.optString("suburb").orEmpty()).ifBlank {
              (addr?.optString("city").orEmpty()).ifBlank { full.substringBefore(",") }
            }
          }.trim()
          add(
            MapPlace(
              full = full,
              short = short,
              latitude = o.optString("lat").toDoubleOrNull(),
              longitude = o.optString("lon").toDoubleOrNull()
            )
          )
        }
      }
    }
  }
}
