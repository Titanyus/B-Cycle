package de.bcycle.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Activity and recording service share existing sensors; no second Bosch client is created. */
class RideRuntime private constructor(val context: Context) : BoschConnection.Listener {
    val preferences = context.getSharedPreferences("bike", Context.MODE_PRIVATE)
    val diagnostics = DiagnosticLog()
    val ride = RideSession()
    val connection = BoschConnection(context, this, diagnostics)
    val gpsSensor = GpsSensor(context, ride.gps) { gpsStatus = it; notifyChanged() }
    private val handler = Handler(Looper.getMainLooper())
    private val bikeReconnect = AutoReconnect()
    private val heartReconnect = AutoReconnect()
    private val times = mutableMapOf<LiveMetric, Long>()
    val observers = linkedSetOf<() -> Unit>()
    var scanFound: ((BluetoothDevice, String) -> Unit)? = null
    var scanStatus: ((String) -> Unit)? = null
    var bikeData = LiveData(); private set
    var bikeStatus = "Nicht verbunden"; private set
    var heartStatus = "Pulsmesser · Nicht verbunden"; private set
    var gpsStatus = "GPS · Aus"; private set
    var lastPacketAt: Long? = null; private set
    var recording = false; private set
    var saving = false; private set
    var recordingStatus = "Bereit · START beginnt die Aufzeichnung"; private set
    var visible = false; private set
    var gpsEnabled = preferences.getBoolean("gpsEnabled", false); private set
    private var heartAt: Long? = null
    private var pulse = PulseValues()
    private var heartSum = 0L; private var heartCount = 0
    private var startElapsed = 0L
    private var startUtc = 0L
    private var activeConfig = RecordingConfig()
    val mapState = MapState(context) { notifyChanged() }
    var mapTrackEnabled = true; private set
    private val store = RecordingStore(context) { text, busy -> saving = busy; recordingStatus = text; notifyChanged() }
    val heart: HeartRateConnection = HeartRateConnection(context, { heartStatus = it; if (heartIsLive()) heartReconnect.connected(); if (!heartActive()) { pulse = pulse.copy(bpm = null); heartAt = null }; notifyChanged() }, {
        heartAt = SystemClock.elapsedRealtime()
        it.bpm?.let { bpm -> heartSum += bpm; heartCount++; pulse = pulse.copy(average = heartSum.toDouble() / heartCount, maximum = maxOf(pulse.maximum ?: bpm, bpm)) }
        pulse = pulse.copy(bpm = it.bpm); notifyChanged()
    }, { pulse = pulse.copy(battery = it); notifyChanged() })
    private fun heartActive() = heart.active
    private fun heartIsLive() = heart.live
    val bikeAddress get() = preferences.getString("address", null)
    val bikeName get() = preferences.getString("name", "E-Bike") ?: "E-Bike"
    val heartAddress get() = preferences.getString("heartAddress", null)
    val heartName get() = preferences.getString("heartName", "Pulsmesser") ?: "Pulsmesser"
    val bikeEnabled get() = preferences.getBoolean("bikeEnabled", true)
    val heartEnabled get() = preferences.getBoolean("heartEnabled", true)
    val active get() = visible || recording
    val elapsedMs get() = if (recording) SystemClock.elapsedRealtime() - startElapsed else 0L
    val pulseValues get() = pulse.copy(bpm = pulse.bpm.takeIf { heartAt?.let { at -> SystemClock.elapsedRealtime() - at <= 5_000 } == true && heart.live })
    val gpsValues get() = ride.gps.snapshot(SystemClock.elapsedRealtime())
    val config get() = RecordingConfig.decode(preferences.getStringSet("recordFields", null), preferences.getString("recordSpeed", null), preferences.getString("recordDistance", null))
    private val tick = object : Runnable {
        override fun run() {
            if (!active) return
            reconnectSensors()
            if (recording) appendSample()
            notifyChanged(); handler.postDelayed(this, 1000)
        }
    }
    init { diagnostics.reset("App=0.5.1 Mode=MCSP Android=${android.os.Build.VERSION.SDK_INT} Device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}") }
    fun notifyChanged() { observers.toList().forEach { it() } }
    fun setVisible(value: Boolean) { visible = value; updateActivity() }
    private fun updateActivity() {
        bikeReconnect.foreground = active; heartReconnect.foreground = active
        handler.removeCallbacks(tick)
        if (active) {
            if (gpsEnabled) gpsSensor.start()
            reconnectSensors(); handler.postDelayed(tick, 1000)
        } else {
            connection.onForegroundChanged(false); heart.stop(); gpsSensor.stop(); ride.bike.pause(); heartAt = null
        }
        notifyChanged()
    }
    fun hasBluetooth() = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN).all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    fun hasGps() = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    @SuppressLint("MissingPermission")
    fun reconnectSensors() {
        if (!active || !hasBluetooth()) return
        val adapter = context.getSystemService(BluetoothManager::class.java).adapter ?: return
        if (!adapter.isEnabled) return
        val now = SystemClock.elapsedRealtime()
        bikeReconnect.enabled = bikeEnabled; heartReconnect.enabled = heartEnabled
        bikeAddress?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }?.let {
            if (bikeReconnect.due(now, connection.isActive)) { bikeReconnect.attempted(now); connection.connect(adapter.getRemoteDevice(it)) }
        }
        heartAddress?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }?.let {
            if (heartReconnect.due(now, heart.active)) { heartReconnect.attempted(now); heart.connect(adapter.getRemoteDevice(it)) }
        }
    }
    fun selectBike(device: BluetoothDevice, name: String) {
        if (bikeAddress != device.address) ride.bike.reset()
        preferences.edit().putString("address", device.address).putString("name", name).putBoolean("bikeEnabled", true).apply()
        bikeReconnect.selecting = false; connection.stop(); bikeReconnect.start(); reconnectSensors(); notifyChanged()
    }
    fun setBikeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("bikeEnabled", enabled).apply()
        if (!enabled) connection.stop() else bikeReconnect.start()
        reconnectSensors(); notifyChanged()
    }
    fun selectHeart(device: BluetoothDevice, name: String) {
        preferences.edit().putString("heartAddress", device.address).putString("heartName", name).putBoolean("heartEnabled", true).apply()
        heart.stop(); pulse = PulseValues(); heartAt = null; heartReconnect.start(); reconnectSensors(); notifyChanged()
    }
    fun setHeartEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("heartEnabled", enabled).apply()
        if (!enabled) { heart.stop(); heartAt = null; pulse = pulse.copy(bpm = null); heartStatus = "Pulsmesser · Getrennt" } else heartReconnect.start()
        reconnectSensors(); notifyChanged()
    }
    fun setGpsEnabled(enabled: Boolean) {
        gpsEnabled = enabled; preferences.edit().putBoolean("gpsEnabled", enabled).apply()
        if (!enabled) { gpsSensor.stop(); gpsStatus = "GPS · Aus" } else if (active) gpsSensor.start()
        notifyChanged()
    }
    fun beginBikeScan() { bikeReconnect.selecting = true; connection.discover() }
    fun endBikeScan() { bikeReconnect.selecting = false; connection.stop(); bikeReconnect.connected(); reconnectSensors() }
    fun saveConfig(config: RecordingConfig) {
        preferences.edit().putStringSet("recordFields", config.fields).putString("recordSpeed", config.speedSource).putString("recordDistance", config.distanceSource).apply()
    }
    fun beginRecording() {
        if (recording || saving) return
        activeConfig = config
        mapState.track.clear(); mapTrackEnabled = "GPS_POSITION" in activeConfig.fields
        ride.gps.reset(); ride.bike.reset(bikeData.value(LiveMetric.ODOMETER).takeIf { connection.isLive })
        heartSum = 0; heartCount = 0; pulse = pulse.copy(average = null, maximum = null)
        startElapsed = SystemClock.elapsedRealtime(); startUtc = System.currentTimeMillis()
        store.begin(startUtc, activeConfig); recording = true
        if (activeConfig.fields.any { it.startsWith("GPS_") } && hasGps()) setGpsEnabled(true)
        recordingStatus = "Aufzeichnung läuft"; appendSample(); updateActivity()
    }
    fun stopRecording() {
        if (!recording) return
        appendSample(); recording = false; saving = true; recordingStatus = "FIT wird gespeichert …"
        store.finish(); updateActivity()
    }
    private fun appendSample() {
        val sample = sample()
        store.append(sample)
        if (mapTrackEnabled) mapState.track.append(sample)
    }
    private fun sample(): RideSample {
        val now = SystemClock.elapsedRealtime(); val gps = gpsValues
        val fresh = LiveData(
            bikeData.speedHundredths.takeIf { fresh(LiveMetric.SPEED, now) }, bikeData.riderPowerWatts.takeIf { fresh(LiveMetric.RIDER_POWER, now) },
            bikeData.otherValues.filterKeys { fresh(it, now) }, bikeData.assistNames, bikeData.rangesKm.takeIf { connection.isLive },
        )
        val values = recordingValues(fresh, gps, ride.bike.distanceM, pulseValues)
        return RideSample(startUtc + now - startElapsed, now - startElapsed, gps.latitude, gps.longitude, values)
    }
    private fun fresh(metric: LiveMetric, now: Long): Boolean {
        val limit = if (metric.group == 2) 120_000 else if (metric.group == 1 || metric == LiveMetric.ASSIST_MODE) 60_000 else 5_000
        return connection.isLive && times[metric]?.let { now - it <= limit } == true
    }
    fun retryPending() { if (!recording && !saving) { saving = true; store.retryPending() } }
    override fun onStatus(message: String) { bikeStatus = message; scanStatus?.invoke(message); if (connection.isLive) bikeReconnect.connected(); notifyChanged() }
    override fun onDevice(device: BluetoothDevice, name: String) { scanFound?.invoke(device, name) }
    override fun onSamples(samples: List<McspSample>) { val now = SystemClock.elapsedRealtime(); samples.forEach { sample -> LiveMetric.forAddress(sample.address)?.let { times[it] = now } } }
    override fun onData(data: LiveData, packetReceived: Boolean) {
        bikeData = data
        if (packetReceived) { ride.bike.update(data.value(LiveMetric.ODOMETER)); lastPacketAt = SystemClock.elapsedRealtime() }
        else { times.clear(); ride.bike.pause(); lastPacketAt = null }
        notifyChanged()
    }
    companion object {
        private var instance: RideRuntime? = null
        fun get(context: Context): RideRuntime = instance?.takeIf { it.context === context.applicationContext } ?: RideRuntime(context.applicationContext).also { instance = it }
    }
}
