package de.bcycle.app

/** Fractions of classified distance, independently normalized for each sensor. */
internal data class ZoneDistanceDistribution(val meters: List<Double>) {
    val totalMeters get() = meters.sum()
    val percentages get() = meters.map { if (totalMeters > 0) it * 100 / totalMeters else 0.0 }
}
internal data class RouteZoneDistributions(val power: ZoneDistanceDistribution? = null, val heart: ZoneDistanceDistribution? = null)

/** Streaming read-only analysis before chart reduction. Previous measurements describe the next
 * interval, with the existing five-second freshness limit. Never bridge gaps, timer pauses or resets.
 * Memory is constant (seven plus five totals), independent of recording length.
 */
internal class RouteZoneAccumulator(private val profile: RiderProfile) {
    private data class Sample(val at: Long, val distance: Double, val powerZone: Int?, val heartZone: Int?)
    private val heartModel = profile.heartZones()
    private val power = DoubleArray(7)
    private val heart = DoubleArray(5)
    private var previous: Sample? = null
    private var paused = false
    fun timer(paused: Boolean) { this.paused = paused; previous = null }
    fun add(at: Long?, distance: Double?, watts: Double?, bpm: Double?, segmentStart: Boolean = false) {
        val old = previous
        val current = if (!paused && at != null && distance != null && distance.isFinite() && distance >= 0) Sample(at, distance,
            Coggan.zone(watts, profile.ftpW),
            bpm?.takeIf { it.isFinite() && it in 1.0..250.0 && it == it.toInt().toDouble() }?.let { heartModel?.zoneFor(it.toInt())?.index }) else null
        if (!segmentStart && old != null && current != null && current.at - old.at in 1..PerformanceEngine.MAX_HOLD_MS) {
            val meters = current.distance - old.distance
            if (meters > 0 && meters.isFinite()) {
                old.powerZone?.let { power[it - 1] += meters }
                old.heartZone?.let { heart[it - 1] += meters }
            }
        }
        previous = current
    }
    fun result(): RouteZoneDistributions {
        fun distribution(values: DoubleArray) = values.takeIf { it.sum() > 0 }?.let { ZoneDistanceDistribution(it.toList()) }
        return RouteZoneDistributions(distribution(power), distribution(heart))
    }
}
