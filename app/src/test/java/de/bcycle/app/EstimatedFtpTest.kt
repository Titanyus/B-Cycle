package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import com.garmin.fit.*
import com.garmin.fit.plugins.ActivityFileValidationPlugin
import com.garmin.fit.plugins.ActivityFileValidationResult
import java.io.File

class EstimatedFtpTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun exactSlidingWindowsIncludeShiftedOutgoingBoundariesAndTimeWeights() {
        val window = PowerDurationWindow(1000)
        window.add(0, 400, 10.0); window.add(400, 1300, 200.0); window.add(1300, 2000, 20.0)
        assertEquals(182.0, window.bestAverageW!!, 1e-9)
        val irregular = PowerDurationWindow(1_200_000)
        irregular.add(0, 100, 100.0); irregular.add(100, 1_200_000, 200.0)
        assertEquals((100.0 * 100 + 200.0 * 1199900) / 1200000, irregular.bestAverageW!!, 1e-9)
    }
    @Test fun insufficientWindowsAndSeparatedSectionsNeverJoinButPriorValidBestSurvives() {
        val w = PowerDurationWindow(1_200_000)
        w.add(0, 1_199_999, 300.0); assertNull(w.bestAverageW)
        w.add(1_200_000, 1_800_000, 300.0); assertNull(w.bestAverageW)
        w.breakContinuity(); w.add(2_000_000, 3_200_000, 200.0); assertEquals(200.0, w.bestAverageW!!, 0.0)
        w.breakContinuity(); w.add(4_000_000, 4_100_000, 800.0); assertEquals(200.0, w.bestAverageW!!, 0.0)
    }
    @Test fun engineIncludesZerosAndDoesNotAcceptOtherSourcesOrPausedTime() {
        fun engine(pause: Boolean = false, missing: Boolean = false, other: Boolean = false): PerformanceEngine {
            val e = PerformanceEngine()
            for (second in 0..1200) {
                val time = second * 1000L
                if (pause && second == 600) e.accept(PerformanceEvent(PerformanceChannel.PAUSE, time))
                if (pause && second == 601) e.accept(PerformanceEvent(PerformanceChannel.RESUME, time))
                e.accept(PerformanceEvent(PerformanceChannel.POWER, time,
                    if (missing && second == 600) null else if (second < 600) 200.0 else 0.0,
                    source = if (other) PowerSource.PEDAL.name else PowerSource.BOSCH_RIDER.name))
            }
            return e
        }
        assertEquals(95.0, engine().snapshot().estimatedFtpW!!, 1e-9)
        assertNull(engine(pause = true).snapshot().estimatedFtpW)
        assertNull(engine(missing = true).snapshot().estimatedFtpW)
        assertNull(engine(other = true).snapshot().estimatedFtpW)
        val stale = PerformanceEngine(); stale.accept(PerformanceEvent(PerformanceChannel.POWER, 0, 200.0, source = PowerSource.BOSCH_RIDER.name)); stale.advance(1_200_000)
        assertNull(stale.snapshot().estimatedFtpW)
    }
    private fun file(export: Boolean, seconds: Int = 1200): File {
        val journal = folder.newFile("${System.nanoTime()}.ride")
        val selection = setOf("RIDER_POWER", "MOTOR_POWER") + if (export) setOf("ESTIMATED_FTP") else emptySet()
        RideJournal(journal, 1_791_626_400_000, RecordingConfig(selection)).use { writer ->
            for (second in 0..seconds) writer.append(RideSample(1_791_626_400_000 + second * 1000, second * 1000L,
                values = mapOf("RIDER_POWER" to 200.0, "MOTOR_POWER" to 800.0), performanceEvents = listOf(
                    PerformanceEvent(PerformanceChannel.POWER, second * 1000L, 200.0, source = PowerSource.BOSCH_RIDER.name))))
        }
        return folder.newFile("${System.nanoTime()}.fit").also { FitWriter.encode(journal, it) }
    }
    @Test fun optionalExportUsesSessionOnlyKeepsGarminValidationAndNeverSynthesizesRecords() {
        assertFalse("ESTIMATED_FTP" in RecordField.defaults)
        val fit = file(true)
        val plugin = ActivityFileValidationPlugin()
        val messages = fit.inputStream().use { FitDecoder().decode(it, plugin) }
        assertTrue(plugin.results.none { it.status == ActivityFileValidationResult.Status.FAILED })
        assertEquals(190.0, (messages.sessionMesgs.single().developerFields.single { it.name == "ESTIMATED_FTP" }.getValue(0) as Number).toDouble(), 1e-9)
        assertFalse(messages.lapMesgs.any { it.developerFields.any { f -> f.name == "ESTIMATED_FTP" } })
        assertFalse(messages.recordMesgs.any { it.developerFields.any { f -> f.name == "ESTIMATED_FTP" } })
        val description = messages.fieldDescriptionMesgs.single { it.getFieldName(0) == "ESTIMATED_FTP" }
        assertEquals(18, description.nativeMesgNum.toInt()); assertEquals("W", description.getUnits(0))
        val absent = file(false).inputStream().use { FitDecoder().decode(it) }
        assertFalse(absent.sessionMesgs.single().developerFields.any { it.name == "ESTIMATED_FTP" })
        val short = file(true, 1199).inputStream().use { FitDecoder().decode(it) }
        assertFalse(short.sessionMesgs.single().developerFields.any { it.name == "ESTIMATED_FTP" })
    }
    @Test fun detailsReadSavedFtpOrCalculateItFromOldRawRecordsIndependentlyOfExportSelection() {
        val entry = ManagedRoute("id", "Ride", true, "FIT", RouteDetails(), false)
        for (selected in listOf(true, false)) {
            val f = file(selected); val bytes = f.readBytes()
            val a = RouteAnalysisRead.read(f, entry)
            assertEquals(190.0, a.metrics.getValue(AnalysisMetric.FTP), .00001)
            assertEquals(listOf(1200000L, 480000L, 300000L, 60000L), a.bestEfforts.map { it.durationMs })
            assertTrue(a.bestEfforts.all { kotlin.math.abs(it.watts - 200.0) < .00001 })
            assertArrayEquals(bytes, f.readBytes())
        }
    }
}
