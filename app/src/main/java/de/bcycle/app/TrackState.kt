package de.bcycle.app

/** Persisted provenance, distinct from a file format or the currently selected map route. */
enum class TrackOrigin { IMPORTED, RECORDED, UNKNOWN }

data class RouteFileStamp(val length: Long, val modified: Long, val hash: String? = null)

/** Streaming extrema with 3 m reversal hysteresis. A confirmed slope counts all height,
 * including sub-threshold progress beyond its extreme; small counter-movements stay ignored.
 * Missing heights and segment boundaries never join, and require no retained track samples.
 */
internal class RouteElevation {
    private var low: Double? = null
    private var high: Double? = null
    private var direction = 0
    private var pairs = 0
    private var up = 0.0
    private var down = 0.0
    val ascent: Double? get() = up.takeIf { pairs > 0 }
    val descent: Double? get() = down.takeIf { pairs > 0 }
    fun breakSegment() { low = null; high = null; direction = 0 }
    fun reset() { breakSegment(); pairs = 0; up = 0.0; down = 0.0 }
    fun add(height: Double?, start: Boolean = false) {
        if (start) breakSegment()
        if (height == null || !height.isFinite() || height !in -500.0..10000.0) { breakSegment(); return }
        if (low == null) { low = height; high = height; return }
        pairs++
        when (direction) {
            0 -> {
                low = minOf(low!!, height); high = maxOf(high!!, height)
                if (height - low!! >= reversalM) { up += height - low!!; high = height; direction = 1 }
                else if (high!! - height >= reversalM) { down += high!! - height; low = height; direction = -1 }
            }
            1 -> {
                if (height > high!!) { up += height - high!!; high = height }
                else if (high!! - height >= reversalM) { down += high!! - height; low = height; direction = -1 }
            }
            -1 -> {
                if (height < low!!) { down += low!! - height; low = height }
                else if (height - low!! >= reversalM) { up += height - low!!; high = height; direction = 1 }
            }
        }
    }
    private companion object { const val reversalM = 3.0 }
}
