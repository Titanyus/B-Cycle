package de.bcycle.app

import android.os.Looper
import android.widget.Button
import android.widget.Toast
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RecordingUpdateUiTest {
    private fun drainLibrary(runtime:RideRuntime) {
        val worker=MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(runtime.mapState) as ExecutorService
        worker.submit {}.get(10,TimeUnit.SECONDS)
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun assertSummaryShown() {
        shadowOf(Looper.getMainLooper()).idle()
        val dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(dialog?.window?.decorView?.findViewWithTag<android.view.View>("ride_summary"))
        assertNotNull(dialog?.window?.decorView?.findViewWithTag<android.view.View>("ride_summary_transfer_divider"))
    }
    @Test fun runtimeRefreshesLibraryOnCompletionAndRecoveryButNotProgressOrWriteNotices() {
        val runtime=RideRuntime.get(RuntimeEnvironment.getApplication())
        drainLibrary(runtime)
        val initial=runtime.mapState.routeRevision
        assertEquals("Bereit · START beginnt die Aufzeichnung",runtime.recordingStatus)
        RecordingUpdate.Progress.entries.forEach {
            runtime.recordingChanged(it)
            assertTrue(runtime.saving)
            assertEquals(it,runtime.recordingUpdate)
        }
        RecordingUpdate.Notice.entries.forEach {
            runtime.recordingChanged(it)
            assertFalse(runtime.saving)
        }
        drainLibrary(runtime)
        assertEquals(initial,runtime.mapState.routeRevision)
        val outcomes=listOf(RideCompletion.Completed("ride"),RideCompletion.LocallyRetained("ride",RecordingFinishStage.EXPORT_LOCAL),
            RideCompletion.NotSaved(RecordingFinishStage.ENCODE_FIT))
        outcomes.forEachIndexed { index,result ->
            runtime.recordingChanged(RecordingUpdate.Finished(result))
            assertFalse(runtime.saving)
            drainLibrary(runtime)
            assertEquals(initial+index+1,runtime.mapState.routeRevision)
        }
        runtime.recordingChanged(RecordingUpdate.Recovered(outcomes));drainLibrary(runtime)
        assertEquals(initial+4,runtime.mapState.routeRevision)
        assertEquals("1 von 3 Fahrten gespeichert · übrige Daten bleiben erhalten",runtime.recordingStatus)
    }
    @Test fun activityPreservesBusyButtonProfileLocksAndLongFeedbackWithoutRepeatedToasts() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=host.get();val runtime=RideRuntime.get(activity)
            drainLibrary(runtime)
            val button=activity.window.decorView.findViewWithTag<Button>("record_bike_button")
            val other=runtime.bikes.add()!!
            val initialToast=ShadowToast.getLatestToast()
            runtime.recordingChanged(RecordingUpdate.Progress.SAVING_FIT)
            assertFalse(button.isEnabled)
            assertFalse(runtime.switchBike(other.slot))
            assertSame(initialToast,ShadowToast.getLatestToast())
            runtime.recordingChanged(RecordingUpdate.Notice.WRITE_FAILED)
            assertTrue(button.isEnabled)
            assertEquals("Fehler beim Schreiben · Zwischendatei bleibt erhalten",ShadowToast.getTextOfLatestToast())
            val errorToast=ShadowToast.getLatestToast()
            assertEquals(Toast.LENGTH_LONG,errorToast.duration)
            repeat(3) { runtime.notifyChanged() }
            assertSame(errorToast,ShadowToast.getLatestToast())
            runtime.recordingChanged(RecordingUpdate.Progress.SAVING_FIT)
            assertSame(errorToast,ShadowToast.getLatestToast())
            runtime.recordingChanged(RecordingUpdate.Finished(RideCompletion.Completed("ride")))
            assertTrue(button.isEnabled)
            assertSame(errorToast,ShadowToast.getLatestToast())
            assertSummaryShown()
            assertTrue(runtime.switchBike(other.slot))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun activityKeepsQuietNoticesQuietAndShowsPartialFailedAndRecoveryResults() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val runtime=RideRuntime.get(host.get());drainLibrary(runtime)
            val initial=ShadowToast.getLatestToast()
            runtime.recordingChanged(RecordingUpdate.Notice.NO_JOURNAL)
            runtime.recordingChanged(RecordingUpdate.Notice.NO_PENDING)
            assertSame(initial,ShadowToast.getLatestToast())
            runtime.recordingChanged(RecordingUpdate.Finished(RideCompletion.LocallyRetained("ride",RecordingFinishStage.QUEUE_UPLOADS)))
            assertSame(initial,ShadowToast.getLatestToast())
            assertSummaryShown()
            runtime.recordingChanged(RecordingUpdate.Finished(RideCompletion.NotSaved(RecordingFinishStage.ENCODE_FIT)))
            assertEquals("FIT nicht gespeichert · Daten bleiben erhalten. Im Menü erneut speichern.",ShadowToast.getTextOfLatestToast())
            runtime.recordingChanged(RecordingUpdate.Recovered(listOf(RideCompletion.Completed("ride"),RideCompletion.NotSaved(RecordingFinishStage.ENCODE_FIT))))
            assertEquals("1 von 2 Fahrten gespeichert · übrige Daten bleiben erhalten",ShadowToast.getTextOfLatestToast())
        } finally { host.pause().stop().destroy() }
    }
}
