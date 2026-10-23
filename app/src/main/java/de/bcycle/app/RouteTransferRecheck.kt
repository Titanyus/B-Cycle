package de.bcycle.app

import android.content.Context

/** Explicit worker-only action. A failed existence check never means a missing activity. */
internal object RouteTransferRecheck {
    fun run(c:Context,entry:ManagedRoute,target:RouteTransferTarget,
            exists:((RideServiceLink,()->Boolean)->Boolean)?=null) {
        require(target!=RouteTransferTarget.LOCAL && target.enabled(c))
        val id=entry.id
        fun identity()=when(target) {
            RouteTransferTarget.DREEVE->ActivityServiceSettings(c,ActivityService.DREEVE).read().identity(ActivityService.DREEVE)
            RouteTransferTarget.STRAVA->ActivityServiceSettings(c,ActivityService.STRAVA).read().identity(ActivityService.STRAVA)
            RouteTransferTarget.INTERVALS->IntervalsSettings(c).read().athlete
            RouteTransferTarget.WEBDAV->WebDavClient.directory(RouteExportSettings(c).read()).toString()
            else->error("Local export is separate")
        }
        val account=identity()
        val stopped={ !target.enabled(c) || identity()!=account }
        fun source()=if(RouteUploads.eligible(c,id)) RouteTransferPayload.file(c,id) else when(entry.format) {
            "GPX"->java.io.File(c.filesDir,"gpx-library/$id.gpx")
            "FIT"->FitLibrary.activeFile(c.filesDir,id)
            else->DreeveSummaryLibrary(c.filesDir).file(id)
        }
        val file=source()
        val hash=WebDavClient.hash(file);val length=file.length();val modified=file.lastModified()
        fun unchanged() {
            check(!stopped() && file.isFile && file.length()==length && file.lastModified()==modified &&
                WebDavClient.hash(source())==hash) { c.getString(R.string.route_source_changed) }
        }
        if(target==RouteTransferTarget.WEBDAV) {
            unchanged()
            // Conditional PUT plus byte verification keeps the same remote filename and never overwrites different data.
            RouteUploads.retry(c,id,force=true)
            return
        }
        val service=when(target) {
            RouteTransferTarget.DREEVE->RideService.DREEVE
            RouteTransferTarget.STRAVA->RideService.STRAVA
            else->RideService.INTERVALS
        }
        val links=RideServiceLinks.links(c,id).filter { it.service==service && it.accountId==account }
        val link=links.lastOrNull { it.fileHash==hash } ?: links.lastOrNull()
        // Existing Strava authorization requests activity:write only. A 404 without read_all
        // cannot establish deletion of a private ride, so its explicit hold repeats the upload.
        val canVerify=target!=RouteTransferTarget.STRAVA
        if(link!=null && canVerify) {
            val present=if(exists!=null) exists(link,stopped) else when(target) {
                RouteTransferTarget.DREEVE->ActivityServiceSettings(c,ActivityService.DREEVE).let {
                    DreeveClient(it.read(),it.credentials().getString("secret"),stopped).activityExists(link.activityId)
                }
                else->IntervalsClient(IntervalsSettings(c).read(),IntervalsSettings(c).apiKey(),stopped).activityExists(link.activityId)
            }
            unchanged()
            if(present) return
        }
        unchanged()
        if(RouteUploads.eligible(c,id)) {
            when(target) {
                RouteTransferTarget.DREEVE->ActivityServiceUploads.dreeve.retry(c,id,force=true)
                RouteTransferTarget.STRAVA->ActivityServiceUploads.strava.retry(c,id,force=true)
                else->IntervalsUploads.retry(c,id,force=true)
            }
        } else check(link!=null) { c.getString(R.string.route_transfer_ineligible) }
        // Queue replacement first so legacy intervals receipts cannot recreate the deleted link.
        if(link!=null && canVerify) { RideServiceLinks.missing(c,link);RideNames.missing(c,link) }
    }
}
