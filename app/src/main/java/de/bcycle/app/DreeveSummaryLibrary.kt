package de.bcycle.app

import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

/** Reported aggregates only. Never turn averages into synthetic samples. */
internal data class DreeveSummary(val metrics:Map<AnalysisMetric,Double> = emptyMap(),val gearId:String?=null,val device:String?=null) {
    companion object {
        private val fields=mapOf(
            "distanceInMeter" to (AnalysisMetric.DISTANCE to .001),"elapsedTimeInSeconds" to (AnalysisMetric.ELAPSED to 1.0),
            "movingTimeInSeconds" to (AnalysisMetric.MOVING to 1.0),"averageSpeedInKmPerHour" to (AnalysisMetric.AVG_SPEED to 1.0),
            "maxSpeedInKmPerHour" to (AnalysisMetric.MAX_SPEED to 1.0),"elevationInMeter" to (AnalysisMetric.ASCENT to 1.0),
            "averagePower" to (AnalysisMetric.AVG_POWER to 1.0),"maxPower" to (AnalysisMetric.MAX_POWER to 1.0),
            "averageHeartRate" to (AnalysisMetric.AVG_HEART to 1.0),"maxHeartRate" to (AnalysisMetric.MAX_HEART to 1.0),
            "averageCadence" to (AnalysisMetric.AVG_CADENCE to 1.0),"calories" to (AnalysisMetric.CALORIES to 1.0),
            "kilojoules" to (AnalysisMetric.WORK to 1.0))
        fun fromApi(row:JSONObject):DreeveSummary {
            val metrics=fields.mapNotNull { (key,conversion) ->
                (row.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() && it>=0 && it<=1e12 }
                    ?.let { conversion.first to it*conversion.second }
            }.toMap()
            return DreeveSummary(metrics,row.validId("gearId"),(row.opt("deviceName") as? String)?.take(120))
        }
    }
    fun json()=JSONObject().put("metrics",JSONObject().also { values -> metrics.forEach { (key,value)->values.put(key.name,value) } })
        .put("gearId",gearId ?: JSONObject.NULL).put("deviceName",device ?: JSONObject.NULL)
}
internal data class StoredDreeveSummary(val id:String,val activity:DreeveActivity,val dateMs:Long=localDate(activity.startLocal),
    val origin:TrackOrigin=TrackOrigin.IMPORTED) {
    val details get():RouteDetails {
        fun duration(metric:AnalysisMetric)=activity.summary.metrics[metric]?.times(1000)?.toLong()
        return RouteDetails(dateMs,activity.distanceM,duration(AnalysisMetric.ELAPSED),ascentM=activity.summary.metrics[AnalysisMetric.ASCENT],
            movingMs=duration(AnalysisMetric.MOVING),bikeType=activity.bikeType)
    }
    companion object {
        private fun localDate(value:String)=runCatching { LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrDefault(0L)
    }
    fun managed(stamp:RouteFileStamp?=null)=ManagedRoute(id,activity.name,false,"JSON",details,false,origin,stamp!=null,stamp)
}

/** All operations use MapState's library worker. One atomic JSON snapshot per activity. */
internal class DreeveSummaryLibrary(root:File) {
    private val directory=File(root,"dreeve-summaries").apply { mkdirs() }
    var entries:List<StoredDreeveSummary> = emptyList();private set
    fun file(id:String):File {
        require(id.matches(Regex("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")))
        return File(directory,"$id.json")
    }
    fun load() {
        entries=directory.listFiles().orEmpty().mapNotNull { candidate ->
            candidate.name.removeSuffix(".bak").takeIf { it.matches(Regex("[a-f0-9-]{36}\\.json")) }?.removeSuffix(".json")
        }.distinct().mapNotNull { id -> runCatching { read(file(id)) }.getOrNull() }
    }
    fun add(id:String,activity:DreeveActivity,origin:TrackOrigin=TrackOrigin.IMPORTED):File {
        require(!activity.hasGpx && entries.none { it.id==id })
        val item=StoredDreeveSummary(id,activity,origin=origin);write(item);entries=entries+item
        return file(id)
    }
    internal fun restoreBackup(item:StoredDreeveSummary):Boolean {
        file(item.id)
        entries.singleOrNull { it.id==item.id }?.let { existing ->
            require(existing.dateMs==item.dateMs && existing.activity.copy(name=item.activity.name)==item.activity) { "Vorhandene Zusammenfassung hat andere Daten" }
            return false
        }
        write(item);entries=entries+item;return true
    }
    fun rename(id:String,name:String) {
        val title=name.trim().take(120);require(title.isNotEmpty())
        val current=entries.single { it.id==id };val next=current.copy(activity=current.activity.copy(name=title))
        write(next);entries=entries.map { if(it.id==id) next else it }
    }
    fun delete(id:String) { AtomicFile(file(id)).delete();entries=entries.filterNot { it.id==id } }
    private fun write(item:StoredDreeveSummary) {
        val a=item.activity
        val json=JSONObject().put("schema",1).put("id",item.id).put("activityId",a.id).put("name",a.name)
            .put("startDateLocal",a.startLocal).put("dateMs",item.dateMs).put("distanceInMeter",a.distanceM ?: JSONObject.NULL).put("sportType",a.sportType)
            .put("summary",a.summary.json()).put("origin",item.origin.name)
        val atomic=AtomicFile(file(item.id));val stream=atomic.startWrite()
        try { stream.write(json.toString().toByteArray(Charsets.UTF_8));atomic.finishWrite(stream) }
        catch(e:Exception) { atomic.failWrite(stream);throw e }
    }
    companion object {
        fun read(file:File):StoredDreeveSummary {
            val atomic=AtomicFile(file)
            val json=atomic.openRead().use { input ->
                val bytes=input.readNBytes(65537);require(bytes.size<=65536);JSONObject(String(bytes,Charsets.UTF_8))
            }
            require(json.getInt("schema")==1 && json.getString("id")==file.nameWithoutExtension)
            val values=json.getJSONObject("summary");val metrics=values.getJSONObject("metrics")
            // Only allow the documented source aggregates, not derived training statistics.
            val allowed=setOf(AnalysisMetric.DISTANCE,AnalysisMetric.ELAPSED,AnalysisMetric.MOVING,AnalysisMetric.AVG_SPEED,AnalysisMetric.MAX_SPEED,
                AnalysisMetric.ASCENT,AnalysisMetric.AVG_POWER,AnalysisMetric.MAX_POWER,AnalysisMetric.AVG_HEART,AnalysisMetric.MAX_HEART,
                AnalysisMetric.AVG_CADENCE,AnalysisMetric.CALORIES,AnalysisMetric.WORK)
            val summary=DreeveSummary(allowed.mapNotNull { metric -> (metrics.opt(metric.name) as? Number)?.toDouble()
                ?.takeIf { it.isFinite() && it>=0 }?.let { metric to it } }.toMap(),values.validId("gearId"),values.opt("deviceName") as? String)
            return StoredDreeveSummary(json.getString("id"),DreeveActivity(json.getString("activityId"),json.getString("name"),
                json.getString("startDateLocal"),(json.opt("distanceInMeter") as? Number)?.toDouble()?.takeIf { it.isFinite() && it>=0 },
                json.getString("sportType"),false,summary),json.getLong("dateMs"),if(json.has("origin")) TrackOrigin.valueOf(json.getString("origin")) else TrackOrigin.IMPORTED)
        }
    }
}
