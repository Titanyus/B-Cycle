package de.bcycle.app

import kotlin.math.*

internal object LiveGraphStyle {
    const val INTERVAL_MS = 10_000L
    const val MAX_POINTS = 2048
    const val LINE_ALPHA = 140
    const val FILL_ALPHA = 30
    const val LINE_WIDTH_DP = 2.6f
    const val HEIGHT_FRACTION = .8f
}
internal data class GraphPoint(val utcMs: Long, val elapsedMs: Long, val distanceM: Double?,
    val values: List<Double?>, val breaks: Int = 0)
internal data class GraphRange(val low: Double, val high: Double)
internal data class GraphSeries(val points: List<GraphPoint>, val range: GraphRange)
internal data class GraphFrame(val version: Long, val distanceM: Double, val series: Map<AnalysisChart, GraphSeries>)

/** Expands only, using original values before display reduction or smoothing. */
internal class GraphScaleManager {
    private val ranges = mutableMapOf<AnalysisChart, GraphRange>()
    fun include(chart: AnalysisChart, value: Double) {
        val step = when(chart) { AnalysisChart.SPEED -> 10.0; AnalysisChart.CADENCE -> 20.0
            AnalysisChart.POWER -> 50.0; AnalysisChart.HEART -> 20.0; AnalysisChart.ELEVATION -> 25.0 }
        val initial = when(chart) { AnalysisChart.SPEED -> GraphRange(0.0,40.0); AnalysisChart.CADENCE -> GraphRange(0.0,120.0)
            AnalysisChart.POWER -> GraphRange(0.0,300.0); AnalysisChart.HEART -> GraphRange(60.0,180.0)
            AnalysisChart.ELEVATION -> (floor(value/step)*step).let { GraphRange(it-50,it+50) } }
        val old = ranges[chart] ?: initial
        val margin = (old.high-old.low)*.1
        var low = old.low; var high = old.high
        if(value <= low+margin) low = min(low-step, floor(((value-.1*high)/.9)/step)*step)
        if(chart != AnalysisChart.ELEVATION) low = max(0.0,low)
        if(value >= high-margin) high = max(high+step, ceil(((value-.1*low)/.9)/step)*step)
        ranges[chart] = GraphRange(low,high)
    }
    fun range(chart: AnalysisChart) = ranges[chart]
}

/** One bounded history for all tiles. Compaction keeps extrema, real zeros and gap boundaries. */
internal class GraphHistoryBuffer(private val capacity: Int = LiveGraphStyle.MAX_POINTS) {
    init { require(capacity >= 64) }
    val scales = GraphScaleManager()
    private val points = ArrayList<GraphPoint>()
    val size get() = points.size
    var distanceM = 0.0; private set
    private var pendingBreaks = 0
    private var lastElapsed: Long? = null
    fun append(input: GraphPoint) {
        val distance = input.distanceM?.takeIf { it.isFinite() && it >= distanceM }
        val timeGap = lastElapsed?.let { input.elapsedMs-it > PerformanceEngine.MAX_HOLD_MS || input.elapsedMs < it } ?: false
        lastElapsed = input.elapsedMs
        if(distance == null) { pendingBreaks = 31; return }
        val values = input.values.mapIndexed { i,v -> v?.takeIf { plausible(AnalysisChart.entries[i],it) } }
        val point = input.copy(distanceM=distance, values=values,
            breaks=input.breaks or pendingBreaks or if(timeGap) 31 else 0)
        pendingBreaks=0; distanceM=distance
        values.forEachIndexed { i,v -> if(v != null) scales.include(AnalysisChart.entries[i],v) }
        points += point
        if(points.size > capacity) compact()
    }
    private fun compact() {
        val reduced = ArrayList<GraphPoint>()
        for(chunk in points.chunked(32)) {
            val keep = sortedSetOf(0,chunk.lastIndex)
            for(i in AnalysisChart.entries.indices) {
                val valid = chunk.indices.filter { chunk[it].values[i] != null }
                if(valid.isNotEmpty()) {
                    keep += valid.minBy { chunk[it].values[i]!! }; keep += valid.maxBy { chunk[it].values[i]!! }
                    val zeros = valid.filter { chunk[it].values[i] == 0.0 }
                    if(zeros.isNotEmpty()) { keep += zeros.first(); keep += zeros.last() }
                }
                chunk.indexOfFirst { it.values[i] == null }.takeIf { it>=0 }?.let { keep += it }
            }
            var breaks=0
            chunk.forEachIndexed { index,p ->
                breaks = breaks or p.breaks
                p.values.forEachIndexed { i,v -> if(v==null) breaks=breaks or (1 shl i) }
                if(index in keep) { reduced += p.copy(breaks=p.breaks or breaks); breaks=0 }
            }
        }
        points.clear(); points.addAll(reduced)
    }
    fun frame(version: Long, width: Int): GraphFrame {
        val pixelWidth=width.coerceIn(64,4096)
        val series=AnalysisChart.entries.mapNotNull { chart -> scales.range(chart)?.let { range ->
            chart to GraphSeries(prepare(chart,pixelWidth),range)
        } }.toMap()
        return GraphFrame(version,distanceM,series)
    }
    private fun prepare(chart: AnalysisChart, width: Int): List<GraphPoint> {
        val i=chart.ordinal; val mask=1 shl i
        val result=ArrayList<GraphPoint>(); val segment=ArrayList<GraphPoint>()
        fun flush() {
            if(segment.isEmpty()) return
            // At an identical distance retain the last actual sample, never create distance.
            val unique=segment.groupBy { it.distanceM }.values.map { same -> same.last().copy(breaks=same.fold(0) { mask,p -> mask or p.breaks }) }
            val reduced=ArrayList<GraphPoint>()
            unique.groupBy { if(distanceM>0) floor(it.distanceM!!/distanceM*width).toInt() else 0 }.values.forEach { bucket ->
                val selected=sortedSetOf(0,bucket.lastIndex, bucket.indices.minBy { bucket[it].values[i]!! }, bucket.indices.maxBy { bucket[it].values[i]!! })
                bucket.indexOfFirst { it.values[i]==0.0 }.takeIf { it>=0 }?.let { selected += it }
                bucket.indexOfLast { it.values[i]==0.0 }.takeIf { it>=0 }?.let { selected += it }
                selected.forEach { reduced += bucket[it] }
            }
            // Gentle distance/pixel-adaptive median blend; convex, zero-preserving, gap-local.
            val radius=if(distanceM>0) distanceM/width*2 else 0.0
            reduced.forEachIndexed { j,p ->
                val original=p.values[i]!!
                val neighbors=listOfNotNull(reduced.getOrNull(j-1),p,reduced.getOrNull(j+1))
                val smooth=if(original==0.0 || neighbors.any { it.values[i]==0.0 } || neighbors.any { abs(it.distanceM!!-p.distanceM!!)>radius }) original
                    else original*.75 + neighbors.map { it.values[i]!! }.sorted()[neighbors.size/2]*.25
                result += p.copy(values=p.values.mapIndexed { k,v -> if(k==i) smooth else v })
            }
            segment.clear()
        }
        for(p in points) {
            if(p.breaks and mask != 0) flush()
            if(p.values[i]==null) { flush(); result += p } else {
                // Carry the discontinuity into the first retained point of the next segment.
                segment += p
            }
        }
        flush(); return result
    }
    companion object {
        fun plausible(chart: AnalysisChart, v: Double): Boolean = v.isFinite() && when(chart) {
            AnalysisChart.SPEED -> v in 0.0..126.0; AnalysisChart.CADENCE -> v in 0.0..254.0
            AnalysisChart.POWER -> v in 0.0..65534.0; AnalysisChart.HEART -> v in 1.0..254.0
            AnalysisChart.ELEVATION -> v in -500.0..10000.0
        }
    }
}

/** Adapt an existing immutable sample; the original journal, values and events stay untouched. */
internal fun graphPoint(sample: RideSample, config: RecordingConfig, continuedZero: Boolean,
    cadenceWasZero: Boolean): GraphPoint {
    val v=sample.values
    val power=v["RIDER_POWER"] ?: if(continuedZero) 0.0 else null
    val cadence=v["CADENCE"] ?: if(continuedZero && cadenceWasZero) 0.0 else null
    return GraphPoint(sample.utcMs,sample.elapsedMs,v[config.distanceSource],
        listOf(v[config.speedSource]?.times(3.6),cadence,power,v["HEART_RATE"],v["GPS_ALTITUDE"]))
}
