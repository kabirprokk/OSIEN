package com.example.data

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.util.UUID

/**
 * Bluetooth proximity proof for "Shoot Done".
 *
 * Both phones press Shoot Done at the venue. Each phone BLE-advertises a
 * booking-specific service UUID *and* scans for it. BLE only travels
 * ~10-30m, so a discovery proves both sides are physically together — the
 * wrap can't be faked from home. No pairing, no connection, no personal
 * data leaves the phone: the advertisement is just a random booking UUID.
 *
 * On discovery the caller writes its row to shoot_done_signals
 * (supabase/011); when BOTH rows exist the booking completes and both
 * parties get "Shoot is Done".
 */
object ShootDoneProximity {

  /** Stable booking-specific 128-bit service UUID (v3, MD5 of the booking). */
  fun serviceUuidFor(bookingId: String): UUID =
    UUID.nameUUIDFromBytes("famego-shoot-done:$bookingId".toByteArray(Charsets.UTF_8))

  /** Runtime permissions still missing on this device (empty = good to go). */
  fun missingPermissions(context: Context): List<String> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      // Pre-31 BLE scans ride on location permission (already in manifest).
      return listOf(Manifest.permission.ACCESS_FINE_LOCATION).filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
      }
    }
    return listOf(
      Manifest.permission.BLUETOOTH_SCAN,
      Manifest.permission.BLUETOOTH_ADVERTISE,
      Manifest.permission.BLUETOOTH_CONNECT
    ).filter {
      ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
  }

  fun isBluetoothOn(context: Context): Boolean {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return manager?.adapter?.isEnabled == true
  }

  /**
   * Starts advertising + scanning. [onPeerFound] fires once on first
   * discovery of the sibling phone. Returns a session — call [Session.stop].
   * Throws on missing hardware/disabled BT (caller checks first).
   */
  fun start(
    context: Context,
    bookingId: String,
    onPeerFound: () -> Unit,
    onError: (String) -> Unit
  ): Session {
    val app = context.applicationContext
    val manager = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter = manager?.adapter
      ?: throw IllegalStateException("This phone has no Bluetooth.")
    if (!adapter.isEnabled) throw IllegalStateException("Bluetooth is off.")
    val advertiser = adapter.bluetoothLeAdvertiser
      ?: throw IllegalStateException("This phone can't broadcast (BLE advertising unsupported).")
    val scanner = adapter.bluetoothLeScanner
      ?: throw IllegalStateException("This phone can't scan for nearby devices.")
    return Session(app, bookingId, advertiser, scanner, onPeerFound, onError).also { it.start() }
  }

  class Session(
    private val context: Context,
    bookingId: String,
    private val advertiser: android.bluetooth.le.BluetoothLeAdvertiser,
    private val scanner: android.bluetooth.le.BluetoothLeScanner,
    private val onPeerFound: () -> Unit,
    private val onError: (String) -> Unit
  ) {
    private val uuid = ParcelUuid(serviceUuidFor(bookingId))
    @Volatile private var stopped = false
    @Volatile private var found = false

    private val advertiseCallback = object : AdvertiseCallback() {
      override fun onStartSuccess(settingsInEffect: AdvertiseSettings) = Unit
      override fun onStartFailure(errorCode: Int) {
        if (!stopped) onError("Couldn't broadcast nearby (code $errorCode). Move closer and retry.")
      }
    }

    private val scanCallback = object : ScanCallback() {
      override fun onScanResult(callbackType: Int, result: ScanResult) {
        if (stopped || found) return
        // Filtered scan already matches our UUID; the RSSI gate keeps it
        // honest — ignore whisper-weak ghosts from floors away.
        if ((result.rssi) < -85) return
        found = true
        runCatching { onPeerFound() }
      }

      override fun onScanFailed(errorCode: Int) {
        if (!stopped) onError("Couldn't look for the other phone (code $errorCode).")
      }
    }

    fun start() {
      try {
        val settings = AdvertiseSettings.Builder()
          .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
          .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
          .setConnectable(false)
          .setTimeout(0)
          .build()
        val data = AdvertiseData.Builder()
          .setIncludeDeviceName(false)
          .setIncludeTxPowerLevel(false)
          .addServiceUuid(uuid)
          .build()
        advertiser.startAdvertising(settings, data, advertiseCallback)
        val filter = ScanFilter.Builder().setServiceUuid(uuid).build()
        val scanSettings = ScanSettings.Builder()
          .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
          .build()
        scanner.startScan(listOf(filter), scanSettings, scanCallback)
      } catch (e: SecurityException) {
        onError("Bluetooth permission was revoked. Allow Nearby devices and retry.")
      } catch (e: Exception) {
        onError("Couldn't start nearby search: ${e.message}")
      }
    }

    fun stop() {
      stopped = true
      runCatching { advertiser.stopAdvertising(advertiseCallback) }
      runCatching { scanner.stopScan(scanCallback) }
    }
  }
}
