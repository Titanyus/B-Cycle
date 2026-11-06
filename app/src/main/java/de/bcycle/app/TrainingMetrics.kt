package de.bcycle.app

import kotlin.math.pow

/** Estimates from prescribed power, not measured ride data or provider scores. */
internal data class TrainingMetrics(val seconds:Double?,val sections:Int,val intensity:Double?,val load:Double?) {
    companion object {
        private data class Key(val steps:List<TrainingStep>,val ftp:Int?)
        private val cache=object:LinkedHashMap<Key,TrainingMetrics>(32,.75f,true) {
            override fun removeEldestEntry(eldest:MutableMap.MutableEntry<Key,TrainingMetrics>?)=size>128
        }
        @Synchronized fun of(workout:TrainingWorkout,profile:RiderProfile):TrainingMetrics {
            val key=Key(workout.steps,profile.ftpW)
            return cache.getOrPut(key) { calculate(workout,profile) }
        }
        private fun calculate(workout:TrainingWorkout,profile:RiderProfile):TrainingMetrics {
            val total=workout.seconds
            fun missing()=TrainingMetrics(total,workout.steps.size,null,null)
            if(total==null || total<30) return missing()
            val ends=DoubleArray(workout.steps.size+1)
            val areas=DoubleArray(ends.size)
            val starts=DoubleArray(workout.steps.size)
            val slopes=DoubleArray(starts.size)
            for((i,step) in workout.steps.withIndex()) {
                val target=step.targets.firstOrNull { it.unit in setOf(TrainingUnit.FTP,TrainingUnit.WATTS) } ?: return missing()
                fun fraction(end:Boolean):Double? {
                    val value=if(target.ramp) { if(end) target.high else target.low } else (target.low+target.high)/2
                    return if(target.unit==TrainingUnit.FTP) value else profile.ftpW?.takeIf { it>0 }?.let { value/it }
                }
                val from=fraction(false) ?: return missing();val to=fraction(true) ?: return missing()
                val seconds=step.seconds!!
                starts[i]=from;slopes[i]=(to-from)/seconds
                ends[i+1]=ends[i]+seconds
                areas[i+1]=areas[i]+(from+to)/2*seconds
            }
            fun integral(time:Double):Double {
                if(time<=0) return 0.0
                if(time>=total) return areas.last()
                val found=ends.binarySearch(time)
                val i=if(found>=0) found else -found-2
                val offset=time-ends[i]
                return areas[i]+starts[i]*offset+slopes[i]*offset*offset/2
            }
            fun average(time:Double)=(integral(time)-integral(time-30))/30
            // Break at every target boundary and its 30 s shift. Within each interval the
            // rolling mean is quadratic; its fourth power is integrated exactly by 5-point Gauss.
            val knots=(listOf(30.0,total)+ends.flatMap { listOf(it,it+30) }.filter { it>30 && it<total }).distinct().sorted()
            val nodes=doubleArrayOf(0.0,-.5384693101056831,.5384693101056831,-.9061798459386640,.9061798459386640)
            val weights=doubleArrayOf(.5688888888888889,.4786286704993665,.4786286704993665,.2369268850561891,.2369268850561891)
            var fourth=0.0
            for(i in 0 until knots.size-1) {
                val middle=(knots[i]+knots[i+1])/2;val half=(knots[i+1]-knots[i])/2
                for(j in nodes.indices) fourth+=half*weights[j]*average(middle+half*nodes[j]).coerceAtLeast(0.0).pow(4)
            }
            val intensity=if(total==30.0) average(total) else (fourth/(total-30)).coerceAtLeast(0.0).pow(.25)
            return TrainingMetrics(total,workout.steps.size,intensity,total/3600*intensity*intensity*100)
        }
    }
}
