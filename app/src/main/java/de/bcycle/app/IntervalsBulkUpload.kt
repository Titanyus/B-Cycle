package de.bcycle.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.time.ZoneOffset
import java.time.YearMonth
import java.io.IOException

internal enum class IntervalsBulkState { UPLOADED,EXISTS,SKIPPED,FAILED,UNCLEAR }
internal data class IntervalsBulkItem(val name:String,val state:IntervalsBulkState,val problem:Int?=null)
internal data class IntervalsBulkResult(val items:List<IntervalsBulkItem>,val interrupted:Boolean=false,val problem:Int?=null,val detail:String?=null) {
    fun count(state:IntervalsBulkState)=items.count { it.state==state }
}

/** Explicit foreground batch only. Shared upload lock prevents races with automatic intervals jobs. */
internal class IntervalsBulkUpload(private val context:Context,
    private val client:(IntervalsConfig,()->Boolean)->IntervalsClient={ config,stopped -> IntervalsClient(config,IntervalsSettings(context).apiKey(),stopped) }) {
    fun run(rides:List<ManagedRoute>,cancelled:()->Boolean,progress:(Int,Int,IntervalsBulkItem)->Unit):IntervalsBulkResult=IntervalsUploads.serial {
        val settings=IntervalsSettings(context);val token=settings.connectionToken();val config=settings.read()
        if(!config.enabled || config.athlete.isBlank() || !settings.hasKey()) throw IntervalsFailure(false,R.string.intervals_enable_first)
        fun wifi()=if(!config.wifiOnly) true else context.getSystemService(ConnectivityManager::class.java).let { network ->
            network.getNetworkCapabilities(network.activeNetwork)?.let { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) }==true
        }
        if(!wifi()) return@serial IntervalsBulkResult(emptyList(),true,R.string.intervals_bulk_wifi)
        val stopped={ cancelled() || settings.connectionToken()!=token || !wifi() }
        val api=client(config,stopped)
        val months=mutableMapOf<YearMonth,MutableList<IntervalsActivity>>()
        val result=mutableListOf<IntervalsBulkItem>()
        fun link(ride:ManagedRoute,remote:String,hash:String) {
            // Byte-identical/local duplicate rides may already have this remote ID on another local entry.
            if(RideServiceLinks.forAccount(context,RideService.INTERVALS,config.athlete).none { it.activityId==remote && it.rideId!=ride.id })
                RideServiceLinks.confirm(context,RideServiceLink(ride.id,RideService.INTERVALS,config.athlete,remote,hash))
        }
        for(ride in rides.distinctBy { it.id }) {
            if(stopped()) return@serial IntervalsBulkResult(result,true,if(settings.connectionToken()!=token) R.string.intervals_bulk_changed else if(!wifi()) R.string.intervals_bulk_wifi else null)
            var global:Int?=null
            val item=try {
                if(!ride.locallySaved || ride.format !in setOf("FIT","GPX") ||
                    ride.origin!=TrackOrigin.RECORDED && !RouteTransferPayload.fromDreeve(context,ride.id)) {
                    IntervalsBulkItem(ride.name,IntervalsBulkState.SKIPPED)
                } else {
                    val file=RouteTransferPayload.file(context,ride.id,allowDreeveImport=true)
                    val length=file.length();val modified=file.lastModified();val hash=WebDavClient.hash(file)
                    val bikeType=RideProfileColor.type(context,ride.id)
                    // This explicit checked batch supersedes a queued blind retry of the same ride.
                    RideServiceLinks.links(context,ride.id)
                    IntervalsUploads.takePending(context,ride.id)
                    fun unchanged() {
                        if(stopped()) throw IOException()
                        check(file.isFile && file.length()==length && file.lastModified()==modified && WebDavClient.hash(file)==hash && RideProfileColor.type(context,ride.id)==bikeType)
                    }
                    val links=RideServiceLinks.links(context,ride.id).filter { it.service==RideService.INTERVALS && it.accountId==config.athlete }
                    var present:String?=null
                    for(old in links.asReversed()) {
                        if(api.activityExists(old.activityId)) { present=old.activityId;break }
                        unchanged()
                        // A confirmed 404, never a network failure, permits dropping the stale association.
                        IntervalsUploads.forget(context,ride.id);RideServiceLinks.missing(context,old);RideNames.missing(context,old)
                    }
                    if(present!=null) IntervalsBulkItem(ride.name,IntervalsBulkState.EXISTS) else {
                        val identity=IntervalsRideIdentity.read(file,stopped)
                        val month=YearMonth.from(identity.start.atZone(ZoneOffset.UTC))
                        val remote=months.getOrPut(month) { api.activities(month.atDay(1).minusDays(1),month.atEndOfMonth().plusDays(1)).toMutableList() }
                        val external="bcycle:${ride.id}"
                        val matches=remote.filter { it.external==external || it.matches(identity) }
                        unchanged()
                        when {
                            matches.size==1 -> { link(ride,matches.single().id,hash);IntervalsBulkItem(ride.name,IntervalsBulkState.EXISTS) }
                            matches.size>1 || remote.any { it.nearStart(identity) } -> IntervalsBulkItem(ride.name,IntervalsBulkState.UNCLEAR)
                            else -> {
                                val id=api.upload(file,"B-Cycle-${ride.id}.fit",external,ride.name)
                                unchanged()
                                IntervalsUploads.completed(context,file,ride.name,config,id)
                                remote.add(IntervalsActivity(id,"Ride",identity.start,null,identity.seconds,identity.distance,external))
                                IntervalsBulkItem(ride.name,IntervalsBulkState.UPLOADED)
                            }
                        }
                    }
                }
            } catch(error:Exception) {
                if(stopped()) return@serial IntervalsBulkResult(result,true,if(settings.connectionToken()!=token) R.string.intervals_bulk_changed else if(!wifi()) R.string.intervals_bulk_wifi else null)
                val problem=ServiceProblems.text(error,R.string.intervals_upload_failed)
                val service=ServiceProblems.of(error)
                if(service?.let { ServiceProblems.global(it) }==true || error is IntervalsFailure && error.problem==R.string.intervals_response_failed || error is IOException && error !is IntervalsFailure) global=problem
                IntervalsBulkItem(ride.name,IntervalsBulkState.FAILED,problem)
            }
            result.add(item);progress(result.size,rides.size,item)
            if(global!=null) return@serial IntervalsBulkResult(result,true,global)
        }
        IntervalsBulkResult(result)
    }
}
