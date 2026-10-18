package de.bcycle.app

import com.garmin.fit.FitDecoder
import com.garmin.fit.FitMessages
import com.garmin.fit.Sport
import com.garmin.fit.SubSport
import com.garmin.fit.plugins.ActivityFileValidationPlugin
import com.garmin.fit.plugins.ActivityFileValidationResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.RandomAccessFile
import java.time.Instant

class FitWriterTest {
    @get:Rule val folder = TemporaryFolder()
    private val start = Instant.parse("2026-10-10T10:00:00Z").toEpochMilli()
    private fun generate(config: RecordingConfig = RecordingConfig(), interrupted: Boolean = false): Pair<FitMessages, ActivityFileValidationPlugin> {
        val file = folder.newFile("ride-${System.nanoTime()}.ride")
        RideJournal(file, start, config).use { journal ->
            for (second in 0..10) journal.append(RideSample(start + second * 1000, second * 1000L, 50.0 + second * 0.00001, 8.0,
                mapOf("SPEED" to 7.0, "GPS_SPEED" to 6.0, "BIKE_DISTANCE" to second * 7.0, "GPS_DISTANCE" to second * 6.0,
                    "RIDER_POWER" to 183.0, "MOTOR_POWER" to 320.0, "CADENCE" to 85.5, "BATTERY" to 96.0,
                    "HEART_RATE" to 145.0, "GPS_ALTITUDE" to 120.0, "GPS_ALTITUDE_MSL" to 1.0,
                    "GPS_GRADIENT" to -2.5, "GPS_ASCENT" to 3.0, "GPS_DESCENT" to 2.0, "ODOMETER" to 14791937.0)))
        }
        if (interrupted) RandomAccessFile(file, "rw").use { it.setLength(it.length() - 5) }
        val fit = folder.newFile("ride-${System.nanoTime()}.fit")
        FitWriter.encode(file, fit)
        val validation = ActivityFileValidationPlugin()
        val messages = fit.inputStream().use { FitDecoder().decode(it, validation) }
        assertTrue(validation.results.isNotEmpty())
        val failures = validation.results.filter { it.status == ActivityFileValidationResult.Status.FAILED }
        assertTrue(failures.joinToString { "$it ${it.description}" }, failures.isEmpty())
        if (!interrupted) {
            val output = java.io.File("build/reports/fit/garmin-validated-example.fit").apply { parentFile!!.mkdirs() }
            fit.copyTo(output, overwrite = true)
        }
        return messages to validation
    }
    @Test fun officialGarminDecoderAndActivityValidatorAcceptAllSourcesAndCorrectScales() {
        val messages = generate().first
        assertEquals(11, messages.recordMesgs.size)
        assertEquals(AppVersion.fitSoftwareVersion, messages.deviceInfoMesgs.single().softwareVersion.toDouble(), .00001)
        assertEquals("B|CYCLE", messages.deviceInfoMesgs.single().productName)
        assertEquals(AppVersion.code.toLong(), messages.developerDataIdMesgs.single().applicationVersion.toLong())
        val record = messages.recordMesgs.last()
        assertEquals(7.0, record.speed.toDouble(), 0.001)
        assertEquals(70.0, record.distance.toDouble(), 0.001)
        assertEquals(183, record.power.toInt())
        assertEquals(320, record.motorPower.toInt())
        assertEquals(96.0, record.batterySoc.toDouble(), 0.001)
        assertEquals(145, record.heartRate.toInt())
        assertEquals(85, record.cadence.toInt())
        assertEquals(0.5, record.fractionalCadence.toDouble(), 0.001)
        assertEquals(120.0, record.enhancedAltitude.toDouble(), 0.001)
        assertEquals(-2.5, record.grade.toDouble(), 0.001)
        assertEquals(50.0001, record.positionLat.toDouble() * 180 / 2147483648.0, 0.000001)
        val secondary = record.developerFields.single { it.name == "GPS_SPEED" }
        assertEquals(6.0, (secondary.getValue(0) as Number).toDouble(), 0.000001)
        assertEquals(Sport.CYCLING, messages.sessionMesgs.single().sport)
        assertEquals(SubSport.E_BIKE_FITNESS, messages.sessionMesgs.single().subSport)
        assertEquals(70.0, messages.sessionMesgs.single().totalDistance.toDouble(), 0.001)
    }
    @Test fun fieldSelectionAndPrimarySourceAreIndependentAndMissingValuesStayUnknown() {
        val config = RecordingConfig(setOf("GPS_SPEED", "GPS_DISTANCE", "HEART_RATE"), "GPS_SPEED", "GPS_DISTANCE")
        val record = generate(config).first.recordMesgs.last()
        assertEquals(6.0, record.speed.toDouble(), 0.001)
        assertEquals(60.0, record.distance.toDouble(), 0.001)
        assertNull(record.power); assertNull(record.motorPower); assertNull(record.positionLat); assertNull(record.altitude)
        assertEquals(setOf("GPS_SPEED", "GPS_DISTANCE", "HEART_RATE"), record.developerFields.map { it.name }.toSet())
        val missing = generate(RecordingConfig(setOf("SPEED", "BIKE_DISTANCE", "HEART_BATTERY"))).first.recordMesgs.last()
        assertNull(missing.heartRate)
        assertFalse(missing.developerFields.any { it.name == "HEART_BATTERY" }) // SDK omits an all-invalid field.
    }
    @Test fun interruptedFinalJournalSampleIsDiscardedAndRecoveredActivityIsValid() {
        val messages = generate(interrupted = true).first
        assertEquals(10, messages.recordMesgs.size)
        assertEquals(9.0, messages.sessionMesgs.single().totalElapsedTime.toDouble(), 0.001)
    }
    @Test fun ellipsoidAltitudeIsPreservedAsExtraFieldAndNeverMislabelledAsSeaLevel() {
        val journal = folder.newFile("ellipsoid.ride")
        RideJournal(journal, start, RecordingConfig(setOf("GPS_ALTITUDE"))).use {
            it.append(RideSample(start, 0, values = mapOf("GPS_ALTITUDE" to 120.0, "GPS_ALTITUDE_MSL" to 0.0)))
            it.append(RideSample(start + 1000, 1000, values = mapOf("GPS_ALTITUDE" to 121.0, "GPS_ALTITUDE_MSL" to 0.0)))
        }
        val fit = folder.newFile("ellipsoid.fit")
        FitWriter.encode(journal, fit)
        val record = fit.inputStream().use { FitDecoder().decode(it).recordMesgs.last() }
        assertNull(record.altitude); assertNull(record.enhancedAltitude)
        assertEquals(121.0, (record.developerFields.single { it.name == "GPS_ALTITUDE" }.getValue(0) as Number).toDouble(), 0.001)
        assertEquals(0.0, (record.developerFields.single { it.name == "GPS_ALTITUDE_MSL" }.getValue(0) as Number).toDouble(), 0.001)
    }
    @Test fun cadenceSummaryOmitsPedalingPausesButOfficialRecordsKeepZeroAndMovingTimeExcludesStops() {
        val journal = folder.newFile("pauses.ride")
        RideJournal(journal, start, RecordingConfig()).use { writer ->
            for (i in 0..4) writer.append(RideSample(start + i * 1000, i * 1000L, values = mapOf(
                "CADENCE" to listOf(80.0, 0.0, 100.0, 0.0, 0.0)[i],
                "SPEED" to if (i < 3) 5.0 else 0.0, "BIKE_DISTANCE" to minOf(i, 2) * 5.0)))
        }
        val file = folder.newFile("pauses.fit"); FitWriter.encode(journal, file)
        val validator = ActivityFileValidationPlugin()
        val decoded = file.inputStream().use { FitDecoder().decode(it, validator) }
        assertFalse(validator.results.any { it.status == ActivityFileValidationResult.Status.FAILED })
        assertEquals(listOf(80, 0, 100, 0, 0), decoded.recordMesgs.map { it.cadence.toInt() })
        assertEquals(90, decoded.sessionMesgs.single().avgCadence.toInt())
        assertEquals(90, decoded.lapMesgs.single().avgCadence.toInt())
        assertEquals(2.0, decoded.sessionMesgs.single().totalMovingTime.toDouble(), .001)
        assertEquals(4.0, decoded.sessionMesgs.single().totalTimerTime.toDouble(), .001)
        val details = FitRead.read(file).details
        assertEquals(2_000L, details.movingMs)
        assertEquals(18.0, details.movingAverageKmh!!, .001)
    }
    @Test fun pureRecordingMappingUsesSiUnitsAndKeepsBatteryTemperatureSeparateFromAmbient() {
        val bike = LiveData(2520, 183, mapOf(LiveMetric.CADENCE to 171L, LiveMetric.BATTERY_TEMPERATURE to 354L, LiveMetric.ODOMETER to 14791937L))
        val values = recordingValues(bike, GpsValues(speedKmh = 21.6), 70, PulseValues(bpm = 145))
        assertEquals(7.0, values["SPEED"]!!, 0.001)
        assertEquals(6.0, values["GPS_SPEED"]!!, 0.001)
        assertEquals(17.7, values["BATTERY_TEMPERATURE"]!!, 0.001)
        assertEquals(14791937.0, values["ODOMETER"]!!, 0.001)
        assertEquals(85.5, values["CADENCE"]!!, 0.001)
    }
}
