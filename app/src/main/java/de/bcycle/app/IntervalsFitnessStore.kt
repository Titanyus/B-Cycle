package de.bcycle.app

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate

/** Re-downloadable account cache, not a second training-load calculator or part of a backup. Worker only. */
internal class IntervalsFitnessStore(private val directory:File) {
    private data class Entry(val row:IntervalsFitnessDay,val fetched:Long)
    private fun file(athlete:String):AtomicFile {
        require(athlete.matches(Regex("i?[0-9]{1,15}")))
        val hash=MessageDigest.getInstance("SHA-256").digest(athlete.toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory,"$hash.json"))
    }
    private fun read(athlete:String):Map<LocalDate,Entry> = runCatching {
        val bytes=file(athlete).openRead().use { it.readNBytes(MAX_BYTES+1) };require(bytes.size<=MAX_BYTES)
        val json=JSONObject(String(bytes,Charsets.UTF_8));require(json.getInt("version")==1 && json.getString("athlete")==athlete)
        val rows=json.getJSONArray("days");require(rows.length()<=MAX_DAYS)
        buildMap {
            for(i in 0 until rows.length()) {
                val item=rows.getJSONObject(i);val row=IntervalsFitnessDay.decode(item);val fetched=item.getLong("fetched")
                require(fetched>=0 && !containsKey(row.day));put(row.day,Entry(row,fetched))
            }
        }
    }.getOrDefault(emptyMap())
    fun cached(athlete:String,oldest:LocalDate,newest:LocalDate):IntervalsFitnessHistory?=synchronized(lock) {
        require(newest>=oldest && java.time.temporal.ChronoUnit.DAYS.between(oldest,newest)<366)
        val entries=read(athlete);val rows=ArrayList<IntervalsFitnessDay>();var fetched=Long.MAX_VALUE;var day=oldest
        while(day<=newest) {
            val entry=entries[day] ?: return@synchronized null
            rows.add(entry.row);fetched=minOf(fetched,entry.fetched);day=day.plusDays(1)
        }
        IntervalsFitnessHistory(oldest,newest,rows,fetched)
    }
    /** Publish a whole successful range; failures/cancellation never turn a partial download into a complete cache. */
    fun save(athlete:String,history:IntervalsFitnessHistory)=synchronized(lock) {
        val entries=read(athlete).toMutableMap()
        history.days.forEach { entries[it.day]=Entry(it,history.fetchedAtMillis) }
        val requested=history.days.map { it.day }.toSet()
        val kept=entries.values.sortedWith(compareByDescending<Entry> { it.row.day in requested }.thenByDescending { it.fetched })
            .take(MAX_DAYS).sortedBy { it.row.day }
        val bytes=JSONObject().put("version",1).put("athlete",athlete).put("days",JSONArray().apply {
            kept.forEach { put(it.row.json().put("fetched",it.fetched)) }
        }).toString().toByteArray(Charsets.UTF_8)
        require(bytes.size<=MAX_BYTES);check(directory.isDirectory || directory.mkdirs())
        val target=file(athlete);val stream=target.startWrite()
        try { stream.write(bytes);target.finishWrite(stream) } catch(error:Exception) { target.failWrite(stream);throw error }
    }
    companion object {
        private val lock=Any()
        private const val MAX_DAYS=1098
        private const val MAX_BYTES=1024*1024
    }
}
