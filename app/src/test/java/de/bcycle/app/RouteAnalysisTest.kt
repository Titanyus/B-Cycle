package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteAnalysisTest {
    private val start = 1_791_626_400_000L
    private fun entry(format: String) = ManagedRoute("selected-id", "My ride", true, format, RouteDetails(), true)
    @Test fun officialFitNativeAndDeveloperSummariesKeepRiderPowerAndRawGapsAndPositiveCadence() {
        val root = Files.createTempDirectory("analysis").toFile()
        try {
            val config = RecordingConfig(RecordingConfig().fields + RecordField.summaries.map { it.id })
            val journal = java.io.File(root, "ride.ride")
            RideJournal(journal, start, config).use { writer ->
                for (i in 0..60) {
                    val at = i * 1000L
                    val raw = mutableMapOf("SPEED" to 10.0, "BIKE_DISTANCE" to i * 10.0, "RIDER_POWER" to 200.0,
                        "MOTOR_POWER" to 800.0, "CADENCE" to if (i < 30) 90.0 else 0.0, "HEART_RATE" to 150.0,
                        "GPS_ALTITUDE" to 600.0 + i, "GPS_ASCENT" to i.toDouble(), "BATTERY" to 80.0 - i * .5)
                    if (i == 40) raw.remove("RIDER_POWER")
                    if (i == 41) raw["RIDER_POWER"] = 0.0
                    writer.append(RideSample(start + at, at, 50.0 + i * .0001, 8.0, raw, performanceEvents = listOf(
                        PerformanceEvent(PerformanceChannel.POWER, at, 200.0, source = PowerSource.BOSCH_RIDER.name),
                        PerformanceEvent(PerformanceChannel.HEART_RATE, at, 150.0), PerformanceEvent(PerformanceChannel.SPEED, at, 10.0))))
                }
            }
            val file = java.io.File(root, "ride.fit"); FitWriter.encode(journal, file)
            val original = file.readBytes()
            val analysis = RouteAnalysisRead.read(file, entry("FIT"))
            val withZones = RouteAnalysisRead.read(file, entry("FIT"), profile = RiderProfile(ftpW = 200, restingHeartRate = 60, maximumHeartRate = 160))
            assertEquals(590.0, withZones.zones.power!!.totalMeters, .001)
            assertEquals(10.0, withZones.zones.power.meters[0], .001)
            assertEquals(580.0, withZones.zones.power.meters[3], .001)
            assertEquals(600.0, withZones.zones.heart!!.meters[4], .001)
            assertEquals(RouteZoneDistributions(), analysis.zones)
            assertEquals("selected-id", analysis.entry.id)
            assertEquals(800.0, analysis.metrics.getValue(AnalysisMetric.AVG_MOTOR), .001)
            assertEquals(11600.0 / 47200.0, analysis.metrics.getValue(AnalysisMetric.RIDER_BIKE_RATIO), .000001)
            assertEquals(30.0, analysis.metrics.getValue(AnalysisMetric.BATTERY_USAGE), .001)
            assertEquals(start, analysis.entry.details.dateMs)
            assertEquals(90.0, analysis.metrics.getValue(AnalysisMetric.AVG_CADENCE), .001)
            assertEquals(200.0, analysis.metrics.getValue(AnalysisMetric.NP), .001)
            assertEquals(12.0, analysis.metrics.getValue(AnalysisMetric.WORK), .001)
            assertEquals(12.0, analysis.metrics.getValue(AnalysisMetric.CALORIES), .001)
            assertEquals(4.0 / 3, analysis.metrics.getValue(AnalysisMetric.EF), 1e-9)
            assertEquals(.18, analysis.metrics.getValue(AnalysisMetric.RATIO), 1e-9)
            assertEquals(60.0, analysis.metrics.getValue(AnalysisMetric.MOVING), .001)
            assertEquals(.6, analysis.metrics.getValue(AnalysisMetric.DISTANCE), .001)
            assertEquals(36.0, analysis.metrics.getValue(AnalysisMetric.AVG_SPEED), .001)
            assertEquals(AnalysisChart.entries, analysis.series.map { it.chart })
            val power = analysis.series.single { it.chart == AnalysisChart.POWER }.points
            assertNull(power[40].value); assertEquals(0.0, power[41].value!!, 0.0)
            assertFalse(power.any { it.value == 800.0 })
            assertEquals(.6, analysis.axisEnd, .001); assertTrue(analysis.distanceAxis)
            assertArrayEquals(original, file.readBytes())
            assertNotNull(analysis.route)
        } finally { root.deleteRecursively() }
    }
    @Test fun fitRecalculationRetainsConfirmedSilentZeroButGpsOnlyOrMissingPositivePowerRemainUnknown() {
        val root=Files.createTempDirectory("analysis-coasting").toFile()
        try {
            fun fixture(name:String,bike:Boolean,zero:Boolean):java.io.File {
                val fields=setOf("RIDER_POWER","HEART_RATE",if(bike) "SPEED" else "GPS_SPEED")
                val config=RecordingConfig(fields,speedSource=if(bike) "SPEED" else "GPS_SPEED")
                val journal=java.io.File(root,"$name.ride")
                RideJournal(journal,start,config).use { writer->
                    for(second in 0..300) {
                        val at=second*1000L
                        val values=mutableMapOf((if(bike) "SPEED" else "GPS_SPEED") to 10.0,"HEART_RATE" to 150.0)
                        if(second<60 || second>=240) values["RIDER_POWER"]=200.0
                        else if(second==60 && zero) values["RIDER_POWER"]=0.0
                        writer.append(RideSample(start+at,at,null,null,values))
                    }
                }
                return java.io.File(root,"$name.fit").also { FitWriter.encode(journal,it) }
            }
            val file=fixture("bike",true,true);val before=file.readBytes()
            val good=RouteAnalysisRead.read(file,entry("FIT"))
            assertNotNull(good.metrics[AnalysisMetric.NP]);assertNotNull(good.metrics[AnalysisMetric.WORK])
            assertNotNull(good.metrics[AnalysisMetric.EF]);assertNotNull(good.metrics[AnalysisMetric.CALORIES])
            assertEquals(24.0,good.metrics.getValue(AnalysisMetric.WORK),.001)
            val raw=good.series.single { it.chart==AnalysisChart.POWER }.points
            assertTrue(raw.any { it.seconds != null && it.seconds in 61.0..239.0 && it.value==null })
            assertFalse(raw.any { it.seconds != null && it.seconds in 61.0..239.0 && it.value==0.0 })
            assertArrayEquals(before,file.readBytes())
            assertNull(RouteAnalysisRead.read(fixture("gps",false,true),entry("FIT")).metrics[AnalysisMetric.NP])
            assertNull(RouteAnalysisRead.read(fixture("unknown",true,false),entry("FIT")).metrics[AnalysisMetric.NP])
        } finally { root.deleteRecursively() }
    }

    @Test fun gpxElevationWithoutTimeHasSharedDistanceAndNoInventedSensorData() {
        val file = Files.createTempFile("analysis", ".gpx").toFile()
        try {
            file.writeText("""<gpx><trk><trkseg><trkpt lat="50" lon="8"><ele>600</ele></trkpt><trkpt lat="50.001" lon="8"><ele>606</ele></trkpt><trkpt lat="50.002" lon="8"><ele>600</ele></trkpt></trkseg></trk></gpx>""")
            val a = RouteAnalysisRead.read(file, entry("GPX"))
            assertEquals(listOf(AnalysisChart.ELEVATION), a.series.map { it.chart })
            assertEquals(6.0, a.metrics.getValue(AnalysisMetric.ASCENT), .001)
            assertEquals(6.0, a.metrics.getValue(AnalysisMetric.DESCENT), .001)
            assertFalse(a.metrics.containsKey(AnalysisMetric.NP)); assertFalse(a.metrics.containsKey(AnalysisMetric.MOVING))
            assertTrue(a.distanceAxis); assertTrue(a.axisEnd > .2)
            assertEquals(a.metrics.getValue(AnalysisMetric.DISTANCE), a.axisEnd, .00001)
        } finally { file.delete() }
    }
    @Test fun noDistanceUsesOneTimeAxisAndMissingChartsDisappear() {
        val root = Files.createTempDirectory("analysis-time").toFile()
        try {
            val journal = java.io.File(root, "ride.ride")
            RideJournal(journal, start, RecordingConfig(setOf("HEART_RATE"))).use { writer ->
                for (i in 0..10) writer.append(RideSample(start + i * 1000, i * 1000L, values = mapOf("HEART_RATE" to 140.0)))
            }
            val file = java.io.File(root, "ride.fit"); FitWriter.encode(journal, file)
            val a = RouteAnalysisRead.read(file, entry("FIT"))
            assertFalse(a.distanceAxis); assertEquals(10.0, a.axisEnd, .001)
            assertEquals(listOf(AnalysisChart.HEART), a.series.map { it.chart }); assertNull(a.route)
            assertFalse(a.metrics.containsKey(AnalysisMetric.WORK))
        } finally { root.deleteRecursively() }
    }
    @Test fun longSeriesAreBoundedPreservePeaksAndNeverTurnMissingIntoZero() {
        val buffer = AnalysisSeriesBuffer()
        for (i in 0 until 100_000) buffer.add(AnalysisPoint(i.toDouble(), i.toDouble(), if (i % 17 == 0) null else if (i == 7777) 999.0 else 50.0))
        val points = buffer.points()
        assertTrue("size=${points.size}", points.size <= 4096)
        assertTrue(points.any { it.value == null }); assertFalse(points.any { it.value == 0.0 })
        assertEquals(999.0, points.mapNotNull { it.value }.max(), 0.0)
        assertTrue(points.zipWithNext().all { it.first.seconds!! <= it.second.seconds!! })
    }
    @Test fun continuousLongSeriesRemainConnectedAfterReductionButActualTimeGapsBreak() {
        val buffer = AnalysisSeriesBuffer()
        for (i in 0 until 50_000) buffer.add(AnalysisPoint(i.toDouble(), i.toDouble(), (i % 300).toDouble()))
        val points = buffer.points()
        assertTrue(points.size <= 4096)
        assertFalse(points.any { it.value == null })
        // Reduction creates large time intervals; those are not sensor outages.
        assertTrue(points.zipWithNext().any { it.second.seconds!! - it.first.seconds!! > 10 })
        val file = Files.createTempFile("analysis-gap", ".gpx").toFile()
        try {
            file.writeText("""<gpx><rte><rtept lat="50" lon="8"><ele>600</ele><time>2026-10-12T10:00:00Z</time></rtept><rtept lat="50.001" lon="8"><ele>605</ele><time>2026-10-12T10:01:00Z</time></rtept></rte></gpx>""")
            val a = RouteAnalysisRead.read(file, entry("GPX"))
            val height = a.series.single { it.chart == AnalysisChart.ELEVATION }.points
            assertTrue(height.any { it.value == null })
            assertEquals(605.0, height.last().value!!, .001)
        } finally { file.delete() }
    }
    @Test fun cancelledOrCorruptFilesNeverProduceAnAnalysis() {
        val file = Files.createTempFile("analysis-invalid", ".fit").toFile()
        try {
            file.writeText("not a FIT original")
            assertTrue(runCatching { RouteAnalysisRead.read(file, entry("FIT")) }.isFailure)
            file.writeText("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='51' lon='8'/></rte></gpx>")
            assertTrue(runCatching { RouteAnalysisRead.read(file, entry("GPX")) { true } }.isFailure)
        } finally { file.delete() }
    }
}
