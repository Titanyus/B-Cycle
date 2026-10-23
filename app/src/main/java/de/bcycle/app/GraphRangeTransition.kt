package de.bcycle.app

/** Expand immediately to retain peaks; return to the visible-window range smoothly over three seconds. */
internal class GraphRangeTransition {
    private var from:GraphRange?=null
    private var target:GraphRange?=null
    private var started=0L
    fun value(now:Long):GraphRange {
        val end=checkNotNull(target);val start=checkNotNull(from)
        val t=((now-started).toDouble()/LiveGraphStyle.SCALE_RETURN_MS).coerceIn(0.0,1.0);val smooth=t*t*(3-2*t)
        return GraphRange(start.low+(end.low-start.low)*smooth,start.high+(end.high-start.high)*smooth)
    }
    fun update(next:GraphRange,now:Long):GraphRange {
        if(target==null) { from=next;target=next;started=now-LiveGraphStyle.SCALE_RETURN_MS }
        else if(target!=next) {
            val current=value(now)
            from=GraphRange(minOf(current.low,next.low),maxOf(current.high,next.high));target=next;started=now
        }
        return value(now)
    }
    fun running(now:Long)=from!=target && now-started<LiveGraphStyle.SCALE_RETURN_MS
}
