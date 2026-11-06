package de.bcycle.app

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

internal enum class TrainingKind(val title:Int) {
    ENDURANCE(R.string.training_endurance),INTERVALS(R.string.training_intervals),THRESHOLD(R.string.training_threshold),RECOVERY(R.string.training_recovery),OTHER(R.string.training_other)
}
internal enum class TrainingUnit { FTP,WATTS,HR_BPM,HR_MAX_PERCENT,POWER_ZONE,HR_ZONE,CADENCE,SPEED,OTHER,HR_THRESHOLD_PERCENT,HR_RESERVE_PERCENT }
internal data class TrainingTarget(val unit:TrainingUnit,val low:Double,val high:Double=low,val ramp:Boolean=false,val label:String="") {
    init { require(low.isFinite() && high.isFinite() && low>=0 && high>=0 && low<=100000 && high<=100000) }
    fun json()=JSONObject().put("unit",unit.name).put("low",low).put("high",high).put("ramp",ramp).put("label",label)
    fun fraction(profile:RiderProfile,end:Boolean=false):Double? {
        val value=if(ramp) { if(end) high else low } else (low+high)/2
        return when(unit) {
            TrainingUnit.FTP->value
            TrainingUnit.WATTS->profile.ftpW?.takeIf { it>0 }?.let { value/it }
            TrainingUnit.POWER_ZONE->listOf(.45,.65,.82,.98,1.12,1.35,1.6).getOrNull(value.toInt()-1)
            TrainingUnit.HR_ZONE->listOf(.5,.65,.75,.85,.95).getOrNull(value.toInt()-1)
            TrainingUnit.HR_MAX_PERCENT->value/100
            TrainingUnit.HR_THRESHOLD_PERCENT->profile.thresholdHeartRate?.let { threshold -> profile.maximumHeartRate?.takeIf { it>0 }?.let { value*threshold/100/it } }
            TrainingUnit.HR_RESERVE_PERCENT->profile.restingHeartRate?.let { rest -> profile.maximumHeartRate?.takeIf { it>rest }?.let { (rest+(it-rest)*value/100)/it } }
            TrainingUnit.HR_BPM->profile.maximumHeartRate?.takeIf { it>0 }?.let { value/it }
            else->null
        }
    }
    fun zone(profile:RiderProfile):Int? {
        val middle=(low+high)/2
        return when(unit) {
            TrainingUnit.POWER_ZONE,TrainingUnit.HR_ZONE->middle.toInt().takeIf { it in 1..if(unit==TrainingUnit.HR_ZONE) 5 else 7 }
            TrainingUnit.FTP->Coggan.boundaries.count { middle*100>it }+1
            TrainingUnit.WATTS->Coggan.zone(middle,profile.ftpW)
            TrainingUnit.HR_THRESHOLD_PERCENT->profile.thresholdHeartRate?.let { profile.heartZones()?.zoneFor(middle*it/100)?.index }
            TrainingUnit.HR_RESERVE_PERCENT->profile.restingHeartRate?.let { rest -> profile.maximumHeartRate?.let { profile.heartZones()?.zoneFor(rest+(it-rest)*middle/100)?.index } }
            TrainingUnit.HR_BPM->profile.heartZones()?.zoneFor(middle)?.index
            TrainingUnit.HR_MAX_PERCENT->profile.maximumHeartRate?.let { profile.heartZones()?.zoneFor(middle*it/100)?.index }
            else->null
        }
    }
    fun color(profile:RiderProfile):Int = zone(profile)?.let { (if(unit in setOf(TrainingUnit.HR_BPM,TrainingUnit.HR_ZONE,TrainingUnit.HR_MAX_PERCENT,TrainingUnit.HR_THRESHOLD_PERCENT,TrainingUnit.HR_RESERVE_PERCENT)) TrainingZoneColors.values else TrainingZoneColors.powerValues).getOrNull(it-1) } ?: TrainingZoneColors.grey
}
internal data class TrainingStep(val name:String,val seconds:Double?,val targets:List<TrainingTarget>,val durationType:String="TIME",val durationValue:Double?=seconds) {
    init { require(seconds==null || seconds.isFinite() && seconds>0 && seconds<=86400);require(name.length<=512 && targets.size<=4);require(durationValue==null || durationValue.isFinite() && durationValue>=0) }
    val target get()=targets.firstOrNull { it.unit!=TrainingUnit.CADENCE } ?: targets.firstOrNull()
    fun json()=JSONObject().put("name",name).put("seconds",seconds ?: JSONObject.NULL).put("durationType",durationType).put("durationValue",durationValue ?: JSONObject.NULL).put("targets",JSONArray(targets.map { it.json() }))
}
internal data class TrainingWorkout(val id:String,val name:String,val description:String,val sport:String,val source:String,val format:String,val steps:List<TrainingStep>,val tags:List<String> = emptyList(),val sourceData:String?=null,val structureSource:String="") {
    init { require(id.matches(Regex("[a-f0-9]{64}")));require(name.isNotBlank() && name.length<=512 && description.length<=20000);require(sourceData==null || sourceData.length<=8*1024*1024);require(steps.size in 1..4096);require(steps.sumOf { it.seconds ?: 0.0 }<=604800) }
    val seconds:Double? get()=if(steps.any { it.seconds==null }) null else steps.sumOf { it.seconds!! }
    val kind:TrainingKind get() {
        val words=(name+" "+tags.joinToString(" ")).lowercase()
        if(listOf("recovery","regener","erholung").any(words::contains)) return TrainingKind.RECOVERY
        if(listOf("vo2","intervall","interval","sprint","anaerob").any(words::contains)) return TrainingKind.INTERVALS
        if(listOf("threshold","schwelle","sweet spot","sweetspot","ftp").any(words::contains)) return TrainingKind.THRESHOLD
        val power=steps.mapNotNull { it.target?.takeIf { it.unit==TrainingUnit.FTP }?.let { (it.low+it.high)/2 } }
        if(power.count { it>=.95 }>=2 && power.any { it<.8 }) return TrainingKind.INTERVALS
        if(power.isNotEmpty() && power.max()<=.8 || listOf("endurance","ausdauer","base","grundlage").any(words::contains)) return TrainingKind.ENDURANCE
        return TrainingKind.OTHER
    }
    fun json()=JSONObject().put("schema",1).put("id",id).put("name",name).put("description",description).put("sport",sport).put("source",source).put("format",format).put("tags",JSONArray(tags)).put("steps",JSONArray(steps.map { it.json() })).put("intervalsData",sourceData?.let(::JSONObject) ?: JSONObject.NULL).put("structureSource",structureSource)
    companion object {
        fun id(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        fun decode(row:JSONObject):TrainingWorkout {
            require(row.getInt("schema")==1)
            val array=row.getJSONArray("steps");require(array.length() in 1..4096)
            val steps=(0 until array.length()).map { n -> val s=array.getJSONObject(n);val targets=s.getJSONArray("targets");require(targets.length()<=4)
                TrainingStep(s.getString("name"),if(s.isNull("seconds")) null else s.getDouble("seconds"),(0 until targets.length()).map { j -> val t=targets.getJSONObject(j);TrainingTarget(TrainingUnit.valueOf(t.getString("unit")),t.getDouble("low"),t.getDouble("high"),t.optBoolean("ramp"),t.optString("label")) },s.getString("durationType"),if(s.isNull("durationValue")) null else s.getDouble("durationValue")) }
            return TrainingWorkout(row.getString("id"),row.getString("name"),row.getString("description"),row.getString("sport"),row.getString("source"),row.getString("format"),steps,row.optJSONArray("tags")?.let { a -> (0 until minOf(a.length(),100)).map { a.getString(it) } }.orEmpty(),row.optJSONObject("intervalsData")?.toString(),row.optString("structureSource"))
        }
    }
}
internal enum class TrainingImportAction(val title:Int,val icon:BicycleIcon,val viewTag:String) {
    INTERVALS(R.string.training_import_intervals,BicycleIcon.Import,"training_import_intervals"),FILE(R.string.training_import_file,BicycleIcon.Import,"training_import_file")
}
