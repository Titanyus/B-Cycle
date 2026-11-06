package de.bcycle.app

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Physical axis values; no watts or elapsed times invented for unsupported target types. */
internal object TrainingChartAxes {
    fun targetWatts(target:TrainingTarget,profile:RiderProfile,end:Boolean=false):Double? {
        val value=if(target.ramp) { if(end) target.high else target.low } else (target.low+target.high)/2
        return when(target.unit) {
            TrainingUnit.WATTS->value
            TrainingUnit.FTP->profile.ftpW?.takeIf { it>0 }?.let { value*it }
            else->null
        }
    }
    fun watts(workout:TrainingWorkout,profile:RiderProfile):Double? {
        val targets=workout.steps.mapNotNull { it.target }
        if(targets.isEmpty()) return null
        val values=targets.flatMap { target -> listOf(targetWatts(target,profile) ?: return null,targetWatts(target,profile,true) ?: return null) }
        return values.max()
    }
    fun wattStep(peak:Double):Double {
        val desired=(peak/4).coerceAtLeast(50.0)
        val magnitude=10.0.pow(floor(log10(desired)))
        return listOf(1.0,2.0,5.0,10.0).first { it*magnitude>=desired }*magnitude
    }
    fun timeTicks(total:Double,slots:Float):List<Double> {
        val desired=total/slots.toDouble().coerceIn(2.0,8.0)
        val step=listOf(15.0,30.0,60.0,120.0,300.0,600.0,900.0,1200.0,1800.0,3600.0).firstOrNull { it>=desired }
            ?: ceil(desired/3600)*3600
        return listOf(0.0)+(1..floor(total/step).toInt()).map { it*step }.filter { it<total }+total
    }
}
