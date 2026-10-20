package de.bcycle.hrv

enum class Contact { UNSUPPORTED, PRESENT, LOST }

/** Bluetooth SIG 0x2A37. Exact RR ticks (including zero) are retained for audit. */
data class HeartRatePacket(val bpm: Int?, val contact: Contact, val rrTicks: List<Int>) {
    companion object {
        fun decode(bytes: ByteArray): HeartRatePacket? {
            if (bytes.size < 2) return null
            val flags = bytes[0].toInt() and 255
            if (flags and 0xe0 != 0) return null
            var index = 1
            fun word(): Int? {
                if (index + 2 > bytes.size) return null
                return (bytes[index++].toInt() and 255) or ((bytes[index++].toInt() and 255) shl 8)
            }
            val hr = if (flags and 1 != 0) word() ?: return null else bytes[index++].toInt() and 255
            if (flags and 8 != 0 && word() == null) return null
            val rr = mutableListOf<Int>()
            if (flags and 16 != 0) {
                if ((bytes.size - index) % 2 != 0) return null
                while (index < bytes.size) rr += word() ?: return null
            }
            if (index != bytes.size) return null
            val contact = when {
                flags and 4 == 0 -> Contact.UNSUPPORTED
                flags and 2 != 0 -> Contact.PRESENT
                else -> Contact.LOST
            }
            return HeartRatePacket(hr.takeIf { contact != Contact.LOST && it in 1..300 }, contact, rr)
        }
    }
}
