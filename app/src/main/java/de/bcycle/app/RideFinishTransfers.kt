package de.bcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/** Durable, non-secret transfer intent. The internal ride is retained before this is written. */
internal object RideFinishTransfers {
    private val lock=Any()
    private val worker=Executors.newSingleThreadExecutor { Thread(it,"B-Cycle-RideFinish") }
    private fun prefs(c:Context)=c.getSharedPreferences("ride-finish-transfers",0)
    fun destinations(c:Context):Set<RecordingDestination> = buildSet {
        val local=RouteExportSettings(c).read()
        if(local.save) add(RecordingDestination.LOCAL_FOLDER)
        if(local.webdav) add(RecordingDestination.WEBDAV)
        if(IntervalsSettings(c).read().enabled) add(RecordingDestination.INTERVALS)
        ActivityService.entries.filter { ActivityServiceSettings(c,it).read().enabled }.forEach {
            add(if(it==ActivityService.DREEVE) RecordingDestination.DREEVE else RecordingDestination.STRAVA)
        }
    }
    private fun account(c:Context,target:RecordingDestination):String {
        val identity=when(target) {
            RecordingDestination.LOCAL_FOLDER->"local"
            RecordingDestination.WEBDAV->RouteExportSettings(c).read().let { "${it.server}\u0000${it.user}\u0000${it.folder}" }
            RecordingDestination.INTERVALS->IntervalsSettings(c).read().athlete
            RecordingDestination.DREEVE->ActivityServiceSettings(c,ActivityService.DREEVE).read().address.trim().trimEnd('/')
            RecordingDestination.STRAVA->ActivityServiceSettings(c,ActivityService.STRAVA).read().account
        }
        return java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    fun hold(c:Context,id:String,name:String,targets:Set<RecordingDestination>,tree:String) = synchronized(lock) {
        val row=JSONObject().put("held",true).put("name",name).put("tree",tree)
            .put("remaining",JSONArray(targets.map { it.name }))
            .put("accounts",JSONObject().apply { targets.forEach { put(it.name,account(c,it)) } })
        check(prefs(c).edit().putString(id,row.toString()).commit())
    }
    internal fun held(c:Context,id:String)=synchronized(lock) {
        prefs(c).getString(id,null)?.let { JSONObject(it).getBoolean("held") } ?: false
    }
    fun forget(c:Context,id:String)=synchronized(lock) { prefs(c).edit().remove(id).apply() }
    /** Every dismissal uses this one idempotent release path; callbacks never do file/network IO. */
    fun release(c:Context,id:String) {
        val app=c.applicationContext
        val exists=synchronized(lock) {
            val text=prefs(app).getString(id,null) ?: return@synchronized false
            val row=JSONObject(text);if(!row.getBoolean("held")) return@synchronized false
            row.put("held",false)
            prefs(app).edit().putString(id,row.toString()).apply();true
        }
        if(exists) worker.execute { runPending(app,id) }
    }
    /** A process death must not strand a retained ride or lose the chosen destinations. */
    fun recover(c:Context) {
        val app=c.applicationContext
        val ids=synchronized(lock) {
            prefs(app).all.keys.toList().also { ids ->
                val edit=prefs(app).edit()
                ids.forEach { id -> prefs(app).getString(id,null)?.let { edit.putString(id,JSONObject(it).put("held",false).toString()) } }
                check(edit.commit())
            }
        }
        ids.forEach { worker.execute { runPending(app,it) } }
    }
    internal fun runPending(c:Context,id:String,
        local:(File,String,String)->Unit={ file,name,tree ->
            RouteLocalSaves.perform(c,id,RouteTransferPayload.file(c,id),tree.ifEmpty { "Downloads/B-Cycle" }) {
                if(LocalRouteExport.has(c,id)) LocalRouteExport.update(c,id,file) else LocalRouteExport.write(c,file,name,tree)
            }
        },cloud:(RecordingDestination,File,String)->Unit={ target,file,name ->
            when(target) {
                RecordingDestination.WEBDAV->RouteUploads.enqueue(c,file,name)
                RecordingDestination.INTERVALS->IntervalsUploads.enqueue(c,file,name)
                RecordingDestination.DREEVE->ActivityServiceUploads.forService(ActivityService.DREEVE).enqueue(c,file)
                RecordingDestination.STRAVA->ActivityServiceUploads.forService(ActivityService.STRAVA).enqueue(c,file)
                else->Unit
            }
        }) {
        val snapshot=synchronized(lock) { prefs(c).getString(id,null)?.let(::JSONObject) } ?: return
        if(snapshot.getBoolean("held")) return
        val remaining=snapshot.getJSONArray("remaining").let { rows -> (0 until rows.length()).map { RecordingDestination.valueOf(rows.getString(it)) } }
        var failed=false
        for(target in remaining) {
            if(!synchronized(lock) { prefs(c).contains(id) }) return
            try {
                check(snapshot.optJSONObject("accounts")?.optString(target.name)==account(c,target)) { "Transfer account changed" }
                // External activity APIs receive their existing immutable payload plus the
                // metadata update on confirmation. WebDAV/local copies include feedback now.
                val file=if(target==RecordingDestination.LOCAL_FOLDER || target==RecordingDestination.WEBDAV)
                    RouteTransferPayload.exportFile(c,id) else RouteTransferPayload.file(c,id)
                if(target==RecordingDestination.LOCAL_FOLDER) local(file,snapshot.getString("name"),snapshot.getString("tree"))
                else cloud(target,file,snapshot.getString("name"))
                synchronized(lock) {
                    prefs(c).getString(id,null)?.let { text ->
                        val row=JSONObject(text);val old=row.getJSONArray("remaining")
                        row.put("remaining",JSONArray((0 until old.length()).map { old.getString(it) }.filterNot { it==target.name }))
                        check(prefs(c).edit().putString(id,row.toString()).commit())
                    }
                }
            } catch(_:Exception) { failed=true /* Keep only failed destinations for the next recovery. */ }
        }
        synchronized(lock) {
            prefs(c).getString(id,null)?.let { text ->
                if(JSONObject(text).getJSONArray("remaining").length()==0) check(prefs(c).edit().remove(id).commit())
            }
        }
        // A note can change in details while the first local copy is being written.
        // Reconcile it after the destination is registered, without holding the UI open.
        RideAnnotations.resync(c,id)
        if(failed) android.os.Handler(android.os.Looper.getMainLooper()).post {
            BicycleFeedback.show(c,R.string.ride_finish_pending,true)
        }
    }
}
