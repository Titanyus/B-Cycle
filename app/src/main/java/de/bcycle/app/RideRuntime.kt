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
    internal val bikes = BikeProfileStore(context, preferences)
    private var connectionsRequested = bikes.current().autoConnect
    private val disconnectedSensors = mutableSetOf<BikeSensor>()
    val themes = ThemeManager(preferences)
    val riderProfiles = RiderProfileStore(preferences)
    val diagnostics = DiagnosticLog()
    val ride = RideSession(gps = GpsRide(collecting = false))
    val connection = BoschConnection(context, this, diagnostics)
    val gpsSensor = GpsSensor(context, ride.gps) { status ->
        gpsStatus = status
        if (status == "GPS · Live") captureGpsPerformance()
        notifyChanged()
    }
    private val handler = Handler(Looper.getMainLooper())
    private val bikeReconnect = AutoReconnect()
    private val heartReconnect = AutoReconnect()
    private val trainerReconnect=AutoReconnect()
    private val phoneBattery=PhoneBattery(context,::notifyChanged)
    private val indoorRide=IndoorRide()
    internal val isIndoor get()=bikes.current().type==BikeType.INDOOR
    private val primaryPowerSource get()=if(isIndoor) PowerSource.FTMS else PowerSource.BOSCH_RIDER
    internal val trainer=TrainerConnection(context,::trainerChanged,::onTrainerMeasurement)
    internal val trainerEnabled get()=bikes.current().trainerEnabled
    internal val trainerAddress get()=bikes.current().trainerAddress
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
    var gpsEnabled = preferences.getBoolean("gpsEnabled", false) && !isIndoor; private set
    private var heartAt: Long? = null
    private var pulse = PulseValues()
    private val cadenceAverage = PedalingAverage()
    /** Derived display values never replace the sensor values used by the recording journal. */
    internal val batterySnapshot get() = BatterySnapshot(mapOf(
        BatterySource.EBIKE to currentBikeBattery?.toInt().takeUnless { isIndoor },
        BatterySource.HEART to pulse.battery.takeIf { heartEnabled && heart.live },
        BatterySource.TRAINER to trainer.battery.takeIf { isIndoor && trainer.live },
        BatterySource.PHONE to phoneBattery.level))
    val dashboardBikeData get() = bikeData.copy(indoor=isIndoor,batteries=batterySnapshot,otherValues = bikeData.otherValues - LiveMetric.AVERAGE_CADENCE - LiveMetric.BIKE_LIGHT +
        (if(isIndoor) indoorRide.summaries()+listOfNotNull(indoorRide.times(elapsedMs,ride.bike.distanceM).movingAverageKmh?.let { LiveMetric.AVERAGE_SPEED to kotlin.math.round(it*100).toLong() }).toMap() else emptyMap()) +
        (currentBikeLight?.let { mapOf(LiveMetric.BIKE_LIGHT to if (it) 1L else 0L) } ?: emptyMap()) +
        (cadenceAverage.value?.let { mapOf(LiveMetric.AVERAGE_CADENCE to kotlin.math.round(it * 2).toLong()) } ?: emptyMap()))
    private var heartSum = 0L; private var heartCount = 0
    /** Only the trend presentation uses this freshness mask; displayed/stored measurements stay intact. */
    internal val trendBikeData:LiveData get() {
        val now=SystemClock.elapsedRealtime();val data=dashboardBikeData
        return data.copy(speedHundredths=data.speedHundredths.takeIf { fresh(LiveMetric.SPEED,now) },
            riderPowerWatts=data.riderPowerWatts.takeIf { fresh(LiveMetric.RIDER_POWER,now) },
            otherValues=if(fresh(LiveMetric.CADENCE,now)) data.otherValues else data.otherValues-LiveMetric.CADENCE)
    }
    private var startElapsed = 0L
    private var startUtc = 0L
    private var completedElapsedMs: Long? = null
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
        PowerMeasurement(primaryPowerSource, it.value, it.receivedAtMs, it.sensorTimeMs, it.connected,
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
        if (recording) it.bpm?.let { bpm -> heartSum += bpm; heartCount++; pulse = pulse.copy(average = heartSum.toDouble() / heartCount, maximum = maxOf(pulse.maximum ?: bpm, bpm)) }
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
    private val useTrainerHeart get()=isIndoor && (!heartEnabled || heartAddress==null)
    private val heartAvailable get()=if(useTrainerHeart) trainer.live else heart.live
    val pulseValues get() = pulse.copy(bpm=if(useTrainerHeart) trainer.readings.value(TrainerField.HEART,SystemClock.elapsedRealtime())?.toInt()
        else pulse.bpm.takeIf { heartAt?.let { at -> SystemClock.elapsedRealtime()-at<=5000 }==true && heart.live })
    private val solarWeather = SolarWeather()
    val gpsValues get(): GpsValues {
        if(isIndoor) return indoorRide.times(elapsedMs,ride.bike.distanceM)
        val gps = ride.gps.snapshot(SystemClock.elapsedRealtime())
        return gps.copy(elapsedMs = elapsedMs, solar = solarWeather.snapshot(gps.latitude, gps.longitude, java.time.Instant.now(), java.time.ZoneId.systemDefault()))
    }
    /** Approximate coordinates stay display-only, never enter the recording/statistic values. */
    internal val mapGpsValues:GpsValues get() {
        val values=gpsValues
        if(isIndoor || !gpsEnabled) return values
        val fix=gpsSensor.mapFix(SystemClock.elapsedRealtime()) ?: return values.copy(accuracyM=null)
        return values.copy(latitude=fix.latitude,longitude=fix.longitude,accuracyM=fix.accuracyM)
    }
    /** Prefer fresh wheel speed; GPS already filters stationary jitter. Unknown is not zero. */
    val currentSpeedKmh: Double? get() {
        val now = SystemClock.elapsedRealtime()
        return bikeData.speedHundredths?.takeIf { fresh(LiveMetric.SPEED, now) }?.div(100.0)
            ?: ride.gps.snapshot(now).speedKmh
    }
    val isMoving: Boolean get() = currentSpeedKmh?.let { it > 0.0 } == true
    /** Reuse the existing connection/freshness rules; never expose a stale mode to the visual status. */
    val currentBikeLight: Boolean? get() = bikeData.value(LiveMetric.BIKE_LIGHT)
        ?.takeIf { it in 0..1 && fresh(LiveMetric.BIKE_LIGHT, SystemClock.elapsedRealtime()) }?.let { it == 1L }
    val currentBikeBattery: Long? get() = bikeData.value(LiveMetric.BATTERY)
        // Bosch sends battery changes, not a periodic heartbeat. Its connection buffer is
        // cleared on disconnect; retain a valid reading for this live session only.
        ?.takeIf { it in 0..100 && connection.isLive && !isIndoor }
    val currentAssistMode: Long? get() = bikeData.value(LiveMetric.ASSIST_MODE)
        .takeIf { fresh(LiveMetric.ASSIST_MODE, SystemClock.elapsedRealtime()) }
    internal fun trainingInputs(now: Long = SystemClock.elapsedRealtime()): TrainingInputs = TrainingInputs(
        bikeData.riderPowerWatts?.toDouble()?.takeIf { fresh(LiveMetric.RIDER_POWER, now) }, times[LiveMetric.RIDER_POWER],
        if(isIndoor) indoorRide.averagePower.takeIf { trainer.live } else bikeData.value(LiveMetric.AVERAGE_POWER)?.toDouble()?.takeIf { fresh(LiveMetric.AVERAGE_POWER, now) },
        pulseValues.bpm?.toDouble()?.takeIf { heartAvailable && heartAt?.let { now - it in 0..5000 } == true }, heartAt,
        pulse.average.takeIf { heartAvailable && heartAt?.let { now - it in 0..5000 } == true })
    private val storedConfig get()=RecordingConfig.decode(preferences.getStringSet("recordFields", null), preferences.getString("recordSpeed", null), preferences.getString("recordDistance", null))
    val config get() = storedConfig.let { if(isIndoor) IndoorFields.config(it) else it }.withGpsPosition(gpsEnabled)
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
            if(isIndoor) refreshTrainerData(SystemClock.elapsedRealtime())
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
    init { ActivityServiceUploads.all.forEach { runCatching { it.schedule(context) } }; IntervalsUploads.schedule(context); RouteUploads.schedule(context); diagnostics.reset("App=${AppVersion.name} Mode=MCSP Android=${android.os.Build.VERSION.SDK_INT} Device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}") }
    fun notifyChanged() { observers.toList().forEach { it() } }
    fun setVisible(value: Boolean) { visible = value; updateActivity() }
    private fun updateActivity() {
        bikeReconnect.foreground = active; heartReconnect.foreground = active;trainerReconnect.foreground=active
        handler.removeCallbacks(tick)
        if (active) {
            phoneBattery.start()
            if (!isIndoor && gpsEnabled && connectionsRequested && BikeSensor.GPS !in disconnectedSensors) gpsSensor.start()
            reconnectSensors(); scheduleTick()
        } else {
            phoneBattery.stop()
            connection.onForegroundChanged(false); trainer.stop();heart.stop(); gpsSensor.stop(); ride.bike.pause(); heartAt = null
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
        bikeReconnect.enabled = !isIndoor && bikeEnabled && connectionsRequested && BikeSensor.EBIKE !in disconnectedSensors
        heartReconnect.enabled = heartEnabled && connectionsRequested && BikeSensor.HEART !in disconnectedSensors
        bikeAddress?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }?.let {
            if (bikeReconnect.due(now, connection.isActive)) { bikeReconnect.attempted(now); connection.connect(adapter.getRemoteDevice(it)) }
        }
        trainerReconnect.enabled=isIndoor && trainerEnabled && connectionsRequested && BikeSensor.TRAINER !in disconnectedSensors
        trainerAddress?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }?.let {
            if(trainerReconnect.due(now,trainer.active)) { trainerReconnect.attempted(now);trainer.connect(adapter.getRemoteDevice(it)) }
        }
        heartAddress?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }?.let {
            if (heartReconnect.due(now, heart.active)) { heartReconnect.attempted(now); heart.connect(adapter.getRemoteDevice(it)) }
        }
    }
    fun selectBike(device: BluetoothDevice, name: String) {
        if (bikeAddress != device.address) { ride.bike.reset(); cadenceAverage.reset() }
        preferences.edit().putString("address", device.address).putString("name", name).putBoolean("bikeEnabled", true).apply()
        bikes.persistActive(); connectionsRequested = true; disconnectedSensors.remove(BikeSensor.EBIKE)
        bikeReconnect.selecting = false; connection.stop(); bikeReconnect.start(); reconnectSensors(); notifyChanged()
    }
    fun setBikeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("bikeEnabled", enabled).apply()
        bikes.persistActive()
        if (!enabled) connection.stop() else { connectionsRequested = true; disconnectedSensors.remove(BikeSensor.EBIKE); bikeReconnect.start() }
        reconnectSensors(); notifyChanged()
    }
    fun selectHeart(device: BluetoothDevice, name: String) {
        preferences.edit().putString("heartAddress", device.address).putString("heartName", name).putBoolean("heartEnabled", true).apply()
        bikes.persistActive(); connectionsRequested = true; disconnectedSensors.remove(BikeSensor.HEART)
        heart.stop(); pulse = PulseValues(); heartAt = null; heartReconnect.start(); reconnectSensors(); notifyChanged()
    }
    fun setHeartEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("heartEnabled", enabled).apply()
        bikes.persistActive()
        if (enabled) { connectionsRequested = true; disconnectedSensors.remove(BikeSensor.HEART) }
        if (!enabled) { heart.stop(); heartAt = null; pulse = pulse.copy(bpm = null); heartStatus = "Pulsmesser · Getrennt" } else heartReconnect.start()
        reconnectSensors(); notifyChanged()
    }
    fun setGpsEnabled(enabled: Boolean) {
        if(isIndoor) return
        gpsEnabled = enabled; preferences.edit().putBoolean("gpsEnabled", enabled).apply(); bikes.persistActive()
        if (enabled) { connectionsRequested = true; disconnectedSensors.remove(BikeSensor.GPS) }
        if (!enabled) { gpsSensor.stop(); gpsStatus = "GPS · Aus"; if ((if (recording) activeConfig else config).speedSource == "GPS_SPEED") capturePerformance(PerformanceChannel.SPEED, null, "GPS_SPEED") } else if (active) gpsSensor.start()
        notifyChanged()
    }
    internal var profileRevision=0;private set
    internal fun copyProfile(sourceSlot:Int):Boolean {
        if(recording || saving) return false
        val target=bikes.current()
        val source=bikes.all().firstOrNull { it.slot==sourceSlot && it.slot!=target.slot } ?: return false
        bikes.ui.ensure(bikes.all().map { it.slot });bikes.ui.save(target.slot)
        activateBike {
            bikes.save(source.copy(slot=target.slot,name=target.name,
                bikeAddress=target.bikeAddress,bikeName=target.bikeName,
                heartAddress=target.heartAddress,heartName=target.heartName,
                trainerAddress=target.trainerAddress,trainerName=target.trainerName))
            bikes.ui.copy(sourceSlot,target.slot)
        }
        return true
    }
    internal fun switchBike(slot: Int): Boolean {
        if (slot == bikes.activeSlot) return true
        if (recording || saving) return false
        activateBike { bikes.select(slot) }
        return true
    }
    internal fun changeBikeType(type:BikeType):Boolean {
        if(recording || saving || !type.available) return false
        if(type==bikes.current().type) return true
        activateBike { bikes.save(bikes.current().copy(type=type)) }
        return true
    }
    private fun activateBike(change:()->Unit) {
        connectionsRequested = false
        connection.stop();trainer.stop(); heart.stop(); gpsSensor.stop()
        change()
        profileRevision++
        themes.reloadFromPreferences()
        mapState.restoreProfileRoute()
        connectionsRequested = bikes.current().autoConnect; disconnectedSensors.clear()
        gpsEnabled = bikes.current().gpsEnabled && !isIndoor
        bikeData = LiveData(indoor=isIndoor); pulse = PulseValues(); heartAt = null; times.clear()
        latestPerformance.clear(); pendingPerformance.clear(); mainBikeSpeedSeen = false; lastPacketAt = null
        ride.gps.reset(); ride.gps.setCollecting(false); ride.bike.reset(); cadenceAverage.reset();indoorRide.reset()
        heartSum = 0; heartCount = 0; completedElapsedMs = null
        bikeStatus = context.getString(R.string.bikes_disconnected)
        heartStatus = context.getString(R.string.bikes_disconnected)
        gpsStatus = context.getString(R.string.bikes_gps_disconnect)
        bikeReconnect.connected();trainerReconnect.connected(); heartReconnect.connected()
        updateActivity()
    }
    internal fun addBike(): Boolean {
        if (recording || saving) return false
        val profile = bikes.add() ?: return false
        return switchBike(profile.slot)
    }
    internal fun deleteCurrentBike(expectedSlot: Int): Boolean {
        if (recording || saving || bikes.activeSlot != expectedSlot) return false
        val rows = bikes.all(); if (rows.size <= 1) return false
        val index = rows.indexOfFirst { it.slot == expectedSlot }
        val next = rows[if (index == rows.lastIndex) index - 1 else index + 1]
        if (!switchBike(next.slot) || !bikes.remove(expectedSlot)) return false
        BikePhotos.worker.execute { BikePhotos.delete(context, expectedSlot) }
        notifyChanged()
        return true
    }
    internal fun moveBike(slot: Int, position: Int): Boolean {
        if (recording || saving) return false
        return bikes.move(slot, position).also { if (it) notifyChanged() }
    }
    internal fun setAutoConnect(enabled: Boolean) {
        bikes.save(bikes.current().copy(autoConnect = enabled))
        if (enabled) connectAssignedSensors() else notifyChanged()
    }
    internal fun connectAssignedSensors() {
        connectionsRequested = true; disconnectedSensors.clear()
        bikeReconnect.connected();trainerReconnect.connected(); heartReconnect.connected(); updateActivity()
    }
    internal fun disconnected(sensor: BikeSensor) = sensor in disconnectedSensors
    internal fun receivedValueCount(sensor: BikeSensor): Int? = if (disconnected(sensor)) null else when (sensor) {
        BikeSensor.EBIKE -> connection.receivedValueCount.takeIf { connection.isLive }
        BikeSensor.HEART -> heart.receivedValueCount.takeIf { heart.live }
        BikeSensor.TRAINER -> trainer.receivedValueCount.takeIf { trainer.live }
        else -> null
    }
    internal fun disconnectSensor(sensor: BikeSensor) {
        disconnectedSensors += sensor
        when (sensor) {
            BikeSensor.EBIKE -> connection.stop()
            BikeSensor.HEART -> { heart.stop(); heartAt = null; pulse = pulse.copy(bpm = null); capturePerformance(PerformanceChannel.HEART_RATE, null, "HEART_RATE") }
            BikeSensor.GPS -> { gpsSensor.stop(); gpsStatus = context.getString(R.string.bikes_gps_disconnect); capturePerformance(PerformanceChannel.SPEED, null, "GPS_SPEED") }
            BikeSensor.TRAINER -> { trainer.stop();trainerChanged() }
            BikeSensor.POWER -> Unit
        }
        notifyChanged()
    }
    internal fun removeSensor(sensor: BikeSensor) {
        disconnectSensor(sensor)
        val p = bikes.current()
        when (sensor) {
            BikeSensor.EBIKE -> bikes.save(p.copy(bikeAddress = null, bikeName = null))
            BikeSensor.HEART -> bikes.save(p.copy(heartAddress = null, heartName = null))
            BikeSensor.TRAINER -> bikes.save(p.copy(trainerAddress=null,trainerName=null))
            else -> Unit
        }
        notifyChanged()
    }
    internal fun selectTrainer(device:BluetoothDevice,name:String) {
        if(!isIndoor) return
        bikes.save(bikes.current().copy(trainerAddress=device.address,trainerName=name,trainerEnabled=true))
        connectionsRequested=true;disconnectedSensors.remove(BikeSensor.TRAINER)
        trainer.stop();trainerReconnect.start();reconnectSensors();notifyChanged()
    }
    internal fun setTrainerEnabled(enabled:Boolean) {
        bikes.save(bikes.current().copy(trainerEnabled=enabled))
        if(!enabled) { trainer.stop();trainerChanged() }
        else { connectionsRequested=true;disconnectedSensors.remove(BikeSensor.TRAINER);trainerReconnect.start() }
        reconnectSensors();notifyChanged()
    }
    internal fun setIndoorCorrection(correction: IndoorCorrection): Boolean {
        if (!isIndoor || recording || saving) return false
        bikes.save(bikes.current().copy(indoorCorrection = correction))
        val now = SystemClock.elapsedRealtime()
        refreshTrainerData(now)
        // Re-seed the existing performance input with corrected values and their original receipt times.
        listOf(TrainerField.POWER, TrainerField.SPEED).forEach { field ->
            val raw = trainer.readings.value(field,now).takeIf { trainer.live }
            val value = correction.apply(field,raw)
            val channel = if (field == TrainerField.POWER) PerformanceChannel.POWER else PerformanceChannel.SPEED
            capturePerformance(channel, if (field == TrainerField.SPEED) value?.div(3.6) else value,
                if (field == TrainerField.POWER) PowerSource.FTMS.name else "SPEED", trainer.readings.at(field) ?: now)
        }
        notifyChanged()
        return true
    }
    private fun refreshTrainerData(now:Long) {
        if(!isIndoor) return
        val correction = bikes.current().indoorCorrection
        fun v(f:TrainerField)=correction.apply(f,trainer.readings.value(f,now))
        bikeData=LiveData(v(TrainerField.SPEED)?.times(100)?.let { kotlin.math.round(it).toInt() },v(TrainerField.POWER)?.toInt(),
            listOfNotNull(v(TrainerField.CADENCE)?.let { LiveMetric.CADENCE to kotlin.math.round(it*2).toLong() },
                v(TrainerField.DISTANCE)?.let { LiveMetric.ODOMETER to it.toLong() }).toMap(),indoor=true)
    }
    private fun trainerChanged() {
        if(!isIndoor) return
        refreshTrainerData(SystemClock.elapsedRealtime())
        if(trainer.live) trainerReconnect.connected()
        if(!trainer.active) {
            lastPacketAt=null;times.clear();ride.bike.pause()
            capturePerformance(PerformanceChannel.POWER,null,PowerSource.FTMS.name)
            capturePerformance(PerformanceChannel.SPEED,null,"SPEED")
        }
        notifyChanged()
    }
    internal fun onTrainerMeasurement(frame:TrainerMeasurement,now:Long) {
        if(!isIndoor) return
        val metrics=mapOf(TrainerField.SPEED to LiveMetric.SPEED,TrainerField.POWER to LiveMetric.RIDER_POWER,
            TrainerField.CADENCE to LiveMetric.CADENCE,TrainerField.DISTANCE to LiveMetric.ODOMETER)
        val correction = bikes.current().indoorCorrection
        frame.values.forEach { (field,raw) ->
            val value = correction.apply(field,raw)
            metrics[field]?.let { if(value==null) times.remove(it) else times[it]=now }
            when(field) {
                TrainerField.SPEED -> capturePerformance(PerformanceChannel.SPEED,value?.div(3.6),"SPEED",now)
                TrainerField.POWER -> capturePerformance(PerformanceChannel.POWER,value,PowerSource.FTMS.name,now)
                TrainerField.CADENCE -> if(recording && value!=null) cadenceAverage.add(value)
                TrainerField.DISTANCE -> if(recording) { if(value!=null) ride.bike.update(value.toLong()) else ride.bike.pause() }
                TrainerField.HEART -> if(useTrainerHeart) {
                    heartAt=if(value!=null) now else null
                    capturePerformance(PerformanceChannel.HEART_RATE,value,"HEART_RATE",now)
                    if(recording && value!=null) { val bpm=value.toInt();heartSum+=bpm;heartCount++;pulse=pulse.copy(average=heartSum.toDouble()/heartCount,maximum=maxOf(pulse.maximum ?: bpm,bpm)) }
                }
                else -> Unit
            }
        }
        refreshTrainerData(now)
        lastPacketAt=now
    }
    fun beginBikeScan() { bikeReconnect.selecting = true; connection.discover() }
    fun endBikeScan() { bikeReconnect.selecting = false; connection.stop(); bikeReconnect.connected(); reconnectSensors() }
    fun saveConfig(config: RecordingConfig) {
        // Indoor is a view of the stored selection, not a destructive replacement of outdoor settings.
        val config=if(isIndoor) storedConfig.let { stored -> stored.copy(
            fields=(stored.fields-IndoorFields.allowed)+(config.fields intersect IndoorFields.allowed)) } else config
        preferences.edit().putStringSet("recordFields", config.fields).putString("recordSpeed", config.speedSource).putString("recordDistance", config.distanceSource).apply()
        bikes.persistActive()
    }
    fun beginRecording() {
        if (recording || saving) return
        // Reserve the coordinate columns outdoors even if GPS is enabled later during this ride.
        // Actual coordinate values below remain strictly controlled by the GPS sensor switch.
        activeConfig = config.withGpsPosition(!isIndoor)
        mapState.track.clear(); mapTrackEnabled = "GPS_POSITION" in activeConfig.fields
        ride.gps.reset(); ride.bike.reset(bikeData.value(LiveMetric.ODOMETER).takeIf { if(isIndoor) trainer.live else connection.isLive })
        cadenceAverage.reset();indoorRide.reset()
        heartSum = 0; heartCount = 0; pulse = pulse.copy(average = null, maximum = null)
        startElapsed = SystemClock.elapsedRealtime(); startUtc = System.currentTimeMillis()
        completedElapsedMs = null
        performance = PerformanceEngine(primaryPowerSource=primaryPowerSource); pendingPerformance.clear(); performanceOverflow = false
        graphContinuedZeroMs=0; liveGraphs.begin()
        if (activeConfig.speedSource == "GPS_SPEED") captureGpsPerformance()
        listOf(PerformanceChannel.POWER, PerformanceChannel.HEART_RATE, PerformanceChannel.SPEED, PerformanceChannel.BIKE_ACTIVITY).forEach { channel ->
            val event = latestPerformance[channel]
            val connected = when (channel) {
                PerformanceChannel.POWER -> if(isIndoor) trainer.live else connection.isLive
                PerformanceChannel.HEART_RATE -> heartAvailable
                PerformanceChannel.BIKE_ACTIVITY -> connection.isLive
                else -> if(isIndoor) trainer.live else if (activeConfig.speedSource == "GPS_SPEED") gpsSensor.active else connection.isLive
            }
            if (event != null && connected && (channel != PerformanceChannel.SPEED || event.source == activeConfig.speedSource))
                pendingPerformance += event.copy(atMs = 0, receivedAtMs = event.receivedAtMs - startElapsed,
                    sensorTimeMs = event.sensorTimeMs?.minus(startElapsed))
        }
        store.begin(startUtc, activeConfig); recording = true; ride.gps.setCollecting(!isIndoor)
        connectionsRequested = true
        recordingStatus = "Aufzeichnung läuft"; appendSample(); updateActivity()
    }
    fun stopRecording() {
        if (!recording) return
        completedElapsedMs = appendSample(); recording = false; saving = true; recordingStatus = "FIT wird gespeichert …"
        ride.gps.setCollecting(false); ride.bike.pause()
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
        val now = SystemClock.elapsedRealtime()
        if(isIndoor && recording) indoorRide.add(now-startElapsed,
            bikeData.speedHundredths?.div(100.0).takeIf { fresh(LiveMetric.SPEED,now) },
            bikeData.riderPowerWatts?.toDouble().takeIf { fresh(LiveMetric.RIDER_POWER,now) },
            bikeData.value(LiveMetric.CADENCE)?.div(2.0).takeIf { fresh(LiveMetric.CADENCE,now) })
        val gps = gpsValues
        val fresh = LiveData(
            bikeData.speedHundredths.takeIf { fresh(LiveMetric.SPEED, now) }, bikeData.riderPowerWatts.takeIf { fresh(LiveMetric.RIDER_POWER, now) },
            bikeData.otherValues.filterKeys { fresh(it, now) }+(if(isIndoor) dashboardBikeData.otherValues.filterKeys { it.group==2 } else emptyMap()), bikeData.assistNames, bikeData.rangesKm.takeIf { connection.isLive },
        )
        val values = recordingValues(fresh, gps, ride.bike.distanceM, pulseValues)
        return RideSample(startUtc + now - startElapsed, now - startElapsed, gps.latitude.takeIf { gpsEnabled && !isIndoor }, gps.longitude.takeIf { gpsEnabled && !isIndoor }, values, pendingPerformance.toList())
    }
    private fun fresh(metric: LiveMetric, now: Long): Boolean {
        val limit = if (metric.group == 2) 120_000 else if (metric.group == 1 || metric == LiveMetric.ASSIST_MODE) 60_000 else 5_000
        if(isIndoor) return trainer.live && times[metric]?.let { now-it in 0..5000 }==true
        return connection.isLive && times[metric]?.let { now - it <= limit } == true
    }
    fun retryPending() { if (!recording && !saving) { saving = true; store.retryPending() } }
    override fun onStatus(message: String) { bikeStatus = message; scanStatus?.invoke(message); if (connection.isLive) bikeReconnect.connected(); notifyChanged() }
    override fun onDevice(device: BluetoothDevice, name: String) { scanFound?.invoke(device, name) }
    override fun onSamples(samples: List<McspSample>) {
        if(isIndoor) return
        val now = SystemClock.elapsedRealtime()
        samples.forEach { sample -> LiveMetric.forAddress(sample.address)?.let {
            times[it] = now
            if (recording && it == LiveMetric.CADENCE && sample.value in 0..it.maxRaw) cadenceAverage.add(sample.value / it.divisor)
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
        if(isIndoor) return
        bikeData = data
        if (packetReceived) { if (recording) ride.bike.update(data.value(LiveMetric.ODOMETER)); lastPacketAt = SystemClock.elapsedRealtime() }
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
