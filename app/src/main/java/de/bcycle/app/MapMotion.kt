package de.bcycle.app

import kotlin.math.*

/** Display-only interpolation. Never feeds sensor processing or recording. */
internal class MapMotion {
    companion object { const val durationMs = 450L; const val frameMs = 33L }
    private var from = GpsValues()
    private var target = GpsValues()
    private var started = 0L
    var active = false; private set
    fun reset() { active = false; from = GpsValues(); target = GpsValues() }
    fun snap(value: GpsValues): GpsValues { active = false; from = value; target = value; return value }
    fun move(value: GpsValues, now: Long, animate: Boolean): GpsValues {
        val current = at(now)
        if (!animate || !valid(current) || !valid(value) || distance(current, value) > 200.0) return snap(value)
        if (value.latitude == target.latitude && value.longitude == target.longitude && value.bearingDegrees == target.bearingDegrees) {
            target = value; return at(now)
        }
        from = current; target = value; started = now; active = true
        return at(now)
    }
    fun at(now: Long): GpsValues {
        if (!active) return target
        val fraction = ((now - started).toDouble() / durationMs).coerceIn(0.0, 1.0)
        if (fraction >= 1.0) return snap(target)
        fun lerp(a: Double, b: Double) = a + (b - a) * fraction
        // Mercator latitude matches the map; longitude/bearing take the shortest arc.
        fun mercator(lat: Double) = ln(tan(PI / 4 + Math.toRadians(lat) / 2))
        val lat = Math.toDegrees(2 * atan(exp(lerp(mercator(from.latitude!!), mercator(target.latitude!!)))) - PI / 2)
        val lon = wrap(from.longitude!! + delta(from.longitude!!, target.longitude!!) * fraction)
        val bearing = target.bearingDegrees?.let { b -> from.bearingDegrees?.let { a -> (a + delta(a, b) * fraction + 360) % 360 } ?: b }
        return target.copy(latitude = lat, longitude = lon, bearingDegrees = bearing)
    }
    private fun valid(v: GpsValues) = v.latitude?.let { it.isFinite() && abs(it) <= 85.05112878 } == true &&
        v.longitude?.let { it.isFinite() && abs(it) <= 180 } == true
    private fun delta(a: Double, b: Double) = ((b - a + 540) % 360) - 180
    private fun wrap(v: Double) = ((v + 540) % 360) - 180
    private fun distance(a: GpsValues, b: GpsValues): Double {
        val lat = Math.toRadians(b.latitude!! - a.latitude!!)
        val lon = Math.toRadians(delta(a.longitude!!, b.longitude!!))
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude!!)) * cos(Math.toRadians(b.latitude!!)) * sin(lon / 2).pow(2)
        return 12_742_000 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
