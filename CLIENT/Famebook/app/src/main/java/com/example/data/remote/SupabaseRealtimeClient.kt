package com.example.data.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Push hub: realtime database changes land here within a second, and visible
 * screens refresh immediately instead of waiting for their poll timer.
 */
object RealtimeHub {
  val bookingsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
  val messagesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
  val notificationsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
}

/**
 * Minimal Supabase Realtime client (Phoenix protocol) over OkHttp websockets.
 * Subscribes to postgres_changes on bookings/messages/notifications with the
 * user's access token, so row-level security still applies server-side.
 * Heartbeats, reconnects with backoff, and rejoins before token expiry.
 * Polling elsewhere stays as a fallback for dropped sockets.
 */
class SupabaseRealtimeClient(
  private val supabaseUrl: String,
  private val publishableKey: String,
  private val tokenProvider: () -> String?,
  private val httpClient: OkHttpClient = OkHttpClient()
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var socket: WebSocket? = null
  private var running = false
  private var refCounter = 0

  companion object {
    private const val HEARTBEAT_MS = 25_000L
    private const val REJOIN_MS = 45 * 60_000L
    private val TABLES = listOf("bookings", "messages", "notifications")
  }

  fun start() {
    if (running) return
    running = true
    scope.launch { connectionLoop() }
  }

  fun stop() {
    running = false
    runCatching { socket?.close(1000, "stop") }
    socket = null
  }

  private suspend fun connectionLoop() {
    var backoff = 2_000L
    while (running) {
      val clean = runCatching { connectAndRun() }.getOrDefault(false)
      backoff = if (clean) 2_000L else (backoff * 2).coerceAtMost(30_000L)
      if (running) delay(backoff)
    }
  }

  private suspend fun connectAndRun(): Boolean {
    val token = tokenProvider() ?: return false
    val done = CompletableDeferred<Boolean>()
    var ref = 0
    fun nextRef(): String = (++ref).toString()

    val wsUrl = supabaseUrl.trimEnd('/').replaceFirst("https://", "wss://")
      .replaceFirst("http://", "ws://") +
      "/realtime/v1/websocket?apikey=$publishableKey&vsn=1.0.0"

    var heartbeatJob: Job? = null
    var rejoinJob: Job? = null

    val listener = object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        TABLES.forEach { table ->
          val joinRef = nextRef()
          val payload = JSONObject()
            .put(
              "config", JSONObject()
                .put("broadcast", JSONObject().put("ack", false))
                .put(
                  "postgres_changes", JSONArray().put(
                    JSONObject().put("event", "*").put("schema", "public").put("table", table)
                  )
                )
            )
            .put("access_token", token)
          webSocket.send(JSONArray().put(joinRef).put(nextRef()).put("realtime:$table").put("phx_join").put(payload).toString())
        }
        heartbeatJob = scope.launch {
          while (running) {
            delay(HEARTBEAT_MS)
            runCatching {
              socket?.send(JSONArray().put(JSONObject.NULL).put(nextRef()).put("phoenix").put("heartbeat").put(JSONObject()).toString())
            }
          }
        }
        rejoinJob = scope.launch {
          delay(REJOIN_MS)
          // Reconnect before the access token expires.
          runCatching { socket?.close(1000, "rejoin") }
          if (!done.isCompleted) done.complete(false)
        }
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        runCatching {
          val msg = JSONArray(text)
          when (msg.optString(3)) {
            "postgres_changes" -> {
              val table = msg.optJSONObject(4)?.optJSONObject("data")?.optString("table")
              when (table) {
                "bookings" -> RealtimeHub.bookingsChanged.tryEmit(Unit)
                "messages" -> RealtimeHub.messagesChanged.tryEmit(Unit)
                "notifications" -> RealtimeHub.notificationsChanged.tryEmit(Unit)
              }
            }
            "phx_reply" -> {
              val status = msg.optJSONObject(4)?.optString("status")
              if (status != null && status != "ok") {
                runCatching { webSocket.close(1000, "auth") }
                if (!done.isCompleted) done.complete(false)
              }
            }
            "phx_error", "phx_close" -> {
              runCatching { webSocket.close(1000, "error") }
              if (!done.isCompleted) done.complete(false)
            }
          }
        }
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        heartbeatJob?.cancel()
        rejoinJob?.cancel()
        if (!done.isCompleted) done.complete(false)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        heartbeatJob?.cancel()
        rejoinJob?.cancel()
        if (!done.isCompleted) done.complete(code == 1000 && reason == "stop")
      }
    }

    val request = Request.Builder().url(wsUrl).header("apikey", publishableKey).build()
    socket = httpClient.newWebSocket(request, listener)
    val clean = done.await()
    heartbeatJob?.cancel()
    rejoinJob?.cancel()
    return clean
  }
}
