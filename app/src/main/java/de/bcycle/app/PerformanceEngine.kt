package de.bcycle.app

import kotlin.math.*

/** Sources are identities, not claims about accuracy or about where a future meter is mounted. */
enum class PowerSource { BOSCH_RIDER, CYCLING_POWER_BLE, ANT_PLUS, PEDAL, FTMS }
enum class PerformanceChannel { POWER, HEART_RATE, SPEED, PAUSE, RESUME, BIKE_ACTIVITY }

/** All times use the ride's monotonic epoch. Null sensor time means receipt time, never invented timing. */
data class PerformanceEvent(
    val channel: PerformanceChannel, val atMs: Long, val value: Double? = null,
    val receivedAtMs: Long = atMs, val sensorTimeMs: Long? = null,
    val source: String = "", val connected: Boolean = true,
)

data class PowerMeasurement(
    val source: PowerSource, val watts: Double?, val receivedAtMs: Long,
    val sensorTimeMs: Long? = null, val connected: Boolean = true, val valid: Boolean = true,
    val precedingGapMs: Long? = null,
) {
    fun ageMs(now: Long) = now - (sensorTimeMs ?: receivedAtMs)
    fun usableAt(now: Long) = connected && valid && ageMs(now) in 0..PerformanceEngine.MAX_HOLD_MS
}

data class PerformanceQuality(
    val activeMs: Long, val powerMs: Long, val pairedSpeedMs: Long, val pairedHeartMs: Long,
    val longestPowerGapMs: Long, val powerGapCount: Long, val npWindows: Long, val heartAvailableMs: Long,
    val continuedZeroMs: Long = 0,
    val potentialNpWindows: Long = 0,
) {
    /** Coverage includes telemetry-supported explicit zero states; it is not sensor accuracy. */
    val powerCoverage get() = if (activeMs > 0) powerMs.toDouble() / activeMs else 0.0
    val npWindowCoverage get() = if(potentialNpWindows>0) npWindows.toDouble()/potentialNpWindows else 0.0
}

data class PerformanceValues(
    val normalizedPowerW: Double?, val mechanicalWorkKj: Double?, val caloriesKcal: Double?,
    val speedToPowerRatio: Double?, val efficiencyFactor: Double?,
    val knownMechanicalWorkKj: Double, val quality: PerformanceQuality,
    val caloriesModel: String = EnergyEngine.MODEL_ID,
    val estimatedFtpW: Double? = null,
)

/** Only bounded state: actual events -> time integral -> complete 1 s bins -> complete 30 s windows. */
class PerformanceEngine(private val energy: EnergyEngine = EnergyEngine(), powerDurationsMs: List<Long> = emptyList(), private val primaryPowerSource:PowerSource=PowerSource.BOSCH_RIDER,private val resolvedPower:Boolean=false) {
    companion object {
        const val MODEL_VERSION = 4
        const val MAX_HOLD_MS = 5_000L
        const val MIN_COVERAGE = .95
        const val MIN_NP_COVERAGE = .90
    }
    private val powerWindows = (powerDurationsMs + 20 * 60 * 1000L).distinct().associateWith { PowerDurationWindow(it) }
    internal fun bestPowerAverages(): Map<Long, Double> = powerWindows.mapNotNull { (duration, window) ->
        window.bestAverageW?.let { duration to it }
    }.toMap()
    private var cursor = 0L
    private var paused = false
    private val readings = mutableMapOf<PerformanceChannel, PerformanceEvent>()
    private var activeMs = 0L; private var powerMs = 0L; private var continuedZeroMs = 0L
    private var workJ = 0.0; private var speedMs = 0L; private var speedIntegral = 0.0; private var pairedWorkJ = 0.0
    private var heartMs = 0L; private var heartIntegral = 0.0
    private var heartAvailableMs = 0L; private var heartAvailableIntegral = 0.0
    private var gapMs = 0L; private var longestGap = 0L; private var gapCount = 0L
    private var binEnd = 1_000L; private var binPowerMs = 0L; private var binWorkJ = 0.0
    private val window = ArrayDeque<Double>(30)
    private var windowSum = 0.0; private var fourthMean = 0.0; private var windowCount = 0L
    private var segmentSeconds = 0L; private var potentialWindows = 0L
    var latestPower: PowerMeasurement? = null; private set
    private val measurements = mutableMapOf<PowerSource, PowerMeasurement>()
    val powerSources: Map<PowerSource, PowerMeasurement> get() = measurements.toMap()
    private fun selectedPower(event:PerformanceEvent)=event.source==primaryPowerSource.name ||
        (resolvedPower && (event.source==PowerSource.CYCLING_POWER_BLE.name || event.source==PowerSource.FTMS.name || event.source==PowerSource.BOSCH_RIDER.name))

    fun accept(event: PerformanceEvent) {
        require(event.atMs >= cursor) { "Performance events must be monotonic" }
        advance(event.atMs)
        if (event.channel == PerformanceChannel.POWER && !selectedPower(event)) {
            runCatching { PowerSource.valueOf(event.source) }.getOrNull()?.let { source ->
                measurements[source] = PowerMeasurement(source, event.value, event.receivedAtMs, event.sensorTimeMs,
                    event.connected, event.value?.let { it.isFinite() && it in 0.0..65535.0 } == true)
            }
            return // An independent comparison source must never invalidate or overwrite Bosch.
        }
        when (event.channel) {
            PerformanceChannel.PAUSE -> { paused = true; powerWindows.values.forEach { it.breakContinuity() }; readings.clear(); clearWindow(); segmentSeconds = 0; gapMs = 0; binPowerMs = 0; binWorkJ = 0.0 }
            PerformanceChannel.RESUME -> { paused = false; powerWindows.values.forEach { it.breakContinuity() }; readings.clear(); clearWindow(); segmentSeconds = 0; gapMs = 0; binEnd = cursor + 1_000; binPowerMs = 0; binWorkJ = 0.0 }
            else -> {
                val finite = event.value?.isFinite() == true
                val valid = finite && event.connected && event.receivedAtMs <= event.atMs &&
                    (event.sensorTimeMs == null || event.sensorTimeMs <= event.receivedAtMs) && when (event.channel) {
                        PerformanceChannel.POWER -> selectedPower(event) && event.value!! in 0.0..65535.0 &&
                            (event.value != 0.0 || observedDeadline(event) > event.atMs)
                        PerformanceChannel.HEART_RATE -> event.value!! in 1.0..255.0
                        PerformanceChannel.SPEED -> event.value!! in 0.0..35.0
                        PerformanceChannel.BIKE_ACTIVITY -> event.source == "SPEED" && event.value!! in 0.0..35.0
                        else -> false
                    }
                if (valid) readings[event.channel] = event else readings.remove(event.channel)
                if (event.channel == PerformanceChannel.POWER) { latestPower = PowerMeasurement(
                    runCatching { PowerSource.valueOf(event.source) }.getOrDefault(PowerSource.BOSCH_RIDER),
                    event.value, event.receivedAtMs, event.sensorTimeMs, event.connected, valid, gapMs.takeIf { it > 0 })
                    measurements[latestPower!!.source] = latestPower!!
                }
            }
        }
    }
    private fun observedDeadline(event: PerformanceEvent) = minOf(event.receivedAtMs,
        event.sensorTimeMs ?: event.receivedAtMs) + MAX_HOLD_MS
    private fun deadline(event: PerformanceEvent): Long {
        val observed = observedDeadline(event)
        if (event.source!=PowerSource.BOSCH_RIDER.name || event.channel != PerformanceChannel.POWER || event.value != 0.0) return observed
        // Bosch may stop repeating an unchanged zero. Only live bike telemetry can
        // maintain that explicit state; GPS alone never proves bike connectivity.
        val heartbeat = readings[PerformanceChannel.BIKE_ACTIVITY]
            ?: readings[PerformanceChannel.SPEED]?.takeIf { it.source == "SPEED" }
        val until = heartbeat?.let(::observedDeadline) ?: observed
        return if (until > cursor) maxOf(observed, until) else observed
    }
    private fun reading(channel: PerformanceChannel): PerformanceEvent? {
        val event = readings[channel] ?: return null
        if (cursor < deadline(event)) return event
        // Once continuity is lost, a later heartbeat cannot revive an old zero.
        readings.remove(channel)
        return null
    }

    fun advance(untilMs: Long) {
        require(untilMs >= cursor) { "Performance clock must be monotonic" }
        if (paused) { cursor = untilMs; return }
        while (cursor < untilMs) {
            val power = reading(PerformanceChannel.POWER)
            val speed = reading(PerformanceChannel.SPEED)
            val heart = reading(PerformanceChannel.HEART_RATE)
            // Long empty stretches take constant time rather than iterating over hours of missing bins.
            if (power == null) {
                powerWindows.values.forEach { it.breakContinuity() }
                var end = untilMs
                speed?.let { end = minOf(end, deadline(it)) }; heart?.let { end = minOf(end, deadline(it)) }
                val dt = end - cursor
                if (heart != null) { heartAvailableMs += dt; heartAvailableIntegral += heart.value!! * dt }
                activeMs += dt; if (gapMs == 0L) gapCount++
                gapMs += dt; longestGap = maxOf(longestGap, gapMs)
                clearWindow(); binPowerMs = 0; binWorkJ = 0.0
                val crossed = if (end >= binEnd) (end - binEnd) / 1000 + 1 else 0
                potentialWindows += (segmentSeconds + crossed - 29).coerceAtLeast(0) - (segmentSeconds - 29).coerceAtLeast(0)
                segmentSeconds += crossed; binEnd += crossed * 1000
                cursor = end
                continue
            }
            var end = minOf(untilMs, binEnd, deadline(power))
            if (power.value == 0.0 && cursor < observedDeadline(power)) end = minOf(end, observedDeadline(power))
            speed?.let { end = minOf(end, deadline(it)) }; heart?.let { end = minOf(end, deadline(it)) }
            val dt = end - cursor
            check(dt > 0)
            powerWindows.values.forEach { it.add(cursor, end, power.value!!) }
            val joules = power.value!! * dt / 1000
            activeMs += dt; powerMs += dt; workJ += joules
            if (power.value == 0.0 && cursor >= observedDeadline(power)) continuedZeroMs += dt
            binPowerMs += dt; binWorkJ += joules; gapMs = 0
            if (speed != null) { speedMs += dt; speedIntegral += speed.value!! * dt; pairedWorkJ += joules }
            if (heart != null) { heartMs += dt; heartIntegral += heart.value!! * dt
                heartAvailableMs += dt; heartAvailableIntegral += heart.value!! * dt }
            cursor = end
            if (cursor == binEnd) {
                segmentSeconds++; if (segmentSeconds >= 30) potentialWindows++
                if (binPowerMs == 1000L) {
                    val mean = binWorkJ // Joules in exactly one second = mean watts.
                    window.addLast(mean); windowSum += mean
                    if (window.size > 30) windowSum -= window.removeFirst()
                    if (window.size == 30) {
                        val fourth = (windowSum / 30).pow(4)
                        windowCount++; fourthMean += (fourth - fourthMean) / windowCount
                    }
                } else clearWindow()
                binEnd += 1000; binPowerMs = 0; binWorkJ = 0.0
            }
        }
    }
    private fun clearWindow() { window.clear(); windowSum = 0.0 }
    /** Read-only mean of the already integrated, resolved recording power. */
    internal val observedAveragePowerW get()=if(powerMs>0) workJ*1000/powerMs else null
    fun snapshot(): PerformanceValues {
        val quality = PerformanceQuality(activeMs, powerMs, speedMs, heartMs, longestGap, gapCount, windowCount, heartAvailableMs, continuedZeroMs,potentialWindows)
        // A local gap breaks its 30 s windows and FTP continuity, not an otherwise
        // well-covered ride. Coverage and complete-window coverage remain required.
        val enough = activeMs > 0 && quality.powerCoverage >= MIN_COVERAGE
        val np = if (activeMs > 0 && quality.powerCoverage >= MIN_NP_COVERAGE && windowCount > 0 && potentialWindows > 0 && windowCount.toDouble() / potentialWindows >= MIN_NP_COVERAGE)
            fourthMean.coerceAtLeast(0.0).pow(.25).takeIf { it.isFinite() } else null
        // Work is the observed integral, not a rescaled estimate for unobserved seconds.
        val work = (workJ / 1000).takeIf { enough && it.isFinite() }
        val ratio = if (enough && speedMs.toDouble() / activeMs >= MIN_COVERAGE && pairedWorkJ > 0)
            (speedIntegral / speedMs * 3.6) / (pairedWorkJ * 1000 / speedMs) else null
        // EF uses the same observed power intervals, never unrelated HR-only time.
        val ef = if (np != null && powerMs > 0 && heartMs == powerMs && heartIntegral > 0)
            np / (heartIntegral / heartMs) else null
        val estimate = energy.estimate(EnergyInput(work, heartAvailableIntegral.takeIf { heartAvailableMs > 0 }?.div(heartAvailableMs),
                activeMs, quality.powerCoverage, if (activeMs > 0) heartAvailableMs.toDouble() / activeMs else 0.0))
        return PerformanceValues(np, work, estimate?.kcal, ratio?.takeIf { it.isFinite() },
            ef?.takeIf { it.isFinite() }, workJ / 1000, quality, estimate?.modelId ?: EnergyEngine.MODEL_ID, powerWindows.getValue(20 * 60 * 1000L).bestAverageW?.times(.95))
    }
}

/** Prepared parameters are optional and are not collected/stored until a model actually needs them. */
data class EnergyProfile(val weightKg: Double? = null, val ageYears: Int? = null, val heightCm: Double? = null,
                         val restingHeartRate: Int? = null, val maximumHeartRate: Int? = null,
                         val ftpW: Double? = null, val calibration: Double? = null)
data class EnergyInput(val mechanicalWorkKj: Double?, val averageHeartRateBpm: Double?, val activeMs: Long,
                       val powerCoverage: Double, val heartCoverage: Double)
data class EnergyEstimate(val kcal: Double, val modelId: String, val grossEfficiency: Double?)
interface EnergyModel {
    val id: String
    fun estimate(input: EnergyInput, profile: EnergyProfile): EnergyEstimate?
}

/** No HR/movement fallback is activated without a validated model and its required personal inputs. */
class PowerEnergyModel(private val grossEfficiency: Double = .24) : EnergyModel {
    init { require(grossEfficiency.isFinite() && grossEfficiency in 0.18..0.25) }
    override val id = "power-gross-efficiency-${grossEfficiency}-v1"
    override fun estimate(input: EnergyInput, profile: EnergyProfile): EnergyEstimate? {
        val work = input.mechanicalWorkKj?.takeIf { it.isFinite() && it >= 0 } ?: return null
        if (input.powerCoverage < PerformanceEngine.MIN_COVERAGE) return null
        return EnergyEstimate(work / (4.184 * grossEfficiency), id, grossEfficiency)
    }
}
class EnergyEngine(val profile: EnergyProfile = EnergyProfile(), private val models: List<EnergyModel> = listOf(PowerEnergyModel())) {
    companion object { const val MODEL_ID = "power-gross-efficiency-0.24-v1" }
    /** Exactly one model supplies the result. Never sum power and HR estimates. */
    fun estimate(input: EnergyInput): EnergyEstimate? = models.firstNotNullOfOrNull { model ->
        model.estimate(input, profile)?.takeIf { it.kcal.isFinite() && it.kcal >= 0 }
    }
}
