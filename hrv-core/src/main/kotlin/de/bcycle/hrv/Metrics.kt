package de.bcycle.hrv

import kotlin.math.ln
import kotlin.math.sqrt

data class Metrics(
    val count: Int,
    val meanNnMs: Double,
    val rmssdMs: Double,
    val lnRmssd: Double?,
    val sdnnMs: Double,
    val nn50Count: Int,
    val pnn50Percent: Double,
    val restingHrBpm: Double,
) {
    val adjacentPairCount get() = count - 1
    // The pinned NeuroKit2 hrv_time uses N, rather than N-1, for this metric.
    val pnn50NeurokitPercent get() = nn50Count * 100.0 / count
    companion object {
        /** Input is ONE continuous NN series. No gap joining or preprocessing here. */
        fun fromNnMs(nn: List<Double>): Metrics {
            require(nn.size >= 2 && nn.all { it.isFinite() && it > 0 })
            val mean = nn.average()
            val diffs = nn.zipWithNext { a, b -> b - a }
            val rmssd = sqrt(diffs.sumOf { it * it } / diffs.size)
            val nn50 = diffs.count { kotlin.math.abs(it) > 50.0 }
            return Metrics(nn.size, mean, rmssd, rmssd.takeIf { it > 0 }?.let(::ln),
                sqrt(nn.sumOf { (it - mean) * (it - mean) } / (nn.size - 1)),
                nn50, nn50 * 100.0 / diffs.size,
                60_000.0 / mean)
        }
    }
}
