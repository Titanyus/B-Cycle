package de.bcycle.app

import java.util.ArrayDeque

/** Bounded, in-memory diagnostics. Only explicit SAF export writes a file. */
class DiagnosticLog(
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 },
    private val maxEventChars: Int = 32_000,
    private val maxPackets: Int = 24,
) {
    private val events = ArrayDeque<String>()
    private val packets = ArrayDeque<String>()
    private val initialReads = ArrayDeque<String>()
    private var eventChars = 0
    private var started = clockMillis()
    private var metadata = ""
    private var reads = 0
    private var notifications = 0
    private var decodeErrors = 0
    private var speedSeen = false
    private var powerSeen = false

    init { require(maxEventChars >= 128 && maxPackets > 0) }

    fun reset(header: String) {
        events.clear(); packets.clear(); initialReads.clear(); eventChars = 0
        reads = 0; notifications = 0; decodeErrors = 0
        speedSeen = false; powerSeen = false
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

    fun packet(kind: String, payload: ByteArray) {
        if (kind == "read") reads++ else notifications++
        val hex = buildString {
            for (index in 0 until minOf(payload.size, 514)) {
                val byte = payload[index].toInt() and 255
                append(HEX[byte ushr 4]); append(HEX[byte and 15])
            }
        }
        val detail = "$kind bytes=${payload.size} hex=$hex${if (payload.size > 514) " [truncated in log]" else ""}"
        // Initial reads and high-rate notifications cannot evict the handshake events.
        val destination = if (kind == "read") initialReads else packets
        destination.addLast("${elapsed()}ms $detail\n")
        val limit = if (kind == "read") 4 else maxPackets
        while (destination.size > limit) destination.removeFirst()
    }

    fun decoded(value: LiveData) {
        speedSeen = speedSeen || value.speedHundredths != null
        powerSeen = powerSeen || value.riderPowerWatts != null
    }

    fun decodeFailed(reason: String) {
        decodeErrors++
        event("DECODE_ERROR $reason")
    }

    fun report() = buildString {
        append("B-Cycle BLE diagnostic report\n")
        append(metadata); append('\n')
        append("Counters: reads=$reads notifications=$notifications decodeErrors=$decodeErrors ")
        append("speedFieldSeen=$speedSeen riderPowerFieldSeen=$powerSeen\n")
        append("No device names or Bluetooth addresses are logged. Raw payloads are limited to Bosch LDI.\n")
        append("Events (bounded history):\n")
        for (line in events) append(line)
        append("Bosch LDI initial reads (last 4):\n")
        for (line in initialReads) append(line)
        append("Bosch LDI notifications (last $maxPackets):\n")
        for (line in packets) append(line)
    }

    private fun elapsed() = clockMillis() - started
    private companion object { const val HEX = "0123456789abcdef" }
}
