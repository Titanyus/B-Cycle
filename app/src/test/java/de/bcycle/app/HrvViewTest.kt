package de.bcycle.app

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class HrvViewTest {
    private fun all(view:View):List<View> = listOf(view)+if(view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()
    @Test fun heartShortcutSitsNextToStartAndRightControlsAreTenPercentNarrowerWithUsableTouchTargets() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val root=a.findViewById<ViewGroup>(android.R.id.content)
            for(width in listOf(320,360,412)) {
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(850,View.MeasureSpec.EXACTLY));root.layout(0,0,width,850)
                val toolbar=root.findViewWithTag<ViewGroup>("ride_toolbar")
                assertEquals(4,toolbar.childCount);assertEquals("record_bike_button",toolbar.getChildAt(0).tag)
                assertEquals("dashboard_hrv",toolbar.getChildAt(1).tag)
                val oldWidth=toolbar.width/5.0
                for(i in 1..3) {
                    assertEquals(oldWidth*.9,toolbar.getChildAt(i).width.toDouble(),1.1)
                    assertTrue(toolbar.getChildAt(i).width>=48)
                }
            }
            root.findViewWithTag<View>("dashboard_hrv").performClick()
            val screen=ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(screen.window!!.decorView.findViewWithTag<View>("hrv_circle"))
            assertNotNull(screen.window!!.decorView.findViewWithTag<View>("hrv_graph"))
            val action=screen.window!!.decorView.findViewWithTag<View>("hrv_action")
            assertFalse(action.isEnabled) // No fabricated RR-capable connection.
            assertFalse(RideRuntime.get(a).recording)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun measurementRendersWithCurrentThemeAndUpdatesRingAndGraphAfterThemeChange() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        val c=host.get();val controller=HrvController(c)
        try {
            val ready=java.util.concurrent.CountDownLatch(1)
            val worker=HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as java.util.concurrent.ExecutorService
            worker.execute { ready.countDown() };assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS))
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val points=(1..60).map { HrvPoint(it*1000L,800+25*kotlin.math.sin(it/4.0)) }
            HrvController::class.java.getDeclaredField("state").apply { isAccessible=true }.set(controller,
                HrvState(de.bcycle.hrv.Phase.MEASURE,60000,120000,73,true,points))
            HrvController::class.java.getDeclaredField("busy").apply { isAccessible=true }.setBoolean(controller,true)
            val view=HrvMeasurementView(c,themes,controller,{"Testgurt"},{true},{})
            val scroll=android.widget.ScrollView(c).apply { addView(view) };c.setContentView(scroll)
            scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(760,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,760)
            fun render():android.graphics.Bitmap=android.graphics.Bitmap.createBitmap(360,760,android.graphics.Bitmap.Config.ARGB_8888).also {
                it.eraseColor(themes.active.tokens.secondary);scroll.draw(android.graphics.Canvas(it))
            }
            fun hasColor(bitmap:android.graphics.Bitmap,color:Int):Boolean {
                val values=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(values,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                return values.count { it==color }>50
            }
            val graph=view.findViewWithTag<View>("hrv_graph")
            assertEquals(286,graph.height)
            val graphBitmap=android.graphics.Bitmap.createBitmap(graph.width,graph.height,android.graphics.Bitmap.Config.ARGB_8888)
            graph.draw(android.graphics.Canvas(graphBitmap))
            // Axis glyph interiors retain 50% alpha even on a transparent canvas.
            var halfOpaqueLabels=0
            for(y in 0 until graph.height) for(x in 0 until 40) {
                val pixel=graphBitmap.getPixel(x,y)
                if(Color.alpha(pixel)==128 && Color.red(pixel)==255 && Color.green(pixel)==255 && Color.blue(pixel)==255) halfOpaqueLabels++
            }
            assertTrue(halfOpaqueLabels>10);graphBitmap.recycle()
            val first=render();assertTrue(hasColor(first,themes.active.tokens.accent));first.recycle()
            val next=themes.presets.first { it.tokens.accent!=themes.active.tokens.accent }
            assertTrue(themes.activate(next.id))
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val second=render()
            assertTrue(hasColor(second,themes.active.tokens.accent))
            val output=java.io.File("build/reports/hrv-measurement-preview.png").apply { parentFile?.mkdirs() }
            output.outputStream().use { second.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };second.recycle()
            assertTrue(view.findViewWithTag<View>("hrv_action").bottom<=760)
            val action=view.findViewWithTag<TextView>("hrv_action")
            BicycleDialogStyle.content(view,themes)
            assertNull(action.background)
            assertEquals(themes.active.tokens.accent,action.currentTextColor)
            val circle=view.findViewWithTag<View>("hrv_circle")
            assertSame(circle.parent,action.parent)
            assertTrue(action.top>circle.height/2)
            assertTrue(action.bottom<circle.bottom)
            assertFalse(all(view).filterIsInstance<TextView>().any { it.text==c.getString(R.string.hrv_title) })
        } finally { controller.close();host.pause().stop().destroy() }
    }
    @Test fun lowCoverageWarnsInOrangeButStillShowsMetricsAndRejectedArtefactsHideMetrics() {
        val c=RuntimeEnvironment.getApplication();val themes=ThemeManager(c.getSharedPreferences("bike",0))
        val row=JSONObject().put("acceptable",true).put("coverage",.8).put("measurementElapsedMs",180000).put("rrDurationMs",144000)
            .put("corrected",JSONObject().put("rmssdMs",30).put("lnRmssd",3.4).put("sdnnMs",20).put("pnn50Percent",1).put("restingHrBpm",75))
        val view=hrvResult(c,themes,row)
        BicycleDialogStyle.content(view,themes)
        assertNotNull(view.findViewWithTag<View>("hrv_metrics"))
        val warning=view.findViewWithTag<TextView>("hrv_coverage_warning")
        assertTrue(warning.text.contains("80"));assertTrue(warning.text.contains("verzerrt"))
        assertEquals(HrvVisuals.warningOrange,warning.currentTextColor)
        row.put("acceptable",false).put("reasons",JSONArray().put("HIGH_CORRECTION"))
        val rejected=hrvResult(c,themes,row)
        assertNull(rejected.findViewWithTag<View>("hrv_metrics"))
        assertTrue(all(rejected).filterIsInstance<TextView>().any { it.text.contains("früheren Qualitätsregel") })
        row.put("acceptable",true).put("warnings",JSONArray().put("HIGH_CORRECTION")).put("affectedFraction",.08)
        val estimated=hrvResult(c,themes,row)
        assertNotNull(estimated.findViewWithTag<View>("hrv_metrics"))
        val correction=estimated.findViewWithTag<TextView>("hrv_correction_warning")
        assertTrue(correction.text.contains("Schätzung"));assertEquals(HrvVisuals.warningOrange,correction.currentTextColor)
        for((reason,text) in listOf("RESIDUAL_ARTIFACTS" to "weiterhin auffällige", "CORRECTION_FAILED" to "technisch fehlgeschlagen")) {
            row.put("acceptable",false).put("reasons",JSONArray().put(reason))
            val failed=hrvResult(c,themes,row)
            assertNull(failed.findViewWithTag<View>("hrv_metrics"))
            assertTrue(all(failed).filterIsInstance<TextView>().any { it.text.contains(text) })
        }
    }
}
