package de.bcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal enum class HrvQuestion(val label:Int,val group:Int) {
    // SPORT is retained only for decoding legacy settings; never offered or re-exported as active.
    SLEEP(R.string.hrvq_sleep,0),WAKE(R.string.hrvq_wake,0),BED(R.string.hrvq_bed,0),SLEEP_DURATION(R.string.hrvq_sleep_duration,0),
    TRAINING(R.string.hrvq_training_setting,1),PHASE(R.string.hrvq_phase,1),SPORT(R.string.hrvq_sport,1),RPE(R.string.hrvq_rpe,1),
    DISTANCE(R.string.hrvq_distance,1),DURATION(R.string.hrvq_duration,1),MOTIVATION(R.string.hrvq_motivation,1),
    INJURY(R.string.hrvq_injured,2),CYCLE(R.string.hrvq_cycle,2),ENERGY(R.string.hrvq_energy,2),SORENESS(R.string.hrvq_soreness,2),CONDITION(R.string.hrvq_condition,2),FATIGUE(R.string.hrvq_fatigue,2),
    ALCOHOL(R.string.hrvq_alcohol,3),LIFESTYLE(R.string.hrvq_lifestyle,3),DIET(R.string.hrvq_diet,3),LOCATION(R.string.hrvq_location,3),SUPPLEMENTS(R.string.hrvq_supplements,3),SICK(R.string.hrvq_sick,3),TRAVEL(R.string.hrvq_traveled,3),NOTES(R.string.hrvq_notes,4)
}
internal data class HrvQuestionConfig(val enabled:Set<HrvQuestion> = HrvQuestion.entries.toSet()-setOf(HrvQuestion.SPORT,HrvQuestion.WAKE,HrvQuestion.BED,HrvQuestion.SLEEP_DURATION,HrvQuestion.PHASE,HrvQuestion.CYCLE,HrvQuestion.DIET,HrvQuestion.LOCATION,HrvQuestion.SUPPLEMENTS),
    val customNames:List<String> = listOf("","","")) {
    fun json():JSONObject {
        require(customNames.size==3 && customNames.all { it.length<=40 && it.none(Char::isISOControl) })
        val combined=combinedSleepTimes(enabled)
        return JSONObject().put("schema",1).put("enabled",JSONArray(HrvQuestion.entries.filter { it in combined && it!=HrvQuestion.SPORT }.map { it.name })).put("customNames",JSONArray(customNames))
    }
    companion object {
        fun combinedSleepTimes(fields:Set<HrvQuestion>)=if(HrvQuestion.WAKE in fields || HrvQuestion.BED in fields) fields+setOf(HrvQuestion.WAKE,HrvQuestion.BED) else fields
        fun decode(row:JSONObject):HrvQuestionConfig {
            require(row.getInt("schema")==1 && row.keys().asSequence().all { it in setOf("schema","enabled","customNames") })
            val enabled=row.getJSONArray("enabled");require(enabled.length()<=HrvQuestion.entries.size)
            val names=row.getJSONArray("customNames");require(names.length()==3)
            val fields=(0 until enabled.length()).map { HrvQuestion.valueOf(enabled.getString(it)) }
            require(fields.distinct().size==fields.size)
            return HrvQuestionConfig(combinedSleepTimes(fields.toSet()-HrvQuestion.SPORT),(0..2).map(names::getString)).also { it.json() }
        }
    }
}
internal class HrvQuestionSettings(private val context:Context) {
    companion object { const val STORE="hrv-questionnaire-settings";const val KEY="config" }
    fun cycleAvailable()=RiderProfileStore(context.getSharedPreferences("bike",0)).read().sex==RiderSex.FEMALE
    val preferences=context.getSharedPreferences(STORE,0)
    fun read()=runCatching { preferences.getString(KEY,null)?.let { HrvQuestionConfig.decode(JSONObject(it)) } ?: HrvQuestionConfig() }.getOrDefault(HrvQuestionConfig())
    fun write(value:HrvQuestionConfig) { preferences.edit().putString(KEY,value.json().toString()).apply() }
}
