package de.bcycle.app

/** Null denotes an absent field, which is different from an explicit zero. */
data class LiveData(val speedHundredths: Int? = null, val riderPowerWatts: Int? = null) {
    fun merge(update: LiveData) = LiveData(
        update.speedHundredths ?: speedHundredths,
        update.riderPowerWatts ?: riderPowerWatts,
    )
}

/** Bounded proto3 wire reader for Bosch LDI 1.0, fields 1 and 5. */
object LiveDataDecoder {
    fun decode(bytes: ByteArray): LiveData {
        require(bytes.size <= 514) { "Payload exceeds the maximum BLE ATT notification size" }
        val reader = Reader(bytes)
        var speed: Int? = null
        var power: Int? = null
        while (!reader.finished()) {
            val tag = reader.tag()
            val field = tag ushr 3
            val wire = tag and 7
            if ((field == 1 || field == 5) && wire == 0) {
                val value = reader.varint()
                require(value in 0..65535) { "Live data outside the documented range" }
                if (field == 1) speed = value.toInt() else power = value.toInt()
            } else {
                reader.skip(field, wire, 0)
            }
        }
        return LiveData(speed, power)
    }

    private class Reader(private val bytes: ByteArray) {
        private var position = 0
        fun finished() = position == bytes.size
        fun varint(): Long {
            var value = 0L
            for (index in 0..9) {
                require(position < bytes.size) { "Truncated protobuf varint" }
                val byte = bytes[position++].toInt() and 255
                require(index != 9 || byte <= 1) { "Protobuf varint overflow" }
                value = value or ((byte and 127).toLong() shl (index * 7))
                if (byte and 128 == 0) return value
            }
            throw IllegalArgumentException("Invalid protobuf varint")
        }
        fun tag(): Int {
            val tag = varint()
            require(tag in 8..0xffffffffL && (tag ushr 3) <= 0x1fffffff) { "Invalid protobuf tag" }
            return tag.toInt()
        }
        private fun advance(count: Long) {
            require(count >= 0 && count <= bytes.size - position) { "Truncated protobuf field" }
            position += count.toInt()
        }
        fun skip(field: Int, wire: Int, depth: Int) {
            when (wire) {
                0 -> varint()
                1 -> advance(8)
                2 -> advance(varint())
                3 -> {
                    require(depth < 64) { "Protobuf groups nested too deeply" }
                    while (true) {
                        require(!finished()) { "Unterminated protobuf group" }
                        val next = tag()
                        if (next and 7 == 4) {
                            require(next ushr 3 == field) { "Mismatched protobuf group" }
                            break
                        }
                        skip(next ushr 3, next and 7, depth + 1)
                    }
                }
                5 -> advance(4)
                else -> throw IllegalArgumentException("Invalid protobuf wire type")
            }
        }
    }
}
