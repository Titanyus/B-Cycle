package de.bcycle.app

/** Positive cadence samples only; sensor records (including zero) remain untouched. */
internal class PedalingAverage {
    private var count = 0L
    var value: Double? = null; private set
    fun add(rpm: Double) {
        if (!rpm.isFinite() || rpm <= 0) return
        count++
        value = (value ?: rpm) + (rpm - (value ?: rpm)) / count
    }
    fun reset() { count = 0; value = null }
}

/** One observation per second and at most 12 points per average, independent of UI redraw frequency. */
internal class AverageTrends {
    private val histories = mutableMapOf<String, ArrayDeque<Pair<Long, Double>>>()
    private val recentPower = ArrayDeque<Pair<Long, Double>>()
    /** Direct values reuse their corresponding average's existing trend and history. */
    fun indicator(field: DashboardField, data: LiveData, gps: GpsValues, pulse: PulseValues, now: Long): Int {
        val averageId = when (field.id) {
            "SPEED" -> "AVERAGE_SPEED"
            "GPS_SPEED" -> "GPS_MOVING_AVERAGE"
            "RIDER_POWER" -> "AVERAGE_POWER"
            "CADENCE" -> "AVERAGE_CADENCE"
            "HEART_RATE" -> "HEART_AVERAGE"
            else -> null
        }
        if (averageId != null) return indicator(DashboardField(averageId), data, gps, pulse, now)
        if (field.id == "AVERAGE_POWER") {
            val power = data.riderPowerWatts?.toDouble()
            val average = field.averageNumber(data, gps, pulse)
            if (power == null || average == null) { recentPower.clear(); return 0 }
            observe(recentPower, power, now)
            val duration = now - recentPower.first().first
            if (duration < 8_000) return 0
            var energy = 0.0
            var previous = recentPower.first()
            for (point in recentPower.drop(1)) {
                energy += previous.second * (point.first - previous.first)
                previous = point
            }
            energy += previous.second * (now - previous.first)
            return compare(energy / duration, average)
        }
        return direction(field.id, field.averageNumber(data, gps, pulse), now)
    }
    private fun observe(points: ArrayDeque<Pair<Long, Double>>, number: Double, now: Long) {
        if (points.lastOrNull()?.let { now < it.first || now - it.first > 15_000 } == true) points.clear()
        if (points.lastOrNull()?.let { now - it.first >= 1000 } != false) points.addLast(now to number)
        while (points.size > 12 || points.size > 1 && now - points.elementAt(1).first >= 10_000) points.removeFirst()
    }
    private fun compare(current: Double?, reference: Double?): Int {
        if (current == null || reference == null || !current.isFinite() || !reference.isFinite()) return 0
        val delta = current - reference
        return when { delta > .000001 -> 1; delta < -.000001 -> -1; else -> 0 }
    }
    fun direction(id: String, number: Double?, now: Long): Int {
        if (number == null || !number.isFinite()) { histories.remove(id); return 0 }
        val points = histories.getOrPut(id) { ArrayDeque() }
        observe(points, number, now)
        val first = points.first()
        if (now - first.first < 8_000) return 0
        val delta = number - first.second
        // Cumulative averages change progressively less as the ride gets longer.
        // Ignore floating-point noise, rather than requiring a visible decimal step.
        return when { delta > .000001 -> 1; delta < -.000001 -> -1; else -> 0 }
    }
}

internal fun DashboardField.averageNumber(data: LiveData, gps: GpsValues, pulse: PulseValues): Double? = when (id) {
    "GPS_MOVING_AVERAGE" -> gps.movingAverageKmh
    "GPS_OVERALL_AVERAGE" -> gps.overallAverageKmh
    "HEART_AVERAGE" -> pulse.average
    "AVERAGE_SPEED" -> data.value(LiveMetric.AVERAGE_SPEED)?.div(LiveMetric.AVERAGE_SPEED.divisor)
    "AVERAGE_POWER" -> data.value(LiveMetric.AVERAGE_POWER)?.toDouble()
    "AVERAGE_CADENCE" -> data.value(LiveMetric.AVERAGE_CADENCE)?.div(LiveMetric.AVERAGE_CADENCE.divisor)
    else -> null
}

/** Scales with the adjacent number, without affecting the number's centre or field geometry. */
internal class AverageChevronSpan(private val direction: Int) : android.text.style.ReplacementSpan() {
    private val line = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE; strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
    }
    override fun getSize(paint: android.graphics.Paint, text: CharSequence, start: Int, end: Int,
                         fm: android.graphics.Paint.FontMetricsInt?) = (paint.textSize * .45f).toInt()
    override fun draw(canvas: android.graphics.Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: android.graphics.Paint) {
        drawChevron(canvas, line, x, (top + bottom) / 2f, paint.textSize * .28f, direction, paint.color)
    }
}

internal fun drawChevron(canvas: android.graphics.Canvas, paint: android.graphics.Paint,
                         x: Float, centerY: Float, size: Float, direction: Int, color: Int) {
    if (direction == 0) return
    paint.color = color; paint.strokeWidth = (size / 6).coerceAtLeast(1f)
    val tip = centerY - direction * size / 4
    val ends = centerY + direction * size / 4
    canvas.drawLine(x, ends, x + size / 2, tip, paint)
    canvas.drawLine(x + size / 2, tip, x + size, ends, paint)
}

/** Same 0.7 m/s movement threshold and 10 s continuity limit as the live GPS estimator. */
internal class MovingTime {
    private var previousTime: Long? = null
    private var previousSpeed: Double? = null
    private var observed = false
    private var total = 0L
    var observedMs=0L;private set
    val value: Long? get() = total.takeIf { observed }
    fun add(time: Long?, speed: Double?) {
        val valid = speed?.takeIf { it.isFinite() && it in 0.0..35.0 }
        val dt = time?.let { now -> previousTime?.let { now - it } }
        if (dt != null && dt in 1..10_000 && valid != null && previousSpeed != null) {
            observed = true;observedMs+=dt
            if (valid >= .7) total += dt
        }
        previousTime = time; previousSpeed = valid
    }
}
