package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.*
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BatterySaverTest {
    private fun layout(v:View) { v.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));v.layout(0,0,360,800) }
    @Test fun switchFollowsWheelAndSurvivesProfileChangesAndSettingsBackup() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val runtime=RideRuntime.get(c);val store=runtime.bikes
            val view=BikesView(c,runtime,{}, {}, {}, { LinearLayout(c) });c.setContentView(view);layout(view)
            val wheel=view.findViewWithTag<View>("bike_wheel_row");val toggle=view.findViewWithTag<Switch>("bike_power_saving")
            val parent=wheel.parent as android.view.ViewGroup
            assertSame(parent,toggle.parent);assertEquals(parent.indexOfChild(wheel)+1,parent.indexOfChild(toggle));assertFalse(toggle.isChecked)
            toggle.isChecked=true;assertTrue(store.current().powerSaving)
            val first=store.activeSlot;val second=checkNotNull(store.add()).slot
            store.select(second);assertFalse(store.current().powerSaving)
            store.select(first);assertTrue(store.current().powerSaving)
            val prefs=c.getSharedPreferences("bike",0);val backup=SettingsBackup(c,ThemeManager(prefs));val data=backup.export(setOf(BackupSection.PROFILE))
            store.save(store.current().copy(powerSaving=false));assertFalse(store.current().powerSaving)
            backup.apply(backup.plan(backup.decode(data),setOf(BackupSection.PROFILE)))
            assertTrue(BikeProfileStore(c,prefs).current().powerSaving)
            val rows=JSONArray(prefs.getString(BikeProfileStore.KEY,"[]"));repeat(rows.length()) { rows.getJSONObject(it).remove("powerSaving") }
            prefs.edit().putString(BikeProfileStore.KEY,rows.toString()).commit()
            assertTrue("old profiles remain at the original behavior",BikeProfileStore(c,prefs).all().none { it.powerSaving })
        } finally { host.pause().stop().destroy() }
    }
    @Test fun displayGateBatchesLatestStateButUrgentActionsAndCancellationStayImmediate() {
        var latest=0;val drawn=mutableListOf<Int>()
        val gate=DisplayUpdateGate(Handler(Looper.getMainLooper())) { drawn+=latest }.apply { intervalMs=1000 }
        latest=1;gate.request();repeat(20) { latest++;gate.request() }
        assertEquals(listOf(1),drawn)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(999));assertEquals(1,drawn.size)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));assertEquals(listOf(1,21),drawn)
        latest=22;gate.request(immediate=true);assertEquals(22,drawn.last())
        latest=23;gate.request();gate.cancel();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));assertEquals(22,drawn.last())
        gate.intervalMs=0;gate.request();latest=24;gate.request();assertEquals(listOf(23,24),drawn.takeLast(2))
    }
    @Test fun automaticMapFramesAreBatchedWhileGpsReceptionAndManualCameraRemainImmediate() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val runtime=RideRuntime.get(host.get());val panel=MapPanel(host.get(),runtime,{}, {}, {})
            host.get().setContentView(panel);layout(panel);panel.resume();panel.setPowerSaving(true)
            fun fix(lon:Double) {
                val now=SystemClock.elapsedRealtime()
                assertTrue(runtime.ride.gps.accept(GpsFix(now,51.0,lon,3.0,5.0,.2,bearingDegrees=90.0,bearingAccuracyDegrees=3.0),now))
                RideRuntime::class.java.getDeclaredMethod("captureGpsPerformance").apply { isAccessible=true }.invoke(runtime)
                panel.refresh()
            }
            @Suppress("UNCHECKED_CAST") val mapChevrons=MapDataFieldsView::class.java.getDeclaredField("chevrons").apply { isAccessible=true }
                .get(panel.findViewWithTag<MapDataFieldsView>("map_data_fields")) as List<TrendChevrons>
            assertTrue(mapChevrons.all { it.powerSaving })
            fix(10.001);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            val first=panel.mapView.center.longitude
            repeat(3) { i -> shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));fix(10.001+(i+1)*.00002);assertEquals(first,panel.mapView.center.longitude,.0000001) }
            assertTrue(runtime.gpsValues.longitude!!>first)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));assertEquals(runtime.mapGpsValues.longitude!!,panel.mapView.center.longitude,.0000001)
            panel.setFollow(false);val before=panel.mapView.center;panel.mapView.panBy(50f,0f);assertNotEquals(before,panel.mapView.center)
            panel.mapView.setZoom(15.5);assertEquals(15.5,panel.mapView.zoom,.001)
            panel.pause();val paused=panel.mapView.center;shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));assertEquals(paused,panel.mapView.center)
            panel.destroy()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun graphsKeepAllSamplesWhilePublishingAtFiveSecondsOrLongerAndOnlyPreparingVisibleSeries() {
        val p=LiveGraphProvider {}.apply { rolling=true }
        val worker=LiveGraphProvider::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(p) as ExecutorService
        fun drain() { worker.submit {}.get(5,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle() }
        val window=GraphWindow.entries.first { it!=GraphWindow.FULL }
        try {
            p.begin();p.configureDisplay(true,true,mapOf(window to setOf(AnalysisChart.POWER)));drain();assertEquals(5000,p.interval)
            fun append(i:Int) { p.append(GraphPoint(i*1000L,i*1000L,i*10.0,AnalysisChart.entries.map { 100.0+i+it.ordinal }));drain() }
            append(0);val first=checkNotNull(p.frame).version
            for(i in 1..4) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));append(i);assertEquals(first,p.frame!!.version) }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));append(5)
            assertTrue(p.frame!!.version>first);assertEquals(setOf(AnalysisChart.POWER),p.frame!!.windows.getValue(window).keys);assertTrue(p.frame!!.series.isEmpty())
            p.applyRuntimePolicy(true,false);val hidden=p.frame!!.version
            for(i in 6..10) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));append(i) }
            assertEquals(hidden,p.frame!!.version)
            p.configureDisplay(true,true,mapOf(window to setOf(AnalysisChart.POWER)));drain();assertEquals(10000,p.frame!!.elapsedMs)
            val history=LiveGraphProvider::class.java.getDeclaredField("history").apply { isAccessible=true }.get(p) as GraphHistoryBuffer
            val complete=history.frame(999,360)
            AnalysisChart.entries.forEach { chart -> assertEquals(110.0+chart.ordinal,complete.series.getValue(chart).points.last().values[chart.ordinal]!!,.001) }
            p.rolling=false;p.configureDisplay(true,true,mapOf(GraphWindow.FULL to setOf(AnalysisChart.POWER)));assertEquals(10000,p.interval)
            p.configureDisplay(false,true,emptyMap());p.rolling=true;assertEquals(1000,p.interval);drain()
            assertEquals(AnalysisChart.entries.toSet(),p.frame!!.series.keys)
        } finally { worker.shutdownNow() }
    }
    @Test fun saverMakesIndicatorsGlowsGraphScalesAndChevronsStaticAndKeepsWarningsVisible() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        android.animation.ValueAnimator::class.java.getDeclaredMethod("setDurationScale",Float::class.javaPrimitiveType).invoke(null,1f)
        try {
            val c=host.get();val view=View(c);c.setContentView(view);layout(view)
            val chevrons=TrendChevrons(view).apply { powerSaving=true;update(3) }
            assertNull(TrendChevrons::class.java.getDeclaredField("animator").apply { isAccessible=true }.get(chevrons))
            chevrons.powerSaving=false;chevrons.update(-3)
            assertNotNull(TrendChevrons::class.java.getDeclaredField("animator").apply { isAccessible=true }.get(chevrons));chevrons.clear()
            val indicator=RecordingButtonIndicator(c,Color.WHITE) {}.apply { powerSaving=true;setAnimating(true) }
            assertFalse(indicator.running);assertEquals(1f,indicator.dotFraction,0f)
            indicator.powerSaving=false;indicator.setAnimating(true);assertTrue(indicator.running);indicator.stop()
            var scheduled=0
            val callback=object:Drawable.Callback {
                override fun invalidateDrawable(who:Drawable) {}
                override fun scheduleDrawable(who:Drawable,what:Runnable,whenMs:Long) { scheduled++ }
                override fun unscheduleDrawable(who:Drawable,what:Runnable) {}
            }
            val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
            val zone=ZoneAmbientGlow().apply { powerSaving=true;setBounds(0,0,100,100);this.callback=callback;update(Color.GREEN) }
            zone.draw(canvas);assertEquals(Color.GREEN,zone.currentColor())
            val bosch=BoschModeGlow(12f).apply { powerSaving=true;setBounds(0,0,100,100);this.callback=callback;update(Color.BLUE,5) }
            bosch.draw(canvas);assertTrue("low battery warning stays red and visible",Color.red(bitmap.getPixel(50,1))>Color.blue(bitmap.getPixel(50,1)))
            val graph=DashboardGraphDrawable(1f).apply { powerSaving=true;setBounds(0,0,100,100);this.callback=callback }
            val points=listOf(GraphPoint(0,0,0.0,List(5){100.0}),GraphPoint(1000,1000,10.0,List(5){120.0}))
            val series=mapOf(AnalysisChart.POWER to GraphSeries(points,GraphRange(0.0,200.0)))
            val a=GraphFrame(1,10.0,series,1000);val b=a.copy(version=2,distanceM=20.0,elapsedMs=2000)
            graph.update(a,listOf(AnalysisChart.POWER),intArrayOf(0,0,0,0));graph.update(b,listOf(AnalysisChart.POWER),intArrayOf(0,0,0,0))
            assertSame("a rolling-frame publication must not shorten the full graph interval",a,DashboardGraphDrawable::class.java.getDeclaredField("frame").apply { isAccessible=true }.get(graph))
            val window=GraphWindow.entries.first { it!=GraphWindow.FULL }
            graph.update(b.copy(windows=mapOf(window to series)),listOf(AnalysisChart.POWER),intArrayOf(0,0,0,0),window=window)
            graph.draw(canvas);assertEquals(0,scheduled);bitmap.recycle()
        } finally { host.pause().stop().destroy() }
    }
}
