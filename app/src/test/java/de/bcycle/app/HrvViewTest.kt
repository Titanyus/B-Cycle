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
    @Test fun routesAreLeftStartIsCenteredAndHrvOpensFromSettingsWithUsableTouchTargets() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val root=a.findViewById<ViewGroup>(android.R.id.content)
            for(width in listOf(320,360,412)) {
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(850,View.MeasureSpec.EXACTLY));root.layout(0,0,width,850)
                val toolbar=root.findViewWithTag<ViewGroup>("ride_toolbar")
                assertEquals(3,toolbar.childCount);assertEquals("dashboard_routes",toolbar.getChildAt(0).tag)
                assertEquals("record_bike_button",toolbar.getChildAt(1).tag)
                assertEquals(toolbar.width/2f,toolbar.getChildAt(1).let { it.left+it.width/2f },1f)
                assertEquals(toolbar.getChildAt(0).width.toDouble(),toolbar.getChildAt(2).width.toDouble(),1.0)
                for(i in 0..2) assertTrue(toolbar.getChildAt(i).width>=48)
                assertNull(root.findViewWithTag<View>("dashboard_hrv"))
            }
            all(root).single { it.contentDescription==a.getString(R.string.ui_menu_open) }.performClick()
            ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("settings_HRV").performClick()
            val settings=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            settings.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));settings.layout(0,0,360,800)
            val preview=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888);preview.eraseColor(RideRuntime.get(a).themes.settings.active.tokens.secondary);settings.draw(android.graphics.Canvas(preview))
            java.io.File("build/reports/ui/hrv-entry-settings.png").apply { parentFile!!.mkdirs() }.outputStream().use { preview.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };preview.recycle()
            settings.findViewWithTag<View>("hrv_open_measurement").performClick()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val screen=ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(screen.window!!.decorView.findViewWithTag<View>("hrv_circle"))
            assertNotNull(screen.window!!.decorView.findViewWithTag<View>("hrv_graph"))
            val header=screen.window!!.decorView.findViewWithTag<View>("bicycle_navigation_header")
            assertEquals(RideRuntime.get(a).themes.active.tokens.secondary,(header.background as android.graphics.drawable.ColorDrawable).color)
            val controller=screen.window!!.insetsController!!
            val statusType=Class.forName("android.view.InsetsState").getField("ITYPE_STATUS_BAR").getInt(null)
            assertFalse(controller.javaClass.getMethod("isRequestedVisible",Int::class.javaPrimitiveType).invoke(controller,statusType) as Boolean)
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
            val first=render();assertTrue(hasColor(first,themes.active.tokens.primary))
            java.io.File("build/reports/hrv-first.png").outputStream().use { first.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            val textPaint=android.graphics.Paint().apply { typeface=c.resources.getFont(R.font.league_gothic);textSize=BicycleDesign.hrvPulseSp }
            val pulseWidth=textPaint.measureText("73")
            textPaint.typeface=android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.NORMAL)
            textPaint.textSize=DashboardTileStyle.ONE.unitMaxSp.toFloat()
            val unitWidth=textPaint.measureText("bpm")
            val bounds=android.graphics.Rect()
            textPaint.typeface=c.resources.getFont(R.font.league_gothic);textPaint.textSize=BicycleDesign.hrvPulseSp
            textPaint.getTextBounds("73",0,2,bounds)
            val textLeft=180-(bounds.left+bounds.right)/2f
            fun inkCenter(left:Int,right:Int):Double {
                val rows=((170+bounds.top)..(170+bounds.bottom)).filter { y -> (left..right).any { x ->
                    val pixel=first.getPixel(x,y)
                    Color.red(pixel)>120 && Color.green(pixel)>120 && Color.blue(pixel)>120
                } }
                assertTrue(rows.isNotEmpty());return (rows.first()+rows.last())/2.0
            }
            assertEquals(inkCenter(textLeft.toInt(),(textLeft+pulseWidth-1).toInt()),
                inkCenter((textLeft+pulseWidth+4).toInt(),(textLeft+pulseWidth+4+unitWidth).toInt()),2.0)
            val columns=(textLeft.toInt()..(textLeft+pulseWidth).toInt()).filter { x ->
                ((170+bounds.top)..(170+bounds.bottom)).any { y ->
                    val pixel=first.getPixel(x,y)
                    Color.red(pixel)>120 && Color.green(pixel)>120 && Color.blue(pixel)>120
                }
            }
            assertEquals(180.0,(columns.first()+columns.last())/2.0,1.0)
            first.recycle()
            val next=themes.presets.first { it.tokens.primary!=themes.active.tokens.primary }
            assertTrue(themes.activate(next.id))
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val second=render()
            assertTrue(hasColor(second,themes.active.tokens.primary))
            val output=java.io.File("build/reports/hrv-measurement-preview.png").apply { parentFile?.mkdirs() }
            output.outputStream().use { second.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };second.recycle()
            assertTrue(view.findViewWithTag<View>("hrv_action").bottom<=760)
            val action=view.findViewWithTag<TextView>("hrv_action")
            BicycleDialogStyle.content(view,themes)
            assertNull(action.background)
            assertEquals(themes.active.tokens.primary,action.currentTextColor)
            val circle=view.findViewWithTag<View>("hrv_circle")
            assertSame(circle.parent,action.parent)
            assertTrue(action.top>circle.height/2)
            assertTrue(action.bottom<circle.bottom)
            assertFalse(all(view).filterIsInstance<TextView>().any { it.text==c.getString(R.string.hrv_title) })
            HrvController::class.java.getDeclaredField("busy").apply { isAccessible=true }.setBoolean(controller,false)
            HrvController::class.java.getDeclaredField("state").apply { isAccessible=true }.set(controller,HrvState(bpm=73,ready=true))
            val idle=HrvMeasurementView(c,themes,controller,{"Testgurt"},{true},{})
            c.setContentView(idle)
            idle.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(760,View.MeasureSpec.EXACTLY));idle.layout(0,0,360,760)
            val start=idle.findViewWithTag<TextView>("hrv_action")
            assertEquals(900,start.typeface.weight)
            BicycleDialogStyle.content(idle,themes)
            assertTrue(start.background is android.graphics.drawable.RippleDrawable)
            assertEquals(Color.WHITE,start.currentTextColor)
            val original=start.background
            assertTrue(themes.activate(themes.presets.first { it.tokens.primary!=themes.active.tokens.primary }.id))
            assertNotSame(original,start.background)
            assertEquals(Color.WHITE,start.currentTextColor)
            val idleImage=android.graphics.Bitmap.createBitmap(360,760,android.graphics.Bitmap.Config.ARGB_8888)
            idleImage.eraseColor(themes.active.tokens.secondary);idle.draw(android.graphics.Canvas(idleImage))
            java.io.File("build/reports/hrv-idle-preview.png").outputStream().use { idleImage.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };idleImage.recycle()
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
        row.put("acceptable",true).put("warnings",JSONArray().put("RESIDUAL_ARTIFACTS"))
            .put("residualArtifacts",JSONObject().put("count",3))
        val flagged=hrvResult(c,themes,row)
        assertNotNull(flagged.findViewWithTag<View>("hrv_metrics"))
        val residual=flagged.findViewWithTag<TextView>("hrv_residual_warning")
        assertTrue(residual.text.contains("3"));assertTrue(residual.text.contains("ausgewertet"))
        assertEquals(HrvVisuals.warningOrange,residual.currentTextColor)
        for((reason,text) in listOf("RESIDUAL_ARTIFACTS" to "weiterhin auffällige", "CORRECTION_FAILED" to "technisch fehlgeschlagen")) {
            row.put("acceptable",false).put("reasons",JSONArray().put(reason))
            val failed=hrvResult(c,themes,row)
            assertNull(failed.findViewWithTag<View>("hrv_metrics"))
            assertTrue(all(failed).filterIsInstance<TextView>().any { it.text.contains(text) })
        }
    }
}
