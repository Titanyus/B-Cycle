package de.bcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

/** Dedicated HRV snapshot. Index is the completion marker; original measurements remain untouched. */
internal class HrvDirectoryBackup(private val context:Context) {
    companion object {
        const val INDEX="bicycle-hrv.json"
        const val PREFIX="Bicycle-HRV-"
        private const val MAX_INDEX=4*1024*1024
    }
    private fun checkStop() { if(Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException() }
    private fun store()=HrvStore(File(context.filesDir,"hrv-measurements"))
    fun export(parent:BackupDirectory,progress:(Int)->Unit={}):RideBackupResult {
        val folder=PREFIX+Instant.now().toString().replace(':','-').replace('.','-')+"-"+UUID.randomUUID().toString().take(8)
        val directory=parent.child(folder,true)
        val temporary=java.nio.file.Files.createTempDirectory(context.cacheDir.toPath(),"hrv-backup-").toFile()
        val rows=JSONArray()
        try {
            val store=store();val history=store.list()
            HrvDailySelection.latest(history).forEach { store.questionnaire(it.id) }
            val measurements=HrvDailySelection.latest(history.filter { it.hasMeasurement && (it.acceptable || it.importedDay!=null) })
            val represented=measurements.mapTo(hashSetOf()) { HrvDailySelection.day(it).toString() }
            fun send(name:String,bytes:ByteArray) {
                checkStop();require(bytes.size in 1..HrvStore.MAX_BYTES)
                val file=File(temporary,name).apply { writeBytes(bytes) }
                directory.write(name,file)
                rows.put(JSONObject().put("name",name).put("bytes",file.length()).put("sha256",WebDavClient.hash(file)))
                file.delete();progress(rows.length())
            }
            measurements.forEach { send("hrv-${it.id}.json",store.backup(it.id)) }
            store.questionnaireDays().filterNot { it in represented }.forEach { send("day-$it.json",store.backupQuestionnaireDay(it)) }
            val index=JSONObject().put("format","bicycle-hrv-daily-backup").put("schema",1)
                .put("createdAt",Instant.now().toString()).put("entries",rows).toString().toByteArray()
            require(index.size<=MAX_INDEX && rows.length()<=20000)
            checkStop();val file=File(temporary,INDEX).apply { writeBytes(index) };directory.write(INDEX,file)
            return RideBackupResult(completed=rows.length(),folder=folder)
        } finally { temporary.deleteRecursively() }
    }
    fun restore(directory:BackupDirectory,progress:(Int)->Unit={}):RideBackupResult {
        val index=directory.open(INDEX).use { BackupJson.objectValue(it.readNBytes(MAX_INDEX+1),MAX_INDEX) }
        require(index.getString("format")=="bicycle-hrv-daily-backup" && index.getInt("schema")==1)
        val rows=index.getJSONArray("entries");require(rows.length()<=20000)
        val seen=hashSetOf<String>();var done=0;var duplicates=0;var failed=0
        val store=store()
        repeat(rows.length()) { i ->
            checkStop()
            try {
                val info=rows.getJSONObject(i);val name=backupName(info.getString("name"))
                require(seen.add(name))
                val length=info.getLong("bytes");require(length in 1..HrvStore.MAX_BYTES)
                val expected=info.getString("sha256");require(expected.matches(Regex("[a-f0-9]{64}")))
                val bytes=directory.open(name).use { it.readNBytes(length.toInt()+1) };require(bytes.size.toLong()==length)
                val hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                require(hash==expected) { "Prüfsumme stimmt nicht" }
                if(store.restore(bytes)) done++ else duplicates++
            } catch(e:Exception) {
                if(e is java.io.InterruptedIOException || Thread.currentThread().isInterrupted || ServiceProblems.of(e)?.let(ServiceProblems::global)==true) throw e
                failed++
            }
            progress(i+1)
        }
        return RideBackupResult(done,duplicates,failed)
    }
}
