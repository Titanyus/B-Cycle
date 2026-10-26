package de.bcycle.app

import com.garmin.fit.Decode
import com.garmin.fit.MesgListener
import org.json.JSONArray
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

internal data class IntervalsRideIdentity(val start:Instant,val seconds:Double,val distance:Double?) {
    companion object {
        fun read(file:File,cancelled:()->Boolean):IntervalsRideIdentity {
            require(file.length() in 14..FitRead.MAX_BYTES)
            require(file.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
            var start:Double?=null;var first:Double?=null;var last:Double?=null
            var seconds=0.0;var distance=0.0;var hasDistance=false;var records=0
            val ok=file.inputStream().buffered().use { input -> Decode().read(input,MesgListener { message ->
                if(cancelled()) throw java.io.IOException()
                fun number(name:String)=message.getFieldDoubleValue(name)?.takeIf { it.isFinite() && it>=0 }
                if(message.num==18) {
                    number("start_time")?.let { start=minOf(start ?: it,it) }
                    number("total_elapsed_time")?.let { seconds+=it }
                    number("total_distance")?.let { distance+=it;hasDistance=true }
                }
                if(message.num==20) {
                    require(++records<=600_000)
                    number("timestamp")?.let { first=minOf(first ?: it,it);last=maxOf(last ?: it,it) }
                }
            }) }
            require(ok)
            val at=start ?: first ?: error("Missing activity start")
            if(seconds<=0) seconds=last?.minus(first ?: at) ?: 0.0
            require(seconds>0 && seconds<=31*86400)
            return IntervalsRideIdentity(Instant.ofEpochMilli((at*1000+631065600000L).toLong()),seconds,distance.takeIf { hasDistance })
        }
    }
}

internal data class IntervalsActivity(val id:String,val type:String,val utc:Instant?,val local:LocalDateTime?,
    val seconds:Double?,val distance:Double?,val external:String?) {
    fun nearStart(ride:IntervalsRideIdentity):Boolean = if(utc!=null) abs(utc.epochSecond-ride.start.epochSecond)<=2
        else local?.let { abs(java.time.Duration.between(it,LocalDateTime.ofInstant(ride.start,ZoneId.systemDefault())).seconds)<=2 } ?: false
    fun matches(ride:IntervalsRideIdentity)=nearStart(ride) && seconds!=null && abs(seconds-ride.seconds)<=2 &&
        (if(distance!=null && ride.distance!=null) abs(distance-ride.distance)<=maxOf(100.0,ride.distance*.01) else distance==null && ride.distance==null)
    companion object {
        private val cycling=setOf("Ride","VirtualRide","EBikeRide","EMountainBikeRide","MountainBikeRide","GravelRide","Handcycle","Velomobile")
        fun decode(text:String):List<IntervalsActivity> = try {
            val rows=JSONArray(text);require(rows.length()<1000) // Never infer absence from a potentially truncated response.
            val result=(0 until rows.length()).mapNotNull { i ->
                val row=rows.getJSONObject(i);val id=row.validId("id") ?: error("Missing id")
                val type=row.getString("type")
                if(type !in cycling) return@mapNotNull null
                val local=(row.opt("start_date_local") as? String)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
                val utc=(row.opt("start_date") as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    ?: (row.opt("timezone") as? String)?.let { zone -> runCatching { local?.atZone(ZoneId.of(zone.substringAfterLast(' ')))?.toInstant() }.getOrNull() }
                require(utc!=null || local!=null)
                fun number(key:String)=(row.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() && it>=0 }
                IntervalsActivity(id,type,utc,local,number("elapsed_time"),number("distance"),(row.opt("external_id") as? String)?.takeIf { it.isNotBlank() })
            }
            require(result.map { it.id }.distinct().size==result.size)
            result
        } catch(_:Exception) { throw IntervalsFailure(false,R.string.intervals_response_failed) }
    }
}
