package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RecordingFieldsTest {
    // Captured from 3e7db0e before changing production code, using synthetic data only.
    private fun reference(name:String)=javaClass.getResourceAsStream("/recording/$name")!!
    @Test fun catalogOrderUnitsScopesLabelsAndDefaultSelectionMatchExistingRelease() {
        val context=RuntimeEnvironment.getApplication()
        val expected=reference("recording-fields.txt").bufferedReader().use { it.readText() }
        val actual=RecordField.options.joinToString("\n") {
            listOf(it.id,it.title,it.source,it.group,it.unit,it.scope.name,it.titleResource?.let(context.resources::getResourceEntryName).orEmpty()).joinToString("|")
        }+"\n"
        assertEquals(expected,actual)
        val defaults=expected.lineSequence().filter { it.isNotEmpty() }.map { it.split('|') }
            .filter { it[5]=="RECORD" }.map { it[0] }.toSet()
        assertEquals(defaults,RecordField.defaults)
        assertEquals(defaults,RecordingConfig.decode(null,null,null).fields)
        assertEquals(defaults,RecordingFields.defaultIds)
        assertEquals(RecordField.options.map { Triple(it.id,it.unit,it.scope) },RecordingFields.options.map { Triple(it.id,it.unit,it.scope) })
    }
    @Test fun writingTheLegacyJournalAgainPreservesEveryByte() {
        val context=RuntimeEnvironment.getApplication()
        val original=reference("legacy-recording.ride").use { it.readBytes() }
        val source=File(context.cacheDir,"source.ride").apply { writeBytes(original) }
        val target=File(context.cacheDir,"rewritten.ride")
        RideJournal.read(source) { start,config,_,records ->
            RideJournal(target,start,config).use { writer -> records.forEach(writer::append) }
        }
        assertArrayEquals(original,target.readBytes())
    }
    @Test fun fullFitFromLegacyJournalPreservesRecordsSummariesAndDeveloperDefinitions() {
        val context=RuntimeEnvironment.getApplication()
        val source=File(context.cacheDir,"legacy.ride")
        reference("legacy-recording.ride").use { input -> source.outputStream().use { input.copyTo(it) } }
        val target=File(context.cacheDir,"new.fit")
        FitWriter.encode(source,target,fullData=true)
        val before=reference("legacy-recording-full.fit").use { com.garmin.fit.FitDecoder().decode(it) }
        val after=target.inputStream().use { com.garmin.fit.FitDecoder().decode(it) }
        fun fields(message:com.garmin.fit.Mesg)=message.fields.map { field ->
            field.num to (0 until field.numValues).map { field.getRawValue(it)?.toString() }
        }
        assertEquals(before.fieldDescriptionMesgs.map(::fields),after.fieldDescriptionMesgs.map(::fields))
        assertEquals(before.recordMesgs.map(::fields),after.recordMesgs.map(::fields))
        assertEquals(before.sessionMesgs.map(::fields),after.sessionMesgs.map(::fields))
        assertEquals(before.lapMesgs.map(::fields),after.lapMesgs.map(::fields))
        fun developer(message:com.garmin.fit.Mesg)=message.developerFields.map { field ->
            Triple(field.name,field.units,(0 until field.numValues).map { field.getValue(it)?.toString() })
        }
        assertEquals(before.recordMesgs.map(::developer),after.recordMesgs.map(::developer))
        assertEquals(before.sessionMesgs.map(::developer),after.sessionMesgs.map(::developer))
    }
    @Test fun preRefactorJournalStillDecodesWithUnknownAndZeroMeasurementsDistinct() {
        val file=File(RuntimeEnvironment.getApplication().cacheDir,"legacy-recording.ride")
        reference("legacy-recording.ride").use { input -> file.outputStream().use { input.copyTo(it) } }
        RideJournal.read(file) { start,config,fields,records ->
            assertEquals(1791626400000L,start)
            assertEquals(RecordingConfig(setOf("SPEED","RIDER_POWER","HEART_RATE")),config)
            val expected=reference("recording-fields.txt").bufferedReader().use { it.readLines() }
                .map { it.split('|') }.filter { it[5]=="RECORD" && it[0]!="GPS_POSITION" }.map { it[0] }+"GPS_ALTITUDE_MSL"
            assertEquals(expected,fields)
            val samples=records.toList()
            assertEquals(listOf(0L,1000L),samples.map { it.elapsedMs })
            assertEquals(listOf(7.0,0.0),samples.map { it.values["SPEED"] })
            assertEquals(listOf(180.0,0.0),samples.map { it.values["RIDER_POWER"] })
            assertEquals(listOf(130.0,135.0),samples.map { it.values["HEART_RATE"] })
            assertTrue(samples.all { "MOTOR_POWER" !in it.values })
        }
    }
    @Test fun currentRangeMappingAgreesForDashboardAndRecordingIncludingOffAndMissingNames() {
        fun check(mode:Long?,names:List<String>?,ranges:List<Long>?,expected:Long?) {
            val data=LiveData(otherValues=mode?.let { mapOf(LiveMetric.ASSIST_MODE to it) }.orEmpty(),assistNames=names,rangesKm=ranges)
            val display=DashboardField("CURRENT_RANGE").value(data)
            assertEquals(expected!=null,display.available)
            assertEquals(expected?.toString() ?: "—",display.text)
            assertEquals(expected?.toDouble(),recordingValues(data,GpsValues(),null,PulseValues())["CURRENT_RANGE"])
        }
        check(2,listOf("OFF","ECO","AUTO"),listOf(135,87),87)
        check(0,listOf("OFF","ECO","AUTO"),listOf(135,87),null)
        check(1,listOf("ECO","AUTO"),listOf(135,87),87)
        check(0,listOf("ECO","AUTO"),listOf(0,87),0)
        check(1,null,listOf(135,87),null)
        check(1,listOf("ECO"),listOf(135,87),null)
        check(null,listOf("ECO","AUTO"),listOf(135,87),null)
        check(9,listOf("ECO","AUTO"),listOf(135,87),null)
        check(1,listOf("ECO","AUTO"),null,null)
    }
}
