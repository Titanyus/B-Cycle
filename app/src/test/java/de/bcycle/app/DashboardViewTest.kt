package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardViewTest {
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    private fun measure(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }

    @Test fun fiveValueTemplateHasTwoTopAndThreeBottomFieldsWithCompactCaptions() {
        val clicked = mutableListOf<Int>()
        val view = DashboardView(RuntimeEnvironment.getApplication()) { clicked += it }
        view.setTemplate(DashboardTemplate.FIVE)
        view.show(DashboardLayout.defaults.replace(0, DashboardField("GPS_MOVING_AVERAGE")), LiveData())
        measure(view, 1080, 750)
        val tiles = descendants(view).filter { it.isClickable }
        assertEquals(5, tiles.size)
        assertSame(tiles[0].parent, tiles[1].parent)
        assertSame(tiles[2].parent, tiles[3].parent)
        assertSame(tiles[3].parent, tiles[4].parent)
        assertNotSame(tiles[0].parent, tiles[2].parent)
        tiles.forEach { it.performClick() }; assertEquals((0..4).toList(), clicked)
        val texts = descendants(view).filterIsInstance<TextView>().map { it.text.toString() }
        assertTrue(texts.contains("\uFFFC Ø TEMPO · FAHRT"))
        assertTrue(texts.contains("\uFFFC TEMPO"))
        assertTrue(texts.contains("KADENZ"))
        assertFalse(texts.any { it == "\uFFFC KADENZ" })
        view.show(DashboardLayout.defaults.replace(0, DashboardField("GPS_ALTITUDE")), LiveData())
        assertEquals("HÖHE", descendants(view).filterIsInstance<TextView>().first().text.toString())
        view.show(DashboardLayout.defaults.replace(0, DashboardField("GPS_MOVING_AVERAGE")), LiveData())
        val caption = descendants(view).filterIsInstance<TextView>().first().text as android.text.Spanned
        assertEquals("GPS", caption.getSpans(0, 1, SourceIconSpan::class.java).single().source)
        assertTrue(tiles.first().contentDescription.toString().contains("GPS · Ø Geschwindigkeit · Bewegung"))
    }

    @Test fun heroPresetShowsOneLargeValueAboveThreeIndependentlySelectableValues() {
        val clicked = mutableListOf<Int>()
        val view = DashboardView(RuntimeEnvironment.getApplication()) { clicked += it }
        view.setTemplate(DashboardTemplate.HERO)
        measure(view, 1080, 900)
        val tiles = descendants(view).filter { it.isClickable }
        assertEquals(4, tiles.size)
        assertTrue(tiles.first().height > tiles.last().height * 1.9)
        assertSame(tiles[1].parent, tiles[2].parent); assertSame(tiles[2].parent, tiles[3].parent)
        tiles.forEach { it.performClick() }
        assertEquals(listOf(2, 3, 4, 5), clicked)
        assertEquals(1, tiles.map { ((it as ViewGroup).getChildAt(0) as TextView).textSize }.distinct().size)
    }

    @Test fun sevenAccessibleTilesFitPortraitAndLandscapeAndEveryTileCanBeSelected() {
        val clicked = mutableListOf<Int>()
        val view = DashboardView(RuntimeEnvironment.getApplication()) { clicked += it }
        val tiles = descendants(view).filter { it.isClickable }
        assertEquals(7, tiles.size)
        for ((index, tile) in tiles.withIndex()) { tile.performClick(); assertEquals(index, clicked.last()) }
        for ((width, height) in listOf(1080 to 1950, 1950 to 750, 720 to 960)) {
            measure(view, width, height)
            for (text in descendants(view).filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }) {
                assertTrue(text.width > 0 && text.height > 0)
                val layout = requireNotNull(text.layout)
                assertTrue("${text.text} at ${width}x$height: layout=${layout.height}, cell=${text.height}", layout.height <= text.height)
                for (line in 0 until layout.lineCount) assertTrue(layout.getLineWidth(line) <= text.width + 1)
            }
        }
    }

    @Test fun compactDashboardKeepsValuesCentredAndUnitsToTheirRightWithSpaceAboveToolbar() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val content = controller.get().findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val runtime = RideRuntime.get(controller.get())
        runtime.onData(LiveData(2530, 183, mapOf(LiveMetric.CADENCE to 170L, LiveMetric.BATTERY to 96L,
            LiveMetric.ASSIST_MODE to 2L, LiveMetric.ODOMETER to 14_791_937L, LiveMetric.MOTOR_POWER to 320L),
            listOf("OFF", "ECO", "TOUR+")), true)
        for ((width, height) in listOf(1080 to 2130, 2130 to 1080, 720 to 1280)) {
            measure(content, width, height)
            val dashboard = descendants(content).filterIsInstance<DashboardView>().single()
            assertEquals(0, dashboard.top)
            assertTrue(dashboard.height < (dashboard.parent as View).height)
            val tiles = descendants(dashboard).filter { it.isClickable }.map { it as ViewGroup }
            assertEquals(7, tiles.size)
            val captionSizes = tiles.map { (it.getChildAt(0) as TextView).textSize }
            assertEquals(1, captionSizes.distinct().size)
            for (tile in tiles) {
                val row = tile.getChildAt(1) as ViewGroup
                val value = row.getChildAt(0) as TextView
                val unit = row.getChildAt(1) as TextView
                assertEquals(row.width / 2.0, (value.left + value.right) / 2.0, 1.0)
                if (unit.visibility == View.VISIBLE) {
                    assertTrue("${value.text} ${unit.text}: unit outside tile", unit.right <= row.width)
                    assertTrue(unit.left >= row.width / 2f + value.layout.getLineWidth(0) / 2f)
                    assertEquals(row.height / 2.0, (unit.top + unit.bottom) / 2.0, 1.0)
                }
            }
        }
        controller.pause().stop().destroy()
    }

    @Test fun bikeSelectionLivesInSensorManagerAlongsidePulseSelection() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        descendants(controller.get().findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
        val menu = ShadowAlertDialog.getLatestAlertDialog()
        val items = descendants(menu.window!!.decorView)
        assertFalse(items.filterIsInstance<TextView>().any { it.text.toString() == "Fahrrad auswählen" })
        items.single { it.tag == "settings_SENSORS" }.performClick()
        val manager = descendants(ShadowAlertDialog.getLatestAlertDialog().window!!.decorView).filterIsInstance<TextView>().map { it.text.toString() }
        assertTrue(manager.contains("Fahrrad auswählen"))
        assertTrue(manager.contains("Sensor auswählen"))
        controller.pause().stop().destroy()
    }

    @Test fun realAndroidRenderUsesPowerDistinctFromMotorAndShrinksLongValues() {
        val view = DashboardView(RuntimeEnvironment.getApplication()) {}
        val data = LiveData(2530, 183, mapOf(
            LiveMetric.CADENCE to 170L, LiveMetric.BATTERY to 96L,
            LiveMetric.ASSIST_MODE to 2L, LiveMetric.ODOMETER to 14_791_937L,
            LiveMetric.MOTOR_POWER to 320L,
        ), listOf("OFF", "ECO", "TOUR+"))
        view.show(DashboardLayout.defaults, data)
        measure(view, 1080, 1950)
        val descriptions = descendants(view).filter { it.isClickable }.map { it.contentDescription.toString() }
        assertTrue(descriptions[0].contains("Fahrerleistung: 183 W"))
        assertTrue(descriptions[6].contains("Motorleistung: 320 W"))
        val bitmap = Bitmap.createBitmap(1080, 1950, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val output = File("build/reports/dashboard/dashboard-portrait.png").apply { parentFile!!.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val configured = DashboardLayout.defaults.replace(2, DashboardField("RIDER_ENERGY_SHARE"))
        view.show(configured, data.copy(otherValues = data.otherValues + (LiveMetric.RIDER_ENERGY_SHARE to 37L)))
        measure(view, 1080, 1950)
        val layout = descendants(view).filterIsInstance<TextView>().single { it.text.toString() == "FAHRERANTEIL" }.layout
        assertNotNull(layout)
        assertTrue(layout.lineCount <= 2)
    }

    @Test fun tappingLargeFieldSavesItsChoiceAndActivityRestartRestoresIt() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("bike", 0).edit().clear().commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val content = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        RideRuntime.get(activity).onData(LiveData(2530, 183, mapOf(
            LiveMetric.CADENCE to 170L, LiveMetric.BATTERY to 96L,
            LiveMetric.ASSIST_MODE to 2L, LiveMetric.ODOMETER to 14_791_937L,
            LiveMetric.MOTOR_POWER to 320L,
        ), listOf("OFF", "ECO", "TOUR+")), true)
        measure(content, 1080, 2130)
        val bitmap = Bitmap.createBitmap(1080, 2130, Bitmap.Config.ARGB_8888)
        content.draw(Canvas(bitmap))
        val output = File("build/reports/dashboard/dashboard-app.png").apply { parentFile!!.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val dashboard = descendants(content).filterIsInstance<DashboardView>().single()
        descendants(dashboard).filter { it.isClickable }[2].performClick()
        val picker = ShadowAlertDialog.getLatestAlertDialog()
        descendants(picker.window!!.decorView).single { it.tag == "category_E-Bike" }.performClick()
        descendants(picker.window!!.decorView).single { it.tag == "RIDER_POWER" }.performClick()
        assertEquals("RIDER_POWER", DashboardLayout.decode(context.getSharedPreferences("bike", 0).getString("dashboard", null)).fields[2].id)
        controller.pause().stop().destroy()
        val restarted = Robolectric.buildActivity(MainActivity::class.java).setup()
        val next = descendants(restarted.get().findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single()
        measure(next, 1080, 1950)
        assertTrue(descendants(next).filter { it.isClickable }[2].contentDescription.toString().startsWith("Fahrerleistung:"))
        restarted.pause().stop().destroy()
    }

    @Test fun groupedPickerSelectsGpsSeparatelyFromBikeAndRequestsLocationOnlyWhenChosen() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("bike", 0).edit().clear().commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val dashboard = descendants(controller.get().findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single()
        measure(dashboard, 1080, 1950)
        val tiles = descendants(dashboard).filter { it.isClickable }
        tiles[2].performClick()
        val picker = ShadowAlertDialog.getLatestAlertDialog()
        val all = descendants(picker.window!!.decorView)
        assertEquals(3, all.count { it.tag?.toString()?.startsWith("category_") == true })
        assertEquals(0, all.count { it.tag == "GPS_DISTANCE" }) // Initially collapsed.
        all.single { it.tag == "category_GPS" }.performClick()
        val expanded = descendants(picker.window!!.decorView)
        assertEquals(1, expanded.count { it.tag == "GPS_DISTANCE" })
        expanded.single { it.tag == "GPS_SPEED" }.performClick()
        val prefs = context.getSharedPreferences("bike", 0)
        assertEquals("GPS_SPEED", DashboardLayout.decode(prefs.getString("dashboard", null)).fields[2].id)
        assertTrue(tiles[2].contentDescription.toString().startsWith("GPS · Geschwindigkeit:"))
        assertTrue(tiles[0].contentDescription.toString().startsWith("Fahrerleistung:"))
        controller.pause().stop().destroy()
    }
}
