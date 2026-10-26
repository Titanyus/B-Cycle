package de.bcycle.app

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.os.Looper
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.TextView
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** The explicit editor replaces the former dashboard standstill restriction. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardEditorTest {
    @Test fun editorExitsAfterFiveIdleSecondsAndDashboardInteractionRestartsTheTimer() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();tile(a,2).performLongClick()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));assertTrue(a.dashboardEditing)
            a.onUserInteraction()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));assertTrue(a.dashboardEditing)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertFalse(a.dashboardEditing)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun openPickerUsesTwelveIdleSecondsAndKeepsSavedSelectionWhenItTimesOut() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();tile(a,2).performLongClick();tile(a,2).performClick()
            val screen=ShadowAlertDialog.getLatestAlertDialog();shadowOf(Looper.getMainLooper()).idle()
            selectTestSource(screen,"E-Bike");all(screen.window!!.decorView).single { it.tag=="CADENCE" }.performClick()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11));assertTrue(a.dashboardEditing);assertTrue(screen.isShowing)
            screen.window!!.callback.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_SHIFT_LEFT))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11));assertTrue(a.dashboardEditing);assertTrue(screen.isShowing)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertFalse(a.dashboardEditing);assertFalse(screen.isShowing)
            assertEquals("CADENCE",DashboardLayout.decode(RideRuntime.get(a).preferences.getString("dashboard",null)).fields[2].id)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun dismissingPickerStartsFreshFiveSecondDeadlineAndBackgroundSuspendsIt() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();tile(a,2).performLongClick();tile(a,2).performClick()
            val screen=ShadowAlertDialog.getLatestAlertDialog();shadowOf(Looper.getMainLooper()).idle()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10));screen.dismiss();shadowOf(Looper.getMainLooper()).idle()
            controller.pause().stop();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20));assertTrue(a.dashboardEditing)
            controller.start().resume();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));assertTrue(a.dashboardEditing)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertFalse(a.dashboardEditing)
        } finally { controller.pause().stop().destroy() }
    }
    private fun all(v: View) = testDescendants(v)
    private fun root(activity: MainActivity) = activity.findViewById<ViewGroup>(android.R.id.content)
    private fun tagged(activity: MainActivity, tag: String) = all(root(activity)).single { it.tag == tag }
    private fun measure(v: View, width: Int = 1080, height: Int = 2100) {
        v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, width, height)
    }
    private fun tile(activity: MainActivity, index: Int) = tagged(activity, "dashboard_tile_$index")
    private fun send(view: View, action: Int, down: Long, time: Long, x: Float, y: Float) {
        val event = MotionEvent.obtain(down, time, action, x, y, 0)
        try { view.dispatchTouchEvent(event) } finally { event.recycle() }
    }
    @Test fun nativeLongPressOnlyEntersEditorAndHorizontalOrVerticalMovementCancelsIt() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); controller.windowFocusChanged(true); measure(root(activity)); shadowOf(Looper.getMainLooper()).idle()
            fun gesture(dx: Float, dy: Float) {
                val field = tile(activity, 2)
                val now = SystemClock.uptimeMillis()
                send(field, MotionEvent.ACTION_DOWN, now, now, 100f, 30f)
                if (dx != 0f || dy != 0f) send(field, MotionEvent.ACTION_MOVE, now, now + 30, 100f + dx, 30f + dy)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout() + 150L))
                send(field, MotionEvent.ACTION_UP, now, SystemClock.uptimeMillis(), 100f + dx, 30f + dy)
            }
            gesture(0f, 100f); assertFalse(activity.dashboardEditing)
            gesture(100f, 0f); assertFalse(activity.dashboardEditing)
            gesture(0f, 0f); assertTrue(activity.dashboardEditing)
            assertNull(ShadowAlertDialog.getLatestAlertDialog())
            gesture(0f, 100f); assertNull(ShadowAlertDialog.getLatestAlertDialog())
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun everyPresetAndMapSlotGetsSameThemeOutlineWithoutChangingGeometry() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val runtime = RideRuntime.get(activity)
            val dashboard = all(root(activity)).filterIsInstance<DashboardView>().single()
            val panel = all(root(activity)).filterIsInstance<MapPanel>().single()
            for (template in DashboardTemplate.entries.filter { it.hasMap }) {
                dashboard.setTemplate(template); measure(root(activity))
                val fields = all(dashboard).filter { it.tag?.toString()?.startsWith("dashboard_tile_") == true }
                val geometry = fields.map { listOf(it.left, it.top, it.width, it.height) }
                val mapHeight = panel.height
                activity.setDashboardEditing(true); measure(root(activity))
                assertEquals(geometry, fields.map { listOf(it.left, it.top, it.width, it.height) })
                assertEquals(mapHeight, panel.height)
                fields.forEach { assertTrue(it.foreground is DashboardEditOutline) }
                for (i in 0..2) {
                    val view = tagged(activity, "map_value_$i")
                    assertEquals(View.VISIBLE, view.visibility); assertTrue(view.foreground is DashboardEditOutline)
                    assertFalse(view.isLongClickable)
                }
                val zoom = all(panel).filter { it.contentDescription in listOf(activity.getString(R.string.ui_map_zoom_in),activity.getString(R.string.ui_map_zoom_out)) }
                assertEquals(2,zoom.size)
                zoom.forEach { assertFalse(it.isEnabled);assertEquals(.45f,it.alpha,0f) }
                val finish = tagged(activity, "dashboard_editor_finish")
                assertEquals(View.VISIBLE, finish.visibility)
                assertEquals(panel.width / 2f, (finish.left + finish.right) / 2f, 1f)
                assertEquals(panel.height / 2f, (finish.top + finish.bottom) / 2f, 1f)
                assertTrue(finish.bottom <= tagged(activity, "map_data_fields").top)
                activity.setDashboardEditing(false)
                fields.forEach { assertNull(it.foreground) }
                assertEquals(View.GONE, finish.visibility)
                zoom.forEach { assertTrue(it.isEnabled);assertEquals(1f,it.alpha,0f) }
            }
            activity.setDashboardEditing(true)
            runtime.themes.activate("preset:electric-blue"); shadowOf(Looper.getMainLooper()).idle()
            val color = runtime.themes.active.tokens.primary
            all(dashboard).filter { it.tag?.toString()?.startsWith("dashboard_tile_") == true }.forEach { assertEquals(color, (it.foreground as DashboardEditOutline).color) }
            for(i in 0..2) assertEquals(color, (tagged(activity,"map_value_$i").foreground as DashboardEditOutline).color)
            val outline = tagged(activity,"map_value_0").foreground
            val image = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
            outline.setBounds(0,0,100,80); outline.draw(Canvas(image))
            val y = (outline as DashboardEditOutline).inset.toInt()
            val scan = (y + 6..85).map { image.getPixel(it,y) }
            assertTrue(scan.any { android.graphics.Color.alpha(it) > 0 }); assertTrue(scan.any { it == 0 })
            assertEquals(0,image.getPixel(0,0)); assertEquals(0,image.getPixel(50,40))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun graphChangesPersistAcrossPagesAndExitWhileCancellationLeavesEarlierChangesIntact() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val prefs = RideRuntime.get(activity).preferences
            tile(activity,2).performLongClick(); tile(activity,2).performClick()
            val picker = ShadowAlertDialog.getLatestAlertDialog()
            all(picker.window!!.decorView).single { it.tag == "graph_SPEED" }.performClick()
            all(picker.window!!.decorView).single { it.tag == "graph_CADENCE" }.performClick()
            testBack(picker); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE),GraphOverlayConfig.read(prefs,0).slots[2])
            val change = MainActivity::class.java.getDeclaredMethod("changeDashboard",Int::class.javaPrimitiveType).apply { isAccessible=true }
            change.invoke(activity,1); assertTrue(activity.dashboardEditing)
            tile(activity,0).performClick(); val next = ShadowAlertDialog.getLatestAlertDialog()
            selectTestSource(next,"E-Bike"); all(next.window!!.decorView).single { it.tag == "CADENCE" }.performClick()
            assertEquals("CADENCE",DashboardLayout.decode(prefs.getString("dashboard_1",null)).fields[0].id)
            assertTrue(next.isShowing);testBack(next)
            assertTrue(activity.dashboardEditing)
            shadowOf(Looper.getMainLooper()).idle()
            change.invoke(activity,-1); assertTrue(activity.dashboardEditing)
            tagged(activity,"dashboard_editor_finish").performClick(); assertFalse(activity.dashboardEditing)
            assertEquals(listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE),GraphOverlayConfig.read(prefs,0).slots[2])
            assertEquals("CADENCE",DashboardLayout.decode(prefs.getString("dashboard_1",null)).fields[0].id)
            tile(activity,2).performClick(); assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun backgroundKeepsEditorButOtherNavigationEndsItAndEmptyMapFieldsCannotEnterIt() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            assertFalse(tagged(activity,"map_value_0").performLongClick()); assertFalse(activity.dashboardEditing)
            tile(activity,2).performLongClick()
            controller.pause().stop(); controller.start().resume(); assertTrue(activity.dashboardEditing)
            tagged(activity,"dashboard_routes").performClick(); assertFalse(activity.dashboardEditing)
            assertEquals(View.GONE,tagged(activity,"dashboard_editor_finish").visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun mapPanIsBlockedOnlyInEditorAndLiveValuesContinueToUpdate() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val runtime = RideRuntime.get(activity); measure(root(activity))
            val panel = all(root(activity)).filterIsInstance<MapPanel>().single()
            fun pan() {
                val now=SystemClock.uptimeMillis()
                send(panel.mapView,MotionEvent.ACTION_DOWN,now,now,100f,100f)
                send(panel.mapView,MotionEvent.ACTION_MOVE,now,now+80,250f,100f)
                send(panel.mapView,MotionEvent.ACTION_UP,now,now+150,250f,100f)
            }
            panel.setFollow(true); tile(activity,2).performLongClick(); pan()
            assertTrue(runtime.preferences.getBoolean("mapFollow",true))
            runtime.onData(LiveData(2530,183,mapOf(LiveMetric.CADENCE to 170L)),true)
            assertTrue(all(tile(activity,2)).filterIsInstance<TextView>().any { it.text.toString().contains("25,3") })
            tagged(activity,"dashboard_editor_finish").performClick(); pan()
            assertFalse(runtime.preferences.getBoolean("mapFollow",true))
        } finally { controller.pause().stop().destroy() }
    }

    @Test @Config(qualifiers = "xhdpi") fun exitButtonRemainsClearOfMapFieldsOnPortraitViewports() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get(); val dashboard=all(root(activity)).filterIsInstance<DashboardView>().single()
            activity.setDashboardEditing(true)
            for(template in DashboardTemplate.entries.filter { it.hasMap }) for((width,height) in listOf(1080 to 2130,840 to 1680,720 to 1280)) {
                dashboard.setTemplate(template); measure(root(activity),width,height)
                val finish=tagged(activity,"dashboard_editor_finish")
                val fields=tagged(activity,"map_data_fields")
                assertTrue("$template ${width}x$height: finish bottom=${finish.y+finish.height}, fields top=${fields.top}",finish.y+finish.height<=fields.top)
                assertTrue(finish.y>=0f)
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun appActivityDeclaresPortraitOrientation() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get()
            val info=activity.packageManager.getActivityInfo(activity.componentName,0)
            assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,info.screenOrientation)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun saveFailureKeepsEditorOpenAndDoesNotReportSuccess() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        val runtime=RideRuntime.get(controller.get()); val original=runtime.preferences
        val property=RideRuntime::class.java.getDeclaredField("preferences").apply { isAccessible=true }
        try {
            val failing=object : android.content.SharedPreferences by original {
                override fun edit(): android.content.SharedPreferences.Editor {
                    val editor=original.edit()
                    return object : android.content.SharedPreferences.Editor by editor { override fun commit()=false }
                }
            }
            tile(controller.get(),2).performLongClick(); property.set(runtime,failing)
            tagged(controller.get(),"dashboard_editor_finish").performClick()
            assertTrue(controller.get().dashboardEditing)
            assertEquals(View.VISIBLE,tagged(controller.get(),"dashboard_editor_finish").visibility)
            assertEquals(controller.get().getString(R.string.dashboard_editor_save_failed),org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));assertTrue(controller.get().dashboardEditing)
        } finally { property.set(runtime,original); controller.pause().stop().destroy() }
    }
    @Test fun normalTapIsInertButEditorCanOpenAndCommitUpperAndMapFieldsDuringMovement() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val runtime = RideRuntime.get(activity)
            val tile = testDescendants(activity.findViewById(android.R.id.content)).single { it.tag == "dashboard_tile_0" }
            ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
            val now = SystemClock.elapsedRealtime()
            // Movement uses enabled live sources; send a real sensor callback rather than
            // writing only the GPS trip accumulator with GPS still disabled.
            shadowOf(activity.application).grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
            runtime.setGpsEnabled(true)
            val listener=GpsSensor::class.java.getDeclaredField("listener").apply { isAccessible=true }.get(runtime.gpsSensor) as android.location.LocationListener
            listener.onLocationChanged(android.location.Location("gps").apply {
                latitude=48.0;longitude=11.0;accuracy=3f;speed=4f;elapsedRealtimeNanos=now*1_000_000
            })
            assertEquals(1,runtime.ride.gps.accepted)
            assertTrue(runtime.isMoving)
            tile.performClick(); assertNull(ShadowAlertDialog.getLatestAlertDialog()); assertFalse(activity.dashboardEditing)
            tile(activity,2).performLongClick(); assertTrue(activity.dashboardEditing); assertNull(ShadowAlertDialog.getLatestAlertDialog())
            for (tag in listOf("dashboard_tile_0", "map_value_0")) {
                testDescendants(activity.findViewById(android.R.id.content)).single { it.tag == tag }.performClick()
                val picker = ShadowAlertDialog.getLatestAlertDialog(); assertTrue(picker.isShowing)
                selectTestSource(picker, "E-Bike")
                testDescendants(picker.window!!.decorView).single { it.tag == "CADENCE" }.performClick()
                assertTrue(picker.isShowing); assertTrue(activity.dashboardEditing)
                testBack(picker);assertFalse(picker.isShowing)
            }
            assertEquals("CADENCE", DashboardLayout.decode(runtime.preferences.getString("dashboard", null)).fields[0].id)
            assertEquals("CADENCE", MapFieldSelection.read(runtime.preferences, 0)?.id)
        } finally { controller.pause().stop().destroy() }
    }
}
