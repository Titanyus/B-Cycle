package de.bcycle.app

/** Exact piecewise-constant integral. Candidates include both incoming and shifted outgoing boundaries.
 * Memory depends on the window duration, never the ride duration. Gaps reset continuity, not prior bests.
 */
internal class PowerDurationWindow(val durationMs: Long) {
    private data class Segment(val start: Long, var end: Long, val watts: Double)
    private val segments = ArrayDeque<Segment>()
    private var epoch = 0L
    private var lastEnd: Long? = null
    private var integral = 0.0 // W * ms
    var bestAverageW: Double? = null; private set
    init { require(durationMs > 0) }
    fun breakContinuity() { segments.clear(); lastEnd = null; integral = 0.0 }
    fun add(start: Long, end: Long, watts: Double) {
        require(end > start && watts.isFinite() && watts >= 0)
        if (lastEnd != start) { breakContinuity(); epoch = start }
        val tail = segments.lastOrNull()
        if (tail != null && tail.end == start && tail.watts == watts) tail.end = end
        else segments.addLast(Segment(start, end, watts))
        var cursor = start
        while (cursor < end) {
            if (cursor - epoch < durationMs) {
                val next = minOf(end, epoch + durationMs)
                integral += watts * (next - cursor); cursor = next
            } else {
                while (segments.first().end <= cursor - durationMs) segments.removeFirst()
                val outgoing = segments.first()
                val next = minOf(end, outgoing.end + durationMs)
                integral += (watts - outgoing.watts) * (next - cursor); cursor = next
            }
            if (cursor - epoch >= durationMs) {
                val average = (integral / durationMs).coerceAtLeast(0.0)
                bestAverageW = maxOf(bestAverageW ?: average, average)
            }
        }
        lastEnd = end
    }
}
