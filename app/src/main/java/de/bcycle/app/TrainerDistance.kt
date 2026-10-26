package de.bcycle.app

/** Ride-local metres: prefer the trainer counter, otherwise integrate observed speed pairs.
 * Source changes only establish an anchor; stale/missing speeds never extend a ride. */
internal class TrainerDistance {
    private var native=false
    private var previous:SourceReading?=null
    var meters:Double?=null;private set
    fun reset() { meters=null;pause() }
    fun pause() { previous=null }
    fun add(now:Long,counter:SourceReading?,speed:SourceReading?):Double? {
        fun valid(r:SourceReading?,max:Double)=r?.takeIf { it.value.isFinite() && it.value in 0.0..max && now-it.atMs in 0..5000 }
        val distance=valid(counter,Double.MAX_VALUE)
        val next=distance ?: valid(speed,126.0)
        if(next==null) { pause();return meters }
        val isNative=distance!=null
        if(previous?.let { native==isNative && next.atMs<it.atMs }==true) return meters
        previous?.takeIf { native==isNative }?.let { old ->
            if(isNative) {
                if(next.atMs>=old.atMs && next.value>=old.value) meters=(meters ?: 0.0)+next.value-old.value
            } else if(next.atMs-old.atMs in 1..5000) {
                meters=(meters ?: 0.0)+(old.value+next.value)/2.0/3.6*(next.atMs-old.atMs)/1000.0
            }
        }
        if(meters==null) meters=0.0
        native=isNative;previous=next
        return meters
    }
}
