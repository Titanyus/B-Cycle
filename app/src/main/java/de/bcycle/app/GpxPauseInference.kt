package de.bcycle.app

/** GPX has no FIT timer events. Infer only stationary gaps with explicit zero
 * rider power/cadence before them, low GPS speed and no more than 20 m displacement.
 * Moving/coasting gaps and missing sensor fields remain unknown data.
 */
internal class GpxPauseInference {
    data class Gap(val fromMs:Long,val toMs:Long) { val durationMs get()=toMs-fromMs }
    private var previous:GpxMeasurement?=null
    fun add(sample:GpxMeasurement):Gap? {
        val before=previous;previous=sample
        val start=before?.time ?: return null;val end=sample.time ?: return null
        if(end-start<=PerformanceEngine.MAX_HOLD_MS || before.power!=0.0 || before.cadence!=0.0) return null
        val speed=sample.speed ?: return null
        if(speed>=.7 || routeDistance(listOf(before.point,sample.point.copy(segmentStart=false)))>20.0) return null
        return Gap(start,end)
    }
}
