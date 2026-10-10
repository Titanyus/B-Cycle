package de.bcycle.app

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import java.util.concurrent.Executors

/** Shared route library and display track. No GPS client or additional ride recording. */
class MapState(private val context: Context, private val changed: () -> Unit) {
    val track = MapTrack()
    var route: GpxRoute? = null; private set
    var routes: List<StoredGpx> = emptyList(); private set
    var selectedRoute: String? = null; private set
    var routeRevision = 0L; private set
    var busy = true; private set
    var message = ""; private set
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "B-Cycle-GPX") }
    private val library = GpxLibrary(context.filesDir)
    init { worker.execute { finishOperation("GPX-Bibliothek geladen") { library.load() } } }

    private fun finishOperation(success: String, action: () -> GpxRoute?) {
        var next = route; var text = success
        try { next = action() }
        catch (e: Exception) { text = "GPX nicht geändert: ${e.message ?: "ungültige Datei"}" }
        val entries = library.entries; val selected = library.selected
        handler.post {
            route = next; routes = entries; selectedRoute = selected; routeRevision++
            busy = false; message = if (success == "GPX-Bibliothek geladen" && text == success) "" else text
            changed()
        }
    }
    private fun operate(success: String, action: () -> GpxRoute?) {
        if (busy) return
        busy = true; changed()
        worker.execute { finishOperation(success, action) }
    }
    fun import(uri: Uri) = operate("GPX gespeichert und angezeigt") {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "GPX-Strecke"
        context.contentResolver.openInputStream(uri)?.use { library.add(it, name) } ?: error("Datei kann nicht geöffnet werden")
    }
    fun selectRoute(id: String?) = operate(if (id == null) "GPX ausgeblendet" else "GPX angezeigt") { library.select(id) }
    fun deleteRoute(id: String) = operate("GPX gelöscht") {
        val active = library.selected == id
        library.delete(id)
        if (active) null else route
    }
}
