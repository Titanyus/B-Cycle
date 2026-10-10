package de.bcycle.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.UUID

/** Standard Heart Rate Service, independent GATT client alongside Bosch/Flow. */
@SuppressLint("MissingPermission") // Runtime checks both Bluetooth permissions before use.
class HeartRateConnection(private val context: Context, private val status: (String) -> Unit,
                          private val measurement: (HeartRateMeasurement) -> Unit, private val battery: (Int) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var client: BluetoothGatt? = null
    private var epoch = 0
    var active = false; private set
    var live = false; private set
    private val timeout = Runnable { fail("Pulsmesser · Zeitlimit erreicht") }
    fun connect(device: BluetoothDevice) {
        stop(); active = true; val session = ++epoch
        status("Pulsmesser · Verbinden")
        handler.postDelayed(timeout, 20_000)
        try {
            client = device.connectGatt(context, false, object : BluetoothGattCallback() {
                private fun dispatch(gatt: BluetoothGatt, action: () -> Unit) { handler.post {
                    if (session == epoch && client === gatt && active) {
                        try { action() }
                        catch (_: SecurityException) { fail("Pulsmesser · Bluetooth-Zugriff fehlt") }
                        catch (_: IllegalStateException) { fail("Pulsmesser · Bluetooth nicht verfügbar") }
                    }
                } }
                override fun onConnectionStateChange(gatt: BluetoothGatt, result: Int, state: Int) = dispatch(gatt) {
                    if (result != BluetoothGatt.GATT_SUCCESS || state == BluetoothProfile.STATE_DISCONNECTED) fail("Pulsmesser · Verbindung getrennt")
                    else if (state == BluetoothProfile.STATE_CONNECTED && !gatt.discoverServices()) fail("Pulsmesser · Dienstsuche nicht möglich")
                }
                override fun onServicesDiscovered(gatt: BluetoothGatt, result: Int) = dispatch(gatt) {
                    val characteristic = gatt.getService(SERVICE)?.getCharacteristic(MEASUREMENT)
                    val descriptor = characteristic?.getDescriptor(CCCD)
                    if (result != BluetoothGatt.GATT_SUCCESS || characteristic == null || descriptor == null ||
                        characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) { fail("Kein BLE-Pulsmesser mit Standard-Pulsdienst"); return@dispatch }
                    if (!gatt.setCharacteristicNotification(characteristic, true) ||
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) != BluetoothStatusCodes.SUCCESS) fail("Pulsmesser · Benachrichtigungen nicht möglich")
                }
                override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, result: Int) = dispatch(gatt) {
                    if (descriptor.uuid != CCCD) return@dispatch
                    if (result != BluetoothGatt.GATT_SUCCESS) { fail("Pulsmesser · Freigabe fehlgeschlagen"); return@dispatch }
                    handler.removeCallbacks(timeout); live = true; status("Pulsmesser · Verbunden, warte auf Puls")
                    gatt.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY)?.let { gatt.readCharacteristic(it) }
                }
                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                    val copy = value.copyOf()
                    dispatch(gatt) { if (characteristic.uuid == MEASUREMENT && live) HeartRateMeasurement.decode(copy)?.let { measurement(it); status(if (it.bpm != null) "Pulsmesser · Live" else "Pulsmesser · Kein Hautkontakt") } }
                }
                override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, result: Int) {
                    val copy = value.copyOf()
                    dispatch(gatt) { if (result == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY && copy.size == 1) (copy[0].toInt() and 255).takeIf { it <= 100 }?.let(battery) }
                }
            }, BluetoothDevice.TRANSPORT_LE)
            if (client == null) fail("Pulsmesser · GATT nicht verfügbar")
        } catch (_: SecurityException) { fail("Pulsmesser · Bluetooth-Zugriff fehlt") }
        catch (_: IllegalStateException) { fail("Pulsmesser · Bluetooth nicht verfügbar") }
    }
    fun stop() {
        epoch++; active = false; live = false; handler.removeCallbacks(timeout)
        val old = client; client = null
        try { old?.disconnect(); old?.close() } catch (_: SecurityException) {}
    }
    private fun fail(message: String) { stop(); status(message) }
    companion object {
        val SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val BATTERY = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    }
}
