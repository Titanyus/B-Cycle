package de.bcycle.app

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

data class StoredGpx(val id: String, val name: String, val points: Int, val segments: Int,
                     val details: RouteDetails = RouteDetails())

/** Worker-thread storage. Originals stay on disk; only the selected route is decoded. */
internal class GpxLibrary(private val root: File) {
    private val directory = File(root, "gpx-library").apply { mkdirs() }
    private val index = AtomicFile(File(directory, "index.json"))
    var entries: List<StoredGpx> = emptyList(); private set
    var selected: String? = null; private set
    internal fun file(id: String): File {
        require(id.matches(Regex("[a-f0-9-]{36}"))) { "Ungültige Strecken-ID" }
        return File(directory, "$id.gpx")
    }
    fun load(): GpxRoute? {
        if (index.baseFile.exists() || File(index.baseFile.path + ".bak").exists()) {
            val json = index.openRead().bufferedReader().use { JSONObject(it.readText()) }
            val array = json.getJSONArray("entries")
            entries = List(array.length()) { i ->
                val item = array.getJSONObject(i)
                StoredGpx(item.getString("id"), item.getString("name"), item.getInt("points"), item.getInt("segments"),
                    RouteDetails.from(item.optJSONObject("details"))).also { file(it.id) }
            }
            selected = json.optString("selected").takeIf { id -> entries.any { it.id == id } }
        } else {
            val legacy = AtomicFile(File(root, "map-route.gpx"))
            if (legacy.baseFile.exists() || File(legacy.baseFile.path + ".bak").exists()) {
                legacy.openRead().use { add(it, "Bisherige GPX-Strecke") }
                legacy.delete()
            }
        }
        if (entries.any { it.details.dateMs == 0L }) {
            val upgraded = entries.map { entry -> if (entry.details.dateMs != 0L) entry else {
                val original = file(entry.id); val route = original.inputStream().use(GpxImport::read)
                entry.copy(details = RouteDetails(original.lastModified(), route.distanceM, route.durationMs, routePreview(route.points)))
            } }
            save(upgraded, selected)
        }
        return selected?.let { file(it).inputStream().use(GpxImport::read) }
    }
    fun add(input: InputStream, name: String): GpxRoute {
        val id = UUID.randomUUID().toString()
        val stored = AtomicFile(file(id))
        try {
            val stream = stored.startWrite()
            try {
                val buffer = ByteArray(16 * 1024); var bytes = 0L
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    bytes += n; require(bytes <= GpxImport.MAX_BYTES) { "GPX ist größer als 20 MB" }
                    stream.write(buffer, 0, n)
                }
                stored.finishWrite(stream)
            } catch (e: Exception) { stored.failWrite(stream); throw e }
            val route = stored.openRead().use(GpxImport::read)
            val title = name.substringAfterLast('/').substringAfterLast('\\').removeSuffix(".gpx").removeSuffix(".GPX").trim().take(120).ifBlank { "GPX-Strecke" }
            save(entries + StoredGpx(id, title, route.originalPoints, route.segments,
                RouteDetails(System.currentTimeMillis(), route.distanceM, route.durationMs, routePreview(route.points))), id)
            return route
        } catch (e: Exception) { stored.delete(); throw e }
    }
    fun select(id: String?): GpxRoute? {
        require(id == null || entries.any { it.id == id })
        val route = id?.let { file(it).inputStream().use(GpxImport::read) }
        save(entries, id)
        return route
    }
    fun delete(id: String) {
        require(entries.any { it.id == id })
        save(entries.filter { it.id != id }, selected?.takeUnless { it == id })
        AtomicFile(file(id)).delete()
    }
    fun rename(id: String, name: String) {
        require(entries.any { it.id == id })
        val title = name.trim().take(120); require(title.isNotEmpty()) { "Bitte einen Namen eingeben" }
        save(entries.map { if (it.id == id) it.copy(name = title) else it }, selected)
    }
    private fun save(next: List<StoredGpx>, active: String?) {
        val array = JSONArray()
        next.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("points", it.points).put("segments", it.segments).put("details", it.details.json())) }
        val bytes = JSONObject().put("entries", array).put("selected", active ?: "").toString().toByteArray(Charsets.UTF_8)
        val stream = index.startWrite()
        try { stream.write(bytes); index.finishWrite(stream) }
        catch (e: Exception) { index.failWrite(stream); throw e }
        entries = next; selected = active
    }
}
