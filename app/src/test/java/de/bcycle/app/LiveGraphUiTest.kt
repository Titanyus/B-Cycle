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
    @Test fun rollingWindowsRenderTimedPowerAtTheCorrectPositionEvenWhileStationary() {
        val h=GraphHistoryBuffer()
        repeat(91) { i -> h.append(GraphPoint(i*1000L,i*1000L,0.0,listOf(null,null,if(i==75) 300.0 else 100.0,null,null))) }
        val frame=h.frame(1,400)
        for((window,fraction) in listOf(GraphWindow.SECONDS_30 to .5f,GraphWindow.SECONDS_60 to .75f,GraphWindow.SECONDS_120 to .625f)) {
            val drawable=DashboardGraphDrawable(1f);drawable.setBounds(0,0,400,200)
            drawable.update(frame,listOf(AnalysisChart.POWER),intArrayOf(0,0,0,0),GraphDisplayMode.GRAPH_ONLY,window)
            assertEquals(1,drawable.axisCount)
            val image=android.graphics.Bitmap.createBitmap(400,200,android.graphics.Bitmap.Config.ARGB_8888)
            try {
                drawable.draw(android.graphics.Canvas(image))
                val color=AnalysisChart.POWER.color(android.graphics.Color.BLACK)
                var top=200;val xs=mutableListOf<Int>()
                for(y in 20 until 180) for(x in 40 until 390) {
                    val pixel=image.getPixel(x,y)
                    if(android.graphics.Color.alpha(pixel)>40 && (pixel and 0xffffff)==(color and 0xffffff)) {
                        if(y<top) { top=y;xs.clear() };if(y==top) xs+=x
                    }
                }
                assertTrue(xs.isNotEmpty())
                assertEquals(400*fraction,xs.average().toFloat(),12f)
            } finally { image.recycle() }
        }
    }
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
        for(template in DashboardTemplate.entries) for((w,h) in listOf(960 to 1500,1440 to 2400)) {
            val view=DashboardView(context) {};view.setTemplate(template);measure(view,w,h)
            val tiles=all(view).filter { it.isClickable };val geometry=tiles.map { listOf(it.left,it.top,it.width,it.height) }
            val fonts=all(view).filterIsInstance<TextView>().map { Pair(it.textSize,it.currentTextColor) }
            view.showGraphs(GraphOverlayConfig().replace(2,listOf(AnalysisChart.SPEED,AnalysisChart.POWER)),history());measure(view,w,h)
            assertEquals(geometry,all(view).filter { it.isClickable }.map { listOf(it.left,it.top,it.width,it.height) })
            assertEquals(fonts,all(view).filterIsInstance<TextView>().map { Pair(it.textSize,it.currentTextColor) })
            assertEquals(if(template.supportsGraph(2)) 1 else 0,all(view).count { it.background is LayerDrawable })
            // Draw a real bitmap to exercise clipping/cached paths on narrow and wider portrait dashboards.
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
        assertTrue(LiveGraphStyle.FILL_ALPHA<LiveGraphStyle.LINE_ALPHA);assertTrue(LiveGraphStyle.LINE_ALPHA<255)
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
        val source=all(picker).filterIsInstance<android.widget.Spinner>().single { it.tag=="field_source" }
        for(i in 0 until source.count) {
            source.setSelection(i);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1,all(picker).count { it===graph })
            assertEquals(listOf(AnalysisChart.HEART,AnalysisChart.ELEVATION),graph.selected)
        }
        source.setSelection(0);shadowOf(Looper.getMainLooper()).idle()
        all(picker).filterIsInstance<android.widget.RadioButton>().single { it.tag=="GPS_SPEED" }.performClick()
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

    @Test fun overlayDotsFollowSelectedColorsAndOrderOnlyOnLargeFieldCaptions() {
        val view=DashboardView(context) {};view.show(DashboardLayout.defaults,LiveData(speedHundredths=3000))
        measure(view,1080,1500)
        val before=all(view).filter { it.isClickable }.map { listOf(it.width,it.height) }
        val cfg=GraphOverlayConfig().replace(2,listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE))
        view.showGraphs(cfg,history());measure(view,1080,1500)
        val caption=all(view).filterIsInstance<TextView>().single { it.text is android.text.Spanned &&
            (it.text as android.text.Spanned).getSpans(0,it.text.length,RecordingDotSpan::class.java).isNotEmpty() }
        val text=caption.text as android.text.Spanned
        val dots=text.getSpans(0,text.length,RecordingDotSpan::class.java)
        assertEquals(2,dots.size);assertTrue(text.toString().contains("Tempo"))
        dots.forEachIndexed { i,dot ->
            val b=Bitmap.createBitmap(30,30,Bitmap.Config.ARGB_8888)
            dot.draw(Canvas(b),"x",0,1,0f,0,20,30,Paint().apply { textSize=18f })
            val colors=(0 until 30).flatMap { y -> (0 until 30).map { x -> b.getPixel(x,y) } }
            assertTrue(colors.contains(cfg.slots[2][i].color(Color.BLACK)))
        }
        assertEquals(before,all(view).filter { it.isClickable }.map { listOf(it.width,it.height) })
        assertTrue(all(view).filterIsInstance<TextView>().any { it.text.toString()=="30,0" })
        view.showGraphs(GraphOverlayConfig(),null)
        assertFalse(all(view).filterIsInstance<TextView>().any { it.text is android.text.Spanned &&
            (it.text as android.text.Spanned).getSpans(0,it.text.length,RecordingDotSpan::class.java).isNotEmpty() })
        view.showGraphs(cfg,history());view.setTemplate(DashboardTemplate.FIVE)
        assertFalse(all(view).filterIsInstance<TextView>().any { it.text is android.text.Spanned &&
            (it.text as android.text.Spanned).getSpans(0,it.text.length,RecordingDotSpan::class.java).isNotEmpty() })
    }
    @Test fun curveHasNoHaloAndFillFadesDownWithoutConnectingMissingSections() {
        val p=listOf(GraphPoint(0,0,0.0,listOf(30.0,null,null,null,null)),
            GraphPoint(1000,1000,100.0,listOf(30.0,null,null,null,null)))
        val d=DashboardGraphDrawable(1f);d.setBounds(0,0,400,200)
        fun draw(points: List<GraphPoint>): Bitmap {
            d.update(GraphFrame(1,100.0,mapOf(AnalysisChart.SPEED to GraphSeries(points,GraphRange(0.0,40.0)))),listOf(AnalysisChart.SPEED),intArrayOf(0,0,0,0),GraphDisplayMode.GRAPH_ONLY)
            return Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888).also { d.draw(Canvas(it)) }
        }
        val b=draw(p)
        assertTrue(Color.alpha(b.getPixel(200,60))>=200)
        assertEquals(0,Color.alpha(b.getPixel(200,56))) // No glow outside the line.
        assertTrue(Color.alpha(b.getPixel(200,80))>Color.alpha(b.getPixel(200,170)))
        assertEquals(0,Color.alpha(b.getPixel(200,190)))
        val gap=draw(listOf(p.first(),p.first().copy(distanceM=30.0),p.first().copy(distanceM=50.0,values=List(5) { null }),
            p.last().copy(distanceM=70.0),p.last()))
        assertEquals(0,Color.alpha(gap.getPixel(200,60)));assertEquals(0,Color.alpha(gap.getPixel(200,80)))
    }

    @Test fun curveBoundaryLinesAreSymmetricAndFadeAtBothVerticalEnds() {
        val d=DashboardGraphDrawable(1f);d.setBounds(0,0,400,200)
        // Missing samples retain valid scales, isolating the boundary from curve/fill pixels.
        val series=GraphSeries(emptyList(),GraphRange(0.0,40.0))
        d.update(GraphFrame(1,100.0,mapOf(AnalysisChart.SPEED to series,AnalysisChart.CADENCE to series)),
            listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE),intArrayOf(0,0,0,0))
        val b=Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888);d.draw(Canvas(b))
        fun opacity(x: Int,y: Int)=maxOf(Color.alpha(b.getPixel(x,y)),Color.alpha(b.getPixel(x-1,y)))
        val middle=opacity(26,100)
        assertTrue(middle>0);assertEquals(middle,opacity(374,100))
        assertTrue(opacity(26,30)<middle);assertTrue(opacity(26,170)<middle)
        assertEquals(0,opacity(26,10));assertEquals(0,opacity(374,190))
    }

    @Test fun hiddenDataKeepsGeometryAndGraphSelectionIndependentFromNumberAndCaptionVisibility() {
        val frame=history()
        for(template in DashboardTemplate.entries) {
            val slot=if(2 in template.slots) 2 else template.slots.first()
            val config=GraphOverlayConfig().replace(slot,listOf(AnalysisChart.SPEED))
            val view=DashboardView(context) {};view.setTemplate(template)
            view.show(DashboardLayout.defaults.replace(slot,DashboardField("SPEED")),LiveData(speedHundredths=3000))
            view.showGraphs(config,frame);measure(view,1080,1500)
            val before=all(view).filter { it.isClickable }.map { listOf(it.left,it.top,it.width,it.height) }
            val tile=all(view).single { it.tag=="dashboard_tile_$slot" }
            val value=all(tile).filterIsInstance<TextView>().single { it.text.toString()=="30,0" }
            view.showDataVisibility(DashboardDataVisibility().replace(slot,false));measure(view,1080,1500)
            assertEquals(before,all(view).filter { it.isClickable }.map { listOf(it.left,it.top,it.width,it.height) })
            assertEquals(View.INVISIBLE,(value.parent as View).visibility)
            assertTrue(all(tile).filterIsInstance<TextView>().any { it.text.toString().contains("Tempo") && it.visibility==View.INVISIBLE })
            view.showGraphs(GraphOverlayConfig(),frame)
            assertEquals(View.INVISIBLE,(value.parent as View).visibility)
            view.showDataVisibility(DashboardDataVisibility())
            assertEquals(View.VISIBLE,(value.parent as View).visibility)
        }
    }
    @Test fun displayModesChangeFillAndLineBrightnessWithoutHaloOrAxisChanges() {
        val points=listOf(GraphPoint(0,0,0.0,listOf(30.0,null,null,null,null)),GraphPoint(1000,1000,100.0,listOf(30.0,null,null,null,null)))
        val frame=GraphFrame(1,100.0,mapOf(AnalysisChart.SPEED to GraphSeries(points,GraphRange(0.0,40.0))))
        val d=DashboardGraphDrawable(1f);d.setBounds(0,0,400,200)
        fun draw(mode:GraphDisplayMode):Bitmap {
            d.update(frame,listOf(AnalysisChart.SPEED),intArrayOf(0,0,0,0),mode)
            return Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888).also { d.draw(Canvas(it)) }
        }
        val only=draw(GraphDisplayMode.GRAPH_ONLY);val overlaid=draw(GraphDisplayMode.WITH_DATA)
        assertTrue(Color.alpha(only.getPixel(200,60))>Color.alpha(overlaid.getPixel(200,60)))
        assertTrue(Color.alpha(only.getPixel(200,90))>Color.alpha(overlaid.getPixel(200,90)))
        assertEquals(0,Color.alpha(only.getPixel(200,51)))
        assertEquals(0,Color.alpha(only.getPixel(200,49)))
        assertEquals(160f,d.plotBounds().height(),.01f)
    }
    @Test fun dataChoicePersistsIndependentlyAndBackupRestoresVisibilityWithLegacyCompatibility() {
        val controller=org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val prefs=activity.getSharedPreferences("bike",0)
            prefs.edit().putString(GraphDisplaySettings.key,GraphDisplayMode.GRAPH_ONLY.name).commit()
            // A fresh read inherits the old global choice for every unconfigured page/tile.
            assertTrue(GraphOverlayConfig.read(prefs,0).displayModes.all { it==GraphDisplayMode.GRAPH_ONLY })
            MainActivity::class.java.getDeclaredMethod("changeDashboard",Int::class.javaPrimitiveType).apply { isAccessible=true }.invoke(activity,0)
            activity.setDashboardEditing(true)
            fun choose():android.app.AlertDialog {
                all(activity.findViewById(android.R.id.content)).single { it.tag=="dashboard_tile_2" }.performClick()
                shadowOf(Looper.getMainLooper()).idle();return org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            }
            val screen=choose()
            assertFalse(all(screen.window!!.decorView).any { it.tag=="graph_display_mode" })
            all(screen.window!!.decorView).single { it.tag=="dashboard_field_empty" }.performClick()
            assertFalse(DashboardDataVisibility.read(prefs,0).visible[2])
            assertTrue(DashboardDataVisibility.read(prefs,0).visible[0])
            assertTrue(DashboardDataVisibility.read(prefs,1).visible[2])
            val reopened=choose()
            assertTrue((all(reopened.window!!.decorView).single { it.tag=="dashboard_field_empty" } as android.widget.RadioButton).isChecked)
            testBack(reopened)
            val backup=SettingsBackup(prefs,activity.getSharedPreferences("route-export",0),ThemeManager(prefs))
            val bytes=backup.export(setOf(BackupSection.DASHBOARD))
            val original=GraphOverlayConfig.read(prefs,0)
            prefs.edit().remove(GraphDisplaySettings.key(0)).commit()
            val doc=backup.decode(bytes);backup.apply(backup.plan(doc,doc.sections.keys))
            assertEquals(original,GraphOverlayConfig.read(prefs,0))
            val json=JSONObject(String(bytes));val page=json.getJSONObject("sections").getJSONObject("dashboard").getJSONArray("pages").getJSONObject(0)
            page.remove("graphDisplayModes");val legacy=backup.decode(json.toString().toByteArray());backup.apply(backup.plan(legacy,legacy.sections.keys));assertEquals(original,GraphOverlayConfig.read(prefs,0))
            page.put("graphDisplayModes",JSONArray(List(DashboardLayout.FIELD_COUNT) { "invalid" }))
            try { backup.decode(json.toString().toByteArray());fail("invalid tile mode accepted") } catch(_:SettingsBackupFailure) {}
            all(activity.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
            all(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().window!!.decorView).single { it.tag=="settings_PROFILES" }.performClick()
            assertFalse(all(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().window!!.decorView).any { it.tag=="graph_display_mode" })
        } finally { controller.pause().stop().destroy() }
    }

}
