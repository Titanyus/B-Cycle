package de.bcycle.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import org.json.JSONObject

internal data class StatisticsRide(val id:String,val dateMs:Long,val distanceM:Double?,val movingMs:Long?,val bikeId:String,val bikeName:String?,val bikeColor:Int?)
internal data class BikeTimeShare(val id:String,val name:String?,val milliseconds:Double,val color:Int?)
internal data class RideStatistics(val year:Int,val months:Map<Int,List<Double>>,val weeks:List<Double>,val bikes:List<BikeTimeShare>,val rideCount:Int)

/** Worker-only cache. No FIT/GPX decoding; the library already provides validated summaries. */
internal class RideStatisticsCache {
    private var previous:List<StatisticsRide>?=null
    private var previousYear=0
    private var previousZone:ZoneId?=null
    private var cached:RideStatistics?=null
    fun get(entries:List<ManagedRoute>,profiles:RideProfileCatalog,year:Int,zone:ZoneId):RideStatistics {
        val rows=entries.asSequence().filter { it.origin==TrackOrigin.RECORDED }.map { route ->
            val detail=route.details
            val meta=profiles.values[route.id]
            val raw=profiles.raw[route.id]?.let { runCatching { JSONObject(it) }.getOrNull() }
            val identity=raw?.optString("bikeId")?.takeIf(String::isNotBlank)
                ?: raw?.takeIf { it.has("slot") }?.let { "legacy:${it.optInt("slot")}" } ?: ""
            val duration=detail.durationMs?.takeIf { it>=0 }
            val moving=detail.movingMs?.takeIf { it>=0 && (duration==null || it<=duration) } ?: duration
            StatisticsRide(route.id,detail.dateMs,detail.distanceM?.takeIf { it.isFinite() && it>=0 },moving,identity,meta?.name,meta?.color)
        }.sortedBy { it.id }.toList()
        if(rows==previous && year==previousYear && zone==previousZone) return checkNotNull(cached)
        val months=sortedMapOf<Int,DoubleArray>()
        val weeks=DoubleArray(LocalDate.of(year,12,28).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        rows.forEach { row -> if(row.dateMs>0) {
            val day=Instant.ofEpochMilli(row.dateMs).atZone(zone).toLocalDate()
            val values=months.getOrPut(day.year) { DoubleArray(12) }
            val km=(row.distanceM ?: 0.0)/1000
            values[day.monthValue-1]+=km
            if(day.get(IsoFields.WEEK_BASED_YEAR)==year) weeks[day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)-1]+=km
        } }
        val bikes=rows.groupBy { it.bikeId }.mapNotNull { (id,rides) ->
            val time=rides.sumOf { it.movingMs?.toDouble() ?: 0.0 }
            if(time>0) BikeTimeShare(id,rides.firstNotNullOfOrNull { it.bikeName },time,rides.firstNotNullOfOrNull { it.bikeColor }) else null
        }.sortedBy { it.id }
        return RideStatistics(year,months.mapValues { it.value.toList() },weeks.toList(),bikes,rows.size).also {
            previous=rows;previousYear=year;previousZone=zone;cached=it
        }
    }
}
