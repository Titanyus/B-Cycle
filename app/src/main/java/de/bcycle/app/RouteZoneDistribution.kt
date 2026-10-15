package de.bcycle.app

/** Fractions of classified distance, independently normalized for each sensor. */
internal data class ZoneDistanceDistribution(val meters: List<Double>) {
    val totalMeters get() = meters.sum()
    val percentages get() = meters.map { if (totalMeters > 0) it * 100 / totalMeters else 0.0 }
}
internal data class RouteZoneDistributions(val power: ZoneDistanceDistribution? = null, val heart: ZoneDistanceDistribution? = null)

/** Streaming read-only analysis before chart reduction. Previous measurements describe the next
 * interval, with the existing five-second freshness limit. Never bridge gaps, timer pauses or resets.
 * Memory is bounded independently of recording length; the same intervals feed time histograms.
 */
internal class RouteZoneAccumulator(private val profile: RiderProfile) {
    private data class Sample(val at: Long, val distance: Double?, val powerZone: Int?, val heartZone: Int?, val watts: Double?, val bpm: Double?)
    private val heartModel = profile.heartZones()
    private val power = DoubleArray(7)
    private val heart = DoubleArray(5)
    private val powerTime = RouteTimeHistogram(65535)
    private val heartTime = RouteTimeHistogram(250)
    private var previous: Sample? = null
    private var paused = false
    fun timer(paused: Boolean) { this.paused = paused; previous = null }
    fun add(at: Long?, distance: Double?, watts: Double?, bpm: Double?, segmentStart: Boolean = false) {
        val old = previous
        val current = if (!paused && at != null) Sample(at, distance?.takeIf { it.isFinite() && it >= 0 },
            Coggan.zone(watts, profile.ftpW),
            bpm?.takeIf { it.isFinite() && it in 1.0..250.0 && it == it.toInt().toDouble() }?.let { heartModel?.zoneFor(it.toInt())?.index }, watts?.takeIf { it.isFinite() && it in 0.0..65535.0 }, bpm?.takeIf { it.isFinite() && it in 1.0..250.0 }) else null
        current?.let { powerTime.observe(it.watts); heartTime.observe(it.bpm) }
        if (!segmentStart && old != null && current != null && current.at - old.at in 1..PerformanceEngine.MAX_HOLD_MS) {
            val duration = current.at - old.at
            powerTime.add(old.watts, duration); heartTime.add(old.bpm, duration)
            val meters = current.distance?.let { end -> old.distance?.let { end - it } }
            if (meters != null && meters > 0 && meters.isFinite()) {
                old.powerZone?.let { power[it - 1] += meters }
                old.heartZone?.let { heart[it - 1] += meters }
            }
        }
        previous = current
    }
    fun timeDistributions(): List<RouteValueDistribution> = listOfNotNull(
        powerTime.result(AnalysisChart.POWER, profile), heartTime.result(AnalysisChart.HEART, profile))
    fun result(): RouteZoneDistributions {
        fun distribution(values: DoubleArray) = values.takeIf { it.sum() > 0 }?.let { ZoneDistanceDistribution(it.toList()) }
        return RouteZoneDistributions(distribution(power), distribution(heart))
    }
}
