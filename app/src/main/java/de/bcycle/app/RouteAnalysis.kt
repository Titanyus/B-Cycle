package de.bcycle.app

import com.garmin.fit.Decode
import com.garmin.fit.Mesg
import com.garmin.fit.MesgListener
import java.io.File

internal enum class AnalysisMetric(val label: Int, val unit: Int, val icon: BicycleIcon) {
    FTP(R.string.analysis_ftp, R.string.analysis_watts, BicycleIcon.Power),
    AVG_MOTOR(R.string.analysis_motor_power, R.string.analysis_watts, BicycleIcon.Power),
    RIDER_BIKE_RATIO(R.string.analysis_rider_bike_ratio, R.string.analysis_no_unit, BicycleIcon.Distance),
    BATTERY_USAGE(R.string.analysis_battery_usage, R.string.analysis_percent, BicycleIcon.Work),
    DISTANCE(R.string.analysis_distance, R.string.analysis_km, BicycleIcon.TrackRecorded),
    MOVING(R.string.analysis_moving, R.string.analysis_hours, BicycleIcon.Duration),
    ELAPSED(R.string.analysis_elapsed, R.string.analysis_hours, BicycleIcon.Duration),
    AVG_SPEED(R.string.analysis_avg_speed, R.string.analysis_kmh, BicycleIcon.Speed),
    MAX_SPEED(R.string.analysis_max_speed, R.string.analysis_kmh, BicycleIcon.Speed),
    ASCENT(R.string.analysis_ascent, R.string.analysis_meters, BicycleIcon.Ascent),
    DESCENT(R.string.analysis_descent, R.string.analysis_meters, BicycleIcon.Ascent),
    AVG_POWER(R.string.analysis_avg_power, R.string.analysis_watts, BicycleIcon.Power),
    MAX_POWER(R.string.analysis_max_power, R.string.analysis_watts, BicycleIcon.Power),
    NP(R.string.analysis_np, R.string.analysis_watts, BicycleIcon.Power),
    AVG_CADENCE(R.string.analysis_avg_cadence, R.string.analysis_rpm, BicycleIcon.Cadence),
    MAX_CADENCE(R.string.analysis_max_cadence, R.string.analysis_rpm, BicycleIcon.Cadence),
    AVG_HEART(R.string.analysis_avg_heart, R.string.analysis_bpm, BicycleIcon.Heart),
    MAX_HEART(R.string.analysis_max_heart, R.string.analysis_bpm, BicycleIcon.Heart),
    WORK(R.string.analysis_work, R.string.analysis_kj, BicycleIcon.Work),
    CALORIES(R.string.analysis_calories, R.string.analysis_kcal, BicycleIcon.Calories),
    EF(R.string.analysis_ef, R.string.analysis_w_bpm, BicycleIcon.Distance),
    RATIO(R.string.analysis_ratio, R.string.analysis_speed_w, BicycleIcon.Distance)
}
/** Fixed semantic order. Color alternatives differ only in brightness for dark/light backgrounds. */
internal enum class AnalysisChart(val label: Int, val unit: Int, val darkColor: Int, val lightColor: Int) {
    SPEED(R.string.analysis_speed, R.string.analysis_kmh, 0xffffbf37.toInt(), 0xff663600.toInt()),
    CADENCE(R.string.analysis_cadence, R.string.analysis_rpm, 0xff5dde85.toInt(), 0xff003d1d.toInt()),
    POWER(R.string.analysis_rider_power, R.string.analysis_watts, 0xff68b5ff.toInt(), 0xff003569.toInt()),
    HEART(R.string.analysis_heart, R.string.analysis_bpm, 0xffff7c8a.toInt(), 0xff720018.toInt()),
    ELEVATION(R.string.analysis_elevation, R.string.analysis_meters, 0xffc6ccd3.toInt(), 0xff424950.toInt());
    val detailLabel get() = if(this==POWER) R.string.analysis_power_group else label
    fun color(background: Int): Int {
        val chosen = if (ThemeValidator.contrast(darkColor, background) >= ThemeValidator.contrast(lightColor, background)) darkColor else lightColor
        if (ThemeValidator.contrast(chosen, background) >= 3) return chosen
        // Custom palettes can use mid-tone surfaces: adjust brightness centrally, retaining the semantic hue.
        val white = 0xffffffff.toInt(); val black = 0xff000000.toInt()
        val target = if (ThemeValidator.contrast(white, background) > ThemeValidator.contrast(black, background)) white else black
        for (step in 1..16) {
            fun channel(shift: Int) = (((chosen ushr shift) and 255) * (16 - step) + ((target ushr shift) and 255) * step) / 16
            val candidate = 0xff000000.toInt() or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
            if (ThemeValidator.contrast(candidate, background) >= 3) return candidate
        }
        return target
    }
}
internal data class AnalysisPoint(val distance: Double?, val seconds: Double?, val value: Double?)
internal data class AnalysisSeries(val chart: AnalysisChart, val points: List<AnalysisPoint>)
internal data class BestEffort(val durationMs: Long, val watts: Double) {
    fun wattsPerKg(weightKg: Double?): Double? = weightKg?.takeIf { it.isFinite() && it in RiderProfileValidation.weightRange }?.let { watts / it }
}
internal object BestEfforts {
    val durationsMs = listOf(5L, 60L, 300L, 480L, 1200L, 3600L, 7200L).map { it * 1000 }
    fun select(averages: Map<Long, Double>): List<BestEffort> = durationsMs.asReversed().mapNotNull { duration ->
        averages[duration]?.takeIf { it.isFinite() && it >= 0 }?.let { BestEffort(duration, it) }
    }.take(4)
}
internal data class RouteAnalysis(val entry: ManagedRoute, val metrics: Map<AnalysisMetric, Double>, val route: GpxRoute?,
    val distanceAxis: Boolean, val axisEnd: Double, val series: List<AnalysisSeries>, val bestEfforts: List<BestEffort> = emptyList(), val zones: RouteZoneDistributions = RouteZoneDistributions(), val distributions: List<RouteValueDistribution> = emptyList(), val weatherTarget:WeatherTarget?=null,
    val powerQuality:PerformanceQuality?=null,val inferredPauseMs:Long=0)

/** Bounded streaming reduction, retaining endpoint/extrema and an explicit break for missing intervals. */
internal class AnalysisSeriesBuffer {
    private var data = ArrayList<AnalysisPoint>()
    fun add(point: AnalysisPoint) {
        // Consecutive missing values need only one marker; genuine zeros are retained.
        if (point.value == null && data.lastOrNull()?.value == null && data.isNotEmpty()) return
        data.add(point)
        if (data.size > 4096) {
            val reduced = ArrayList<AnalysisPoint>()
            for (start in data.indices step 8) {
                val end = minOf(start + 8, data.size)
                val indices = mutableSetOf(start, end - 1)
                val valid = (start until end).filter { data[it].value != null }
                valid.minByOrNull { data[it].value!! }?.let(indices::add)
                valid.maxByOrNull { data[it].value!! }?.let(indices::add)
                (start until end).firstOrNull { data[it].value == null }?.let(indices::add)
                // Collapse a mixed bucket to disconnected points: never bridge a discarded gap.
                val hasGap = (start until end).any { data[it].value == null }
                indices.sorted().forEach { i ->
                    if (hasGap && reduced.lastOrNull()?.value != null && data[i].value != null)
                        reduced.add(data[i].copy(value = null))
                    reduced.add(data[i])
                }
            }
            data = reduced
            // Worst case fragmented data: retain markers and thin isolated observations.
            if (data.size > 4096) data = ArrayList(data.filterIndexed { i, p -> p.value == null || i % 2 == 0 })
        }
    }
    fun points() = data.toList()
}

/** Read-only detail loader. A file is decoded once, off the UI thread, for all sections together. */
internal object RouteAnalysisRead {
    fun read(file: File, entry: ManagedRoute, profile: RiderProfile = RiderProfile(), cancelled: () -> Boolean = { false }): RouteAnalysis {
        if(entry.format=="JSON") {
            check(!cancelled())
            val summary=DreeveSummaryLibrary.read(file)
            return RouteAnalysis(entry,summary.activity.summary.metrics,null,false,0.0,emptyList())
        }
        val builder = Builder(entry, profile)
        if (entry.format == "GPX") {
            val pauses=GpxPauseInference()
            val route = file.inputStream().buffered().use { GpxImport.samples(it) { sample ->
                check(!cancelled())
                pauses.add(sample)?.let(builder::gpxPause)
                builder.record(sample.time,null,sample.point,sample.speed,sample.power,sample.cadence,sample.heart,sample.height,null,null)
            } }
            return builder.finish(route)
        }
        require(file.length() in 14..FitRead.MAX_BYTES)
        require(file.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
        val track = MapTrack()
        val listener = MesgListener { message ->
            check(!cancelled())
            when (message.num) {
                12 -> builder.sport(number(message,"sub_sport"))
                20 -> {
                    fun v(name: String) = number(message, name)
                    fun dev(name: String, units: String) = developer(message, name, units)
                    val time = v("timestamp")?.let { (it * 1000 + 631065600000L).toLong() }
                    val lat = v("position_lat")?.times(180.0 / 2147483648.0)
                    val lon = v("position_long")?.times(180.0 / 2147483648.0)
                    val point = if (lat != null && lon != null && lat in -85.05112878..85.05112878 && lon in -180.0..180.0) MapPoint(lat, lon) else null
                    track.append(RideSample(time ?: 0, time ?: 0, point?.latitude, point?.longitude))
                    builder.record(time, v("distance") ?: dev("GPS_DISTANCE", "m") ?: dev("BIKE_DISTANCE", "m"), point,
                        v("enhanced_speed") ?: v("speed") ?: dev("SPEED", "m/s") ?: dev("GPS_SPEED", "m/s"),
                        v("power") ?: dev("RIDER_POWER", "W"), v("cadence") ?: dev("CADENCE", "rpm") ?: dev("CADENCE", "U/min"),
                        v("heart_rate") ?: dev("HEART_RATE", "bpm"), v("enhanced_altitude") ?: v("altitude") ?: dev("PRIMARY_HEIGHT","m") ?: dev("GPS_ALTITUDE", "m"),
                        v("motor_power") ?: dev("MOTOR_POWER", "W"), v("battery_soc") ?: dev("BATTERY", "%"), dev("SPEED", "m/s"))
                }
                18, 19 -> builder.summary(message, message.num == 18)
                21 -> if (number(message, "event") == 0.0) builder.timer(number(message, "timestamp"), number(message, "event_type"))
            }
        }
        require(file.inputStream().buffered().use { Decode().read(it, listener) })
        val points = track.points.toList()
        val route = points.takeIf { it.zipWithNext().any { pair -> !pair.second.segmentStart } }?.let { GpxRoute(it, it.size, it.count { p -> p.segmentStart }) }
        return builder.finish(route)
    }
    private fun number(message: Mesg, name: String): Double? = message.getFieldDoubleValue(name)?.takeIf { it.isFinite() }
    private fun developer(message: Mesg, name: String, units: String): Double? = FitDeveloperValues.number(message,name,units)

    private class Builder(val entry: ManagedRoute, profile: RiderProfile) {
        val weather=WeatherTargetBuffer()
        val zones = RouteZoneAccumulator(profile)
        val buffers = AnalysisChart.entries.associateWith { AnalysisSeriesBuffer() }
        val sessions = mutableListOf<Map<AnalysisMetric, Double>>()
        val laps = mutableListOf<Map<AnalysisMetric, Double>>()
        val cadence = PedalingAverage(); val moving = MovingTime(); val elevation = RouteElevation()
        private var powerSource=if(entry.format=="GPX") PowerSource.CYCLING_POWER_BLE else PowerSource.BOSCH_RIDER
        var performance = PerformanceEngine(powerDurationsMs = BestEfforts.durationsMs,primaryPowerSource=powerSource)
        fun sport(subSport:Double?) {
            if(count==0 && subSport==6.0) {
                powerSource=PowerSource.FTMS
                performance=PerformanceEngine(powerDurationsMs=BestEfforts.durationsMs,primaryPowerSource=powerSource)
            }
        }
        val rideMeans = RideAnalysisMeans()
        var count = 0; var firstTime: Long? = null; var lastTime: Long? = null
        var previousRecordTime: Long? = null
        var previous: MapPoint? = null; var geoDistance = 0.0; var nativeStart: Double? = null
        var distanceMax = 0.0; var distanceCount = 0; var lastDistance: Double? = null; var invalidDistance = false
        var nativeDistanceMode: Boolean? = null
        val maxima = mutableMapOf<AnalysisMetric, Double>()
        private var inferredPauseMs=0L
        fun gpxPause(gap:GpxPauseInference.Gap) {
            val start=firstTime ?: return
            performance.accept(PerformanceEvent(PerformanceChannel.PAUSE,gap.fromMs-start))
            performance.accept(PerformanceEvent(PerformanceChannel.RESUME,gap.toMs-start))
            zones.timer(true);zones.timer(false)
            inferredPauseMs+=gap.durationMs
        }
        fun timer(fitStamp: Double?, kind: Double?) {
            val start = firstTime ?: return
            val time = fitStamp?.let { (it * 1000 + 631065600000L).toLong() - start } ?: return
            if (time < 0 || lastTime?.let { time < it - start } == true) return
            if (kind == 0.0 || kind == 1.0 || kind == 4.0) {
                zones.timer(paused = kind != 0.0)
                performance.accept(PerformanceEvent(if (kind == 0.0) PerformanceChannel.RESUME else PerformanceChannel.PAUSE, time))
            }
        }
        fun record(time: Long?, nativeDistance: Double?, point: MapPoint?, speed: Double?, power: Double?, rpm: Double?, heart: Double?, altitude: Double?, motor: Double?, battery: Double?, bikeSpeed: Double? = null) {
            count++; require(count <= 600_000)
            val stamp = time?.takeIf { firstTime == null || it >= (lastTime ?: it) }
            if (stamp != null) { if (firstTime == null) firstTime = stamp; lastTime = stamp }
            weather.add(stamp,point)
            val seconds = stamp?.let { (it - firstTime!!) / 1000.0 }
            val dataGap = stamp != null && previousRecordTime != null && stamp - previousRecordTime!! > 10_000
            val timeGap = stamp != null && previousRecordTime != null && stamp - previousRecordTime!! > 5_000
            if (point != null && previous != null && !point.segmentStart && !timeGap) geoDistance += routeDistance(listOf(previous!!, point))
            previousRecordTime = stamp
            previous = point
            val native = nativeDistance?.takeIf { it.isFinite() && it >= 0 }
            if (nativeDistanceMode == null && (native != null || point != null)) {
                nativeDistanceMode = native != null; nativeStart = native
            }
            val distance = if (nativeDistanceMode == true) native?.minus(nativeStart!!) else point?.let { geoDistance }
            if (distance != null) {
                if (lastDistance != null && distance < lastDistance!!) invalidDistance = true
                lastDistance = distance; distanceMax = maxOf(distanceMax, distance); distanceCount++
            }
            fun valid(value: Double?, range: ClosedFloatingPointRange<Double>) = value?.takeIf { it.isFinite() && it in range }
            val s = valid(speed, 0.0..35.0); val p = valid(power, 0.0..65535.0)
            val c = valid(rpm, 0.0..254.0); val h = valid(heart, 1.0..254.0); val a = valid(altitude, -500.0..10000.0)
            zones.add(stamp, distance, p, h, point?.segmentStart == true)
            c?.let(cadence::add); moving.add(stamp, s)
            rideMeans.add(stamp, p, valid(motor, 0.0..65535.0), valid(battery, 0.0..100.0))
            elevation.add(a, point?.segmentStart == true)
            listOf(AnalysisMetric.MAX_SPEED to s?.times(3.6), AnalysisMetric.MAX_POWER to p,
                AnalysisMetric.MAX_CADENCE to c, AnalysisMetric.MAX_HEART to h).forEach { (key, value) ->
                if (value != null) maxima[key] = maxOf(maxima[key] ?: value, value)
            }
            val readings = mapOf(AnalysisChart.SPEED to s?.times(3.6), AnalysisChart.CADENCE to c,
                AnalysisChart.POWER to p, AnalysisChart.HEART to h, AnalysisChart.ELEVATION to a)
            readings.forEach { (chart, value) ->
                val buffer = buffers.getValue(chart)
                if (dataGap || point?.segmentStart == true) buffer.add(AnalysisPoint(distance, seconds, null))
                buffer.add(AnalysisPoint(distance, seconds, value))
            }
            if (seconds != null) {
                val at = (seconds * 1000).toLong()
                // Only our verified bike SPEED developer field proves bike telemetry;
                // GPS/native FIT speed alone cannot extend a Bosch zero state.
                valid(bikeSpeed, 0.0..35.0)?.takeIf { powerSource==PowerSource.BOSCH_RIDER }?.let {
                    performance.accept(PerformanceEvent(PerformanceChannel.BIKE_ACTIVITY, at, it, source = "SPEED"))
                }
                // An absent FIT sample is not an explicit invalidation of a known zero.
                // The central engine still expires it when bike telemetry becomes stale.
                if (powerSource!=PowerSource.BOSCH_RIDER || p != null || performance.latestPower?.watts != 0.0)
                    performance.accept(PerformanceEvent(PerformanceChannel.POWER, at, p, source = powerSource.name))
                performance.accept(PerformanceEvent(PerformanceChannel.SPEED, at, s, source = "FIT"))
                performance.accept(PerformanceEvent(PerformanceChannel.HEART_RATE, at, h, source = "FIT"))
            }
        }
        fun summary(message: Mesg, session: Boolean) {
            val values = mutableMapOf<AnalysisMetric, Double>()
            fun add(key: AnalysisMetric, field: String, scale: Double = 1.0) { number(message, field)?.times(scale)?.takeIf { it >= 0 }?.let { values[key] = it } }
            add(AnalysisMetric.DISTANCE, "total_distance", .001)
            add(AnalysisMetric.MOVING, "total_moving_time"); add(AnalysisMetric.ELAPSED, "total_elapsed_time")
            add(AnalysisMetric.MAX_SPEED, "enhanced_max_speed", 3.6)
            if (AnalysisMetric.MAX_SPEED !in values) add(AnalysisMetric.MAX_SPEED, "max_speed", 3.6)
            add(AnalysisMetric.ASCENT, "total_ascent"); add(AnalysisMetric.DESCENT, "total_descent")
            add(AnalysisMetric.AVG_POWER, "avg_power"); add(AnalysisMetric.MAX_POWER, "max_power")
            add(AnalysisMetric.MAX_CADENCE, "max_cadence"); add(AnalysisMetric.AVG_HEART, "avg_heart_rate"); add(AnalysisMetric.MAX_HEART, "max_heart_rate")
            add(AnalysisMetric.NP, "normalized_power"); add(AnalysisMetric.WORK, "total_work", .001); add(AnalysisMetric.CALORIES, "total_calories")
            developer(message, "ESTIMATED_FTP", "W")?.let { values[AnalysisMetric.FTP] = it }
            developer(message, "EFFICIENCY_FACTOR", "W/bpm")?.let { values[AnalysisMetric.EF] = it }
            developer(message, "SPEED_POWER_RATIO", "km/h/W")?.let { values[AnalysisMetric.RATIO] = it }
            if (session) sessions += values else laps += values
        }
        fun finish(route: GpxRoute?): RouteAnalysis {
            val metrics = mutableMapOf<AnalysisMetric, Double>()
            val preferred = if (sessions.isNotEmpty()) sessions else laps
            // Whole-session NP/ratios must never be averaged from independent laps/sessions.
            if (preferred.size == 1) metrics.putAll(preferred.single())
            else if (preferred.isNotEmpty()) {
                val sums = setOf(AnalysisMetric.DISTANCE, AnalysisMetric.MOVING, AnalysisMetric.ELAPSED, AnalysisMetric.ASCENT, AnalysisMetric.DESCENT, AnalysisMetric.WORK, AnalysisMetric.CALORIES)
                for (key in AnalysisMetric.entries) {
                    val values = preferred.mapNotNull { it[key] }
                    if (values.size != preferred.size) continue
                    if (key in sums) metrics[key] = values.sum()
                    else if (key.name.startsWith("MAX_")) metrics[key] = values.max()
                    else if (key in setOf(AnalysisMetric.AVG_POWER, AnalysisMetric.AVG_HEART)) {
                        val times = preferred.map { it[AnalysisMetric.ELAPSED] }
                        if (times.all { it != null && it > 0 }) metrics[key] = values.indices.sumOf { values[it] * times[it]!! } / times.sumOf { it!! }
                    }
                }
            }
            fun fallback(key: AnalysisMetric, value: Double?) { if (key !in metrics && value != null && value.isFinite() && value >= 0) metrics[key] = value }
            fallback(AnalysisMetric.DISTANCE, route?.distanceM?.div(1000) ?: distanceMax.takeIf { distanceCount > 1 && !invalidDistance }?.div(1000))
            fallback(AnalysisMetric.ELAPSED, firstTime?.let { lastTime?.minus(it)?.div(1000.0) })
            fallback(AnalysisMetric.MOVING, route?.movingMs?.div(1000.0) ?: moving.value?.div(1000.0))
            fallback(AnalysisMetric.ASCENT, route?.ascentM ?: elevation.ascent)
            fallback(AnalysisMetric.DESCENT, elevation.descent)
            cadence.value?.let { metrics[AnalysisMetric.AVG_CADENCE] = it }
            maxima.forEach { (key, value) -> fallback(key, value) }
            val duration = metrics[AnalysisMetric.MOVING]
            if (duration != null && duration > 0) metrics[AnalysisMetric.DISTANCE]?.let { metrics[AnalysisMetric.AVG_SPEED] = it * 3600 / duration }
            fallback(AnalysisMetric.AVG_POWER, rideMeans.riderMean)
            fallback(AnalysisMetric.AVG_MOTOR, rideMeans.motorMean)
            fallback(AnalysisMetric.RIDER_BIKE_RATIO, rideMeans.pairedRatio)
            fallback(AnalysisMetric.BATTERY_USAGE, rideMeans.batteryUsage)
            val calculated = performance.snapshot()
            fallback(AnalysisMetric.FTP, calculated.estimatedFtpW)
            fallback(AnalysisMetric.NP, calculated.normalizedPowerW); fallback(AnalysisMetric.WORK, calculated.mechanicalWorkKj)
            fallback(AnalysisMetric.CALORIES, calculated.caloriesKcal); fallback(AnalysisMetric.EF, calculated.efficiencyFactor)
            fallback(AnalysisMetric.RATIO, calculated.speedToPowerRatio)
            val distanceAxis = !invalidDistance && distanceCount > 1 && distanceMax > 0
            val end = if (distanceAxis) maxOf(distanceMax / 1000, metrics[AnalysisMetric.DISTANCE] ?: 0.0) else firstTime?.let { lastTime?.minus(it)?.div(1000.0) } ?: 0.0
            val series = AnalysisChart.entries.mapNotNull { chart ->
                val data = buffers.getValue(chart).points()
                if (data.any { it.value != null && (if (distanceAxis) it.distance else it.seconds) != null }) AnalysisSeries(chart, data) else null
            }
            val dated = firstTime?.let { entry.copy(details = entry.details.copy(dateMs = it)) } ?: entry
            return RouteAnalysis(dated, metrics, route, distanceAxis, end.coerceAtLeast(1e-6), series, BestEfforts.select(performance.bestPowerAverages()), if (invalidDistance) RouteZoneDistributions() else zones.result(), zones.timeDistributions(),weather.middle(dated.details.dateMs,metrics[AnalysisMetric.ELAPSED]?.times(1000)?.toLong()),calculated.quality,inferredPauseMs)
        }
    }
}
