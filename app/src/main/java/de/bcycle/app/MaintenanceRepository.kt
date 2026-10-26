package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

internal data class MaintenanceSnapshot(val tasks:List<DreeveMaintenanceTask>,val gears:Map<String,String>,val fetchedAt:Long,val incompleteNames:Boolean=false)
internal data class MaintenanceState(val snapshot:MaintenanceSnapshot?=null,val loading:Boolean=false,val problem:Int?=null,val gearProblem:Int?=null)
internal class MaintenanceAccount(val config:ActivityServiceConfig,val key:String) {
    val owner=java.security.MessageDigest.getInstance("SHA-256").digest((config.identity(ActivityService.DREEVE)+"\u0000"+key).toByteArray())
        .joinToString("") { "%02x".format(it) }
}
internal data class MaintenancePayload(val tasks:List<DreeveMaintenanceTask>,val gears:Map<String,String>,val gearProblem:Int?=null)

/** One serialized read-only worker per app; cached data never crosses address/key identities. */
internal class MaintenanceRepository(private val context:Context,
    private val account:()->MaintenanceAccount?={ configured(context) },
    private val fetch:(MaintenanceAccount,()->Boolean)->MaintenancePayload={ owner,cancelled ->
        val client=DreeveClient(owner.config,owner.key,cancelled,transport=ActivityHttp.brief(cancelled))
        val tasks=client.maintenance();val names=runCatching { client.gears() }
        MaintenancePayload(tasks,names.getOrDefault(emptyMap()),names.exceptionOrNull()?.let { ServiceProblems.text(it,R.string.maintenance_failed) })
    },private val clock:()->Long=System::currentTimeMillis):AutoCloseable {
    enum class Reason { AUTO, MANUAL, UPLOAD }
    private val worker=Executors.newSingleThreadExecutor { Thread(it,"B-Cycle-maintenance").apply { isDaemon=true } }
    private val main=Handler(Looper.getMainLooper())
    private val lock=Any()
    private val generation=AtomicLong()
    private val observers=linkedSetOf<(MaintenanceState)->Unit>()
    private val prefs=context.getSharedPreferences("activity-service-dreeve",0)
    private val refreshPrefs=context.getSharedPreferences("maintenance-refresh",0)
    private var running=false
    private var fetching=false
    private var pending:Reason?=null
    @Volatile private var closed=false
    @Volatile var state=MaintenanceState();private set
    // Worker-owned state.
    private var owner:String?=null
    private var cached:MaintenanceSnapshot?=null
    private var lastAttempt:Long?=null
    private var attemptedUploadToken:String?=null
    private var lastProblem:Int?=null
    private var lastGearProblem:Int?=null
    private var cachedUploadToken=""
    private val settingsListener=SharedPreferences.OnSharedPreferenceChangeListener { _,key ->
        if(key==null || key in setOf("address","password")) {
            generation.incrementAndGet();publish(MaintenanceState(),generation.get())
            synchronized(lock) { if(running) pending=Reason.MANUAL }
            refresh(Reason.MANUAL)
        }
    }
    init { prefs.registerOnSharedPreferenceChangeListener(settingsListener) }
    fun observe(observer:(MaintenanceState)->Unit):AutoCloseable {
        synchronized(lock) { observers+=observer }
        main.post { if(!closed && synchronized(lock) { observer in observers }) observer(state) }
        return AutoCloseable { synchronized(lock) { observers-=observer } }
    }
    fun refresh(reason:Reason=Reason.AUTO) {
        synchronized(lock) {
            if(closed) return
            if(reason==Reason.UPLOAD) refreshPrefs.edit().putString("uploadToken",java.util.UUID.randomUUID().toString()).commit()
            if(running) {
                if(reason==Reason.UPLOAD || reason==Reason.MANUAL && !fetching) pending=reason
                return
            }
            running=true
            worker.execute {
                var next:Reason?=reason
                while(next!=null && !closed) {
                    val stamp=generation.get()
                    try { update(next!!,stamp) } catch(error:Exception) {
                        lastProblem=ServiceProblems.text(error,R.string.maintenance_failed)
                        publish(MaintenanceState(cached,problem=lastProblem),stamp)
                    }
                    synchronized(lock) { fetching=false }
                    next=synchronized(lock) { pending.also { pending=null;if(it==null) running=false } }
                }
            }
        }
    }
    private fun update(reason:Reason,stamp:Long) {
        val identity=try { account() } catch(error:Exception) {
            owner=null;cached=null;lastAttempt=null;lastProblem=ServiceProblems.text(error,R.string.maintenance_failed)
            publish(MaintenanceState(problem=lastProblem),stamp);return
        }
        if(identity==null) {
            owner=null;cached=null;lastAttempt=null;lastProblem=R.string.dreeve_import_config_missing
            publish(MaintenanceState(problem=lastProblem),stamp);return
        }
        if(owner!=identity.owner) {
            owner=identity.owner;cachedUploadToken="";cached=readCache(identity.owner);lastAttempt=null;attemptedUploadToken=null;lastProblem=null;lastGearProblem=null
        }
        val now=clock()
        val uploadToken=refreshPrefs.getString("uploadToken","")!!
        val recent=lastAttempt?.takeIf { attemptedUploadToken==uploadToken }
            ?: cached?.fetchedAt?.takeIf { cachedUploadToken==uploadToken }
        if(reason==Reason.AUTO && recent!=null && now-recent in 0 until COALESCE_MS) {
            publish(MaintenanceState(cached,problem=lastProblem,gearProblem=lastGearProblem),stamp);return
        }
        lastAttempt=now;attemptedUploadToken=uploadToken
        synchronized(lock) { fetching=true }
        publish(MaintenanceState(cached,loading=true),stamp)
        val result=runCatching { fetch(identity) { closed || generation.get()!=stamp } }
        if(closed || generation.get()!=stamp || runCatching { account()?.owner }.getOrNull()!=identity.owner) return
        result.fold({ payload ->
            val data=MaintenanceSnapshot(payload.tasks.toList(),
                if(payload.gearProblem!=null) cached?.gears.orEmpty()+payload.gears else payload.gears.toMap(),clock(),payload.gearProblem!=null)
            val saved=runCatching { writeCache(identity.owner,data,uploadToken) }.isSuccess
            if(saved) cachedUploadToken=uploadToken
            cached=data;lastProblem=if(saved) null else R.string.maintenance_cache_failed;lastGearProblem=payload.gearProblem
            publish(MaintenanceState(data,problem=lastProblem,gearProblem=payload.gearProblem),stamp)
        },{ error ->
            lastProblem=ServiceProblems.text(error,R.string.maintenance_failed)
            publish(MaintenanceState(cached,problem=lastProblem,gearProblem=lastGearProblem),stamp)
        })
    }
    private fun publish(value:MaintenanceState,stamp:Long) {
        main.post {
            if(closed || generation.get()!=stamp) return@post
            state=value;synchronized(lock) { observers.toList() }.forEach { it(value) }
        }
    }
    private fun cacheFile()=AtomicFile(File(context.noBackupFilesDir,"dreeve-maintenance.json"))
    private fun readCache(identity:String):MaintenanceSnapshot?=runCatching {
        val raw=cacheFile().openRead().use { it.readNBytes(MAX_BYTES+1) };require(raw.size<=MAX_BYTES)
        val json=JSONObject(String(raw,Charsets.UTF_8));require(json.getInt("version")==1)
        if(json.getString("owner")!=identity) return@runCatching null
        val tasks=DreeveMaintenanceTask.decode(json)
        val names=json.getJSONObject("gears");require(names.length()<=10000)
        val gears=names.keys().asSequence().associateWith { key -> names.getString(key).also { require(it.length<=120 && key.length<=512) } }
        val at=json.getLong("fetchedAt");require(at>0)
        cachedUploadToken=json.optString("uploadToken").also { require(it.length<=64) }
        MaintenanceSnapshot(tasks,gears,at,json.optBoolean("incompleteNames",false))
    }.getOrNull()
    private fun writeCache(identity:String,data:MaintenanceSnapshot,uploadToken:String) {
        val tasks=JSONArray().apply { data.tasks.forEach { task -> put(JSONObject().apply {
            put("id",task.id);put("label",task.label);put("component",JSONObject().put("label",task.component).put("id",task.componentId ?: JSONObject.NULL))
            put("gearIds",JSONArray(task.gearIds));put("interval",JSONObject().put("value",task.interval).put("unit",task.unit))
            put("lastPerformedOn",task.lastPerformed?.toString() ?: JSONObject.NULL)
            put("elapsed",task.elapsed ?: JSONObject.NULL);put("remaining",task.remaining ?: JSONObject.NULL);put("percentage",task.percentage ?: JSONObject.NULL)
            put("isDue",task.due);put("isOverdue",task.overdue)
        }) } }
        val bytes=JSONObject().put("version",1).put("owner",identity).put("fetchedAt",data.fetchedAt).put("uploadToken",uploadToken)
            .put("incompleteNames",data.incompleteNames).put("maintenanceTasks",tasks).put("gears",JSONObject(data.gears)).toString().toByteArray()
        require(bytes.size<=MAX_BYTES)
        val file=cacheFile();val output=file.startWrite()
        try { output.write(bytes);file.finishWrite(output) } catch(error:Exception) { file.failWrite(output);throw error }
    }
    override fun close() {
        synchronized(lock) { closed=true;pending=null;observers.clear();generation.incrementAndGet() }
        prefs.unregisterOnSharedPreferenceChangeListener(settingsListener);worker.shutdownNow()
    }
    companion object {
        const val COALESCE_MS=60000L
        private const val MAX_BYTES=4*1024*1024
        private var instance:MaintenanceRepository?=null
        @Synchronized fun get(context:Context):MaintenanceRepository {
            val app=context.applicationContext
            return instance?.takeIf { it.context===app } ?: MaintenanceRepository(app).also { instance?.close();instance=it }
        }
        private fun configured(context:Context):MaintenanceAccount? = synchronized(ActivityServiceSettings.lock) {
            val settings=ActivityServiceSettings(context,ActivityService.DREEVE);val config=settings.read()
            val key=settings.credentials().optString("secret")
            if(config.address.isBlank() || key.isBlank()) null else MaintenanceAccount(config,key)
        }
    }
}
