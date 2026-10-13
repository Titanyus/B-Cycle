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
    val themes = ThemeManager(preferences)
    val riderProfiles = RiderProfileStore(preferences)
    val diagnostics = DiagnosticLog()
    val ride = RideSession()
    val connection = BoschConnection(context, this, diagnostics)
    val gpsSensor = GpsSensor(context, ride.gps) { status ->
        gpsStatus = status
        if (status == "GPS · Live") captureGpsPerformance()
        notifyChanged()
    }
    private val handler = Handler(Looper.getMainLooper())
    private val bikeReconnect = AutoReconnect()
    private val heartReconnect = AutoReconnect()
    private val times = mutableMapOf<LiveMetric, Long>()
    val observers = linkedSetOf<() -> Unit>()
    internal val liveGraphs = LiveGraphProvider { if (visible) notifyChanged() }
    private var graphContinuedZeroMs = 0L
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
    private val cadenceAverage = PedalingAverage()
    /** Derived display values never replace the sensor values used by the recording journal. */
    val dashboardBikeData get() = bikeData.copy(otherValues = bikeData.otherValues - LiveMetric.AVERAGE_CADENCE - LiveMetric.BIKE_LIGHT +
        (currentBikeLight?.let { mapOf(LiveMetric.BIKE_LIGHT to if (it) 1L else 0L) } ?: emptyMap()) +
        (cadenceAverage.value?.let { mapOf(LiveMetric.AVERAGE_CADENCE to kotlin.math.round(it * 2).toLong()) } ?: emptyMap()))
    private var heartSum = 0L; private var heartCount = 0
    private var startElapsed = 0L
    private var startUtc = 0L
    private var completedElapsedMs: Long? = null
    private var hasRecordingClock = false
    private var nextTickUptime = 0L
    private var lastSlowTickLoggedAt = -5000L
    private var activeConfig = RecordingConfig()
    var performance = PerformanceEngine(); private set
    val performanceValues get() = performance.snapshot()
    private val latestPerformance = mutableMapOf<PerformanceChannel, PerformanceEvent>()
    private val pendingPerformance = ArrayList<PerformanceEvent>()
    private var performanceOverflow = false
    private var mainBikeSpeedSeen = false
    val powerMeasurement: PowerMeasurement? get() = latestPerformance[PerformanceChannel.POWER]?.let {
        PowerMeasurement(PowerSource.BOSCH_RIDER, it.value, it.receivedAtMs, it.sensorTimeMs, it.connected,
            it.connected && it.value != null && SystemClock.elapsedRealtime() - it.receivedAtMs <= PerformanceEngine.MAX_HOLD_MS)
    }
    private fun capturePerformance(channel: PerformanceChannel, value: Double?, source: String,
                                   now: Long = SystemClock.elapsedRealtime(), sensorTime: Long? = null) {
        val event = PerformanceEvent(channel, now, value, now, sensorTime, source, value != null)
        latestPerformance[channel] = event
        if (!recording || performanceOverflow) return
        if (pendingPerformance.size >= 256) {
            val gapAt = pendingPerformance.first().atMs
            pendingPerformance.clear()
            listOf(PerformanceChannel.POWER, PerformanceChannel.HEART_RATE, PerformanceChannel.SPEED, PerformanceChannel.BIKE_ACTIVITY).forEach {
                pendingPerformance += PerformanceEvent(it, gapAt, connected = false)
            }
            performanceOverflow = true
            diagnostics.event("PERFORMANCE_INPUT_GAP bounded event batch exceeded")
        } else pendingPerformance += event.copy(atMs = now - startElapsed, receivedAtMs = now - startElapsed,
            sensorTimeMs = sensorTime?.minus(startElapsed))
    }
    private fun captureGpsPerformance() {
        val chosen = if (recording) activeConfig else config
        if (chosen.speedSource != "GPS_SPEED") return
        val now = SystemClock.elapsedRealtime()
        val time = ride.gps.lastObservationTimeMs ?: return
        if (latestPerformance[PerformanceChannel.SPEED]?.sensorTimeMs == time) return
        capturePerformance(PerformanceChannel.SPEED, ride.gps.snapshot(now).speedKmh?.div(3.6), "GPS_SPEED", now, time)
    }
    val mapState = MapState(context) { notifyChanged() }
    var mapTrackEnabled = true; private set
    private val store = RecordingStore(context) { text, busy ->
        saving = busy; recordingStatus = text
        if (!busy && (text.startsWith("Gespeichert:") || text.startsWith("FIT nicht") || text.startsWith("FIT lokal gespeichert") || text.contains("Fahrten gespeichert"))) mapState.refreshLibrary()
        notifyChanged()
    }
    val heart: HeartRateConnection = HeartRateConnection(context, { heartStatus = it; if (heartIsLive()) heartReconnect.connected(); if (!heartActive()) { pulse = pulse.copy(bpm = null); heartAt = null; capturePerformance(PerformanceChannel.HEART_RATE, null, "HEART_RATE") }; notifyChanged() }, {
        heartAt = SystemClock.elapsedRealtime()
        capturePerformance(PerformanceChannel.HEART_RATE, it.bpm?.toDouble(), "HEART_RATE")
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
    val elapsedMs get() = if (recording) SystemClock.elapsedRealtime() - startElapsed else completedElapsedMs ?: 0L
    val pulseValues get() = pulse.copy(bpm = pulse.bpm.takeIf { heartAt?.let { at -> SystemClock.elapsedRealtime() - at <= 5_000 } == true && heart.live })
    private val solarWeather = SolarWeather()
    val gpsValues get(): GpsValues {
        val gps = ride.gps.snapshot(SystemClock.elapsedRealtime())
        return gps.copy(elapsedMs = if (hasRecordingClock) elapsedMs else gps.elapsedMs, solar = solarWeather.snapshot(gps.latitude, gps.longitude, java.time.Instant.now(), java.time.ZoneId.systemDefault()))
    }
    /** Prefer fresh wheel speed; GPS already filters stationary jitter. Unknown is not zero. */
    val currentSpeedKmh: Double? get() {
        val now = SystemClock.elapsedRealtime()
        return bikeData.speedHundredths?.takeIf { fresh(LiveMetric.SPEED, now) }?.div(100.0)
            ?: ride.gps.snapshot(now).speedKmh
    }
    val isMoving: Boolean get() = currentSpeedKmh?.let { it > 0.0 } == true
    val canEditDataFields: Boolean get() = currentSpeedKmh?.let { it == 0.0 } ?: !recording
    /** Reuse the existing connection/freshness rules; never expose a stale mode to the visual status. */
    val currentBikeLight: Boolean? get() = bikeData.value(LiveMetric.BIKE_LIGHT)
        ?.takeIf { it in 0..1 && fresh(LiveMetric.BIKE_LIGHT, SystemClock.elapsedRealtime()) }?.let { it == 1L }
    val currentBikeBattery: Long? get() = bikeData.value(LiveMetric.BATTERY)
        ?.takeIf { it in 0..100 && fresh(LiveMetric.BATTERY, SystemClock.elapsedRealtime()) }
    val currentAssistMode: Long? get() = bikeData.value(LiveMetric.ASSIST_MODE)
        .takeIf { fresh(LiveMetric.ASSIST_MODE, SystemClock.elapsedRealtime()) }
    val config get() = RecordingConfig.decode(preferences.getStringSet("recordFields", null), preferences.getString("recordSpeed", null), preferences.getString("recordDistance", null))
    private fun scheduleTick() {
        handler.removeCallbacks(tick)
        if (!active) return
        val now = SystemClock.elapsedRealtime()
        val phase = if (recording) now - startElapsed else now
        nextTickUptime = SystemClock.uptimeMillis() + 1000 - phase % 1000
        handler.postAtTime(tick, nextTickUptime)
    }
    private val tick = object : Runnable {
        override fun run() {
            if (!active) return
            val entered = SystemClock.uptimeMillis()
            val lateMs = (entered - nextTickUptime).coerceAtLeast(0)
            reconnectSensors()
            val captureStarted = SystemClock.uptimeMillis()
            if (recording) appendSample()
            val captureMs = SystemClock.uptimeMillis() - captureStarted
            val uiStarted = SystemClock.uptimeMillis()
            notifyChanged()
            val uiMs = SystemClock.uptimeMillis() - uiStarted
            val finished = SystemClock.uptimeMillis()
            if ((lateMs >= 150 || finished - entered >= 150) && finished - lastSlowTickLoggedAt >= 5000) {
                lastSlowTickLoggedAt = finished
                diagnostics.event("RUNTIME_TICK_SLOW lateMs=$lateMs captureMs=$captureMs uiMs=$uiMs recording=$recording")
            }
            // Skip missed boundaries; never queue a burst of artificial catch-up samples.
            scheduleTick()
        }
    }
    init { RouteUploads.schedule(context); diagnostics.reset("App=${AppVersion.name} Mode=MCSP Android=${android.os.Build.VERSION.SDK_INT} Device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}") }
    fun notifyChanged() { observers.toList().forEach { it() } }
    fun setVisible(value: Boolean) { visible = value; updateActivity() }
    private fun updateActivity() {
        bikeReconnect.foreground = active; heartReconnect.foreground = active
        handler.removeCallbacks(tick)
        if (active) {
            if (gpsEnabled) gpsSensor.start()
            reconnectSensors(); scheduleTick()
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
        if (bikeAddress != device.address) { ride.bike.reset(); cadenceAverage.reset() }
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
        if (!enabled) { gpsSensor.stop(); gpsStatus = "GPS · Aus"; if ((if (recording) activeConfig else config).speedSource == "GPS_SPEED") capturePerformance(PerformanceChannel.SPEED, null, "GPS_SPEED") } else if (active) gpsSensor.start()
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
        cadenceAverage.reset()
        heartSum = 0; heartCount = 0; pulse = pulse.copy(average = null, maximum = null)
        startElapsed = SystemClock.elapsedRealtime(); startUtc = System.currentTimeMillis()
        completedElapsedMs = null; hasRecordingClock = true
        performance = PerformanceEngine(); pendingPerformance.clear(); performanceOverflow = false
        graphContinuedZeroMs=0; liveGraphs.begin()
        if (activeConfig.speedSource == "GPS_SPEED") captureGpsPerformance()
        listOf(PerformanceChannel.POWER, PerformanceChannel.HEART_RATE, PerformanceChannel.SPEED, PerformanceChannel.BIKE_ACTIVITY).forEach { channel ->
            val event = latestPerformance[channel]
            val connected = when (channel) {
                PerformanceChannel.POWER -> connection.isLive
                PerformanceChannel.HEART_RATE -> heart.live
                PerformanceChannel.BIKE_ACTIVITY -> connection.isLive
                else -> if (activeConfig.speedSource == "GPS_SPEED") gpsSensor.active else connection.isLive
            }
            if (event != null && connected && (channel != PerformanceChannel.SPEED || event.source == activeConfig.speedSource))
                pendingPerformance += event.copy(atMs = 0, receivedAtMs = event.receivedAtMs - startElapsed,
                    sensorTimeMs = event.sensorTimeMs?.minus(startElapsed))
        }
        store.begin(startUtc, activeConfig); recording = true
        if (activeConfig.needsGps && hasGps()) setGpsEnabled(true)
        recordingStatus = "Aufzeichnung läuft"; appendSample(); updateActivity()
    }
    fun stopRecording() {
        if (!recording) return
        completedElapsedMs = appendSample(); recording = false; saving = true; recordingStatus = "FIT wird gespeichert …"
        liveGraphs.stop(); store.finish(); updateActivity()
    }
    private fun appendSample(): Long {
        val sample = sample()
        sample.performanceEvents.forEach(performance::accept)
        performance.advance(sample.elapsedMs)
        val continued=performance.snapshot().quality.continuedZeroMs
        liveGraphs.append(graphPoint(sample,activeConfig,continued > graphContinuedZeroMs,
            connection.isLive && bikeData.value(LiveMetric.CADENCE)==0L))
        graphContinuedZeroMs=continued
        pendingPerformance.clear(); performanceOverflow = false
        store.append(sample)
        if (mapTrackEnabled) mapState.track.append(sample)
        return sample.elapsedMs
    }
    private fun sample(): RideSample {
        val now = SystemClock.elapsedRealtime(); val gps = gpsValues
        val fresh = LiveData(
            bikeData.speedHundredths.takeIf { fresh(LiveMetric.SPEED, now) }, bikeData.riderPowerWatts.takeIf { fresh(LiveMetric.RIDER_POWER, now) },
            bikeData.otherValues.filterKeys { fresh(it, now) }, bikeData.assistNames, bikeData.rangesKm.takeIf { connection.isLive },
        )
        val values = recordingValues(fresh, gps, ride.bike.distanceM, pulseValues)
        return RideSample(startUtc + now - startElapsed, now - startElapsed, gps.latitude, gps.longitude, values, pendingPerformance.toList())
    }
    private fun fresh(metric: LiveMetric, now: Long): Boolean {
        val limit = if (metric.group == 2) 120_000 else if (metric.group == 1 || metric == LiveMetric.ASSIST_MODE) 60_000 else 5_000
        return connection.isLive && times[metric]?.let { now - it <= limit } == true
    }
    fun retryPending() { if (!recording && !saving) { saving = true; store.retryPending() } }
    override fun onStatus(message: String) { bikeStatus = message; scanStatus?.invoke(message); if (connection.isLive) bikeReconnect.connected(); notifyChanged() }
    override fun onDevice(device: BluetoothDevice, name: String) { scanFound?.invoke(device, name) }
    override fun onSamples(samples: List<McspSample>) {
        val now = SystemClock.elapsedRealtime()
        samples.forEach { sample -> LiveMetric.forAddress(sample.address)?.let {
            times[it] = now
            if (it == LiveMetric.CADENCE && sample.value in 0..it.maxRaw) cadenceAverage.add(sample.value / it.divisor)
            if (it == LiveMetric.SPEED) {
                if (sample.address == LiveMetric.SPEED.address) mainBikeSpeedSeen = true
                if (sample.address == LiveMetric.SPEED.address || !mainBikeSpeedSeen)
                    capturePerformance(PerformanceChannel.BIKE_ACTIVITY, sample.value / 360.0, "SPEED", now)
            }
            if (it == LiveMetric.RIDER_POWER) capturePerformance(PerformanceChannel.POWER, sample.value.toDouble(), PowerSource.BOSCH_RIDER.name, now)
            val chosen = if (recording) activeConfig else config
            if (it == LiveMetric.SPEED && chosen.speedSource == "SPEED") {
                if (sample.address == LiveMetric.SPEED.address || !mainBikeSpeedSeen)
                    capturePerformance(PerformanceChannel.SPEED, sample.value / 360.0, "SPEED", now)
            }
        } }
    }
    override fun onData(data: LiveData, packetReceived: Boolean) {
        bikeData = data
        if (packetReceived) { ride.bike.update(data.value(LiveMetric.ODOMETER)); lastPacketAt = SystemClock.elapsedRealtime() }
        else {
            times.clear(); ride.bike.pause(); lastPacketAt = null; mainBikeSpeedSeen = false
            capturePerformance(PerformanceChannel.POWER, null, PowerSource.BOSCH_RIDER.name)
            capturePerformance(PerformanceChannel.BIKE_ACTIVITY, null, "SPEED")
            val chosen = if (recording) activeConfig else config
            if (chosen.speedSource == "SPEED") capturePerformance(PerformanceChannel.SPEED, null, "SPEED")
        }
        notifyChanged()
    }
    companion object {
        private var instance: RideRuntime? = null
        fun get(context: Context): RideRuntime = instance?.takeIf { it.context === context.applicationContext } ?: RideRuntime(context.applicationContext).also { instance = it }
    }
}
