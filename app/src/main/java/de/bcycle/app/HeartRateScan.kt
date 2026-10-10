package de.bcycle.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid

@SuppressLint("MissingPermission")
class HeartRateScan(context: Context, private val found: (BluetoothDevice, String) -> Unit, private val status: (String) -> Unit) {
    private val adapter = context.getSystemService(BluetoothManager::class.java).adapter
    private val handler = Handler(Looper.getMainLooper())
    private var callback: ScanCallback? = null
    private val stopScan = Runnable { stop(); status("Suche beendet") }
    fun start() {
        stop()
        try {
            val scanner = adapter?.bluetoothLeScanner ?: run { status("Bitte Bluetooth einschalten"); return }
            val next = object : ScanCallback() {
                override fun onScanResult(type: Int, result: ScanResult) { handler.post {
                    if (callback === this) {
                        try { found(result.device, result.scanRecord?.deviceName ?: result.device.name ?: "BLE-Pulsmesser") }
                        catch (_: SecurityException) { stop(); status("Bluetooth-Zugriff fehlt") }
                    }
                } }
                override fun onScanFailed(error: Int) { handler.post { if (callback === this) { stop(); status("Suche fehlgeschlagen ($error)") } } }
            }
            callback = next
            scanner.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(HeartRateConnection.SERVICE)).build()),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), next)
            status("Suche nach BLE-Pulsmessern"); handler.postDelayed(stopScan, 10_000)
        } catch (_: SecurityException) { stop(); status("Bluetooth-Zugriff fehlt") }
        catch (_: IllegalStateException) { stop(); status("Bluetooth nicht verfügbar") }
    }
    fun stop() {
        handler.removeCallbacks(stopScan)
        val old = callback; callback = null
        try { if (old != null) adapter?.bluetoothLeScanner?.stopScan(old) } catch (_: SecurityException) {} catch (_: IllegalStateException) {}
    }
}
