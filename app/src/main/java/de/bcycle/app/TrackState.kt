package de.bcycle.app

/** Persisted provenance, distinct from a file format or the currently selected map route. */
enum class TrackOrigin { IMPORTED, RECORDED, UNKNOWN }

data class RouteFileStamp(val length: Long, val modified: Long, val hash: String? = null)

/** Bounded streaming accumulation with 3 m hysteresis; missing heights/segments never join. */
internal class RouteElevation {
    private var anchor: Double? = null
    private var pairs = 0
    private var total = 0.0
    val ascent: Double? get() = total.takeIf { pairs > 0 }
    fun add(height: Double?, start: Boolean = false) {
        if (start) anchor = null
        if (height == null || !height.isFinite() || height !in -500.0..10000.0) { anchor = null; return }
        val previous = anchor
        if (previous == null) anchor = height
        else {
            pairs++
            if (kotlin.math.abs(height - previous) >= 3.0) {
                if (height > previous) total += height - previous
                anchor = height
            }
        }
    }
}
