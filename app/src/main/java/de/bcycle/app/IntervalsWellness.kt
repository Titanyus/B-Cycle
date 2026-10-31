package de.bcycle.app

import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** Daily API values only. No RR sequences, personal notes or invented readiness scores. */
internal data class IntervalsWellness(val day:LocalDate,val values:JSONObject) {
    init {
        require(values.keys().asSequence().all { it in fields })
        require(values.has("hrv") && values.getDouble("hrv").let { it.isFinite() && it>=0 })
        values.keys().forEach { require(values.getDouble(it).isFinite()) }
    }
    fun json()=JSONObject(values.toString()).put("id",day.toString())
    companion object {
        val hrvFields=setOf("hrv","hrvSDNN")
        val supplementFields=setOf("sleepScore","stress","injury")
        val fields=hrvFields+setOf("restingHR","sleepSecs","sleepScore","sleepQuality","motivation","soreness","fatigue","injury","stress")
        fun from(row:HrvHistorySummary,answers:HrvQuestionnaire?):IntervalsWellness? {
            if(!(row.acceptable || row.importedDay!=null && row.hasMeasurement)) return null
            val rmssd=(row.metrics["rmssdMs"] ?: row.rmssdMs)?.takeIf { it.isFinite() && it>=0 } ?: return null
            val day=HrvDailySelection.day(row)
            require(answers==null || answers.day==day.toString())
            val data=JSONObject().put("hrv",rmssd)
            row.metrics["sdnnMs"]?.takeIf { it.isFinite() && it>=0 }?.let { data.put("hrvSDNN",it) }
            row.metrics["restingHrBpm"]?.takeIf { it.isFinite() && it>0 && it<=300 }?.let { data.put("restingHR",it.roundToInt()) }
            answers?.let { q ->
                // API: positive ratings are Excellent(1)..Poor(4), burden None(0)..Extreme(4).
                q.ratings["sleep"]?.let {
                    data.put("sleepScore",it) // Numeric subjective 0–10 score, no invented provider normalization.
                    data.put("sleepQuality",1+((10-it)*3.0/10).roundToInt())
                }
                q.injured?.let { data.put("injury",if(it) 4 else 1) }
                q.stress?.let { data.put("stress",it) }
                q.ratings["motivation"]?.let { data.put("motivation",1+((10-it)*3.0/10).roundToInt()) }
                for(key in listOf("soreness","fatigue")) q.ratings[key]?.let { data.put(key,(it*4.0/10).roundToInt()) }
                val seconds=q.extras["sleepHours"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..24.0 }?.times(3600)?.roundToInt()
                    ?: runCatching {
                        val bed=java.time.LocalTime.parse(q.extras["bed"] ?: return@runCatching null)
                        val wake=java.time.LocalTime.parse(q.extras["wake"] ?: return@runCatching null)
                        Math.floorMod(wake.toSecondOfDay()-bed.toSecondOfDay(),86400)
                    }.getOrNull()
                seconds?.let { data.put("sleepSecs",it) }
            }
            return IntervalsWellness(day,data)
        }
        fun hasHrv(data:JSONObject)=hrvFields.any { data.has(it) && !data.isNull(it) }
        fun same(a:Any?,b:Any?):Boolean = if(a is Number && b is Number)
            a.toDouble().isFinite() && b.toDouble().isFinite() && abs(a.toDouble()-b.toDouble())<=maxOf(.0001,abs(b.toDouble())*.000001)
            else a==b
        fun ownsHrv(remote:JSONObject,receipt:JSONObject?)=receipt!=null && hasHrv(remote) &&
            hrvFields.filter { remote.has(it) && !remote.isNull(it) }.all { receipt.has(it) && same(remote.opt(it),receipt.opt(it)) }
        fun missingSupplements(record:IntervalsWellness,remote:JSONObject)=JSONObject().apply {
            supplementFields.filter { record.values.has(it) && (!remote.has(it) || remote.isNull(it)) }
                .forEach { put(it,record.values.get(it)) }
        }
        /** Existing foreign wellness fields are preserved even when that day has no HRV yet. */
        fun patch(record:IntervalsWellness,remote:JSONObject,receipt:JSONObject?):JSONObject = JSONObject().apply {
            record.values.keys().forEach { key ->
                if(key in hrvFields || !remote.has(key) || remote.isNull(key) || receipt?.has(key)==true && same(remote.opt(key),receipt.opt(key)))
                    put(key,record.values.get(key))
            }
            // Do not leave yesterday's selection of SDNN attached to a newer RMSSD-only measurement.
            if(!record.values.has("hrvSDNN") && receipt?.has("hrvSDNN")==true && same(remote.opt("hrvSDNN"),receipt.opt("hrvSDNN")))
                put("hrvSDNN",JSONObject.NULL)
        }
    }
}
