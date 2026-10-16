package de.bcycle.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.UUID

/** One bounded scan, with serial service discovery for devices that omit service UUIDs in advertising. */
@SuppressLint("MissingPermission")
internal class BikeSensorScanner(private val context: Context, private val excluded: Set<String>,
    private val found: (BluetoothDevice, String, BikeSensor) -> Unit, private val status: (String) -> Unit, private val allowed:Set<BikeSensor> = setOf(BikeSensor.EBIKE,BikeSensor.HEART,BikeSensor.TRAINER)) {
    private val adapter = context.getSystemService(BluetoothManager::class.java).adapter
    private val handler = Handler(Looper.getMainLooper())
    private var callback: ScanCallback? = null
    private var probe: BluetoothGatt? = null
    private var running = false
    private val seen = mutableSetOf<String>()
    private val queue = ArrayDeque<Pair<BluetoothDevice, String>>()
    private val finish = Runnable { stop(); status(context.getString(R.string.bikes_search_done)) }
    private val probeTimeout = Runnable { endProbe(); nextProbe() }
    private fun kind(services: List<UUID>) = when {
        BikeSensor.TRAINER in allowed && TrainerConnection.SERVICE in services -> BikeSensor.TRAINER
        BikeSensor.EBIKE in allowed && BoschConnection.MCSP_SERVICE in services -> BikeSensor.EBIKE
        BikeSensor.HEART in allowed && HeartRateConnection.SERVICE in services -> BikeSensor.HEART
        else -> null
    }
    fun start() {
        stop(); running = true; seen.clear()
        try {
            val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
                ?: run { stop(); status(context.getString(R.string.bikes_scan_bluetooth)); return }
            val next = object : ScanCallback() {
                override fun onScanResult(type: Int, result: ScanResult) { handler.post {
                    if (!running || callback !== this) return@post
                    try {
                        val device = result.device
                        val address = device.address
                        if (address in excluded || !seen.add(address)) return@post
                        val name = result.scanRecord?.deviceName ?: device.name ?: address
                        val category = kind(result.scanRecord?.serviceUuids.orEmpty().map { it.uuid })
                        if (category != null) found(device, name, category)
                        else if (queue.size < 10) { queue.addLast(device to name); nextProbe() }
                    } catch (_: SecurityException) { stop(); status(context.getString(R.string.bikes_scan_permission)) }
                } }
                override fun onScanFailed(errorCode: Int) { handler.post {
                    if (callback !== this) return@post
                    stop(); status(context.getString(R.string.bikes_scan_error, errorCode))
                } }
            }
            callback = next
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), next)
            status(context.getString(R.string.bikes_searching)); handler.postDelayed(finish, 10_000)
            // Keep discoverability of previously paired bikes even while another app holds a link.
            val known = context.getSystemService(BluetoothManager::class.java).getConnectedDevices(BluetoothProfile.GATT) + adapter?.bondedDevices.orEmpty()
            known.distinctBy { it.address }.filter { it.type != BluetoothDevice.DEVICE_TYPE_CLASSIC }.forEach { device ->
                if (device.address !in excluded && seen.add(device.address) && queue.size < 10) queue.addLast(device to (device.name ?: device.address))
            }
            nextProbe()
        } catch (_: SecurityException) { stop(); status(context.getString(R.string.bikes_scan_permission)) }
        catch (_: IllegalStateException) { stop(); status(context.getString(R.string.bikes_scan_bluetooth)) }
    }
    private fun nextProbe() {
        if (!running || probe != null || queue.isEmpty()) return
        val (device, name) = queue.removeFirst()
        try {
            probe = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, statusCode: Int, state: Int) { handler.post {
                    if (!running || probe !== gatt) return@post
                    if (statusCode == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED) {
                        try { if (!gatt.discoverServices()) { endProbe(); nextProbe() } }
                        catch (_: SecurityException) { endProbe(); nextProbe() }
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED || statusCode != BluetoothGatt.GATT_SUCCESS) { endProbe(); nextProbe() }
                } }
                override fun onServicesDiscovered(gatt: BluetoothGatt, statusCode: Int) { handler.post {
                    if (!running || probe !== gatt) return@post
                    try {
                        if (statusCode == BluetoothGatt.GATT_SUCCESS) kind(gatt.services.map { it.uuid })?.let { found(device, name, it) }
                    } catch (_: SecurityException) { stop();status(context.getString(R.string.bikes_scan_permission));return@post }
                    endProbe(); nextProbe()
                } }
            }, BluetoothDevice.TRANSPORT_LE)
            handler.postDelayed(probeTimeout, 3000)
        } catch (_: SecurityException) { stop(); status(context.getString(R.string.bikes_scan_permission)) }
        catch (_: IllegalStateException) { endProbe(); nextProbe() }
    }
    private fun endProbe() {
        handler.removeCallbacks(probeTimeout)
        val old = probe; probe = null
        try { old?.disconnect(); old?.close() } catch (_: SecurityException) { }
    }
    fun stop() {
        running = false; handler.removeCallbacks(finish)
        val old = callback; callback = null
        try { if (old != null) adapter?.bluetoothLeScanner?.stopScan(old) } catch (_: SecurityException) { } catch (_: IllegalStateException) { }
        queue.clear(); endProbe()
    }
}
