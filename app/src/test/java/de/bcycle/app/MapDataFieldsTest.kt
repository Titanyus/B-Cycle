package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapDataFieldsTest {
    private fun descendants(view: View): List<View> = listOf(view) + if (view is ViewGroup)
        (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    private fun measure(view: View) {
        view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(2100, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 1080, 2100)
    }
    @Test fun transparentValuesUseExistingFormattersHideUnavailableSlotsAndPassTouchesToMap() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("map_values_test", 0)
        preferences.edit().clear().putString(MapFieldSelection.key(0), "GPS_ELAPSED_TIME")
            .putString(MapFieldSelection.key(2), "BATTERY").commit()
        val view = MapDataFieldsView(context)
        view.reload(preferences)
        view.show(LiveData(otherValues = mapOf(LiveMetric.BATTERY to 75L)), GpsValues(elapsedMs = 3_661_000), null, PulseValues())
        val text = descendants(view).filterIsInstance<TextView>()
        assertEquals(listOf("1:01:01", "", "75"), text.map { it.text.toString() })
        assertEquals(listOf(View.VISIBLE, View.INVISIBLE, View.VISIBLE), text.map { it.visibility })
        assertNull(view.background)
        text.forEach { assertEquals(Color.BLACK, it.currentTextColor); assertNull(it.background); assertFalse(it.isClickable) }
        measure(view)
        assertTrue(text[0].left < text[1].left && text[1].left < text[2].left)
        text.filter { it.visibility == View.VISIBLE }.forEach { assertTrue(it.layout.height <= it.height) }
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        assertFalse(view.dispatchTouchEvent(down)); down.recycle()
        view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(144, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 1080, 144)
        val preview = android.graphics.Bitmap.createBitmap(1080, 144, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(preview); canvas.drawColor(Color.rgb(225, 225, 225)); view.draw(canvas)
        val output = java.io.File("build/reports/settings/map-data-fields.png").apply { parentFile!!.mkdirs() }
        output.outputStream().use { preview.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }

        view.show(LiveData(), GpsValues(), null, PulseValues())
        assertEquals(View.GONE, view.visibility)
        assertTrue(text.all { it.text.isEmpty() })
        preferences.edit().clear().putString(MapFieldSelection.key(0), "invalid").commit()
        view.reload(preferences)
        view.show(LiveData(otherValues = mapOf(LiveMetric.BATTERY to 0L)), GpsValues(), null, PulseValues())
        assertEquals(View.GONE, view.visibility)
        preferences.edit().putString(MapFieldSelection.key(2), "BATTERY").commit(); view.reload(preferences)
        view.show(LiveData(otherValues = mapOf(LiveMetric.BATTERY to 0L)), GpsValues(), null, PulseValues())
        assertEquals("0", text[2].text.toString()) // Valid zero is not an empty field.
    }
    @Test fun liveMapValueReusesHiddenAverageTrendInsteadOfInstantaneousComparison() {
        val context=RuntimeEnvironment.getApplication();val prefs=context.getSharedPreferences("map_trend_test",0)
        prefs.edit().clear().putString(MapFieldSelection.key(0),"HEART_RATE").commit()
        val view=MapDataFieldsView(context);view.reload(prefs)
        val text=descendants(view).filterIsInstance<TextView>().first()
        for(second in 0..10) {
            if(second>0) org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1))
            view.show(LiveData(),GpsValues(),null,PulseValues(bpm=100,average=130.0+second))
            if(second<8) assertEquals("100",text.text.toString()) else {
                val span=text.text as android.text.Spanned
                assertEquals(1,span.getSpans(0,span.length,AverageChevronSpan::class.java).size)
            }
        }
        // Equality of direct and average values does not hide a rising average's trend.
        view.show(LiveData(),GpsValues(),null,PulseValues(bpm=140,average=140.0))
        val span=text.text as android.text.Spanned
        assertEquals(1,span.getSpans(0,span.length,AverageChevronSpan::class.java).size)
        view.show(LiveData(),GpsValues(),null,PulseValues(bpm=145))
        assertEquals("145",text.text.toString())
    }

    @Test fun editorSelectsClearsAndPersistsMapFieldsWithoutChangingDashboard() {
        val app = RuntimeEnvironment.getApplication()
        val preferences = app.getSharedPreferences("bike", 0)
        preferences.edit().clear().commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        fun latest() = ShadowAlertDialog.getLatestAlertDialog()
        fun click(dialog: AlertDialog, tag: String) = descendants(dialog.window!!.decorView).single { it.tag == tag }.performClick()
        fun fields(activity: MainActivity) = descendants(activity.findViewById(android.R.id.content))
        fun enter(activity: MainActivity) {
            fields(activity).single { it.contentDescription == "Menü öffnen" }.performClick()
            click(latest(), "settings_DASHBOARDS"); click(latest(), "dashboard_editor_open")
            assertFalse(latest().isShowing); assertTrue(activity.dashboardEditing)
        }
        try {
            val activity = controller.get(); enter(activity)
            fields(activity).single { it.tag == "map_value_0" }.performClick()
            selectTestSource(latest(), "GPS"); click(latest(), "GPS_SPEED")
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals("GPS_SPEED", MapFieldSelection.read(preferences, 0)?.id)
            assertNull(preferences.getString("dashboard", null)); assertTrue(activity.dashboardEditing)
            fields(activity).single { it.tag == "map_value_1" }.performClick()
            testBack(latest())
            assertNull(MapFieldSelection.read(preferences, 1))
            fields(activity).single { it.tag == "dashboard_editor_finish" }.performClick()
            assertFalse(activity.dashboardEditing)
        } finally { controller.pause().stop().destroy() }
        val restarted = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            assertFalse(restarted.get().dashboardEditing)
            assertEquals("GPS_SPEED", MapFieldSelection.read(preferences, 0)?.id)
            enter(restarted.get()); fields(restarted.get()).single { it.tag == "map_value_0" }.performClick()
            click(latest(), "map_field_empty")
            assertNull(MapFieldSelection.read(preferences, 0))
        } finally { restarted.pause().stop().destroy() }
    }
    @Test fun valuesRemainClearOfRotatedAttributionAcrossPagesFullscreenAndAutoHiddenControls() {
        val app = RuntimeEnvironment.getApplication()
        val preferences = app.getSharedPreferences("bike", 0)
        preferences.edit().clear().putString(MapFieldSelection.key(0), "BATTERY").commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val fields = descendants(panel).filterIsInstance<MapDataFieldsView>().single()
            fun display() {
                fields.show(LiveData(otherValues = mapOf(LiveMetric.BATTERY to 75L)), GpsValues(), null, PulseValues())
                measure(root)
                val credit = descendants(panel).single { it.contentDescription == "Kartenquelle, Urheber und Legende" }
                assertTrue(credit is MapCreditView)
                assertEquals(-90f, (credit as MapCreditView).label.rotation, 0f)
                assertEquals(0, credit.left); assertEquals(panel.height, credit.bottom)
                assertTrue(credit.top >= 0 && credit.width > 0)
                assertTrue(fields.left >= credit.right)
                val zoom = descendants(panel).single { it.tag == "map_zoom_controls" }
                assertEquals(fields.top-BicycleDesign.dp(panel.context,BicycleDesign.smallGap),zoom.bottom)
                assertEquals(0,panel.height-fields.bottom)
                assertTrue(credit.right <= zoom.left || zoom.visibility != View.VISIBLE)
                assertTrue(fields.top >= 0)
            }
            display()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6)); display()
            assertEquals(View.VISIBLE, fields.visibility)
            repeat(3) {
                swipeTestDashboard(root); display()
                assertSame(fields, descendants(panel).filterIsInstance<MapDataFieldsView>().single())
            }
            descendants(panel).single { it.contentDescription == "Karte im Vollbild" }.performClick(); display()
            assertEquals("BATTERY", MapFieldSelection.read(preferences, 0)?.id)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun recordingDotAppearsBeforeMiddleValueOrAloneAndDisappearsOnStop() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("record-dot-test", 0)
        val view = MapDataFieldsView(context)
        view.reload(prefs)
        fun show(recording: Boolean) = view.show(LiveData(), GpsValues(elapsedMs = 42000), null, PulseValues(), recording)
        show(false); assertEquals(View.GONE, view.visibility)
        show(true)
        val middle = descendants(view).filterIsInstance<TextView>()[1]
        assertEquals(View.VISIBLE, view.visibility); assertEquals(View.VISIBLE, middle.visibility)
        assertEquals("Aufzeichnung läuft", middle.contentDescription)
        assertFalse(middle.isClickable)
        view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(144, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 1080, 144)
        val bitmap = android.graphics.Bitmap.createBitmap(1080, 144, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val red = mutableListOf<Int>()
        for (y in 0 until 144) for (x in 0 until 1080) {
            val color = bitmap.getPixel(x, y)
            if (Color.alpha(color) > 100 && Color.red(color) > 200 && Color.green(color) < 40) red += x
        }
        assertTrue(red.isNotEmpty()); assertEquals(540.0, red.average(), 2.0)
        prefs.edit().putString(MapFieldSelection.key(1), "GPS_ELAPSED_TIME").commit()
        view.reload(prefs); show(true)
        assertTrue(middle.text.toString().endsWith("0:00:42"))
        assertTrue(middle.contentDescription.toString().startsWith("Aufzeichnung läuft"))
        show(false)
        assertEquals("0:00:42", middle.text.toString())
        assertFalse(middle.contentDescription.toString().contains("Aufzeichnung läuft"))
        prefs.edit().clear().commit(); view.reload(prefs); show(false)
        assertEquals(View.GONE, view.visibility)
    }

    @Test fun lightIconIsRedAfterMiddleValueAndWorksTogetherWithRecordDotOrEmptyField() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("light_overlay_test", 0)
        prefs.edit().clear().putString(MapFieldSelection.key(1), "GPS_ELAPSED_TIME").commit()
        val view = MapDataFieldsView(context); view.reload(prefs)
        val middle = descendants(view).filterIsInstance<TextView>()[1]
        fun show(recording: Boolean, light: Boolean?) = view.show(LiveData(), GpsValues(elapsedMs = 42000), null, PulseValues(), recording, light)
        show(false, true)
        assertTrue(middle.text.toString().startsWith("0:00:42 "))
        assertTrue(middle.contentDescription.toString().contains("Fahrradlicht an"))
        assertFalse(middle.isClickable)
        view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(144, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 1080, 144)
        val bitmap = android.graphics.Bitmap.createBitmap(1080, 144, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val red = mutableListOf<Int>()
        for (y in 0 until 144) for (x in 0 until 1080) {
            val c = bitmap.getPixel(x, y)
            if (Color.alpha(c) > 100 && Color.red(c) > 200 && Color.green(c) < 40) red += x
        }
        assertTrue(red.isNotEmpty()); assertTrue(red.min() > 540)
        show(true, true)
        assertEquals(2, middle.text.count { it == '\uFFFC' })
        assertTrue(middle.contentDescription.toString().contains("Aufzeichnung läuft"))
        show(false, false); assertEquals("0:00:42", middle.text.toString())
        prefs.edit().clear().commit(); view.reload(prefs)
        show(false, true); assertEquals(View.VISIBLE, view.visibility)
        show(false, null); assertEquals(View.GONE, view.visibility)
        show(true, null); assertEquals(1, middle.text.count { it == '\uFFFC' })
        show(false, true); assertEquals("Fahrradlicht an", middle.contentDescription)
    }

}
