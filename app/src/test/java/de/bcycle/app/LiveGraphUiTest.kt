package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.time.Duration
import android.os.Looper
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveGraphUiTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private fun all(v: View): List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun measure(v: View,w: Int,h: Int) { v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h) }
    private fun history(): GraphFrame { val h=GraphHistoryBuffer();repeat(31) { i -> h.append(GraphPoint(i*1000L,i*1000L,i*20.0,listOf(25.0+i/2,80.0,200.0,130.0,500.0))) };return h.frame(1,320) }
    @Test fun orderedChoicesPreventThirdRemainDeselectableAndReloadPerPageAndSlot() {
        val prefs=context.getSharedPreferences("graphs",0);val themes=ThemeManager(prefs)
        val chooser=GraphOverlayPicker(context,themes,emptyList());val boxes=all(chooser).filterIsInstance<CheckBox>()
        assertTrue(boxes.first().isChecked);boxes[1].performClick();boxes[2].performClick()
        assertEquals(listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE),chooser.selected)
        assertFalse(boxes[3].isEnabled);boxes[3].performClick();assertEquals(2,chooser.selected.size)
        boxes[1].performClick();boxes[3].performClick();assertEquals(listOf(AnalysisChart.CADENCE,AnalysisChart.POWER),chooser.selected)
        val config=GraphOverlayConfig().replace(2,chooser.selected)
        prefs.edit().putString(GraphOverlayConfig.key(3),config.encode()).commit()
        assertEquals(config,GraphOverlayConfig.read(context.getSharedPreferences("graphs",0),3));assertTrue(GraphOverlayConfig.read(prefs,0).slots.all { it.isEmpty() })
        assertTrue(GraphOverlayConfig.decode(null).slots.all { it.isEmpty() });assertEquals(2,GraphOverlayConfig.decode("SPEED,POWER,CADENCE").slots[0].size)
        boxes.first().performClick();assertTrue(chooser.selected.isEmpty())
    }
    @Test fun overlaysDoNotChangeGeometryFontsForegroundOrUnsupportedTiles() {
        for(template in DashboardTemplate.entries) for((w,h) in listOf(960 to 1500,1440 to 780)) {
            val view=DashboardView(context) {};view.setTemplate(template);measure(view,w,h)
            val tiles=all(view).filter { it.isClickable };val geometry=tiles.map { listOf(it.left,it.top,it.width,it.height) }
            val fonts=all(view).filterIsInstance<TextView>().map { Triple(it.textSize,it.currentTextColor,it.text.toString()) }
            view.showGraphs(GraphOverlayConfig().replace(2,listOf(AnalysisChart.SPEED,AnalysisChart.POWER)),history());measure(view,w,h)
            assertEquals(geometry,all(view).filter { it.isClickable }.map { listOf(it.left,it.top,it.width,it.height) })
            assertEquals(fonts,all(view).filterIsInstance<TextView>().map { Triple(it.textSize,it.currentTextColor,it.text.toString()) })
            assertEquals(if(template.supportsGraph(2)) 1 else 0,all(view).count { it.background is LayerDrawable })
            // Draw a real bitmap to exercise clipping/cached paths on narrow and landscape fields.
            view.draw(Canvas(Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)))
            view.showGraphs(GraphOverlayConfig(),null);assertFalse(all(view).any { it.background is LayerDrawable })
        }
    }
    @Test fun axesAndEveryGraphPixelStayInsideEightyPercentOfInnerHeight() {
        val f=history();val d=DashboardGraphDrawable(1f);d.setBounds(0,0,400,200)
        d.update(f,listOf(AnalysisChart.SPEED),intArrayOf(10,8,10,4))
        assertEquals(1,d.axisCount);val plot=d.plotBounds();assertEquals((200-12)*.8f,plot.height(),.001f)
        val b=Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888);d.draw(Canvas(b))
        for(y in 0 until 200) if(y<plot.top.toInt() || y>plot.bottom.toInt()) for(x in 0 until 400) assertEquals(0,b.getPixel(x,y))
        d.update(f,listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE),intArrayOf(10,8,10,4));assertEquals(2,d.axisCount)
        assertTrue(LiveGraphStyle.FILL_ALPHA<LiveGraphStyle.LINE_ALPHA);assertTrue(LiveGraphStyle.LINE_ALPHA<160)
        assertEquals(AnalysisChart.SPEED.darkColor,AnalysisChart.SPEED.color(Color.BLACK))
    }
    @Test fun dashboardBackupRoundTripsGraphsPreservesLegacyAndRejectsThirdChoice() {
        val bike=context.getSharedPreferences("bike",0);val route=context.getSharedPreferences("route-export",0)
        val cfg=GraphOverlayConfig().replace(2,listOf(AnalysisChart.HEART,AnalysisChart.ELEVATION))
        bike.edit().putString(GraphOverlayConfig.key(0),cfg.encode()).commit()
        val b=SettingsBackup(bike,route,ThemeManager(bike));val bytes=b.export(setOf(BackupSection.DASHBOARD))
        bike.edit().remove(GraphOverlayConfig.key(0)).commit();val doc=b.decode(bytes);b.apply(b.plan(doc,doc.sections.keys));assertEquals(cfg,GraphOverlayConfig.read(bike,0))
        val old=JSONObject(String(bytes));val page=old.getJSONObject("sections").getJSONObject("dashboard").getJSONArray("pages").getJSONObject(0)
        page.remove("graphs");val legacy=b.decode(old.toString().toByteArray());b.apply(b.plan(legacy,legacy.sections.keys));assertEquals(cfg,GraphOverlayConfig.read(bike,0))
        page.put("graphs",JSONArray(List(8) { if(it==2) JSONArray(listOf("POWER","SPEED","CADENCE")) else JSONArray() }))
        try { b.decode(old.toString().toByteArray());fail("third graph accepted") } catch(_:SettingsBackupFailure) {}
    }
    @Test fun oneSharedWorkerPublishesAtTenSecondsStopsAndResetsForNextRide() {
        val p=LiveGraphProvider {};val worker=LiveGraphProvider::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(p) as ThreadPoolExecutor
        fun drain() { worker.submit {}.get(5,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle() }
        fun append(t: Long) { p.append(GraphPoint(t,t,t/100.0,listOf(20.0,80.0,200.0,null,null)));drain() }
        p.begin();append(0);val first=p.frame!!;append(9000);assertSame(first,p.frame)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        append(10000);val second=p.frame!!;assertTrue(second.version>first.version);assertEquals(100.0,second.distanceM,0.0)
        append(11000);assertSame(second,p.frame)
        p.stop();drain();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10));drain();assertEquals(110.0,p.frame!!.distanceM,0.0)
        val stopped=p.frame;append(999999);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20));drain();assertSame(stopped,p.frame)
        p.begin();drain();assertNull(p.frame);append(0);assertEquals(0.0,p.frame!!.distanceM,0.0)
        worker.shutdownNow()
    }
    @Test fun sourceDropdownReusesGraphDraftWhileMainNumberRemainsIndependent() {
        val themes=ThemeManager(context.getSharedPreferences("graph-source",0))
        val graph=GraphOverlayPicker(context,themes,listOf(AnalysisChart.HEART,AnalysisChart.ELEVATION))
        var chosen: DashboardField?=null
        val picker=FieldPickerView(context,LiveData(),GpsValues(),null,PulseValues(),DashboardField("SPEED"),false,themes,graph) { chosen=it }
        val source=all(picker).filterIsInstance<android.widget.Spinner>().single()
        for(i in 0 until source.count) {
            source.setSelection(i);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1,all(picker).count { it===graph })
            assertEquals(listOf(AnalysisChart.HEART,AnalysisChart.ELEVATION),graph.selected)
        }
        source.setSelection(0);shadowOf(Looper.getMainLooper()).idle()
        all(picker).filterIsInstance<android.widget.RadioButton>().first().performClick()
        assertNotNull(chosen);assertEquals(listOf(AnalysisChart.HEART,AnalysisChart.ELEVATION),graph.selected)
    }
    @Test fun workerOverloadDropsOnlyDisplaySamplesAndMarksTheNextSegmentAsAGap() {
        val p=LiveGraphProvider {};val worker=LiveGraphProvider::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(p) as ThreadPoolExecutor
        p.begin();worker.submit {}.get(5,TimeUnit.SECONDS)
        val entered=java.util.concurrent.CountDownLatch(1);val release=java.util.concurrent.CountDownLatch(1)
        worker.execute { entered.countDown();release.await(5,TimeUnit.SECONDS) };assertTrue(entered.await(5,TimeUnit.SECONDS))
        repeat(32) { i -> p.append(GraphPoint(i*100L,i*100L,i.toDouble(),listOf(20.0,80.0,200.0,null,null))) }
        assertEquals(16,worker.queue.size);release.countDown()
        val deadline=System.nanoTime()+5_000_000_000L
        while(worker.queue.isNotEmpty() && System.nanoTime()<deadline) Thread.yield()
        assertTrue(worker.queue.isEmpty())
        worker.submit {}.get(5,TimeUnit.SECONDS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(40))
        p.append(GraphPoint(3200,3200,32.0,listOf(20.0,80.0,200.0,null,null)));worker.submit {}.get(5,TimeUnit.SECONDS)
        p.stop();worker.submit {}.get(5,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10));worker.submit {}.get(5,TimeUnit.SECONDS)
        assertEquals(31,p.frame!!.series.getValue(AnalysisChart.POWER).points.last().breaks)
        worker.shutdownNow()
    }

}
