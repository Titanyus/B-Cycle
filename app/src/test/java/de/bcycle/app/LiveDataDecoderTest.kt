package de.bcycle.app

import com.google.protobuf.CodedOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class LiveDataDecoderTest {
    private fun message(write: CodedOutputStream.() -> Unit): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = CodedOutputStream.newInstance(buffer)
        output.write(); output.flush()
        return buffer.toByteArray()
    }

    @Test fun speedAndPowerDecodeInAnyOrder() {
        val packet = message { writeUInt32(5, 287); writeUInt32(1, 2534) }
        assertEquals(LiveData(2534, 287), LiveDataDecoder.decode(packet))
    }
    @Test fun missingFieldsAreDifferentFromExplicitZero() {
        assertEquals(LiveData(), LiveDataDecoder.decode(byteArrayOf()))
        assertEquals(LiveData(0, 0), LiveDataDecoder.decode(message { writeUInt32(1, 0); writeUInt32(5, 0) }))
        assertEquals(LiveData(2534, 0), LiveData(2534, 287).merge(LiveDataDecoder.decode(message { writeUInt32(5, 0) })))
    }
    @Test fun partialUpdatesPreserveOtherValues() {
        val initial = LiveDataDecoder.decode(message { writeUInt32(1, 2500); writeUInt32(5, 300) })
        assertEquals(LiveData(2500, 310), initial.merge(LiveDataDecoder.decode(message { writeUInt32(5, 310) })))
    }
    @Test fun allUnknownWireTypesAndSignedCadenceAreSkipped() {
        val packet = message {
            writeUInt32(1, 1423)
            writeInt32(2, -1)
            writeUInt64(100, -1L)
            writeFixed64(101, 1234)
            writeString(102, "future field")
            writeTag(103, 3); writeUInt32(1, 9999); writeTag(103, 4)
            writeFixed32(104, 1234)
            writeEnum(17, 999)
            writeUInt32(5, 180)
        }
        assertEquals(LiveData(1423, 180), LiveDataDecoder.decode(packet))
    }
    @Test fun duplicateFieldsUseLastValue() {
        assertEquals(LiveData(1700), LiveDataDecoder.decode(message { writeUInt32(1, 1600); writeUInt32(1, 1700) }))
    }
    @Test fun rangeLimitsAreChecked() {
        assertEquals(LiveData(65535, 65535), LiveDataDecoder.decode(message { writeUInt32(1, 65535); writeUInt32(5, 65535) }))
        assertThrows(IllegalArgumentException::class.java) { LiveDataDecoder.decode(message { writeUInt32(1, 65536) }) }
        assertThrows(IllegalArgumentException::class.java) { LiveDataDecoder.decode(message { writeUInt32(5, -1) }) }
    }
    @Test fun truncatedPacketCannotPartiallyOverwriteGoodValues() {
        val initial = LiveData(2500, 200)
        val packet = message { writeUInt32(1, 3500); writeUInt32(5, 450) }.dropLast(1).toByteArray()
        assertThrows(IllegalArgumentException::class.java) { initial.merge(LiveDataDecoder.decode(packet)) }
        assertEquals(LiveData(2500, 200), initial)
    }
    @Test fun malformedLengthsTagsGroupsAndVarintsAreRejected() {
        for (packet in listOf(
            byteArrayOf(0), byteArrayOf(8, -128), byteArrayOf(18, 10, 1),
            byteArrayOf(11, 20), byteArrayOf(11), byteArrayOf(12), byteArrayOf(15),
            byteArrayOf(8) + ByteArray(10) { -1 },
        )) {
            assertThrows(IllegalArgumentException::class.java) { LiveDataDecoder.decode(packet) }
        }
    }
    @Test fun fullMtuPayloadCanContainFutureFields() {
        val packet = message { writeUInt32(1, 100); writeByteArray(100, ByteArray(238)) }
        assertEquals(244, packet.size)
        assertEquals(LiveData(100), LiveDataDecoder.decode(packet))
        assertThrows(IllegalArgumentException::class.java) { LiveDataDecoder.decode(ByteArray(515)) }
    }
    @Test fun wrongWireTypeForKnownFieldIsSkippedLikeAnUnknownField() {
        assertEquals(LiveData(null, 200), LiveDataDecoder.decode(message { writeString(1, "future"); writeUInt32(5, 200) }))
    }
}
