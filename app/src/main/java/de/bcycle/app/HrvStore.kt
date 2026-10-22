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
        const val POLICY="bcycle-hrv-v4-frequency-boundary"
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
        val row=JSONObject().put("elapsedMs",packet.elapsedMs).put("selected",packet.selected).put("bytes",android.util.Base64.encodeToString(packet.bytes,android.util.Base64.NO_WRAP))
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
            .put("problem",session.problem ?: JSONObject.NULL).put("policy",POLICY)
            .put("algorithm",correction?.version ?: ArtifactCorrection.MEASUREMENT_VERSION)
            .put("acceptable",evaluation.acceptable).put("reasons",JSONArray(evaluation.reasons)).put("warnings",JSONArray(evaluation.warnings))
            .put("coverage",evaluation.coverage).put("affectedFraction",evaluation.affectedFraction)
            .put("measurementElapsedMs",diagnostics.measurementElapsedMs).put("rrDurationMs",diagnostics.rrDurationMs)
            .put("packetCount",session.packets.size).put("rrTicks",JSONArray(session.rrTicks))
            .put("captureDiagnostics",JSONObject().put("rrCount",diagnostics.rrCount)
                .put("selectedPacketCount",diagnostics.selectedPacketCount).put("sessionPacketCount",diagnostics.sessionPacketCount)
                .put("maxRrPerSelectedPacket",diagnostics.maxRrPerSelectedPacket)
                .put("firstSelectedAtMs",diagnostics.firstSelectedAtMs ?: JSONObject.NULL)
                .put("lastSelectedAtMs",diagnostics.lastSelectedAtMs ?: JSONObject.NULL))
            .put("raw",metrics(evaluation.raw)).put("corrected",metrics(evaluation.corrected))
            .put("frequency",evaluation.frequency?.let { f -> JSONObject()
                .put("method",FrequencyMetrics.VERSION).put("sampleHz",FrequencyDomain.SAMPLE_HZ)
                .put("lfLowerHz",.04).put("lfUpperHz",.15).put("hfLowerHz",.15).put("hfUpperHz",.4)
                .put("lfMs2",f.lfMs2).put("hfMs2",f.hfMs2).put("lfHf",f.lfHf ?: JSONObject.NULL)
                .put("durationSeconds",f.durationSeconds).put("segmentSamples",f.segmentSamples).put("segments",f.segments)
            } ?: JSONObject.NULL)
            .put("nnMs",JSONArray(correction?.nnMs ?: emptyList<Double>()))
            .put("affectedRawIndices",JSONArray(correction?.affectedRawIndices ?: emptyList<Int>()))
            .put("residualArtifacts",correction?.residual?.let { residual -> JSONObject()
                .put("count",residual.count).put("ectopic",JSONArray(residual.ectopic))
                .put("missed",JSONArray(residual.missed)).put("extra",JSONArray(residual.extra))
                .put("longshort",JSONArray(residual.longshort)) } ?: JSONObject.NULL)
            .put("correctionPasses",JSONArray().apply { correction?.passes?.forEach { pass ->
                put(JSONObject().put("beforePeaks",JSONArray(pass.beforePeaks)).put("afterPeaks",JSONArray(pass.afterPeaks))
                    .put("ectopic",JSONArray(pass.classes.ectopic)).put("missed",JSONArray(pass.classes.missed))
                    .put("extra",JSONArray(pass.classes.extra)).put("longshort",JSONArray(pass.classes.longshort)))
            } })
        atomic(File(dir,"result.json"),row.toString().toByteArray())
        return row
    }
    fun ids()=root.listFiles().orEmpty().filter { it.isDirectory && validId(it.name) && File(it,"result.json").isFile }.map { it.name }
    fun list():List<HrvHistorySummary> = root.listFiles().orEmpty().filter { it.isDirectory && validId(it.name) }
        .mapNotNull { dir -> runCatching {
            AtomicFile(File(dir,"result.json")).openRead().use { input ->
                require(input.channel.size()<=MAX_BYTES)
                android.util.JsonReader(input.reader(Charsets.UTF_8).buffered()).use { HrvHistorySummary.read(it,dir.name) }
            }
        }.getOrNull() }.sortedByDescending { it.utcMs }
    fun daily():List<HrvHistorySummary> = HrvDailySelection.latest(list())
    fun readDaily(id:String):JSONObject {
        val rows=list();val selected=rows.firstOrNull { it.id==id } ?: error("Missing HRV measurement")
        val day=HrvDailySelection.day(selected)
        return read(HrvDailySelection.latest(rows).first { HrvDailySelection.day(it)==day }.id)
    }
    fun read(id:String):JSONObject=AtomicFile(File(directory(id),"result.json")).openRead().use {
        BackupJson.objectValue(it.readNBytes(MAX_BYTES+1),MAX_BYTES).also { row ->
            require(row.getString("format")=="bicycle-hrv" && row.getInt("schema")==1 && row.getString("id")==id)
        }
    }
    fun questionnaire(id:String):HrvQuestionnaire? {
        val file=File(directory(id),"questionnaire.json")
        return if(!file.exists()) null else AtomicFile(file).openRead().use {
            HrvQuestionnaire.decode(BackupJson.objectValue(it.readNBytes(65537),65536))
        }
    }
    /** Upgrade imported missing answers from the archived original, never from a changed re-import. */
    fun questionnaireForEditing(id:String):HrvQuestionnaire? {
        val saved=questionnaire(id)
        val row=read(id);val external=row.optJSONObject("external")
        if(external?.optString("provider")!=Hrv4TrainingCsv.PROVIDER) return saved
        val original=external.optJSONObject("originalColumns") ?: return saved
        val source=original.keys().asSequence().associateWith { original.optString(it) }
        val ratings=Hrv4TrainingCsv.ratings(source)
        val base=saved ?: HrvQuestionnaire.forMeasurement(row)
        val sleepHours=source["sleep_time"]?.trim()?.replace(',','.')?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..24.0 }
        val extras=if(sleepHours!=null && "sleepHours" !in base.extras) base.extras+("sleepHours" to sleepHours.toString()) else base.extras
        val merged=base.copy(ratings=ratings+base.ratings,extras=extras)
        if(merged!=base) saveQuestionnaire(id,merged)
        return merged
    }
    fun saveQuestionnaire(id:String,value:HrvQuestionnaire) {
        val row=read(id)
        require(value.day==HrvQuestionnaire.forMeasurement(row).day)
        val bytes=value.json().toString().toByteArray();require(bytes.size<=65536)
        atomic(File(directory(id),"questionnaire.json"),bytes)
    }
    fun recover() {
        root.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith(".hrv4-import-") &&
            validId(it.name.removePrefix(".hrv4-import-")) }.forEach { it.deleteRecursively() }
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
            .put("capture",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP))
            .apply { questionnaire(id)?.let { put("questionnaire",it.json()) } }.toString().toByteArray().also { require(it.size<=MAX_BYTES) }
    }
    fun analysisJson(id:String,dailySelected:Boolean?=null):ByteArray {
        val result=read(id)
        val capture=File(directory(id),"capture.jsonl").takeIf { it.isFile }?.readBytes() ?: byteArrayOf()
        require(capture.size<=MAX_BYTES)
        val packets=JSONArray()
        capture.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val packet=JSONObject(line)
            val decoded=HeartRatePacket.decode(android.util.Base64.decode(packet.getString("bytes"),android.util.Base64.DEFAULT))
            packet.put("decoded",decoded?.let { d -> JSONObject().put("bpm",d.bpm)
                .put("contact",d.contact.name).put("rrTicks",JSONArray(d.rrTicks)) } ?: JSONObject.NULL)
            packets.put(packet)
        }
        return JSONObject().put("format","bicycle-hrv-analysis").put("schema",1)
            .put("rrTicksPerSecond",1024).put("result",result).put("packets",packets)
            .apply { dailySelected?.let { put("dailySelected",it) } }
            .apply { questionnaire(id)?.let { put("questionnaire",it.json()) } }.toString(2).toByteArray()
    }
    fun importHrv4Training(entry:HrvCsvEntry):Boolean {
        val row=entry.result;val id=row.getString("id")
        require(row.getJSONObject("external").getString("provider")==Hrv4TrainingCsv.PROVIDER)
        // Stable source/day/time identity: re-imports never overwrite measurements or edited answers.
        val target=directory(id)
        if(File(target,"result.json").exists()) return false
        require(!target.exists() && entry.questionnaire.day==HrvQuestionnaire.forMeasurement(row).day)
        val bytes=row.toString().toByteArray();require(bytes.size<=MAX_BYTES)
        val staging=File(root,".hrv4-import-${UUID.randomUUID()}");check(staging.mkdir())
        try {
            atomic(File(staging,"capture.jsonl"),byteArrayOf())
            atomic(File(staging,"questionnaire.json"),entry.questionnaire.json().toString().toByteArray())
            atomic(File(staging,"result.json"),bytes)
            check(staging.renameTo(target))
        } finally { staging.deleteRecursively() }
        return true
    }
    fun restore(bytes:ByteArray):Boolean {
        val backup=BackupJson.objectValue(bytes,MAX_BYTES)
        require(backup.getString("format")=="bicycle-hrv-backup" && backup.getInt("schema")==1)
        val row=backup.getJSONObject("result");val id=row.getString("id")
        require(validId(id) && row.getString("format")=="bicycle-hrv" && row.getInt("schema")==1)
        require(row.getLong("utcMs")>=0 && Phase.valueOf(row.getString("phase")) in setOf(Phase.COMPLETE,Phase.ABORTED))
        require(!backup.has("questionnaire") || backup.get("questionnaire") is JSONObject)
        val questionnaire=backup.optJSONObject("questionnaire")?.let(HrvQuestionnaire::decode)
        require(questionnaire==null || questionnaire.day==HrvQuestionnaire.forMeasurement(row).day)
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
        try {
            atomic(File(dir,"capture.jsonl"),capture)
            questionnaire?.let { atomic(File(dir,"questionnaire.json"),it.json().toString().toByteArray()) }
            atomic(File(dir,"result.json"),row.toString().toByteArray())
        }
        catch(e:Exception) { dir.deleteRecursively();throw e }
        return true
    }
}
