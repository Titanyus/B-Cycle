package de.bcycle.app

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.RandomAccessFile

class PerformanceJournalTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun subsecondObservationsAndMeasurementAgeSurviveRecoveryIndependentlyOfRawCheckboxes() {
        val file = folder.newFile("observations.ride")
        val config = RecordingConfig(setOf("MECHANICAL_WORK"))
        val samples = listOf(
            RideSample(1000, 0, performanceEvents = listOf(PerformanceEvent(PerformanceChannel.POWER, 0, 100.0, -100, -200, PowerSource.BOSCH_RIDER.name))),
            RideSample(2000, 1000, performanceEvents = listOf(PerformanceEvent(PerformanceChannel.POWER, 250, 300.0, source = PowerSource.BOSCH_RIDER.name))),
            RideSample(3000, 2000, performanceEvents = listOf(PerformanceEvent(PerformanceChannel.POWER, 1900, 0.0, source = PowerSource.BOSCH_RIDER.name))),
            RideSample(4000, 3000, performanceEvents = listOf(PerformanceEvent(PerformanceChannel.POWER, 2500, connected = false, source = PowerSource.BOSCH_RIDER.name))))
        RideJournal(file, 1000, config).use { journal -> samples.forEach(journal::append) }
        RideJournal.read(file) { start, restored, raw, records ->
            assertEquals(1000L, start); assertEquals(config, restored); assertTrue("RIDER_POWER" in raw); assertTrue("MOTOR_POWER" in raw)
            val decoded = records.toList(); assertEquals(samples, decoded)
            val live = PerformanceEngine(); val replayed = PerformanceEngine()
            for (sample in samples) { sample.performanceEvents.forEach(live::accept); live.advance(sample.elapsedMs) }
            for (sample in decoded) { sample.performanceEvents.forEach(replayed::accept); replayed.advance(sample.elapsedMs) }
            assertEquals(live.snapshot(), replayed.snapshot())
            assertEquals(.520, replayed.snapshot().knownMechanicalWorkKj, 1e-9)
            assertEquals(500L, replayed.snapshot().quality.longestPowerGapMs)
        }
    }
    @Test fun interruptedLastSampleLeavesEarlierCompleteEventBatchesReadable() {
        val file = folder.newFile("interrupted.ride")
        RideJournal(file, 1000, RecordingConfig(setOf("MECHANICAL_WORK"))).use { writer ->
            repeat(3) { index -> writer.append(RideSample(1000 + index * 1000L, index * 1000L,
                performanceEvents = listOf(PerformanceEvent(PerformanceChannel.POWER, index * 1000L, 200.0, source = PowerSource.BOSCH_RIDER.name)))) }
        }
        RandomAccessFile(file, "rw").use { it.setLength(it.length() - 5) }
        RideJournal.read(file) { _, _, _, records ->
            val recovered = records.toList(); assertEquals(2, recovered.size)
            assertEquals(listOf(0L, 1000L), recovered.map { it.performanceEvents.single().atMs })
        }
    }
}
