package de.bcycle.app

import java.io.File
import java.nio.file.Files
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class GpxPauseAnalysisTest {
    private fun analyze(file:File):RouteAnalysis {
        val route=file.inputStream().use(GpxImport::read)
        return RouteAnalysisRead.read(file,ManagedRoute("test","Test",false,"GPX",
            RouteDetails(distanceM=route.distanceM,durationMs=route.durationMs),true))
    }
    @Test fun flowStyleStationaryGapIsPauseRatherThanMissingPowerAndKeepsZerosAndElapsedTime() {
        val file=Files.createTempFile("flow-pause-",".gpx").toFile()
        try {
            val start=Instant.parse("2026-01-01T00:00:00Z")
            file.writeText(buildString {
                append("<gpx xmlns:t='http://www.garmin.com/xmlschemas/TrackPointExtension/v1'><trk><trkseg>")
                fun point(second:Int,index:Int,watts:Int) {
                    append("<trkpt lat='0' lon='${index*.00005}'><time>${start.plusSeconds(second.toLong())}</time><extensions><power>$watts</power><t:TrackPointExtension><t:cad>${if(watts==0) 0 else 80}</t:cad></t:TrackPointExtension></extensions></trkpt>")
                }
                for(i in 0..120) point(i,i,if(i>=119) 0 else 200)
                point(1092,120,0)
                for(i in 1..120) point(1092+i,120+i,200)
                append("</trkseg></trk></gpx>")
            })
            val analysis=analyze(file)
            assertEquals(972000,analysis.inferredPauseMs)
            assertEquals(1212.0,analysis.metrics.getValue(AnalysisMetric.ELAPSED),.001)
            assertEquals(240000,analysis.powerQuality!!.activeMs)
            assertEquals(1.0,analysis.powerQuality.powerCoverage,.00001)
            assertEquals(1.0,analysis.powerQuality.npWindowCoverage,.00001)
            assertTrue(analysis.metrics.getValue(AnalysisMetric.NP) in 190.0..200.0)
            assertTrue(analysis.series.single { it.chart==AnalysisChart.POWER }.points.any { it.value==0.0 })
            // Optional local validation of the user-supplied original; no GPS or personal
            // sensor series is checked into the repository or fetched by CI.
            System.getenv("BCYCLE_FLOW_REGRESSION_GPX")?.let { path ->
                val actual=analyze(File(path));val q=actual.powerQuality!!
                assertEquals(1560000,actual.inferredPauseMs)
                assertEquals(7745000,q.activeMs)
                assertEquals(1.0,q.powerCoverage,.00001)
                assertEquals(1.0,q.npWindowCoverage,.00001)
                assertNotNull(actual.metrics[AnalysisMetric.NP])
                println("Flow regression: activeMs=${q.activeMs}, pauseMs=${actual.inferredPauseMs}, powerCoverage=${q.powerCoverage}, windowCoverage=${q.npWindowCoverage}, NP=${actual.metrics[AnalysisMetric.NP]}")
            }
        } finally { file.delete() }
    }
    @Test fun movingCoastingAndUnknownSensorGapsAreNotReclassifiedAsStationaryPauses() {
        fun sample(at:Long,longitude:Double,power:Double?=0.0,cadence:Double?=0.0,speed:Double=0.0)=
            GpxMeasurement(MapPoint(0.0,longitude),at,null,speed,power,cadence,null,null)
        val stationary=GpxPauseInference();stationary.add(sample(0,0.0))
        assertEquals(60000,stationary.add(sample(60000,0.00001))!!.durationMs)
        val moving=GpxPauseInference();moving.add(sample(0,0.0))
        assertNull(moving.add(sample(60000,.01,speed=10.0)))
        val unknown=GpxPauseInference();unknown.add(sample(0,0.0,cadence=null))
        assertNull(unknown.add(sample(60000,0.0)))
        val power=GpxPauseInference();power.add(sample(0,0.0,power=200.0))
        assertNull(power.add(sample(60000,0.0)))
    }
}
