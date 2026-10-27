package de.bcycle.app

/** GPS-derived speed is an estimate over a short path/time window, not a one-point derivative.
 * Repeated positions retain their elapsed time. Explicit segments, nonmonotone/missing time and
 * gaps over ten seconds reset the window. No synthetic points or changed timestamps.
 */
internal class GpxMotion {
    private data class Point(val time:Long,val distance:Double)
    private val window=ArrayDeque<Point>()
    private var previous:GpxMeasurement?=null
    private var distance=0.0
    fun speed(sample:GpxMeasurement):Double? {
        val before=previous;previous=sample
        val time=sample.time
        val dt=if(time!=null && before?.time!=null) time-before.time else null
        if(sample.point.segmentStart || dt==null || dt !in 1..10_000) { window.clear();distance=0.0 }
        else distance+=routeDistance(listOf(before!!.point,sample.point))
        if(time==null) return null
        window.addLast(Point(time,distance))
        while(window.size>1 && window.elementAt(1).time<=time-10_000) window.removeFirst()
        while(window.size>128) window.removeFirst()
        val duration=time-window.first().time
        val estimated=if(duration>0) (distance-window.first().distance)*1000/duration else 0.0
        return (sample.speed ?: estimated).takeIf { it.isFinite() && it in 0.0..35.0 }
    }
}
