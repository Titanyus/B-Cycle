package de.bcycle.app

/** Integer BPM ranges are half-open. Z5 additionally includes the entered maximum BPM. */
data class KarvonenZone(val index: Int, val fromPercent: Int, val toPercent: Int, val lowerBpm: Int, val upperExclusiveBpm: Int) {
    val upperBpm get() = upperExclusiveBpm - 1
    val isEmpty get() = lowerBpm >= upperExclusiveBpm
    operator fun contains(bpm: Int) = bpm >= lowerBpm && bpm < upperExclusiveBpm
}
data class KarvonenResult(val restingHeartRate: Int, val maximumHeartRate: Int, val zones: List<KarvonenZone>) {
    val modelId = "karvonen-hrr-v1"
    val reserve get() = maximumHeartRate - restingHeartRate
    fun zoneFor(bpm: Int): KarvonenZone? = zones.firstOrNull { bpm in it }
}
object Karvonen {
    val boundariesPercent: List<Int> = listOf(0, 60, 70, 80, 90, 100)
    fun targetBpm(resting: Int, maximum: Int, intensity: Double): Double? =
        if (resting !in RiderProfileValidation.restingRange || maximum !in RiderProfileValidation.maximumRange || maximum <= resting || !intensity.isFinite() || intensity !in 0.0..1.0) null
        else resting + (maximum - resting) * intensity
    fun calculate(resting: Int?, maximum: Int?): KarvonenResult? {
        if (resting == null || maximum == null || targetBpm(resting, maximum, .5) == null) return null
        // Exact integer ceiling avoids floating-point artefacts at boundaries such as 133.000...1.
        val limits = boundariesPercent.map { (resting * 100 + (maximum - resting) * it + 99) / 100 }
        return KarvonenResult(resting, maximum, List(5) { i ->
            KarvonenZone(i + 1, boundariesPercent[i], boundariesPercent[i + 1], if(i==0) 1 else limits[i], if (i == 4) maximum + 1 else limits[i + 1])
        })
    }
}
