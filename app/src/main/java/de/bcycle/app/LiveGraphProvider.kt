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
    @Volatile var width=640
    private var history=GraphHistoryBuffer()
    private var lastPublishElapsed=-LiveGraphStyle.INTERVAL_MS
    private var lastPublishWall=-LiveGraphStyle.INTERVAL_MS
    private var sequence=0L
    private var overflow=false
    private var active=false
    private var finishing: Runnable?=null
    fun begin() {
        active=true
        finishing?.let(handler::removeCallbacks); finishing=null
        synchronized(generationLock) { generation++; frame=null }; worker.queue.clear(); overflow=false
        val ride=generation
        worker.execute { if(ride==generation) { history=GraphHistoryBuffer(); lastPublishElapsed=-LiveGraphStyle.INTERVAL_MS; lastPublishWall=-LiveGraphStyle.INTERVAL_MS } }
    }
    fun append(point: GraphPoint) {
        if(!active) return
        val ride=generation; val input=if(overflow) point.copy(breaks=31) else point
        try { worker.execute {
            if(ride==generation) {
                history.append(input)
                if(point.elapsedMs-lastPublishElapsed >= LiveGraphStyle.INTERVAL_MS && SystemClock.uptimeMillis()-lastPublishWall >= LiveGraphStyle.INTERVAL_MS) {
                    lastPublishElapsed=point.elapsedMs; publish(ride)
                }
            }
        }; overflow=false } catch(_:RejectedExecutionException) { overflow=true }
    }
    private fun publish(ride: Long) {
        val next=history.frame(++sequence,width)
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
                val delay=(LiveGraphStyle.INTERVAL_MS-(SystemClock.uptimeMillis()-lastPublishWall)).coerceAtLeast(0)
                val finish=Runnable { try { worker.execute { if(ride==generation) publish(ride) } } catch(_:RejectedExecutionException) {} }
                handler.post { if(ride==generation) { finishing=finish; handler.postDelayed(finish,delay) } }
            }
        } } catch(_:RejectedExecutionException) {}
    }
}
