package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.example.R

/**
 * Soft UI sound effects (AOSP system sounds, Apache 2.0 — see Credits).
 * SoundPool keeps taps instant and light: short clips, low volumes, and
 * total silence unless the ringer is on NORMAL so it never irritates.
 */
object FameGoSfx {
  enum class Kind(val resId: Int, val volume: Float) {
    CLICK(R.raw.sfx_click, 0.30f),
    TAP(R.raw.sfx_tap, 0.30f),
    POP(R.raw.sfx_pop, 0.45f),
    SUCCESS(R.raw.sfx_success, 0.50f),
    ERROR(R.raw.sfx_error, 0.50f),
    NOTIFY(R.raw.sfx_notify, 0.70f),
  }

  @Volatile private var pool: SoundPool? = null
  private val soundIds = mutableMapOf<Kind, Int>()
  private val loaded = mutableSetOf<Kind>()
  private val lock = Any()

  /** Preloads clips; safe to call repeatedly from any component. */
  fun ensure(context: Context) {
    if (pool != null) return
    synchronized(lock) {
      if (pool != null) return
      val app = context.applicationContext
      val sp = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .build()
      sp.setOnLoadCompleteListener { _, sampleId, status ->
        if (status == 0) {
          synchronized(lock) {
            soundIds.entries.firstOrNull { it.value == sampleId }?.key?.let { loaded += it }
          }
        }
      }
      Kind.entries.forEach { kind ->
        runCatching { soundIds[kind] = sp.load(app, kind.resId, 1) }
      }
      pool = sp
    }
  }

  fun play(context: Context, kind: Kind) {
    ensure(context)
    // Respect quiet: silent/vibrate ringer means no UI sounds at all.
    val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    if (audio != null && audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
    val sp = pool ?: return
    val id = synchronized(lock) { soundIds[kind] } ?: return
    if (kind !in synchronized(lock) { loaded.toSet() }) return
    runCatching { sp.play(id, kind.volume, kind.volume, 1, 0, 1f) }
  }

  fun click(context: Context) = play(context, Kind.CLICK)
  fun tap(context: Context) = play(context, Kind.TAP)
  fun pop(context: Context) = play(context, Kind.POP)
  fun success(context: Context) = play(context, Kind.SUCCESS)
  fun error(context: Context) = play(context, Kind.ERROR)
  fun notify(context: Context) = play(context, Kind.NOTIFY)
}
