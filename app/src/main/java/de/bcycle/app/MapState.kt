package de.bcycle.app

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import java.io.File
import java.util.concurrent.Executors

/** Shared route and display track. No GPS client or additional ride recording. */
class MapState(private val context: Context, private val changed: () -> Unit) {
    val track = MapTrack()
    var route: GpxRoute? = null; private set
    var routeRevision = 0L; private set
    var busy = false; private set
    var message = ""; private set
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "B-Cycle-GPX") }
    private val stored = AtomicFile(File(context.filesDir, "map-route.gpx"))
    init {
        worker.execute {
            val loaded = try { stored.openRead().use { GpxImport.read(it) } } catch (_: Exception) { null }
            handler.post { route = loaded; routeRevision++; changed() }
        }
    }
    fun import(uri: Uri) {
        if (busy) return
        busy = true; message = "GPX wird geladen …"; changed()
        worker.execute {
            var result: GpxRoute? = null
            var text: String
            val temp = File(context.cacheDir, "gpx-import.tmp")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024); var bytes = 0L
                        while (true) { val n = input.read(buffer); if (n < 0) break; bytes += n
                            require(bytes <= GpxImport.MAX_BYTES) { "GPX ist größer als 20 MB" }; output.write(buffer, 0, n) }
                    }
                } ?: error("Datei kann nicht geöffnet werden")
                result = temp.inputStream().use { GpxImport.read(it) }
                val stream = stored.startWrite()
                try { temp.inputStream().use { it.copyTo(stream) }; stored.finishWrite(stream) }
                catch (e: Exception) { stored.failWrite(stream); throw e }
                text = "GPX geladen · ${result.originalPoints} Punkte"
            } catch (e: Exception) { result = null; text = "GPX nicht geladen: ${e.message ?: "ungültige Datei"}" }
            finally { temp.delete() }
            handler.post { if (result != null) { route = result; routeRevision++ }; busy = false; message = text; changed() }
        }
    }
    fun removeRoute() {
        if (busy) return
        busy = true
        worker.execute {
            stored.delete()
            handler.post { route = null; routeRevision++; busy = false; message = "GPX entfernt"; changed() }
        }
    }
}
