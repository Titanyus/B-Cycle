package de.bcycle.app

import android.os.Handler
import android.os.SystemClock

/** Coalesce presentation work; samples and recording never pass through this gate. */
internal class DisplayUpdateGate(private val handler:Handler,private val clock:()->Long={SystemClock.uptimeMillis()},private val draw:()->Unit) {
    var intervalMs=0L
    private var last:Long?=null
    private var queued=false
    private val frame=Runnable { queued=false;last=clock();draw() }
    fun request(immediate:Boolean=false) {
        val now=clock();val delay=last?.let { (it+intervalMs-now).coerceAtLeast(0) } ?: 0
        if(immediate || intervalMs<=0 || delay==0L) {
            handler.removeCallbacks(frame);queued=false;last=now;draw()
        } else if(!queued) { queued=true;handler.postDelayed(frame,delay) }
    }
    fun cancel() { handler.removeCallbacks(frame);queued=false;last=null }
}
