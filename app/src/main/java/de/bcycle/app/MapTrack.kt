package de.bcycle.app

/** Display cache only. FIT and the disk journal retain the original selected samples. */
data class MapPoint(val latitude: Double, val longitude: Double, val segmentStart: Boolean = false)

class MapTrack(private val capacity: Int = 20_000) {
    val points = ArrayList<MapPoint>()
    var revision = 0L; private set
    private var lastTime: Long? = null
    private var gap = true
    private var spacing = 3.0
    init { require(capacity >= 4) }
    fun clear() { points.clear(); lastTime = null; gap = true; spacing = 3.0; revision++ }
    fun append(sample: RideSample) {
        val lat = sample.latitude; val lon = sample.longitude
        if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() || lat !in -85.05112878..85.05112878 || lon !in -180.0..180.0) {
            gap = true; return
        }
        val previousTime = lastTime
        if (previousTime != null && sample.elapsedMs <= previousTime) return
        val start = gap || previousTime == null || sample.elapsedMs - previousTime > 5_000
        lastTime = sample.elapsedMs
        val previous = points.lastOrNull()
        if (!start && previous != null) {
            if (sample.values["GPS_SPEED"]?.let { it < .7 } == true) return
            if (GpsRide.metres(GpsFix(0, previous.latitude, previous.longitude, 0.0), GpsFix(0, lat, lon, 0.0)) < spacing) return
        }
        gap = false
        points.add(MapPoint(lat, lon, start)); revision++
        if (points.size > capacity) {
            // Preserve breaks when coarsening; never join segments across a GPS outage.
            val reduced = ArrayList<MapPoint>(capacity / 2 + 1)
            var pendingStart = true
            points.forEachIndexed { index, point ->
                pendingStart = pendingStart || point.segmentStart
                if (index % 2 == 0 || index == points.lastIndex) {
                    reduced.add(point.copy(segmentStart = pendingStart)); pendingStart = false
                }
            }
            points.clear(); points.addAll(reduced); spacing *= 1.5
        }
    }
}
