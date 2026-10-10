package de.bcycle.app

/** Read-only MCSP subset. Protocol references and attribution: docs/bosch-mcsp.md. */
data class McspTransmission(val description: String, val bytes: ByteArray)
data class McspSample(val address: Int, val value: Int, val isPush: Boolean)
data class McspOutcome(
    val update: LiveData,
    val samples: List<McspSample>,
    val replies: List<McspTransmission>,
    val events: List<String>,
    val messages: Int,
    val ignoredMessages: Int,
    val controlFrames: Int,
    val readResponses: Int,
)

class McspProtocol {
    private var partial = byteArrayOf()
    private var sequence = 0

    fun subscriptions() = requests(6)
    fun reads() = requests(0)

    private fun requests(type: Int) = listOf(SPEED, POWER).map { address ->
        val seq = sequence
        sequence = (sequence + 1) and 15
        val description = "${if (type == 6) "SUBSCRIBE" else "READ"} address=${hex(address)} seq=$seq"
        McspTransmission(description, frame(byteArrayOf(
            0x40, 0x80.toByte(), ((address ushr 8) or 0x80).toByte(), address.toByte(),
            ((type shl 4) or seq).toByte(),
        )))
    }

    /** Nothing outside the two measurements and two local startup fields is retained. */
    fun receive(packet: ByteArray): McspOutcome {
        try {
            require(packet.size <= 514) { "MCSP packet exceeds BLE notification limit" }
            // Validate the whole notification before changing reassembly state.
            val segments = mutableListOf<Segment>()
            var offset = 0
            while (offset < packet.size) {
                require(packet.size - offset >= 2) { "Truncated MCSP header" }
                val header = packet[offset++].toInt() and 255
                val length = ((header and 15) shl 8) or (packet[offset++].toInt() and 255)
                require(length <= packet.size - offset) { "Truncated MCSP segment" }
                segments += Segment(header ushr 5, header and 16 != 0, packet.copyOfRange(offset, offset + length))
                offset += length
            }
            val bodies = mutableListOf<ByteArray>()
            var controls = 0
            var pending = partial
            for (segment in segments) {
                if (segment.channel != 1) {
                    if (segment.channel == 0) controls++
                    continue
                }
                require(pending.size + segment.body.size <= 4096) { "MCSP message exceeds reassembly limit" }
                pending += segment.body
                if (segment.complete) {
                    bodies += pending
                    pending = byteArrayOf()
                }
            }
            var update = LiveData()
            val samples = mutableListOf<McspSample>()
            val replies = mutableListOf<McspTransmission>()
            val events = mutableListOf<String>()
            var ignored = 0
            var readResponses = 0
            for (body in bodies) {
                require(body.size >= 2) { "MCSP message has no source address" }
                val source = address(body, 0)
                val field = source and 0x7fff
                var value: Int? = null
                if (source and 0x8000 != 0) {
                    if (isMeasurement(field)) {
                        // A zero-length push is a proto3 scalar wrapper at its default, zero.
                        value = scalar(body.copyOfRange(2, body.size)) ?: 0
                    } else ignored++
                } else {
                    require(body.size >= 5) { "Truncated MCSP envelope" }
                    val destination = address(body, 2)
                    val type = (body[4].toInt() and 255) ushr 4
                    val seq = body[4].toInt() and 15
                    val payload = body.copyOfRange(5, body.size)
                    when {
                        destination and 0x7fff == FEATURES && type == 0 -> {
                            replies += startupReply(source, FEATURES, 1, seq, byteArrayOf(0x18, 0x01))
                            events += "MCSP_STARTUP_FEATURES stagedStartup=true"
                        }
                        destination and 0x7fff == STAGE && type == 2 -> {
                            val stage = scalar(payload) ?: 0
                            require(stage in 0..10) { "Invalid MCSP startup stage" }
                            replies += startupReply(source, STAGE, 3, seq, byteArrayOf())
                            events += "MCSP_STARTUP_STAGE value=$stage"
                        }
                        destination and 0x7fff == HOST && isMeasurement(field) && (type == 1 || type == 7) -> {
                            if (type == 1) readResponses++
                            if (destination and 0x8000 == 0) {
                                val status = payload.firstOrNull()?.toInt()?.and(255)
                                events += "MCSP_REFUSED address=${hex(field)} type=$type seq=$seq status=$status"
                            } else {
                                events += "MCSP_RESPONSE address=${hex(field)} type=$type seq=$seq payloadBytes=${payload.size}"
                                // An empty reply can acknowledge a subscription without a value.
                                if (payload.isNotEmpty()) value = scalar(payload) ?: 0
                            }
                        }
                        else -> ignored++
                    }
                }
                if (value != null) {
                    samples += McspSample(field, value, source and 0x8000 != 0)
                    update = update.merge(if (field == POWER) LiveData(riderPowerWatts = value) else LiveData(speedHundredths = value))
                }
            }
            partial = pending
            return McspOutcome(update, samples, replies, events, bodies.size, ignored, controls, readResponses)
        } catch (error: IllegalArgumentException) {
            // Never join the next notification onto a damaged partial message.
            partial = byteArrayOf()
            throw error
        }
    }

    private fun startupReply(requester: Int, field: Int, type: Int, seq: Int, payload: ByteArray): McspTransmission {
        val body = byteArrayOf(
            (field ushr 8).toByte(), field.toByte(), ((requester ushr 8) or 0x80).toByte(),
            requester.toByte(), ((type shl 4) or seq).toByte(),
        ) + payload
        return McspTransmission("STARTUP_REPLY field=${hex(field)} type=$type seq=$seq", frame(body))
    }

    private fun scalar(payload: ByteArray): Int? {
        val reader = LiveDataDecoder.Reader(payload)
        var result: Int? = null
        while (!reader.finished()) {
            val tag = reader.tag()
            if (tag ushr 3 == 1) {
                require(tag == 8) { "MCSP scalar has invalid wire type" }
                val value = reader.varint()
                require(value in 0..65535) { "MCSP measurement outside uint16 range" }
                result = value.toInt()
            } else reader.skip(tag ushr 3, tag and 7, 0)
        }
        return result
    }

    private data class Segment(val channel: Int, val complete: Boolean, val body: ByteArray)
    private companion object {
        const val HOST = 0x4080
        const val SPEED = 0x182d
        const val POWER = 0x185b
        const val FEATURES = 0x40aa
        const val STAGE = 0x40a9
        fun isMeasurement(field: Int) = field == SPEED || field == 0x1808 || field == POWER
        fun address(bytes: ByteArray, offset: Int) = ((bytes[offset].toInt() and 255) shl 8) or (bytes[offset + 1].toInt() and 255)
        fun hex(value: Int) = "0x${value.toString(16).padStart(4, '0')}"
        fun frame(body: ByteArray) = byteArrayOf((0x30 or (body.size ushr 8)).toByte(), body.size.toByte()) + body
    }
}
