package de.bcycle.app

import android.util.AtomicFile
import de.bcycle.hrv.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Private person-level measurements. Worker only; result.json is the completion marker. */
internal class HrvStore(private val root:File) {
    init { check(root.isDirectory || root.mkdirs()) }
    companion object {
        const val POLICY="bcycle-hrv-v2-low-coverage-warning"
        const val MAX_BYTES=4*1024*1024
        fun validId(id:String)=runCatching { UUID.fromString(id).toString()==id }.getOrDefault(false)
    }
    private fun directory(id:String)=File(root,id).also { require(validId(id)) }
    private fun atomic(file:File,bytes:ByteArray) {
        val target=AtomicFile(file);val stream=target.startWrite()
        try { stream.write(bytes);target.finishWrite(stream) } catch(e:Exception) { target.failWrite(stream);throw e }
    }
    fun begin(id:String,utc:Long,sensor:String,session:MeasurementSession) {
        val dir=directory(id);check(dir.mkdir())
        atomic(File(dir,"draft.json"),JSONObject().put("id",id).put("utcMs",utc).put("sensor",sensor)
            .put("zone",java.time.ZoneId.systemDefault().id).put("restMs",session.protocol.restMs)
            .put("measureMs",session.protocol.measureMs).toString().toByteArray())
    }
    fun append(id:String,packet:CapturedPacket) {
        val row=JSONObject().put("elapsedMs",packet.elapsedMs).put("bytes",android.util.Base64.encodeToString(packet.bytes,android.util.Base64.NO_WRAP))
        FileOutputStream(File(directory(id),"capture.jsonl"),true).use { it.write((row.toString()+"\n").toByteArray());it.fd.sync() }
    }
    private fun metrics(value:Metrics?):Any=value?.let {
        JSONObject().put("count",it.count).put("meanNnMs",it.meanNnMs).put("rmssdMs",it.rmssdMs)
            .put("lnRmssd",it.lnRmssd ?: JSONObject.NULL).put("sdnnMs",it.sdnnMs)
            .put("pnn50Percent",it.pnn50Percent).put("restingHrBpm",it.restingHrBpm)
    } ?: JSONObject.NULL
    fun save(id:String,session:MeasurementSession,evaluation:Evaluation,now:Long):JSONObject {
        val dir=directory(id)
        val draft=JSONObject(AtomicFile(File(dir,"draft.json")).openRead().bufferedReader().use { it.readText() })
        val diagnostics=session.diagnostics(now)
        val correction=evaluation.correction
        val row=draft.put("format","bicycle-hrv").put("schema",1).put("phase",session.phase.name)
            .put("problem",session.problem ?: JSONObject.NULL).put("policy",POLICY).put("algorithm",ArtifactCorrection.VERSION)
            .put("acceptable",evaluation.acceptable).put("reasons",JSONArray(evaluation.reasons)).put("warnings",JSONArray(evaluation.warnings))
            .put("coverage",evaluation.coverage).put("affectedFraction",evaluation.affectedFraction)
            .put("measurementElapsedMs",diagnostics.measurementElapsedMs).put("rrDurationMs",diagnostics.rrDurationMs)
            .put("packetCount",session.packets.size).put("rrTicks",JSONArray(session.rrTicks))
            .put("raw",metrics(evaluation.raw)).put("corrected",metrics(evaluation.corrected))
            .put("nnMs",JSONArray(correction?.nnMs ?: emptyList<Double>()))
            .put("affectedRawIndices",JSONArray(correction?.affectedRawIndices ?: emptyList<Int>()))
            .put("correctionPasses",JSONArray().apply { correction?.passes?.forEach { pass ->
                put(JSONObject().put("beforePeaks",JSONArray(pass.beforePeaks)).put("afterPeaks",JSONArray(pass.afterPeaks))
                    .put("ectopic",JSONArray(pass.classes.ectopic)).put("missed",JSONArray(pass.classes.missed))
                    .put("extra",JSONArray(pass.classes.extra)).put("longshort",JSONArray(pass.classes.longshort)))
            } })
        atomic(File(dir,"result.json"),row.toString().toByteArray())
        return row
    }
    fun ids()=root.listFiles().orEmpty().filter { it.isDirectory && validId(it.name) && File(it,"result.json").isFile }.map { it.name }
    fun list():List<JSONObject> = root.listFiles().orEmpty().filter { it.isDirectory && validId(it.name) }
        .mapNotNull { dir -> runCatching { read(dir.name) }.getOrNull() }.sortedByDescending { it.optLong("utcMs") }
    fun read(id:String):JSONObject=AtomicFile(File(directory(id),"result.json")).openRead().use {
        BackupJson.objectValue(it.readNBytes(MAX_BYTES+1),MAX_BYTES).also { row ->
            require(row.getString("format")=="bicycle-hrv" && row.getInt("schema")==1 && row.getString("id")==id)
        }
    }
    fun recover() {
        root.listFiles().orEmpty().filter { it.isDirectory && validId(it.name) && !File(it,"result.json").exists() }.forEach { dir ->
            val draft=JSONObject(AtomicFile(File(dir,"draft.json")).openRead().bufferedReader().use { it.readText() })
            val session=MeasurementSession(0,Protocol(draft.getLong("restMs"),draft.getLong("measureMs")))
            val capture=File(dir,"capture.jsonl").takeIf { it.isFile }?.readBytes() ?: byteArrayOf()
            require(capture.size<=MAX_BYTES)
            capture.toString(Charsets.UTF_8).substringBeforeLast('\n',"").lineSequence().filter { it.isNotBlank() }.take(4096).forEach { line ->
                val packet=JSONObject(line)
                session.accept(packet.getLong("elapsedMs"),android.util.Base64.decode(packet.getString("bytes"),android.util.Base64.DEFAULT))
            }
            session.abort("PROCESS_INTERRUPTED",discardUncommittedCompletion=true)
            save(dir.name,session,Evaluate.session(session),session.packets.lastOrNull()?.elapsedMs ?: 0)
        }
    }
    fun backup(id:String):ByteArray {
        val dir=directory(id);val result=read(id)
        val bytes=File(dir,"capture.jsonl").takeIf { it.isFile }?.readBytes() ?: byteArrayOf()
        require(bytes.size<=MAX_BYTES)
        return JSONObject().put("format","bicycle-hrv-backup").put("schema",1).put("result",result)
            .put("capture",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)).toString().toByteArray().also { require(it.size<=MAX_BYTES) }
    }
    fun restore(bytes:ByteArray):Boolean {
        val backup=BackupJson.objectValue(bytes,MAX_BYTES)
        require(backup.getString("format")=="bicycle-hrv-backup" && backup.getInt("schema")==1)
        val row=backup.getJSONObject("result");val id=row.getString("id")
        require(validId(id) && row.getString("format")=="bicycle-hrv" && row.getInt("schema")==1)
        require(row.getLong("utcMs")>=0 && Phase.valueOf(row.getString("phase")) in setOf(Phase.COMPLETE,Phase.ABORTED))
        val capture=android.util.Base64.decode(backup.getString("capture"),android.util.Base64.DEFAULT)
        require(capture.size<=MAX_BYTES)
        val lines=capture.toString(Charsets.UTF_8)
        require(lines.isEmpty() || lines.endsWith('\n'))
        var count=0;var last=-1L
        lines.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            require(++count<=4096);val packet=JSONObject(line);val time=packet.getLong("elapsedMs")
            require(time>=last && time<=24*60*60*1000L);last=time
            require(android.util.Base64.decode(packet.getString("bytes"),android.util.Base64.DEFAULT).size<=512)
        }
        val dir=directory(id)
        if(File(dir,"result.json").isFile) { require(backup(id).contentEquals(bytes)) { "Abweichende HRV-Messung bereits vorhanden" };return false }
        require(!dir.exists());check(dir.mkdir())
        try { atomic(File(dir,"capture.jsonl"),capture);atomic(File(dir,"result.json"),row.toString().toByteArray()) }
        catch(e:Exception) { dir.deleteRecursively();throw e }
        return true
    }
}
