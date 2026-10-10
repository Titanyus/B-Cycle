package de.bcycle.app

/** Bounded protobuf wire reader shared by the MCSP measurement types. */
internal class ProtoReader(private val bytes: ByteArray) {
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
    fun bytes(): ByteArray {
        val length = varint()
        require(length >= 0 && length <= bytes.size - position) { "Truncated protobuf bytes" }
        val result = bytes.copyOfRange(position, position + length.toInt())
        position += length.toInt()
        return result
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
