package de.bcycle.app

import java.util.ArrayDeque

/** Bounded MCSP diagnostics; only explicit SAF export writes a file. */
class DiagnosticLog(
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 },
    private val maxEventChars: Int = 32_000,
    private val maxPackets: Int = 24,
) {
    private val events = ArrayDeque<String>()
    private val samples = ArrayDeque<String>()
    private val powerSamples = ArrayDeque<String>()
    private val stats = linkedMapOf<Int, MeasurementStats>()
    private var eventChars = 0
    private var started = clockMillis()
    private var metadata = ""
    private var reads = 0
    private var notifications = 0
    private var decodeErrors = 0
    private var bytes = 0L
    private var messages = 0
    private var ignored = 0
    private var controls = 0

    init { require(maxEventChars >= 128 && maxPackets > 0) }

    fun reset(header: String) {
        events.clear(); samples.clear(); powerSamples.clear(); stats.clear(); eventChars = 0
        reads = 0; notifications = 0; decodeErrors = 0; bytes = 0
        messages = 0; ignored = 0; controls = 0
        metadata = header.take(2048)
        started = clockMillis()
        event("Diagnostic session started")
    }

    fun event(message: String) {
        val line = "${elapsed()}ms ${message.take(2048)}\n"
        events.addLast(line)
        eventChars += line.length
        while (eventChars > maxEventChars && events.isNotEmpty()) eventChars -= events.removeFirst().length
    }

    fun mcspNotification(byteCount: Int) {
        notifications++; bytes += byteCount
    }

    fun mcspOutcome(outcome: McspOutcome) {
        messages += outcome.messages; ignored += outcome.ignoredMessages; controls += outcome.controlFrames
        reads += outcome.readResponses
        for (sample in outcome.samples) {
            // Whitelist again at the export boundary; never retain arbitrary mixed MCSP payloads.
            if (LiveMetric.forAddress(sample.address) == null) continue
            stats.getOrPut(sample.address) { MeasurementStats() }.add(sample.value, elapsed())
            val line = "${elapsed()}ms mcsp address=0x${sample.address.toString(16)} raw=${sample.value} source=${if (sample.isPush) "push" else "reply"}\n"
            remember(samples, line)
            if (sample.address == LiveMetric.RIDER_POWER.address) remember(powerSamples, line)
        }
    }

    private fun remember(history: ArrayDeque<String>, line: String) {
        history.addLast(line)
        while (history.size > maxPackets) history.removeFirst()
    }

    fun decodeFailed(reason: String) { decodeErrors++; event("DECODE_ERROR $reason") }

    fun report() = buildString {
        append("B-Cycle BLE diagnostic report\n$metadata\n")
        append("Counters: reads=$reads notifications=$notifications decodeErrors=$decodeErrors\n")
        append("MCSP: bytes=$bytes messages=$messages ignored=$ignored controlFrames=$controls\n")
        append("Known numeric fields: statistics cover the whole session; receipt times are relative milliseconds.\n")
        for (metric in LiveMetric.entries) {
            append("${metric.title} address=0x${metric.address.toString(16)} ${stats[metric.address]?.report() ?: "count=0"}\n")
        }
        stats[0x1808]?.let { append("Alternate speed address=0x1808 ${it.report()}\n") }
        append("No names, Bluetooth addresses, raw MCSP packets, mode-name strings or component identifiers are logged.\n")
        append("Events (bounded history):\n")
        for (line in events) append(line)
        append("Filtered numeric samples (last $maxPackets):\n")
        for (line in samples) append(line)
        append("Independent rider power history (last $maxPackets):\n")
        for (line in powerSamples) append(line)
    }

    private class MeasurementStats {
        private var count = 0L
        private var nonzero = 0L
        private var minimum: Long? = null
        private var maximum: Long? = null
        private var firstAt: Long? = null
        private var lastAt: Long? = null
        private var last: Long? = null
        fun add(value: Long, at: Long) {
            count++
            if (value != 0L) nonzero++
            minimum = minOf(minimum ?: value, value); maximum = maxOf(maximum ?: value, value)
            if (firstAt == null) firstAt = at
            lastAt = at; last = value
        }
        fun report() = "count=$count nonzero=$nonzero min=$minimum max=$maximum last=$last firstAtMs=$firstAt lastAtMs=$lastAt"
    }

    private fun elapsed() = clockMillis() - started
}
