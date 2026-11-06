package de.bcycle.app

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Global workout collection, independent of rides and of the active bike; atomic, bounded updates. */
internal class TrainingStore(private val directory:File) {
    private val index get()=AtomicFile(File(directory,"workouts.json"))
    fun read():List<TrainingWorkout> = synchronized(LOCK) {
        val file=index
        if(!file.baseFile.exists() && !File(file.baseFile.path+".bak").exists()) return emptyList()
        val bytes=file.openRead().use { it.readNBytes(MAX_BYTES+1) };require(bytes.size<=MAX_BYTES)
        val root=JSONObject(bytes.toString(Charsets.UTF_8));require(root.getInt("schema")==1)
        val rows=root.getJSONArray("workouts");require(rows.length()<=500)
        return (0 until rows.length()).map { TrainingWorkout.decode(rows.getJSONObject(it)) }.also { require(it.map { w -> w.id }.toSet().size==it.size) }
    }
    fun merge(workouts:List<TrainingWorkout>):List<TrainingWorkout> = synchronized(LOCK) {
        val current=read().associateBy { it.id }.toMutableMap()
        workouts.forEach { current[it.id]=it }
        return current.values.toList().also(::write)
    }
    fun delete(ids:Set<String>)=synchronized(LOCK) { read().filterNot { it.id in ids }.also(::write) }
    private fun write(rows:List<TrainingWorkout>) {
        require(rows.size<=500 && rows.sumOf { it.steps.size }<=50000)
        val bytes=JSONObject().put("schema",1).put("workouts",JSONArray(rows.map { it.json() })).toString().toByteArray()
        require(bytes.size<=MAX_BYTES);check(directory.isDirectory || directory.mkdirs())
        val atomic=index;val out=atomic.startWrite()
        try { out.write(bytes);atomic.finishWrite(out) } catch(error:Exception) { atomic.failWrite(out);throw error }
    }
    companion object { private val LOCK=Any();private const val MAX_BYTES=16*1024*1024 }
}

/** All storage/import/network work shares a serial worker. No BLE, ride, upload or calendar writes. */
internal class TrainingRepository(context:android.content.Context,
    private val settings:IntervalsSettings=IntervalsSettings(context),
    private val network:(IntervalsConfig)->Boolean={ IntervalsHrvUploads.network(context.applicationContext,it) },
    private val api:(IntervalsConfig,String,()->Boolean)->IntervalsClient={ config,key,cancel -> IntervalsClient(config,key,cancelled=cancel) }) {
    private val app=context.applicationContext
    private val main=android.os.Handler(android.os.Looper.getMainLooper())
    private val worker=java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it,"B-Cycle-Training") }
    private val store=TrainingStore(File(app.filesDir,"training-workouts"))
    private fun <T> task(done:(Result<T>)->Unit,block:()->T) {
        worker.execute { val result=runCatching(block);main.post { done(result) } }
    }
    fun close() { worker.shutdown() }
    fun read(done:(Result<List<TrainingWorkout>>)->Unit)=task(done) { store.read() }
    fun original(workout:TrainingWorkout,done:(Result<JSONObject?>)->Unit)=task(done) { workout.sourceData?.let(::JSONObject) }
    fun delete(ids:Set<String>,done:(Result<List<TrainingWorkout>>)->Unit)=task(done) { store.delete(ids) }
    fun importFile(uri:android.net.Uri,done:(Result<List<TrainingWorkout>>)->Unit)=task(done) {
        val name=app.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c -> if(c.moveToFirst()) c.getString(0) else null } ?: uri.lastPathSegment.orEmpty()
        val bytes=app.contentResolver.openInputStream(uri)?.use { it.readNBytes(TrainingFileImport.MAX_BYTES+1) } ?: error("Missing workout file")
        store.merge(listOf(TrainingFileImport.read(bytes,name)))
    }
    fun download(cancelled:()->Boolean,progress:(Int,Int)->Unit,done:(Result<List<TrainingWorkout>>)->Unit)=task(done) {
        val config=settings.read();val token=settings.connectionToken()
        if(config.athlete.isBlank() || !settings.hasKey()) throw IntervalsFailure(false,R.string.training_cloud_configure)
        fun checkActive() {
            if(cancelled()) throw java.io.InterruptedIOException()
            if(settings.connectionToken()!=token) throw IntervalsFailure(false,R.string.intervals_bulk_changed)
            if(!network(config)) throw IntervalsFailure(true,R.string.intervals_bulk_wifi)
        }
        checkActive()
        val client=api(config,settings.apiKey()) { cancelled() || settings.connectionToken()!=token || !network(config) }
        val library=client.workoutLibrary();val parsed=ArrayList<TrainingWorkout>();var bytesRead=0L
        for((index,summary) in library.withIndex()) {
            checkActive();main.post { progress(index,library.size) }
            val row=client.workout(summary.getLong("id"))
            bytesRead+=row.toString().toByteArray(Charsets.UTF_8).size;require(bytesRead<=32L*1024*1024)
            val imported=IntervalsTraining.read(row) ?: run {
                val bytes=client.workoutFit(row);bytesRead+=bytes.size;require(bytesRead<=32L*1024*1024)
                TrainingFileImport.read(bytes,"${row.getString("name")}.fit").copy(sourceData=row.toString(),structureSource="fit-conversion")
            }
            val tags=row.optJSONArray("tags")?.let { a -> (0 until minOf(a.length(),100)).map { a.getString(it).take(100) } }.orEmpty()
            parsed+=imported.copy(id=TrainingWorkout.id("intervals\u0000${config.athlete}\u0000${row.getLong("id")}".toByteArray()),name=row.getString("name").take(512),description=(if(row.isNull("description")) imported.description else row.optString("description").ifBlank { imported.description }).take(20000),sport=row.optString("type").ifBlank { imported.sport },source="Intervals.icu",tags=tags)
            require(parsed.sumOf { it.steps.size }<=50000)
        }
        checkActive();store.merge(parsed)
    }
}
