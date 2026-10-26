package de.bcycle.app

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

internal data class DreeveActivity(val id:String,val name:String,val startLocal:String,val distanceM:Double?,
    val sportType:String,val hasGpx:Boolean,val summary:DreeveSummary=DreeveSummary(),val fileFormat:String?=null) {
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
internal data class DreeveItemResult(val id:String,val state:DreeveImportState,val fields:Set<DreeveDataField> = emptySet(),val problem:Int?=null,val summaryOnly:Boolean=false,
    val localId:String?=null,val fileFormat:String?=null,val fallbackProblem:Int?=null)
internal data class DreeveImportResult(val items:List<DreeveItemResult>,val interrupted:Boolean=false) {
    fun count(state:DreeveImportState)=items.count { it.state==state }
}

/** Runs on MapState's one library worker. No new library instance or automatic upload queue. */
internal class DreeveImport(private val context:Context,private val library:GpxLibrary,
    private val existingIds:()->Set<String>,private val summaries:DreeveSummaryLibrary?=null,
    private val fits:FitLibrary?=null,private val downloadFit:((String,File)->Boolean)?=null,private val download:(String,File)->Unit) {
    companion object {
        fun localId(account:String,activity:String)=UUID.nameUUIDFromBytes("bcycle:dreeve:$account\u0000$activity".toByteArray(Charsets.UTF_8)).toString()
    }
    fun read(account:String,activities:List<DreeveActivity>,cancelled:()->Boolean={false},origin:TrackOrigin=TrackOrigin.IMPORTED,
        bikeTypeFor:(DreeveActivity)->BikeType?={ it.bikeType },progress:(DreeveItemResult)->Unit={}):DreeveImportResult {
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
                        confirm(account,activity.id,local,library.file(local))
                        DreeveItemResult(activity.id,DreeveImportState.DUPLICATE,localId=local,fileFormat="GPX")
                    }
                    summaries?.entries?.any { it.id==local }==true->{
                        confirm(account,activity.id,local,summaries.file(local))
                        DreeveItemResult(activity.id,DreeveImportState.DUPLICATE,summaryOnly=true,localId=local)
                    }
                    else->preferred(account,activity,local,cancelled,origin,bikeTypeFor(activity))
                }
            } catch(e:Exception) {
                if(cancelled()) return DreeveImportResult(result,true)
                DreeveItemResult(activity.id,DreeveImportState.FAILED,problem=ServiceProblems.text(e,R.string.dreeve_import_download_failed))
            }
            if(item.state in setOf(DreeveImportState.IMPORTED,DreeveImportState.DUPLICATE)) item.localId?.let { id ->
                RideServiceLinks.links(context,id).firstOrNull { it.service==RideService.DREEVE && it.accountId==account && it.activityId==activity.id }?.let { links[activity.id]=it }
            }
            result.add(item);progress(item)
        }
        return DreeveImportResult(result)
    }
    private fun confirm(account:String,activity:String,id:String,file:File) {
        RideServiceLinks.confirm(context,RideServiceLink(id,RideService.DREEVE,account,activity,WebDavClient.hash(file)))
    }
    private fun preferred(account:String,activity:DreeveActivity,local:String,cancelled:()->Boolean,origin:TrackOrigin,bikeType:BikeType?):DreeveItemResult {
        var fallback:Int?=null
        if(fits!=null && downloadFit!=null) {
            val temporary=File.createTempFile("dreeve-original-",".fit",context.cacheDir)
            try {
                if(downloadFit.invoke(activity.id,temporary)) {
                    if(cancelled()) throw java.io.IOException()
                    val inspected=runCatching { DreeveFitFields.read(temporary,cancelled) }
                    if(cancelled()) throw java.io.IOException()
                    if(inspected.isSuccess) {
                        val before=fits.entries.map { it.id }.toSet()
                        val entry=temporary.inputStream().use { checkNotNull(fits.add(it,activity.name,origin==TrackOrigin.RECORDED,preserveDate=true,upgradeExisting=false)) }
                        val created=entry.id !in before
                        try {
                            if(created) fits.rename(entry.id,activity.name)
                            confirm(account,activity.id,entry.id,temporary)
                        } catch(error:Exception) { if(created) fits.delete(entry.id);throw error }
                        return DreeveItemResult(activity.id,if(created) DreeveImportState.IMPORTED else DreeveImportState.DUPLICATE,
                            inspected.getOrThrow(),localId=entry.id,fileFormat="FIT")
                    }
                    fallback=R.string.dreeve_fit_invalid_fallback
                }
            } finally { temporary.delete() }
        }
        if(cancelled()) throw java.io.IOException()
        if(!activity.hasGpx) {
            if(summaries==null) return DreeveItemResult(activity.id,DreeveImportState.UNAVAILABLE,fallbackProblem=fallback)
            val file=summaries.add(local,activity,origin,bikeType)
            try { confirm(account,activity.id,local,file) } catch(error:Exception) { summaries.delete(local);throw error }
            return DreeveItemResult(activity.id,DreeveImportState.IMPORTED,summaryOnly=true,localId=local,fallbackProblem=fallback)
        }
        val temporary=File.createTempFile("dreeve-import-",".gpx",context.cacheDir)
        try {
            download(activity.id,temporary)
            if(cancelled()) throw java.io.IOException()
            val fields=mutableSetOf<DreeveDataField>()
            temporary.inputStream().use { input -> library.add(input,activity.name,select=false,preserveDate=true,stableId=local,bikeType=bikeType,origin=origin) { sample ->
                fields.add(DreeveDataField.POSITION)
                if(sample.time!=null) fields.add(DreeveDataField.TIME)
                if(sample.height?.isFinite()==true) fields.add(DreeveDataField.HEIGHT)
                if(sample.power!=null) fields.add(DreeveDataField.POWER)
                if(sample.heart!=null) fields.add(DreeveDataField.HEART)
                if(sample.cadence!=null) fields.add(DreeveDataField.CADENCE)
                if(sample.temperature!=null) fields.add(DreeveDataField.TEMPERATURE)
            } }
            try { library.rename(local,activity.name);confirm(account,activity.id,local,library.file(local)) }
            catch(error:Exception) { library.delete(local);throw error }
            return DreeveItemResult(activity.id,DreeveImportState.IMPORTED,fields,localId=local,fileFormat="GPX",fallbackProblem=fallback)
        } finally { temporary.delete() }
    }
}
