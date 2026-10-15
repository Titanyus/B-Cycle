package de.bcycle.app

import kotlin.math.*

/** Monotonic timestamps and SI units; independent of Android and of the bike sensor. */
data class GpsFix(
    val timeMs: Long, val latitude: Double, val longitude: Double, val accuracyM: Double,
    val speedMps: Double? = null, val speedAccuracyMps: Double? = null,
    val altitudeM: Double? = null, val verticalAccuracyM: Double? = null, val seaLevel: Boolean = false,
    val bearingDegrees: Double? = null, val bearingAccuracyDegrees: Double? = null,
)

data class GpsValues(
    val speedKmh: Double? = null, val distanceM: Double? = null,
    val movingAverageKmh: Double? = null, val overallAverageKmh: Double? = null,
    val maximumKmh: Double? = null, val movingMs: Long? = null, val stoppedMs: Long? = null,
    val elapsedMs: Long? = null, val altitudeM: Double? = null, val ascentM: Double? = null,
    val descentM: Double? = null, val gradientPercent: Double? = null, val seaLevel: Boolean = false,
    val latitude: Double? = null, val longitude: Double? = null,
    val bearingDegrees: Double? = null,
    val solar: SolarValues = SolarValues(),
)

/** Conservative foreground trip estimates. Coordinates are held only as a distance anchor. */
class GpsRide(private var collecting: Boolean = true) {
    private var seenTime = -1L
    private var last: GpsFix? = null
    val lastObservationTimeMs get() = last?.timeMs
    private var anchor: GpsFix? = null
    private var clock: Long? = null
    private var hasFix = false
    private var speed: Double? = null
    private var distance = 0.0
    // Spatial continuity for live gradient/speed is independent of recorded trip totals.
    private var liveDistance = 0.0
    private var movingTime = 0L
    private var stoppedTime = 0L
    private var elapsed = 0L
    private var observedElapsed = 0L
    private var maximum = 0.0
    private var height: Double? = null
    private var heightReference: Double? = null
    private var heightTime: Long? = null
    private var heightDatum = false
    private val heights = ArrayDeque<Double>()
    private var ascent: Double? = null
    private var descent: Double? = null
    private val slope = ArrayDeque<Pair<Double, Double>>()
    private var gradient: Double? = null
    private var bearing: Double? = null
    var accepted = 0; private set
    var rejected = 0; private set

    fun pause() {
        last = null; anchor = null; clock = null; speed = null; bearing = null
        clearHeightContinuity()
    }

    fun reset() {
        pause(); seenTime = -1L; hasFix = false; distance = 0.0
        movingTime = 0; stoppedTime = 0; elapsed = 0; observedElapsed = 0; maximum = 0.0; liveDistance = 0.0
        height = null; ascent = null; descent = null; accepted = 0; rejected = 0
    }

    fun setCollecting(enabled: Boolean) {
        if (collecting == enabled) return
        collecting = enabled
        clock = null
    }

    private fun advance(now: Long) {
        if (!collecting) return
        clock?.let { if (now > it) elapsed += now - it }
        if (clock != null) clock = max(now, clock!!)
    }

    fun snapshot(now: Long): GpsValues {
        advance(now)
        val fresh = last?.let { now - it.timeMs in 0..5_000 } == true
        return GpsValues(
            if (fresh) speed?.times(3.6) else null, if (hasFix) distance else null,
            if (movingTime > 0) distance * 3600 / movingTime else if (hasFix) 0.0 else null,
            if (observedElapsed > 0) distance * 3600 / observedElapsed else if (hasFix) 0.0 else null,
            if (hasFix) maximum else null, if (hasFix) movingTime else null,
            if (hasFix) stoppedTime else null, if (hasFix) elapsed else null,
            if (fresh && heightTime?.let { now - it <= 5_000 } == true) height else null,
            ascent, descent, if (fresh) gradient else null, heightDatum,
            if (fresh) last?.latitude else null, if (fresh) last?.longitude else null,
            if (fresh) bearing else null,
        )
    }

    fun accept(fix: GpsFix, now: Long): Boolean {
        // Late, out-of-order and cached fixes must never change counters or revive old speed.
        if (fix.timeMs <= seenTime || now - fix.timeMs !in 0..5_000) { rejected++; return false }
        seenTime = fix.timeMs
        if (!fix.latitude.isFinite() || fix.latitude !in -90.0..90.0 ||
            !fix.longitude.isFinite() || fix.longitude !in -180.0..180.0 ||
            !fix.accuracyM.isFinite() || fix.accuracyM !in 0.0..20.0) {
            rejected++; breakFix(); return false
        }
        val previous = last
        val dt = previous?.let { (fix.timeMs - it.timeMs) / 1000.0 }
        if (previous != null && dt != null && dt <= 10 &&
            metres(previous, fix) > 35 * dt + max(previous.accuracyM, fix.accuracyM) + 5) {
            rejected++; breakFix(); return false
        }
        advance(fix.timeMs)
        if (collecting && clock == null) clock = fix.timeMs
        // Match the distance observation to its own timestamp, even after UI ticks or delivery delays.
        if (collecting) {
            observedElapsed = elapsed - ((clock ?: fix.timeMs) - fix.timeMs).coerceAtLeast(0)
            hasFix = true
        }
        accepted++
        val continuous = dt != null && dt in 0.001..10.0
        if (!continuous) { anchor = fix; speed = null; bearing = null; clearHeightContinuity() }

        val direct = fix.speedMps?.takeIf { it.isFinite() && it in 0.0..35.0 &&
            (fix.speedAccuracyMps == null || (fix.speedAccuracyMps.isFinite() && fix.speedAccuracyMps in 0.0..1.5)) &&
            (!continuous || speed == null || abs(it - speed!!) <= max(4.0, 6 * dt!!)) }
        val origin = anchor
        val displacement = origin?.let { metres(it, fix) } ?: 0.0
        val anchorSeconds = origin?.let { (fix.timeMs - it.timeMs) / 1000.0 } ?: 0.0
        // GNSS Doppler speed is preferred; position-only speed needs displacement above uncertainty.
        val derived = if (continuous && origin != null && anchorSeconds > 0 &&
            displacement >= max(4.0, origin.accuracyM + fix.accuracyM)) displacement / anchorSeconds else null
        val fallbackStopped = direct == null && derived == null && anchorSeconds >= 5
        val measured = direct ?: derived ?: if (fallbackStopped) 0.0 else speed ?: 0.0
        val moving = measured >= 0.7
        speed = if (!moving) 0.0 else {
            val old = speed
            if (old == null || !continuous) measured else old + (measured - old) *
                (1 - exp(-(if (direct == null && derived != null) anchorSeconds else dt!!) / 1.5))
        }
        if (collecting && continuous) maximum = max(maximum, speed!! * 3.6)
        if (continuous) {
            val interval = fix.timeMs - previous!!.timeMs
            if (collecting) { if (moving) movingTime += interval else stoppedTime += interval }
            if (moving && origin != null && (direct != null || derived != null) &&
                displacement >= max(2.5, (origin.accuracyM + fix.accuracyM) * 0.35)) {
                if (collecting) distance += displacement
                liveDistance += displacement
                anchor = fix
            } else if (!moving && (direct != null || fallbackStopped)) anchor = fix
        }
        if (moving) {
            val direction = fix.bearingDegrees?.takeIf { it.isFinite() && it in 0.0..360.0 &&
                (fix.bearingAccuracyDegrees?.let { accuracy -> accuracy.isFinite() && accuracy in 0.0..35.0 } == true) }
                ?: previous?.takeIf { continuous && metres(it, fix) >= max(5.0, it.accuracyM + fix.accuracyM) }?.let {
                    val a = Math.toRadians(it.latitude); val b = Math.toRadians(fix.latitude)
                    val dl = Math.toRadians(fix.longitude - it.longitude)
                    (Math.toDegrees(atan2(sin(dl) * cos(b), cos(a) * sin(b) - sin(a) * cos(b) * cos(dl))) + 360) % 360
                }
            direction?.let { next ->
                val old = bearing
                bearing = if (old == null || !continuous) next % 360 else
                    (old + (((next - old + 540) % 360) - 180) * .4 + 360) % 360
            }
        }
        last = fix
        updateHeight(fix, moving)
        return true
    }

    private fun breakFix() {
        last = null; anchor = null; speed = null; bearing = null; clearHeightContinuity()
    }

    private fun clearHeightContinuity() {
        heights.clear(); heightReference = null; heightTime = null; slope.clear(); gradient = null
    }

    private fun updateHeight(fix: GpsFix, moving: Boolean) {
        val raw = fix.altitudeM
        val accuracy = fix.verticalAccuracyM
        if (raw == null || !raw.isFinite() || raw !in -1000.0..10000.0 ||
            accuracy == null || !accuracy.isFinite() || accuracy !in 0.0..12.0) {
            clearHeightContinuity(); return
        }
        if (heightTime == null || heightDatum != fix.seaLevel) {
            clearHeightContinuity(); height = raw; heightDatum = fix.seaLevel
        }
        heights.addLast(raw)
        if (heights.size > 5) heights.removeFirst()
        val sorted = heights.sorted()
        val median = sorted[sorted.size / 2]
        val dt = heightTime?.let { (fix.timeMs - it) / 1000.0 } ?: 0.0
        if (dt > 0) height = height!! + (median - height!!) * (1 - exp(-dt / 6.0))
        heightTime = fix.timeMs
        val filtered = height!!
        if (collecting && ascent == null) { ascent = 0.0; descent = 0.0 }
        if (!moving) {
            heightReference = filtered; slope.clear(); gradient = null; return
        }
        val reference = heightReference
        if (reference == null) heightReference = filtered
        else if (abs(filtered - reference) >= max(3.0, accuracy * 0.5)) {
            if (collecting) {
                if (filtered > reference) ascent = ascent!! + filtered - reference
                else descent = descent!! + reference - filtered
            }
            heightReference = filtered
        }
        if (slope.lastOrNull()?.first != liveDistance) slope.addLast(liveDistance to filtered)
        while (slope.size > 2 && slope.elementAt(1).first <= liveDistance - 40) slope.removeFirst()
        while (slope.size > 100) slope.removeFirst()
        val start = slope.firstOrNull()
        gradient = if (start != null && liveDistance - start.first >= 30) {
            ((filtered - start.second) / (liveDistance - start.first) * 100).takeIf { it.isFinite() && abs(it) <= 50 }
        } else null
    }

    companion object {
        fun metres(a: GpsFix, b: GpsFix): Double {
            val lat = Math.toRadians(b.latitude - a.latitude)
            val lon = Math.toRadians(b.longitude - a.longitude)
            val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
                cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
            return 6_371_008.8 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
        }
    }
}

class BikeRide {
    private var previous: Long? = null
    var distanceM: Long? = null; private set
    fun update(odometer: Long?) {
        if (odometer == null) return
        previous?.let { if (odometer >= it) distanceM = (distanceM ?: 0) + odometer - it }
        if (distanceM == null) distanceM = 0
        previous = odometer
    }
    fun pause() { previous = null }
    fun reset(odometer: Long? = null) { previous = odometer; distanceM = if (odometer == null) null else 0 }
}

class RideSession(val gps: GpsRide = GpsRide(), val bike: BikeRide = BikeRide())
