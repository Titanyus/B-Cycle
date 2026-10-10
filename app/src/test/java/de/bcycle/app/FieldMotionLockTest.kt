package de.bcycle.app

import android.os.SystemClock
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FieldMotionLockTest {
    private fun speed(runtime: RideRuntime, mps: Double) {
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        val now = SystemClock.elapsedRealtime()
        assertTrue(runtime.ride.gps.accept(GpsFix(now, 48.0, 11.0, 3.0, speedMps = mps), now))
    }

    @Test fun movingGpsLocksAndFilteredStandstillUnlocksWhileUnknownRecordingStaysLocked() {
        val runtime = RideRuntime.get(RuntimeEnvironment.getApplication())
        assertTrue(runtime.canEditDataFields)
        speed(runtime, 4.0)
        assertFalse(runtime.canEditDataFields)
        speed(runtime, 0.0)
        assertTrue(runtime.canEditDataFields)
        // Unknown sensor data during an active ride must not be interpreted as standstill.
        RideRuntime::class.java.getDeclaredField("recording").apply { isAccessible = true }.setBoolean(runtime, true)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(6))
        assertFalse(runtime.canEditDataFields)
        speed(runtime, 0.0)
        assertTrue(runtime.canEditDataFields)
    }

    @Test fun dashboardAndMapSelectionsCannotOpenOrCommitAfterMovementStarts() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val runtime = RideRuntime.get(activity)
        val choose = MainActivity::class.java.getDeclaredMethod("chooseField", Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType, kotlin.jvm.functions.Function0::class.java).apply { isAccessible = true }
        for (mapField in listOf(false, true)) {
            speed(runtime, 0.0)
            val before = HashMap(runtime.preferences.all)
            choose.invoke(activity, 0, mapField, null)
            val picker = ShadowAlertDialog.getLatestAlertDialog()
            assertTrue(picker.isShowing)
            selectTestSource(picker, "GPS")
            val value = testDescendants(picker.findViewById<View>(android.R.id.content)).single { it.tag == "GPS_DISTANCE" }
            speed(runtime, 4.0)
            value.performClick()
            assertFalse(picker.isShowing)
            assertEquals(before, runtime.preferences.all)
            choose.invoke(activity, 0, mapField, null)
            assertFalse(picker.isShowing)
        }
        controller.pause().stop().destroy()
    }
}
