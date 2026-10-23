package de.bcycle.app

import android.app.job.*
import android.content.ComponentName
import android.content.Context
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal data class DreeveNameSyncResult(val updated:Int,val unchanged:Int,val failed:Int,val interrupted:Boolean=false)

/** Durable, coalesced name changes. Never uploads ride files; manual names always win. */
internal object RideNames {
    private const val JOB=8305
    private val lock=Any();private val workerLock=Any()
    private fun prefs(c:Context)=c.getSharedPreferences("ride-names",0)
    private fun data(c:Context)=JSONObject(prefs(c).getString("data","{}")!!)
    private fun save(c:Context,value:JSONObject) { check(prefs(c).edit().putString("data",value.toString()).putLong("revision",System.nanoTime()).commit()) }
    internal class Snapshot(val values:Map<String,String>)
    private var snapshotKey:String?=null
    private var snapshot:Snapshot?=null
    internal fun snapshot(c:Context):Snapshot=synchronized(lock) {
        val key=prefs(c).getString("data","{}")!!
        if(snapshotKey!=key || snapshot==null) {
            val rows=names(JSONObject(key))
            snapshot=Snapshot(rows.keys().asSequence().mapNotNull { id -> rows.getJSONObject(id).optString("name").takeIf { it.isNotBlank() }?.let { id to it } }.toMap())
            snapshotKey=key
        }
        checkNotNull(snapshot)
    }
    fun allNames(c:Context):Map<String,String> = snapshot(c).values
    fun revision(c:Context)=prefs(c).getLong("revision",0)
    fun name(c:Context,id:String):String?=synchronized(lock) { data(c).optJSONObject("names")?.optJSONObject(id)?.optString("name")?.takeIf { it.isNotBlank() } }
    private fun names(all:JSONObject)=all.optJSONObject("names") ?: JSONObject().also { all.put("names",it) }
    private fun tasks(all:JSONObject)=all.optJSONObject("tasks") ?: JSONObject().also { all.put("tasks",it) }
    private fun key(link:RideServiceLink)=UUID.nameUUIDFromBytes("${link.rideId}\u0000${link.service}\u0000${link.accountId}\u0000${link.activityId}".toByteArray()).toString()
    private fun task(all:JSONObject,link:RideServiceLink,name:String?,read:Boolean=false) {
        if(read) {
            val jobs=tasks(all)
            jobs.keys().asSequence().toList().filter { id -> val old=jobs.getJSONObject(id)
                old.optBoolean("read") && old.optString("ride")==link.rideId && old.optString("account")==link.accountId }.forEach(jobs::remove)
        }
        tasks(all).put(key(link),JSONObject().put("ride",link.rideId).put("service",link.service.name).put("account",link.accountId)
            .put("activity",link.activityId).put("name",name ?: "").put("read",read).put("token",UUID.randomUUID().toString())
            .put("after",System.currentTimeMillis()+if(read) 3000 else 0).put("state","pending"))
    }
    fun pending(c:Context,id:String,service:RideService):String?=synchronized(lock) {
        val jobs=tasks(data(c));val matching=jobs.keys().asSequence().map { jobs.getJSONObject(it) }
            .filter { it.optString("ride")==id && it.optString("service")==service.name && current(c,it) }.toList()
        if(matching.any { it.optString("state")=="failed" }) "failed" else if(matching.isNotEmpty()) "pending" else null
    }
    fun imported(c:Context,id:String,name:String)=synchronized(lock) {
        val all=data(c);if(names(all).has(id)) return@synchronized
        names(all).put(id,JSONObject().put("name",name).put("manual",false));save(c,all)
    }
    fun manual(c:Context,id:String,name:String) {
        require(name.isNotBlank() && name.length<=120)
        val links=RideServiceLinks.links(c,id)
        synchronized(lock) {
            val all=data(c);names(all).put(id,JSONObject().put("name",name).put("manual",true))
            links.forEach { task(all,it,name) };save(c,all)
        };schedule(c)
    }
    fun confirmed(c:Context,link:RideServiceLink) {
        synchronized(lock) {
            val all=data(c);val row=names(all).optJSONObject(link.rideId)
            if(link.service==RideService.DREEVE && row?.optBoolean("manual")!=true) task(all,link,null,read=true)
            else row?.optString("name")?.takeIf { it.isNotBlank() }?.let { task(all,link,it) }
            save(c,all)
        };schedule(c)
    }
    fun forget(c:Context,id:String)=synchronized(lock) {
        val all=data(c);names(all).remove(id);val jobs=tasks(all)
        jobs.keys().asSequence().toList().filter { jobs.getJSONObject(it).optString("ride")==id }.forEach(jobs::remove);save(c,all)
    }
    fun missing(c:Context,link:RideServiceLink)=synchronized(lock) {
        val all=data(c);tasks(all).remove(key(link));save(c,all)
    }
    private fun current(c:Context,job:JSONObject):Boolean=when(RideService.valueOf(job.getString("service"))) {
        RideService.DREEVE->ActivityServiceSettings(c,ActivityService.DREEVE).read().identity(ActivityService.DREEVE)==job.getString("account")
        RideService.STRAVA->ActivityServiceSettings(c,ActivityService.STRAVA).read().identity(ActivityService.STRAVA)==job.getString("account")
        RideService.INTERVALS->IntervalsSettings(c).read().athlete==job.getString("account")
    }
    fun settingsChanged(c:Context) {
        synchronized(lock) { val all=data(c);val jobs=tasks(all);jobs.keys().forEach { key -> val job=jobs.getJSONObject(key);if(job.optString("state")=="failed" && current(c,job)) job.put("state","pending") };save(c,all) }
        schedule(c)
    }
    fun schedule(c:Context) {
        val jobs=synchronized(lock) { tasks(data(c)) }
        val pending=jobs.keys().asSequence().any { val it=jobs.getJSONObject(it);it.optString("state")=="pending" && current(c,it) }
        val scheduler=c.getSystemService(JobScheduler::class.java)
        if(!pending) return
        if(scheduler.getPendingJob(JOB)==null) scheduler.schedule(JobInfo.Builder(JOB,ComponentName(c,RideNameService::class.java))
            .setPersisted(true).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setBackoffCriteria(60000,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
    }
    /** Explicit pull: Dreeve wins locally, without scheduling any outbound name/file transfers. */
    fun pullDreeve(c:Context,account:String,localNames:Map<String,String>,exists:(String)->Boolean,
        cancelled:()->Boolean,read:(RideServiceLink)->String,progress:(Int,Int)->Unit={ _,_-> }):DreeveNameSyncResult=synchronized(workerLock) {
        fun current()= !cancelled() && ActivityServiceSettings(c,ActivityService.DREEVE).read().identity(ActivityService.DREEVE)==account
        val links=RideServiceLinks.forAccount(c,RideService.DREEVE,account)
            .filter { it.rideId in localNames && exists(it.rideId) }.groupBy { it.rideId }.values.map { it.last() }
        var updated=0;var unchanged=0;var failed=0
        fun state(all:JSONObject,id:String)=names(all).optJSONObject(id)?.toString() to tasks(all).keys().asSequence()
            .filter { tasks(all).getJSONObject(it).optString("ride")==id }.sorted()
            .map { it to tasks(all).getJSONObject(it).toString() }.toList()
        progress(0,links.size)
        for(link in links) {
            if(!current()) return@synchronized DreeveNameSyncResult(updated,unchanged,failed,true)
            val before=synchronized(lock) { state(data(c),link.rideId) }
            try {
                val name=read(link).trim().take(120)
                require(name.isNotBlank() && name.none(Char::isISOControl))
                if(!current()) return@synchronized DreeveNameSyncResult(updated,unchanged,failed,true)
                val changed=synchronized(lock) {
                    val all=data(c)
                    check(current() && exists(link.rideId) && link in RideServiceLinks.links(c,link.rideId) && state(all,link.rideId)==before)
                    val old=names(all).optJSONObject(link.rideId)?.optString("name") ?: localNames.getValue(link.rideId)
                    names(all).put(link.rideId,JSONObject().put("name",name).put("manual",false))
                    // Cancel stale writes/reads, including other services, without creating new tasks.
                    val jobs=tasks(all);jobs.keys().asSequence().toList().filter { jobs.getJSONObject(it).optString("ride")==link.rideId }.forEach(jobs::remove)
                    save(c,all);old!=name
                }
                if(changed) updated++ else unchanged++
            } catch(_:Exception) {
                if(!current()) return@synchronized DreeveNameSyncResult(updated,unchanged,failed,true)
                failed++
            }
            progress(updated+unchanged+failed,links.size)
        }
        DreeveNameSyncResult(updated,unchanged,failed)
    }
    fun run(c:Context,cancelled:()->Boolean,read:((RideServiceLink)->String)?=null,write:((RideServiceLink,String)->Unit)?=null,
        wait:(Long,()->Boolean)->Boolean=ActivityServiceUploads::waitForConfirmation,round:Int=0):Boolean=synchronized(workerLock) {
        val snapshot=synchronized(lock) { tasks(data(c)) }
        var retry=false
        for(key in snapshot.keys().asSequence().toList()) {
            if(cancelled()) return@synchronized true
            val job=snapshot.getJSONObject(key)
            if(job.optString("state")!="pending" || !current(c,job)) continue
            val token=job.getString("token")
            fun valid()= !cancelled() && current(c,job) && synchronized(lock) { tasks(data(c)).optJSONObject(key)?.optString("token")==token }
            val link=RideServiceLink(job.getString("ride"),RideService.valueOf(job.getString("service")),job.getString("account"),job.getString("activity"),"")
            try {
                val delay=(job.getLong("after")-System.currentTimeMillis()).coerceAtLeast(0)
                if(delay>0 && !wait(minOf(delay,3000),{ !valid() })) { retry=true;continue }
                if(!valid()) continue
                if(job.getBoolean("read")) {
                    val result=(read?.invoke(link) ?: DreeveClient(ActivityServiceSettings(c,ActivityService.DREEVE).read(),
                        ActivityServiceSettings(c,ActivityService.DREEVE).credentials().getString("secret"),{ !valid() }).activityName(link.activityId)).trim().take(120)
                    require(result.isNotBlank())
                    val links=RideServiceLinks.links(c,link.rideId)
                    synchronized(lock) {
                        val all=data(c);val latest=tasks(all).optJSONObject(key)
                        if(latest?.optString("token")==token && names(all).optJSONObject(link.rideId)?.optBoolean("manual")!=true) {
                            names(all).put(link.rideId,JSONObject().put("name",result).put("manual",false))
                            tasks(all).remove(key);links.filter { it.service!=RideService.DREEVE }.forEach { task(all,it,result) };save(c,all)
                        }
                    }
                } else {
                    val name=job.getString("name")
                    if(write!=null) write(link,name) else when(link.service) {
                        RideService.DREEVE->{ val settings=ActivityServiceSettings(c,ActivityService.DREEVE)
                            DreeveClient(settings.read(),settings.credentials().getString("secret"),{ !valid() }).rename(link.activityId,name) }
                        RideService.STRAVA->StravaClient(ActivityServiceSettings(c,ActivityService.STRAVA),{ !valid() }).rename(link.activityId,name)
                        RideService.INTERVALS->IntervalsClient(IntervalsSettings(c).read(),IntervalsSettings(c).apiKey(),{ !valid() }).rename(link.activityId,name)
                    }
                    synchronized(lock) { val all=data(c);if(tasks(all).optJSONObject(key)?.optString("token")==token) { tasks(all).remove(key);save(c,all) } }
                }
            } catch(e:Exception) {
                val temporary=cancelled() || e is IOException && (e !is ActivityServiceFailure || e.retryable) && (e !is IntervalsFailure || e.retryable)
                retry=retry || temporary
                synchronized(lock) { val all=data(c);val item=tasks(all).optJSONObject(key)
                    if(item?.optString("token")==token) { item.put("state",if(temporary) "pending" else "failed");save(c,all) } }
            }
        }
        val pending=synchronized(lock) { val jobs=tasks(data(c));jobs.keys().asSequence().any { jobs.getJSONObject(it).optString("state")=="pending" && current(c,jobs.getJSONObject(it)) } }
        if(pending && !retry && round<1) run(c,cancelled,read,write,wait,round+1) else retry || pending
    }
}
class RideNameService:JobService() {
    private var stopped=AtomicBoolean(false)
    override fun onStartJob(params:JobParameters):Boolean {
        val token=AtomicBoolean(false);stopped=token
        Thread { val retry=runCatching { RideNames.run(applicationContext,token::get) }.getOrDefault(true)
            if(!token.get()) jobFinished(params,retry) }.start();return true
    }
    override fun onStopJob(params:JobParameters):Boolean { stopped.set(true);return true }
}
