package de.bcycle.app

import android.content.Context

internal enum class TrainingVariant(val delta:Int,val label:String) {
    QUICK_PLUS(-2,"Quick+"),QUICK(-1,"Quick"),STANDARD(0,"Std"),EXTENDED(1,"Ext"),EXTENDED_PLUS(2,"Ext+")
}

/** Conservative recognition from the actual expanded steps, shared by all import formats. */
internal object TrainingVariants {
    internal data class Block(val start:Int,val size:Int,val count:Int,val tailSize:Int=0) {
        val end get()=start+size*count-if(tailSize>0) size-tailSize else 0
    }
    private data class Key(val seconds:Double?,val targets:List<TrainingTarget>,val durationType:String,val durationValue:Double?)
    private val cache=object:LinkedHashMap<List<TrainingStep>,Block?>(32,.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<List<TrainingStep>,Block?>?)=size>32
    }
    @Synchronized internal fun block(workout:TrainingWorkout):Block? {
        if(cache.containsKey(workout.steps)) return cache[workout.steps]
        return detect(workout.steps).also { cache[workout.steps]=it }
    }
    private fun detect(steps:List<TrainingStep>):Block? {
        if(steps.size<9) return null
        val keys=steps.map { Key(it.seconds,it.targets.map { target -> target.copy(label="") },it.durationType,it.durationValue) }
        val candidates=ArrayList<Block>()
        // At most 64 different steps per repetition; linear matching for each period.
        for(size in 2..minOf(64,(steps.size-1)/4)) {
            val matches=IntArray(steps.size+1)
            for(i in steps.size-size-1 downTo 0) if(keys[i]==keys[i+size]) matches[i]=matches[i+1]+1
            for(start in 0..steps.size-size*4-1) {
                if(matches[start]<size*3+1) continue
                if(start>=size && (0 until size).all { keys[start-size+it]==keys[start+it] }) continue
                val tailSize=matches[start]%size
                val count=1+matches[start]/size+if(tailSize>0) 1 else 0
                if(count<5) continue
                val cycle=steps.subList(start,start+size)
                if(cycle.any { it.seconds==null }) continue
                val targets=cycle.map { step -> step.targets.firstOrNull { it.unit!=TrainingUnit.CADENCE } }
                val first=targets.first() ?: continue
                if(first.unit in setOf(TrainingUnit.OTHER,TrainingUnit.SPEED) || targets.any { it==null || it.unit!=first.unit }) continue
                val efforts=targets.map { (it!!.low+it.high)/2 }
                // Start with work and finish with lower recovery; no variants for a
                // repeated steady block, cadence-only changes or an arbitrary ramp.
                if(efforts.first()!=efforts.max() || efforts.last()>=efforts.first()) continue
                // The final repetition may contain only the work prefix, without
                // another recovery before cooldown. Never truncate a recovery or ramp.
                if(tailSize>0 && (efforts.take(tailSize).any { it!=efforts.first() } ||
                    efforts.drop(tailSize).any { it>=efforts.first() })) continue
                candidates+=Block(start,size,count,tailSize)
            }
        }
        // Coarser periods and partial copies must not create additional main sets.
        val maximal=candidates.filter { a -> candidates.none { b ->
            b!==a && b.start<=a.start && b.end>=a.end && (b.start<a.start || b.end>a.end || b.size<a.size)
        } }
        return maximal.singleOrNull()
    }
    fun available(workout:TrainingWorkout):List<TrainingVariant> {
        val block=block(workout) ?: return listOf(TrainingVariant.STANDARD)
        val duration=workout.steps.subList(block.start,block.start+block.size).sumOf { it.seconds!! }
        return TrainingVariant.entries.filter { variant ->
            block.count+variant.delta>0 && workout.steps.size+block.size*variant.delta<=4096 &&
                workout.steps.sumOf { it.seconds ?: 0.0 }+duration*variant.delta<=604800
        }
    }
    fun apply(workout:TrainingWorkout,variant:TrainingVariant):TrainingWorkout {
        if(variant==TrainingVariant.STANDARD || variant !in available(workout)) return workout
        val block=block(workout) ?: return workout
        val cycle=workout.steps.subList(block.start,block.start+block.size)
        val steps=buildList {
            addAll(workout.steps.take(block.start))
            repeat(block.count+variant.delta-if(block.tailSize>0) 1 else 0) { addAll(cycle) }
            if(block.tailSize>0) addAll(cycle.take(block.tailSize))
            addAll(workout.steps.drop(block.end))
        }
        return workout.copy(steps=steps)
    }
    private fun prefs(c:Context)=c.getSharedPreferences("training-variants",0)
    fun selected(c:Context,workout:TrainingWorkout):TrainingVariant {
        val saved=prefs(c).getString(workout.id,null)
        val value=TrainingVariant.entries.firstOrNull { it.name==saved } ?: TrainingVariant.STANDARD
        return value.takeIf { it in available(workout) } ?: TrainingVariant.STANDARD
    }
    fun cycle(c:Context,workout:TrainingWorkout):TrainingVariant {
        val options=available(workout)
        val next=options[(options.indexOf(selected(c,workout))+1)%options.size]
        prefs(c).edit().putString(workout.id,next.name).apply()
        return next
    }
    fun forget(c:Context,ids:Set<String>) { val edit=prefs(c).edit();ids.forEach { edit.remove(it) };edit.apply() }
}
