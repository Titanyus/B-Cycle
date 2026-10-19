package de.bcycle.app

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import java.util.concurrent.Executors

/** Shared GPX/FIT manager and map selection. All original-file I/O stays on one worker. */
class MapState(private val context: Context, private val changed: () -> Unit) {
    val track = MapTrack()
    var route: GpxRoute? = null; private set
    var routes: List<StoredGpx> = emptyList(); private set
    var activities: List<StoredFit> = emptyList(); private set
    var selectedRoute: String? = null; private set
    var routeRevision = 0L; private set
    var busy = true; private set
    var message = ""; private set
    var archiveStatus = ""; private set
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "B-Cycle-routes") }
    private val library = GpxLibrary(context.filesDir)
    private val fits = FitLibrary(context.filesDir)
    private val preferences = context.getSharedPreferences("bike", Context.MODE_PRIVATE)
    private var selectedFit: String? = null
    private var workerRoute: GpxRoute? = null
    private var retainedFiles: Map<String, RouteFileStamp> = emptyMap()
    private var correctedIds=emptySet<String>()
    val entries: List<ManagedRoute> get() = (routes.map { ManagedRoute(it.id, it.name, false, "GPX", it.details, true, it.origin, it.id in retainedFiles, retainedFiles[it.id]) } +
        activities.map { ManagedRoute(it.id, it.name, it.recorded, "FIT", it.details, it.hasRoute, it.origin, it.id in retainedFiles, retainedFiles[it.id],it.id in correctedIds) })
        .sortedByDescending { it.details.dateMs }
    init {
        worker.execute { finishOperation("") {
            workerRoute = library.load(); fits.load(); fits.discoverDownloads(context)
            if(preferences.contains("displayGpxId")) {
                val id=preferences.getString("displayGpxId","")?.takeIf { wanted -> library.entries.any { it.id==wanted } }
                workerRoute=library.select(id)
            } else preferences.edit().putString("displayGpxId",library.selected ?: "").apply()
            selectedFit = preferences.getString("displayFitId", null)?.takeIf { id -> fits.entries.any { it.id == id && it.hasRoute } }
            if (selectedFit != null) workerRoute = FitRead.read(fits.file(selectedFit!!)).route
            defaultRouteIfUnselected()
        } }
    }
    /** Worker only: use a GPS-capable recorded ride, never an arbitrary imported track. */
    private fun defaultRouteIfUnselected(): GpxRoute? {
        if (selectedFit != null || library.selected != null) return workerRoute
        val latest = fits.entries.filter { it.origin == TrackOrigin.RECORDED && it.hasRoute && fits.file(it.id).canRead() }
            .maxByOrNull { it.details.dateMs } ?: return workerRoute
        val displayed = FitRead.read(fits.file(latest.id)).route ?: return workerRoute
        setSelectedFit(latest.id)
        return displayed
    }
    /** Details share the existing worker; decoding never mutates the displayed route or library. */
    internal fun loadAnalysis(id: String, complete: (Result<RouteAnalysis>) -> Unit): () -> Unit {
        val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
        worker.execute {
            val result = runCatching {
                val fit = fits.entries.singleOrNull { it.id == id }
                val gpx = library.entries.singleOrNull { it.id == id }
                require(fit != null || gpx != null)
                val entry = if (fit != null) ManagedRoute(fit.id, fit.name, fit.recorded, "FIT", fit.details, fit.hasRoute, fit.origin,elevationCorrected=fits.corrected(id))
                    else gpx!!.let { ManagedRoute(it.id, it.name, false, "GPX", it.details, true, it.origin) }
                val original = if (fit != null) fits.file(id) else library.file(id)
                val stamp = RouteFileStamp(original.length(), original.lastModified(), WebDavClient.hash(original))
                val analysis = RouteAnalysisRead.read(original, entry.copy(locallySaved = original.isFile, fileStamp = stamp), profile = RiderProfileStore(preferences).read(), cancelled = cancelled::get)
                check(original.length() == stamp.length && original.lastModified() == stamp.modified)
                analysis
            }
            if (!cancelled.get()) handler.post { if (!cancelled.get()) complete(result) }
        }
        return { cancelled.set(true) }
    }
    internal fun correctElevation(id:String,complete:(Result<Unit>)->Unit):()->Unit {
        if(busy) { complete(Result.failure(IllegalStateException()));return {} }
        val cancelled=java.util.concurrent.atomic.AtomicBoolean(false)
        busy=true;changed()
        worker.execute {
            var result:Result<Unit> = Result.failure(IllegalStateException())
            finishOperation("") {
                result=runCatching {
                    val entry=fits.entries.single { it.id==id }
                    require(entry.origin==TrackOrigin.RECORDED && entry.hasRoute)
                    val target=FitLibrary.correctedFile(context.filesDir,id)
                    val backup=if(target.isFile) java.io.File.createTempFile("height-previous-",".fit",context.cacheDir).also { target.copyTo(it,true) } else null
                    try {
                        FitElevationCorrection.correct(context,fits.originalFile(id),target,cancelled::get)
                        fits.refreshDetails(id)
                    } catch(e:Exception) {
                        if(backup!=null) java.nio.file.Files.move(backup.toPath(),target.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                        else target.delete()
                        throw java.io.IOException(context.getString(R.string.elevation_failed),e)
                    } finally { backup?.delete() }

                }
                result.getOrThrow()
                if(selectedFit==id) FitRead.read(fits.file(id)).route else workerRoute
            }
            if(!cancelled.get()) handler.post { if(!cancelled.get()) complete(result) }
        }
        return { cancelled.set(true) }
    }
    fun setRouteVisible(visible: Boolean) {
        if (preferences.getBoolean("mapGpxVisible", true) == visible) return
        preferences.edit().putBoolean("mapGpxVisible", visible).apply()
        routeRevision++; changed()
    }
    private fun finishOperation(success: String, action: () -> GpxRoute?) {
        var text = success
        try { workerRoute = action() }
        catch (e: Exception) { text = "Strecke nicht geändert: ${e.message ?: "Datei nicht verfügbar"}" }
        val gpxEntries = library.entries; val fitEntries = fits.entries; val selected = selectedFit ?: library.selected
        val files = (gpxEntries.map { it.id to library.file(it.id) } + fitEntries.map { it.id to fits.file(it.id) })
            .mapNotNull { (id, file) -> if (file.isFile && file.canRead()) id to RouteFileStamp(file.length(), file.lastModified()) else null }.toMap()
        val displayed = workerRoute
        val corrected=fitEntries.filter { fits.corrected(it.id) }.map { it.id }.toSet()
        handler.post {
            correctedIds=corrected
            retainedFiles = files; route = displayed; routes = gpxEntries; activities = fitEntries; selectedRoute = selected; routeRevision++
            busy = false; message = text
            if (archiveStatus.isNotEmpty() && text.isNotEmpty()) archiveStatus = text
            changed()
        }
    }
    private fun operate(success: String, action: () -> GpxRoute?) {
        if (busy) return
        archiveStatus = ""
        busy = true; changed()
        worker.execute { finishOperation(success, action) }
    }
    fun refreshLibrary() {
        // A completed recording must be registered even while another operation is queued.
        busy = true; changed()
        worker.execute {
            var inaccessible = 0
            finishOperation("") {
                fits.refresh(); inaccessible = fits.discoverDownloads(context); defaultRouteIfUnselected()
            }
            if (inaccessible > 0) handler.post {
                message = "Einige alte FIT-Dateien nicht lesbar · bitte über FIT-Fahrt übernehmen auswählen"; changed()
            }
        }
    }
    fun import(uri: Uri, recorded: Boolean = false) = operate("Strecke gespeichert und angezeigt") {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: if (recorded) "FIT-Fahrt.fit" else "GPX-Strecke"
        context.contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            input.mark(16); val header = ByteArray(12); val bytes = input.read(header); input.reset()
            val isFit = bytes == 12 && header.copyOfRange(8, 12).contentEquals(".FIT".toByteArray())
            if (recorded || isFit || name.endsWith(".fit", true)) {
                val entry = fits.add(input, name, recorded) ?: error("FIT konnte nicht übernommen werden")
                val displayed = FitRead.read(fits.file(entry.id)).route
                if (displayed != null) { library.select(null); setSelectedFit(entry.id) }
                displayed ?: workerRoute
            } else {
                val displayed = library.add(input, name); setSelectedFit(null); displayed
            }
        } ?: error("Datei kann nicht geöffnet werden")
    }
    internal fun existingDreeve(target:String,items:List<DreeveActivity>):Set<String> {
        val local=entries.filter { it.locallySaved }.map { it.id }.toSet()
        val confirmed=RideServiceLinks.forAccount(context,RideService.DREEVE,target)
            .filter { it.rideId in local }.map { it.activityId }.toSet()
        return items.filter { it.id in confirmed || DreeveImport.localId(target,it.id) in local }.map { it.id }.toSet()
    }
    internal fun importDreeve(target:String,items:List<DreeveActivity>,client:DreeveClient,cancelled:()->Boolean,
        progress:(DreeveItemResult)->Unit,complete:(Result<DreeveImportResult>)->Unit) {
        if(busy) { complete(Result.failure(IllegalStateException()));return }
        busy=true;changed()
        worker.execute {
            var result:Result<DreeveImportResult> = Result.failure(IllegalStateException())
            finishOperation("") {
                result=runCatching { DreeveImport(context,library,{ library.entries.map { it.id }.toSet()+fits.entries.map { it.id } },client::downloadGpx)
                    .read(target,items,cancelled) { item -> handler.post { progress(item) } } }
                workerRoute
            }
            handler.post { complete(result) }
        }
    }
    fun importArchive(uri: Uri) {
        if (busy) return
        busy = true; archiveStatus = context.getString(R.string.archive_import_progress, 0); changed()
        worker.execute {
            var result: ArchiveImportResult? = null
            var lastProgress = 0L
            finishOperation("") {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    result = RouteArchiveImport(context.filesDir, context.cacheDir, fits, library).read(stream) { count ->
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastProgress >= 500) {
                            lastProgress = now
                            handler.post { archiveStatus = context.getString(R.string.archive_import_progress, count); changed() }
                        }
                    }
                } ?: throw java.io.IOException(context.getString(R.string.archive_import_open_failed))
                workerRoute
            }
            result?.let { summary -> handler.post {
                message = context.getString(R.string.archive_import_result, summary.imported, summary.duplicates, summary.failed, summary.unsupported) +
                    if (summary.interrupted) context.getString(R.string.archive_import_interrupted) else ""
                archiveStatus = message
                changed()
            } }
        }
    }
    private fun setSelectedFit(id: String?) { selectedFit = id; preferences.edit().apply { if (id == null) remove("displayFitId") else putString("displayFitId", id);putString("displayGpxId",if(id==null) library.selected ?: "" else "") }.apply() }
    /** Queued on the existing library worker, so switching during a library operation is safe. */
    internal fun restoreProfileRoute() {
        val fit=preferences.getString("displayFitId",null)
        val gpx=preferences.getString("displayGpxId",null)
        busy=true
        worker.execute { finishOperation("") {
            selectedFit=fit?.takeIf { id -> fits.entries.any { it.id==id && it.hasRoute } }
            val selected=gpx?.takeIf { id -> library.entries.any { it.id==id } }
            val displayed=library.select(if(selectedFit==null) selected else null)
            selectedFit?.let { runCatching { FitRead.read(fits.file(it)).route }.getOrNull() } ?: displayed
        } }
    }
    fun selectRoute(id: String?) = operate(if (id == null) "Strecke ausgeblendet" else "Strecke angezeigt") {
        if (id != null && fits.entries.any { it.id == id }) {
            val displayed = FitRead.read(fits.file(id)).route ?: error("Diese FIT-Datei enthält keine darstellbaren GPS-Punkte")
            library.select(null); preferences.edit().putString("displayGpxId","").apply();setSelectedFit(id); displayed
        } else { val displayed = library.select(id);preferences.edit().putString("displayGpxId",id ?: "").apply(); setSelectedFit(null); displayed }
    }
    fun renameRoute(id: String, name: String) = operate("") {
        if (fits.entries.any { it.id == id }) fits.rename(id, name) else library.rename(id, name)
        workerRoute
    }
    fun exportRoute(id: String) = operate("Nach Download/B-Cycle exportiert") {
        val fit = fits.entries.firstOrNull { it.id == id }
        val file = if (fit != null) fits.file(id) else library.file(id)
        val name = fit?.name ?: library.entries.single { it.id == id }.name
        RouteLocalSaves.perform(context, id, file, "Downloads/B-Cycle") {
            DownloadFiles.write(context, file, "$name.${file.extension}")
        }
        workerRoute
    }
    internal fun uploadRoute(id: String,target:RouteTransferTarget?=null) = operate(context.getString(R.string.cloud_uploads_queued)) {
        val entry = fits.entries.singleOrNull { it.id == id }
        require(entry?.origin == TrackOrigin.RECORDED) { "Cloud-Upload nur für selbst gefahrene FIT-Fahrten" }
        if(target==null) RouteCloudUploads.retry(context,id) else {
            require(target!=RouteTransferTarget.LOCAL && target.enabled(context))
            target.retry(context,id)
        }; workerRoute
    }
    fun saveRouteLocally(id: String) = operate("") {
        val file = if (fits.entries.any { it.id == id }) fits.file(id) else library.file(id)
        require(file.isFile && file.canRead()) { "Lokale Originaldatei fehlt · bitte erneut importieren" }
        workerRoute
    }
    fun deleteRoute(id: String) = operate("Aus der Sammlung gelöscht · vorhandene Downloads bleiben erhalten") {
        val active = (selectedFit ?: library.selected) == id
        if (fits.entries.any { it.id == id }) { fits.delete(id); if (selectedFit == id) setSelectedFit(null) } else library.delete(id)
        RouteLocalSaves.forget(context, id)
        IntervalsUploads.forget(context,id)
        ActivityServiceUploads.all.forEach { it.forget(context,id) }
        RideServiceLinks.forget(context,id)
        RideWeather.forget(context,id)
        if (active) null else workerRoute
    }
}

data class ManagedRoute(val id: String, val name: String, val recorded: Boolean, val format: String,
                        val details: RouteDetails, val hasRoute: Boolean,
                        val origin: TrackOrigin = if (recorded) TrackOrigin.RECORDED else TrackOrigin.IMPORTED,
                        val locallySaved: Boolean = false, val fileStamp: RouteFileStamp? = null, val elevationCorrected:Boolean = false)
