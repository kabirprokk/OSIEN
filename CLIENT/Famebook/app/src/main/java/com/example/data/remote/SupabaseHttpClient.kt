package com.example.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class SupabaseHttpClient(
  private val baseUrl: String,
  private val publishableKey: String,
  private val httpClient: OkHttpClient = OkHttpClient()
) {
  private val jsonMediaType = "application/json".toMediaType()

  fun request(
    method: String,
    path: String,
    body: String? = null,
    accessToken: String? = null,
    prefer: String? = null
  ): Response {
    val requestBuilder = Request.Builder()
      .url(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))
      .header("apikey", publishableKey)
      .header("Accept", "application/json")

    accessToken?.let { requestBuilder.header("Authorization", "Bearer $it") }
    prefer?.let { requestBuilder.header("Prefer", it) }

    val requestBody = body?.toRequestBody(jsonMediaType)
    requestBuilder.method(method, requestBody)
    return httpClient.newCall(requestBuilder.build()).execute()
  }
}
