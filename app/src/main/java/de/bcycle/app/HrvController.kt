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
    val result:JSONObject?=null,val saving:Boolean=false,val saveError:Boolean=false,
    val lastBeatAtMs:Long?=null,val beatIntervalMs:Double?=null,
    val restMs:Long=30000,val measureMs:Long=180000)

/** One bounded serial worker; BLE/Activity never perform HRV calculations or file IO. */
internal class HrvController(private val context:Context,private val clock:()->Long={ SystemClock.elapsedRealtime() }) {
    private val main=Handler(Looper.getMainLooper())
    private val settings=HrvSettings(context)
    private var measurementProtocol=settings.read().protocol
    private val worker=ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,ArrayBlockingQueue(128)) { Thread(it,"Bicycle-HRV") }
    private val root=File(context.filesDir,"hrv-measurements")
    private var store:HrvStore?=null
    private var session:MeasurementSession?=null
    private var id=""
    private val points=ArrayList<HrvPoint>()
    @Volatile private var lastRrAt=-10000L
    private var beatIntervalMs:Double?=null
    private var bpm:Int?=null
    @Volatile private var abortRequest:String?=null
    @Volatile private var pending:Pair<MeasurementSession,Evaluation>?=null
    private val listeners=linkedSetOf<(HrvState)->Unit>()
    val canRetrySave get()=pending!=null
    @Volatile var state=HrvState();private set
    @Volatile var busy=false;private set
    private val settingsListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,_->
        submit { if(!busy && state.phase==null) publish(state) }
    }
    private val tick=object:Runnable { override fun run() {
        if(!busy) return
        submit { session?.let { current ->
            abortRequest?.let { current.abort(it);abortRequest=null }
            current.advance(clock());update(current)
        } }
        main.postDelayed(this,250)
    } }
    init {
        settings.preferences.registerOnSharedPreferenceChangeListener(settingsListener)
        submit { store=HrvStore(root).also { it.recover() };publish(state) }
    }
    private fun submit(block:()->Unit) {
        try { worker.execute { try { block() } catch(_:Exception) { failure() } } }
        catch(_:java.util.concurrent.RejectedExecutionException) { if(busy) abortRequest="BUFFER_OVERFLOW" }
    }
    private fun publish(next:HrvState) {
        val protocol=if(next.phase==null) settings.read().protocol else measurementProtocol
        val value=next.copy(lastBeatAtMs=lastRrAt.takeIf { it>=0 },beatIntervalMs=beatIntervalMs,
            restMs=protocol.restMs,measureMs=protocol.measureMs,
            remainingMs=if(next.phase==null) protocol.measureMs else next.remainingMs)
        state=value;main.post { listeners.toList().forEach { it(value) } }
    }
    fun observe(listener:(HrvState)->Unit):AutoCloseable {
        listeners+=listener;listener(state)
        return AutoCloseable { listeners-=listener }
    }
    fun packet(receivedAt:Long,bytes:ByteArray) {
        val copy=bytes.copyOf()
        submit {
            val decoded=HeartRatePacket.decode(copy)
            bpm=decoded?.bpm
            if(decoded?.contact!=Contact.LOST && decoded?.rrTicks?.any { it>0 }==true) {
                lastRrAt=receivedAt;beatIntervalMs=decoded.rrTicks.last { it>0 }*1000.0/1024
            }
            val current=session
            if(current==null) {
                if(!busy) publish(state.copy(bpm=bpm,ready=decoded?.contact!=Contact.LOST && clock()-lastRrAt<=5000))
            } else {
                // A notification received before Start can reach the main callback later.
                if(receivedAt<current.startMs) return@submit
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
                            // Only excess time beyond the observed RR can indicate a delivery gap.
                            // Counting RR itself incorrectly split ordinary notification jitter.
                            val gap=points.lastOrNull()?.let { at-it.atMs-rr>maxOf(1500.0,rr*1.5) } ?: true
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
        val startedAt=clock()
        val protocol=settings.read().protocol
        busy=true
        submit {
            check(clock()-lastRrAt<=5000)
            measurementProtocol=protocol
            val next=MeasurementSession(startedAt,protocol)
            id=UUID.randomUUID().toString();points.clear();abortRequest=null
            checkNotNull(store).begin(id,System.currentTimeMillis(),sensor,next)
            session=next;update(next)
        }
        main.removeCallbacks(tick);main.post(tick)
        return true
    }
    private fun update(current:MeasurementSession) {
        if(current.phase==Phase.COMPLETE || current.phase==Phase.ABORTED) {
            if(current.phase==Phase.COMPLETE) {
                val config=settings.read()
                main.post { HrvCompletionSignal.play(context,config) }
            }
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
        refreshIntervals(saved)
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
    fun history(done:(List<HrvHistorySummary>)->Unit) { submit { val rows=checkNotNull(store).daily();main.post { done(rows) } } }
    private fun <T> storageTask(done:(Result<T>)->Unit,block:()->T) {
        try { worker.execute { val result=runCatching(block);main.post { done(result) } } }
        catch(e:java.util.concurrent.RejectedExecutionException) { main.post { done(Result.failure(e)) } }
    }
    fun measurement(id:String,done:(Result<JSONObject>)->Unit) =
        storageTask(done) { checkNotNull(store).readDaily(id) }
    fun intervalsWellness(done:(Result<List<IntervalsWellness>>)->Unit) =
        storageTask(done) { checkNotNull(store).intervalsWellness() }
    private fun refreshIntervals(row:JSONObject) {
        if(!IntervalsSettings(context).read().enabled) return
        // Queue failures must never invalidate an otherwise successfully saved measurement.
        runCatching {
            val day=java.time.LocalDate.parse(HrvQuestionnaire.forMeasurement(row).day)
            val record=checkNotNull(store).intervalsWellness(day).singleOrNull()
            if(record==null) IntervalsHrvUploads.discard(context,day.toString()) else IntervalsHrvUploads.enqueue(context,record)
        }
    }
    fun deleteMeasurement(id:String,done:(Result<Unit>)->Unit) =
        storageTask(done) {
            check(!busy && pending==null)
            val removed=checkNotNull(store).read(id)
            checkNotNull(store).delete(id)
            // Deletion is local only; never delete the remote day. Resolve any remaining local measurement.
            refreshIntervals(removed)
            if(state.result?.optString("id")==id)
                publish(state.copy(result=null,phase=null,elapsedMs=0,remainingMs=state.measureMs,points=emptyList()))
        }
    fun trends(done:(Result<Map<HrvTrendMetric,List<HrvTrendPoint>>>)->Unit) =
        storageTask(done) {
            val rows=checkNotNull(store).daily()
            HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) }
        }
    fun trendsWithContext(done:(Result<HrvTrendPresentation>)->Unit) = storageTask(done) {
        val storage=checkNotNull(store);val rows=storage.daily()
        HrvTrendPresentation(HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) },HrvContextColors.daily(storage.trendQuestionnaires()))
    }
    fun dailyAssessment(id:String,done:(Result<HrvDailyAssessment>)->Unit) = storageTask(done) {
        val storage=checkNotNull(store)
        HrvDailyAssessment.from(storage.list(),storage.trendQuestionnaires(),id)
    }
    fun questionnaire(id:String,done:(Result<HrvQuestionnaire?>)->Unit) =
        storageTask(done) { checkNotNull(store).questionnaireForEditing(id) }
    fun saveQuestionnaire(id:String,value:HrvQuestionnaire,done:(Result<Unit>)->Unit) =
        storageTask(done) {
            checkNotNull(store).saveQuestionnaire(id,value)
            refreshIntervals(checkNotNull(store).read(id))
        }
    fun previewHrv4Training(uri:android.net.Uri,order:HrvCsvDateOrder?,done:(Result<HrvCsvPreview>)->Unit) =
        storageTask(done) {
            check(!busy)
            checkNotNull(context.contentResolver.openInputStream(uri)).use { Hrv4TrainingCsv.preview(it,java.time.ZoneId.systemDefault(),order) }
        }
    fun importHrv4Training(preview:HrvCsvPreview,done:(Result<HrvCsvImportReport>)->Unit) =
        storageTask(done) {
            check(!busy)
            val journal=checkNotNull(store)
            val report=Hrv4TrainingCsv.import(preview,journal)
            // Do not keep an obsolete native result on the live screen after importing a later one.
            state.result?.let { current ->
                if(journal.readDaily(current.getString("id")).getString("id")!=current.getString("id"))
                    publish(state.copy(result=null,phase=null,elapsedMs=0,points=emptyList()))
            }
            report
        }
    @Volatile private var exporting=false
    fun export(done:(Int,Boolean)->Unit) {
        if(busy || exporting) { main.post { done(0,false) };return }
        exporting=true
        val task=Runnable {
            val file=File(context.cacheDir,"Bicycle-HRV-${System.currentTimeMillis()}.zip")
            try {
                val count=HrvDiagnosticExport.write(checkNotNull(store),file)
                DownloadFiles.write(context,file,file.name)
                main.post { done(count,true) }
            } catch(_:Exception) { main.post { done(0,false) } }
            finally { file.delete();exporting=false }
        }
        try { worker.execute(task) } catch(_:java.util.concurrent.RejectedExecutionException) { exporting=false;main.post { done(0,false) } }
    }
    internal fun close() { settings.preferences.unregisterOnSharedPreferenceChangeListener(settingsListener);main.removeCallbacks(tick);worker.shutdownNow();listeners.clear() }
}
