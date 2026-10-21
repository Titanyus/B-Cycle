package de.bcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class RideNamePart(val label:Int,val icon:BicycleIcon) {
    DATE(R.string.ride_name_date,BicycleIcon.Date),TIME(R.string.ride_name_time,BicycleIcon.Duration),
    DISTANCE(R.string.ride_name_distance,AnalysisMetric.DISTANCE.icon),
    MOVING(R.string.ride_name_moving,BicycleIcon.Duration),SPEED(R.string.ride_name_speed,BicycleIcon.Speed)
}
internal data class RideNaming(val text:String="Fahrt",val parts:Set<RideNamePart> = setOf(RideNamePart.DATE,RideNamePart.TIME)) {
    fun json()=JSONObject().put("text",text).put("parts",JSONArray(RideNamePart.entries.filter { it in parts }.map { it.name }))
    fun format(details:RouteDetails,zone:ZoneId,locale:Locale=Locale.getDefault()):String {
        val chunks=mutableListOf<String>();text.trim().takeIf { it.isNotEmpty() }?.let(chunks::add)
        val start=details.dateMs.takeIf { it>0 }?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val stamp=buildList {
            if(RideNamePart.DATE in parts && start!=null) add(start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy",locale)))
            if(RideNamePart.TIME in parts && start!=null) add(start.format(DateTimeFormatter.ofPattern("HH:mm",locale))+"h")
        }.joinToString(" ")
        if(stamp.isNotEmpty()) chunks+=stamp
        if(RideNamePart.DISTANCE in parts) details.distanceM?.let { chunks+=String.format(locale,"%.1fkm",it/1000) }
        if(RideNamePart.MOVING in parts) details.movingMs?.let { ms ->
            val seconds=ms/1000;chunks+=String.format(Locale.ROOT,"%d:%02d:%02dh",seconds/3600,seconds/60%60,seconds%60)
        }
        if(RideNamePart.SPEED in parts) details.movingAverageKmh?.let { chunks+=String.format(locale,"%.1fkm/h",it) }
        return chunks.joinToString(" · ").ifBlank { "Fahrt" }.take(120)
    }
    companion object {
        fun decode(json:JSONObject):RideNaming {
            require(json.keys().asSequence().all { it in setOf("text","parts") })
            val text=json.getString("text");require(text.length<=60 && text.none(Char::isISOControl))
            val array=json.getJSONArray("parts");require(array.length()<=RideNamePart.entries.size)
            val values=(0 until array.length()).map { RideNamePart.valueOf(array.getString(it)) }
            require(values.distinct().size==values.size)
            return RideNaming(text,values.toSet())
        }
    }
}
internal class RideNamingSettings(c:Context) {
    companion object { const val KEY="rideNamingV1" }
    private val prefs=c.getSharedPreferences("bike",0)
    fun read()=runCatching { prefs.getString(KEY,null)?.let { RideNaming.decode(JSONObject(it)) } ?: RideNaming() }.getOrDefault(RideNaming())
    fun write(value:RideNaming) { RideNaming.decode(value.json());prefs.edit().putString(KEY,value.json().toString()).apply() }
}
