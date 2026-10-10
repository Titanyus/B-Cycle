package de.bcycle.app

import com.google.protobuf.CodedOutputStream
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class McspProtocolTest {
    private fun hex(value: String) = value.split(' ').filter { it.isNotEmpty() }.map { it.toInt(16).toByte() }.toByteArray()
    private fun proto(write: CodedOutputStream.() -> Unit): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = CodedOutputStream.newInstance(buffer)
        output.write(); output.flush()
        return buffer.toByteArray()
    }
    private fun segment(body: ByteArray, complete: Boolean) = byteArrayOf(
        ((if (complete) 0x30 else 0x20) or (body.size ushr 8)).toByte(), body.size.toByte(),
    ) + body

    @Test fun decodesPublishedRiderPowerFramesIncludingDefaultZero() {
        // Published RobbyPee examples: 79 W, 562 W, then an empty proto3 wrapper = 0 W.
        val protocol = McspProtocol()
        assertEquals(LiveData(riderPowerWatts = 79), protocol.receive(hex("30 04 98 5b 08 4f")).update)
        assertEquals(LiveData(riderPowerWatts = 562), protocol.receive(hex("30 05 98 5b 08 b2 04")).update)
        assertEquals(LiveData(riderPowerWatts = 0), protocol.receive(hex("30 02 98 5b")).update)
    }

    @Test fun walksPackedFramesWithoutMistakingPayload30ForAHeader() {
        val packet = hex("10 06 02 01 00 00 02 ff 30 04 98 2d 08 30 30 04 98 5b 08 64")
        val result = McspProtocol().receive(packet)
        assertEquals(LiveData(48, 100), result.update)
        assertEquals(1, result.controlFrames)
        assertEquals(2, result.messages)
        assertTrue(result.samples.all { it.isPush })
    }

    @Test fun handlesReadStatusWordWithoutTreatingItAsProtobuf() {
        val result = McspProtocol().receive(hex("30 08 18 5b c0 80 10 08 ac 02"))
        assertEquals(LiveData(riderPowerWatts = 300), result.update)
        assertEquals(1, result.readResponses)
        assertFalse(result.samples.single().isPush)
        val refused = McspProtocol().receive(hex("30 06 18 5b 40 80 10 06"))
        assertEquals(LiveData(), refused.update)
        assertTrue(refused.events.single().contains("status=6"))
    }

    @Test fun emptyAcknowledgementAndRepliesToFlowDoNotInventMeasurements() {
        val protocol = McspProtocol()
        assertEquals(LiveData(), protocol.receive(hex("30 05 18 5b c0 80 70")).update)
        assertEquals(LiveData(), protocol.receive(hex("30 05 18 5b c0 80 10")).update)
        val flowReply = protocol.receive(hex("30 07 18 5b c1 80 10 08 64"))
        assertEquals(LiveData(), flowReply.update)
        assertEquals(1, flowReply.ignoredMessages)
    }

    @Test fun readRequestsAndSubscriptionsCannotWriteBikeSettings() {
        val protocol = McspProtocol()
        assertArrayEquals(hex("30 05 40 80 98 2d 60"), protocol.subscriptions()[0].bytes)
        // Fresh protocol for the independent, documented READ grammar.
        assertArrayEquals(hex("30 05 40 80 98 2d 00"), McspProtocol().reads()[0].bytes)
        repeat(40) {
            for (request in protocol.reads() + protocol.subscriptions()) {
                val type = (request.bytes[6].toInt() and 255) ushr 4
                assertTrue(type == 0 || type == 6)
                assertEquals(7, request.bytes.size)
                assertTrue(request.bytes[5] == 0x2d.toByte() || request.bytes[5] == 0x5b.toByte())
            }
        }
    }

    @Test fun respondsToDocumentedStartupRequestsWithMatchingSequence() {
        val protocol = McspProtocol()
        val features = protocol.receive(hex("30 05 20 80 c0 aa 05"))
        assertArrayEquals(hex("30 07 40 aa a0 80 15 18 01"), features.replies.single().bytes)
        val stage = protocol.receive(hex("30 07 20 80 c0 a9 27 08 09"))
        assertArrayEquals(hex("30 05 40 a9 a0 80 37"), stage.replies.single().bytes)
        assertTrue(stage.events.contains("MCSP_STARTUP_STAGE value=9"))
        assertEquals(LiveData(), stage.update)
        assertTrue(protocol.receive(hex("30 07 20 80 c0 ab 27 08 09")).replies.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { protocol.receive(hex("30 07 20 80 c0 a9 27 08 63")) }
    }

    @Test fun reassemblesContinuationWhileSkippingUnrelatedChannels() {
        val scalar = proto { writeUInt32(1, 2430); writeByteArray(100, ByteArray(240)) }
        val body = hex("98 2d") + scalar
        val protocol = McspProtocol()
        assertEquals(LiveData(), protocol.receive(segment(body.copyOfRange(0, 242), false)).update)
        val continuation = hex("10 01 02") + segment(body.copyOfRange(242, body.size), true)
        assertEquals(LiveData(2430), protocol.receive(continuation).update)
    }

    @Test fun malformedPacketCannotPartiallyUpdateMeasurementsAndParserRecovers() {
        val protocol = McspProtocol()
        for (packet in listOf(
            hex("30"), hex("30 06 98 5b 08 01"),
            hex("30 04 98 5b 08 01 30"), hex("30 03 98 5b 08"),
            hex("30 04 98 5b 0a 00"),
        )) assertThrows(IllegalArgumentException::class.java) { protocol.receive(packet) }
        assertEquals(LiveData(riderPowerWatts = 100), protocol.receive(hex("30 04 98 5b 08 64")).update)
    }

    @Test fun reassemblyMemoryAndMeasurementRangesAreBounded() {
        val protocol = McspProtocol()
        repeat(16) { protocol.receive(segment(ByteArray(256), false)) }
        assertThrows(IllegalArgumentException::class.java) { protocol.receive(segment(byteArrayOf(1), false)) }
        assertThrows(IllegalArgumentException::class.java) { protocol.receive(ByteArray(515)) }
        val overflow = segment(hex("98 5b") + proto { writeUInt32(1, 65536) }, true)
        assertThrows(IllegalArgumentException::class.java) { protocol.receive(overflow) }
        assertEquals(LiveData(riderPowerWatts = 0), protocol.receive(hex("30 02 98 5b")).update)
    }

    @Test fun unknownPrivateFieldsAreDiscardedAndScalarDefaultIsPreserved() {
        val unknown = segment(hex("a0 81") + proto { writeString(1, "private bike serial") }, true)
        val protocol = McspProtocol()
        val result = protocol.receive(unknown)
        assertEquals(1, result.ignoredMessages)
        assertTrue(result.samples.isEmpty()); assertTrue(result.events.isEmpty()); assertTrue(result.replies.isEmpty())
        val zeroSpeed = segment(hex("98 2d") + proto { writeBool(2, true) }, true)
        assertEquals(LiveData(0), protocol.receive(zeroSpeed).update)
    }
}
