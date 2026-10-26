package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.LayerDrawable
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardAppearanceTest {
    @Test fun everyChartAndTimeWindowShowsMaximumMidpointAndMinimumOnBothAxes() {
        val history=GraphHistoryBuffer()
        repeat(61) { i -> history.append(GraphPoint(i*1000L,i*1000L,i*20.0,listOf(30.0,80.0,180.0,130.0,500.0))) }
        val frame=history.frame(1,400)
        data class Label(val text:String,val x:Float,val y:Float,val align:Paint.Align)
        val selections=AnalysisChart.entries.map { listOf(it) }+listOf(listOf(AnalysisChart.SPEED,AnalysisChart.POWER))
        for(window in GraphWindow.entries) for(charts in selections) {
            val d=DashboardGraphDrawable(1f);d.setBounds(0,0,400,200)
            d.update(frame,charts,intArrayOf(0,0,0,0),window=window)
            val labels=mutableListOf<Label>()
            val bitmap=Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888)
            d.draw(object:Canvas(bitmap) {
                override fun drawText(text:String,x:Float,y:Float,paint:Paint) {
                    labels+=Label(text,x,y,paint.textAlign);super.drawText(text,x,y,paint)
                }
            })
            assertEquals("$window $charts",charts.size*3,labels.size)
            charts.forEachIndexed { index,chart ->
                val range=(if(window==GraphWindow.FULL) frame.series else frame.windows.getValue(window)).getValue(chart).range
                val axis=labels.drop(index*3).take(3)
                val expected=listOf(range.high,(range.low+range.high)/2,range.low).map { String.format(java.util.Locale.getDefault(),"%.0f",it) }
                assertEquals(expected,axis.map { it.text })
                assertTrue(axis[1].y>axis[0].y && axis[1].y<axis[2].y)
                assertTrue(axis.all { it.align==if(index==0) Paint.Align.LEFT else Paint.Align.RIGHT })
                assertTrue(axis.all { it.x in d.plotBounds().left..d.plotBounds().right })
            }
            bitmap.recycle()
        }
    }
    private fun layout(view:View,w:Int,h:Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h)
    }
    @Test fun liveDashboardPixelsAndTrainingGlowStayIdenticalWhenSettingsDesignChanges() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val view=DashboardView(a) {};view.bindEditorTheme(themes);a.setContentView(view)
            val history=GraphHistoryBuffer()
            repeat(61) { i -> history.append(GraphPoint(i*1000L,i*1000L,i*20.0,listOf(30.0,80.0,180.0,130.0,500.0))) }
            view.show(DashboardLayout.defaults,LiveData(speedHundredths=3000))
            view.showTraining(ZoneGlowConfig().replace(2,ZoneGlowSource.POWER_CURRENT),TrainingSnapshot(RiderProfile(ftpW=200),180.0))
            view.showGraphs(GraphOverlayConfig().replace(2,listOf(AnalysisChart.SPEED,AnalysisChart.POWER)),history.frame(1,360))
            layout(view,360,800);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600))
            // The Activity's queued first traversal uses its default viewport; normalize
            // both captures after that traversal, not just the capture after switching modes.
            layout(view,360,800)
            fun image()=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
            val glow=image()
            for(flat in listOf(true,false)) {
                themes.setFlatDesign(flat);shadowOf(Looper.getMainLooper()).idle();layout(view,360,800)
                val tile=view.findViewWithTag<View>("dashboard_tile_2")
                val layers=tile.background as LayerDrawable
                assertTrue((0 until layers.numberOfLayers).any { layers.getDrawable(it) is ZoneAmbientGlow })
                layers.setBounds(0,0,tile.width,tile.height)
                val background=Bitmap.createBitmap(tile.width,tile.height,Bitmap.Config.ARGB_8888)
                layers.draw(Canvas(background))
                assertTrue("Training glow remains visible below fading graph",Color.alpha(background.getPixel(tile.width/2,tile.height-2))>0)
                background.recycle()
                val actual=image()
                if(!glow.sameAs(actual)) {
                    val dir=java.io.File("build/reports/ui").apply { mkdirs() }
                    java.io.File(dir,"dashboard-mode-before.png").outputStream().use { glow.compress(Bitmap.CompressFormat.PNG,100,it) }
                    java.io.File(dir,"dashboard-mode-after.png").outputStream().use { actual.compress(Bitmap.CompressFormat.PNG,100,it) }
                }
                assertTrue("Dashboard must ignore Flat=$flat",glow.sameAs(actual));actual.recycle()
            }
            glow.recycle()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun axisValuesHaveNoColoredCircleAndFillStillFadesToTransparent() {
        val points=listOf(GraphPoint(0,0,0.0,listOf(30.0,null,null,null,null)),GraphPoint(1000,1000,100.0,listOf(30.0,null,null,null,null)))
        val d=DashboardGraphDrawable(1f);d.setBounds(0,0,400,200)
        d.update(GraphFrame(1,100.0,mapOf(AnalysisChart.SPEED to GraphSeries(points,GraphRange(0.0,40.0)))),listOf(AnalysisChart.SPEED),intArrayOf(0,0,0,0),GraphDisplayMode.GRAPH_ONLY)
        val b=Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888);var circles=0
        d.draw(object:Canvas(b) {
            override fun drawCircle(cx:Float,cy:Float,radius:Float,paint:Paint) { circles++;super.drawCircle(cx,cy,radius,paint) }
        })
        assertEquals(0,circles)
        assertTrue(Color.alpha(b.getPixel(200,80))>Color.alpha(b.getPixel(200,170)))
        assertEquals(0,Color.alpha(b.getPixel(200,190)));b.recycle()
    }
    @Test fun dashboardFooterKeepsItsShadowAcrossSettingsDesignModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val root=FrameLayout(a).apply { setBackgroundColor(Color.WHITE) }
            val footer=LinearLayout(a).apply { styleBicycleFooter(this,themes,dashboard=true) }
            root.addView(footer,FrameLayout.LayoutParams(-1,48,Gravity.BOTTOM));a.setContentView(root);layout(root,180,180)
            fun image()=Bitmap.createBitmap(180,180,Bitmap.Config.ARGB_8888).also { root.draw(Canvas(it)) }
            val glow=image();assertTrue(Color.red(glow.getPixel(90,131))<255)
            themes.setFlatDesign(true);shadowOf(Looper.getMainLooper()).idle();layout(root,180,180)
            val flat=image();assertTrue(glow.sameAs(flat));flat.recycle();glow.recycle()
        } finally { host.pause().stop().destroy() }
    }
}
