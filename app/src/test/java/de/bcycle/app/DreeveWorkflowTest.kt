package de.bcycle.app

import android.view.View
import android.widget.ListView
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DreeveWorkflowTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val themes get()=ThemeManager(c.getSharedPreferences("bike",0))
    private fun page(index:Int,totalPages:Int=2)=JSONObject().put("activities",JSONArray().put(JSONObject().put("id","activity-$index").put("name","Fahrt $index")
        .put("startDateLocal","2026-10-20T10:00:00").put("sportType","Ride").put("hasGpx",false).put("distanceInMeter",index*1000).put("movingTimeInSeconds",3600)))
        .put("pagination",JSONObject().put("page",index).put("size",50).put("totalPages",totalPages).put("total",2))
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+10_000_000_000L
        while(!condition() && System.nanoTime()<end) { org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(5) }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(condition())
    }
    private fun shutdown(state:MapState) { (MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as java.util.concurrent.ExecutorService).shutdownNow() }
    @Test fun bulkDeletePreservesUnselectedRouteAndRemovesOnlySelectedLocalEntries() {
        val library=GpxLibrary(c.filesDir);library.load()
        val text="<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>"
        repeat(3) { library.add(text.byteInputStream(),"Route $it") }
        val ids=library.entries.map { it.id };RideProfileColor.assign(c,ids.first(),BikeProfileStore(c,c.getSharedPreferences("bike",0)).current().slot)
        val state=MapState(c,{})
        try {
            await { !state.busy };assertEquals(ids.last(),state.selectedRoute)
            state.deleteRoutes(ids.take(2).toSet());await { !state.busy }
            assertEquals(listOf(ids.last()),state.entries.map { it.id });assertEquals(ids.last(),state.selectedRoute);assertNotNull(state.route)
            assertNull(RideProfileColor.slot(c,ids.first()));assertFalse(library.file(ids.first()).exists());assertTrue(library.file(ids.last()).exists())
            state.deleteRoutes(setOf(ids.last()));await { !state.busy };assertNull(state.route);assertTrue(state.entries.isEmpty())
        } finally { shutdown(state) }
    }
    @Test fun detailTitleKeepsItsFontSizeAcrossWidthsAndStopsAnimatingWhenDetached() {
        val host=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val title=RideTitleView(host.get());title.text="Eine sehr lange Fahrt mit einem Titel, der nicht in den Header passt"
            host.get().setContentView(title)
            val size=title.textSize
            title.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(64,View.MeasureSpec.EXACTLY));title.layout(0,0,200,64)
            assertEquals(size,title.textSize,0f);assertEquals(android.widget.TextView.AUTO_SIZE_TEXT_TYPE_NONE,title.autoSizeTextType)
            title.measure(View.MeasureSpec.makeMeasureSpec(350,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(64,View.MeasureSpec.EXACTLY));title.layout(0,0,350,64)
            assertEquals(size,title.textSize,0f)
            host.get().setContentView(View(host.get()))
            assertNull(RideTitleView::class.java.getDeclaredField("motion").apply { isAccessible=true }.get(title))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun allPagesAndGearNamesAreLoadedAndInterruptedPaginationCanResume() {
        val server=MockWebServer();server.start()
        try {
            val api=DreeveClient(ActivityServiceConfig(),"key",base=server.url("/"))
            server.enqueue(MockResponse().setBody("{\"gear\":[{\"id\":\"gear-1\",\"name\":\"Matrix\"}]}"))
            assertEquals(mapOf("gear-1" to "Matrix"),api.gears());assertEquals("/api/v1/gear",server.takeRequest().path)
            val first=page(1).put("activities",JSONArray((1..50).map { id -> page(1).getJSONArray("activities").getJSONObject(0).put("id","activity-$id") }))
            server.enqueue(MockResponse().setBody(first.toString()));server.enqueue(MockResponse().setResponseCode(503).setBody("{}"))
            val pages=mutableListOf<DreeveActivityPage>()
            assertThrows(ActivityServiceFailure::class.java) { api.allActivities { pages.add(it) } }
            assertEquals(listOf(1),pages.map { it.page })
            assertEquals("1",server.takeRequest().requestUrl!!.queryParameter("pagination[page]"));server.takeRequest()
            server.enqueue(MockResponse().setBody(page(2).toString()));api.allActivities(2) { pages.add(it) }
            assertEquals(listOf(1,2),pages.map { it.page });assertEquals(51,pages.sumOf { it.activities.size });assertEquals("2",server.takeRequest().requestUrl!!.queryParameter("pagination[page]"))
            var cancelled=false;val cancellable=DreeveClient(ActivityServiceConfig(),"key",{cancelled},server.url("/"))
            server.enqueue(MockResponse().setBody(page(1).toString()))
            assertThrows(java.io.IOException::class.java) { cancellable.allActivities { cancelled=true } }
            assertEquals(5,server.requestCount)
        } finally { server.shutdown() }
    }
    @Test fun importScreenUsesSharedRouteQueryForFilteringAndSortingAndKeepsSelection() {
        val view=DreeveImportView(c,themes,{},{})
        val rows=(1..3).map { DreeveActivityPage.decode(page(it,3),it).activities.single() }
        view.loaded(DreeveActivityPage(rows,1,1,3),emptySet())
        val list=view.findViewWithTag<ListView>("dreeve_import_list")
        val query=DreeveImportView::class.java.getDeclaredField("query").apply { isAccessible=true }.get(view) as RouteQuery
        query.sort=RouteSort.DISTANCE;query.descending=false;query.distance=RouteRange(2.0,3.0)
        DreeveImportView::class.java.getDeclaredMethod("refreshQuery").apply { isAccessible=true }.invoke(view)
        assertEquals(3,list.adapter.count) // two rows plus the native More footer
        assertEquals("activity-2",(list.adapter.getItem(0) as DreeveActivity).id)
        assertEquals("activity-3",(list.adapter.getItem(1) as DreeveActivity).id)
        assertNotNull(view.findViewWithTag<View>("dreeve_filters"));assertNotNull(view.findViewWithTag<View>("dreeve_sort"))
    }
    @Test fun rememberedGearMappingUsesStableProfilesAndIsScopedToTheDreeveInstance() {
        val store=BikeProfileStore(c,c.getSharedPreferences("bike",0));val profile=store.current()
        DreeveBikeMappings.save(c,"https://one.example/",mapOf("gear-1" to profile.slot,"gear-removed" to 999))
        assertEquals(mapOf("gear-1" to profile.slot),DreeveBikeMappings.read(c,"https://one.example/"))
        assertTrue(DreeveBikeMappings.read(c,"https://two.example/").isEmpty())
        RideProfileColor.assign(c,"local-ride",profile.slot)
        assertEquals(profile.slot,RideProfileColor.slot(c,"local-ride"));assertEquals(profile.name,RideProfileColor.name(c,"local-ride"))
    }
    @Test fun bikeAssignmentFormRestoresMappingsAndKeepsUnknownGearUnassigned() {
        val profiles=BikeProfileStore(c,c.getSharedPreferences("bike",0)).all()
        val view=DreeveBikeAssignmentView(c,themes,listOf("known","unknown"),mapOf("known" to "Matrix"),profiles,mapOf("known" to profiles.first().slot))
        assertEquals(profiles.first().slot,view.assignments()["known"]);assertEquals(-1,view.assignments()["unknown"])
        view.findViewWithTag<android.widget.Spinner>("dreeve_gear_unknown").setSelection(1)
        assertEquals(profiles.first().slot,view.assignments()["unknown"])
        assertTrue(DreeveBikeMappings.read(c,"https://example/").isEmpty())
    }
    @Test fun completeLocalAnalysisRetainsEbikeValuesButExportRemainsFiltered() {
        val start=1792483200000L;val source=File(c.cacheDir,"complete.ride")
        RideJournal(source,start,RecordingConfig(setOf("HEART_RATE"))).use { journal ->
            for(second in 0..60) journal.append(RideSample(start+second*1000,second*1000L,50.0+second*.00001,8.0,
                mapOf("HEART_RATE" to 130.0,"RIDER_POWER" to 200.0,"MOTOR_POWER" to 300.0,"BATTERY" to 90.0,"SPEED" to 5.0,"BIKE_DISTANCE" to second*5.0)))
        }
        val export=File(c.cacheDir,"filtered.fit");FitWriter.encode(source,export)
        val original=FitLibrary.retainRecording(c.filesDir,export)
        val id=original.nameWithoutExtension;RideAnalysisFiles.retain(c.filesDir,id,source)
        val entry=ManagedRoute(id,"Test",true,"FIT",RouteDetails(),false)
        val filtered=RouteAnalysisRead.read(export,entry)
        assertNull(filtered.metrics[AnalysisMetric.AVG_MOTOR]);assertNull(filtered.metrics[AnalysisMetric.AVG_POWER]);assertNull(filtered.route)
        val full=RouteAnalysisRead.read(RideAnalysisFiles.file(c.filesDir,id),entry)
        assertEquals(300.0,full.metrics[AnalysisMetric.AVG_MOTOR]!!,0.0)
        assertEquals(200.0,full.metrics[AnalysisMetric.NP]!!,.1)
        assertNotNull(full.metrics[AnalysisMetric.EF]);assertNotNull(full.route)
        val state=MapState(c,{})
        try {
            await { !state.busy }
            val managed=state.entries.single();assertTrue(managed.hasRoute);assertTrue(managed.details.distanceM!!>0)
            var loaded:RouteAnalysis?=null
            state.loadAnalysis(id) { loaded=it.getOrThrow() };await { loaded!=null }
            assertEquals(300.0,loaded!!.metrics[AnalysisMetric.AVG_MOTOR]!!,0.0)
            assertArrayEquals(export.readBytes(),original.readBytes())
        } finally { shutdown(state) }
    }
    @Test fun importedGpxComputesNormalizedPowerAndPerformanceFromActualSamples() {
        val file=File(c.cacheDir,"performance.gpx")
        file.writeText("<gpx><trk><trkseg>"+(0..120).joinToString("") { second ->
            "<trkpt lat='${50+second*.00001}' lon='8'><time>${java.time.Instant.ofEpochMilli(1792483200000+second*1000)}</time><extensions><power>200</power><gpxtpx:TrackPointExtension xmlns:gpxtpx='http://www.garmin.com/xmlschemas/TrackPointExtension/v1'><gpxtpx:hr>130</gpxtpx:hr></gpxtpx:TrackPointExtension></extensions></trkpt>"
        }+"</trkseg></trk></gpx>")
        val analysis=RouteAnalysisRead.read(file,ManagedRoute("gpx","Test",false,"GPX",RouteDetails(),true))
        assertEquals(200.0,analysis.metrics[AnalysisMetric.NP]!!,.1);assertNotNull(analysis.metrics[AnalysisMetric.WORK]);assertNotNull(analysis.metrics[AnalysisMetric.EF])
        assertTrue(analysis.bestEfforts.isNotEmpty());assertTrue(analysis.series.any { it.chart==AnalysisChart.POWER })
    }
    @Test fun longPressEntersMultiselectAndDetailsHaveNoVerticalRules() {
        val a=ManagedRoute("a","A",false,"GPX",RouteDetails(),false)
        val b=ManagedRoute("b","B",false,"GPX",RouteDetails(),false)
        var opened=0;val manager=RouteManagerView(c,{},themes,{ opened++ });manager.submit(listOf(a,b),null,false)
        val list=manager.findViewWithTag<ListView>("route_list")
        val card=list.adapter.getView(0,null,list);card.performLongClick()
        assertTrue(manager.selecting);assertEquals(setOf("a"),manager.checkedIds)
        list.adapter.getView(1,null,list).performClick();assertEquals(setOf("a","b"),manager.checkedIds);assertEquals(0,opened)
        manager.clearSelection();assertFalse(manager.selecting);card.performClick();assertEquals(1,opened)
        val detail=RouteAnalysisView(c,themes,"a",{})
        detail.show(RouteAnalysis(a,mapOf(AnalysisMetric.DISTANCE to 10.0,AnalysisMetric.MOVING to 3600.0,AnalysisMetric.AVG_SPEED to 10.0),null,false,0.0,emptyList()))
        assertNull(detail.findViewWithTag<View>("analysis_vertical_divider"))
        assertEquals(BicycleDesign.dp(c,10),detail.findViewWithTag<View>("analysis_identity_gap").layoutParams.height)
        detail.dispose()
    }
}
