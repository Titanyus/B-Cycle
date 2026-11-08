package de.bcycle.app

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException

/** Display worker only: consumes the existing recording tick, never subscribes to sensors. */
internal class LiveGraphProvider(private val changed: () -> Unit) {
    private val handler=Handler(Looper.getMainLooper())
    private val worker=ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,ArrayBlockingQueue(16),
        { task -> Thread(task,"BicycleGraphDisplay").apply { isDaemon=true } })
    @Volatile var frame: GraphFrame? = null; private set
    private val generationLock=Any()
    @Volatile private var generation=0L
    @Volatile var rolling=false
    @Volatile private var powerSaving=false
    @Volatile private var displayVisible=true
    @Volatile private var demand:Map<GraphWindow,Set<AnalysisChart>> = emptyMap()
    private var preparedDemand:Map<GraphWindow,Set<AnalysisChart>>?=null
    private var policyChanged=false
    fun applyRuntimePolicy(saving:Boolean,foreground:Boolean) {
        if(powerSaving!=saving) { powerSaving=saving;policyChanged=true }
        if(!foreground) displayVisible=false
    }
    internal val interval get()=(if(rolling) 1000L else LiveGraphStyle.INTERVAL_MS).let { if(powerSaving) maxOf(it,5000L) else it }
    fun configureDisplay(saving:Boolean,visible:Boolean,requested:Map<GraphWindow,Set<AnalysisChart>>) {
        val changed=policyChanged || powerSaving!=saving || displayVisible!=visible || demand!=requested
        if(!changed) return
        policyChanged=false;powerSaving=saving;displayVisible=visible;demand=requested.mapValues { it.value.toSet() }
        if(visible) {
            val ride=generation
            try { worker.execute { if(ride==generation && hasSamples && displayVisible) publish(ride,forceFull=true) } } catch(_:RejectedExecutionException) {}
        }
    }
    fun hideDisplay() { displayVisible=false }
    private var lastFullWall=-LiveGraphStyle.INTERVAL_MS
    @Volatile var width=640
    private var history=GraphHistoryBuffer()
    private var lastPublishElapsed=-LiveGraphStyle.INTERVAL_MS
    private var lastPublishWall=-LiveGraphStyle.INTERVAL_MS
    private var sequence=0L
    private var overflow=false
    @Volatile private var hasSamples=false
    private var active=false
    private var finishing: Runnable?=null
    fun begin() {
        active=true;hasSamples=false
        finishing?.let(handler::removeCallbacks); finishing=null
        synchronized(generationLock) { generation++; frame=null }; worker.queue.clear(); overflow=false
        val ride=generation
        worker.execute { if(ride==generation) { history=GraphHistoryBuffer(); lastPublishElapsed=-LiveGraphStyle.INTERVAL_MS; lastPublishWall=-LiveGraphStyle.INTERVAL_MS;lastFullWall=-LiveGraphStyle.INTERVAL_MS } }
    }
    fun append(point: GraphPoint) {
        if(!active) return
        val ride=generation; val input=if(overflow) point.copy(breaks=31) else point
        try { worker.execute {
            if(ride==generation) {
                history.append(input);hasSamples=true
                if(displayVisible && point.elapsedMs-lastPublishElapsed >= interval && SystemClock.uptimeMillis()-lastPublishWall >= interval) {
                    lastPublishElapsed=point.elapsedMs; publish(ride)
                }
            }
        }; overflow=false } catch(_:RejectedExecutionException) { overflow=true }
    }
    private fun publish(ride: Long,forceFull:Boolean=false) {
        val now=SystemClock.uptimeMillis()
        val requested=if(powerSaving) demand else null
        val refreshFull=forceFull || now-lastFullWall>=LiveGraphStyle.INTERVAL_MS || preparedDemand!=requested
        val next=history.frame(++sequence,width,if(refreshFull) null else frame,requested)
        preparedDemand=requested
        if(refreshFull) lastFullWall=now
        synchronized(generationLock) {
            if(ride!=generation) return
            lastPublishWall=SystemClock.uptimeMillis(); frame=next
        }
        handler.post { if(ride==generation) changed() }
    }
    fun stop() {
        if(!active) return
        active=false
        val ride=generation
        // One final display flush, respecting the same ten-second publication limit.
        try { worker.execute {
            if(ride==generation) {
                val delay=(interval-(SystemClock.uptimeMillis()-lastPublishWall)).coerceAtLeast(0)
                val finish=Runnable { try { worker.execute { if(ride==generation) publish(ride,forceFull=true) } } catch(_:RejectedExecutionException) {} }
                handler.post { if(ride==generation) { finishing=finish; handler.postDelayed(finish,delay) } }
            }
        } } catch(_:RejectedExecutionException) {}
    }
}
