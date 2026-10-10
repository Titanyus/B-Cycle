package de.bcycle.app

/** Bluetooth SIG Heart Rate Measurement (0x2A37), not a manufacturer-specific format. */
data class HeartRateMeasurement(val bpm: Int?, val energyKj: Int?, val rrSeconds: List<Double>) {
    companion object {
        fun decode(bytes: ByteArray): HeartRateMeasurement? {
            if (bytes.size < 2) return null
            val flags = bytes[0].toInt() and 255
            if (flags and 0xe0 != 0) return null
            var index = 1
            fun word(): Int? {
                if (index + 2 > bytes.size) return null
                return ((bytes[index++].toInt() and 255) or ((bytes[index++].toInt() and 255) shl 8))
            }
            val raw = if (flags and 1 != 0) word() ?: return null else bytes[index++].toInt() and 255
            val energy = if (flags and 8 != 0) word() ?: return null else null
            val rr = mutableListOf<Double>()
            if (flags and 16 != 0) {
                if ((bytes.size - index) % 2 != 0) return null
                while (index < bytes.size) { val value = word() ?: return null; if (value > 0) rr += value / 1024.0 }
            }
            if (index != bytes.size) return null
            val contact = flags and 4 == 0 || flags and 2 != 0
            return HeartRateMeasurement(raw.takeIf { contact && it in 1..300 }, energy, rr)
        }
    }
}
