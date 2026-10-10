package de.bcycle.app

import android.content.ContentValues
import android.content.Context
import android.content.ContentUris
import android.os.Environment
import android.provider.MediaStore
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class StoredFit(val id: String, val name: String, val recorded: Boolean, val details: RouteDetails, val hasRoute: Boolean)

/** Private immutable FIT originals plus a small atomic index. Downloads stay independent. */
internal class FitLibrary(root: File) {
    private val directory = File(root, "fit-library").apply { mkdirs() }
    private val index = AtomicFile(File(directory, "index.json"))
    var entries: List<StoredFit> = emptyList(); private set
    private val excluded = mutableSetOf<String>()
    private val seenDownloads = mutableSetOf<String>()
    internal fun file(id: String): File {
        require(id.matches(Regex("(recorded|imported)-[a-f0-9]{64}")))
        return File(directory, "$id.fit")
    }
    fun load() {
        if (index.baseFile.exists() || File(index.baseFile.path + ".bak").exists()) {
            val json = index.openRead().bufferedReader().use { JSONObject(it.readText()) }
            val array = json.getJSONArray("entries")
            entries = List(array.length()) { i -> val item = array.getJSONObject(i)
                StoredFit(item.getString("id"), item.getString("name"), item.getBoolean("recorded"),
                    RouteDetails.from(item.getJSONObject("details")), item.getBoolean("hasRoute")).also { file(it.id) }
            }
            json.optJSONArray("excluded")?.let { a -> repeat(a.length()) { excluded += a.getString(it) } }
            json.optJSONArray("seenDownloads")?.let { a -> repeat(a.length()) { seenDownloads += a.getString(it) } }
        }
        refresh()
    }
    fun refresh() {
        var next = entries
        for (original in directory.listFiles { f -> f.extension == "fit" }.orEmpty()) {
            val id = original.nameWithoutExtension
            if (next.any { it.id == id }) continue
            if (id.substringAfter('-') in excluded) continue
            file(id)
            val read = FitRead.read(original)
            val recorded = id.startsWith("recorded-")
            val title = if (recorded) "Fahrt · " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(read.details.dateMs)) else "FIT-Strecke"
            next = next + StoredFit(id, title, recorded, read.details, read.route != null)
        }
        if (next != entries) save(next)
    }
    fun add(input: InputStream, name: String, recorded: Boolean, automatic: Boolean = false): StoredFit? {
        val temporary = File(directory, "${UUID.randomUUID()}.tmp")
        try {
            temporary.outputStream().use { output -> copyBounded(input, output) }
            val hash = hash(temporary)
            if (automatic && hash in excluded) return null
            val existing = entries.firstOrNull { it.id.endsWith(hash) }
            if (existing != null) {
                if (recorded && !existing.recorded) {
                    val upgraded = existing.copy(recorded = true, details = FitRead.read(file(existing.id)).details)
                    save(entries.map { if (it.id == existing.id) upgraded else it }); return upgraded
                }
                return existing
            }
            val read = FitRead.read(temporary)
            val id = (if (recorded) "recorded-" else "imported-") + hash
            val target = AtomicFile(file(id)); val stream = target.startWrite()
            try { temporary.inputStream().use { it.copyTo(stream) }; target.finishWrite(stream) }
            catch (e: Exception) { target.failWrite(stream); throw e }
            val title = name.trim().removeSuffix(".fit").removeSuffix(".FIT").take(120).ifBlank { "FIT-Strecke" }
            val details = if (recorded) read.details else read.details.copy(dateMs = System.currentTimeMillis())
            val entry = StoredFit(id, title, recorded, details, read.route != null)
            save(entries + entry)
            return entry
        } finally { temporary.delete() }
    }
    fun discoverDownloads(context: Context): Int {
        var inaccessible = 0
        val resolver = context.contentResolver
        try {
            resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
                "${MediaStore.Downloads.RELATIVE_PATH} IN (?, ?) AND ${MediaStore.Downloads.IS_PENDING} = 0",
                arrayOf(Environment.DIRECTORY_DOWNLOADS + "/B-Cycle/", Environment.DIRECTORY_DOWNLOADS + "/B-Cycle"), null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1) ?: continue
                    if (!name.startsWith("B-Cycle-") || !name.endsWith(".fit", true)) continue
                    val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0))
                    if (uri.toString() in seenDownloads) continue
                    try {
                        resolver.openInputStream(uri)?.use { add(it, name, true, automatic = true) } ?: error("Datei nicht lesbar")
                        seenDownloads += uri.toString(); save(entries)
                    } catch (_: Exception) { inaccessible++ }
                }
            }
        } catch (_: Exception) { inaccessible++ }
        return inaccessible
    }
    fun rename(id: String, name: String) {
        require(entries.any { it.id == id }); val title = name.trim().take(120)
        require(title.isNotEmpty()) { "Bitte einen Namen eingeben" }
        save(entries.map { if (it.id == id) it.copy(name = title) else it })
    }
    fun delete(id: String) {
        require(entries.any { it.id == id })
        // Persist the exclusion before deleting, so a Downloads scan cannot resurrect it.
        excluded += id.substringAfter('-')
        save(entries.filter { it.id != id }); AtomicFile(file(id)).delete()
    }
    private fun save(next: List<StoredFit>) {
        val array = JSONArray()
        next.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("recorded", it.recorded)
            .put("details", it.details.json()).put("hasRoute", it.hasRoute)) }
        val bytes = JSONObject().put("entries", array).put("excluded", JSONArray(excluded.toList()))
            .put("seenDownloads", JSONArray(seenDownloads.toList())).toString().toByteArray()
        val stream = index.startWrite()
        try { stream.write(bytes); index.finishWrite(stream) } catch (e: Exception) { index.failWrite(stream); throw e }
        entries = next
    }
    companion object {
        private fun hash(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input -> val buffer = ByteArray(16 * 1024); while (true) {
                val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count)
            } }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        private fun copyBounded(input: InputStream, output: java.io.OutputStream) {
            val buffer = ByteArray(16 * 1024); var bytes = 0L
            while (true) { val count = input.read(buffer); if (count < 0) break
                bytes += count; require(bytes <= FitRead.MAX_BYTES) { "FIT ist größer als 100 MB" }; output.write(buffer, 0, count)
            }
        }
        /** Recording worker writes only immutable originals; MapState alone owns the index. */
        fun retainRecording(root: File, encoded: File): File {
            val directory = File(root, "fit-library").apply { mkdirs() }
            val destination = AtomicFile(File(directory, "recorded-${hash(encoded)}.fit"))
            if (destination.baseFile.exists()) {
                check(hash(destination.baseFile) == hash(encoded)) { "Vorhandene FIT-Kopie ist beschädigt · Original bleibt erhalten" }
                return destination.baseFile
            }
            val output = destination.startWrite()
            try { encoded.inputStream().use { it.copyTo(output) }; destination.finishWrite(output) }
            catch (e: Exception) { destination.failWrite(output); throw e }
            return destination.baseFile
        }
    }
}
