package de.bcycle.app

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class DreeveImportProgressTest {
    @Test fun progressIsModalWithOneBorderlessCancelActionAndRetainsCompletedCountWhileCancelling() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0)).settings
            val nav=DialogNavigation();var cancelled=0
            val body=DreeveImportProgressView(c,themes,400) { cancelled++ }
            val dialog=BicycleDialogBuilder(c,themes,nav,fullscreen=false,helpEnabled=false).setView(body).setCancelable(false).create()
            dialog.setCanceledOnTouchOutside(false);dialog.show()
            val decor=dialog.window!!.decorView
            decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.AT_MOST));decor.layout(0,0,360,decor.measuredHeight)
            repeat(123) { body.completed("Fahrt $it") }
            assertEquals(123,body.findViewWithTag<ProgressBar>("dreeve_import_progress_bar").progress)
            assertTrue(body.findViewWithTag<TextView>("dreeve_import_progress_count").text.contains("123 von 400"))
            dialog.onBackPressed();assertTrue(dialog.isShowing)
            val event=MotionEvent.obtain(0,0,MotionEvent.ACTION_OUTSIDE,-10f,-10f,0)
            try { dialog.onTouchEvent(event) } finally { event.recycle() };assertTrue(dialog.isShowing)
            val cancel=body.findViewWithTag<View>("dreeve_import_progress_cancel")
            assertNull(cancel.background)
            cancel.performClick();assertEquals(1,cancelled);assertFalse(cancel.isEnabled);assertTrue(dialog.isShowing)
            body.completed("Letzte Fahrt")
            assertEquals(c.getString(R.string.dreeve_import_cancelling),body.findViewWithTag<TextView>("dreeve_import_progress_name").text.toString())
            assertEquals(124,body.findViewWithTag<ProgressBar>("dreeve_import_progress_bar").progress)
            dialog.dismiss()
        } finally { host.pause().stop().destroy() }
    }
}
