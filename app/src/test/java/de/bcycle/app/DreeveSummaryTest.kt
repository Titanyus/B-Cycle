package de.bcycle.app

import android.os.Looper
import android.widget.TextView
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.ExecutorService

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DreeveSummaryTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val target="https://dreeve.example/"
    private fun row()=JSONObject().put("id","activity-indoor").put("name","Indoor / Matrix").put("startDateLocal","2026-10-20T10:00:00")
        .put("sportType","VirtualRide").put("hasGpx",false).put("startLatLng",JSONObject.NULL).put("distanceInMeter",12000)
        .put("elapsedTimeInSeconds",2400).put("movingTimeInSeconds",2300).put("averageSpeedInKmPerHour",18.7).put("maxSpeedInKmPerHour",28)
        .put("averagePower",140).put("maxPower",300).put("averageHeartRate",130).put("maxHeartRate",155).put("averageCadence",85)
        .put("calories",400).put("kilojoules",350).put("gearId","gear-matrix").put("deviceName","Matrix")
    private fun page(row:JSONObject)=JSONObject().put("activities",JSONArray().put(row))
        .put("pagination",JSONObject().put("page",1).put("size",50).put("total",1).put("totalPages",1))
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+10_000_000_000L
        while(!condition() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5) }
        shadowOf(Looper.getMainLooper()).idle();assertTrue(condition())
    }
    @Test fun activityWithoutGpxCarriesAggregatesButNoInventedTrainingMetrics() {
        val activity=DreeveActivityPage.decode(page(row()),1).activities.single()
        assertFalse(activity.hasGpx);assertEquals(BikeType.INDOOR,activity.bikeType)
        assertEquals(12.0,activity.summary.metrics[AnalysisMetric.DISTANCE]!!,0.0)
        assertEquals(140.0,activity.summary.metrics[AnalysisMetric.AVG_POWER]!!,0.0)
        assertEquals("gear-matrix",activity.summary.gearId);assertEquals("Matrix",activity.summary.device)
        val missing=row().put("averagePower",JSONObject.NULL).put("maxPower",-1).put("averageCadence",0).put("averageHeartRate","invalid")
        val sparse=DreeveActivityPage.decode(page(missing),1).activities.single().summary.metrics
        assertFalse(AnalysisMetric.AVG_POWER in sparse);assertFalse(AnalysisMetric.MAX_POWER in sparse);assertFalse(AnalysisMetric.AVG_HEART in sparse)
        assertEquals(0.0,sparse[AnalysisMetric.AVG_CADENCE]!!,0.0)
        assertFalse(AnalysisMetric.NP in sparse);assertFalse(AnalysisMetric.FTP in sparse)
    }
    @Test fun summaryIsAtomicPersistentDistinctFromGpxAndDeduplicatedWithoutDownload() {
        val root=File(c.cacheDir,"summaries").apply { mkdirs() };val gpx=GpxLibrary(root);gpx.load()
        val summaries=DreeveSummaryLibrary(root);summaries.load()
        val activity=DreeveActivityPage.decode(page(row()),1).activities.single()
        val runner=DreeveImport(c,gpx,{ summaries.entries.map { it.id }.toSet() },summaries) { _,_->fail("No GPX request allowed") }
        val first=runner.read(target,listOf(activity));assertTrue(first.items.single().summaryOnly)
        val item=summaries.entries.single();val file=summaries.file(item.id)
        assertTrue(file.isFile);assertTrue(gpx.entries.isEmpty());assertFalse(RouteUploads.eligible(c,item.id))
        assertEquals(1,runner.read(target,listOf(activity)).count(DreeveImportState.DUPLICATE))
        val restored=DreeveSummaryLibrary(root);restored.load()
        assertEquals(activity.summary,restored.entries.single().activity.summary)
        assertEquals(item.details.dateMs,restored.entries.single().details.dateMs)
        restored.rename(item.id,"Neuer Name");assertEquals("Neuer Name",DreeveSummaryLibrary.read(file).activity.name)
        val analysis=RouteAnalysisRead.read(file,restored.entries.single().managed())
        assertEquals(300.0,analysis.metrics[AnalysisMetric.MAX_POWER]!!,0.0)
        assertNull(analysis.route);assertTrue(analysis.series.isEmpty());assertTrue(analysis.bestEfforts.isEmpty())
        assertNull(analysis.zones.power);assertNull(analysis.zones.heart);assertTrue(analysis.distributions.isEmpty());assertNull(analysis.weatherTarget)
        val view=RouteAnalysisView(c,ThemeManager(c.getSharedPreferences("bike",0)),item.id,{})
        view.show(analysis)
        assertTrue(view.findViewWithTag<TextView>("analysis_summary_notice").text.contains("Zusammenfassung"));view.dispose()
        restored.delete(item.id);assertFalse(file.exists());restored.load();assertTrue(restored.entries.isEmpty())
    }
    @Test fun managerImportsListsAnalysesRenamesAndDeletesAnIndoorSummaryWithoutGpxCall() {
        val server=MockWebServer();server.start();val state=MapState(c,{})
        try {
            await { !state.busy }
            server.enqueue(MockResponse().setBody(page(row()).toString()))
            val api=DreeveClient(ActivityServiceConfig(),"test",base=server.url("/"))
            val activity=api.activities().activities.single()
            assertEquals("/api/v1/activities",server.takeRequest().requestUrl!!.encodedPath)
            var imported:Result<DreeveImportResult>?=null
            state.importDreeve(target,listOf(activity),api,{false},{},{ imported=it })
            await { imported!=null && !state.busy };assertEquals(1,imported!!.getOrThrow().count(DreeveImportState.IMPORTED))
            val entry=state.entries.single()
            assertEquals("JSON",entry.format);assertTrue(entry.locallySaved);assertFalse(entry.hasRoute);assertEquals(TrackOrigin.IMPORTED,entry.origin)
            assertEquals(12000.0,entry.details.distanceM!!,0.0);assertEquals(2400000L,entry.details.durationMs);assertEquals(2300000L,entry.details.movingMs)
            assertNull(state.route);assertNull(state.selectedRoute);assertEquals(setOf(activity.id),state.existingDreeve(target,listOf(activity)))
            var analysis:Result<RouteAnalysis>?=null
            state.loadAnalysis(entry.id) { analysis=it };await { analysis!=null }
            assertEquals(130.0,analysis!!.getOrThrow().metrics[AnalysisMetric.AVG_HEART]!!,0.0)
            assertTrue(analysis!!.getOrThrow().series.isEmpty())
            val rows=RouteTransferRows.read(c,entry,false)
            assertTrue(rows.single { it.target==RouteTransferTarget.LOCAL }.canStart)
            assertTrue(rows.filter { it.target!=RouteTransferTarget.LOCAL }.none { it.canStart })
            state.renameRoute(entry.id,"Indoor neu");await { !state.busy };assertEquals("Indoor neu",state.entries.single().name)
            state.deleteRoute(entry.id);await { !state.busy };assertTrue(state.entries.isEmpty());assertTrue(RideServiceLinks.links(c,entry.id).isEmpty())
            assertEquals(1,server.requestCount)
        } finally {
            (MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as ExecutorService).shutdownNow()
            server.shutdown()
        }
    }
}
