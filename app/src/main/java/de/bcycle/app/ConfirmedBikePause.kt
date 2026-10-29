package de.bcycle.app

/** NP-only rest evidence. Neither a BLE loss alone nor an old zero speed proves a pause. */
internal class ConfirmedBikePause {
    companion object {
        const val FLAG="BCYCLE_NP_PAUSED"
        const val CONFIRM_MS=10_000L
    }
    private var stillSince:Long?=null
    private var lastChecked:Long?=null
    fun reset() { stillSince=null;lastChecked=null }
    fun update(now:Long,eligible:Boolean,bikeConnected:Boolean,powerAvailable:Boolean,
               gpsSpeedMps:Double?,fixAt:Long?,accuracyM:Double?):Boolean {
        if(lastChecked?.let { now-it !in 0..PerformanceEngine.MAX_HOLD_MS }==true) stillSince=null
        lastChecked=now
        val stationary=eligible && fixAt!=null && now-fixAt in 0..PerformanceEngine.MAX_HOLD_MS &&
            accuracyM!=null && accuracyM.isFinite() && accuracyM in 0.0..25.0 &&
            gpsSpeedMps!=null && gpsSpeedMps.isFinite() && gpsSpeedMps in 0.0..<.7
        if(!stationary) { stillSince=null;return false }
        if(stillSince==null || now<stillSince!!) stillSince=now
        return !bikeConnected && !powerAvailable && now-stillSince!!>=CONFIRM_MS
    }
}

/** Live and journal replay consume identical evidence without changing the recording timer. */
internal fun PerformanceEngine.acceptSample(sample:RideSample) {
    sample.performanceEvents.forEach(::accept)
    sample.values[ConfirmedBikePause.FLAG]?.takeIf { it==0.0 || it==1.0 }?.let {
        accept(PerformanceEvent(PerformanceChannel.NP_PAUSE,sample.elapsedMs,it))
    }
    advance(sample.elapsedMs)
}
