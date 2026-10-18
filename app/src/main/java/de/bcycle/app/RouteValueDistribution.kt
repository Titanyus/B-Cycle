package de.bcycle.app

import kotlin.math.ceil
import kotlin.math.floor

internal data class DistributionBand(val zone: Int, val lower: Double, val upper: Double, val color: Int)
internal data class RouteValueDistribution(val chart: AnalysisChart, val minimum: Double, val maximum: Double,
    val milliseconds: List<Double>, val totalMs: Double, val bands: List<DistributionBand>, val observedMaximum: Double) {
    /** Visual-only five-tap smoothing. Non-negative, bounded storage; total time remains unchanged. */
    fun smoothed(): List<Double> {
        val kernel = listOf(1.0, 4.0, 6.0, 4.0, 1.0)
        var data = milliseconds
        repeat(2) {
            val previous = data
            data = previous.indices.map { i -> kernel.indices.sumOf { k -> previous.getOrElse(i + k - 2) { 0.0 } * kernel[k] / 16 } }
        }
        val sum = data.sum()
        return if (sum > 0) data.map { it * totalMs / sum } else data
    }
}

/** Integer-unit histogram before chart downsampling. FIT power/BPM are integer fields. */
internal class RouteTimeHistogram(private val maximumValue: Int) {
    private val milliseconds = DoubleArray(maximumValue + 1)
    private var observedMin: Double? = null
    private var observedMax: Double? = null
    fun observe(value: Double?) {
        if (value == null || !value.isFinite() || value !in 0.0..maximumValue.toDouble()) return
        observedMin = minOf(observedMin ?: value, value); observedMax = maxOf(observedMax ?: value, value)
    }
    fun add(value: Double?, durationMs: Long) {
        if (value == null || !value.isFinite() || value !in 0.0..maximumValue.toDouble() || durationMs <= 0) return
        milliseconds[value.toInt()] += durationMs.toDouble()
    }
    fun result(chart: AnalysisChart, profile: RiderProfile): RouteValueDistribution? {
        val total = milliseconds.sum().takeIf { it > 0 } ?: return null
        val observed = observedMax ?: return null
        val low: Double
        val high: Double
        val bands: List<DistributionBand>
        if (chart == AnalysisChart.POWER) {
            val ftp = profile.ftpW?.takeIf { Coggan.zones(it) != null } ?: return null
            low = 0.0
            // Show all seven zones, with usable room for open-ended Z7, and every observed peak.
            high = ceil(maxOf(observed, ftp * 1.75) / 5) * 5
            val edges = listOf(0.0) + Coggan.boundaries.map { ftp * it / 100.0 } + high
            bands = List(7) { i -> DistributionBand(i + 1, edges[i], edges[i + 1], ZoneGlowColors.powerColors[i]) }
        } else {
            require(chart == AnalysisChart.HEART)
            val model = profile.heartZones() ?: return null
            low = floor((if(model.openLastZone) minOf(observedMin!!,model.zones[1].lowerBpm-10.0) else minOf(observedMin!!,model.zones.first().lowerBpm.toDouble()))/5)*5
            high = ceil(maxOf(observed, if(model.openLastZone) model.zones.last().lowerBpm+10.0 else model.maximumHeartRate.toDouble()) / 5) * 5
            bands = model.zones.filterNot { it.isEmpty }.map { zone -> DistributionBand(zone.index, if(model.openLastZone && zone.index==1) low else zone.lowerBpm.toDouble(),
                if (zone.index == 5) { if(model.openLastZone) high else model.maximumHeartRate.toDouble() } else zone.upperExclusiveBpm.toDouble(), TrainingZoneColors.values[zone.index - 1]) }
        }
        val bins = DoubleArray(minOf(128, (high - low).toInt().coerceAtLeast(1)))
        milliseconds.forEachIndexed { value, ms -> if (ms > 0) {
            val index = ((value - low) / (high - low) * bins.size).toInt().coerceIn(bins.indices)
            bins[index] += ms
        } }
        return RouteValueDistribution(chart, low, high, bins.toList(), total, bands, observed)
    }
}
