package de.bcycle.app

import kotlin.math.*

internal object LiveGraphStyle {
    const val INTERVAL_MS = 10_000L
    const val MAX_POINTS = 2048
    const val LINE_ALPHA = 225
    const val FILL_ALPHA = 117
    const val LINE_WIDTH_DP = 2.6f
    const val MARKER_DIAMETER_DP = 7f
    const val SCALE_RETURN_MS = 3000L
    fun lineAlpha(mode: GraphDisplayMode)=if(mode==GraphDisplayMode.WITH_DATA) (LINE_ALPHA*.7f).toInt() else LINE_ALPHA
    fun fillAlpha(mode: GraphDisplayMode)=if(mode==GraphDisplayMode.WITH_DATA) (FILL_ALPHA*.85f).toInt() else FILL_ALPHA
    const val HEIGHT_FRACTION = .8f
}
internal data class GraphPoint(val utcMs: Long, val elapsedMs: Long, val distanceM: Double?,
    val values: List<Double?>, val breaks: Int = 0)
internal data class GraphRange(val low: Double, val high: Double)
internal data class GraphSeries(val points: List<GraphPoint>, val range: GraphRange)
internal data class GraphFrame(val version: Long, val distanceM: Double, val series: Map<AnalysisChart, GraphSeries>, val elapsedMs:Long=0,
    val windows:Map<GraphWindow,Map<AnalysisChart,GraphSeries>> = emptyMap(),val timeAxis:Boolean=false)

/** Expands only, using original values before display reduction or smoothing. */
internal class GraphScaleManager(private val baseline:Map<AnalysisChart,GraphRange> = emptyMap()) {
    private val ranges = mutableMapOf<AnalysisChart, GraphRange>()
    fun include(chart: AnalysisChart, value: Double) {
        val step = when(chart) { AnalysisChart.SPEED -> 5.0; AnalysisChart.CADENCE -> 10.0
            AnalysisChart.POWER -> 30.0; AnalysisChart.HEART -> 20.0; AnalysisChart.ELEVATION -> 25.0 }
        val initial = baseline[chart] ?: initial(chart,value)
        val old = ranges[chart] ?: initial
        val margin = (old.high-old.low)*.1
        var low = old.low; var high = old.high
        if(value <= low+margin) low = min(low-step, floor(((value-.1*high)/.9)/step)*step)
        if(chart != AnalysisChart.ELEVATION) low = max(0.0,low)
        if(value >= high-margin) high = max(high+step, ceil(((value-.1*low)/.9)/step)*step)
        ranges[chart] = GraphRange(low,high)
    }
    companion object {
        fun initial(chart:AnalysisChart,value:Double):GraphRange=when(chart) { AnalysisChart.SPEED -> GraphRange(0.0,20.0); AnalysisChart.CADENCE -> GraphRange(0.0,60.0)
            AnalysisChart.POWER -> GraphRange(0.0,120.0); AnalysisChart.HEART -> GraphRange(50.0,100.0)
            AnalysisChart.ELEVATION -> (round(value/25)*25).let { GraphRange(it-25,it+25) } }
    }
    fun range(chart: AnalysisChart) = ranges[chart]
}

/** One bounded history for all tiles. Compaction keeps extrema, real zeros and gap boundaries. */
internal class GraphHistoryBuffer(private val capacity: Int = LiveGraphStyle.MAX_POINTS) {
    init { require(capacity >= 64) }
    val scales = GraphScaleManager()
    private val initialRanges=mutableMapOf<AnalysisChart,GraphRange>()
    private val points = ArrayList<GraphPoint>()
    private val recent=java.util.ArrayDeque<GraphPoint>()
    val size get() = points.size
    var distanceM = 0.0; private set
    private var pendingBreaks = 0
    private var lastElapsed: Long? = null
    fun append(input: GraphPoint) {
        val distance = input.distanceM?.takeIf { it.isFinite() && it >= distanceM }
        val timeGap = lastElapsed?.let { input.elapsedMs-it > PerformanceEngine.MAX_HOLD_MS || input.elapsedMs < it } ?: false
        lastElapsed = input.elapsedMs
        val values = input.values.mapIndexed { i,v -> v?.takeIf { plausible(AnalysisChart.entries[i],it) } }
        val timed=input.copy(values=values,breaks=input.breaks or if(timeGap) 31 else 0)
        recent.addLast(timed)
        while(recent.size>LiveGraphStyle.MAX_POINTS || recent.size>2 && recent.elementAt(1).elapsedMs<input.elapsedMs-120000) recent.removeFirst()
        values.forEachIndexed { i,v -> if(v!=null) {
            val chart=AnalysisChart.entries[i];initialRanges.putIfAbsent(chart,GraphScaleManager.initial(chart,v));scales.include(chart,v)
        } }
        if(distance==null && (input.distanceM!=null || distanceM>0)) { pendingBreaks=31;return }
        val point = input.copy(distanceM=distance, values=values,
            breaks=input.breaks or pendingBreaks or if(timeGap) 31 else 0)
        pendingBreaks=0; if(distance!=null) distanceM=distance
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
    fun frame(version: Long, width: Int, cachedFull:GraphFrame?=null): GraphFrame {
        val pixelWidth=width.coerceIn(64,4096)
        val series=cachedFull?.series ?: AnalysisChart.entries.mapNotNull { chart -> scales.range(chart)?.let { range ->
            chart to GraphSeries(prepare(chart,pixelWidth),range)
        } }.toMap()
        val elapsed=recent.peekLast()?.elapsedMs ?: 0
        val windows=GraphWindow.entries.filter { it!=GraphWindow.FULL }.associateWith { window ->
            val start=(elapsed-window.milliseconds).coerceAtLeast(0)
            val history=recent.toList()
            val first=(history.indexOfFirst { it.elapsedMs>=start }.let { if(it<0) history.size else it }-1).coerceAtLeast(0)
            val selected=history.drop(first)
            val windowScales=GraphScaleManager(initialRanges)
            selected.filter { it.elapsedMs>=start }.forEach { point -> point.values.forEachIndexed { i,value ->
                if(value!=null) windowScales.include(AnalysisChart.entries[i],value)
            } }
            AnalysisChart.entries.mapNotNull { chart -> (windowScales.range(chart) ?: initialRanges[chart])?.let { chart to GraphSeries(selected,it) } }.toMap()
        }
        return GraphFrame(version,distanceM,series,elapsed,windows,points.none { it.distanceM!=null })
    }
    private fun prepare(chart: AnalysisChart, width: Int): List<GraphPoint> {
        val i=chart.ordinal; val mask=1 shl i
        val time=points.none { it.distanceM!=null }
        fun position(p:GraphPoint)=if(time) p.elapsedMs.toDouble() else p.distanceM!!
        val extent=if(time) (points.lastOrNull()?.elapsedMs ?: 0).toDouble() else distanceM
        val result=ArrayList<GraphPoint>(); val segment=ArrayList<GraphPoint>()
        fun flush() {
            if(segment.isEmpty()) return
            // At an identical distance retain the last actual sample, never create distance.
            val unique=segment.groupBy { position(it) }.values.map { same -> same.last().copy(breaks=same.fold(0) { mask,p -> mask or p.breaks }) }
            val reduced=ArrayList<GraphPoint>()
            unique.groupBy { if(extent>0) floor(position(it)/extent*width).toInt() else 0 }.values.forEach { bucket ->
                val selected=sortedSetOf(0,bucket.lastIndex, bucket.indices.minBy { bucket[it].values[i]!! }, bucket.indices.maxBy { bucket[it].values[i]!! })
                bucket.indexOfFirst { it.values[i]==0.0 }.takeIf { it>=0 }?.let { selected += it }
                bucket.indexOfLast { it.values[i]==0.0 }.takeIf { it>=0 }?.let { selected += it }
                selected.forEach { reduced += bucket[it] }
            }
            // Gentle distance/pixel-adaptive median blend; convex, zero-preserving, gap-local.
            val radius=if(extent>0) extent/width*2 else 0.0
            reduced.forEachIndexed { j,p ->
                val original=p.values[i]!!
                val neighbors=listOfNotNull(reduced.getOrNull(j-1),p,reduced.getOrNull(j+1))
                val smooth=if(original==0.0 || neighbors.any { it.values[i]==0.0 } || neighbors.any { abs(position(it)-position(p))>radius }) original
                    else original*.75 + neighbors.map { it.values[i]!! }.sorted()[neighbors.size/2]*.25
                result += p.copy(values=p.values.mapIndexed { k,v -> if(k==i) smooth else v })
            }
            segment.clear()
        }
        for(p in points) {
            if(!time && p.distanceM==null) { flush();continue }
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
