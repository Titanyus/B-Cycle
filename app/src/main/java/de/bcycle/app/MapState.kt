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
    internal var mapSlots=MapRouteSlots();private set
    internal var mapRoutes:List<DisplayedMapRoute> = emptyList();private set
    private data class CachedMapRoute(val path:String,val length:Long,val modified:Long,val route:GpxRoute)
    private val mapRouteCache=mutableMapOf<String,CachedMapRoute>()
    internal fun routeNumber(id:String)=mapSlots.slot(id)?.plus(1)
    internal fun routeVisible(id:String)=preferences.getBoolean("mapGpxVisible",true) && mapSlots.slot(id)?.let { mapSlots.visible[it] }==true && mapRoutes.any { it.id==id }
    internal fun visibleRouteIds()=mapRoutes.filter { routeVisible(it.id) }.map { it.id }.toSet()
    var routeRevision = 0L; private set
    var busy = true; private set
    var message = ""; private set
    var archiveStatus = ""; private set
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "B-Cycle-routes") }
    private val statisticsCache=RideStatisticsCache()
    private var statisticsEntries:List<ManagedRoute>?=null
    private var statisticsProfiles:RideProfileCatalog?=null
    private var statisticsYear=0
    private var statisticsZone:java.time.ZoneId?=null
    private var statistics:RideStatistics?=null
    private var statisticsGeneration=0
    private var statisticsCallback:((RideStatistics)->Unit)?=null
    internal fun loadStatistics(complete:(RideStatistics)->Unit) {
        val source=entries;val profiles=RideProfileColor.catalog(context)
        val zone=java.time.ZoneId.systemDefault();val year=java.time.LocalDate.now(zone).year
        statisticsCallback=complete
        if(statisticsEntries===source && statisticsProfiles===profiles && statisticsYear==year && statisticsZone==zone) {
            statistics?.let(complete);return
        }
        statisticsEntries=source;statisticsProfiles=profiles;statisticsYear=year;statisticsZone=zone;statistics=null
        val generation=++statisticsGeneration
        worker.execute {
            val value=statisticsCache.get(source,profiles,year,zone)
            handler.post { if(statisticsGeneration==generation) { statistics=value;statisticsCallback?.invoke(value) } }
        }
    }
    internal fun cancelStatistics() { statisticsCallback=null }
    private val library = GpxLibrary(context.filesDir)
    private val fits = FitLibrary(context.filesDir)
    internal fun transferBackup(directory:BackupDirectory,restore:Boolean,hrv:Boolean=false,progress:(Int)->Unit,complete:(Result<RideBackupResult>)->Unit):Boolean {
        if(busy) return false
        busy=true;changed()
        worker.execute {
            var result:Result<RideBackupResult> = Result.failure(IllegalStateException())
            finishOperation("") {
                result=runCatching {
                    val report:(Int)->Unit={ count -> handler.post { progress(count) } }
                    if(hrv) {
                        val backup=HrvDirectoryBackup(context)
                        if(restore) backup.restore(directory,report) else backup.export(directory,report)
                    } else {
                        val backup=RideDirectoryBackup(context,fits,library,dreeveSummaries)
                        if(restore) backup.restore(directory,report) else backup.export(directory,report)
                    }
                }
                workerRoute
            }
            handler.post { complete(result) }
        }
        return true
    }
    private val dreeveSummaries=DreeveSummaryLibrary(context.filesDir)
    private var summaries:List<StoredDreeveSummary> = emptyList()
    private val preferences = context.getSharedPreferences("bike", Context.MODE_PRIVATE)
    private var selectedFit: String? = null
    private var workerRoute: GpxRoute? = null
    @Volatile private var mapProfileGeneration=0
    private var retainedFiles: Map<String, RouteFileStamp> = emptyMap()
    private var correctedIds=emptySet<String>()
    private var entriesRevision=-1L
    private var entriesProfiles:RideProfileCatalog?=null
    private var entriesNames:RideNames.Snapshot?=null
    private var cachedEntries:List<ManagedRoute> = emptyList()
    private var entriesById:Map<String,ManagedRoute> = emptyMap()
    val entries: List<ManagedRoute> get() {
        val profiles=RideProfileColor.catalog(context);val names=RideNames.snapshot(context)
        if(entriesRevision!=routeRevision || entriesProfiles!==profiles || entriesNames!==names) {
            cachedEntries=(routes.map { ManagedRoute(it.id,it.name,false,"GPX",it.details,true,it.origin,it.id in retainedFiles,retainedFiles[it.id]) } +
                activities.map { ManagedRoute(it.id,it.name,it.recorded,"FIT",it.details,it.hasRoute,it.origin,it.id in retainedFiles,retainedFiles[it.id],it.id in correctedIds) } +
                summaries.map { it.managed(retainedFiles[it.id]) })
                .map { it.copy(name=names.values[it.id] ?: it.name,details=it.details.copy(bikeType=profiles.values[it.id]?.type ?: it.details.bikeType)) }
                .sortedByDescending { it.details.dateMs }
            entriesById=cachedEntries.associateBy { it.id }
            entriesRevision=routeRevision;entriesProfiles=profiles;entriesNames=names
        }
        return cachedEntries
    }
    fun entry(id:String):ManagedRoute? { entries;return entriesById[id] }
    private var orphanEntries:List<ManagedRoute>?=null
    private var orphanGroups=emptyList<OrphanProfileGroup>()
    internal fun orphanProfiles():List<OrphanProfileGroup> {
        val current=entries
        if(orphanEntries!==current) { orphanGroups=OrphanRideProfiles.find(context,current);orphanEntries=current }
        return orphanGroups
    }
    internal fun assignOrphanProfile(group:OrphanProfileGroup,targetId:String,targetType:BikeType,canEdit:()->Boolean,
        complete:(Result<Int>)->Unit):Boolean {
        if(busy || !canEdit()) return false
        val ids=group.rides.map { it.id }.toSet()
        if(ids.isEmpty()) return false
        val snapshot=entries
        val raw=RideProfileColor.catalog(context).raw
        val expected=ids.associateWith { raw[it] }
        busy=true;changed()
        worker.execute {
            var result:Result<Int> = Result.failure(IllegalStateException())
            finishOperation("") {
                result=runCatching {
                    check(canEdit())
                    val present=(library.entries.filter { it.origin==TrackOrigin.RECORDED }.map { it.id }+
                        fits.entries.filter { it.origin==TrackOrigin.RECORDED }.map { it.id }+
                        dreeveSummaries.entries.filter { it.origin==TrackOrigin.RECORDED }.map { it.id }).toSet()
                    check(ids.all { it in present })
                    val current=OrphanRideProfiles.find(context,snapshot).singleOrNull { it.key==group.key }
                    check(current?.rides?.map { it.id }?.toSet()==ids)
                    RideProfileColor.reassign(context,expected,targetId,targetType)
                }
                workerRoute
            }
            handler.post { complete(result) }
        }
        return true
    }
    init {
        worker.execute { finishOperation("") {
            workerRoute = library.load(); fits.load(); dreeveSummaries.load();fits.discoverDownloads(context)
            RideProfileColor.ensureIdentities(context)
            library.updateBikeTypes { RideProfileColor.type(context,it) }
            dreeveSummaries.updateBikeTypes { RideProfileColor.type(context,it) }
            if(preferences.contains("displayGpxId")) {
                val id=preferences.getString("displayGpxId","")?.takeIf { wanted -> library.entries.any { it.id==wanted } }
                workerRoute=library.select(id)
            } else preferences.edit().putString("displayGpxId",library.selected ?: "").apply()
            selectedFit = preferences.getString("displayFitId", null)?.takeIf { id -> fits.entries.any { it.id == id && it.hasRoute } }
            if (selectedFit != null) workerRoute = FitRead.read(fits.analysisFile(selectedFit!!)).route
            defaultRouteIfUnselected()
        } }
    }
    /** Worker only: use a GPS-capable recorded ride, never an arbitrary imported track. */
    private fun defaultRouteIfUnselected(): GpxRoute? {
        if (preferences.contains(MapRouteSlots.KEY) || selectedFit != null || library.selected != null) return workerRoute
        val latest = fits.entries.filter { it.origin == TrackOrigin.RECORDED && it.hasRoute && fits.file(it.id).canRead() }
            .maxByOrNull { it.details.dateMs } ?: return workerRoute
        val displayed = FitRead.read(fits.analysisFile(latest.id)).route ?: return workerRoute
        setSelectedFit(latest.id)
        return displayed
    }
    /** Yesterday's training suggestions use the same local sources and HR zones as ride details. */
    internal fun loadHrvTraining(day:java.time.LocalDate,zone:java.time.ZoneId,complete:(HrvTrainingSummary)->Unit):()->Unit {
        val cancelled=java.util.concurrent.atomic.AtomicBoolean(false)
        val source=entries
        val summary=HrvTrainingSummary.previousDay(day,zone,source)
        val profile=RiderProfileStore(preferences).read()
        worker.execute {
            val analyses=source.filter { it.id in summary.rideIds }.mapNotNull { entry ->
                if(cancelled.get()) return@execute
                runCatching {
                    val file=when(entry.format) {
                        "FIT"->fits.analysisFile(entry.id)
                        "GPX"->library.file(entry.id)
                        "JSON"->dreeveSummaries.file(entry.id)
                        else->return@mapNotNull null
                    }
                    RouteAnalysisRead.read(file,entry,profile,cancelled::get)
                }.getOrNull()
            }
            val value=summary.withHeart(profile,analyses)
            handler.post { if(!cancelled.get()) complete(value) }
        }
        return { cancelled.set(true) }
    }
    /** Details share the existing worker; decoding never mutates the displayed route or library. */
    internal fun loadAnalysis(id: String, complete: (Result<RouteAnalysis>) -> Unit): () -> Unit {
        val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
        worker.execute {
            val result = runCatching {
                val fit = fits.entries.singleOrNull { it.id == id }
                val gpx = library.entries.singleOrNull { it.id == id }
                val summary=dreeveSummaries.entries.singleOrNull { it.id==id }
                require(fit != null || gpx != null || summary!=null)
                val entry = if (fit != null) ManagedRoute(fit.id, fit.name, fit.recorded, "FIT", fit.details, fit.hasRoute, fit.origin,elevationCorrected=fits.corrected(id))
                    else if(summary!=null) summary.managed() else gpx!!.let { ManagedRoute(it.id, it.name, false, "GPX", it.details, true, it.origin) }
                val original = if (fit != null) fits.file(id) else if(summary!=null) dreeveSummaries.file(id) else library.file(id)
                val stamp = RouteFileStamp(original.length(), original.lastModified(), WebDavClient.hash(original))
                val namedEntry=entry.copy(name=RideNames.name(context,id) ?: entry.name,
                    details=entry.details.copy(bikeType=RideProfileColor.type(context,id) ?: entry.details.bikeType))
                val analysisFile=if(fit!=null) RideAnalysisFiles.file(context.filesDir,id).takeIf { it.isFile } ?: original else original
                var analysis = RouteAnalysisRead.read(analysisFile, namedEntry.copy(locallySaved = original.isFile, fileStamp = stamp), profile = RiderProfileStore(preferences).read(), cancelled = cancelled::get)
                if(analysisFile!=original && fits.corrected(id)) {
                    val corrected=RouteAnalysisRead.read(original,entry,profile=RiderProfileStore(preferences).read(),cancelled=cancelled::get)
                    val elevation=setOf(AnalysisMetric.ASCENT,AnalysisMetric.DESCENT)
                    analysis=analysis.copy(metrics=analysis.metrics+corrected.metrics.filterKeys { it in elevation },
                        series=analysis.series.filterNot { it.chart==AnalysisChart.ELEVATION }+corrected.series.filter { it.chart==AnalysisChart.ELEVATION })
                }
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
                        FitReexport.refreshElevation(context.filesDir,id)
                        fits.refreshDetails(id)
                    } catch(e:Exception) {
                        if(backup!=null) java.nio.file.Files.move(backup.toPath(),target.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                        else target.delete()
                        throw java.io.IOException(context.getString(R.string.elevation_failed),e)
                    } finally { backup?.delete() }

                }
                result.getOrThrow()
                if(selectedFit==id) FitRead.read(fits.analysisFile(id)).route else workerRoute
            }
            if(!cancelled.get()) handler.post { if(!cancelled.get()) complete(result) }
        }
        return { cancelled.set(true) }
    }
    fun setRouteVisible(visible: Boolean) {
        if (preferences.getBoolean("mapGpxVisible", true) == visible && (!visible || mapSlots.visible.all { it })) return
        if(visible) { mapSlots=mapSlots.all(true);mapSlots.write(preferences) }
        preferences.edit().putBoolean("mapGpxVisible", visible).apply()
        routeRevision++; changed()
    }
    internal fun setRouteSlotVisible(slot:Int) {
        if(busy || mapSlots.ids.getOrNull(slot)==null) return
        val master=preferences.getBoolean("mapGpxVisible",true)
        mapSlots=(if(master) mapSlots else mapSlots.all(false)).show(slot,!(master && mapSlots.visible[slot]))
        mapSlots.write(preferences)
        preferences.edit().putBoolean("mapGpxVisible",true).apply()
        routeRevision++;changed()
    }
    internal fun toggleMapRoute(id:String) {
        if(routeNumber(id)!=null && routeVisible(id)) removeMapRoute(id)
        else selectRoute(id)
    }
    internal fun removeMapRoute(id:String) {
        if(busy) return
        mapSlots=mapSlots.remove(id);mapSlots.write(preferences)
        operate("") { workerRoute }
    }
    internal fun assignMapRoute(id:String,slot:Int) {
        if(busy || entries.none { it.id==id && it.hasRoute && it.locallySaved }) return
        val current=if(preferences.getBoolean("mapGpxVisible",true)) mapSlots else mapSlots.all(false)
        mapSlots=current.assign(id,slot);mapSlots.write(preferences)
        preferences.edit().putBoolean("mapGpxVisible",true).apply()
        operate("") { workerRoute }
    }
    /** Only the library worker decodes geometry; unchanged routes are reused across refreshes. */
    private fun prepareMapRoutes():Pair<MapRouteSlots,List<DisplayedMapRoute>> {
        var slots=if(preferences.contains(MapRouteSlots.KEY)) MapRouteSlots.read(preferences) else {
            val legacy=preferences.getString("displayFitId",null)?.takeIf { it.isNotEmpty() }
                ?: preferences.getString("displayGpxId",null)?.takeIf { it.isNotEmpty() }
            if(legacy!=null && MapRouteSlots.validId(legacy)) MapRouteSlots().add(legacy) else MapRouteSlots()
        }
        val loaded=slots.ids.mapIndexedNotNull { index,id ->
            if(id==null) return@mapIndexedNotNull null
            val file=when {
                fits.entries.any { it.id==id && it.hasRoute } -> fits.analysisFile(id)
                library.entries.any { it.id==id } -> library.file(id)
                else->null
            }
            val geometry=file?.takeIf { it.isFile }?.let { source ->
                mapRouteCache[id]?.takeIf { it.path==source.absolutePath && it.length==source.length() && it.modified==source.lastModified() }?.route
                    ?: runCatching {
                        val route=if(source.extension.equals("gpx",true)) source.inputStream().use(GpxImport::read) else FitRead.read(source).route
                        route?.takeIf { it.points.isNotEmpty() }?.also { mapRouteCache[id]=CachedMapRoute(source.absolutePath,source.length(),source.lastModified(),it) }
                    }.getOrNull()
            }
            if(geometry==null) { slots=slots.remove(id);null } else DisplayedMapRoute(index,id,geometry)
        }
        mapRouteCache.keys.retainAll(loaded.map { it.id }.toSet())
        return slots to loaded
    }
    private fun finishOperation(success: String, action: () -> GpxRoute?) {
        val generation=mapProfileGeneration
        var text = success
        try { workerRoute = action() }
        catch (e: Exception) { text = "Strecke nicht geändert: ${e.message ?: "Datei nicht verfügbar"}" }
        val summaryEntries=dreeveSummaries.entries
        val prepared=prepareMapRoutes()
        val primary=prepared.first.order.lastOrNull()?.let { prepared.first.ids[it] }
        workerRoute=prepared.second.firstOrNull { it.id==primary }?.route
        val gpxEntries = library.entries; val fitEntries = fits.entries; val selected = primary
        val primaryFit=primary?.takeIf { id -> fitEntries.any { it.id==id } }
        val files = (gpxEntries.map { it.id to library.file(it.id) } + fitEntries.map { it.id to fits.file(it.id) } + summaryEntries.map { it.id to dreeveSummaries.file(it.id) })
            .mapNotNull { (id, file) -> if (file.isFile && file.canRead()) id to RouteFileStamp(file.length(), file.lastModified()) else null }.toMap()
        val displayed = workerRoute
        val corrected=fitEntries.filter { fits.corrected(it.id) }.map { it.id }.toSet()
        handler.post {
            summaries=summaryEntries
            correctedIds=corrected
            retainedFiles = files;routes=gpxEntries;activities=fitEntries
            if(generation==mapProfileGeneration) {
                val latest=MapRouteSlots.read(preferences)
                mapSlots=if(latest.ids==prepared.first.ids) prepared.first.copy(visible=latest.visible) else prepared.first
                mapRoutes=prepared.second;mapSlots.write(preferences)
                preferences.edit().putString("displayFitId",primaryFit ?: "").putString("displayGpxId",primary?.takeUnless { it==primaryFit } ?: "").apply()
                route=displayed;selectedRoute=selected
                busy=false
            }
            routeRevision++;message = text
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
                if (displayed != null) { library.select(null); setSelectedFit(entry.id);MapRouteSlots.read(preferences).add(entry.id).write(preferences) }
                displayed ?: workerRoute
            } else {
                val displayed = library.add(input, name); setSelectedFit(null)
                library.selected?.let { MapRouteSlots.read(preferences).add(it).write(preferences) };displayed
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
        progress:(DreeveItemResult)->Unit,complete:(Result<DreeveImportResult>)->Unit,mappings:Map<String,Int> = emptyMap(),origin:TrackOrigin=TrackOrigin.IMPORTED) {
        if(busy) { complete(Result.failure(IllegalStateException()));return }
        busy=true;changed()
        worker.execute {
            var result:Result<DreeveImportResult> = Result.failure(IllegalStateException())
            finishOperation("") {
                result=runCatching { DreeveImport(context,library,{ library.entries.map { it.id }.toSet()+fits.entries.map { it.id }+dreeveSummaries.entries.map { it.id } },dreeveSummaries,fits=fits,downloadFit=client::downloadOriginalFit,download=client::downloadGpx)
                    .read(target,items,cancelled,origin=origin,bikeTypeFor={ activity ->
                        activity.summary.gearId?.let { mappings[it] }?.let { slot ->
                            BikeProfileStore(context,preferences).all().find { it.slot==slot }?.type
                        } ?: activity.bikeType
                    }) { item ->
                        if(item.state==DreeveImportState.IMPORTED) {
                            val activity=items.find { it.id==item.id }
                            val local=item.localId ?: DreeveImport.localId(target,item.id)
                            activity?.summary?.gearId?.let { mappings[it] }?.takeIf { it>=0 }?.let { RideProfileColor.assign(context,local,it) }
                            activity?.let { RideNames.imported(context,local,it.name) }
                        }
                        handler.post { progress(item) }
                    } }
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
    internal fun prepareArchive(uri:Uri,cancelled:()->Boolean,complete:(Result<PreparedRideArchive>)->Unit) {
        if(busy) { complete(Result.failure(IllegalStateException()));return }
        busy=true;changed()
        worker.execute {
            var result:Result<PreparedRideArchive> = Result.failure(IllegalStateException())
            finishOperation("") {
                result=runCatching { context.contentResolver.openInputStream(uri)?.use { input ->
                    RouteArchiveImport(context.filesDir,context.cacheDir,fits,library).prepare(input,cancelled)
                } ?: error(context.getString(R.string.archive_import_open_failed)) }
                workerRoute
            }
            handler.post { complete(result) }
        }
    }
    internal fun importPreparedArchive(archive:PreparedRideArchive,ids:Set<String>,cancelled:()->Boolean,
        progress:(DreeveItemResult)->Unit,complete:(Result<DreeveImportResult>)->Unit) {
        if(busy) { complete(Result.failure(IllegalStateException()));return }
        busy=true;changed()
        worker.execute {
            var result:Result<DreeveImportResult> = Result.failure(IllegalStateException())
            finishOperation("") {
                result=runCatching { archive.importSelection(fits,library,ids,cancelled) { item -> handler.post { progress(item) } } }
                workerRoute
            }
            handler.post { complete(result) }
        }
    }
    internal fun discardArchive(archive:PreparedRideArchive) { worker.execute { archive.close() } }
    private fun setSelectedFit(id: String?) { selectedFit = id; preferences.edit().apply { if (id == null) remove("displayFitId") else putString("displayFitId", id);putString("displayGpxId",if(id==null) library.selected ?: "" else "") }.apply() }
    /** Queued on the existing library worker, so switching during a library operation is safe. */
    internal fun restoreProfileRoute() {
        mapProfileGeneration++
        mapSlots=MapRouteSlots.read(preferences);mapRoutes=emptyList();route=null;selectedRoute=null;routeRevision++
        val fit=preferences.getString("displayFitId",null)
        val gpx=preferences.getString("displayGpxId",null)
        busy=true
        worker.execute { finishOperation("") {
            selectedFit=fit?.takeIf { id -> fits.entries.any { it.id==id && it.hasRoute } }
            val selected=gpx?.takeIf { id -> library.entries.any { it.id==id } }
            val displayed=library.select(if(selectedFit==null) selected else null)
            selectedFit?.let { runCatching { FitRead.read(fits.analysisFile(it)).route }.getOrNull() } ?: displayed
        } }
    }
    fun selectRoute(id: String?) {
        if(busy) return
        if(id!=null) require(entries.any { it.id==id && it.hasRoute })
        val current=if(preferences.getBoolean("mapGpxVisible",true)) mapSlots else mapSlots.all(false)
        mapSlots=if(id==null) MapRouteSlots() else current.add(id)
        mapSlots.write(preferences)
        if(id!=null) preferences.edit().putBoolean("mapGpxVisible",true).apply()
        operate(if(id==null) "Strecke ausgeblendet" else "Strecke angezeigt") { workerRoute }
    }
    fun renameRoute(id: String, name: String) = operate("") {
        if (fits.entries.any { it.id == id }) fits.rename(id, name) else if(dreeveSummaries.entries.any { it.id==id }) dreeveSummaries.rename(id,name) else library.rename(id, name)
        RideNames.manual(context,id,name.trim().take(120))
        workerRoute
    }
    fun exportRoute(id: String) = operate("Nach Download/B-Cycle exportiert") { writeRouteDownload(id);workerRoute }
    private fun writeRouteDownload(id:String) {
        val fit = fits.entries.firstOrNull { it.id == id }
        val summary=dreeveSummaries.entries.singleOrNull { it.id==id }
        val file = if (fit != null) RouteTransferPayload.file(context,id) else if(summary!=null) dreeveSummaries.file(id) else library.file(id)
        val name = RideNames.name(context,id) ?: fit?.name ?: summary?.activity?.name ?: library.entries.single { it.id == id }.name
        RouteLocalSaves.perform(context, id, file, "Downloads/B-Cycle") {
            DownloadFiles.write(context, file, "$name.${file.extension}")
        }
    }
    internal fun uploadRoute(id: String,target:RouteTransferTarget?=null) = operate(context.getString(R.string.cloud_uploads_queued)) {
        val origin=fits.entries.singleOrNull { it.id==id }?.origin ?: library.entries.singleOrNull { it.id==id }?.origin
        require(origin == TrackOrigin.RECORDED) { "Cloud-Upload nur für gefahrene Fahrten" }
        if(target==null) RouteCloudUploads.retry(context,id) else {
            require(target!=RouteTransferTarget.LOCAL && target.enabled(context))
            target.retry(context,id)
        }; workerRoute
    }
    internal fun uploadAllIntervals(cancelled:()->Boolean,progress:(Int,Int,IntervalsBulkItem)->Unit,
        complete:(Result<IntervalsBulkResult>)->Unit):Boolean {
        if(busy) return false
        val snapshot=entries.toList()
        busy=true;changed()
        worker.execute {
            var result:Result<IntervalsBulkResult> = Result.failure(IllegalStateException())
            finishOperation("") {
                result=runCatching { IntervalsBulkUpload(context).run(snapshot,cancelled) { done,total,item -> handler.post { progress(done,total,item) } } }
                workerRoute
            }
            handler.post { complete(result) }
        }
        return true
    }
    internal fun recheckTransfer(id:String,target:RouteTransferTarget) {
        operate("") {
            val entry=entries.single { it.id==id }
            try {
                if(FitReexport.rebuild(context,entry)) fits.refreshDetails(id)
                if(target==RouteTransferTarget.LOCAL) writeRouteDownload(id) else RouteTransferRecheck.run(context,entry,target)
            }
            catch(e:ActivityServiceFailure) { throw IllegalStateException(context.getString(e.problem),e) }
            catch(e:IntervalsFailure) { throw IllegalStateException(context.getString(e.problem),e) }
            catch(e:java.io.IOException) { throw IllegalStateException(context.getString(ServiceProblems.text(e,R.string.route_transfer_check_failed)),e) }
            workerRoute
        }
    }
    fun saveRouteLocally(id: String) = operate("") {
        val file = if (fits.entries.any { it.id == id }) fits.file(id) else if(dreeveSummaries.entries.any { it.id==id }) dreeveSummaries.file(id) else library.file(id)
        require(file.isFile && file.canRead()) { "Lokale Originaldatei fehlt · bitte erneut importieren" }
        workerRoute
    }
    fun deleteRoute(id:String)=deleteRoutes(setOf(id))
    fun deleteRoutes(ids:Set<String>)=operate("Aus der Sammlung gelöscht · vorhandene Downloads bleiben erhalten") {
        ids.forEach { id -> workerRoute=deleteOne(id) };workerRoute
    }
    private fun deleteOne(id:String):GpxRoute? {
        MapRouteSlots.read(preferences).remove(id).write(preferences)
        val active = (selectedFit ?: library.selected) == id
        if (fits.entries.any { it.id == id }) { fits.delete(id); if (selectedFit == id) setSelectedFit(null) } else if(dreeveSummaries.entries.any { it.id==id }) dreeveSummaries.delete(id) else library.delete(id)
        RideAnalysisFiles.file(context.filesDir,id).delete()
        RideNames.forget(context,id)
        RideProfileColor.forget(context,id)
        RouteTransferPayload.forget(context,id)
        RouteLocalSaves.forget(context, id)
        IntervalsUploads.forget(context,id)
        ActivityServiceUploads.all.forEach { it.forget(context,id) }
        RideServiceLinks.forget(context,id)
        RideWeather.forget(context,id)
        return if (active) null else workerRoute
    }
}

data class ManagedRoute(val id: String, val name: String, val recorded: Boolean, val format: String,
                        val details: RouteDetails, val hasRoute: Boolean,
                        val origin: TrackOrigin = if (recorded) TrackOrigin.RECORDED else TrackOrigin.IMPORTED,
                        val locallySaved: Boolean = false, val fileStamp: RouteFileStamp? = null, val elevationCorrected:Boolean = false)
