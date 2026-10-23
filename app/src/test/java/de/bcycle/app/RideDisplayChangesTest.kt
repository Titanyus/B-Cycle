package de.bcycle.app

import android.os.Looper
import android.os.SystemClock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideDisplayChangesTest {
    private val context get()=RuntimeEnvironment.getApplication()
    @Test fun rollingScaleReturnsToInitialRangeWhileFullRetainsItsPeak() {
        val history=GraphHistoryBuffer()
        for(second in 0..241) history.append(GraphPoint(second*1000L,second*1000L,null,listOf(null,null,if(second==0) 600.0 else 60.0,null,null)))
        val frame=history.frame(1,400)
        assertTrue(frame.series.getValue(AnalysisChart.POWER).range.high>600)
        GraphWindow.entries.filter { it!=GraphWindow.FULL }.forEach {
            assertEquals(GraphRange(0.0,120.0),frame.windows.getValue(it).getValue(AnalysisChart.POWER).range)
        }
        val range=GraphRangeTransition()
        assertEquals(600.0,range.update(GraphRange(0.0,600.0),0).high,0.0)
        assertEquals(600.0,range.update(GraphRange(0.0,120.0),100).high,0.0)
        assertEquals(360.0,range.value(1600).high,0.0)
        assertEquals(120.0,range.value(3100).high,0.0)
        assertFalse(range.running(3100))
        assertEquals(900.0,range.update(GraphRange(0.0,900.0),3200).high,0.0)
    }
    @Test fun batteryPersistsByProfileIdentityAndInvalidValuesNeverReplaceTheLastKnownCharge() {
        val prefs=context.getSharedPreferences("bike-battery-test",0);val a=UUID.randomUUID().toString();val b=UUID.randomUUID().toString()
        BikeBatteryMemory(prefs).remember(a,67)
        val restarted=BikeBatteryMemory(prefs)
        assertEquals(67L,restarted.read(a));assertNull(restarted.read(b))
        restarted.remember(b,25);restarted.remember(a,null);restarted.remember(a,101)
        assertEquals(67L,restarted.read(a));assertEquals(25L,restarted.read(b))
        restarted.remember(a,0);assertEquals(0L,restarted.read(a))
        assertNull(restarted.read(UUID.randomUUID().toString()))
    }
    @Test fun explicitStopDiscardsBelowTenSecondsButKeepsExactlyTenSeconds() {
        RouteExportSettings(context).write(RouteExportConfig(save=false,webdav=false))
        val runtime=RideRuntime.get(context)
        val store=RideRuntime::class.java.getDeclaredField("store").apply { isAccessible=true }.get(runtime) as RecordingStore
        val worker=RecordingStore::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(store) as ExecutorService
        fun drain() { worker.submit {}.get(10,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle() }
        runtime.beginRecording();SystemClock.sleep(9999);runtime.stopRecording();drain()
        assertEquals(RideCompletion.TooShort,(runtime.recordingUpdate as RecordingUpdate.Finished).result)
        assertNull(runtime.pendingRideSummary);assertFalse(runtime.saving)
        assertTrue(File(context.filesDir,"pending-rides").listFiles().orEmpty().isEmpty())
        assertTrue(File(context.filesDir,"fit-library").listFiles { file -> file.extension=="fit" }.orEmpty().isEmpty())
        runtime.beginRecording();SystemClock.sleep(10000);runtime.stopRecording();drain()
        assertTrue((runtime.recordingUpdate as RecordingUpdate.Finished).result is RideCompletion.Completed)
        assertEquals(1,File(context.filesDir,"fit-library").listFiles { file -> file.extension=="fit" }.orEmpty().size)
    }
}
