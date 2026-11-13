package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class DashboardEfficiencyTest {
    @Test fun graphDemandUsesAllGraphChoicesAndTemplateWithoutRebuildingEqualSnapshots() {
        val cache=DashboardGraphDemand()
        val config=GraphOverlayConfig().replace(2,listOf(AnalysisChart.POWER)).withWindow(2,GraphWindow.SECONDS_30)
            .replace(0,listOf(AnalysisChart.HEART)).withWindow(0,GraphWindow.SECONDS_30)
        val original=cache.get(DashboardTemplate.FULL_FOUR,config)
        assertEquals(mapOf(GraphWindow.SECONDS_30 to setOf(AnalysisChart.POWER,AnalysisChart.HEART)),original)
        assertSame(original,cache.get(DashboardTemplate.FULL_FOUR,config.copy(slots=config.slots.map { it.toList() })))
        assertTrue(cache.get(DashboardTemplate.MAP,config).isEmpty())
        assertEquals(original,cache.get(DashboardTemplate.FULL_FOUR,config))
        val window=config.withWindow(2,GraphWindow.SECONDS_60)
        assertEquals(mapOf(GraphWindow.SECONDS_30 to setOf(AnalysisChart.HEART),GraphWindow.SECONDS_60 to setOf(AnalysisChart.POWER)),cache.get(DashboardTemplate.FULL_FOUR,window))
        assertEquals(setOf(AnalysisChart.SPEED),cache.get(DashboardTemplate.FULL_FOUR,window.replace(2,listOf(AnalysisChart.SPEED))).getValue(GraphWindow.SECONDS_60))
        assertTrue(cache.get(DashboardTemplate.FULL_FOUR,GraphOverlayConfig()).isEmpty())
    }
    @Test fun metricTilesIgnoreUnrelatedPacketsButRefreshMissingValuesSourcesGraphsAndTime() {
        val view=DashboardView(RuntimeEnvironment.getApplication()) {}
        var layout=DashboardLayout.defaults
        var data=LiveData(speedHundredths=1234,riderPowerWatts=100)
        var gps=GpsValues(elapsedMs=1000,speedKmh=20.0)
        fun show()=view.show(layout,data,gps,pulse=PulseValues(bpm=120))
        fun tile()=view.findViewWithTag<View>("dashboard_tile_2")
        show();val first=tile().contentDescription
        repeat(3) { data=data.copy(riderPowerWatts=200+it);gps=gps.copy(elapsedMs=2000L+it);show();assertSame(first,tile().contentDescription) }
        data=data.copy(speedHundredths=1350);show();assertTrue(tile().contentDescription.toString().contains("13,5"))
        data=data.copy(speedHundredths=null);show();assertNotEquals(first,tile().contentDescription)
        data=data.copy(speedHundredths=1350,indoor=true);show();assertTrue(tile().contentDescription.toString().contains("Indoor"))
        layout=layout.replace(2,DashboardField("GPS_SPEED"));show();assertTrue(tile().contentDescription.toString().contains("20,0"))
        val same=tile().contentDescription;gps=gps.copy(elapsedMs=3000);show();assertSame(same,tile().contentDescription)
        gps=gps.copy(speedKmh=21.0);show();assertTrue(tile().contentDescription.toString().contains("21,0"))
        layout=layout.replace(2,DashboardField("GPS_ELAPSED_TIME"));show()
        val time=tile().contentDescription;gps=gps.copy(elapsedMs=10000);show();assertNotEquals(time,tile().contentDescription)
        layout=layout.replace(2,DashboardField("ASSIST_MODE"));data=data.copy(otherValues=mapOf(LiveMetric.ASSIST_MODE to 1),assistNames=listOf("OFF","Eco"));show()
        assertTrue(tile().contentDescription.toString().contains("Eco"))
        data=data.copy(assistNames=listOf("OFF","Tour"));show();assertTrue(tile().contentDescription.toString().contains("Tour"))
        val config=GraphOverlayConfig().replace(2,listOf(AnalysisChart.SPEED))
        view.showGraphs(config,null);show()
        val title=testDescendants(tile()).filterIsInstance<TextView>().first()
        assertTrue(title.text is android.text.Spanned)
        view.showDataVisibility(DashboardDataVisibility().replace(2,false));show()
        assertEquals(view.context.getString(R.string.dashboard_no_data),tile().contentDescription)
        view.showDataVisibility(DashboardDataVisibility());show();assertTrue(tile().contentDescription.toString().contains("Tour"))
    }
    @Test fun unchangedTrainingSnapshotAlwaysUsesNaturalZoneStripsDespiteLegacyStyleCalls() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val view=DashboardView(a) {};view.bindEditorTheme(themes);a.setContentView(view);view.setPowerSaving(true)
            val config=ZoneGlowConfig().replace(2,ZoneGlowSource.POWER_CURRENT)
            val snapshot=TrainingSnapshot(RiderProfile(ftpW=200),180.0)
            for(flat in listOf(false,true,false)) {
                themes.setFlatDesign(flat);shadowOf(Looper.getMainLooper()).idle()
                view.showTraining(config,snapshot)
                val background=view.findViewWithTag<View>("dashboard_tile_2").background as android.graphics.drawable.LayerDrawable
                val zone=(0 until background.numberOfLayers).map(background::getDrawable).filterIsInstance<ZoneAmbientGlow>().single()
                assertTrue(zone.flat)
                assertEquals(snapshot.glow(ZoneGlowSource.POWER_CURRENT,false,natural=true),zone.currentColor())
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun coveredDashboardStillHandlesRecordingErrorsAndOffersTheCompletedRideSummary() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        val a=host.get();val runtime=RideRuntime.get(a)
        val nav=MainActivity::class.java.getDeclaredField("dialogs").apply { isAccessible=true }.get(a) as DialogNavigation
        val full=BicycleDialogBuilder(a,runtime.themes.settings,nav).setTitle("Test").setMessage("Test").show()
        try {
            assertTrue(nav.dashboardCovered)
            val button=a.window.decorView.findViewWithTag<android.widget.Button>("record_bike_button")
            runtime.recordingChanged(RecordingUpdate.Progress.SAVING_FIT);assertFalse(button.isEnabled)
            runtime.recordingChanged(RecordingUpdate.Notice.WRITE_FAILED);assertTrue(button.isEnabled)
            assertEquals("Fehler beim Schreiben · Zwischendatei bleibt erhalten",org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
            runtime.recordingChanged(RecordingUpdate.Finished(RideCompletion.Completed("ride")))
            shadowOf(Looper.getMainLooper()).idle()
            val summary=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(summary.window!!.decorView.findViewWithTag<View>("ride_summary"))
            assertFalse(full.isShowing);assertFalse(nav.dashboardCovered)
            summary.dismiss();shadowOf(Looper.getMainLooper()).idle()
        } finally { full.dismiss();host.pause().stop().destroy() }
    }
    @Test fun fullscreenCoverageStopsDashboardPreparationAndReturnsFreshWhileOverlaysStayLive() {
        RuntimeEnvironment.getApplication().getSharedPreferences("bike",0).edit()
            .putString(GraphOverlayConfig.key(0),GraphOverlayConfig().replace(2,listOf(AnalysisChart.POWER)).encode()).commit()
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        val a=host.get();val runtime=RideRuntime.get(a)
        val nav=MainActivity::class.java.getDeclaredField("dialogs").apply { isAccessible=true }.get(a) as DialogNavigation
        val dashboard=testDescendants(a.window.decorView).filterIsInstance<DashboardView>().single()
        val shown=DashboardView::class.java.getDeclaredField("data").apply { isAccessible=true }
        fun packet(speed:Int) {
            runtime.onSamples(listOf(McspSample(LiveMetric.SPEED.address,speed.toLong(),true)))
            runtime.onData(LiveData(speedHundredths=speed),true)
        }
        fun current()=shown.get(dashboard) as LiveData
        val graphVisible=LiveGraphProvider::class.java.getDeclaredField("displayVisible").apply { isAccessible=true }
        val open=mutableListOf<AlertDialog>()
        fun dialog(full:Boolean)=BicycleDialogBuilder(a,runtime.themes.settings,nav,fullscreen=full)
            .setTitle("Test").setMessage("Test").show().also { open+=it;shadowOf(Looper.getMainLooper()).idle() }
        try {
            packet(1200);assertEquals(1200,current().speedHundredths)
            val full=dialog(true);assertTrue(nav.dashboardCovered)
            assertFalse(graphVisible.getBoolean(runtime.liveGraphs))
            val hidden=current()
            packet(1300);packet(1400);assertSame(hidden,current())
            assertEquals(1400,runtime.bikeData.speedHundredths)
            val overlay=dialog(false);packet(1500);assertSame(hidden,current())
            overlay.dismiss();shadowOf(Looper.getMainLooper()).idle();assertTrue(nav.dashboardCovered);assertSame(hidden,current())
            full.dismiss();shadowOf(Looper.getMainLooper()).idle();assertFalse(nav.dashboardCovered)
            assertEquals(1500,current().speedHundredths)
            assertTrue(graphVisible.getBoolean(runtime.liveGraphs))
            val compact=dialog(false);assertFalse(nav.dashboardCovered)
            packet(1600);assertEquals(1600,current().speedHundredths)
            compact.dismiss();shadowOf(Looper.getMainLooper()).idle()
            runtime.setPowerSaving(true)
            val saverFull=dialog(true);val saverHidden=current()
            packet(1700);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertSame(saverHidden,current())
            saverFull.dismiss();shadowOf(Looper.getMainLooper()).idle();assertEquals(1700,current().speedHundredths)
        } finally { open.asReversed().forEach { it.dismiss() };host.pause().stop().destroy() }
    }
}
