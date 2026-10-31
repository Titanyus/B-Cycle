package de.bcycle.app

import com.garmin.fit.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.nio.file.Files
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class FitSplitRecordsTest {
    private val start=1790000000000L
    private fun time(i:Int)=DateTime(java.util.Date(start+i*1000L))
    private fun gps(i:Int)=RecordMesg().apply {
        setTimestamp(time(i));setPositionLat(((50.0+i*.0001)/180*2147483648).roundToInt())
        setPositionLong((8.0/180*2147483648).roundToInt())
    }
    private fun telemetry(i:Int)=RecordMesg().apply {
        setTimestamp(time(i));setDistance(i*10f);setSpeed(10f);setPower(200);setCadence(80.toShort());setAltitude(500f+i)
    }
    private fun file(root:java.io.File,split:Boolean):java.io.File {
        val file=java.io.File(root,if(split) "split.fit" else "full.fit")
        val encoder=FileEncoder(file,Fit.ProtocolVersion.V2_0)
        try {
            encoder.write(FileIdMesg().apply { setType(com.garmin.fit.File.ACTIVITY);setManufacturer(137);setTimeCreated(time(0)) })
            for(i in 0..60) {
                if(split) {
                    // Both legal field orders must reconstruct the same logical sample.
                    if(i%2==0) { encoder.write(gps(i));encoder.write(telemetry(i)) }
                    else { encoder.write(telemetry(i));encoder.write(gps(i)) }
                } else encoder.write(gps(i).apply { telemetry(i).fields.filter { it.num!=253 }.forEach { addField(Field(it)) } })
            }
            encoder.write(SessionMesg().apply { setStartTime(time(0));setTotalDistance(600f);setTotalTimerTime(60f);setTotalElapsedTime(60f) })
        } finally { encoder.close() }
        return file
    }
    @Test fun splitAndCompleteRecordsProduceSameMapMetadataAndAnalysisWithoutChangingOriginal() {
        val root=Files.createTempDirectory("split-fit").toFile()
        try {
            val split=file(root,true);val before=split.readBytes();val complete=file(root,false)
            var raw=0
            val result=FitRead.read(split) { raw++ };val reference=FitRead.read(complete)
            assertEquals(122,raw);assertNotNull(result.route)
            assertEquals(reference.route,result.route);assertEquals(reference.details,result.details)
            fun entry()=ManagedRoute("id","Fahrt",false,"FIT",RouteDetails(),true)
            val analysis=RouteAnalysisRead.read(split,entry());val expected=RouteAnalysisRead.read(complete,entry())
            assertEquals(expected.route,analysis.route);assertEquals(expected.metrics,analysis.metrics)
            assertEquals(expected.series,analysis.series);assertTrue(analysis.distanceAxis)
            assertArrayEquals(before,split.readBytes())
        } finally { root.deleteRecursively() }
    }
    @Test fun overlappingSamplesEventsAndMissingOrDifferentTimestampsAreNotCombined() {
        val result=mutableListOf<Mesg>();val join=FitRecordJoiner(MesgListener { result.add(it) })
        join.onMesg(gps(0));join.onMesg(telemetry(0))
        join.onMesg(gps(0));join.onMesg(telemetry(0));join.flush()
        assertEquals(2,result.size);assertTrue(result.all { it.getFieldDoubleValue("power")==200.0 && it.hasField(0) })
        result.clear()
        join.onMesg(gps(1));join.onMesg(EventMesg());join.onMesg(telemetry(1));join.flush()
        assertEquals(listOf(20,21,20),result.map { it.num })
        result.clear()
        join.onMesg(gps(2));join.onMesg(telemetry(3));join.onMesg(RecordMesg().apply { setPower(20) });join.flush()
        assertEquals(3,result.size);assertNull(result.first().getFieldDoubleValue("power"))
    }
    @Test fun realGpsOutageIsStillASegmentBreakAndSensorOnlyFilesStayWithoutRoute() {
        val root=Files.createTempDirectory("split-gap").toFile()
        try {
            val file=java.io.File(root,"gap.fit");val encoder=FileEncoder(file,Fit.ProtocolVersion.V2_0)
            try {
                for(i in listOf(0,1,20,21)) { encoder.write(gps(i));encoder.write(telemetry(i)) }
            } finally { encoder.close() }
            assertEquals(2,FitRead.read(file).route!!.points.count { it.segmentStart })
            val indoor=java.io.File(root,"indoor.fit");val sensor=FileEncoder(indoor,Fit.ProtocolVersion.V2_0)
            try { for(i in 0..4) sensor.write(telemetry(i)) } finally { sensor.close() }
            assertNull(FitRead.read(indoor).route)
        } finally { root.deleteRecursively() }
    }
    @Test fun telemetryBetweenSlowerGpsUpdatesDoesNotBreakMapOrNativeSpeedTime() {
        val root=Files.createTempDirectory("slow-gps-fit").toFile()
        try {
            val file=java.io.File(root,"different-rates.fit");val encoder=FileEncoder(file,Fit.ProtocolVersion.V2_0)
            try {
                for(i in 0..60) { if(i%2==0) encoder.write(gps(i));encoder.write(telemetry(i)) }
            } finally { encoder.close() }
            val read=FitRead.read(file);assertNotNull(read.route);assertEquals(1,read.route!!.segments)
            assertEquals(60000L,read.details.movingMs)
            val analysis=RouteAnalysisRead.read(file,ManagedRoute("id","Fahrt",false,"FIT",read.details,true))
            assertEquals(1,analysis.route!!.segments)
            assertEquals(60.0,analysis.metrics.getValue(AnalysisMetric.MOVING),.001)
            assertEquals(36.0,analysis.metrics.getValue(AnalysisMetric.AVG_SPEED),.001)
        } finally { root.deleteRecursively() }
    }
    @Test fun sparseCadenceAndHeightUpdatesSurviveOtherTelemetryAndGpsOutageWithoutArtificialGaps() {
        val root=Files.createTempDirectory("sparse-fit-channels").toFile()
        try {
            val file=java.io.File(root,"sparse.fit");val encoder=FileEncoder(file,Fit.ProtocolVersion.V2_0)
            try {
                for(i in 0..60) {
                    if(i<5 || i>45) encoder.write(gps(i))
                    encoder.write(RecordMesg().apply { setTimestamp(time(i));setDistance(i*10f);setSpeed(10f);setPower(200) })
                    if(i%3==0) encoder.write(RecordMesg().apply {
                        setTimestamp(time(i));setPower(200);setCadence((if(i in 18..36) 255 else if(i in 42..48) 0 else 80).toShort())
                    })
                    if(i%5==0) encoder.write(RecordMesg().apply { setTimestamp(time(i));setPower(200);setAltitude(500f+i) })
                }
                encoder.write(SessionMesg().apply { setStartTime(time(0));setTotalDistance(600f);setTotalElapsedTime(60f);setTotalTimerTime(60f) })
            } finally { encoder.close() }
            val read=FitRead.read(file)
            assertEquals(600.0,read.details.distanceM!!,.001);assertEquals(60000L,read.details.movingMs)
            assertEquals(2,read.route!!.segments)
            val analysis=RouteAnalysisRead.read(file,ManagedRoute("id","Fahrt",false,"FIT",read.details,true))
            assertEquals(60.0,analysis.metrics.getValue(AnalysisMetric.MOVING),.001)
            assertEquals(36.0,analysis.metrics.getValue(AnalysisMetric.AVG_SPEED),.001)
            assertFalse(analysis.distanceAxis) // Preserve channel samples without a simultaneous distance counter.
            val height=analysis.series.single { it.chart==AnalysisChart.ELEVATION }
            val cad=analysis.series.single { it.chart==AnalysisChart.CADENCE }
            assertEquals(13,height.points.count { it.value!=null })
            assertEquals(1,RouteChartCurve.prepare(height,false,analysis.axisEnd,300f).size)
            val cadenceRuns=RouteChartCurve.prepare(cad,false,analysis.axisEnd,300f)
            assertEquals(2,cadenceRuns.size) // A prolonged channel outage remains a genuine gap, despite other telemetry.
            assertTrue(cadenceRuns.all { it.size>1 })
            assertTrue(cad.points.any { it.value==0.0 }) // Real coasting is never filled with invented pedalling.
        } finally { root.deleteRecursively() }
    }
    @Test fun existingMissingMapIsRepairedOnceWithoutChangingIdNameDateOriginOrBytes() {
        val root=Files.createTempDirectory("split-index").toFile()
        try {
            val original=file(root,true);val bytes=original.readBytes();val lib=FitLibrary(root).apply { load() }
            val entry=original.inputStream().use { lib.add(it,"Meine Fahrt",false) }!!
            val index=java.io.File(root,"fit-library/index.json")
            val json=JSONObject(index.readText()).put("detailsVersion",1)
            json.getJSONArray("entries").getJSONObject(0).put("hasRoute",false)
            index.writeText(json.toString())
            val reopened=FitLibrary(root).apply { load() };val repaired=reopened.entries.single()
            assertTrue(repaired.hasRoute);assertEquals(entry.id,repaired.id);assertEquals(entry.name,repaired.name)
            assertEquals(entry.origin,repaired.origin);assertEquals(entry.details.dateMs,repaired.details.dateMs)
            assertArrayEquals(bytes,reopened.originalFile(entry.id).readBytes())
            assertEquals(3,JSONObject(index.readText()).getInt("detailsVersion"))
        } finally { root.deleteRecursively() }
    }
}
