package de.bcycle.hrv

/** Protocol is injectable for future integration, deliberately fixed in the test-app UI. */
data class Protocol(val restMs: Long = 30_000, val measureMs: Long = 180_000) {
    init { require(restMs >= 0 && measureMs >= 60_000) }
}
enum class Phase { REST, MEASURE, COMPLETE, ABORTED }
data class CapturedPacket(val elapsedMs: Long, val bytes: ByteArray, val decoded: HeartRatePacket?, val selected: Boolean)

/** A foreground-only session. No inferred RR, no silent reconnect or gap bridging. */
class MeasurementSession(val startMs: Long, val protocol: Protocol = Protocol()) {
    var phase = Phase.REST; private set
    var problem: String? = null; private set
    val packets = mutableListOf<CapturedPacket>()
    val rrTicks = mutableListOf<Int>()
    val measureStartMs = startMs + protocol.restMs
    val deadlineMs = measureStartMs + protocol.measureMs
    var lastRrMs = startMs; private set
    private var anchored = false
    private var lastPacketMs = startMs

    fun advance(nowMs: Long) {
        if (phase in setOf(Phase.ABORTED, Phase.COMPLETE)) return
        if (nowMs - lastRrMs > 5_000) { abort("RR_GAP"); return }
        if (nowMs >= deadlineMs) phase = Phase.COMPLETE
        else if (nowMs >= measureStartMs) phase = Phase.MEASURE
    }

    fun accept(atMs: Long, bytes: ByteArray) {
        if (phase in setOf(Phase.ABORTED, Phase.COMPLETE)) return
        val decoded = HeartRatePacket.decode(bytes)
        var select = false
        if (atMs < deadlineMs) {
            when {
                atMs < lastPacketMs -> abort("CLOCK_ORDER")
                decoded == null -> abort("MALFORMED_PACKET")
                decoded.contact == Contact.LOST -> abort("CONTACT_LOST")
                decoded.rrTicks.any { it == 0 } -> abort("ZERO_RR")
                decoded.rrTicks.isNotEmpty() -> {
                    if (atMs - lastRrMs > 5_000) abort("RR_GAP")
                    lastRrMs = atMs
                    if (phase != Phase.ABORTED && atMs >= measureStartMs) {
                        if (anchored) { rrTicks += decoded.rrTicks; select = true }
                        else anchored = true // first packet after boundary is an uncertain pre-window anchor
                    }
                }
            }
            lastPacketMs = atMs
        }
        if (packets.size >= 4096 || rrTicks.size > 4096) abort("CAPTURE_LIMIT")
        else packets += CapturedPacket(atMs - startMs, bytes.copyOf(), decoded, select)
        advance(atMs)
    }

    /** Counts only analyzed batches; wall time is independent of cumulative RR time.
     * Live coverage is provisional because delivery/anchor boundaries can shift it.
     */
    fun diagnostics(nowMs: Long): CaptureDiagnostics {
        val selected = packets.filter { it.selected }
        val wallMs = (nowMs - measureStartMs).coerceIn(0, protocol.measureMs)
        val durationMs = rrTicks.sumOf { it.toLong() } * 1000.0 / 1024
        return CaptureDiagnostics(wallMs, durationMs, rrTicks.size, selected.size, packets.size,
            selected.maxOfOrNull { it.decoded?.rrTicks?.size ?: 0 } ?: 0,
            selected.firstOrNull()?.let { it.elapsedMs - protocol.restMs },
            selected.lastOrNull()?.let { it.elapsedMs - protocol.restMs })
    }

    /** Recovery can invalidate an in-memory completion that was never committed to disk. */
    fun abort(reason: String, discardUncommittedCompletion: Boolean = false) {
        if ((phase == Phase.COMPLETE && !discardUncommittedCompletion) || phase == Phase.ABORTED) return
        problem = reason; phase = Phase.ABORTED
    }
    fun remainingMs(now: Long): Long = when (phase) {
        Phase.REST -> (measureStartMs - now).coerceAtLeast(0)
        Phase.MEASURE -> (deadlineMs - now).coerceAtLeast(0)
        else -> 0
    }
}

data class CaptureDiagnostics(
    val measurementElapsedMs: Long,
    val rrDurationMs: Double,
    val rrCount: Int,
    val selectedPacketCount: Int,
    val sessionPacketCount: Int,
    val maxRrPerSelectedPacket: Int,
    val firstSelectedAtMs: Long?,
    val lastSelectedAtMs: Long?,
) {
    val coverage get() = if (measurementElapsedMs > 0) rrDurationMs / measurementElapsedMs else 0.0
}

data class Evaluation(val raw: Metrics?, val corrected: Metrics?, val correction: ArtifactCorrection.Result?,
                      val reasons: List<String>, val coverage: Double, val affectedFraction: Double,
                      val warnings: List<String> = emptyList()) {
    val acceptable get() = reasons.isEmpty()
}

object Evaluate {
    fun session(s: MeasurementSession): Evaluation {
        val reasons = mutableListOf<String>()
        if (s.phase != Phase.COMPLETE) reasons += s.problem ?: "INCOMPLETE"
        val durationMs = s.rrTicks.sumOf { it.toLong() } * 1000.0 / 1024
        val coverage = durationMs / s.protocol.measureMs
        if (coverage > 1.02) reasons += "RR_DURATION"
        val warnings = if (coverage < 0.95) listOf("LOW_RR_COVERAGE") else emptyList()
        if (durationMs < 60_000) reasons += "SHORT_SERIES"
        if (s.rrTicks.size < 3 || s.rrTicks.any { it !in 1..65535 })
            return Evaluation(null, null, null, (reasons + "INVALID_RR").distinct(), coverage, 0.0, warnings)
        // Interrupted captures have no aggregate result: never join known gaps.
        if (s.phase != Phase.COMPLETE) return Evaluation(null, null, null, reasons, coverage, 0.0, warnings)
        val raw = Metrics.fromNnMs(s.rrTicks.map { it * 1000.0 / 1024 })
        val result = try { ArtifactCorrection.correct(s.rrTicks) } catch (_: IllegalArgumentException) {
            return Evaluation(raw, null, null, reasons + "CORRECTION_FAILED", coverage, 0.0, warnings)
        } catch (_: IllegalStateException) {
            return Evaluation(raw, null, null, reasons + "CORRECTION_FAILED", coverage, 0.0, warnings)
        }
        val affected = result.affectedRawIndices.size.toDouble() / s.rrTicks.size
        if (affected > 0.05) reasons += "HIGH_CORRECTION"
        if (result.residual.count > 0) reasons += "RESIDUAL_ARTIFACTS"
        val corrected = Metrics.fromNnMs(result.nnMs)
        if (corrected.lnRmssd == null) reasons += "ZERO_VARIABILITY"
        return Evaluation(raw, corrected, result, reasons.distinct(), coverage, affected, warnings)
    }
}
