package com.example.data

import android.os.Handler
import android.os.Looper
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Multiplexed channel adapter: ONE websocket carries every subscription
 * (chat, notifications, bookings) instead of one socket per channel.
 * Less radio wakeups, less battery, less data — and a single reconnect path.
 * The server remains the source of truth; a change only triggers a refresh.
 */
object SupabaseRealtimeClient {
  private val http = SupabaseNetwork.http
  private data class Channel(val topic: String, val changes: org.json.JSONArray, val onChanged: () -> Unit)
  private val channels = ConcurrentHashMap<String, Channel>()
  private val lock = Any()
  @Volatile private var socket: WebSocket? = null
  private val refCounter = AtomicInteger(1)
  private val mainHandler = Handler(Looper.getMainLooper())
  private val reconnectRunnable = Runnable {
    synchronized(lock) {
      if (socket == null && channels.isNotEmpty()) connectLocked()
    }
  }
  private val heartbeatRunnable = object : Runnable {
    override fun run() {
      val ws = synchronized(lock) { socket } ?: return
      // Supabase drops idle sockets (~60s). A heartbeat keeps the single
      // multiplexed socket alive without rejoining every channel.
      runCatching {
        ws.send(
          JSONObject().apply {
            put("event", "heartbeat")
            put("topic", "phoenix")
            put("ref", refCounter.getAndIncrement().toString())
            put("payload", JSONObject())
          }.toString()
        )
      }
      mainHandler.removeCallbacks(this)
      mainHandler.postDelayed(this, 25_000)
    }
  }

  /** Force a reconnect (network regain, fresh token). Safe to call anytime. */
  fun reconnectNow() {
    synchronized(lock) {
      runCatching { socket?.close(1000, "reconnect") }
      socket = null
      mainHandler.removeCallbacks(reconnectRunnable)
      mainHandler.removeCallbacks(heartbeatRunnable)
      if (channels.isNotEmpty()) connectLocked()
    }
  }

  private fun scheduleReconnect() {
    mainHandler.removeCallbacks(reconnectRunnable)
    mainHandler.postDelayed(reconnectRunnable, 3000)
  }

  fun subscribeToChat(bookingId: String, onChanged: () -> Unit) {
    if (!SupabaseConfig.isConfigured || SupabaseSession.accessToken.isNullOrBlank() || bookingId.isBlank()) return
    addChannel(
      key = "chat:$bookingId",
      topic = "realtime:booking_$bookingId",
      changes = org.json.JSONArray().put(JSONObject().apply {
        put("event", "*")
        put("schema", "public")
        put("table", "chat_messages")
        put("filter", "booking_id=eq.$bookingId")
      }),
      onChanged = onChanged
    )
  }

  /** Bell + lists stay live: notifications addressed to this user. */
  fun subscribeToNotifications(userId: String, onChanged: () -> Unit) {
    if (!SupabaseConfig.isConfigured || SupabaseSession.accessToken.isNullOrBlank() || userId.isBlank()) return
    addChannel(
      key = "notifications:$userId",
      topic = "realtime:notifications_$userId",
      changes = org.json.JSONArray().put(JSONObject().apply {
        put("event", "*")
        put("schema", "public")
        put("table", "notifications")
        put("filter", "target_user_id=eq.$userId")
      }),
      onChanged = onChanged
    )
  }

  /** Crew discover new paid requests and clients see status flips without refresh. */
  fun subscribeToBookings(key: String, onChanged: () -> Unit) {
    if (!SupabaseConfig.isConfigured || SupabaseSession.accessToken.isNullOrBlank()) return
    addChannel(
      key = "bookings:$key",
      topic = "realtime:bookings_$key",
      changes = org.json.JSONArray().put(JSONObject().apply {
        put("event", "*")
        put("schema", "public")
        put("table", "bookings")
      }),
      onChanged = onChanged
    )
  }

  fun unsubscribeFromChat(bookingId: String) {
    removeChannel("chat:$bookingId")
  }

  private fun addChannel(key: String, topic: String, changes: org.json.JSONArray, onChanged: () -> Unit) {
    channels[key] = Channel(topic, changes, onChanged)
    synchronized(lock) {
      val ws = socket
      if (ws == null) connectLocked()
      else sendJoin(ws, topic, changes)
    }
  }

  private fun removeChannel(key: String) {
    val removed = channels.remove(key)
    synchronized(lock) {
      // Tell the server to stop pushing this topic; otherwise it keeps
      // sending events for a chat the user already left.
      if (removed != null) {
        socket?.let { ws ->
          runCatching {
            ws.send(
              JSONObject().apply {
                put("event", "phx_leave")
                put("topic", removed.topic)
                put("ref", refCounter.getAndIncrement().toString())
                put("payload", JSONObject())
              }.toString()
            )
          }
        }
      }
      if (channels.isEmpty()) {
        mainHandler.removeCallbacks(heartbeatRunnable)
        mainHandler.removeCallbacks(reconnectRunnable)
        socket?.close(1000, "idle")
        socket = null
      }
    }
  }

  private fun connectLocked() {
    val socketUrl = SupabaseConfig.baseUrl.replaceFirst("https://", "wss://") +
      "/realtime/v1/websocket?apikey=${SupabaseConfig.publishableKey}&vsn=1.0.0"
    val request = Request.Builder().url(socketUrl).build()
    socket = http.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
        // (Re)join every channel on the single socket.
        channels.values.forEach { sendJoin(webSocket, it.topic, it.changes) }
        mainHandler.removeCallbacks(heartbeatRunnable)
        mainHandler.postDelayed(heartbeatRunnable, 25_000)
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        runCatching {
          val root = JSONObject(text)
          val event = root.optString("event")
          // Join results / errors: a rejected join (expired JWT, RLS) needs
          // a rejoin with a fresh token, not silence.
          if (event == "phx_reply") {
            val status = root.optJSONObject("payload")?.optString("status")
            if (status != null && status != "ok") {
              synchronized(lock) {
                if (socket === webSocket) socket = null
              }
              scheduleReconnect()
            }
            return
          }
          if (event != "postgres_changes") return
          val topic = root.optString("topic")
          if (topic.isBlank()) return
          // Exact topic match only. The old ifEmpty-fallback fanned one
          // booking's event out to every chat + bell + list (refresh storm).
          channels.values.firstOrNull { it.topic == topic }?.let { target ->
            runCatching { target.onChanged() }
          }
        }
      }

      private fun dropAndReconnect(webSocket: WebSocket) {
        synchronized(lock) {
          if (socket === webSocket) socket = null
        }
        mainHandler.removeCallbacks(heartbeatRunnable)
        // One delayed reconnect for all channels (network blips, sleep/wake).
        scheduleReconnect()
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        dropAndReconnect(webSocket)
      }

      // A clean server close does NOT call onFailure — without this the
      // socket reference stays non-null but dead forever.
      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        dropAndReconnect(webSocket)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        synchronized(lock) {
          if (socket === webSocket) socket = null
        }
        mainHandler.removeCallbacks(heartbeatRunnable)
        scheduleReconnect()
      }
    })
  }

  private fun sendJoin(webSocket: WebSocket, topic: String, changes: org.json.JSONArray) {
    val join = JSONObject().apply {
      put("event", "phx_join")
      put("topic", topic)
      put("ref", refCounter.getAndIncrement().toString())
      put("payload", JSONObject().apply {
        put("config", JSONObject().apply {
          // private:true enforces RLS on the socket. The old false value
          // broadcast every subscribed row to any authenticated client.
          put("private", true)
          put("postgres_changes", changes)
        })
        // Read the token fresh on every (re)join so a refreshed session
        // heals an expired-token kick without an app restart.
        put("access_token", SupabaseSession.accessToken)
      })
    }
    runCatching { webSocket.send(join.toString()) }
  }

  /** Call on logout so no socket keeps firing callbacks for the old session. */
  fun closeAll() {
    mainHandler.removeCallbacks(reconnectRunnable)
    mainHandler.removeCallbacks(heartbeatRunnable)
    synchronized(lock) {
      socket?.close(1000, "logout")
      socket = null
    }
    channels.clear()
  }
}
