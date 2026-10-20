package de.bcycle.app

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

internal data class DreeveActivity(val id:String,val name:String,val startLocal:String,val distanceM:Double?,
    val sportType:String,val hasGpx:Boolean,val summary:DreeveSummary=DreeveSummary()) {
    val bikeType get()=when(sportType) {
        "VirtualRide"->BikeType.INDOOR
        "EBikeRide","EMountainBikeRide"->BikeType.EBIKE
        "Ride","MountainBikeRide","GravelRide"->BikeType.BIKE
        else->null
    }
}
internal data class DreeveActivityPage(val activities:List<DreeveActivity>,val page:Int,val totalPages:Int,val total:Int) {
    companion object {
        fun decode(json:JSONObject,requested:Int):DreeveActivityPage {
            try {
                val pagination=json.getJSONObject("pagination")
                val page=pagination.getInt("page");val size=pagination.getInt("size")
                val pages=pagination.getInt("totalPages");val total=pagination.getInt("total")
                val rows=json.getJSONArray("activities")
                require(page==requested && size in 1..100 && pages in 0..10000 && total>=0 && rows.length()<=size)
                require(page<=maxOf(pages,1))
                val activities=(0 until rows.length()).map { i ->
                    val row=rows.getJSONObject(i)
                    val id=row.validId("id") ?: throw IllegalArgumentException()
                    val hasGpx=row.get("hasGpx");require(hasGpx is Boolean)
                    val name=row.optString("name").trim().take(120).ifBlank { id }
                    DreeveActivity(id,name,row.optString("startDateLocal").take(64),
                        row.optDouble("distanceInMeter").takeIf { it.isFinite() && it>=0 },row.optString("sportType").take(64),hasGpx,DreeveSummary.fromApi(row))
                }
                require(activities.map { it.id }.distinct().size==activities.size)
                return DreeveActivityPage(activities,page,pages,total)
            } catch(_:Exception) { throw ActivityServiceFailure(false,R.string.service_response_failed) }
        }
    }
}
internal enum class DreeveDataField(val label:Int) {
    POSITION(R.string.dreeve_field_position),TIME(R.string.dreeve_field_time),HEIGHT(R.string.dreeve_field_height),
    POWER(R.string.dreeve_field_power),HEART(R.string.dreeve_field_heart),CADENCE(R.string.dreeve_field_cadence),TEMPERATURE(R.string.dreeve_field_temperature)
}
internal enum class DreeveImportState { IMPORTED,DUPLICATE,UNAVAILABLE,FAILED }
internal data class DreeveItemResult(val id:String,val state:DreeveImportState,val fields:Set<DreeveDataField> = emptySet(),val problem:Int?=null,val summaryOnly:Boolean=false)
internal data class DreeveImportResult(val items:List<DreeveItemResult>,val interrupted:Boolean=false) {
    fun count(state:DreeveImportState)=items.count { it.state==state }
}

/** Runs on MapState's one library worker. No new library instance or automatic upload queue. */
internal class DreeveImport(private val context:Context,private val library:GpxLibrary,
    private val existingIds:()->Set<String>,private val summaries:DreeveSummaryLibrary?=null,private val download:(String,File)->Unit) {
    companion object {
        fun localId(account:String,activity:String)=UUID.nameUUIDFromBytes("bcycle:dreeve:$account\u0000$activity".toByteArray(Charsets.UTF_8)).toString()
    }
    fun read(account:String,activities:List<DreeveActivity>,cancelled:()->Boolean={false},origin:TrackOrigin=TrackOrigin.IMPORTED,progress:(DreeveItemResult)->Unit={}):DreeveImportResult {
        require(origin==TrackOrigin.IMPORTED || origin==TrackOrigin.RECORDED)
        val result=mutableListOf<DreeveItemResult>()
        val links=RideServiceLinks.forAccount(context,RideService.DREEVE,account).associateBy { it.activityId }.toMutableMap()
        for(activity in activities.distinctBy { it.id }) {
            if(cancelled()) return DreeveImportResult(result,true)
            val local=localId(account,activity.id)
            val item=try {
                when {
                    links[activity.id]?.rideId in existingIds()->DreeveItemResult(activity.id,DreeveImportState.DUPLICATE)
                    library.entries.any { it.id==local }->{
                        val hash=WebDavClient.hash(library.file(local))
                        RideServiceLinks.confirm(context,RideServiceLink(local,RideService.DREEVE,account,activity.id,hash))
                        DreeveItemResult(activity.id,DreeveImportState.DUPLICATE)
                    }
                    summaries?.entries?.any { it.id==local }==true->{
                        RideServiceLinks.confirm(context,RideServiceLink(local,RideService.DREEVE,account,activity.id,WebDavClient.hash(summaries.file(local))))
                        DreeveItemResult(activity.id,DreeveImportState.DUPLICATE)
                    }
                    !activity.hasGpx && summaries!=null->{
                        val file=summaries.add(local,activity,origin)
                        try {
                            val link=RideServiceLink(local,RideService.DREEVE,account,activity.id,WebDavClient.hash(file))
                            RideServiceLinks.confirm(context,link);links[activity.id]=link
                        } catch(e:Exception) { summaries.delete(local);throw e }
                        DreeveItemResult(activity.id,DreeveImportState.IMPORTED,summaryOnly=true)
                    }
                    !activity.hasGpx->DreeveItemResult(activity.id,DreeveImportState.UNAVAILABLE)
                    else->{
                        val temporary=File.createTempFile("dreeve-import-",".gpx",context.cacheDir)
                        try {
                            download(activity.id,temporary)
                            if(cancelled()) return DreeveImportResult(result,true)
                            val fields=mutableSetOf<DreeveDataField>()
                            temporary.inputStream().use { input -> library.add(input,activity.name,select=false,preserveDate=true,stableId=local,bikeType=activity.bikeType,origin=origin) { sample ->
                                fields.add(DreeveDataField.POSITION)
                                if(sample.time!=null) fields.add(DreeveDataField.TIME)
                                if(sample.height?.isFinite()==true) fields.add(DreeveDataField.HEIGHT)
                                if(sample.power!=null) fields.add(DreeveDataField.POWER)
                                if(sample.heart!=null) fields.add(DreeveDataField.HEART)
                                if(sample.cadence!=null) fields.add(DreeveDataField.CADENCE)
                                if(sample.temperature!=null) fields.add(DreeveDataField.TEMPERATURE)
                            } }
                            try {
                                library.rename(local,activity.name)
                                val link=RideServiceLink(local,RideService.DREEVE,account,activity.id,WebDavClient.hash(library.file(local)))
                                RideServiceLinks.confirm(context,link);links[activity.id]=link
                            } catch(e:Exception) { library.delete(local);throw e }
                            DreeveItemResult(activity.id,DreeveImportState.IMPORTED,fields)
                        } finally { temporary.delete() }
                    }
                }
            } catch(e:Exception) {
                if(cancelled()) return DreeveImportResult(result,true)
                DreeveItemResult(activity.id,DreeveImportState.FAILED,problem=(e as? ActivityServiceFailure)?.problem ?: R.string.dreeve_import_download_failed)
            }
            result.add(item);progress(item)
        }
        return DreeveImportResult(result)
    }
}
