package de.bcycle.app

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

data class StoredGpx(val id: String, val name: String, val points: Int, val segments: Int,
                     val details: RouteDetails = RouteDetails(), val origin: TrackOrigin = TrackOrigin.IMPORTED)

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
            val refreshMotion=json.optInt("motionVersion",0)<1
            val array = json.getJSONArray("entries")
            val missingAscent = mutableSetOf<String>()
            entries = List(array.length()) { i ->
                val item = array.getJSONObject(i)
                if (refreshMotion || item.optJSONObject("details")?.has("moving") != true) missingAscent += item.getString("id")
                StoredGpx(item.getString("id"), item.getString("name"), item.getInt("points"), item.getInt("segments"),
                    RouteDetails.from(item.optJSONObject("details")),
                    if (!item.has("origin")) TrackOrigin.IMPORTED else runCatching { TrackOrigin.valueOf(item.getString("origin")) }.getOrDefault(TrackOrigin.UNKNOWN)).also { file(it.id) }
            }
            val upgraded = entries.map { entry -> if (entry.id !in missingAscent || !file(entry.id).isFile) entry else {
                runCatching { val route = file(entry.id).inputStream().use(GpxImport::read)
                    entry.copy(details = entry.details.copy(ascentM = route.ascentM, movingMs = route.movingMs)) }.getOrDefault(entry)
            } }
            if (refreshMotion || upgraded != entries || missingAscent.isNotEmpty()) save(upgraded, json.optString("selected").takeIf { id -> entries.any { it.id == id } })
            selected = json.optString("selected").takeIf { id -> entries.any { it.id == id } }
        } else {
            val legacy = AtomicFile(File(root, "map-route.gpx"))
            if (legacy.baseFile.exists() || File(legacy.baseFile.path + ".bak").exists()) {
                legacy.openRead().use { add(it, "Bisherige GPX-Strecke") }
                legacy.delete()
            }
        }
        if (entries.any { it.details.dateMs == 0L }) {
            val upgraded = entries.map { entry -> if (entry.details.dateMs != 0L || !file(entry.id).isFile) entry else {
                val original = file(entry.id); val route = original.inputStream().use(GpxImport::read)
                entry.copy(details = RouteDetails(original.lastModified(), route.distanceM, route.durationMs, routePreview(route.points), route.ascentM, route.movingMs))
            } }
            save(upgraded, selected)
        }
        return selected?.takeIf { file(it).isFile }?.let { file(it).inputStream().use(GpxImport::read) }
    }
    fun add(input: InputStream, name: String, select: Boolean = true, preserveDate: Boolean = false,
        stableId:String?=null,bikeType:BikeType?=null,origin:TrackOrigin=TrackOrigin.IMPORTED,measurements:((GpxMeasurement)->Unit)?=null): GpxRoute {
        val id = stableId ?: UUID.randomUUID().toString()
        require(entries.none { it.id==id })
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
            var date: Long? = null
            val route = stored.openRead().use { stream ->
                if(measurements!=null) GpxImport.samples(stream) { sample ->
                    if(preserveDate && date==null && sample.time!=null) date=sample.time
                    measurements(sample)
                } else GpxImport.read(stream, if (preserveDate) { _, time, _, _ -> if (date == null && time != null) date = time } else null)
            }
            val title = name.substringAfterLast('/').substringAfterLast('\\').removeSuffix(".gpx").removeSuffix(".GPX").trim().take(120).ifBlank { "GPX-Strecke" }
            save(entries + StoredGpx(id, title, route.originalPoints, route.segments,
                RouteDetails(date ?: System.currentTimeMillis(), route.distanceM, route.durationMs, routePreview(route.points), route.ascentM, route.movingMs,bikeType),origin), if (select) id else selected)
            return route
        } catch (e: Exception) { stored.delete(); throw e }
    }
    fun select(id: String?): GpxRoute? {
        require(id == null || entries.any { it.id == id })
        val route = id?.let { file(it).inputStream().use(GpxImport::read) }
        save(entries, id)
        return route
    }
    internal fun restoreBackup(id:String,name:String,origin:TrackOrigin,details:RouteDetails,source:File):Boolean {
        file(id)
        source.inputStream().use(GpxImport::read)
        if(entries.any { it.id==id }) {
            require(WebDavClient.hash(file(id))==WebDavClient.hash(source)) { "Vorhandene Fahrt hat andere Daten" }
            return false
        }
        source.inputStream().use { add(it,name,select=false,preserveDate=true,stableId=id,bikeType=details.bikeType) }
        save(entries.map { if(it.id==id) it.copy(details=it.details.copy(dateMs=details.dateMs),origin=origin) else it },selected)
        return true
    }
    fun delete(id: String) {
        require(entries.any { it.id == id })
        save(entries.filter { it.id != id }, selected?.takeUnless { it == id })
        AtomicFile(file(id)).delete()
    }
    fun updateBikeTypes(type:(String)->BikeType?) {
        val next=entries.map { entry -> type(entry.id)?.let { entry.copy(details=entry.details.copy(bikeType=it)) } ?: entry }
        if(next!=entries) save(next,selected)
    }
    fun rename(id: String, name: String) {
        require(entries.any { it.id == id })
        val title = name.trim().take(120); require(title.isNotEmpty()) { "Bitte einen Namen eingeben" }
        save(entries.map { if (it.id == id) it.copy(name = title) else it }, selected)
    }
    private fun save(next: List<StoredGpx>, active: String?) {
        val array = JSONArray()
        next.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("points", it.points).put("segments", it.segments).put("details", it.details.json()).put("origin", it.origin.name)) }
        val bytes = JSONObject().put("motionVersion",1).put("entries", array).put("selected", active ?: "").toString().toByteArray(Charsets.UTF_8)
        val stream = index.startWrite()
        try { stream.write(bytes); index.finishWrite(stream) }
        catch (e: Exception) { index.failWrite(stream); throw e }
        entries = next; selected = active
    }
}
