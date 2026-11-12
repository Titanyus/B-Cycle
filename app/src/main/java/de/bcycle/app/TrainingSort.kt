package de.bcycle.app

internal enum class TrainingSort(val title:Int,val icon:BicycleIcon) {
    DURATION(R.string.training_metric_duration,BicycleIcon.Duration),
    LOAD(R.string.training_metric_load,BicycleIcon.Power),
    INTENSITY(R.string.training_metric_intensity,BicycleIcon.Speed),
    STRENGTH(R.string.training_sort_strength,BicycleIcon.Training);
    fun value(metrics:TrainingMetrics):Double?=when(this) {
        STRENGTH->null;DURATION->metrics.seconds;LOAD->metrics.load;INTENSITY->metrics.intensity
    }
    fun value(workout:TrainingWorkout,profile:RiderProfile):Double? =
        if(this==STRENGTH) TrainingIntensity.of(workout,profile)?.ordinal?.toDouble() else value(TrainingMetrics.of(workout,profile))
    fun ordered(workouts:List<TrainingWorkout>,profile:RiderProfile,descending:Boolean):List<TrainingWorkout> {
        val values=workouts.associate { it.id to value(it,profile) }
        return workouts.sortedWith { a,b ->
            val av=values[a.id];val bv=values[b.id]
            val comparison=when { av==null && bv==null->0;av==null->1;bv==null->-1;descending->bv.compareTo(av);else->av.compareTo(bv) }
            if(comparison!=0) comparison else String.CASE_INSENSITIVE_ORDER.compare(a.name,b.name).takeIf { it!=0 } ?: a.id.compareTo(b.id)
        }
    }
}
