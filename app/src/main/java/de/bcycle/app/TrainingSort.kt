package de.bcycle.app

internal enum class TrainingSort(val title:Int,val icon:BicycleIcon) {
    DURATION(R.string.training_metric_duration,BicycleIcon.Duration),
    LOAD(R.string.training_metric_load,BicycleIcon.Power),
    INTENSITY(R.string.training_metric_intensity,BicycleIcon.Speed),
    SECTIONS(R.string.training_metric_sections,BicycleIcon.Sort);
    fun value(metrics:TrainingMetrics):Double?=when(this) {
        DURATION->metrics.seconds;LOAD->metrics.load;INTENSITY->metrics.intensity;SECTIONS->metrics.sections.toDouble()
    }
    fun ordered(workouts:List<TrainingWorkout>,profile:RiderProfile,descending:Boolean):List<TrainingWorkout> {
        val values=workouts.associate { it.id to value(TrainingMetrics.of(it,profile)) }
        return workouts.sortedWith { a,b ->
            val av=values[a.id];val bv=values[b.id]
            val comparison=when { av==null && bv==null->0;av==null->1;bv==null->-1;descending->bv.compareTo(av);else->av.compareTo(bv) }
            if(comparison!=0) comparison else String.CASE_INSENSITIVE_ORDER.compare(a.name,b.name).takeIf { it!=0 } ?: a.id.compareTo(b.id)
        }
    }
}
