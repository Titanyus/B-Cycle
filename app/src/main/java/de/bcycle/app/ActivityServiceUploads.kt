package de.bcycle.app

import android.app.job.*
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Independent durable operations, including asynchronous remote processing IDs.
 * Older versions retain their own operation until confirmed; they cannot mark a new file saved.
 */
internal class ActivityServiceUploads private constructor(val service:ActivityService) {
    private val lock=Any();private val runLock=Any()
    private fun prefs(c:Context)=c.getSharedPreferences("activity-uploads-${service.name.lowercase()}",0)
    private fun entries(c:Context)=JSONObject(prefs(c).getString("entries","{}")!!)
    private fun save(c:Context,all:JSONObject) { check(prefs(c).edit().putString("entries",all.toString()).putLong("revision",System.nanoTime()).commit()) }
    fun revision(c:Context)=prefs(c).getLong("revision",0)
    fun state(c:Context,id:String,stamp:RouteFileStamp?):RouteUploadStatus=synchronized(lock) {
        val config=ActivityServiceSettings(c,service).read();val target=config.identity(service);val all=entries(c)
        val item=all.keys().asSequence().map { all.getJSONObject(it) }.filter { it.optString("ride")==id && it.optString("target")==target &&
            (stamp==null || (it.optLong("length")==stamp.length && it.optLong("modified")==stamp.modified && (stamp.hash==null || it.optString("hash")==stamp.hash))) }.maxByOrNull { it.optLong("sequence") }
        RouteUploadStatus(when(item?.optString("state")) { "saved"->UploadState.UPLOADED;"pending"->UploadState.WAITING;"processing"->UploadState.UPLOADING;"failed"->UploadState.FAILED;else->UploadState.NOT_UPLOADED })
    }
    fun problem(c:Context):Int?=synchronized(lock) {
        val config=ActivityServiceSettings(c,service).read();val all=entries(c)
        val latest=all.keys().asSequence().map { all.getJSONObject(it) }.filter { it.optString("target")==config.identity(service) }.maxByOrNull { it.optLong("sequence") }
        if(latest?.optString("state")!="failed") return@synchronized null
        when(latest.optString("problem")) { "auth"->R.string.service_auth_failed;"duplicate"->R.string.service_duplicate;"response"->R.string.service_response_failed;else->R.string.service_upload_failed }
    }
    fun enqueue(c:Context,file:File) {
        val config=ActivityServiceSettings(c,service).read();if(!config.enabled) return
        val id=file.nameWithoutExtension;require(RouteUploads.eligible(c,id) && file.isFile)
        val hash=WebDavClient.hash(file);val target=config.identity(service);require(target.isNotBlank())
        synchronized(lock) {
            val all=entries(c)
            if(all.keys().asSequence().map { all.getJSONObject(it) }.any { it.optString("ride")==id && it.optString("target")==target && it.optString("hash")==hash &&
                it.optLong("length")==file.length() && it.optLong("modified")==file.lastModified() && it.optString("state")!="failed" }) return@synchronized
            val operation=UUID.randomUUID().toString()
            val sequence=all.keys().asSequence().map { all.getJSONObject(it).optLong("sequence") }.maxOrNull()?.plus(1) ?: 1L
            all.put(operation,JSONObject().put("ride",id).put("target",target).put("hash",hash).put("length",file.length()).put("modified",file.lastModified())
                .put("filename","B-Cycle-$id-${hash.take(16)}-$operation.fit").put("job","").put("sequence",sequence).put("state","pending"))
            save(c,all)
        }
        schedule(c)
    }
    fun retry(c:Context,id:String)=enqueue(c,FitLibrary.activeFile(c.filesDir,id))
    fun forget(c:Context,id:String)=synchronized(lock) {
        val all=entries(c);all.keys().asSequence().toList().filter { all.getJSONObject(it).optString("ride")==id }.forEach(all::remove);save(c,all)
    }
    private fun pending(all:JSONObject,target:String)=all.keys().asSequence().filter { val it=all.getJSONObject(it)
        it.optString("target")==target && it.optString("state") in setOf("pending","processing") }.toList()
    fun schedule(c:Context) {
        val config=ActivityServiceSettings(c,service).read();val scheduler=c.getSystemService(JobScheduler::class.java)
        if(!config.enabled || synchronized(lock) { pending(entries(c),config.identity(service)).isEmpty() }) { scheduler.cancel(service.job);return }
        if(scheduler.getPendingJob(service.job)!=null) return
        val type=if(service==ActivityService.DREEVE) DreeveUploadService::class.java else StravaUploadService::class.java
        scheduler.schedule(JobInfo.Builder(service.job,ComponentName(c,type)).setPersisted(true)
            .setRequiredNetworkType(if(config.wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(60000,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
    }
    fun settingsChanged(c:Context) {
        c.getSystemService(JobScheduler::class.java).cancel(service.job)
        // Retry failed requests after a credentials/configuration fix, keeping accepted remote jobs.
        val config=ActivityServiceSettings(c,service).read()
        synchronized(lock) {
            val all=entries(c)
            if(config.enabled) all.keys().forEach { val it=all.getJSONObject(it)
                if(it.optString("target")==config.identity(service) && it.optString("state")=="failed" && it.optString("problem")=="auth")
                    it.put("state",if(it.optString("job").isEmpty()) "pending" else "processing")
            };save(c,all)
        };schedule(c);RideNames.settingsChanged(c)
    }
    private fun update(c:Context,operation:String,change:(JSONObject)->Unit)=synchronized(lock) {
        val all=entries(c);val item=all.optJSONObject(operation) ?: return@synchronized
        change(item);save(c,all)
    }
    fun run(c:Context,cancelled:()->Boolean,confirmationWait:((Long,()->Boolean)->Boolean)?=null,step:((ActivityServiceConfig,File,String,String,String,()->Boolean)->UploadProgress)?=null):Boolean=synchronized(runLock) {
        val settings=ActivityServiceSettings(c,service);val config=settings.read()
        if(!config.enabled || cancelled()) return@synchronized false
        if(config.wifiOnly) {
            val network=c.getSystemService(ConnectivityManager::class.java)
            if(network.getNetworkCapabilities(network.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)!=true) return@synchronized true
        }
        val target=config.identity(service)
        // One bounded confirmation window per worker, not per archived ride.
        val confirmationDelays=java.util.ArrayDeque(listOf(2000L,3000L,5000L,10000L,10000L))
        val snapshot=synchronized(lock) { entries(c) }
        for(operation in pending(snapshot,target)) {
            if(cancelled()) return@synchronized true
            val item=snapshot.getJSONObject(operation);val id=item.getString("ride")
            val stopped={ cancelled() || settings.read().let { !it.enabled || it.identity(service)!=target } }
            try {
                val file=FitLibrary.activeFile(c.filesDir,id)
                check(RouteUploads.eligible(c,id) && file.isFile)
                // Processing IDs can still be resolved after a correction replaces the active FIT.
                if(item.getString("job").isEmpty()) check(WebDavClient.hash(file)==item.getString("hash") && file.lastModified()==item.getLong("modified"))
                var job=item.getString("job")
                while(true) {
                    if(stopped()) return@synchronized true
                    if(synchronized(lock) { !entries(c).has(operation) }) break
                    val result=if(step!=null) step(config,file,item.getString("filename"),"$id-${item.getString("hash")}",job,stopped)
                        else when(service) {
                            ActivityService.DREEVE->DreeveClient(config,settings.credentials().getString("secret"),stopped).step(file,item.getString("filename"),job)
                            ActivityService.STRAVA->StravaClient(settings,stopped).step(file,item.getString("filename"),"$id-${item.getString("hash")}",job)
                        }
                    // Persist acceptance before another request or Android cancellation; never re-POST it.
                    require(result.jobId.isNotBlank());job=result.jobId
                    update(c,operation) { it.put("job",job).put("state","processing").remove("problem") }
                    if(stopped()) return@synchronized true
                    if(result.activityId!=null && file.isFile && RouteUploads.eligible(c,id)) {
                        synchronized(lock) {
                            if(entries(c).has(operation)) {
                                RideServiceLinks.confirm(c,RideServiceLink(id,service.link,target,result.activityId,item.getString("hash")))
                                update(c,operation) { it.put("state","saved").put("remoteId",result.activityId).remove("problem") }
                                RideNames.confirmed(c,RideServiceLink(id,service.link,target,result.activityId,item.getString("hash")))
                            }
                        }
                        break
                    }
                    // Normal Dreeve processing is not a failed network transfer. Check it promptly.
                    if(service!=ActivityService.DREEVE || confirmationWait==null || confirmationDelays.isEmpty()) break
                    if(!confirmationWait(confirmationDelays.removeFirst(),stopped) || stopped()) return@synchronized true
                }
            } catch(e:Exception) {
                val transient=stopped() || (e is ActivityServiceFailure && e.retryable) || (e is IOException && e !is ActivityServiceFailure && e !is javax.net.ssl.SSLHandshakeException)
                update(c,operation) { it.put("state",if(transient) (if(it.optString("job").isEmpty()) "pending" else "processing") else "failed")
                    .put("problem",when((e as? ActivityServiceFailure)?.problem) {
                        R.string.service_auth_failed->"auth";R.string.service_duplicate->"duplicate";R.string.service_response_failed->"response";else->"upload"
                    }) }
            }
        }
        synchronized(lock) { pending(entries(c),target).isNotEmpty() }
    }
    companion object {
        internal fun waitForConfirmation(delayMs:Long,stopped:()->Boolean):Boolean {
            val end=android.os.SystemClock.elapsedRealtime()+delayMs
            while(!stopped()) {
                val remaining=end-android.os.SystemClock.elapsedRealtime()
                if(remaining<=0) return true
                try { Thread.sleep(minOf(remaining,250L)) } catch(_:InterruptedException) {
                    Thread.currentThread().interrupt();return false
                }
            }
            return false
        }
        val dreeve=ActivityServiceUploads(ActivityService.DREEVE)
        val strava=ActivityServiceUploads(ActivityService.STRAVA)
        fun forService(service:ActivityService)=if(service==ActivityService.DREEVE) dreeve else strava
        val all get()=listOf(dreeve,strava)
    }
}
abstract class ActivityUploadService:JobService() {
    internal abstract val provider:ActivityService
    private var stopped=AtomicBoolean(false)
    override fun onStartJob(params:JobParameters):Boolean {
        val token=AtomicBoolean(false);stopped=token
        Thread({ val retry=try { ActivityServiceUploads.forService(provider).run(applicationContext,token::get,confirmationWait=ActivityServiceUploads::waitForConfirmation) } catch(_:Exception) { true }
            if(!token.get() && provider==ActivityService.DREEVE) runCatching { RideNames.run(applicationContext,token::get) }
            if(!token.get()) jobFinished(params,retry)
        },"B-Cycle-${provider.name}").start();return true
    }
    override fun onStopJob(params:JobParameters):Boolean { stopped.set(true);return true }
}
class DreeveUploadService:ActivityUploadService() { internal override val provider=ActivityService.DREEVE }
class StravaUploadService:ActivityUploadService() { internal override val provider=ActivityService.STRAVA }
