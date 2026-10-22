package de.bcycle.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Subjective context only. Never an input to HRV calculation or measurement acceptance. */
internal data class HrvQuestionnaire(
    val day:String,
    val ratings:Map<String,Int> = emptyMap(),
    val training:Int?=null,
    val sport:String?=null,
    val trainingKm:Double?=null,
    val trainingMinutes:Double?=null,
    val trainingSource:String?=null,
    val rideIds:List<String> = emptyList(),
    val traveled:Boolean?=null,
    val alcohol:Int?=null,
    val sick:Boolean?=null,
    val injured:Boolean?=null,
    val injury:String="",
    val notes:String="",
    val updatedAtMs:Long=0,
    val extras:Map<String,String> = emptyMap(),
    val customNames:Map<String,String> = emptyMap(),
    val legacyRatings:Map<String,Int> = emptyMap()
) {
    fun json():JSONObject {
        validate()
        return JSONObject().put("schema",2).put("day",day).put("ratings",JSONObject().apply { ratings.toSortedMap().forEach { (k,v)->put(k,v) } })
            .put("legacyRatings",JSONObject().apply { legacyRatings.toSortedMap().forEach { (k,v)->put(k,v) } })
            .put("training",training ?: JSONObject.NULL).put("sport",sport ?: JSONObject.NULL)
            .put("trainingKm",trainingKm ?: JSONObject.NULL).put("trainingMinutes",trainingMinutes ?: JSONObject.NULL)
            .put("trainingSource",trainingSource ?: JSONObject.NULL).put("rideIds",JSONArray(rideIds))
            .put("traveled",traveled ?: JSONObject.NULL).put("alcohol",alcohol ?: JSONObject.NULL)
            .put("sick",sick ?: JSONObject.NULL).put("injured",injured ?: JSONObject.NULL)
            .put("injury",injury).put("notes",notes).put("updatedAtMs",updatedAtMs)
            .put("extras",JSONObject().apply { extras.toSortedMap().forEach { (k,v)->put(k,v) } })
            .put("customNames",JSONObject().apply { customNames.toSortedMap().forEach { (k,v)->put(k,v) } })
    }
    fun validate() {
        require(LocalDate.parse(day).toString()==day)
        require(ratings.all { (key,value)->key in ratingKeys && value in 0..10 })
        require(legacyRatings.all { (key,value)->key in ratingKeys && key!="rpe" && value in 0..8 })
        require(training==null || training in 0..3)
        require(sport==null || sport in sports)
        require(listOf(trainingKm,trainingMinutes).all { it==null || it.isFinite() && it in 0.0..1_000_000.0 })
        require(trainingSource==null || trainingSource in setOf("rides","manual"))
        require(rideIds.size<=10000 && rideIds.distinct().size==rideIds.size && rideIds.all { it.length in 1..128 && it.none(Char::isISOControl) })
        require(alcohol==null || alcohol in 0..2)
        require(extras.keys.all { it in extraKeys } && extras.values.all { it.length<=500 })
        extras.forEach { (key,value)-> when(key) {
            "wake","bed"->require(java.time.LocalTime.parse(value).second==0)
            "custom1","custom2","custom3"->require(value.toDoubleOrNull()?.let { it.isFinite() && it in -1_000_000.0..1_000_000.0 }==true)
            "phase"->require(value in phases)
            "cycle"->require(value in cycles)
        } }
        require(extras.keys.filter { it.startsWith("custom") }.all { it in customNames })
        require(customNames.all { (key,value)->key in setOf("custom1","custom2","custom3") && value.length in 1..40 && value.none(Char::isISOControl) })
        require(injury.length<=500 && notes.length<=1000 && updatedAtMs>=0)
    }
    companion object {
        val extraKeys=setOf("wake","bed","phase","cycle","diet","location","supplements","custom1","custom2","custom3")
        val phases=listOf("base","build","competition","recovery","other")
        val cycles=listOf("menstruation","follicular","ovulation","luteal","unknown")
        val ratingKeys=setOf("sleep","motivation","condition","energy","soreness","fatigue","lifestyle","rpe")
        val sports=listOf("cycling","running","swimming","strength","other")
        fun forMeasurement(row:JSONObject)=HrvQuestionnaire(
            Instant.ofEpochMilli(row.getLong("utcMs")).atZone(ZoneId.of(row.getString("zone"))).toLocalDate().toString())
        fun decode(row:JSONObject):HrvQuestionnaire {
            val schema=row.getInt("schema");require(schema in 1..2)
            fun int(key:String):Int? {
                if(row.isNull(key)) return null
                val value=row.getDouble(key);require(value.isFinite() && value==value.toInt().toDouble());return value.toInt()
            }
            fun boolean(key:String)=if(row.isNull(key)) null else row.getBoolean(key)
            fun string(key:String)=if(row.isNull(key)) null else row.getString(key)
            fun number(key:String)=if(row.isNull(key)) null else row.getDouble(key)
            fun map(key:String)=row.optJSONObject(key)?.let { all->all.keys().asSequence().associateWith(all::getString) }.orEmpty()
            val ratings=row.getJSONObject("ratings");val ids=row.getJSONArray("rideIds")
            require(ids.length()<=10000)
            val original=ratings.keys().asSequence().associateWith { key ->
                val value=ratings.getDouble(key)
                require(value.isFinite() && value==value.toInt().toDouble() && value in 0.0..if(schema==1 && key!="rpe") 8.0 else 10.0)
                value.toInt()
            }
            val legacy=if(schema==1) original.filterKeys { it!="rpe" } else row.optJSONObject("legacyRatings")?.let { all ->
                all.keys().asSequence().associateWith { key ->
                    val value=all.getDouble(key);require(value.isFinite() && value==value.toInt().toDouble());value.toInt()
                }
            }.orEmpty()
            val normalized=if(schema==1) original.mapValues { (key,value)->if(key=="rpe") value else (value*10.0/8).roundToInt() } else original
            return HrvQuestionnaire(row.getString("day"),normalized,int("training"),string("sport"),number("trainingKm"),number("trainingMinutes"),string("trainingSource"),
                (0 until ids.length()).map(ids::getString),boolean("traveled"),int("alcohol"),boolean("sick"),boolean("injured"),
                row.getString("injury"),row.getString("notes"),row.getLong("updatedAtMs"),map("extras"),map("customNames"),legacy).also { it.validate() }
        }
    }
}

internal data class HrvTrainingSummary(val rideIds:List<String>,val km:Double?,val minutes:Double?) {
    companion object {
        /** Calendar day in the measurement's saved timezone, including DST and all bike profiles. */
        fun previousDay(day:LocalDate,zone:ZoneId,entries:List<ManagedRoute>):HrvTrainingSummary {
            val start=day.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end=day.atStartOfDay(zone).toInstant().toEpochMilli()
            val rides=entries.filter { it.origin==TrackOrigin.RECORDED && it.details.dateMs in start until end }.distinctBy { it.id }
            val distances=rides.map { it.details.distanceM?.takeIf { d->d.isFinite() && d>=0 } }
            val durations=rides.map { it.details.movingMs ?: it.details.durationMs }
            return HrvTrainingSummary(rides.map { it.id },
                if(rides.isNotEmpty() && distances.all { it!=null }) distances.sumOf { it!! }/1000 else null,
                if(rides.isNotEmpty() && durations.all { it!=null && it>=0 }) durations.sumOf { it!! }/60000.0 else null)
        }
    }
}
