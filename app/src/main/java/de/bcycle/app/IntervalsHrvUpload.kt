package de.bcycle.app

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONObject
import java.io.IOException
import java.time.YearMonth
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Explicit history upload. A month query avoids a separate GET for every already populated day. */
internal class IntervalsHrvUpload(private val context:Context,
    private val client:(IntervalsConfig,()->Boolean)->IntervalsClient={ config,stop -> IntervalsClient(config,IntervalsSettings(context).apiKey(),stop) }) {
    fun run(records:List<IntervalsWellness>,cancelled:()->Boolean,progress:(Int,Int,IntervalsBulkItem)->Unit):IntervalsBulkResult = IntervalsHrvUploads.serial {
        val settings=IntervalsSettings(context);val config=settings.read();val token=settings.connectionToken()
        if(!config.enabled || config.athlete.isBlank() || !settings.hasKey()) throw IntervalsFailure(false,R.string.intervals_enable_first)
        if(!IntervalsHrvUploads.network(context,config)) return@serial IntervalsBulkResult(emptyList(),true,R.string.intervals_bulk_wifi)
        val stopped={ cancelled() || settings.connectionToken()!=token || !IntervalsHrvUploads.network(context,config) }
        val api=client(config,stopped);val months=mutableMapOf<YearMonth,Map<java.time.LocalDate,JSONObject>>()
        val result=mutableListOf<IntervalsBulkItem>()
        val days=records.distinctBy { it.day }.sortedBy { it.day }
        for(record in days) {
            if(stopped()) return@serial IntervalsBulkResult(result,true,stopReason(settings,token,config))
            val item=try {
                val month=YearMonth.from(record.day)
                val known=months.getOrPut(month) { api.wellness(month.atDay(1),month.atEndOfMonth()) }[record.day]
                if(known!=null && IntervalsWellness.hasHrv(known) &&
                    (!IntervalsWellness.ownsHrv(known,IntervalsHrvUploads.receipt(context,config.athlete,record.day.toString())) ||
                        IntervalsWellness.missingSupplements(record,known).length()==0))
                    IntervalsBulkItem(record.day.toString(),IntervalsBulkState.EXISTS)
                else {
                    // Recheck immediately before PUT; preserve a measurement added since the month query.
                    transfer(context,config,api,record,allowOwnUpdate=false,stopped)
                }
            } catch(error:Exception) {
                if(stopped()) return@serial IntervalsBulkResult(result,true,stopReason(settings,token,config))
                val problem=ServiceProblems.text(error,R.string.intervals_hrv_failed)
                val item=IntervalsBulkItem(record.day.toString(),IntervalsBulkState.FAILED,problem)
                result.add(item);progress(result.size,days.size,item)
                // No blind PUT after malformed, unavailable or unauthorised remote state.
                val failure=error as? IntervalsFailure
                val detail=context.getString(R.string.intervals_hrv_day_error,record.day.toString(),context.getString(problem))+
                    failure?.wellnessStage?.let { " · "+context.getString(it.label) }.orEmpty()+
                    failure?.wellnessField?.let { " · "+context.getString(R.string.intervals_hrv_rejected_field,it) }.orEmpty()+
                    failure?.httpStatus?.let { " (HTTP $it)" }.orEmpty()
                return@serial IntervalsBulkResult(result,true,problem,detail)
            }
            result.add(item);progress(result.size,days.size,item)
        }
        IntervalsBulkResult(result)
    }
    private fun stopReason(settings:IntervalsSettings,token:String,config:IntervalsConfig):Int? =
        if(settings.connectionToken()!=token) R.string.intervals_bulk_changed else if(!IntervalsHrvUploads.network(context,config)) R.string.intervals_bulk_wifi else null
    companion object {
        internal fun transfer(context:Context,config:IntervalsConfig,api:IntervalsClient,record:IntervalsWellness,
            allowOwnUpdate:Boolean,stopped:()->Boolean):IntervalsBulkItem {
            val remote=api.wellness(record.day)
            val receipt=IntervalsHrvUploads.receipt(context,config.athlete,record.day.toString())
            if(IntervalsWellness.hasHrv(remote) && !IntervalsWellness.ownsHrv(remote,receipt))
                return IntervalsBulkItem(record.day.toString(),IntervalsBulkState.EXISTS)
            val supplementOnly=IntervalsWellness.hasHrv(remote) && !allowOwnUpdate
            val patch=if(supplementOnly) IntervalsWellness.missingSupplements(record,remote)
                else IntervalsWellness.patch(record,remote,if(allowOwnUpdate) receipt else null)
            if(patch.keys().asSequence().all { IntervalsWellness.same(patch.opt(it),remote.opt(it)) })
                return IntervalsBulkItem(record.day.toString(),IntervalsBulkState.EXISTS)
            if(stopped()) throw IOException()
            if(supplementOnly) api.supplementWellness(record.day,patch) else api.updateWellness(record.day,patch)
            // Persist only an acknowledged payload, also if a newer local edit arrived during this PUT.
            IntervalsHrvUploads.confirm(context,config.athlete,record.day.toString(),patch)
            return IntervalsBulkItem(record.day.toString(),IntervalsBulkState.UPLOADED)
        }
    }
}

/** Small durable daily outbox; independent of FIT receipts, using the existing intervals account and Wi-Fi rule. */
internal object IntervalsHrvUploads {
    private const val JOB=8306
    private val lock=Any();private val runLock=Any()
    internal fun <T> serial(action:()->T):T=synchronized(runLock) { action() }
    private fun prefs(c:Context)=c.getSharedPreferences("intervals-hrv-uploads",0)
    private fun entries(c:Context)=JSONObject(prefs(c).getString("entries","{}")!!)
    private fun save(c:Context,all:JSONObject) { check(prefs(c).edit().putString("entries",all.toString()).commit()) }
    private fun key(athlete:String,day:String)="$athlete:$day"
    internal fun receipt(c:Context,athlete:String,day:String):JSONObject?=synchronized(lock) { entries(c).optJSONObject(key(athlete,day))?.optJSONObject("last") }
    internal fun confirm(c:Context,athlete:String,day:String,data:JSONObject)=synchronized(lock) {
        val all=entries(c);val id=key(athlete,day);val item=all.optJSONObject(id) ?: JSONObject().put("athlete",athlete).put("day",day)
        // Keep prior owned fields so later missing-field uploads do not claim foreign wellness values.
        val last=item.optJSONObject("last") ?: JSONObject();data.keys().forEach { last.put(it,data.get(it)) }
        item.put("last",last);all.put(id,item);save(c,all)
    }
    fun enqueue(c:Context,record:IntervalsWellness) {
        val settings=IntervalsSettings(c);val config=settings.read();if(!config.enabled || !settings.hasKey()) return
        synchronized(lock) {
            val all=entries(c);val id=key(config.athlete,record.day.toString());val item=all.optJSONObject(id) ?: JSONObject()
            item.put("athlete",config.athlete).put("day",record.day.toString()).put("data",record.values)
                .put("revision",UUID.randomUUID().toString()).put("state","pending")
            all.put(id,item);save(c,all)
        }
        schedule(c)
    }
    fun discard(c:Context,day:String)=synchronized(lock) {
        val all=entries(c)
        all.keys().forEach { id -> val item=all.getJSONObject(id);if(item.optString("day")==day) { item.remove("data");item.remove("state");item.remove("revision") } }
        save(c,all)
    }
    internal fun network(c:Context,config:IntervalsConfig):Boolean = !config.wifiOnly || c.getSystemService(ConnectivityManager::class.java).let { manager ->
        manager.getNetworkCapabilities(manager.activeNetwork)?.let { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) }==true
    }
    fun schedule(c:Context) {
        val settings=IntervalsSettings(c);val config=settings.read();val scheduler=c.getSystemService(JobScheduler::class.java)
        val pending=synchronized(lock) { val all=entries(c);all.keys().asSequence().any { all.getJSONObject(it).let { row -> row.optString("athlete")==config.athlete && row.optString("state")=="pending" } } }
        if(!config.enabled || !settings.hasKey() || !pending) { scheduler.cancel(JOB);return }
        if(scheduler.getPendingJob(JOB)==null) scheduler.schedule(JobInfo.Builder(JOB,ComponentName(c,IntervalsHrvUploadService::class.java))
            .setRequiredNetworkType(if(config.wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency(30000).setPersisted(true).setBackoffCriteria(60000,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
    }
    fun settingsChanged(c:Context) {
        c.getSystemService(JobScheduler::class.java).cancel(JOB)
        synchronized(lock) {
            val all=entries(c);val athlete=IntervalsSettings(c).read().athlete
            all.keys().forEach { val row=all.getJSONObject(it);if(row.optString("athlete")==athlete && row.optString("state")=="failed") row.put("state","pending") }
            save(c,all)
        }
        schedule(c)
    }
    fun run(c:Context,cancelled:()->Boolean,client:(IntervalsConfig,()->Boolean)->IntervalsClient={ config,stop -> IntervalsClient(config,IntervalsSettings(c).apiKey(),stop) }):Boolean=serial {
        val settings=IntervalsSettings(c);val config=settings.read();val token=settings.connectionToken()
        if(!config.enabled || !settings.hasKey() || cancelled()) return@serial false
        if(!network(c,config)) return@serial true
        val snapshot=synchronized(lock) { entries(c) }
        for(id in snapshot.keys()) {
            val row=snapshot.getJSONObject(id)
            if(row.optString("athlete")!=config.athlete || row.optString("state")!="pending") continue
            fun current()=synchronized(lock) { entries(c).optJSONObject(id)?.optString("revision")==row.optString("revision") }
            val apiStopped={ cancelled() || settings.connectionToken()!=token || !network(c,config) }
            val stopped={ apiStopped() || !current() }
            if(stopped()) return@serial true
            var state="saved";var problem=""
            try {
                val record=IntervalsWellness(java.time.LocalDate.parse(row.getString("day")),row.getJSONObject("data"))
                IntervalsHrvUpload.transfer(c,config,client(config,apiStopped),record,true,stopped)
            } catch(error:Exception) {
                val retry=stopped() || error is IntervalsFailure && error.retryable || error is IOException && error !is IntervalsFailure && error !is javax.net.ssl.SSLException
                state=if(retry) "pending" else "failed";problem=ServiceProblems.of(error)?.key ?: "response"
            }
            synchronized(lock) {
                val all=entries(c);val item=all.optJSONObject(id)
                if(item?.optString("revision")==row.optString("revision")) { item.put("state",state).put("problem",problem);save(c,all) }
            }
            if(state=="pending") return@serial true
        }
        synchronized(lock) { val all=entries(c);all.keys().asSequence().any { all.getJSONObject(it).let { row -> row.optString("athlete")==config.athlete && row.optString("state")=="pending" } } }
    }
}
class IntervalsHrvUploadService:JobService() {
    private var stop=AtomicBoolean(false)
    override fun onStartJob(params:JobParameters):Boolean {
        val token=AtomicBoolean(false);stop=token
        Thread({ val retry=try { IntervalsHrvUploads.run(applicationContext,token::get) } catch(_:Exception) { true }
            if(!token.get()) jobFinished(params,retry)
        },"B-Cycle-intervals-HRV").start();return true
    }
    override fun onStopJob(params:JobParameters):Boolean { stop.set(true);return true }
}
