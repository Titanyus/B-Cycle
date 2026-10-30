package de.bcycle.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Activity and recording service share existing sensors; no second Bosch client is created. */
class RideRuntime private constructor(val context: Context) : BoschConnection.Listener {
    val preferences = context.getSharedPreferences("bike", Context.MODE_PRIVATE)
    internal val bikes = BikeProfileStore(context, preferences)
    private var connectionsRequested = bikes.current().autoConnect
    private var autoStartWasConnected=false
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
    private val powerReconnect=AutoReconnect()
    private val phoneBattery=PhoneBattery(context,::notifyChanged)
    private val indoorRide=IndoorRide()
    private val trainerDistance=TrainerDistance()
    internal val barometer=PhoneBarometer(context)
    private var primaryDistance=SourceDistance()
    private var primaryElevation=RouteElevation()
    private var primaryHeightSource:RecordingSource?=null
    private var primaryHeightLast:Double?=null
    private var primaryHeightOffset=0.0
    private var primaryHeightDatum=false
    private var primarySlope:Pair<Double,Double>?=null
    private val rawSpeeds=mutableMapOf<RecordingSource,PerformanceEvent>()
    private var selectedSpeedSource:RecordingSource?=null
    private val rawPowers=mutableMapOf<RecordingSource,PerformanceEvent>()
    private val rawCadences=mutableMapOf<RecordingSource,SourceReading>()
    private var selectedPowerSource:RecordingSource?=null
    private var lastCadenceSample:Pair<RecordingSource,Long>?=null
    internal val isIndoor get()=bikes.current().type==BikeType.INDOOR
    private val primaryPowerSource get()=if(isIndoor) PowerSource.FTMS else PowerSource.BOSCH_RIDER
    internal val trainer=TrainerConnection(context,::trainerChanged,::onTrainerMeasurement)
    internal val powerMeter=CyclingPowerConnection(context,::powerMeterChanged,::onPowerMeterMeasurement)
    internal val shiftGroup=AxsBatteryGroup(context,::notifyChanged)
    internal val shiftEnabled get()=bikes.current().shiftEnabled
    internal val powerEnabled get()=bikes.current().powerEnabled
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
    internal val drinkReminder=DrinkReminder()
    internal val shiftBatteryWarning=ShiftBatteryWarning()
    var recording = false; private set
    var saving = false; private set
    internal var recordingUpdate: RecordingUpdate? = null; private set
    internal var pendingRideSummary: RideCompletion? = null; private set
    internal fun takeRideSummary():RideCompletion? = pendingRideSummary.also { pendingRideSummary=null }
    val recordingStatus get() = recordingUpdate?.let { RecordingStatusText.text(context, it) }
        ?: context.getString(if (recording) R.string.recording_running else R.string.recording_ready)
    var visible = false; private set
    var gpsEnabled = preferences.getBoolean("gpsEnabled", false) && !isIndoor; private set
    private var heartAt: Long? = null
    private var pulse = PulseValues()
    private val cadenceAverage = PedalingAverage()
    /** First paired SRAM component is the currently supported derailleur; never combine other batteries. */
    internal val derailleurBattery:AxsBattery? get() = bikes.current().takeIf { it.type==BikeType.EBIKE && it.shiftEnabled }
        ?.shiftDevices?.firstOrNull()?.let { shiftGroup.battery(it.address) }
    /** Derived display values never replace the sensor values used by the recording journal. */
    internal val batterySnapshot get() = BatterySnapshot(mapOf(
        BatterySource.EBIKE to currentBikeBattery?.toInt().takeUnless { isIndoor },
        BatterySource.HEART to pulse.battery.takeIf { heartEnabled && heart.live },
        BatterySource.TRAINER to trainer.battery.takeIf { isIndoor && trainer.live },
        BatterySource.POWER to powerMeter.battery.takeIf { powerEnabled && powerMeter.live },
        BatterySource.PHONE to phoneBattery.level,
        BatterySource.SHIFT to derailleurBattery?.percent))
    val dashboardBikeData get() = selectedBikeData(SystemClock.elapsedRealtime()).copy(indoor=isIndoor,batteries=batterySnapshot,otherValues = selectedBikeData(SystemClock.elapsedRealtime()).otherValues - LiveMetric.AVERAGE_CADENCE - LiveMetric.BIKE_LIGHT - LiveMetric.BATTERY +
        (currentBikeBattery?.let { mapOf(LiveMetric.BATTERY to it) } ?: emptyMap()) +
        (if(isIndoor) indoorRide.summaries()+listOfNotNull(indoorRide.times(elapsedMs,ride.bike.distanceM).movingAverageKmh?.let { LiveMetric.AVERAGE_SPEED to kotlin.math.round(it*100).toLong() }).toMap() else emptyMap()) +
        (currentBikeLight?.let { mapOf(LiveMetric.BIKE_LIGHT to if (it) 1L else 0L) } ?: emptyMap()) +
        (cadenceAverage.value?.let { mapOf(LiveMetric.AVERAGE_CADENCE to kotlin.math.round(it * 2).toLong()) } ?: emptyMap()))
    private var heartSum = 0L; private var heartCount = 0
    /** One bounded display calculation; every view reads this same snapshot. */
    private val averageTrends=AverageTrends()
    internal var trendSnapshot=RideTrendSnapshot();private set
    private var trendUpdatedAt:Long?=null
    private fun resetDisplayTrends() { averageTrends.clear();trendSnapshot=RideTrendSnapshot();trendUpdatedAt=null }
    private fun updateDisplayTrends(now:Long) {
        if(trendUpdatedAt?.let { now>=it && now-it<1000 }==true) return
        trendUpdatedAt=now
        val speed=primarySpeed(now);val power=primaryPower(now);val cadence=primaryCadence(now)
        val heart=chooseReading(RecordingQuantity.HEART,sources(),heartReadings(now),now)
        val speedMean=when(speed?.source) {
            RecordingSource.GPS->ride.gps.snapshot(now).movingAverageKmh
            RecordingSource.TRAINER->indoorRide.times(elapsedMs,ride.bike.distanceM).movingAverageKmh
            RecordingSource.BIKE->bikeData.value(LiveMetric.AVERAGE_SPEED)?.div(LiveMetric.AVERAGE_SPEED.divisor)
            else->null
        }
        val powerMean=if(recording || completedElapsedMs!=null) performance.observedAveragePowerW else when(power?.source) {
            RecordingSource.BIKE->bikeData.value(LiveMetric.AVERAGE_POWER)?.toDouble()
            RecordingSource.TRAINER->indoorRide.averagePower
            else->null
        }
        trendSnapshot=averageTrends.update(PrimaryTrendInputs(
            TrendReading(speed?.reading?.value?.times(3.6),speedMean,speed?.source),
            TrendReading(power?.reading?.value,powerMean,power?.source),
            TrendReading(cadence?.reading?.value,cadenceAverage.value,cadence?.source),
            TrendReading(heart?.reading?.value,pulse.average,heart?.source)),now)
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
    private val confirmedBikePause=ConfirmedBikePause()
    private var mainBikeSpeedSeen = false
    val powerMeasurement: PowerMeasurement? get() = latestPerformance[PerformanceChannel.POWER]?.let {
        PowerMeasurement(runCatching { PowerSource.valueOf(it.source) }.getOrDefault(primaryPowerSource), it.value, it.receivedAtMs, it.sensorTimeMs, it.connected,
            it.connected && it.value != null && SystemClock.elapsedRealtime() - it.receivedAtMs <= PerformanceEngine.MAX_HOLD_MS)
    }
    private fun capturePerformance(channel: PerformanceChannel, value: Double?, source: String,
                                   now: Long = SystemClock.elapsedRealtime(), sensorTime: Long? = null) {
        var event=PerformanceEvent(channel,now,value,now,sensorTime,source,value!=null)
        if(channel==PerformanceChannel.SPEED) {
            val provider=if(source=="GPS_SPEED") RecordingSource.GPS else if(isIndoor) RecordingSource.TRAINER else RecordingSource.BIKE
            rawSpeeds[provider]=event
            val chosen=primarySpeed(now)
            if(chosen?.source!=provider && chosen?.source==selectedSpeedSource) return
            selectedSpeedSource=chosen?.source
            val original=chosen?.let { rawSpeeds[it.source] }
            event=event.copy(value=chosen?.reading?.value,receivedAtMs=original?.receivedAtMs ?: now,sensorTimeMs=original?.sensorTimeMs,
                source=(if(recording) activeConfig else config).speedSource,connected=chosen!=null)
        }
        if(channel==PerformanceChannel.POWER) {
            val provider=when(source) { PowerSource.CYCLING_POWER_BLE.name->RecordingSource.POWER;PowerSource.FTMS.name->RecordingSource.TRAINER;else->RecordingSource.BIKE }
            rawPowers[provider]=event.copy(value=value?.takeIf { it.isFinite() && it in 0.0..65535.0 })
            val chosen=primaryPower(now)
            if(chosen?.source!=provider && chosen?.source==selectedPowerSource) return
            selectedPowerSource=chosen?.source
            val original=chosen?.let { rawPowers[it.source] }
            event=event.copy(value=chosen?.reading?.value,receivedAtMs=original?.receivedAtMs ?: now,sensorTimeMs=original?.sensorTimeMs,
                source=original?.source ?: source,connected=chosen!=null)
        }
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
        } else pendingPerformance += event.copy(atMs = now - startElapsed, receivedAtMs = event.receivedAtMs - startElapsed,
            sensorTimeMs = event.sensorTimeMs?.minus(startElapsed))
    }
    private fun captureGpsPerformance() {
        if(isIndoor) return
        val now = SystemClock.elapsedRealtime()
        val time = ride.gps.lastObservationTimeMs ?: return
        if(rawSpeeds[RecordingSource.GPS]?.sensorTimeMs==time) return
        capturePerformance(PerformanceChannel.SPEED, ride.gps.snapshot(now).speedKmh?.div(3.6), "GPS_SPEED", now, time)
    }
    val mapState = MapState(context) { notifyChanged() }
    var mapTrackEnabled = true; private set
    private val store = RecordingStore(context, ::recordingChanged)
    internal fun recordingChanged(update: RecordingUpdate) {
        recordingUpdate = update; saving = update.busy
        if(update is RecordingUpdate.Finished && update.result.rideId!=null) pendingRideSummary=update.result
        if (update is RecordingUpdate.Finished || update is RecordingUpdate.Recovered && update.results.isNotEmpty()) mapState.refreshLibrary()
        notifyChanged()
    }
    internal val hrv=HrvController(context)
    val heart: HeartRateConnection = HeartRateConnection(context, { heartStatus = it; if (heartIsLive()) heartReconnect.connected(); if (!heartActive()) { pulse = pulse.copy(bpm = null); heartAt = null; if(!useTrainerHeart) capturePerformance(PerformanceChannel.HEART_RATE, null, "HEART_RATE") }; notifyChanged() }, {
        heartAt = SystemClock.elapsedRealtime()
        pulse=pulse.copy(bpm=it.bpm)
        if(!useTrainerHeart) {
            capturePerformance(PerformanceChannel.HEART_RATE,it.bpm?.toDouble(),"HEART_RATE")
            if(recording) it.bpm?.let { bpm -> heartSum+=bpm;heartCount++;pulse=pulse.copy(average=heartSum.toDouble()/heartCount,maximum=maxOf(pulse.maximum ?: bpm,bpm)) }
        }
        notifyChanged()
    }, { pulse = pulse.copy(battery = it); notifyChanged() },rawMeasurement=hrv::packet,stopped=hrv::disconnected)
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
    private fun sources()=if(recording) activeConfig.sourcePreferences ?: RecordingSources.defaults(activeConfig,isIndoor)
        else storedConfig.sourcePreferences ?: RecordingSources.defaults(storedConfig,isIndoor)
    private val useTrainerHeart get()=isIndoor && chooseReading(RecordingQuantity.HEART,sources(),heartReadings(SystemClock.elapsedRealtime()),SystemClock.elapsedRealtime())?.source==RecordingSource.TRAINER
    private fun heartReadings(now:Long):Map<RecordingSource,SourceReading> = buildMap {
        if(heartEnabled && heart.live) pulse.bpm?.takeIf { it in 1..254 }?.let { bpm -> heartAt?.let { put(RecordingSource.HEART,SourceReading(bpm.toDouble(),it)) } }
        if(isIndoor && trainer.live) trainer.readings.value(TrainerField.HEART,now)?.let { put(RecordingSource.TRAINER,SourceReading(it,trainer.readings.at(TrainerField.HEART) ?: now)) }
    }
    private val heartAvailable get()=if(useTrainerHeart) trainer.live else heart.live
    private val selectedHeartAt get()=if(useTrainerHeart) trainer.readings.at(TrainerField.HEART) else heartAt
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
    /** Use the same preferred/fallback speed as recording and performance, never cached display values. */
    val currentSpeedKmh: Double? get() = primarySpeed(SystemClock.elapsedRealtime())?.reading?.value?.times(3.6)
    val isMoving: Boolean get() = currentSpeedKmh?.let { it > 0.0 } == true
    /** Bosch light state is sent on change; retain it for this connection, never across disconnect. */
    val currentBikeLight: Boolean? get() = bikeData.value(LiveMetric.BIKE_LIGHT)
        ?.takeIf { it in 0..1 && fresh(LiveMetric.BIKE_LIGHT, SystemClock.elapsedRealtime()) }?.let { it == 1L }
    private val savedBattery=BikeBatteryMemory(context.getSharedPreferences("bike-battery",0))
    private fun batteryIdentity()=BikeIdentities.id(preferences,bikes.activeSlot)
    val currentBikeBattery:Long? get()=if(isIndoor) null else
        bikeData.value(LiveMetric.BATTERY)?.takeIf { it in 0..100 && connection.isLive }
            ?: savedBattery.read(batteryIdentity())
    internal val bikeBatteryWarningLevel:Long? get()=currentBikeBattery.takeIf { bikes.current().type==BikeType.EBIKE && connection.isLive }
    val currentAssistMode: Long? get() = bikeData.value(LiveMetric.ASSIST_MODE)
        .takeIf { fresh(LiveMetric.ASSIST_MODE, SystemClock.elapsedRealtime()) }
    internal fun trainingInputs(now: Long = SystemClock.elapsedRealtime()): TrainingInputs = TrainingInputs(
        bikeData.riderPowerWatts?.toDouble()?.takeIf { fresh(LiveMetric.RIDER_POWER, now) }, times[LiveMetric.RIDER_POWER],
        if(isIndoor) indoorRide.averagePower.takeIf { trainer.live } else bikeData.value(LiveMetric.AVERAGE_POWER)?.toDouble()?.takeIf { fresh(LiveMetric.AVERAGE_POWER, now) },
        pulseValues.bpm?.toDouble()?.takeIf { heartAvailable && selectedHeartAt?.let { now - it in 0..5000 } == true }, selectedHeartAt,
        pulse.average.takeIf { heartAvailable && selectedHeartAt?.let { now - it in 0..5000 } == true })
    private val storedConfig get()=RecordingConfig.decode(preferences.getStringSet("recordFields", null), preferences.getString("recordSpeed", null), preferences.getString("recordDistance", null), preferences.getStringSet("recordMetadata",null),preferences.getString(RecordingSources.KEY,null))
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
    init { RideNames.schedule(context);ActivityServiceUploads.all.forEach { runCatching { it.schedule(context) } }; IntervalsUploads.schedule(context); IntervalsHrvUploads.schedule(context); RouteUploads.schedule(context); diagnostics.reset("App=${AppVersion.name} Mode=MCSP Android=${android.os.Build.VERSION.SDK_INT} Device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}") }
    fun notifyChanged() {
        if(observers.isNotEmpty()) updateDisplayTrends(SystemClock.elapsedRealtime())
        observers.toList().forEach { it() }
    }
    fun setVisible(value: Boolean) {
        val opened=value && !visible
        visible=value;if(!value) { hrv.abort("APP_BACKGROUND");shiftGroup.stop() };updateActivity()
        if(opened) {
            autoStartOnConnection(recheck=true)
            MaintenanceRepository.get(context).refresh()
        }
    }
    private fun updateActivity() {
        bikeReconnect.foreground = active; heartReconnect.foreground = active;trainerReconnect.foreground=active;powerReconnect.foreground=active
        handler.removeCallbacks(tick)
        if (active) {
            phoneBattery.start()
            if(!isIndoor) barometer.start()
            if (!isIndoor && gpsEnabled && connectionsRequested && BikeSensor.GPS !in disconnectedSensors) gpsSensor.start()
            reconnectSensors(); scheduleTick()
        } else {
            shiftGroup.stop()
            phoneBattery.stop();barometer.stop()
            connection.onForegroundChanged(false); trainer.stop();powerMeter.stop();heart.stop(); gpsSensor.stop(); ride.bike.pause(); heartAt = null
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
        val profile=bikes.current()
        shiftGroup.configure(if(profile.type==BikeType.EBIKE) profile.shiftDevices else emptyList())
        if(visible && shiftEnabled && connectionsRequested && BikeSensor.SHIFT !in disconnectedSensors) shiftGroup.refresh()
        bikeReconnect.enabled = !isIndoor && bikeEnabled && connectionsRequested && BikeSensor.EBIKE !in disconnectedSensors
        heartReconnect.enabled = heartEnabled && connectionsRequested && BikeSensor.HEART !in disconnectedSensors
        bikeAddress?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }?.let {
            if (bikeReconnect.due(now, connection.isActive)) { bikeReconnect.attempted(now); connection.connect(adapter.getRemoteDevice(it)) }
        }
        trainerReconnect.enabled=isIndoor && trainerEnabled && connectionsRequested && BikeSensor.TRAINER !in disconnectedSensors
        trainerAddress?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }?.let {
            if(trainerReconnect.due(now,trainer.active)) { trainerReconnect.attempted(now);trainer.connect(adapter.getRemoteDevice(it)) }
        }
        powerReconnect.enabled=powerEnabled && connectionsRequested && BikeSensor.POWER !in disconnectedSensors
        bikes.current().powerAddress?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }?.let {
            if(powerReconnect.due(now,powerMeter.active)) { powerReconnect.attempted(now);powerMeter.connect(adapter.getRemoteDevice(it)) }
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
                trainerAddress=target.trainerAddress,trainerName=target.trainerName,
                powerAddress=target.powerAddress,powerName=target.powerName,shiftDevices=target.shiftDevices))
            bikes.ui.copy(sourceSlot,target.slot)
        }
        return true
    }
    internal fun switchBike(slot: Int): Boolean {
        if (slot == bikes.activeSlot) return true
        if (recording || saving || hrv.busy) return false
        activateBike(reuseHeart=true) { bikes.select(slot) }
        return true
    }
    internal fun changeBikeType(type:BikeType):Boolean {
        if(recording || saving || !type.available) return false
        if(type==bikes.current().type) return true
        activateBike { bikes.save(bikes.current().copy(type=type)) }
        return true
    }
    internal fun setIndoorType(type:IndoorBikeType):Boolean {
        if(recording || saving || hrv.busy || !isIndoor || !type.available) return false
        if(bikes.current().indoorType!=type) { bikes.save(bikes.current().copy(indoorType=type));profileRevision++;notifyChanged() }
        return true
    }
    private fun activateBike(reuseHeart:Boolean=false,change:()->Unit) {
        shiftGroup.stop()
        val sharedAddress=if(reuseHeart && heartEnabled && hasBluetooth() && heart.live && !disconnected(BikeSensor.HEART)) heart.deviceAddress else null
        val sharedName=heartName;val sharedPulse=pulse;val sharedAt=heartAt;val sharedStatus=heartStatus
        connectionsRequested = false
        connection.stop();trainer.stop();powerMeter.stop();gpsSensor.stop();autoStartWasConnected=false
        if(sharedAddress==null) heart.stop()
        change()
        val target=bikes.current()
        val keepHeart=sharedAddress!=null && target.heartEnabled && (target.heartAddress==null || target.heartAddress==sharedAddress)
        if(keepHeart && target.heartAddress==null) bikes.save(target.copy(heartAddress=sharedAddress,heartName=sharedName))
        if(sharedAddress!=null && !keepHeart) heart.stop()
        profileRevision++;resetDisplayTrends();rawSpeeds.clear();rawPowers.clear();rawCadences.clear();selectedPowerSource=null;selectedSpeedSource=null;lastCadenceSample=null;barometer.resetReference()
        if(isIndoor) barometer.stop()
        themes.reloadFromPreferences()
        mapState.restoreProfileRoute()
        connectionsRequested = bikes.current().autoConnect; disconnectedSensors.clear()
        gpsEnabled = bikes.current().gpsEnabled && !isIndoor
        bikeData = LiveData(indoor=isIndoor)
        pulse = if(keepHeart) PulseValues(bpm=sharedPulse.bpm,battery=sharedPulse.battery) else PulseValues()
        heartAt = if(keepHeart) sharedAt else null;times.clear()
        latestPerformance.clear(); pendingPerformance.clear(); mainBikeSpeedSeen = false; lastPacketAt = null
        ride.gps.reset(); ride.gps.setCollecting(false); ride.bike.reset(); cadenceAverage.reset();indoorRide.reset();trainerDistance.reset()
        heartSum = 0; heartCount = 0; completedElapsedMs = null
        bikeStatus = context.getString(R.string.bikes_disconnected)
        heartStatus = if(keepHeart) sharedStatus else context.getString(R.string.bikes_disconnected)
        gpsStatus = context.getString(R.string.bikes_gps_disconnect)
        bikeReconnect.connected();trainerReconnect.connected();powerReconnect.connected(); heartReconnect.connected()
        updateActivity()
    }
    internal fun restoredBikeSettings() {
        check(!recording && !saving)
        activateBike {}
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
        bikeReconnect.connected();trainerReconnect.connected();powerReconnect.connected(); heartReconnect.connected(); updateActivity()
    }
    internal fun disconnected(sensor: BikeSensor) = sensor in disconnectedSensors
    internal fun receivedValueCount(sensor: BikeSensor): Int? = if (disconnected(sensor)) null else when (sensor) {
        BikeSensor.EBIKE -> connection.receivedValueCount.takeIf { connection.isLive }
        BikeSensor.HEART -> heart.receivedValueCount.takeIf { heart.live }
        BikeSensor.TRAINER -> trainer.receivedValueCount.takeIf { trainer.live }
        BikeSensor.POWER -> powerMeter.receivedValueCount.takeIf { powerMeter.live }
        else -> null
    }
    internal fun receivedValueNames(sensor:BikeSensor):List<String>? {
        if(receivedValueCount(sensor)==null) return null
        fun label(id:Int)=context.getString(id)
        return when(sensor) {
            BikeSensor.EBIKE->connection.receivedMetrics.map { metric ->
                val title=DashboardTextResources.title(context,metric.name) ?: metric.title
                if(metric.unit.isEmpty()) title else "$title · ${metric.unit}"
            }+if(connection.receivedRanges) listOf(label(R.string.sensor_values_range)) else emptyList()
            BikeSensor.HEART->heart.receivedTypes.sorted().map { label(when(it) {
                "pulse"->R.string.sensor_values_heart;"energy"->R.string.sensor_values_energy
                "rr"->R.string.sensor_values_rr;else->R.string.sensor_values_battery
            }) }
            BikeSensor.TRAINER->TrainerField.entries.filter { it in trainer.readings.receivedTypes }.map { label(when(it) {
                TrainerField.SPEED->R.string.sensor_values_speed;TrainerField.POWER->R.string.sensor_values_power
                TrainerField.CADENCE->R.string.sensor_values_cadence;TrainerField.DISTANCE->R.string.sensor_values_distance
                TrainerField.HEART->R.string.sensor_values_heart;TrainerField.ELAPSED->R.string.sensor_values_elapsed
            }) }+if(trainer.receivedValueCount>trainer.readings.count) listOf(label(R.string.sensor_values_battery)) else emptyList()
            BikeSensor.POWER->buildList {
                if(TrainerField.POWER in powerMeter.readings.receivedTypes) add(label(R.string.sensor_values_power))
                if(TrainerField.CADENCE in powerMeter.readings.receivedTypes) add(label(R.string.sensor_values_cadence))
                if(powerMeter.receivedValueCount>powerMeter.readings.count) add(label(R.string.sensor_values_battery))
            }
            else->emptyList()
        }
    }
    internal fun disconnectSensor(sensor: BikeSensor) {
        disconnectedSensors += sensor
        when (sensor) {
            BikeSensor.EBIKE -> connection.stop()
            BikeSensor.HEART -> { heart.stop(); heartAt = null; pulse = pulse.copy(bpm = null); capturePerformance(PerformanceChannel.HEART_RATE, null, "HEART_RATE") }
            BikeSensor.GPS -> { gpsSensor.stop(); gpsStatus = context.getString(R.string.bikes_gps_disconnect); capturePerformance(PerformanceChannel.SPEED, null, "GPS_SPEED") }
            BikeSensor.TRAINER -> { trainer.stop();trainerChanged() }
            BikeSensor.POWER -> { powerMeter.stop();powerMeterChanged() }
            BikeSensor.SHIFT -> shiftGroup.stop()
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
            BikeSensor.POWER -> bikes.save(p.copy(powerAddress=null,powerName=null))
            BikeSensor.SHIFT -> { bikes.save(p.copy(shiftDevices=emptyList()));shiftGroup.configure(emptyList()) }
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
    internal fun selectPowerMeter(device:BluetoothDevice,name:String) {
        if(recording || saving) return
        bikes.save(bikes.current().copy(powerAddress=device.address,powerName=name,powerEnabled=true))
        connectionsRequested=true;disconnectedSensors.remove(BikeSensor.POWER)
        powerMeter.stop();rawPowers.remove(RecordingSource.POWER);rawCadences.remove(RecordingSource.POWER)
        powerReconnect.start();reconnectSensors();notifyChanged()
    }
    internal fun selectShiftComponent(device:BluetoothDevice,name:String) {
        val p=bikes.current()
        if(recording || saving || p.type!=BikeType.EBIKE) return
        val address=device.address.uppercase(java.util.Locale.ROOT)
        if(p.shiftDevices.any { it.address==address } || p.shiftDevices.size>=AxsDevice.MAX_COUNT) return
        val safeName=name.filterNot(Char::isISOControl).trim().take(248).ifBlank { address }
        val component=AxsDevice(address,safeName)
        bikes.save(p.copy(shiftEnabled=true,shiftDevices=p.shiftDevices+component))
        connectionsRequested=true;disconnectedSensors.remove(BikeSensor.SHIFT)
        refreshShiftGroup()
    }
    internal fun setShiftEnabled(enabled:Boolean) {
        bikes.save(bikes.current().copy(shiftEnabled=enabled))
        if(!enabled) shiftGroup.stop() else { connectionsRequested=true;disconnectedSensors.remove(BikeSensor.SHIFT) }
        if(enabled) refreshShiftGroup()
        reconnectSensors();notifyChanged()
    }
    internal fun refreshShiftGroup() {
        val p=bikes.current()
        if(p.type!=BikeType.EBIKE || !p.shiftEnabled || !hasBluetooth() || !visible) return
        connectionsRequested=true;disconnectedSensors.remove(BikeSensor.SHIFT)
        shiftGroup.configure(p.shiftDevices);shiftGroup.refresh(force=true);notifyChanged()
    }
    internal fun removeShiftComponent(address:String) {
        if(recording || saving) return
        val p=bikes.current();bikes.save(p.copy(shiftDevices=p.shiftDevices.filterNot { it.address==address }))
        shiftGroup.configure(bikes.current().shiftDevices);notifyChanged()
    }
    internal fun setPowerEnabled(enabled:Boolean) {
        bikes.save(bikes.current().copy(powerEnabled=enabled))
        if(!enabled) { powerMeter.stop();powerMeterChanged() }
        else { connectionsRequested=true;disconnectedSensors.remove(BikeSensor.POWER);powerReconnect.start() }
        reconnectSensors();notifyChanged()
    }
    private fun powerMeterChanged() {
        if(powerMeter.live) powerReconnect.connected()
        if(!powerMeter.active) {
            captureCadence(null,RecordingSource.POWER,SystemClock.elapsedRealtime())
            capturePerformance(PerformanceChannel.POWER,null,PowerSource.CYCLING_POWER_BLE.name)
        }
        notifyChanged()
    }
    internal fun onPowerMeterMeasurement(frame:TrainerMeasurement,now:Long) {
        if(TrainerField.POWER in frame.values) capturePerformance(PerformanceChannel.POWER,frame.values[TrainerField.POWER],PowerSource.CYCLING_POWER_BLE.name,now)
        if(TrainerField.CADENCE in frame.values) captureCadence(frame.values[TrainerField.CADENCE],RecordingSource.POWER,now)
    }
    private fun powerSourceConnected(source:RecordingSource)=when(source) {
        RecordingSource.POWER->powerEnabled && powerMeter.live
        RecordingSource.TRAINER->isIndoor && trainerEnabled && trainer.live
        RecordingSource.BIKE->!isIndoor && bikeEnabled && connection.isLive
        else->false
    }
    private fun primaryPower(now:Long):ChosenReading? = chooseReading(RecordingQuantity.POWER,sources(),rawPowers.mapNotNull { (source,event) ->
        if(powerSourceConnected(source) && event.connected && event.value!=null)
            source to SourceReading(event.value,event.sensorTimeMs ?: event.receivedAtMs) else null
    }.toMap(),now)
    private fun primaryCadence(now:Long)=chooseReading(RecordingQuantity.CADENCE,sources(),rawCadences.filterKeys(::powerSourceConnected),now)
    private fun captureCadence(value:Double?,source:RecordingSource,now:Long) {
        val valid=value?.takeIf { it.isFinite() && it in 0.0..254.0 }
        if(valid==null) rawCadences.remove(source) else rawCadences[source]=SourceReading(valid,now)
        val chosen=primaryCadence(now) ?: return
        val key=chosen.source to chosen.reading.atMs
        if(recording && key!=lastCadenceSample) { cadenceAverage.add(chosen.reading.value);lastCadenceSample=key }
    }
    private fun selectedBikeData(now:Long):LiveData {
        if(bikes.current().powerAddress==null) return bikeData
        val watts=primaryPower(now)?.reading?.value?.toInt()
        val cadence=primaryCadence(now)?.reading?.value
        return bikeData.copy(riderPowerWatts=watts,otherValues=(bikeData.otherValues-LiveMetric.CADENCE)+
            (cadence?.let { mapOf(LiveMetric.CADENCE to kotlin.math.round(it*2).toLong()) } ?: emptyMap()))
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
        autoStartOnConnection()
        if(!trainer.active) {
            lastPacketAt=null;times.clear();ride.bike.pause();trainerDistance.pause()
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
                TrainerField.CADENCE -> captureCadence(value,RecordingSource.TRAINER,now)
                TrainerField.DISTANCE -> Unit // Resolved once below, with a speed fallback.
                TrainerField.HEART -> if(useTrainerHeart) {
                    capturePerformance(PerformanceChannel.HEART_RATE,value,"HEART_RATE",now)
                    if(recording && value!=null) { val bpm=value.toInt();heartSum+=bpm;heartCount++;pulse=pulse.copy(average=heartSum.toDouble()/heartCount,maximum=maxOf(pulse.maximum ?: bpm,bpm)) }
                }
                else -> Unit
            }
        }
        if(recording) updateTrainerDistance(now)
        refreshTrainerData(now)
        lastPacketAt=now
    }
    private fun updateTrainerDistance(now:Long,seed:Boolean=false) {
        if(!trainer.live) { trainerDistance.pause();return }
        val correction=bikes.current().indoorCorrection
        fun reading(field:TrainerField):SourceReading?=trainer.readings.value(field,now)?.let { raw ->
            correction.apply(field,raw)?.let { SourceReading(it,if(seed) now else trainer.readings.at(field) ?: now) }
        }
        trainerDistance.add(now,reading(TrainerField.DISTANCE),reading(TrainerField.SPEED))?.let { metres ->
            if(ride.bike.distanceM==null) ride.bike.reset(0)
            ride.bike.update(metres.toLong())
        }
    }
    fun beginBikeScan() { bikeReconnect.selecting = true; connection.discover() }
    fun endBikeScan() { bikeReconnect.selecting = false; connection.stop(); bikeReconnect.connected(); reconnectSensors() }
    fun saveConfig(config: RecordingConfig) {
        // Indoor is a view of the stored selection, not a destructive replacement of outdoor settings.
        val config=if(isIndoor) storedConfig.let { stored -> stored.copy(
            fields=(stored.fields-IndoorFields.allowed)+(config.fields intersect IndoorFields.allowed),metadata=config.metadata,
            sourcePreferences=config.sourcePreferences?.let { incoming ->
                // Indoor multi-source choices; retain hidden outdoor wheel/GPS/height preferences.
                val previous=stored.sourcePreferences ?: RecordingSources.defaults(stored,false)
                RecordingSources(previous.preferred+incoming.preferred.filterKeys { it in setOf(RecordingQuantity.HEART,RecordingQuantity.POWER,RecordingQuantity.CADENCE) })
            } ?: stored.sourcePreferences) } else config
        preferences.edit().putStringSet("recordFields", config.fields).putString("recordSpeed", config.speedSource).putString("recordDistance", config.distanceSource).putStringSet("recordMetadata",config.metadata.map { it.name }.toSet()).apply { config.sourcePreferences?.let { putString(RecordingSources.KEY,it.json()) } ?: remove(RecordingSources.KEY) }.apply()
        bikes.persistActive()
        if(!recording) {
            val now=SystemClock.elapsedRealtime();val selected=primaryPower(now)
            selectedPowerSource=selected?.source
            val event=selected?.let { rawPowers[it.source] }
            if(event==null) latestPerformance.remove(PerformanceChannel.POWER)
            else latestPerformance[PerformanceChannel.POWER]=event.copy(atMs=now)
        }
    }
    fun beginRecording() {
        if (recording || saving || hrv.busy) return
        resetDisplayTrends()
        // Reserve the coordinate columns outdoors even if GPS is enabled later during this ride.
        // Actual coordinate values below remain strictly controlled by the GPS sensor switch.
        activeConfig=config.withGpsPosition(!isIndoor).let { it.copy(fields=it.fields+PrimaryRecording.FLAG,sourcePreferences=it.sourcePreferences ?: RecordingSources.defaults(it,isIndoor)) }
        primaryDistance=SourceDistance();primaryElevation=RouteElevation();primaryHeightSource=null;primaryHeightLast=null;primaryHeightOffset=0.0;primarySlope=null
        mapState.track.clear(); mapTrackEnabled = "GPS_POSITION" in activeConfig.fields
        ride.gps.reset(); ride.bike.reset(bikeData.value(LiveMetric.ODOMETER).takeIf { !isIndoor && connection.isLive })
        trainerDistance.reset()
        if(isIndoor) updateTrainerDistance(SystemClock.elapsedRealtime(),seed=true)
        cadenceAverage.reset();lastCadenceSample=null;indoorRide.reset()
        heartSum = 0; heartCount = 0; pulse = pulse.copy(average = null, maximum = null)
        startElapsed = SystemClock.elapsedRealtime(); startUtc = System.currentTimeMillis()
        completedElapsedMs = null;confirmedBikePause.reset()
        performance = PerformanceEngine(primaryPowerSource=primaryPowerSource,resolvedPower=true); pendingPerformance.clear(); performanceOverflow = false
        graphContinuedZeroMs=0; liveGraphs.begin()
        if (activeConfig.speedSource == "GPS_SPEED") captureGpsPerformance()
        listOf(PerformanceChannel.POWER, PerformanceChannel.HEART_RATE, PerformanceChannel.SPEED, PerformanceChannel.BIKE_ACTIVITY).forEach { channel ->
            val event = latestPerformance[channel]
            val connected = when (channel) {
                PerformanceChannel.POWER -> primaryPower(SystemClock.elapsedRealtime())!=null
                PerformanceChannel.HEART_RATE -> heartAvailable
                PerformanceChannel.BIKE_ACTIVITY -> connection.isLive
                else -> primarySpeed(SystemClock.elapsedRealtime())!=null
            }
            if (event != null && connected && (channel != PerformanceChannel.SPEED || event.source == activeConfig.speedSource))
                pendingPerformance += event.copy(atMs = 0, receivedAtMs = event.receivedAtMs - startElapsed,
                    sensorTimeMs = event.sensorTimeMs?.minus(startElapsed))
        }
        recordingUpdate = null
        store.begin(startUtc, activeConfig); recording = true; ride.gps.setCollecting(!isIndoor)
        drinkReminder.reset();drinkReminder.update(true,0,DrinkReminderSettings.read(preferences),false)
        shiftBatteryWarning.start(derailleurBattery?.percent,bikes.current().shiftBatteryWarning)
        connectionsRequested = true
        appendSample(); updateActivity()
    }
    fun stopRecording() {
        if (!recording) return
        drinkReminder.reset();shiftBatteryWarning.clear()
        completedElapsedMs = appendSample(); recording = false; saving = true; recordingUpdate = RecordingUpdate.Progress.SAVING_FIT
        ride.gps.setCollecting(false); ride.bike.pause()
        liveGraphs.stop(); store.finish(discardShort=completedElapsedMs!!<10_000); updateActivity()
    }
    private fun appendSample(): Long {
        val sample = sample()
        performance.acceptSample(sample)
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
        if(isIndoor && recording) updateTrainerDistance(now)
        if(isIndoor && recording) indoorRide.add(now-startElapsed,
            bikeData.speedHundredths?.div(100.0).takeIf { fresh(LiveMetric.SPEED,now) },
            primaryPower(now)?.reading?.value,primaryCadence(now)?.reading?.value)
        val gps = gpsValues
        val fresh = LiveData(
            bikeData.speedHundredths.takeIf { fresh(LiveMetric.SPEED, now) }, bikeData.riderPowerWatts.takeIf { fresh(LiveMetric.RIDER_POWER, now) },
            bikeData.otherValues.filterKeys { fresh(it, now) }+(if(isIndoor) dashboardBikeData.otherValues.filterKeys { it.group==2 } else emptyMap()), bikeData.assistNames, bikeData.rangesKm.takeIf { connection.isLive },
        )
        val values=recordingValues(fresh,gps,ride.bike.distanceM,pulseValues).toMutableMap()
        if(recording && PrimaryRecording.FLAG in activeConfig.fields) {
            fun set(id:String,value:Double?) { if(value!=null && value.isFinite()) values[id]=value }
            set("PRIMARY_SPEED",primarySpeed(now)?.reading?.value)
            val gpsAt=ride.gps.lastObservationTimeMs
            val gpsValid=!isIndoor && gpsEnabled && gpsAt!=null && now-gpsAt in 0..5000
            val bikeValid=if(isIndoor) trainer.live else connection.isLive && lastPacketAt?.let { now-it in 0..5000 }==true
            val distances=buildMap<RecordingSource,SourceReading> {
                if(gpsValid) gps.distanceM?.let { put(RecordingSource.GPS,SourceReading(it,gpsAt!!)) }
                if(bikeValid) ride.bike.distanceM?.let { put(if(isIndoor) RecordingSource.TRAINER else RecordingSource.BIKE,SourceReading(it.toDouble(),now)) }
            }
            val distance=primaryDistance.add(chooseReading(RecordingQuantity.DISTANCE,sources(),distances,now));set("PRIMARY_DISTANCE",distance)
            set("PRIMARY_POWER",primaryPower(now)?.reading?.value);set("PRIMARY_CADENCE",primaryCadence(now)?.reading?.value)
            set("PRIMARY_HEART",chooseReading(RecordingQuantity.HEART,sources(),heartReadings(now),now)?.reading?.value)
            val reference=if(gpsValid) gps.altitudeM?.let { SourceReading(it,gpsAt!!,gps.seaLevel) } else null
            val heights=buildMap<RecordingSource,SourceReading> {
                reference?.let { put(RecordingSource.GPS,it) };if(!isIndoor) barometer.reading(reference,now)?.let { put(RecordingSource.BAROMETER,it) }
            }
            val height=chooseReading(RecordingQuantity.HEIGHT,sources(),heights,now)
            if(height!=null) {
                if(primaryHeightSource!=height.source) {
                    primaryHeightOffset=primaryHeightLast?.minus(height.reading.value) ?: 0.0
                    if(primaryHeightLast==null) primaryHeightDatum=height.reading.seaLevel
                    primaryHeightSource=height.source;primarySlope=null
                }
                val absolute=height.reading.value+primaryHeightOffset
                set("PRIMARY_HEIGHT",absolute);set("PRIMARY_HEIGHT_MSL",if(primaryHeightDatum) 1.0 else 0.0)
                primaryElevation.add(absolute,start=!isMoving);primaryHeightLast=absolute
                if(!isMoving) primarySlope=null
                primarySlope?.let { (d,h) -> if(distance-d>=5) set("PRIMARY_GRADIENT",(absolute-h)/(distance-d)*100) }
                if(isMoving && (primarySlope==null || distance-primarySlope!!.first>=5)) primarySlope=distance to absolute
            } else { primaryElevation.breakSegment();primarySlope=null }
            set("PRIMARY_ASCENT",primaryElevation.ascent);set("PRIMARY_DESCENT",primaryElevation.descent)
        }
        val npPause=confirmedBikePause.update(now,recording && bikes.current().type==BikeType.EBIKE,
            connection.isLive,primaryPower(now)!=null,gps.speedKmh?.div(3.6),
            ride.gps.lastObservationTimeMs.takeIf { gpsEnabled && !isIndoor },ride.gps.lastObservationAccuracyM)
        values[ConfirmedBikePause.FLAG]=if(npPause) 1.0 else 0.0
        return RideSample(startUtc + now - startElapsed, now - startElapsed, gps.latitude.takeIf { gpsEnabled && !isIndoor }, gps.longitude.takeIf { gpsEnabled && !isIndoor }, values, pendingPerformance.toList())
    }
    private fun primarySpeed(now:Long):ChosenReading? {
        val readings=rawSpeeds.mapNotNull { (source,event) ->
            val connected=when(source) { RecordingSource.GPS->gpsEnabled && !isIndoor;RecordingSource.TRAINER->isIndoor && trainer.live;else->!isIndoor && connection.isLive }
            if(connected && event.connected && event.value!=null) source to SourceReading(event.value,event.sensorTimeMs ?: event.receivedAtMs) else null
        }.toMap()
        return chooseReading(RecordingQuantity.SPEED,sources(),readings,now)
    }
    private fun fresh(metric: LiveMetric, now: Long): Boolean {
        // Bosch sends charge changes, not a periodic battery heartbeat. Keep only the live
        // connection's actual reading; the persisted dashboard fallback must never enter samples.
        // Light is likewise a state-change notification, not a periodic measurement.
        if(metric==LiveMetric.BIKE_LIGHT && !isIndoor)
            return connection.isLive && times[metric]!=null && bikeData.value(metric)?.let { it in 0..1 }==true
        if(metric==LiveMetric.BATTERY && !isIndoor)
            return connection.isLive && bikeData.value(metric)?.let { it in 0..100 }==true
        val limit = if (metric.group == 2) 120_000 else if (metric.group == 1 || metric == LiveMetric.ASSIST_MODE) 60_000 else 5_000
        if(isIndoor) return trainer.live && times[metric]?.let { now-it in 0..5000 }==true
        return connection.isLive && times[metric]?.let { now - it <= limit } == true
    }
    fun retryPending() { if (!recording && !saving) { saving = true; store.retryPending() } }
    private fun autoStartOnConnection(recheck:Boolean=false) {
        val connected=if(isIndoor) trainer.subscribed else connection.isLive
        val newlyConnected=connected && (!autoStartWasConnected || recheck)
        autoStartWasConnected=connected
        if(!newlyConnected || !visible || !bikes.current().autoStartRecording || recording || saving || hrv.busy) return
        try { context.startForegroundService(Intent(context,RecordingService::class.java).setAction(RecordingService.START)) }
        catch(_:SecurityException) { BicycleFeedback.show(context,R.string.ui_record_permission_error,true) }
        catch(_:IllegalStateException) { BicycleFeedback.show(context,R.string.ui_record_visible_error,true) }
    }
    override fun onStatus(message: String) { bikeStatus = message; scanStatus?.invoke(message); if (connection.isLive) bikeReconnect.connected();autoStartOnConnection(); notifyChanged() }
    override fun onDevice(device: BluetoothDevice, name: String) { scanFound?.invoke(device, name) }
    override fun onSamples(samples: List<McspSample>) {
        if(isIndoor) return
        val now = SystemClock.elapsedRealtime()
        samples.forEach { sample -> LiveMetric.forAddress(sample.address)?.let {
            times[it] = now
            if (it == LiveMetric.CADENCE && sample.value in 0..it.maxRaw) captureCadence(sample.value / it.divisor,RecordingSource.BIKE,now)
            if (it == LiveMetric.SPEED) {
                if (sample.address == LiveMetric.SPEED.address) mainBikeSpeedSeen = true
                if (sample.address == LiveMetric.SPEED.address || !mainBikeSpeedSeen)
                    capturePerformance(PerformanceChannel.BIKE_ACTIVITY, sample.value / 360.0, "SPEED", now)
            }
            if (it == LiveMetric.RIDER_POWER) capturePerformance(PerformanceChannel.POWER, sample.value.toDouble(), PowerSource.BOSCH_RIDER.name, now)
            if (it == LiveMetric.SPEED) {
                if (sample.address == LiveMetric.SPEED.address || !mainBikeSpeedSeen)
                    capturePerformance(PerformanceChannel.SPEED, sample.value / 360.0, "SPEED", now)
            }
        } }
    }
    override fun onData(data: LiveData, packetReceived: Boolean) {
        if(isIndoor) return
        bikeData = data
        if(packetReceived) savedBattery.remember(batteryIdentity(),data.value(LiveMetric.BATTERY))
        if (packetReceived) { if (recording) ride.bike.update(data.value(LiveMetric.ODOMETER)); lastPacketAt = SystemClock.elapsedRealtime() }
        else {
            times.clear(); ride.bike.pause(); lastPacketAt = null; mainBikeSpeedSeen = false
            capturePerformance(PerformanceChannel.POWER, null, PowerSource.BOSCH_RIDER.name)
            capturePerformance(PerformanceChannel.BIKE_ACTIVITY, null, "SPEED")
            val chosen = if (recording) activeConfig else config
            if (chosen.speedSource == "SPEED") capturePerformance(PerformanceChannel.SPEED, null, "SPEED")
        }
        autoStartOnConnection();notifyChanged()
    }
    companion object {
        private var instance: RideRuntime? = null
        fun get(context: Context): RideRuntime = instance?.takeIf { it.context === context.applicationContext } ?: RideRuntime(context.applicationContext).also { instance = it }
    }
}
