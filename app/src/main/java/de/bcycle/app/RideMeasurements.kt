package de.bcycle.app

/** Numeric bike-supplied measurements shared by recording and presentation. */
internal object RideMeasurements {
    fun alignedRangeNames(data: LiveData): List<String>? {
        val names = data.assistNames ?: return null
        val ranges = data.rangesKm ?: return null
        return when {
            names.size == ranges.size -> names
            names.size == ranges.size + 1 && names.first().equals("OFF", true) -> names.drop(1)
            else -> null
        }
    }
    fun currentRangeKm(data: LiveData): Long? {
        val mode = data.value(LiveMetric.ASSIST_MODE)?.toInt() ?: return null
        val names = alignedRangeNames(data) ?: return null
        // A leading OFF without its own range shifts the bike-supplied mode index by one.
        val offset = (data.assistNames?.size ?: 0) - names.size
        return data.rangesKm?.getOrNull(mode - offset)
    }
}
