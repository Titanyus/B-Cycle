package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class DiagnosticLogTest {
    @Test fun emptySuccessfulReadsNeverBecomeZeroMeasurements() {
        val log = DiagnosticLog({ 0 })
        log.reset("empty LDI response regression")
        log.event("CCCD_WRITE_RESULT status=0")
        repeat(2) {
            val packet = byteArrayOf()
            log.packet("read", packet)
            log.decoded(LiveDataDecoder.decode(packet))
        }
        val report = log.report()
        assertTrue(report.contains("reads=2 notifications=0 decodeErrors=0"))
        assertTrue(report.contains("speedFieldSeen=false riderPowerFieldSeen=false"))
        assertTrue(report.contains("read bytes=0 hex="))
    }

    @Test fun repeatedManualReadsRetainOnlyTheLastFourWithoutLosingTotals() {
        val log = DiagnosticLog({ 0 })
        log.reset("manual reads")
        log.event("CCCD_WRITE_RESULT status=0")
        for (power in 1..6) {
            log.packet("read", byteArrayOf(40, power.toByte()))
        }
        val report = log.report()
        assertTrue(report.contains("reads=6 notifications=0"))
        assertTrue(report.contains("CCCD_WRITE_RESULT status=0"))
        assertFalse(report.contains("hex=2801"))
        assertFalse(report.contains("hex=2802"))
        for (hex in listOf("2803", "2804", "2805", "2806")) assertTrue(report.contains("hex=$hex"))
    }

    @Test fun reportsReadAndNotificationCountsIncludingExplicitZeroFields() {
        var now = 1000L
        val log = DiagnosticLog({ now })
        log.reset("App 0.1.1")
        now += 50
        log.packet("read", byteArrayOf(8, 0))
        log.decoded(LiveData(0))
        log.packet("notification", byteArrayOf(40, 0))
        log.decoded(LiveData(riderPowerWatts = 0))
        log.decodeFailed("truncated varint")
        val report = log.report()
        assertTrue(report.contains("reads=1 notifications=1 decodeErrors=1"))
        assertTrue(report.contains("speedFieldSeen=true riderPowerFieldSeen=true"))
        assertTrue(report.contains("50ms read bytes=2 hex=0800"))
        assertTrue(report.contains("DECODE_ERROR truncated varint"))
    }

    @Test fun packetHistoryCannotEvictTheHandshakeAndRetainsTotalCounts() {
        val log = DiagnosticLog({ 0 }, maxPackets = 2)
        log.reset("device metadata")
        log.event("MTU_CHANGED mtu=247")
        log.packet("read", byteArrayOf(8, 100))
        log.packet("notification", byteArrayOf(1))
        log.packet("notification", byteArrayOf(2))
        log.packet("notification", byteArrayOf(3))
        val report = log.report()
        assertTrue(report.contains("MTU_CHANGED mtu=247"))
        assertTrue(report.contains("read bytes=2 hex=0864"))
        assertTrue(report.contains("notifications=3"))
        assertFalse(report.contains("hex=01"))
        assertTrue(report.contains("hex=02"))
        assertTrue(report.contains("hex=03"))
    }

    @Test fun eventAndPayloadStorageAreBounded() {
        val log = DiagnosticLog({ 0 }, maxEventChars = 128, maxPackets = 1)
        repeat(1000) { log.event("event $it") }
        log.packet("read", ByteArray(10_000) { -1 })
        val report = log.report()
        assertFalse(report.contains("event 0\n"))
        assertTrue(report.contains("event 999\n"))
        assertTrue(report.contains("bytes=10000"))
        assertTrue(report.contains("[truncated in log]"))
        assertTrue(report.length < 2000)
    }

    @Test fun stopEventsPreserveFailureAndANewSessionClearsOldData() {
        val log = DiagnosticLog({ 0 })
        log.reset("first session")
        log.event("FAIL no Bosch service")
        log.event("STOP")
        assertTrue(log.report().contains("FAIL no Bosch service"))
        log.packet("read", byteArrayOf(8, 0))
        log.decoded(LiveData(0))
        log.reset("second session")
        val report = log.report()
        assertFalse(report.contains("first session"))
        assertFalse(report.contains("FAIL"))
        assertTrue(report.contains("reads=0 notifications=0 decodeErrors=0"))
        assertTrue(report.contains("speedFieldSeen=false riderPowerFieldSeen=false"))
    }

    @Test fun mcspReportRetainsOnlyWhitelistedNumericSamplesAndCountsAllNotifications() {
        val protocol = McspProtocol()
        val log = DiagnosticLog({ 0 })
        val private = "private bike serial".toByteArray()
        val unknown = byteArrayOf(0x30, (private.size + 4).toByte(), 0xa0.toByte(), 0x81.toByte(), 0x0a, private.size.toByte()) + private
        val measurement = byteArrayOf(0x30, 4, 0x98.toByte(), 0x5b, 8, 100)
        for (packet in listOf(unknown, measurement)) {
            log.mcspNotification(packet.size)
            log.mcspOutcome(protocol.receive(packet))
        }
        val report = log.report()
        assertTrue(report.contains("notifications=2"))
        assertTrue(report.contains("messages=2 ignored=1"))
        assertTrue(report.contains("address=0x185b raw=100 source=push"))
        assertFalse(report.contains("private bike serial"))
        assertFalse(report.contains("70726976617465"))
    }
}
