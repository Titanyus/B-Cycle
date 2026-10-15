package de.bcycle.app

import kotlin.math.abs
import kotlin.math.sqrt

internal data class RouteCurvePoint(val x: Double, val value: Double)

/** Display-only, gap-local smoothing of the already bounded analysis series. At most nine nearby
 * observations participate; the radius represents two plot pixels, independently of ride length.
 */
internal object RouteChartCurve {
    fun prepare(series: AnalysisSeries, distanceAxis: Boolean, end: Double, pixels: Float): List<List<RouteCurvePoint>> {
        val segments = mutableListOf<MutableList<RouteCurvePoint>>()
        var current: MutableList<RouteCurvePoint>? = null
        for (point in series.points) {
            val x = if (distanceAxis) point.distance?.div(1000) else point.seconds
            val value = point.value
            if (x == null || !x.isFinite() || x !in 0.0..end || value == null || !value.isFinite()) { current = null; continue }
            if (current == null || x < current.last().x) {
                current = mutableListOf(); segments += current
            }
            current.add(RouteCurvePoint(x, value))
        }
        val values = segments.flatten().map { it.value }
        val range = (values.maxOrNull() ?: 0.0) - (values.minOrNull() ?: 0.0)
        val radius = end / pixels.coerceAtLeast(1f) * 2
        return segments.map { points -> points.mapIndexed { i, point ->
            val neighbors = (maxOf(0, i - 4)..minOf(points.lastIndex, i + 4)).map { points[it] }
                .filter { abs(it.x - point.x) <= radius }
            val sorted = neighbors.map { it.value }.sorted()
            val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2] else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
            // Preserve real zeros and their transition edges, endpoints and pronounced peaks/dips.
            val preserve = i == 0 || i == points.lastIndex || neighbors.any { it.value == 0.0 } || abs(point.value - median) > maxOf(range * .12, abs(median) * .05)
            if (preserve) point else point.copy(value = point.value * .55 + median * .45)
        } }
    }

    /** Monotone cubic Hermite tangents: no interpolation beyond adjacent endpoint values. */
    fun tangents(points: List<RouteCurvePoint>): DoubleArray {
        if (points.size < 2) return DoubleArray(points.size)
        val d = points.zipWithNext { a, b -> if (b.x > a.x) (b.value - a.value) / (b.x - a.x) else 0.0 }
        val m = DoubleArray(points.size)
        m[0] = d.first(); m[m.lastIndex] = d.last()
        for (i in 1 until m.lastIndex) {
            val a = d[i - 1]; val b = d[i]
            m[i] = if (a * b > 0) 2 * a * b / (a + b) else 0.0
        }
        d.forEachIndexed { i, slope ->
            if (slope == 0.0) { m[i] = 0.0; m[i + 1] = 0.0 }
            else {
                val a = m[i] / slope; val b = m[i + 1] / slope
                val norm = sqrt(a * a + b * b)
                if (norm > 3) { val factor = 3 / norm; m[i] *= factor; m[i + 1] *= factor }
            }
        }
        return m
    }
}
