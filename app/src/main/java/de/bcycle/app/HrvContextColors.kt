package de.bcycle.app

import java.time.LocalDate
import kotlin.math.roundToInt

internal enum class HrvContextMode(val label:Int,val icon:BicycleIcon) {
    STANDARD(R.string.hrvc_standard,BicycleIcon.Distance),TRAINING(R.string.hrvc_training,BicycleIcon.Bikes),
    SLEEP(R.string.hrvc_sleep,BicycleIcon.Moon),ENERGY(R.string.hrvc_energy,BicycleIcon.Power),
    SORENESS(R.string.hrvc_soreness,BicycleIcon.RecordingSettings),WELLBEING(R.string.hrvc_wellbeing,BicycleIcon.Heart)
}

/** Subjective context only: 0 good .. 10 poor; training retains its explicit 0..3 categories. */
internal object HrvContextColors {
    fun daily(answers:Map<LocalDate,HrvQuestionnaire>):Map<LocalDate,Map<HrvContextMode,Double>> {
        val result=mutableMapOf<LocalDate,MutableMap<HrvContextMode,Double>>()
        answers.forEach { (day,q) ->
            val values=result.getOrPut(day) { mutableMapOf() }
            fun burden(key:String,inverse:Boolean)=q.ratings[key]?.toDouble()?.let { if(inverse) 10-it else it }
            burden("sleep",true)?.let { values[HrvContextMode.SLEEP]=it }
            burden("energy",true)?.let { values[HrvContextMode.ENERGY]=it }
            burden("soreness",false)?.let { values[HrvContextMode.SORENESS]=it }
            listOfNotNull(burden("sleep",true),burden("energy",true),burden("motivation",true),burden("fatigue",false),burden("soreness",false))
                .takeIf { it.isNotEmpty() }?.average()?.let { values[HrvContextMode.WELLBEING]=it }
            q.training?.let { result.getOrPut(day.minusDays(1)) { mutableMapOf() }[HrvContextMode.TRAINING]=it.toDouble() }
        }
        return result.mapValues { it.value.toMap() }
    }
    fun aggregate(daily:Map<LocalDate,Map<HrvContextMode,Double>>,period:HrvTrendPeriod)=
        daily.entries.groupBy { period.start(it.key) }.mapValues { (_,days) ->
            HrvContextMode.entries.filter { it!=HrvContextMode.STANDARD }.mapNotNull { mode ->
                days.mapNotNull { it.value[mode] }.takeIf { it.isNotEmpty() }?.average()?.let { mode to it }
            }.toMap()
        }
    fun color(mode:HrvContextMode,value:Double?):Int {
        if(value==null || !value.isFinite()) return TrainingZoneColors.grey
        if(mode==HrvContextMode.TRAINING) return when(value.roundToInt().coerceIn(0,3)) {
            0->TrainingZoneColors.grey;1->TrainingZoneColors.green;2->TrainingZoneColors.orange;else->TrainingZoneColors.red
        }
        return TrainingZoneColors.powerValues[2+(value.coerceIn(0.0,10.0)/2).toInt().coerceAtMost(4)]
    }
}

internal data class HrvTrendPresentation(val series:Map<HrvTrendMetric,List<HrvTrendPoint>>,val context:Map<LocalDate,Map<HrvContextMode,Double>>)
