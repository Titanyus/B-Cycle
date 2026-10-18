package de.bcycle.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteManagerViewTest {
    private fun descendants(view: View): List<View> = listOf(view) + if (view is android.view.ViewGroup)
        (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    @Test fun archiveProgressAndSummaryRemainVisibleWithoutChangingFilterOrRows() {
        val context = android.view.ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.SettingsDialogTheme)
        val view = RouteManagerView(context, {})
        view.submit(emptyList(), null, true)
        val progress = context.getString(R.string.archive_import_progress, 12)
        view.showImportStatus(progress)
        assertTrue(descendants(view).filterIsInstance<TextView>().any { it.text.toString() == progress && it.visibility == View.VISIBLE })
        val summary = context.getString(R.string.archive_import_result, 10, 2, 0, 1)
        view.showImportStatus(summary)
        view.submit(emptyList(), null, false)
        assertTrue(descendants(view).filterIsInstance<TextView>().any { it.text.toString() == summary && it.visibility == View.VISIBLE })
        assertEquals(0, view.findViewWithTag<ListView>("route_list").adapter.count)
        view.showImportStatus("")
        assertTrue(descendants(view).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.ui_routes_empty) })
    }

    @Test fun commonListFiltersBothOriginsAndKeepsFilterFixedWhileRowsScrollAndRefresh() {
        val context = android.view.ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.SettingsDialogTheme)
        val clicked = mutableListOf<ManagedRoute>()
        val view = RouteManagerView(context, { clicked += it }, details = { clicked += it }).apply { layoutDirection = resources.configuration.layoutDirection }
        val preview = listOf(MapPoint(50.0, 8.0, true), MapPoint(50.01, 8.01), MapPoint(50.02, 8.0))
        val entries = (0..19).map { i -> ManagedRoute("route-$i", "Testrunde $i", i % 2 == 0,
            if (i % 2 == 0) "FIT" else "GPX", RouteDetails(1_791_626_400_000, 52300.0, 9000000, preview, movingMs = 8280000,bikeType=BikeType.EBIKE), i != 0, locallySaved = true) }
        fun measure(width: Int = 1080, height: Int = 1700) {
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
            view.layout(0, 0, width, view.measuredHeight)
        }
        view.submit(entries, "route-1", false)
        shadowOf(Looper.getMainLooper()).idle(); measure()
        val list = view.findViewWithTag<ListView>("route_list")
        assertNull(view.findViewWithTag<View>("route_filter"))
        assertEquals(20, list.adapter.count)
        val firstCard = list.adapter.getView(1, null, list)
        assertNull(firstCard.background)
        assertEquals(BicycleDesign.linePixels(context), list.dividerHeight)
        assertEquals((255*BicycleDesign.dividerAlpha).toInt(),list.divider!!.alpha)
        assertEquals(android.graphics.Color.WHITE and 0x00ffffff,(list.divider as android.graphics.drawable.ColorDrawable).color and 0x00ffffff)
        val title = firstCard.findViewWithTag<TextView>("route_name_route-1")
        val date = firstCard.findViewWithTag<TextView>("route_date_route-1")
        val eye = firstCard.findViewWithTag<View>("route_eye_route-1")
        assertSame(title.parent, eye.parent)
        assertEquals(1, title.maxLines)
        assertEquals("52,3", firstCard.findViewWithTag<TextView>("route_distance_route-1").text.toString())
        assertEquals("2:18", firstCard.findViewWithTag<TextView>("route_moving_route-1").text.toString())
        assertEquals("22,7", firstCard.findViewWithTag<TextView>("route_average_route-1").text.toString())
        assertNull(date.compoundDrawablesRelative[0])
        val at = java.time.Instant.ofEpochMilli(entries[1].details.dateMs).atZone(java.time.ZoneId.systemDefault())
        assertEquals(at.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yy")), date.text.toString())
        val metricRow=firstCard.findViewWithTag<View>("route_metrics_route-1")
        val margin=(metricRow.layoutParams as android.widget.LinearLayout.LayoutParams).topMargin
        val density=view.resources.displayMetrics.density
        assertEquals(-(BicycleDesign.metadataSpacingTrim*density).toInt()+(3*density).toInt(),margin)
        assertEquals(at.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm 'h'")), firstCard.findViewWithTag<TextView>("route_clock_route-1").text.toString())
        assertNull(firstCard.findViewWithTag<View>("route_more_route-1"))
        assertNull(firstCard.findViewWithTag<View>("route_cloud_route-1"))
        assertNull(firstCard.findViewWithTag<View>("route_local_route-1"))
        assertNull(firstCard.findViewWithTag<View>("route_storage_route-1"))
        assertTrue(list.height > 0)
        assertEquals(View.GONE,view.findViewWithTag<View>("route_eye_route-0").visibility)
        firstCard.performClick(); assertEquals(entries[1], clicked.last())
        val count = clicked.size
        eye.performClick(); assertEquals(count + 1, clicked.size); assertEquals(entries[1], clicked.last())
        assertNull(view.findViewWithTag<View>("route_settings"))
        list.setSelection(14); measure()
        view.query.kinds=setOf(RouteKind.EBIKE);view.refreshQuery(); shadowOf(Looper.getMainLooper()).idle(); measure()
        assertEquals(10, list.adapter.count); assertTrue((list.adapter.getItem(0) as ManagedRoute).recorded)
        view.submit(entries, null, true); assertEquals(setOf(RouteKind.EBIKE),view.query.kinds)
        assertFalse(descendants(view).filterIsInstance<TextView>().any { it.text.toString().contains("Dateien werden verarbeitet") })
        assertFalse(list.adapter.getView(0, null, list).findViewWithTag<View>("route_eye_route-0").isEnabled)
        view.query.kinds=setOf(RouteKind.IMPORTED);view.refreshQuery(); shadowOf(Looper.getMainLooper()).idle(); measure()
        assertEquals(10, list.adapter.count); assertFalse((list.adapter.getItem(0) as ManagedRoute).recorded)
        view.submit(entries, null, false); view.query.clearFilters();view.refreshQuery(); shadowOf(Looper.getMainLooper()).idle(); measure()
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File("build/reports/settings/route-manager.png").apply { parentFile!!.mkdirs(); outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        measure(840, 1500); assertTrue(list.height > 0) // Narrow portrait dialogs retain a usable list.
    }
    @Test fun twoThemesKeepGeometryAndActionsIdenticalAcrossPortraitSizesAndEmptyLists() {
        val controller = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val activity = controller.get()
            val themes = ThemeManager(activity.getSharedPreferences("bike", 0))
            val clicked = mutableListOf<String>()
            val manager = RouteManagerView(activity, { clicked += "map:${it.id}" },
                themes = themes, details = { clicked += "details:${it.id}" })
            activity.setContentView(manager)
            val preview = listOf(MapPoint(50.0, 8.0, true), MapPoint(50.01, 8.01), MapPoint(50.02, 8.0))
            val entries = listOf(
                ManagedRoute("recorded", "Meine sehr lange selbst aufgezeichnete Rennradtour im Herbst", true, "FIT", RouteDetails(1791626400000, 60700.0, 9000000, preview, 432.0, movingMs = 8400000,bikeType=BikeType.EBIKE), true, TrackOrigin.RECORDED, true),
                ManagedRoute("imported", "Importierte Strecke", false, "GPX", RouteDetails(1791626400000, 53700.0, 7500000, preview, movingMs = 6780000), true, TrackOrigin.IMPORTED, true),
                ManagedRoute("unknown", "Fehlende Originaldatei", false, "FIT", RouteDetails(distanceM = 1000.0, durationMs = 600000), false, TrackOrigin.UNKNOWN, false))
            manager.submit(entries, "recorded", false)
            fun layout(width: Int, height: Int) {
                manager.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
                manager.layout(0, 0, width, manager.measuredHeight)
            }
            fun descendants(view: View): List<View> = listOf(view) + if (view is android.view.ViewGroup)
                (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
            fun geometry() = descendants(manager).map { listOf(it.left, it.top, it.width, it.height, it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom) }
            fun snapshot(name: String) {
                val bitmap = Bitmap.createBitmap(manager.width, manager.height, Bitmap.Config.ARGB_8888)
                manager.draw(Canvas(bitmap))
                File("build/reports/settings/$name.png").apply { parentFile!!.mkdirs(); outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            }
            layout(1080, 1750); snapshot("route-manager-red")
            val before = geometry()
            val importedEye = manager.findViewWithTag<View>("route_eye_imported")
            val importedName = manager.findViewWithTag<View>("route_name_imported")
            assertSame(importedName.parent, importedEye.parent)
            assertEquals("53,7", manager.findViewWithTag<TextView>("route_distance_imported").text.toString())
            assertEquals("1:53", manager.findViewWithTag<TextView>("route_moving_imported").text.toString())
            assertEquals("28,5", manager.findViewWithTag<TextView>("route_average_imported").text.toString())
            assertEquals("1,0", manager.findViewWithTag<TextView>("route_distance_unknown").text.toString())
            assertEquals("", manager.findViewWithTag<TextView>("route_moving_unknown").text.toString())
            assertEquals("", manager.findViewWithTag<TextView>("route_average_unknown").text.toString())
            assertFalse(manager.findViewWithTag<View>("route_eye_unknown").isEnabled)
            assertFalse(descendants(manager).filterIsInstance<TextView>().any { it.text.toString().contains("Nextcloud-Upload") })
            assertFalse(descendants(manager).any { it.tag?.toString()?.startsWith("route_cloud_") == true || it.tag?.toString()?.startsWith("route_local_") == true || it.tag?.toString()?.startsWith("route_more_") == true })
            importedEye.performClick(); assertEquals(listOf("map:imported"), clicked)
            manager.findViewWithTag<View>("route_entry_imported").performClick(); assertEquals("details:imported", clicked.last())
            assertEquals(BicycleDesign.touchSize * activity.resources.displayMetrics.density,
                manager.findViewWithTag<View>("route_eye_recorded").width.toFloat(), 1f)
            descendants(manager.findViewWithTag<View>("route_metrics_recorded")).filterIsInstance<AnalysisValueRow>().forEach { row ->
                val value = row.getChildAt(0) as TextView; val unit = row.getChildAt(1) as TextView
                assertEquals(BicycleDesign.dashboardUnitColor, unit.currentTextColor)
                if (unit.visibility != View.GONE) {
                    assertEquals((value.top + value.bottom) / 2f, (unit.top + unit.bottom) / 2f, 1f)
                    assertTrue(value.textSize > unit.textSize)
                }
                assertEquals(1, value.maxLines)
            }
            val readings = descendants(manager.findViewWithTag<View>("route_metrics_recorded")).filterIsInstance<AnalysisValueRow>()
            assertEquals(4, readings.size)
            val separators = descendants(manager.findViewWithTag<View>("route_metrics_recorded")).filter { it.tag == "route_metric_divider" }
            assertEquals(3, separators.size)
            separators.forEach { assertEquals(BicycleDesign.linePixels(activity), it.width); assertTrue(it.height > 0) }
            readings.forEach { reading ->
                val label = (reading.parent as android.widget.LinearLayout).getChildAt(1) as TextView
                assertEquals(android.view.Gravity.CENTER, label.gravity)
                assertEquals(reading.width, label.width)
            }
            assertTrue(descendants(manager.findViewWithTag<View>("route_metrics_recorded")).filterIsInstance<TextView>().any { it.text.toString() == "Zeit" })
            assertFalse(descendants(manager).any { it.tag?.toString()?.startsWith("route_storage_") == true })
            val light = CustomTheme("custom:light", "Light", ThemeTokens(0xffeeeeee.toInt(), 0xffcccccc.toInt(), 0xffaaaaaa.toInt(), 0xff000000.toInt()))
            assertTrue(themes.saveCustom(light).valid); assertTrue(themes.activate(light.id)); layout(1080, 1750)
            assertEquals(before, geometry())
            assertEquals(light.tokens.foreground, manager.findViewWithTag<TextView>("route_name_recorded").currentTextColor)
            snapshot("route-manager-light")
            assertNull(manager.findViewWithTag<View>("route_filter"))
            manager.query.kinds=setOf(RouteKind.EBIKE);manager.refreshQuery(); shadowOf(Looper.getMainLooper()).idle(); layout(1080, 1750)
            assertEquals(1, manager.findViewWithTag<ListView>("route_list").count)
            manager.query.kinds=setOf(RouteKind.IMPORTED);manager.refreshQuery(); shadowOf(Looper.getMainLooper()).idle(); layout(1080, 1750)
            assertEquals(1, manager.findViewWithTag<ListView>("route_list").count)
            manager.query.clearFilters();manager.refreshQuery(); shadowOf(Looper.getMainLooper()).idle(); layout(1080, 1750)
            themes.activate(BicycleThemes.red.id)
            manager.submit(entries, null, false); layout(840, 1500); snapshot("route-manager-narrow")
            descendants(manager.findViewWithTag<View>("route_metrics_recorded")).filterIsInstance<TextView>().filter { it.tag != null }.forEach {
                assertTrue(it.layout.getLineWidth(0) <= it.width + 1)
            }
            manager.submit(entries, null, true); layout(840, 1500)
            val count = clicked.size
            manager.findViewWithTag<View>("route_eye_recorded").performClick()
            manager.findViewWithTag<View>("route_entry_recorded").performClick()
            assertEquals(count, clicked.size)
            manager.submit(entries, null, false)
            layout(960, 1750); snapshot("route-manager-portrait")
            manager.submit(emptyList(), null, false); layout(1080, 1750); snapshot("route-manager-empty")
            assertEquals(0, manager.findViewWithTag<ListView>("route_list").count)
        } finally { controller.pause().stop().destroy() }
    }

}
