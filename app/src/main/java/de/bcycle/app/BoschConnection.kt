package de.bcycle.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import java.util.UUID

/** Native BLE client: read-only Smart System telemetry, over the hardware-tested MCSP path. */
@SuppressLint("MissingPermission") // MainActivity checks Bluetooth permissions before use.
class BoschConnection(context: Context, private val listener: Listener, private val diagnostics: DiagnosticLog) {
    interface Listener {
        fun onStatus(message: String)
        fun onData(data: LiveData, packetReceived: Boolean)
        fun onDevice(device: BluetoothDevice, name: String)
        fun onSamples(samples: List<McspSample>) {}
    }
    private enum class Phase { STOPPED, SCANNING, CHOOSING, CONNECTING, BONDING, MTU, DISCOVERY, SUBSCRIBING, LIVE }
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(BluetoothManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val retiredClients = DeferredConnectionClose<BluetoothGatt>(
        disconnect = {
            diagnostics.event("OWN_GATT_RELEASE disconnect requested")
            try { it.disconnect() } catch (_: SecurityException) { } catch (_: IllegalStateException) { }
        },
        close = {
            diagnostics.event("OWN_GATT_RELEASE client closed")
            try { it.close() } catch (_: SecurityException) { }
        },
        schedule = { task, delay -> handler.postDelayed(task, delay) },
        cancel = { handler.removeCallbacks(it) },
    )
    private var mcsp = McspProtocol()
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var writes: SerialWriteQueue<McspTransmission>? = null
    private var phase = Phase.STOPPED
    val isActive: Boolean get() = phase != Phase.STOPPED
    val isLive: Boolean get() = phase == Phase.LIVE
    private var epoch = 0
    private var client: BluetoothGatt? = null
    private var bike: BluetoothDevice? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var negotiatedMtu = 23
    private var liveData = LiveData()
    private var liveMeasurementsReceived = false
    private var receiverRegistered = false
    private var lastDiagnosticStatus: String? = null
    private var scanCallback: ScanCallback? = null
    private val timeout = Runnable { fail("Zeitlimit erreicht. Bitte Verbindung und Firmware prüfen und erneut versuchen.") }
    private val scanTimeout = Runnable {
        stopScan()
        phase = Phase.CHOOSING
        reportStatus("Suche beendet · Fahrrad aus der Liste auswählen")
    }
    private val bondReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
            val session = epoch
            dispatch {
                if (!isCurrent(session) || phase != Phase.BONDING || device.address != bike?.address) return@dispatch
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                diagnostics.event("BOND_CHANGED state=$state")
                when (state) {
                    BluetoothDevice.BOND_BONDED -> client?.let { negotiateMtu(it) }
                    BluetoothDevice.BOND_NONE -> fail("Kopplung fehlgeschlagen oder abgelehnt")
                }
            }
        }
    }

    fun discover() {
        stop()
        try {
            val adapter = manager.adapter
            if (adapter == null || !adapter.isEnabled) {
                reportStatus("Bitte Bluetooth einschalten")
                return
            }
            phase = Phase.SCANNING
            val session = ++epoch
            diagnostics.event("SCAN_BEGIN session=$session")
            // Existing links (e.g. Flow) remain usable even if the bike stops advertising.
            val known = manager.getConnectedDevices(BluetoothProfile.GATT) + adapter.bondedDevices.orEmpty()
            diagnostics.event("Known BLE candidates=${known.size}; no names or addresses retained")
            for (device in known.distinctBy { it.address }) {
                if (device.type != BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                    listener.onDevice(device, device.name ?: "Bekanntes BLE-Gerät")
                }
            }
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    dispatch {
                        if (!isCurrent(session) || phase != Phase.SCANNING) return@dispatch
                        listener.onDevice(result.device, result.scanRecord?.deviceName ?: result.device.name ?: "Unbenanntes BLE-Gerät")
                    }
                }
                override fun onScanFailed(errorCode: Int) {
                    dispatch {
                        if (!isCurrent(session) || phase != Phase.SCANNING) return@dispatch
                        stopScan()
                        phase = Phase.CHOOSING
                        reportStatus("Suche fehlgeschlagen ($errorCode) · bekannte Geräte können ausgewählt werden")
                    }
                }
            }
            scanCallback = callback
            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                phase = Phase.CHOOSING
                reportStatus("BLE-Suche nicht verfügbar · bekannte Geräte können ausgewählt werden")
                return
            }
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
            handler.postDelayed(scanTimeout, 10_000)
            reportStatus("Suche nach BLE-Geräten · Fahrrad auswählen")
        } catch (_: SecurityException) {
            fail("Bluetooth-Berechtigung fehlt")
        } catch (_: IllegalStateException) {
            fail("Bluetooth ist nicht verfügbar")
        }
    }

    fun connect(device: BluetoothDevice) {
        stop()
        diagnostics.event("MODE direct Android BLE client protocol=MCSP")
        beginClient(device, ++epoch)
    }

    fun onForegroundChanged(foreground: Boolean) {
        diagnostics.event("FOREGROUND value=$foreground protocol=MCSP phase=$phase")
        if (!foreground) stop()
    }

    private fun beginClient(device: BluetoothDevice, session: Int) {
        phase = Phase.CONNECTING
        bike = device
        try {
            diagnostics.event("CONNECT_BEGIN session=$session deviceType=${device.type} bondState=${device.bondState}")
            appContext.registerReceiver(bondReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED), Context.RECEIVER_EXPORTED)
            receiverRegistered = true
            reportStatus("Verbindung zum E-Bike wird aufgebaut")
            armTimeout(20_000)
            client = device.connectGatt(appContext, false, clientCallback(session), BluetoothDevice.TRANSPORT_LE)
            diagnostics.event("CONNECT_REQUEST clientCreated=${client != null}")
            if (client == null) fail("Android konnte keinen GATT-Client öffnen")
        } catch (_: SecurityException) {
            fail("Bluetooth-Berechtigung fehlt")
        } catch (_: IllegalStateException) {
            fail("Bluetooth ist nicht verfügbar")
        }
    }

    private fun negotiateMtu(gatt: BluetoothGatt) {
        phase = Phase.MTU
        reportStatus("Verbindung wird vorbereitet")
        armTimeout(20_000)
        val requested = gatt.requestMtu(247)
        diagnostics.event("MTU_REQUEST value=247 started=$requested")
        if (!requested) fail("MTU-Anfrage konnte nicht gestartet werden")
    }

    private fun clientCallback(session: Int) = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            dispatch {
                if (newState == BluetoothProfile.STATE_DISCONNECTED && retiredClients.onDisconnected(gatt)) {
                    diagnostics.event("OWN_GATT_RELEASE disconnected status=$status")
                    return@dispatch
                }
                if (!isClient(session, gatt)) return@dispatch
                diagnostics.event("CONNECTION_STATE status=$status newState=$newState phase=$phase")
                if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                    fail("Android-GATT-Verbindung beendet ($status)")
                } else if (newState == BluetoothProfile.STATE_CONNECTED && phase == Phase.CONNECTING) {
                    val device = bike ?: return@dispatch
                    if (device.bondState == BluetoothDevice.BOND_BONDED) {
                        negotiateMtu(gatt)
                    } else {
                        phase = Phase.BONDING
                        reportStatus("GATT verbunden · Android-Kopplung wird geprüft")
                        armTimeout(60_000)
                        diagnostics.event("BOND_CHECK state=${device.bondState}")
                        if (device.bondState == BluetoothDevice.BOND_NONE) {
                            val requested = device.createBond()
                            diagnostics.event("BOND_REQUEST started=$requested")
                            if (!requested) fail("Android konnte die Kopplung nicht starten")
                        }
                    }
                }
            }
        }
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            dispatch {
                if (!isClient(session, gatt) || phase != Phase.MTU) return@dispatch
                diagnostics.event("MTU_CHANGED status=$status value=$mtu")
                if (status != BluetoothGatt.GATT_SUCCESS || mtu < 247) {
                    fail("MTU $mtu ist nicht ausreichend · mindestens 247 erforderlich")
                    return@dispatch
                }
                negotiatedMtu = mtu
                phase = Phase.DISCOVERY
                reportStatus("Fahrdatenzugriff wird vorbereitet")
                armTimeout(20_000)
                val requested = gatt.discoverServices()
                diagnostics.event("DISCOVER_SERVICES started=$requested")
                if (!requested) fail("Dienstsuche konnte nicht gestartet werden")
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            dispatch {
                if (!isClient(session, gatt) || phase != Phase.DISCOVERY) return@dispatch
                diagnostics.event("SERVICES_DISCOVERED status=$status count=${gatt.services.size}")
                if (status != BluetoothGatt.GATT_SUCCESS) { fail("Dienstsuche fehlgeschlagen ($status)"); return@dispatch }
                for (service in gatt.services) {
                    diagnostics.event("SERVICE uuid=${service.uuid} instance=${service.instanceId}")
                    for (attribute in service.characteristics) {
                        diagnostics.event("  CHARACTERISTIC uuid=${attribute.uuid} properties=${attribute.properties} descriptors=${attribute.descriptors.map { it.uuid }}")
                    }
                }
                val item = gatt.getService(MCSP_SERVICE)?.getCharacteristic(MCSP_RX)
                val descriptor = item?.getDescriptor(CCCD)
                val required = BluetoothGattCharacteristic.PROPERTY_NOTIFY
                diagnostics.event("MCSP_CHECK characteristicFound=${item != null} cccdFound=${descriptor != null} properties=${item?.properties}")
                if (item == null || descriptor == null || item.properties and required != required) {
                    fail("Bosch-MCSP-Datenkanal nicht verfügbar")
                    return@dispatch
                }
                val tx = gatt.getService(MCSP_SERVICE)?.getCharacteristic(MCSP_TX)
                if (tx == null || tx.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE == 0) {
                    fail("Bosch-MCSP-Anfragekanal nicht verfügbar")
                    return@dispatch
                }
                writeCharacteristic = tx
                writes = SerialWriteQueue(
                    send = { request ->
                        diagnostics.event("MCSP_TX ${request.description}")
                        try {
                            val result = gatt.writeCharacteristic(tx, request.bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                            diagnostics.event("MCSP_TX_STARTED result=$result")
                            result == BluetoothStatusCodes.SUCCESS
                        } catch (_: SecurityException) {
                            diagnostics.event("MCSP_TX_EXCEPTION SecurityException"); false
                        } catch (_: IllegalStateException) {
                            diagnostics.event("MCSP_TX_EXCEPTION IllegalStateException"); false
                        }
                    },
                    schedule = { task, delay -> handler.postDelayed(task, delay) },
                    cancel = { handler.removeCallbacks(it) },
                    changed = { if (phase == Phase.LIVE) reportMeasurementsStatus() },
                    failed = { fail(it) },
                )
                characteristic = item
                phase = Phase.SUBSCRIBING
                reportStatus("Fahrdaten werden abonniert")
                armTimeout(20_000)
                val localEnabled = gatt.setCharacteristicNotification(item, true)
                diagnostics.event("NOTIFICATION_LOCAL enabled=$localEnabled")
                if (!localEnabled) {
                    fail("Benachrichtigungen konnten nicht aktiviert werden")
                } else {
                    val result = gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    diagnostics.event("CCCD_WRITE_REQUEST result=$result value=0100")
                    if (result != BluetoothStatusCodes.SUCCESS) fail("Benachrichtigungen konnten nicht aktiviert werden")
                }
            }
        }
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            dispatch {
                if (!isClient(session, gatt) || phase != Phase.SUBSCRIBING || descriptor.uuid != CCCD ||
                    descriptor.characteristic !== characteristic) return@dispatch
                diagnostics.event("CCCD_WRITE_RESULT status=$status")
                if (status != BluetoothGatt.GATT_SUCCESS) { fail("Benachrichtigungsfreigabe fehlgeschlagen ($status)"); return@dispatch }
                phase = Phase.LIVE
                handler.removeCallbacks(timeout)
                diagnostics.event("MCSP_READY; subscribing to known live fields, then requesting snapshots")
                writes?.enqueue(mcsp.subscriptions() + mcsp.reads())
                writes?.start()
                if (isClient(session, gatt)) reportMeasurementsStatus()
            }
        }
        override fun onCharacteristicChanged(gatt: BluetoothGatt, item: BluetoothGattCharacteristic, value: ByteArray) {
            val payload = value.copyOf()
            dispatch {
                if (!isClient(session, gatt) || item.uuid != characteristic?.uuid ||
                    (phase != Phase.SUBSCRIBING && phase != Phase.LIVE)) return@dispatch
                receiveMcsp(payload)
            }
        }
        override fun onCharacteristicWrite(gatt: BluetoothGatt, item: BluetoothGattCharacteristic, status: Int) {
            dispatch {
                if (!isClient(session, gatt) || item.uuid != writeCharacteristic?.uuid) return@dispatch
                diagnostics.event("MCSP_TX_RESULT status=$status")
                writes?.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }
    }

    private fun receiveMcsp(payload: ByteArray) {
        diagnostics.mcspNotification(payload.size)
        val outcome = try {
            require(payload.size <= negotiatedMtu - 3) { "MCSP payload exceeds negotiated MTU" }
            mcsp.receive(payload)
        } catch (error: IllegalArgumentException) {
            diagnostics.decodeFailed(error.message ?: "Invalid MCSP packet")
            return
        }
        diagnostics.mcspOutcome(outcome)
        for (event in outcome.events) diagnostics.event(event)
        if (outcome.replies.isNotEmpty()) writes?.enqueue(outcome.replies, urgent = true)
        if (phase == Phase.STOPPED) return
        if (outcome.update.hasValues) {
            listener.onSamples(outcome.samples)
            liveMeasurementsReceived = liveMeasurementsReceived || outcome.samples.any { it.isPush }
            liveData = liveData.merge(outcome.update)
            listener.onData(liveData, true)
            if (phase == Phase.LIVE) reportMeasurementsStatus()
        }
    }

    private fun reportMeasurementsStatus() {
        val message = when {
            !liveData.hasValues -> "Verbunden · noch keine Fahrdaten"
            liveMeasurementsReceived -> "Live-Messwerte empfangen"
            else -> "Fahrdaten gelesen · warte auf Live-Messwerte"
        }
        val requesting = if (writes?.idle == false) " · Werte werden angefragt" else ""
        reportStatus("$message$requesting")
    }

    private fun reportStatus(message: String) {
        val event = "STATUS phase=$phase $message"
        if (event != lastDiagnosticStatus) {
            diagnostics.event(event)
            lastDiagnosticStatus = event
        }
        listener.onStatus(message)
    }
    private fun dispatch(action: () -> Unit) {
        handler.post {
            try { action() }
            catch (_: SecurityException) { diagnostics.event("CALLBACK_EXCEPTION SecurityException"); fail("Bluetooth-Berechtigung fehlt") }
            catch (_: IllegalStateException) { diagnostics.event("CALLBACK_EXCEPTION IllegalStateException"); fail("Bluetooth ist nicht verfügbar") }
        }
    }
    private fun isCurrent(session: Int) = session == epoch && phase != Phase.STOPPED
    private fun isClient(session: Int, gatt: BluetoothGatt) = isCurrent(session) && client === gatt
    private fun armTimeout(delay: Long) { handler.removeCallbacks(timeout); handler.postDelayed(timeout, delay) }
    private fun fail(message: String) { stop(); reportStatus(message) }
    private fun stopScan() {
        handler.removeCallbacks(scanTimeout)
        val callback = scanCallback
        scanCallback = null
        if (callback != null) {
            try { manager.adapter?.bluetoothLeScanner?.stopScan(callback) }
            catch (_: SecurityException) { }
            catch (_: IllegalStateException) { }
        }
    }
    fun stop() {
        val wasActive = phase != Phase.STOPPED
        if (wasActive) diagnostics.event("STOP phase=$phase")
        phase = Phase.STOPPED
        epoch++
        handler.removeCallbacks(timeout)
        stopScan()
        writes?.stop(); writes = null
        writeCharacteristic = null
        mcsp = McspProtocol()
        if (receiverRegistered) { appContext.unregisterReceiver(bondReceiver); receiverRegistered = false }
        val oldClient = client
        client = null; bike = null; characteristic = null
        oldClient?.let { retiredClients.release(it) }
        liveData = LiveData(); negotiatedMtu = 23
        liveMeasurementsReceived = false
        listener.onData(liveData, false)
        if (wasActive) reportStatus("Gestoppt")
    }
    fun close() { stop(); retiredClients.closeAll(); handler.removeCallbacksAndMessages(null) }

    private companion object {
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val MCSP_SERVICE: UUID = UUID.fromString("00000010-eaa2-11e9-81b4-2a2ae2dbcce4")
        val MCSP_RX: UUID = UUID.fromString("00000011-eaa2-11e9-81b4-2a2ae2dbcce4")
        val MCSP_TX: UUID = UUID.fromString("00000012-eaa2-11e9-81b4-2a2ae2dbcce4")
    }
}
