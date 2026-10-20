package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DreeveImportPerformanceTest {
    @Test fun importedPowerTimeSeriesCalculateNpFtpEfWorkAndBestEffortsAfterReload() {
        val context=RuntimeEnvironment.getApplication()
        val root=File(context.cacheDir,"dreeve-performance-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val library=GpxLibrary(root).apply { load() }
            val start=Instant.parse("2026-10-20T10:00:00Z")
            val xml=buildString {
                append("<gpx xmlns='http://www.topografix.com/GPX/1/1' xmlns:tpx='http://www.garmin.com/xmlschemas/TrackPointExtension/v1'><trk><trkseg>")
                for(second in 0..1200) {
                    val watts=if(second<600) 100 else 300
                    append("<trkpt lat='${50+second*.00001}' lon='8'><time>${start.plusSeconds(second.toLong())}</time><extensions><power>$watts</power><tpx:TrackPointExtension><tpx:hr>150</tpx:hr><tpx:cad>90</tpx:cad></tpx:TrackPointExtension></extensions></trkpt>")
                }
                append("</trkseg></trk></gpx>")
            }
            val activity=DreeveActivity("performance-ride","20 Minuten","2026-10-20T10:00:00",null,"Ride",true)
            val importer=DreeveImport(context,library,{ library.entries.map { it.id }.toSet() }) { _,file -> file.writeText(xml) }
            assertEquals(1,importer.read("https://performance.example/",listOf(activity)).count(DreeveImportState.IMPORTED))
            val reloaded=GpxLibrary(root).apply { load() }
            val ride=reloaded.entries.single();val file=reloaded.file(ride.id)
            val before=file.readBytes()
            val entry=ManagedRoute(ride.id,ride.name,false,"GPX",ride.details,true,ride.origin)
            val analysis=RouteAnalysisRead.read(file,entry)
            val np=analysis.metrics.getValue(AnalysisMetric.NP)
            assertEquals(200.0,analysis.metrics.getValue(AnalysisMetric.AVG_POWER),.001)
            assertTrue(np in 245.0..260.0) // Variable effort must not collapse NP to average power.
            assertEquals(190.0,analysis.metrics.getValue(AnalysisMetric.FTP),.001)
            assertEquals(np/150,analysis.metrics.getValue(AnalysisMetric.EF),.001)
            assertEquals(240.0,analysis.metrics.getValue(AnalysisMetric.WORK),.001)
            assertTrue(analysis.metrics.getValue(AnalysisMetric.RATIO)>0)
            assertTrue(analysis.metrics.getValue(AnalysisMetric.CALORIES)>0)
            assertTrue(analysis.bestEfforts.isNotEmpty())
            assertArrayEquals(before,file.readBytes())
        } finally { root.deleteRecursively() }
    }
}
