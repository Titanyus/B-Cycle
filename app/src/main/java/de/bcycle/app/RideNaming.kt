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
/** DATE and TIME retain independent switches but form one movable naming block. */
internal enum class RideNameGroup(val parts:List<RideNamePart>) {
    DATE_TIME(listOf(RideNamePart.DATE,RideNamePart.TIME)),DISTANCE(listOf(RideNamePart.DISTANCE)),
    MOVING(listOf(RideNamePart.MOVING)),SPEED(listOf(RideNamePart.SPEED))
}
internal data class RideNaming(val text:String="Fahrt",val parts:Set<RideNamePart> = setOf(RideNamePart.DATE,RideNamePart.TIME),
    val order:List<RideNameGroup> = RideNameGroup.entries.toList()) {
    init { require(order.size==RideNameGroup.entries.size && order.toSet()==RideNameGroup.entries.toSet()) }
    fun json()=JSONObject().put("text",text).put("parts",JSONArray(RideNamePart.entries.filter { it in parts }.map { it.name }))
        .put("order",JSONArray(order.map { it.name }))
    fun format(details:RouteDetails,zone:ZoneId,locale:Locale=Locale.getDefault()):String {
        val chunks=mutableListOf<String>();text.trim().takeIf { it.isNotEmpty() }?.let(chunks::add)
        val start=details.dateMs.takeIf { it>0 }?.let { Instant.ofEpochMilli(it).atZone(zone) }
        fun part(part:RideNamePart):String? = when(part) {
            RideNamePart.DATE->start?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy",locale))
            RideNamePart.TIME->start?.format(DateTimeFormatter.ofPattern("HH:mm",locale))?.plus("h")
            RideNamePart.DISTANCE->details.distanceM?.let { String.format(locale,"%.1fkm",it/1000) }
            RideNamePart.MOVING->details.movingMs?.let { ms ->
                val seconds=ms/1000;String.format(Locale.ROOT,"%d:%02d:%02dh",seconds/3600,seconds/60%60,seconds%60)
            }
            RideNamePart.SPEED->details.movingAverageKmh?.let { String.format(locale,"%.1fkm/h",it) }
        }
        order.forEach { group ->
            group.parts.filter { it in parts }.mapNotNull(::part).joinToString(" ").takeIf { it.isNotEmpty() }?.let(chunks::add)
        }
        return chunks.joinToString(" · ").ifBlank { "Fahrt" }.take(120)
    }
    companion object {
        fun decode(json:JSONObject):RideNaming {
            require(json.keys().asSequence().all { it in setOf("text","parts","order") })
            val text=json.getString("text");require(text.length<=60 && text.none(Char::isISOControl))
            val array=json.getJSONArray("parts");require(array.length()<=RideNamePart.entries.size)
            val values=(0 until array.length()).map { RideNamePart.valueOf(array.getString(it)) }
            require(values.distinct().size==values.size)
            val order=if(json.has("order")) json.getJSONArray("order").let { list ->
                require(list.length()==RideNameGroup.entries.size)
                (0 until list.length()).map { RideNameGroup.valueOf(list.getString(it)) }
            } else RideNameGroup.entries.toList()
            return RideNaming(text,values.toSet(),order)
        }
    }
}
internal class RideNamingSettings(c:Context) {
    companion object { const val KEY="rideNamingV1" }
    private val prefs=c.getSharedPreferences("bike",0)
    fun read()=runCatching { prefs.getString(KEY,null)?.let { RideNaming.decode(JSONObject(it)) } ?: RideNaming() }.getOrDefault(RideNaming())
    fun write(value:RideNaming) { RideNaming.decode(value.json());prefs.edit().putString(KEY,value.json().toString()).apply() }
}
