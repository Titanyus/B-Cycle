package de.bcycle.app

/** Intensity of the prescribed power targets, independent of warm-up/recovery duration. */
internal enum class TrainingIntensity(val title:Int) {
    EASY(R.string.training_tag_easy),SWEET_SPOT(R.string.training_tag_sweet_spot),HARD(R.string.training_tag_hard);
    companion object {
        fun of(workout:TrainingWorkout,profile:RiderProfile):TrainingIntensity? {
            val power=workout.steps.flatMap { it.targets }.filter { it.unit in setOf(TrainingUnit.FTP,TrainingUnit.WATTS,TrainingUnit.POWER_ZONE) }
            if(power.isEmpty()) return null
            val peaks=power.map { target ->
                val peak=maxOf(target.low,target.high)
                when(target.unit) {
                    TrainingUnit.FTP->peak
                    TrainingUnit.WATTS->profile.ftpW?.takeIf { it>0 }?.let { peak/it } ?: return null
                    // A power zone spans a range and cannot establish an exact FTP percentage.
                    else->return null
                }
            }
            return when(val peak=peaks.max()) {
                in 0.0..<.70->EASY
                in .88..<.95->SWEET_SPOT
                else->if(peak>=.95) HARD else null
            }
        }
    }
}
