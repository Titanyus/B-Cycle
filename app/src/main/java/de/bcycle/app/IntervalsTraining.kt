package de.bcycle.app

import org.json.JSONArray
import org.json.JSONObject

/** Original workout_doc preserves ramp direction, unlike a Garmin FIT target range. */
internal object IntervalsTraining {
    fun read(row:JSONObject):TrainingWorkout? {
        val document=row.optJSONObject("workout_doc") ?: return null
        val source=document.optJSONArray("steps") ?: return null
        if(source.length()==0) return null
        val steps=ArrayList<TrainingStep>()
        fun number(value:Any?):Double?=(value as? Number)?.toDouble()?.also { require(it.isFinite() && it>=0) }
        fun target(key:String,value:Any?,ramp:Boolean):TrainingTarget? {
            if(value==null || value==JSONObject.NULL) return null
            val data=value as? JSONObject ?: JSONObject().put("value",value)
            val units=data.optString("units").lowercase()
            val unit=when(key) {
                "power"->when(units) { "%ftp"->TrainingUnit.FTP;"w","watts"->TrainingUnit.WATTS;"zone","power_zone"->TrainingUnit.POWER_ZONE;else->TrainingUnit.OTHER }
                "hr"->when(units) { "bpm"->TrainingUnit.HR_BPM;"%hr","%maxhr"->TrainingUnit.HR_MAX_PERCENT;"%lthr"->TrainingUnit.HR_THRESHOLD_PERCENT;"%hrr"->TrainingUnit.HR_RESERVE_PERCENT;"zone","hr_zone"->TrainingUnit.HR_ZONE;else->TrainingUnit.OTHER }
                "cadence"->TrainingUnit.CADENCE
                "pace"->if(units=="m/s") TrainingUnit.SPEED else TrainingUnit.OTHER
                else->TrainingUnit.OTHER
            }
            val start=number(data.opt("start"));val end=number(data.opt("end"))
            val low=if(ramp) start ?: number(data.opt("value")) else number(data.opt("value")) ?: start
            val high=if(ramp) end ?: number(data.opt("max_value")) ?: low else number(data.opt("max_value")) ?: end ?: low
            if(low==null || high==null) return null
            val scale=if(unit==TrainingUnit.FTP) 100.0 else 1.0
            return TrainingTarget(unit,low/scale,high/scale,ramp,"$key $units".trim())
        }
        fun append(array:JSONArray,depth:Int) {
            require(depth<=16 && array.length()<=4096)
            for(i in 0 until array.length()) {
                val entry=array.getJSONObject(i)
                val block=entry.optJSONArray("steps")
                if(block!=null) {
                    val repetitions=number(entry.opt("reps")) ?: 1.0
                    require(repetitions in 1.0..1000.0 && repetitions==repetitions.toInt().toDouble())
                    repeat(repetitions.toInt()) { append(block,depth+1) }
                    continue
                }
                val ramp=entry.optBoolean("ramp",false)
                val targets=listOf("power","hr","cadence","pace","resistance").mapNotNull { key -> target(key,entry.opt(key),ramp && key!="cadence") }
                val duration=number(entry.opt("duration"))?.takeIf { it>0 }
                val distance=number(entry.opt("distance"))?.takeIf { it>0 }
                val name=listOf("text","name").firstNotNullOfOrNull { entry.opt(it) as? String }.orEmpty().take(512)
                steps+=TrainingStep(name,duration,targets,when { duration!=null->"TIME";distance!=null->"DISTANCE_METERS";else->"OPEN" },duration ?: distance)
                require(steps.size<=4096)
            }
        }
        append(source,0)
        return TrainingWorkout("0".repeat(64),row.getString("name").take(512),row.optString("description","").takeIf { !row.isNull("description") }.orEmpty().take(20000),row.optString("type").ifBlank { "Ride" },"Intervals.icu","json",steps,
            sourceData=row.toString(),structureSource="intervals-native")
    }
}
