package de.bcycle.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import de.bcycle.hrv.*
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal data class HrvPoint(val atMs:Long,val rrMs:Double,val gap:Boolean=false)
internal data class HrvState(val phase:Phase?=null,val elapsedMs:Long=0,val remainingMs:Long=180000,
    val bpm:Int?=null,val ready:Boolean=false,val points:List<HrvPoint> = emptyList(),
    val result:JSONObject?=null,val saving:Boolean=false,val saveError:Boolean=false)

/** One bounded serial worker; BLE/Activity never perform HRV calculations or file IO. */
internal class HrvController(context:Context,private val clock:()->Long={ SystemClock.elapsedRealtime() }) {
    private val main=Handler(Looper.getMainLooper())
    private val worker=ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,ArrayBlockingQueue(128)) { Thread(it,"Bicycle-HRV") }
    private val root=File(context.filesDir,"hrv-measurements")
    private var store:HrvStore?=null
    private var session:MeasurementSession?=null
    private var id=""
    private val points=ArrayList<HrvPoint>()
    @Volatile private var lastRrAt=-10000L
    private var bpm:Int?=null
    @Volatile private var abortRequest:String?=null
    @Volatile private var pending:Pair<MeasurementSession,Evaluation>?=null
    private val listeners=linkedSetOf<(HrvState)->Unit>()
    val canRetrySave get()=pending!=null
    @Volatile var state=HrvState();private set
    @Volatile var busy=false;private set
    private val tick=object:Runnable { override fun run() {
        if(!busy) return
        submit { session?.let { current ->
            abortRequest?.let { current.abort(it);abortRequest=null }
            current.advance(clock());update(current)
        } }
        main.postDelayed(this,250)
    } }
    init { submit { store=HrvStore(root).also { it.recover() } } }
    private fun submit(block:()->Unit) {
        try { worker.execute { try { block() } catch(_:Exception) { failure() } } }
        catch(_:java.util.concurrent.RejectedExecutionException) { if(busy) abortRequest="BUFFER_OVERFLOW" }
    }
    private fun publish(next:HrvState) { state=next;main.post { listeners.toList().forEach { it(next) } } }
    fun observe(listener:(HrvState)->Unit):AutoCloseable {
        listeners+=listener;listener(state)
        return AutoCloseable { listeners-=listener }
    }
    fun packet(receivedAt:Long,bytes:ByteArray) {
        val copy=bytes.copyOf()
        submit {
            val decoded=HeartRatePacket.decode(copy)
            bpm=decoded?.bpm
            if(decoded?.contact!=Contact.LOST && decoded?.rrTicks?.any { it>0 }==true) lastRrAt=receivedAt
            val current=session
            if(current==null) {
                if(!busy) publish(state.copy(bpm=bpm,ready=decoded?.contact!=Contact.LOST && clock()-lastRrAt<=5000))
            } else {
                abortRequest?.let { current.abort(it);abortRequest=null }
                val before=current.packets.size
                current.accept(receivedAt,copy)
                if(current.packets.size>before) {
                    val captured=current.packets.last();checkNotNull(store).append(id,captured)
                    if(captured.selected) {
                        val values=captured.decoded?.rrTicks.orEmpty()
                        var at=(receivedAt-current.measureStartMs).toDouble()-values.sum()*1000.0/1024
                        values.forEach { ticks ->
                            val rr=ticks*1000.0/1024;at+=rr
                            val gap=points.lastOrNull()?.let { at-it.atMs>maxOf(1500.0,rr*1.5) } ?: true
                            points+=HrvPoint(at.toLong().coerceAtLeast(0),rr,gap)
                        }
                    }
                }
                update(current)
            }
        }
    }
    fun start(sensor:String):Boolean {
        if(busy || pending!=null || !state.ready || clock()-lastRrAt>5000) return false
        busy=true
        submit {
            check(clock()-lastRrAt<=5000)
            val next=MeasurementSession(clock())
            id=UUID.randomUUID().toString();points.clear();abortRequest=null
            checkNotNull(store).begin(id,System.currentTimeMillis(),sensor,next)
            session=next;update(next)
        }
        main.removeCallbacks(tick);main.post(tick)
        return true
    }
    private fun update(current:MeasurementSession) {
        if(current.phase==Phase.COMPLETE || current.phase==Phase.ABORTED) {
            pending=current to Evaluate.session(current);session=null
            savePending();return
        }
        publish(HrvState(current.phase,current.diagnostics(clock()).measurementElapsedMs,current.remainingMs(clock()),bpm,
            clock()-lastRrAt<=5000,points.toList()))
    }
    private fun savePending() {
        val (current,evaluation)=pending ?: return
        publish(state.copy(phase=current.phase,points=points.toList(),saving=true,saveError=false))
        val saved=checkNotNull(store).save(id,current,evaluation,clock())
        pending=null;busy=false
        main.removeCallbacks(tick)
        publish(HrvState(current.phase,current.diagnostics(clock()).measurementElapsedMs,0,bpm,clock()-lastRrAt<=5000,points.toList(),saved))
    }
    fun retrySave() { if(pending!=null && !busy) { busy=true;submit { savePending() } } }
    fun abort(reason:String="USER_ABORT") {
        if(!busy) return
        abortRequest=reason
        submit { session?.let { it.abort(abortRequest ?: reason);abortRequest=null;update(it) } }
    }
    fun disconnected() {
        submit { lastRrAt=-10000;bpm=null;session?.let { it.abort("DISCONNECTED");update(it) }
            ?: publish(state.copy(ready=false,bpm=null)) }
    }
    private fun failure() {
        session?.let { current -> current.abort("STORAGE_FAILED");pending=current to Evaluate.session(current);session=null }
        busy=false;main.removeCallbacks(tick)
        publish(state.copy(saving=false,saveError=true))
    }
    fun history(done:(List<JSONObject>)->Unit) { submit { val rows=checkNotNull(store).list();main.post { done(rows) } } }
    internal fun close() { main.removeCallbacks(tick);worker.shutdownNow();listeners.clear() }
}
