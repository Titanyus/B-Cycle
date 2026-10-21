package de.bcycle.app

import java.io.File
import java.time.Instant
import java.time.ZoneId

/** Immutable preview; files stay outside the libraries until explicitly selected. Worker only. */
internal data class PreparedArchiveRide(val id:String,val name:String,val format:String,val file:File,
    val details:RouteDetails,val hasRoute:Boolean,val known:Boolean) {
    fun selection():DreeveActivity {
        val metrics=buildMap {
            details.distanceM?.let { put(AnalysisMetric.DISTANCE,it/1000) }
            details.durationMs?.let { put(AnalysisMetric.ELAPSED,it/1000.0) }
            details.movingMs?.let { put(AnalysisMetric.MOVING,it/1000.0) }
            details.movingAverageKmh?.let { put(AnalysisMetric.AVG_SPEED,it) }
        }
        val sport=when(details.bikeType) { BikeType.BIKE->"Ride";BikeType.EBIKE->"EBikeRide";BikeType.INDOOR->"VirtualRide";null->"" }
        val date=details.dateMs.takeIf { it>0 }?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime().toString() }.orEmpty()
        return DreeveActivity(id,name,date,details.distanceM,sport,hasRoute,DreeveSummary(metrics),format)
    }
    companion object {
        fun read(id:String,name:String,format:String,source:File,target:File,known:Boolean):PreparedArchiveRide {
            val title=name.substringBeforeLast('.').trim().take(120).ifBlank { name.take(120) }
            val read=if(format=="FIT") FitRead.read(source) else {
                var firstTime:Long?=null
                val route=source.inputStream().use { GpxImport.samples(it) { sample -> if(firstTime==null) firstTime=sample.time } }
                ReadFit(RouteDetails(firstTime ?: 0,route.distanceM,route.durationMs,routePreview(route.points),route.ascentM,route.movingMs),route)
            }
            return PreparedArchiveRide(id,title,format,target,read.details,read.route!=null,known)
        }
    }
}

internal class PreparedRideArchive(private val directory:File,val entries:List<PreparedArchiveRide>,val report:ArchiveImportResult):AutoCloseable {
    /** The owning MapState worker serializes import and cleanup; never delete under an active reader. */
    fun importSelection(fits:FitLibrary,gpx:GpxLibrary,ids:Set<String>,cancelled:()->Boolean,
        progress:(DreeveItemResult)->Unit):DreeveImportResult {
        require(ids.all { wanted -> entries.any { it.id==wanted } })
        val fitHashes=fits.entries.map { it.id.substringAfter('-') }.toMutableSet()
        val gpxHashes=gpx.entries.mapNotNull { item -> gpx.file(item.id).takeIf { it.isFile }?.let(WebDavClient::hash) }.toMutableSet()
        val results=mutableListOf<DreeveItemResult>()
        for(entry in entries.filter { it.id in ids }) {
            if(cancelled()) return DreeveImportResult(results,true)
            val digest=entry.id.substringAfter('-');val hashes=if(entry.format=="FIT") fitHashes else gpxHashes
            val outcome=try {
                if(entry.known || digest in hashes) DreeveImportState.DUPLICATE else {
                    check(entry.file.isFile && WebDavClient.hash(entry.file)==digest)
                    entry.file.inputStream().use { input ->
                        if(entry.format=="FIT") checkNotNull(fits.add(input,entry.name,recorded=false,preserveDate=true))
                        else gpx.add(input,entry.name,select=false,preserveDate=true)
                    }
                    hashes+=digest;DreeveImportState.IMPORTED
                }
            } catch(_:Exception) { DreeveImportState.FAILED }
            DreeveItemResult(entry.id,outcome,problem=if(outcome==DreeveImportState.FAILED) R.string.archive_import_entry_failed else null)
                .also { results+=it;progress(it) }
        }
        return DreeveImportResult(results)
    }
    override fun close() { directory.deleteRecursively() }
}
