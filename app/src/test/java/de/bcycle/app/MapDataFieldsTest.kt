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
    @Test fun managerSelectsClearsAndPersistsMapFieldsWithoutChangingDashboardAndReturnsToParent() {
        val app = RuntimeEnvironment.getApplication()
        val preferences = app.getSharedPreferences("bike", 0)
        preferences.edit().clear().commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        fun latest() = ShadowAlertDialog.getLatestAlertDialog()
        fun click(dialog: AlertDialog, tag: String) = descendants(dialog.window!!.decorView).single { it.tag == tag }.performClick()
        try {
            val activity = controller.get()
            descendants(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            click(latest(), "settings_DASHBOARDS")
            val manager = latest()
            click(manager, "map_fields_manage")
            val fields = latest()
            click(fields, "map_field_slot_0")
            val picker = latest()
            selectTestSource(picker, "E-Bike"); click(picker, "BATTERY")
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals("BATTERY", MapFieldSelection.read(preferences, 0)?.id)
            assertNull(preferences.getString("dashboard", null))
            assertEquals(View.VISIBLE, fields.window!!.decorView.visibility)
            assertTrue((descendants(fields.window!!.decorView).single { it.tag == "map_field_slot_0" } as TextView).text.contains("Akku"))
            click(fields, "map_field_slot_1")
            val cancelled = latest(); selectTestSource(cancelled, "GPS"); cancelled.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertNull(MapFieldSelection.read(preferences, 1))
            fields.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
        val restarted = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            assertEquals("BATTERY", MapFieldSelection.read(preferences, 0)?.id)
            descendants(restarted.get().findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            click(latest(), "settings_DASHBOARDS"); click(latest(), "map_fields_manage"); click(latest(), "map_field_slot_0")
            click(latest(), "map_field_empty")
            assertNull(MapFieldSelection.read(preferences, 0))
        } finally { restarted.pause().stop().destroy() }
    }
    @Test fun valuesRemainAboveAttributionAcrossPagesFullscreenAndAutoHiddenControls() {
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
                assertTrue(fields.bottom <= credit.top)
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
}
