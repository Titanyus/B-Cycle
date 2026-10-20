package de.bcycle.hrv

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Adapted from NeuroKit2 signal_fixpeaks (MIT, see THIRD_PARTY_NOTICES.md).
 * Reference SHA ff419d983568ef492eb8d229af643c0ef0100b32, integer peaks at 1024 Hz.
 * This is a reference-compatible profile, not certification by Kubios.
 */
object ArtifactCorrection {
    const val VERSION = "nk-ff419d9-int1024-v1"
    const val REFERENCE_SHA = "ff419d983568ef492eb8d229af643c0ef0100b32"
    data class Classes(val ectopic: List<Int>, val missed: List<Int>, val extra: List<Int>, val longshort: List<Int>) {
        val count get() = ectopic.size + missed.size + extra.size + longshort.size
    }
    data class Pass(val classes: Classes, val beforePeaks: List<Long>, val afterPeaks: List<Long>)
    data class Result(val peaks: List<Long>, val passes: List<Pass>, val residual: Classes,
                      val affectedRawIndices: List<Int>) {
        val nnMs get() = peaks.zipWithNext { a, b -> (b - a) * 1000.0 / 1024 }
    }

    fun correct(rrTicks: List<Int>): Result {
        require(rrTicks.size in 3..4096 && rrTicks.all { it in 1..65535 })
        val original = mutableListOf(0L)
        rrTicks.forEach { original += original.last() + it }
        var peaks: List<Long> = original.toList()
        var classes = classify(peaks)
        val passes = mutableListOf<Pass>()
        // Match NeuroKit's first pass and strictly decreasing accepted subsequent counts.
        var previousCount = Int.MAX_VALUE
        while (passes.isEmpty() || classes.count < previousCount) {
            check(passes.size < 32) { "Correction iteration limit" }
            val next = apply(peaks, classes)
            passes += Pass(classes, peaks, next)
            previousCount = classes.count
            peaks = next
            classes = classify(peaks)
        }
        require(peaks.zipWithNext().all { (a, b) -> b > a }) { "Non-monotonic corrected peaks" }
        val kept = peaks.toHashSet()
        val affected = rrTicks.indices.filter { i ->
            original[i] !in kept || original[i + 1] !in kept ||
                peaks.any { it > original[i] && it < original[i + 1] }
        }
        return Result(peaks, passes, classes, affected)
    }

    fun classify(peaks: List<Long>): Classes {
        require(peaks.size >= 3)
        val rr = DoubleArray(peaks.size) { i -> if (i == 0) 0.0 else (peaks[i] - peaks[i - 1]) / 1024.0 }
        rr[0] = rr.drop(1).average()
        val drr = DoubleArray(rr.size) { i -> if (i == 0) 0.0 else rr[i] - rr[i - 1] }
        drr[0] = drr.drop(1).average()
        val th1 = thresholds(drr)
        drr.indices.forEach { i -> drr[i] = if (th1[i] == 0.0) Double.NaN else drr[i] / th1[i] }
        fun reflected(index: Int): Double = when {
            index < 0 -> drr[-index]
            index >= drr.size -> drr[2 * drr.size - 2 - index]
            else -> drr[index]
        }
        val s12 = DoubleArray(rr.size) { i -> when {
            drr[i] > 0 -> max(reflected(i - 1), reflected(i + 1))
            drr[i] < 0 -> min(reflected(i - 1), reflected(i + 1))
            else -> 0.0
        } }
        val s22 = DoubleArray(rr.size) { i -> when {
            drr[i] >= 0 -> min(reflected(i + 1), reflected(i + 2))
            drr[i] < 0 -> max(reflected(i + 1), reflected(i + 2))
            else -> 0.0
        } }
        val medrr = DoubleArray(rr.size) { i -> quantile(window(rr, i, 5), 0.5) }
        val mrr = DoubleArray(rr.size) { i -> (rr[i] - medrr[i]).let { if (it < 0) it * 2 else it } }
        val th2 = thresholds(mrr)
        mrr.indices.forEach { i -> mrr[i] = if (th2[i] == 0.0) Double.NaN else mrr[i] / th2[i] }
        val ectopic = mutableListOf<Int>(); val missed = mutableListOf<Int>()
        val extra = mutableListOf<Int>(); val longshort = mutableListOf<Int>()
        var i = 0
        while (i < rr.size - 2) {
            if (abs(drr[i]) <= 1) { i++; continue }
            if ((drr[i] > 1 && s12[i] < -0.13 * drr[i] - 0.17) ||
                (drr[i] < -1 && s12[i] > -0.13 * drr[i] + 0.17)) {
                ectopic += i; i++; continue
            }
            if (!(abs(drr[i]) > 1 || abs(mrr[i]) > 3)) { i++; continue }
            val candidates = mutableListOf(i)
            if (abs(drr[i + 1]) < abs(drr[i + 2])) candidates += i + 1
            for (j in candidates) {
                val long = drr[j] > 1 && s22[j] < -1
                val short = drr[j] < -1 && s22[j] > 1
                if (!(long || abs(mrr[j]) > 3 || short)) { i++; continue }
                when {
                    short && abs(rr[j] + rr[j + 1] - medrr[j]) < th2[j] -> extra += j
                    long && abs(rr[j] / 2 - medrr[j]) < th2[j] -> missed += j
                    else -> longshort += j
                }
                i++
            }
        }
        return Classes(ectopic, missed, extra, longshort)
    }

    private fun window(values: DoubleArray, i: Int, radius: Int): List<Double> =
        (max(0, i - radius)..min(values.lastIndex, i + radius)).map { values[it] }

    private fun quantile(values: List<Double>, p: Double): Double {
        val sorted = values.sorted(); val index = (sorted.size - 1) * p
        val lo = index.toInt(); val fraction = index - lo
        return sorted[lo] + fraction * (sorted[min(lo + 1, sorted.lastIndex)] - sorted[lo])
    }

    private fun thresholds(values: DoubleArray): DoubleArray = DoubleArray(values.size) { i ->
        val samples = window(values, i, 45).map(::abs)
        5.2 * ((quantile(samples, 0.75) - quantile(samples, 0.25)) / 2)
    }

    private fun update(source: List<Int>, indices: List<Int>, delta: Int): List<Int> {
        var result = indices
        source.forEach { s -> result = result.map { if (it > s) it + delta else it } }
        return result.distinct().sorted()
    }

    private fun apply(input: List<Long>, c: Classes): List<Long> {
        var peaks = input
        var missed = c.missed; var ectopic = c.ectopic; var longshort = c.longshort
        if (c.extra.isNotEmpty()) {
            peaks = peaks.filterIndexed { i, _ -> i !in c.extra }
            missed = update(c.extra, missed, -1)
            ectopic = update(c.extra, ectopic, -1)
            longshort = update(c.extra, longshort, -1)
        }
        if (missed.isNotEmpty()) {
            val valid = missed.filter { it > 1 && it < peaks.size }
            peaks = buildList { peaks.forEachIndexed { i, p ->
                if (i in valid) add(peaks[i - 1] + (p - peaks[i - 1]) / 2)
                add(p)
            } }
            // Deliberately mirrors the reference's index update, including rejected edge indices.
            ectopic = update(missed, ectopic, 1)
            longshort = update(missed, longshort, 1)
        }
        fun move(indices: List<Int>) {
            val valid = indices.filter { it > 1 && it < peaks.lastIndex }
            val replacements = valid.map { peaks[it - 1] + (peaks[it + 1] - peaks[it - 1]) / 2 }
            peaks = (peaks.filterIndexed { i, _ -> i !in valid } + replacements).sorted()
        }
        if (ectopic.isNotEmpty()) move(ectopic)
        if (longshort.isNotEmpty()) move(longshort)
        return peaks
    }
}
