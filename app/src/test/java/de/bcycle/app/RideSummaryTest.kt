package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RideSummaryTest {
    private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun drain(runtime:RideRuntime) {
        val worker=MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(runtime.mapState) as ExecutorService
        worker.submit {}.get(10,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun savedRideAppearsOnceOnDashboardWithDetailValuesAndLiveUploadReceipt() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val c=host.get();val runtime=RideRuntime.get(c);drain(runtime)
            val journal=File(c.cacheDir,"summary.ride").apply {
                RecordingResultTest::class.java.getResourceAsStream("/recording/legacy-recording.ride")!!.use { input -> outputStream().use { input.copyTo(it) } }
            }
            val encoded=File(c.cacheDir,"summary.fit");FitWriter.encode(journal,encoded)
            val original=FitLibrary.retainRecording(c.filesDir,encoded);val id=original.nameWithoutExtension
            RideAnalysisFiles.retain(c.filesDir,id,journal)
            RouteExportSettings(c).write(RouteExportConfig(save=false,webdav=true,server="https://cloud.example.org",user="rider",wifiOnly=false))
            RouteUploads.enqueue(c,original,"summary.fit")
            val completion=RideCompletion.Completed(id,setOf(RecordingDestination.WEBDAV))
            // Finish while backgrounded: show on the next foreground, not underneath settings.
            all(c.findViewById(android.R.id.content)).single { it.contentDescription==c.getString(R.string.ui_menu_open) }.performClick()
            val menu=ShadowAlertDialog.getLatestAlertDialog()
            host.pause().stop();runtime.recordingChanged(RecordingUpdate.Finished(completion))
            assertEquals(completion,runtime.pendingRideSummary)
            host.start().resume();repeat(4) { drain(runtime) }
            val dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing);assertFalse(menu.isShowing)
            val view=dialog.window!!.decorView.findViewWithTag<RideSummaryView>("ride_summary");assertNotNull(view)
            assertNull(runtime.pendingRideSummary)
            val header=dialog.window!!.decorView.findViewWithTag<View>("bicycle_navigation_header")
            assertEquals(runtime.themes.active.tokens.secondary,(header.background as android.graphics.drawable.ColorDrawable).color)
            val rows=(view.getChildAt(0) as ViewGroup)
            assertEquals(3,(rows.getChildAt(0) as ViewGroup).childCount);assertEquals(2,(rows.getChildAt(1) as ViewGroup).childCount)
            var analysis:RouteAnalysis?=null
            runtime.mapState.loadAnalysis(id) { analysis=it.getOrThrow() };drain(runtime)
            val tiles=all(view).filterIsInstance<AnalysisMetricTile>()
            val battery=if(analysis!!.entry.details.bikeType==BikeType.EBIKE) listOf(AnalysisMetric.SHIFT_BATTERY_LEVEL,AnalysisMetric.BATTERY_LEVEL,AnalysisMetric.BATTERY_TEMPERATURE) else emptyList()
            assertEquals(listOf(AnalysisMetric.DISTANCE,AnalysisMetric.MOVING,AnalysisMetric.AVG_SPEED,AnalysisMetric.AVG_HEART,AnalysisMetric.NP)+battery,tiles.map { it.alternatives.single() })
            tiles.forEach { tile -> assertEquals(AnalysisMetricTile(c,runtime.themes,tile.alternatives,analysis!!.metrics).contentDescription,tile.contentDescription) }
            val status=view.findViewWithTag<android.widget.TextView>("ride_summary_status")
            assertEquals(View.GONE,status.visibility)
            fun upload()=view.findViewWithTag<View>("ride_summary_state_WEBDAV").getTag(R.id.summary_transfer_state)
            assertEquals(UploadState.WAITING,upload())
            assertFalse(RouteUploads.run(c,{false}) { _,_,_,_ -> })
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(UploadState.UPLOADED,upload())
            val dashboard=c.findViewById<View>(android.R.id.content).findViewWithTag<android.widget.FrameLayout>("dashboard_host").getChildAt(0)
            assertTrue(DashboardModal.blurred(dashboard))
            assertEquals(BicycleDesign.dp(c,BicycleDesign.dashboardModalShadow).toFloat(),dialog.window!!.decorView.elevation,0f)
            for(width in listOf(288,328,380)) {
                view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                view.layout(0,0,width,view.measuredHeight)
                assertTrue(view.height<600)
                if(width==328) {
                    val image=Bitmap.createBitmap(width,view.height,Bitmap.Config.ARGB_8888)
                    image.eraseColor(runtime.themes.active.tokens.secondary);view.draw(Canvas(image))
                    File("build/reports/ui/ride-summary.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
                }
            }
            val details=view.findViewWithTag<View>("ride_summary_details")
            assertTrue(details.isEnabled);details.performClick();drain(runtime)
            assertFalse(dialog.isShowing)
            assertFalse(DashboardModal.blurred(dashboard))
            assertNotNull(ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("route_analysis_$id"))
            host.pause().stop().start().resume();repeat(2) { drain(runtime) }
            assertFalse(dialog.isShowing);assertNull(runtime.pendingRideSummary)
            runtime.recordingChanged(RecordingUpdate.Finished(RideCompletion.NotSaved(RecordingFinishStage.ENCODE_FIT)))
            assertNull(runtime.pendingRideSummary)
            runtime.recordingChanged(RecordingUpdate.Recovered(listOf(completion)));drain(runtime)
            assertNull(runtime.pendingRideSummary)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun queuedAndFailedServicesAreNeverReportedAsSuccessfulUploads() {
        val c=RuntimeEnvironment.getApplication()
        val result=RideCompletion.Completed("ride",RecordingDestination.entries.toSet())
        assertEquals("",RideSummaryStatus.warnings(c,result))
        val states=mapOf(RouteTransferTarget.WEBDAV to UploadState.UPLOADED,
            RouteTransferTarget.INTERVALS to UploadState.UPLOADING,RouteTransferTarget.DREEVE to UploadState.WAITING,RouteTransferTarget.STRAVA to UploadState.FAILED)
        val view=RideSummaryTransfers(c,ThemeManager(c.getSharedPreferences("bike",0)))
        view.show(states.map { (target,state) -> SummaryTransfer(target,c.getString(target.title),state) })
        states.forEach { (target,state) -> assertEquals(state,view.findViewWithTag<View>("ride_summary_state_${target.name}").getTag(R.id.summary_transfer_state)) }
        assertTrue(view.findViewWithTag<View>("ride_summary_transfer_STRAVA").contentDescription.contains(c.getString(R.string.route_transfer_failed)))
        val failure=RideSummaryStatus.warnings(c,RideCompletion.LocallyRetained("ride",RecordingFinishStage.EXPORT_LOCAL))
        assertEquals(c.getString(R.string.recording_locally_retained),failure)
    }
}
