package de.bcycle.app

import android.content.Context
import org.json.JSONObject

/** A ride's subjective feedback; deliberately unrelated to the HRV day questionnaire. */
internal data class RideAnnotation(val note:String="",val rpe:Int?=null) {
    init { require(note.length<=MAX_NOTE && (rpe==null || rpe in 0..10)) }
    val empty get()=note.isEmpty() && rpe==null
    fun json()=JSONObject().put("note",note).put("rpe",rpe ?: JSONObject.NULL)
    companion object {
        const val MAX_NOTE=6000
        fun decode(row:JSONObject)=RideAnnotation(row.getString("note"),if(row.isNull("rpe")) null else row.getInt("rpe").also { require(row.getDouble("rpe")==it.toDouble()) })
    }
}
internal object RideAnnotations {
    private val lock=Any()
    private val worker=java.util.concurrent.Executors.newSingleThreadScheduledExecutor { Thread(it,"B-Cycle-RideFeedback") }
    private val pending=mutableMapOf<String,java.util.concurrent.ScheduledFuture<*>>()
    private fun prefs(c:Context)=c.getSharedPreferences("ride-annotations",0)
    fun problem(c:Context,id:String)=prefs(c).getBoolean("problem:$id",false)
    fun contains(c:Context,id:String)=prefs(c).contains(id)
    fun read(c:Context,id:String)=synchronized(lock) { prefs(c).getString(id,null)?.let { RideAnnotation.decode(JSONObject(it)) } ?: RideAnnotation() }
    fun note(c:Context,id:String,note:String)=change(c,id) { it.copy(note=note) }
    fun rpe(c:Context,id:String,rpe:Int)=change(c,id) { it.copy(rpe=rpe) }
    private fun change(c:Context,id:String,update:(RideAnnotation)->RideAnnotation) {
        require(id.matches(Regex("(recorded|imported)-[a-f0-9]{64}|[a-f0-9-]{36}")))
        val app=c.applicationContext
        synchronized(lock) {
            val next=update(read(app,id));if(next==read(app,id)) return
            // apply updates memory synchronously; Android completes pending writes at lifecycle stops.
            prefs(app).edit().putString(id,next.json().toString()).putBoolean("dirty:$id",true).apply()
            schedule(app,id,1200)
        }
    }
    private fun schedule(c:Context,id:String,delay:Long) {
        pending.remove(id)?.cancel(false)
        pending[id]=worker.schedule({ flush(c,id) },delay,java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    fun flushAsync(c:Context,id:String)=synchronized(lock) { if(prefs(c).getBoolean("dirty:$id",false)) schedule(c.applicationContext,id,0) }
    fun recover(c:Context)=synchronized(lock) { prefs(c).all.keys.filter { it.startsWith("dirty:") }.forEach { schedule(c.applicationContext,it.removePrefix("dirty:"),0) } }
    internal fun flush(c:Context,id:String) {
        if(!prefs(c).getBoolean("dirty:$id",false)) return
        val snapshot=read(c,id)
        try {
            RideNames.annotated(c,id,snapshot)
            if(LocalRouteExport.has(c,id)) LocalRouteExport.update(c,id,RideAnnotationFiles.current(c,id))
            if(RouteUploads.eligible(c,id) && RouteExportSettings(c).read().webdav && RouteUploads.wasQueued(c,id)) RouteUploads.retry(c,id)
            synchronized(lock) { if(read(c,id)==snapshot) check(prefs(c).edit().remove("dirty:$id").remove("problem:$id").commit()) }
        } catch(_:Exception) {
            // The note itself is still local. Keep the retry marker and expose external-save failure.
            if(contains(c,id)) prefs(c).edit().putBoolean("problem:$id",true).apply()
        }
    }
    fun restore(c:Context,id:String,row:JSONObject) { val value=RideAnnotation.decode(row);check(prefs(c).edit().putString(id,value.json().toString()).commit()) }
    fun forget(c:Context,id:String)=synchronized(lock) { pending.remove(id)?.cancel(false);prefs(c).edit().remove(id).remove("dirty:$id").remove("problem:$id").apply() }
    fun description(existing:String,value:RideAnnotation,includeRpe:Boolean=true):String {
        val start="[B|CYCLE]";val end="[/B|CYCLE]"
        val pattern=Regex("(?s)\\n?\\n?\\[B\\|CYCLE]\\n.*?\\n\\[/B\\|CYCLE]")
        val preserved=existing.replace(pattern,"").trimEnd()
        val parts=buildList { if(value.note.isNotBlank()) add(value.note.replace("[B|CYCLE]","［B|CYCLE］").replace("[/B|CYCLE]","［/B|CYCLE］"));if(includeRpe && value.rpe!=null) add("RPE: ${value.rpe}/10") }
        return if(parts.isEmpty()) preserved else listOf(preserved,"$start\n${parts.joinToString("\n\n")}\n$end").filter { it.isNotEmpty() }.joinToString("\n\n")
    }
}
