package de.bcycle.app

import android.app.job.*
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Separate receipts and jobs: WebDAV success never marks an intervals.icu upload successful. */
internal object IntervalsUploads {
    private const val JOB=8302
    private val lock=Any();private val runLock=Any()
    private fun prefs(c:Context)=c.getSharedPreferences("intervals-uploads",0)
    private fun entries(c:Context)=JSONObject(prefs(c).getString("entries","{}")!!)
    private fun save(c:Context,all:JSONObject) { check(prefs(c).edit().putString("entries",all.toString()).putLong("revision",System.nanoTime()).commit()) }
    fun revision(c:Context)=prefs(c).getLong("revision",0)
    fun state(c:Context,id:String,stamp:RouteFileStamp?=null):RouteUploadStatus=synchronized(lock) {
        val item=entries(c).optJSONObject(id) ?: return@synchronized RouteUploadStatus(UploadState.NOT_UPLOADED)
        if(item.optString("athlete")!=IntervalsSettings(c).read().athlete || (stamp!=null &&
            (item.optLong("length")!=stamp.length || item.optLong("modified")!=stamp.modified || (stamp.hash!=null && item.optString("hash")!=stamp.hash))))
            return@synchronized RouteUploadStatus(UploadState.NOT_UPLOADED)
        RouteUploadStatus(when(item.optString("state")) { "saved"->UploadState.UPLOADED;"failed"->UploadState.FAILED;"uploading"->UploadState.UPLOADING;else->UploadState.WAITING })
    }
    fun enqueue(c:Context,file:File,name:String) {
        val config=IntervalsSettings(c).read();if(!config.enabled) return
        require(RouteUploads.eligible(c,file.nameWithoutExtension))
        val hash=WebDavClient.hash(file)
        synchronized(lock) {
            val all=entries(c);val old=all.optJSONObject(file.nameWithoutExtension)
            RideServiceLinks.links(c,file.nameWithoutExtension) // Migrate an existing confirmed receipt before replacing it.
            if(old?.optString("athlete")==config.athlete && old.optString("hash")==hash && old.optLong("modified")==file.lastModified() &&
                old.optString("state") in setOf("pending","uploading","saved")) return@synchronized
            all.put(file.nameWithoutExtension,JSONObject().put("athlete",config.athlete).put("hash",hash).put("length",file.length())
                .put("modified",file.lastModified()).put("name",name).put("state","pending"));save(c,all)
        }
        schedule(c)
    }
    fun retry(c:Context,id:String) {
        check(IntervalsSettings(c).read().enabled) { c.getString(R.string.intervals_enable_first) }
        enqueue(c,FitLibrary.activeFile(c.filesDir,id),"B-Cycle-$id.fit")
    }
    fun forget(c:Context,id:String)=synchronized(lock) { val all=entries(c);all.remove(id);save(c,all) }
    fun schedule(c:Context) {
        val scheduler=c.getSystemService(JobScheduler::class.java);val config=IntervalsSettings(c).read()
        val pending=synchronized(lock) { val all=entries(c);all.keys().asSequence().any { all.getJSONObject(it).optString("state") in setOf("pending","uploading") } }
        if(!config.enabled || !pending) { scheduler.cancel(JOB);return }
        if(scheduler.getPendingJob(JOB)!=null) return
        scheduler.schedule(JobInfo.Builder(JOB,ComponentName(c,IntervalsUploadService::class.java))
            .setRequiredNetworkType(if(config.wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true).setBackoffCriteria(60000,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
    }
    fun settingsChanged(c:Context) {
        c.getSystemService(JobScheduler::class.java).cancel(JOB)
        synchronized(lock) { val all=entries(c);val athlete=IntervalsSettings(c).read().athlete
            all.keys().forEach { val item=all.getJSONObject(it);if(item.optString("athlete")==athlete && item.optString("state")=="failed") item.put("state","pending") };save(c,all)
        };schedule(c)
    }
    private fun update(c:Context,id:String,expected:JSONObject,state:String,remote:String="",error:String="")=synchronized(lock) {
        val all=entries(c);val item=all.optJSONObject(id) ?: return@synchronized
        if(item.optString("hash")!=expected.optString("hash") || item.optString("athlete")!=expected.optString("athlete") ||
            item.optLong("modified")!=expected.optLong("modified") || item.optLong("length")!=expected.optLong("length")) return@synchronized
        item.put("state",state).put("remoteId",remote).put("error",error);save(c,all)
    }
    fun run(c:Context,cancelled:()->Boolean,upload:(IntervalsConfig,File,String,()->Boolean)->String={ config,file,name,stopped ->
        IntervalsClient(config,IntervalsSettings(c).apiKey(),stopped).upload(file,name)
    }):Boolean=synchronized(runLock) {
        val config=IntervalsSettings(c).read();if(!config.enabled || cancelled()) return@synchronized false
        if(config.wifiOnly) {
            val network=c.getSystemService(ConnectivityManager::class.java)
            if(network.getNetworkCapabilities(network.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)!=true) return@synchronized true
        }
        val all=synchronized(lock) { entries(c) }
        var retry=false
        for(id in all.keys()) {
            val item=all.getJSONObject(id);if(item.optString("state") !in setOf("pending","uploading")) continue
            if(cancelled()) return@synchronized true
            try {
                check(item.getString("athlete")==config.athlete)
                check(RouteUploads.eligible(c,id))
                val file=FitLibrary.activeFile(c.filesDir,id)
                check(file.isFile && WebDavClient.hash(file)==item.getString("hash") && file.length()==item.getLong("length") && file.lastModified()==item.getLong("modified"))
                update(c,id,item,"uploading")
                val stopped={ cancelled() || IntervalsSettings(c).read().let { !it.enabled || it.athlete!=config.athlete } }
                val remote=upload(config,file,item.getString("name"),stopped)
                if(stopped()) throw IOException()
                RideServiceLinks.confirm(c,RideServiceLink(id,RideService.INTERVALS,config.athlete,remote,item.getString("hash")))
                check(remote.isNotBlank() && file.isFile && WebDavClient.hash(file)==item.getString("hash") && file.lastModified()==item.getLong("modified"))
                update(c,id,item,"saved",remote)
            } catch(e:Exception) {
                val temporary=cancelled() || (e is IntervalsFailure && e.retryable) || (e is IOException && e !is IntervalsFailure && e !is javax.net.ssl.SSLHandshakeException)
                retry=retry || temporary
                update(c,id,item,if(temporary) "pending" else "failed",error=if(e is IntervalsFailure && e.problem==R.string.intervals_auth_failed) "auth" else "upload")
            }
        }
        retry || synchronized(lock) { val latest=entries(c);latest.keys().asSequence().any { latest.getJSONObject(it).optString("state") in setOf("pending","uploading") } }
    }
}
class IntervalsUploadService:JobService() {
    private var stopped=AtomicBoolean(false)
    override fun onStartJob(params:JobParameters):Boolean {
        val token=AtomicBoolean(false);stopped=token
        Thread({ val retry=try { IntervalsUploads.run(applicationContext,token::get) } catch(_:Exception) { true }
            if(!token.get()) jobFinished(params,retry)
        },"B-Cycle-intervals").start();return true
    }
    override fun onStopJob(params:JobParameters):Boolean { stopped.set(true);return true }
}

/** Existing cloud action fans out to enabled services; each retains its own durable receipt. */
internal object RouteCloudUploads {
    fun revision(c:Context)="${RouteUploads.revision(c)}:${IntervalsUploads.revision(c)}"
    fun state(c:Context,id:String,stamp:RouteFileStamp?):RouteUploadStatus {
        val targets=listOf<()->RouteUploadStatus?>(
            { if(RouteExportSettings(c).read().webdav) RouteUploads.state(c,id,stamp) else null },
            { if(IntervalsSettings(c).read().enabled) IntervalsUploads.state(c,id,stamp) else null }
        )
        val states=targets.mapNotNull { runCatching(it).getOrElse { RouteUploadStatus(UploadState.FAILED) } }
        return RouteUploadStatus(if(states.isNotEmpty() && states.all { it.state==UploadState.UPLOADED }) UploadState.UPLOADED else UploadState.NOT_UPLOADED)
    }
    private fun both(c:Context,dav:()->Unit,intervals:()->Unit) {
        dispatch(listOf(
            { if(RouteExportSettings(c).read().webdav) dav() },
            { if(IntervalsSettings(c).read().enabled) intervals() }
        ))
    }
    /** Include configuration reads in isolation: a broken provider must not skip later targets. */
    internal fun dispatch(targets:List<()->Unit>) {
        val results=targets.map { runCatching(it) }
        results.firstOrNull { it.isFailure }?.getOrThrow()
    }
    fun enqueue(c:Context,file:File,name:String)=both(c,{RouteUploads.enqueue(c,file,name)},{IntervalsUploads.enqueue(c,file,name)})
    fun retry(c:Context,id:String) {
        var enabled=0
        both(c,{enabled++;RouteUploads.retry(c,id)},{enabled++;IntervalsUploads.retry(c,id)})
        check(enabled>0) { c.getString(R.string.intervals_no_target) }
    }
}
