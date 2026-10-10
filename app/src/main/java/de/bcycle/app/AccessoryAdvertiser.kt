package de.bcycle.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import java.util.UUID

/** Experimental GAP peripheral. Never initiates an outgoing connection. */
@SuppressLint("MissingPermission") // Activity requests ADVERTISE and CONNECT before starting.
class AccessoryAdvertiser(context: Context, private val diagnostics: DiagnosticLog) {
    interface Listener {
        fun onReady(waitingForDisconnect: Boolean)
        fun onPeerStateChanged(waitingForDisconnect: Boolean)
        fun onIncomingConnection(device: BluetoothDevice)
        fun onFailure(message: String)
    }
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(BluetoothManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0
    private var server: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var callback: AdvertiseCallback? = null
    private var linkGate: AccessoryLinkGate? = null

    fun start(target: BluetoothDevice, listener: Listener) {
        stop()
        val session = generation
        val identity = AccessoryIdentity(appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "unknown")
        fun dispatch(action: () -> Unit) {
            handler.post {
                if (session != generation) return@post
                try { action() }
                catch (_: SecurityException) { listener.onFailure("Bluetooth-Berechtigung fehlt") }
                catch (_: IllegalStateException) { listener.onFailure("Bluetooth ist nicht verfügbar") }
            }
        }
        fun matches(device: BluetoothDevice) = device.address == target.address
        val adapter = manager.adapter
        if (adapter == null || !adapter.isEnabled) { listener.onFailure("Bitte Bluetooth einschalten"); return }
        advertiser = adapter.bluetoothLeAdvertiser
        val sessionAdvertiser = advertiser
        if (sessionAdvertiser == null) { listener.onFailure("Dieses Smartphone kann kein BLE-Zubehör ankündigen"); return }
        val existingLink = manager.getConnectedDevices(BluetoothProfile.GATT).any { matches(it) }
        val gate = AccessoryLinkGate(existingLink)
        linkGate = gate
        diagnostics.event("ACCESSORY_LINK_BASELINE selectedPeerConnected=$existingLink")
        val gattCallback = object : BluetoothGattServerCallback() {
            override fun onServiceAdded(status: Int, service: BluetoothGattService) = dispatch {
                diagnostics.event("ACCESSORY_SERVICE_ADDED status=$status uuid=${service.uuid}")
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    listener.onFailure("Eigener Informationsdienst konnte nicht bereitgestellt werden ($status)")
                    return@dispatch
                }
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .setConnectable(true).setTimeout(120_000).build()
                // Solicitation (AD type 0x15), not an announcement of a local LDI server.
                val data = AdvertiseData.Builder().addServiceSolicitationUuid(ParcelUuid(LDI)).build()
                var includeName = true
                lateinit var advertisingCallback: AdvertiseCallback
                fun advertise() {
                    val response = AdvertiseData.Builder().addServiceUuid(ParcelUuid(DIS))
                        .setIncludeDeviceName(includeName).build()
                    diagnostics.event("ACCESSORY_ADVERTISE_REQUEST solicitation=$LDI connectable=true nameIncluded=$includeName appearanceAD=unavailable")
                    sessionAdvertiser.startAdvertising(settings, data, response, advertisingCallback)
                }
                advertisingCallback = object : AdvertiseCallback() {
                    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                        val completedCallback = this
                        handler.post {
                            // Stop may precede Android's asynchronous start confirmation.
                            if (session != generation || callback !== completedCallback) {
                                try { sessionAdvertiser.stopAdvertising(completedCallback) } catch (_: SecurityException) { }
                                return@post
                            }
                            gate.onAdvertisingStarted()
                            diagnostics.event("ACCESSORY_ADVERTISE_STARTED waitingForDisconnect=${gate.waitingForDisconnect}")
                            listener.onReady(gate.waitingForDisconnect)
                        }
                    }
                    override fun onStartFailure(errorCode: Int) = dispatch {
                        diagnostics.event("ACCESSORY_ADVERTISE_FAILED code=$errorCode")
                        if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE && includeName) {
                            includeName = false
                            advertise()
                        } else listener.onFailure("BLE-Ankündigung konnte nicht gestartet werden ($errorCode)")
                    }
                }
                callback = advertisingCallback
                advertise()
            }

            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) = dispatch {
                val selected = matches(device)
                diagnostics.event("ACCESSORY_PEER_STATE status=$status state=$newState selectedPeer=$selected advertisingConfirmed=${gate.advertisingStarted}")
                if (!selected) return@dispatch
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gate.onDisconnected()
                    if (gate.advertisingStarted) listener.onPeerStateChanged(false)
                } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    val decision = gate.onConnected()
                    diagnostics.event("ACCESSORY_LINK_DECISION result=$decision")
                    if (decision == AccessoryLinkGate.Decision.ACCEPT) listener.onIncomingConnection(device)
                    else if (gate.advertisingStarted && gate.waitingForDisconnect) listener.onPeerStateChanged(true)
                }
            }

            override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) = dispatch {
                val accepted = matches(device)
                val value = identity.values[characteristic.uuid]
                val response = if (accepted && value != null) identity.read(characteristic.uuid, offset) else null
                val status = when {
                    !accepted -> BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION
                    value == null -> BluetoothGatt.GATT_READ_NOT_PERMITTED
                    response == null -> BluetoothGatt.GATT_INVALID_OFFSET
                    else -> BluetoothGatt.GATT_SUCCESS
                }
                diagnostics.event("ACCESSORY_INFO_READ uuid=${characteristic.uuid} offset=$offset status=$status")
                server?.sendResponse(device, requestId, status, offset, response)
            }

            override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) = dispatch {
                if (responseNeeded) server?.sendResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED, offset, null)
            }
        }
        server = manager.openGattServer(appContext, gattCallback)
        val service = BluetoothGattService(DIS, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        for (uuid in identity.values.keys) {
            service.addCharacteristic(BluetoothGattCharacteristic(uuid, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ))
        }
        diagnostics.event("ACCESSORY_SERVER_OPEN created=${server != null}")
        if (server?.addService(service) != true) listener.onFailure("Eigener BLE-Dienst konnte nicht gestartet werden")
    }

    fun stopAdvertising() {
        linkGate?.onAdvertisingStopped()
        val oldCallback = callback
        callback = null
        if (oldCallback != null) {
            try { advertiser?.stopAdvertising(oldCallback) } catch (_: SecurityException) { }
        }
    }

    fun stop() {
        generation++
        linkGate?.stop()
        linkGate = null
        stopAdvertising()
        val oldServer = server
        server = null; advertiser = null
        try { oldServer?.close() } catch (_: SecurityException) { }
        handler.removeCallbacksAndMessages(null)
    }

    private companion object {
        val LDI: UUID = UUID.fromString("0000eb20-eaa2-11e9-81b4-2a2ae2dbcce4")
        val DIS: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    }
}
