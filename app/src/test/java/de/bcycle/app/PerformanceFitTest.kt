package de.bcycle.app

import com.garmin.fit.*
import com.garmin.fit.plugins.ActivityFileValidationPlugin
import com.garmin.fit.plugins.ActivityFileValidationResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.DataOutputStream
import java.time.Instant

class PerformanceFitTest {
    @get:Rule val folder = TemporaryFolder()
    private val start = Instant.parse("2026-10-11T10:00:00Z").toEpochMilli()
    private fun create(selected: Set<String>, seconds: Int = 60, missing: Boolean = false): Pair<File, FitMessages> {
        val journal = folder.newFile("${System.nanoTime()}.ride")
        RideJournal(journal, start, RecordingConfig(selected)).use { writer ->
            for (second in 0..seconds) {
                val time = second * 1000L
                val events = listOf(
                    PerformanceEvent(PerformanceChannel.POWER, time, if (missing) null else 200.0, source = PowerSource.BOSCH_RIDER.name),
                    PerformanceEvent(PerformanceChannel.HEART_RATE, time, 150.0, source = "HEART_RATE"),
                    PerformanceEvent(PerformanceChannel.SPEED, time, 10.0, source = "SPEED"))
                writer.append(RideSample(start + time, time, values = mapOf("RIDER_POWER" to 200.0, "HEART_RATE" to 150.0,
                    "MOTOR_POWER" to 800.0, "CADENCE" to 90.0, "SPEED" to 10.0, "BIKE_DISTANCE" to second * 10.0), performanceEvents = events))
            }
        }
        val file = folder.newFile("${System.nanoTime()}.fit"); FitWriter.encode(journal, file)
        val plugin = ActivityFileValidationPlugin()
        val decoded = file.inputStream().use { FitDecoder().decode(it, plugin) }
        assertTrue(plugin.results.isNotEmpty())
        assertTrue(plugin.results.joinToString { it.description ?: it.status.toString() }, plugin.results.none { it.status == ActivityFileValidationResult.Status.FAILED })
        return file to decoded
    }
    @Test fun nativeFieldsHaveOfficialNumbersTypesUnitsAndScaleOne() {
        val scales = Field::class.java.getDeclaredMethod("getScale").apply { isAccessible = true }
        val offsets = Field::class.java.getDeclaredMethod("getOffset").apply { isAccessible = true }
        for ((message, number) in listOf(18 to 34, 19 to 33)) {
            val np = Factory.createField(message, "normalized_power")
            assertEquals(number, np.num); assertEquals(0x84, np.type); assertEquals("watts", np.units)
            assertEquals(1.0, scales.invoke(np) as Double, 0.0); assertEquals(0.0, offsets.invoke(np) as Double, 0.0)
            val work = Factory.createField(message, "total_work")
            assertEquals(if (message == 18) 48 else 41, work.num); assertEquals(0x86, work.type); assertEquals("J", work.units)
            assertEquals(1.0, scales.invoke(work) as Double, 0.0)
            val calories = Factory.createField(message, "total_calories")
            assertEquals(11, calories.num); assertEquals(0x84, calories.type); assertEquals("kcal", calories.units)
        }
    }
    @Test fun allMetricsBelongToOneExistingSessionAndLapAndNeverToRecords() {
        val selected = RecordingConfig().fields + RecordField.summaries.map { it.id }
        val (file, messages) = create(selected)
        assertEquals(61, messages.recordMesgs.size); assertEquals(1, messages.sessionMesgs.size); assertEquals(1, messages.lapMesgs.size)
        val session = messages.sessionMesgs.single(); val lap = messages.lapMesgs.single()
        assertEquals(200, session.normalizedPower.toInt()); assertEquals(200, lap.normalizedPower.toInt())
        assertEquals(12000L, session.totalWork); assertEquals(12000L, lap.totalWork)
        assertEquals(12, session.totalCalories.toInt()); assertEquals(12, lap.totalCalories.toInt())
        fun value(m: Mesg, name: String) = (m.developerFields.single { it.name == name }.getValue(0) as Number).toDouble()
        for (m in listOf(session, lap)) {
            assertEquals(.18, value(m, "SPEED_POWER_RATIO"), 1e-9)
            assertEquals(4.0 / 3, value(m, "EFFICIENCY_FACTOR"), 1e-9)
            assertEquals(100.0, value(m, "POWER_DATA_COVERAGE"), 1e-9)
            assertEquals(0.0, value(m, "POWER_MISSING_TIME"), 1e-9)
            assertEquals(.24, value(m, "ENERGY_GROSS_EFFICIENCY"), 0.0)
            assertEquals("km/h/W", m.developerFields.single { it.name == "SPEED_POWER_RATIO" }.units)
            assertEquals("W/bpm", m.developerFields.single { it.name == "EFFICIENCY_FACTOR" }.units)
        }
        for (record in messages.recordMesgs) {
            assertEquals(200, record.power.toInt()); assertEquals(800, record.motorPower.toInt())
            assertFalse(record.developerFields.any { it.name in RecordField.summaries.map { f -> f.id } || it.name == "POWER_DATA_COVERAGE" })
        }
        // Definition metadata associates custom summaries with their actual message, not Record 20.
        val descriptions = messages.fieldDescriptionMesgs.filter { it.getFieldName(0) in setOf("SPEED_POWER_RATIO", "EFFICIENCY_FACTOR") }
        assertEquals(setOf(18, 19), descriptions.map { it.nativeMesgNum.toInt() }.toSet())
        assertEquals(4, descriptions.map { it.fieldDefinitionNumber }.toSet().size)
        assertTrue(messages.developerDataIdMesgs.isNotEmpty())
        val example = File("build/reports/fit/performance-garmin-validated.fit").apply { parentFile!!.mkdirs() }
        file.copyTo(example, true)
    }
    @Test fun efUsesInternalNpEvenWhenNpAndPowerRecordCheckboxesAreOff() {
        val messages = create(setOf("EFFICIENCY_FACTOR")).second
        assertNull(messages.sessionMesgs.single().normalizedPower)
        assertNull(messages.sessionMesgs.single().totalWork); assertNull(messages.sessionMesgs.single().totalCalories)
        assertTrue(messages.recordMesgs.all { it.power == null && it.heartRate == null })
        val ef = messages.sessionMesgs.single().developerFields.single { it.name == "EFFICIENCY_FACTOR" }
        assertEquals(4.0 / 3, (ef.getValue(0) as Number).toDouble(), 1e-9)
        assertFalse(messages.sessionMesgs.single().developerFields.any { it.name == "SPEED_POWER_RATIO" })
    }
    @Test fun eachOptionalCheckboxExportsOnlyItsOwnMetricWithoutEnablingDependencies() {
        for (field in RecordField.summaries) {
            val messages = create(setOf(field.id)).second
            val session = messages.sessionMesgs.single()
            assertEquals(field.id == "NORMALIZED_POWER", session.normalizedPower != null)
            assertEquals(field.id == "MECHANICAL_WORK", session.totalWork != null)
            assertEquals(field.id == "CALORIES", session.totalCalories != null)
            assertEquals(field.id == "SPEED_POWER_RATIO", session.developerFields.any { it.name == "SPEED_POWER_RATIO" })
            assertEquals(field.id == "EFFICIENCY_FACTOR", session.developerFields.any { it.name == "EFFICIENCY_FACTOR" })
        }
    }
    @Test fun defaultSelectionAndPoorQualityOmitAllNewMetricsRatherThanWritingFakeNumbers() {
        for (selection in listOf(RecordingConfig().fields, RecordField.summaries.map { it.id }.toSet())) {
            val messages = create(selection, missing = true).second
            assertNull(messages.sessionMesgs.single().normalizedPower); assertNull(messages.sessionMesgs.single().totalWork)
            assertNull(messages.sessionMesgs.single().totalCalories)
            assertTrue(messages.sessionMesgs.single().developerFields.none { it.name in RecordField.summaries.map { f -> f.id } })
        }
        val short = create(RecordField.summaries.map { it.id }.toSet(), 10).second
        assertNull(short.sessionMesgs.single().normalizedPower)
        assertTrue(short.sessionMesgs.single().developerFields.none { it.name == "EFFICIENCY_FACTOR" })
        assertEquals(2000L, short.sessionMesgs.single().totalWork)
    }
    @Test fun recordInputSelectionDoesNotAlterSummaryResultsAndSummarySelectionDoesNotAlterRawRecords() {
        val all = create(RecordingConfig().fields + RecordField.summaries.map { it.id }).second
        val none = create(RecordingConfig().fields).second
        val dependent = create(RecordField.summaries.map { it.id }.toSet()).second
        assertEquals(all.sessionMesgs.single().normalizedPower, dependent.sessionMesgs.single().normalizedPower)
        assertEquals(all.sessionMesgs.single().totalWork, dependent.sessionMesgs.single().totalWork)
        for ((a, b) in all.recordMesgs.zip(none.recordMesgs)) {
            assertEquals(a.timestamp.timestamp, b.timestamp.timestamp); assertEquals(a.power, b.power)
            assertEquals(a.cadence, b.cadence); assertEquals(a.heartRate, b.heartRate); assertEquals(a.speed, b.speed)
            assertEquals(a.distance, b.distance); assertEquals(a.developerFields.map { it.name }, b.developerFields.map { it.name })
        }
    }
    @Test fun originalV4JournalsRemainReadableWithoutInventingSensorTimestampsOrSummaryValues() {
        val old = folder.newFile("legacy.ride")
        DataOutputStream(old.outputStream()).use {
            it.writeInt(0x42435934); it.writeLong(start); it.writeUTF("SPEED"); it.writeUTF("BIKE_DISTANCE")
            it.writeBoolean(false); it.writeInt(1); it.writeUTF("RIDER_POWER")
            repeat(2) { i -> it.writeLong(start + i * 1000); it.writeLong(i * 1000L); it.writeDouble(Double.NaN); it.writeDouble(Double.NaN); it.writeDouble(200.0) }
        }
        val file = folder.newFile("legacy.fit"); FitWriter.encode(old, file)
        val decoded = file.inputStream().use { FitDecoder().decode(it) }
        assertEquals(listOf(200, 200), decoded.recordMesgs.map { it.power.toInt() })
        assertNull(decoded.sessionMesgs.single().normalizedPower)
    }
}
