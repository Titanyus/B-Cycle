package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class DiagnosticLogTest {
    private fun power(value: Byte) = byteArrayOf(0x30, 4, 0x98.toByte(), 0x5b, 8, value)

    @Test fun reportsKnownNumericFieldsAndCountsDiscardedPrivateMessagesWithoutRetainingThem() {
        val log = DiagnosticLog({ 0 })
        val protocol = McspProtocol()
        val private = "private bike serial".toByteArray()
        val unknown = byteArrayOf(0x30, (private.size + 4).toByte(), 0xa0.toByte(), 0x81.toByte(), 0x0a, private.size.toByte()) + private
        for (packet in listOf(unknown, power(100))) {
            log.mcspNotification(packet.size); log.mcspOutcome(protocol.receive(packet))
        }
        val report = log.report()
        assertTrue(report.contains("notifications=2"))
        assertTrue(report.contains("messages=2 ignored=1"))
        assertTrue(report.contains("address=0x185b raw=100 source=push"))
        assertFalse(report.contains("private bike serial"))
        assertFalse(report.contains("70726976617465"))
    }

    @Test fun speedTrafficCannotEvictPowerHistoryOrChangeItsReceiptTimes() {
        var now = 1000L
        val log = DiagnosticLog({ now }, maxPackets = 2)
        val protocol = McspProtocol()
        log.reset("power regression")
        now += 100; log.mcspOutcome(protocol.receive(power(79)))
        now += 200; log.mcspOutcome(protocol.receive(byteArrayOf(0x30, 2, 0x98.toByte(), 0x5b)))
        now += 10_000
        repeat(100) { log.mcspOutcome(protocol.receive(byteArrayOf(0x30, 4, 0x98.toByte(), 0x2d, 8, 100))) }
        val report = log.report()
        assertTrue(report.contains("Fahrerleistung address=0x185b count=2 nonzero=1 min=0 max=79 last=0 firstAtMs=100 lastAtMs=300"))
        assertTrue(report.contains("100ms mcsp address=0x185b raw=79 source=push"))
        assertTrue(report.contains("300ms mcsp address=0x185b raw=0 source=push"))
    }

    @Test fun allPackedPowerSamplesCountEvenAfterHistoryEvictionAndResetClearsThem() {
        val log = DiagnosticLog({ 0 }, maxPackets = 1)
        val packet = power(79) + byteArrayOf(0x30, 5, 0x98.toByte(), 0x5b, 8, 0xb2.toByte(), 4) +
            byteArrayOf(0x30, 2, 0x98.toByte(), 0x5b)
        log.mcspOutcome(McspProtocol().receive(packet))
        assertTrue(log.report().contains("Fahrerleistung address=0x185b count=3 nonzero=2 min=0 max=562 last=0"))
        assertFalse(log.report().contains("raw=562"))
        log.reset("next ride")
        assertTrue(log.report().contains("Fahrerleistung address=0x185b count=0"))
        assertFalse(log.report().contains("raw=0"))
    }

    @Test fun emptyReadReplyIsDistinctFromReceivedZero() {
        val protocol = McspProtocol()
        val log = DiagnosticLog({ 0 })
        log.mcspOutcome(protocol.receive(byteArrayOf(0x30, 5, 0x18, 0x5b, 0xc0.toByte(), 0x80.toByte(), 0x13)))
        assertTrue(log.report().contains("Fahrerleistung address=0x185b count=0"))
        log.mcspOutcome(protocol.receive(byteArrayOf(0x30, 2, 0x98.toByte(), 0x5b)))
        assertTrue(log.report().contains("Fahrerleistung address=0x185b count=1 nonzero=0 min=0 max=0 last=0"))
    }

    @Test fun optionalFieldStatisticsSupportUint32AndDoNotLogModeStrings() {
        val log = DiagnosticLog({ 0 })
        val protocol = McspProtocol()
        val odometer = byteArrayOf(0x30, 8, 0x98.toByte(), 0x18, 8, -1, -1, -1, -1, 15)
        log.mcspOutcome(protocol.receive(odometer))
        val names = byteArrayOf(0x30, 7, 0x98.toByte(), 0x0d, 10, 3, 69, 67, 79)
        log.mcspOutcome(protocol.receive(names))
        assertTrue(log.report().contains("Gesamtkilometer address=0x1818 count=1 nonzero=1 min=4294967295"))
        assertFalse(log.report().contains("ECO"))
    }

    @Test fun historiesAndEventsRemainBoundedWithoutLosingTotalsOrHandshake() {
        val log = DiagnosticLog({ 0 }, maxEventChars = 128, maxPackets = 1)
        repeat(1000) { log.event("event $it") }
        assertFalse(log.report().contains("event 0\n"))
        assertTrue(log.report().contains("event 999\n"))
        log.reset("bounded")
        log.event("CCCD_WRITE_RESULT status=0")
        val protocol = McspProtocol()
        repeat(10_000) { log.mcspOutcome(protocol.receive(power((it % 100).toByte()))) }
        val report = log.report()
        assertTrue(report.contains("CCCD_WRITE_RESULT status=0"))
        assertTrue(report.contains("count=10000 nonzero=9900 min=0 max=99"))
        assertTrue(report.length < 3000)
        assertFalse(report.contains("raw=98"))
    }
}
