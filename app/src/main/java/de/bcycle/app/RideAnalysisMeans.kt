package de.bcycle.app

/** Read-only time-weighted ride summaries. Ratios use precisely the same paired intervals.
 * Missing readings and long time gaps never become zero or unlimited sample holds.
 */
internal class RideAnalysisMeans {
    private var previousTime: Long? = null
    private var previousRider: Double? = null; private var previousMotor: Double? = null
    private var riderIntegral = 0.0; private var riderMs = 0L
    private var motorIntegral = 0.0; private var motorMs = 0L
    private var pairedRider = 0.0; private var pairedMotor = 0.0; private var pairedMs = 0L
    private var firstTime: Long? = null; private var lastTime: Long? = null
    private var firstBattery: Pair<Long, Double>? = null; private var lastBattery: Pair<Long, Double>? = null
    private var recharged = false
    val riderMean get() = if (riderMs > 0) riderIntegral / riderMs else null
    val motorMean get() = if (motorMs > 0) motorIntegral / motorMs else null
    val pairedRatio get() = if (pairedMs > 0 && pairedMotor > 0) pairedRider / pairedMotor else null
    val batteryUsage: Double? get() {
        val first = firstBattery ?: return null; val last = lastBattery ?: return null
        if (recharged || first.first - (firstTime ?: return null) > 60_000 || (lastTime ?: return null) - last.first > 60_000 || last.first <= first.first) return null
        return (first.second - last.second).takeIf { it >= 0 }
    }
    fun add(time: Long?, rider: Double?, motor: Double?, battery: Double?) {
        if (time != null) { if (firstTime == null) firstTime = time; lastTime = time }
        val dt = time?.let { now -> previousTime?.let { now - it } }
        if (dt != null && dt in 1..PerformanceEngine.MAX_HOLD_MS) {
            previousRider?.let { riderIntegral += it * dt; riderMs += dt }
            previousMotor?.let { motorIntegral += it * dt; motorMs += dt }
            if (previousRider != null && previousMotor != null) {
                pairedRider += previousRider!! * dt; pairedMotor += previousMotor!! * dt; pairedMs += dt
            }
        }
        if (time != null && battery != null) {
            if (firstBattery == null) firstBattery = time to battery
            if (lastBattery?.let { battery > it.second } == true) recharged = true
            lastBattery = time to battery
        }
        previousTime = time; previousRider = rider; previousMotor = motor
    }
}
