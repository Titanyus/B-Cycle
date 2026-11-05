package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import de.bcycle.hrv.Phase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvRepeatWarningTest {
    private fun drain(controller:HrvController) {
        (HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
        shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun manualAndAutomaticStartsWarnAndOnlyUnderstandingStartsTheRepeat() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val controller=RideRuntime.get(a).hrv;drain(controller)
            val store=HrvStore(File(a.filesDir,"hrv-measurements"))
            Hrv4TrainingCsv.preview("date,time,rMSSD,HR\n${LocalDate.now()},07:15,40,60\n".byteInputStream(),ZoneId.systemDefault(),HrvCsvDateOrder.YEAR_MONTH_DAY).entries.forEach(store::importHrv4Training)
            val original=store.daily().single();val originalJson=store.read(original.id).toString()
            fun ready() { controller.packet(android.os.SystemClock.elapsedRealtime(),byteArrayOf(0x16,75,0,4));drain(controller) }
            fun open(auto:Boolean):AlertDialog {
                MainActivity::class.java.getDeclaredMethod("showHrv",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,auto)
                return ShadowAlertDialog.getLatestAlertDialog()
            }
            ready();val measurement=open(false)
            measurement.window!!.decorView.findViewWithTag<View>("hrv_action").performClick();drain(controller)
            var warning=ShadowAlertDialog.getLatestAlertDialog()
            assertNotSame(measurement,warning);assertFalse(controller.busy)
            assertEquals("Verstanden",warning.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
            val decor=warning.window!!.decorView
            decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.AT_MOST));decor.layout(0,0,360,decor.measuredHeight)
            val cancel=warning.getButton(AlertDialog.BUTTON_NEGATIVE);val understood=warning.getButton(AlertDialog.BUTTON_POSITIVE)
            assertEquals(cancel.top,understood.top);assertTrue(cancel.right<=understood.left)
            warning.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();drain(controller)
            assertFalse(controller.busy);assertEquals(originalJson,store.read(original.id).toString())
            measurement.dismiss();ready()
            open(true);drain(controller)
            warning=ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(warning.findViewById<android.widget.TextView>(android.R.id.message))
            assertFalse(controller.busy)
            warning.getButton(AlertDialog.BUTTON_POSITIVE).performClick();drain(controller)
            assertTrue(controller.busy);assertEquals(Phase.REST,controller.state.phase)
            assertEquals("Old value stays stored while the new measurement is running",originalJson,store.read(original.id).toString())
        } finally { host.pause().stop().destroy() }
    }
    @Test fun firstMeasurementDoesNotRequireConfirmation() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val controller=RideRuntime.get(a).hrv;drain(controller)
            controller.packet(android.os.SystemClock.elapsedRealtime(),byteArrayOf(0x16,75,0,4));drain(controller)
            MainActivity::class.java.getDeclaredMethod("showHrv",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,false)
            val measurement=ShadowAlertDialog.getLatestAlertDialog()
            measurement.window!!.decorView.findViewWithTag<View>("hrv_action").performClick();drain(controller)
            assertSame(measurement,ShadowAlertDialog.getLatestAlertDialog());assertTrue(controller.busy)
        } finally { host.pause().stop().destroy() }
    }
}
