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
    @Test fun commonListFiltersBothOriginsAndKeepsFilterFixedWhileRowsScrollAndRefresh() {
        val context = android.view.ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.SettingsDialogTheme)
        val clicked = mutableListOf<ManagedRoute>()
        val view = RouteManagerView(context, { clicked += it }, { clicked += it }).apply { layoutDirection = resources.configuration.layoutDirection }
        val preview = listOf(MapPoint(50.0, 8.0, true), MapPoint(50.01, 8.01), MapPoint(50.02, 8.0))
        val entries = (0..19).map { i -> ManagedRoute("route-$i", "Testrunde $i", i % 2 == 0,
            if (i % 2 == 0) "FIT" else "GPX", RouteDetails(1_791_626_400_000, 52300.0, 9000000, preview, movingMs = 8280000), i != 0) }
        fun measure(width: Int = 1080, height: Int = 1700) {
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
            view.layout(0, 0, width, view.measuredHeight)
        }
        view.submit(entries, "route-1", false)
        shadowOf(Looper.getMainLooper()).idle(); measure()
        val list = view.findViewWithTag<ListView>("route_list")
        val filter = view.findViewWithTag<Spinner>("route_filter")
        assertEquals(20, list.adapter.count)
        val firstCard = list.adapter.getView(1, null, list)
        assertNull(firstCard.background)
        assertEquals(1, list.dividerHeight)
        assertEquals(android.graphics.Color.WHITE, (list.divider as android.graphics.drawable.ColorDrawable).color)
        val title = firstCard.findViewWithTag<TextView>("route_name_route-1")
        val date = firstCard.findViewWithTag<TextView>("route_date_route-1")
        val metrics = firstCard.findViewWithTag<TextView>("route_metrics_route-1")
        val eye = firstCard.findViewWithTag<View>("route_eye_route-1")
        val more = firstCard.findViewWithTag<View>("route_more_route-1")
        assertSame(eye.parent, firstCard.findViewWithTag<View>("route_local_route-1").parent)
        assertSame(eye.parent, firstCard.findViewWithTag<View>("route_cloud_route-1").parent)
        assertEquals(android.widget.LinearLayout.HORIZONTAL, (eye.parent as android.widget.LinearLayout).orientation)
        assertTrue(eye.paddingLeft > 3 * context.resources.displayMetrics.density)
        assertEquals(1, title.maxLines)
        assertEquals(metrics.textSize, date.textSize, 0f)
        assertEquals("52,3 km · 2:18 h", metrics.text.toString())
        assertEquals(1, metrics.maxLines)
        assertNotNull(date.compoundDrawablesRelative[0]); assertNotNull(metrics.compoundDrawablesRelative[0])
        assertTrue(list.height > 0)
        assertFalse(view.findViewWithTag<View>("route_eye_route-0").isEnabled)
        view.findViewWithTag<View>("route_more_route-0").performClick(); assertEquals(entries.first(), clicked.last())
        fun centerX(child: View): Int {
            val bounds = android.graphics.Rect(0, 0, child.width, child.height)
            view.offsetDescendantRectToMyCoords(child, bounds)
            return bounds.centerX()
        }
        assertEquals(centerX(view.findViewWithTag("route_settings")), centerX(view.findViewWithTag("route_more_route-0")))
        val top = filter.top
        list.setSelection(14); measure(); assertEquals(top, filter.top)
        filter.setSelection(1); shadowOf(Looper.getMainLooper()).idle(); measure()
        assertEquals(10, list.adapter.count); assertTrue((list.adapter.getItem(0) as ManagedRoute).recorded)
        view.submit(entries, null, true); assertEquals(1, filter.selectedItemPosition)
        assertFalse(list.adapter.getView(0, null, list).findViewWithTag<View>("route_more_route-0").isEnabled)
        filter.setSelection(2); shadowOf(Looper.getMainLooper()).idle(); measure()
        assertEquals(10, list.adapter.count); assertFalse((list.adapter.getItem(0) as ManagedRoute).recorded)
        view.submit(entries, null, false); filter.setSelection(0); shadowOf(Looper.getMainLooper()).idle(); measure()
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File("build/reports/settings/route-manager.png").apply { parentFile!!.mkdirs(); outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        measure(1950, 750); assertTrue(list.height > 0) // Short landscape dialogs retain a usable list.
    }
    @Test fun twoThemesKeepGeometryAndActionsIdenticalAcrossNarrowLandscapeAndEmptyLists() {
        val controller = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val activity = controller.get()
            val themes = ThemeManager(activity.getSharedPreferences("bike", 0))
            val statuses = mutableMapOf("recorded" to RouteUploadStatus(UploadState.WAITING, "Nextcloud-Upload ausstehend"))
            val clicked = mutableListOf<String>()
            val manager = RouteManagerView(activity, { clicked += "map:${it.id}" }, { clicked += "more:${it.id}" },
                themes = themes, uploadState = { statuses[it.id] ?: RouteUploadStatus(UploadState.NOT_UPLOADED) },
                upload = { clicked += "cloud:${it.id}" }, local = { clicked += "local:${it.id}" })
            activity.setContentView(manager)
            val preview = listOf(MapPoint(50.0, 8.0, true), MapPoint(50.01, 8.01), MapPoint(50.02, 8.0))
            val entries = listOf(
                ManagedRoute("recorded", "Meine sehr lange selbst aufgezeichnete Rennradtour im Herbst", true, "FIT", RouteDetails(1791626400000, 60700.0, 9000000, preview, 432.0, movingMs = 8400000), true, TrackOrigin.RECORDED, true),
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
            assertEquals(View.GONE, manager.findViewWithTag<View>("route_cloud_imported").visibility)
            val importedEye = manager.findViewWithTag<View>("route_eye_imported")
            val importedLocal = manager.findViewWithTag<View>("route_local_imported")
            assertEquals(importedEye.right, importedLocal.left) // No empty cloud slot.
            val importedDistance = manager.findViewWithTag<View>("route_metrics_imported")
            assertSame(importedDistance.parent.parent, importedEye.parent.parent)
            assertEquals("53,7 km · 1:53 h", (importedDistance as TextView).text.toString())
            assertEquals("1,0 km", manager.findViewWithTag<TextView>("route_metrics_unknown").text.toString())
            assertEquals(1, importedDistance.maxLines)
            assertTrue(importedDistance.layout.getLineWidth(0) <= importedDistance.width - importedDistance.compoundPaddingLeft - importedDistance.compoundPaddingRight)
            assertFalse(manager.findViewWithTag<View>("route_cloud_recorded").isEnabled)
            assertFalse(descendants(manager).filterIsInstance<TextView>().any { it.text.toString().contains("Nextcloud-Upload") })
            assertEquals(android.widget.LinearLayout.HORIZONTAL,
                (manager.findViewWithTag<View>("route_metrics_recorded").parent.parent as android.widget.LinearLayout).orientation)
            manager.findViewWithTag<View>("route_local_recorded").performClick(); assertEquals("local:recorded", clicked.last())
            manager.findViewWithTag<View>("route_more_recorded").performClick(); assertEquals("more:recorded", clicked.last())
            assertEquals(BicycleDesign.compactActionWidth * activity.resources.displayMetrics.density,
                manager.findViewWithTag<View>("route_eye_recorded").width.toFloat(), 1f)
            val light = CustomTheme("custom:light", "Light", ThemeTokens(0xffeeeeee.toInt(), 0xffcccccc.toInt(), 0xffaaaaaa.toInt(), 0xff000000.toInt()))
            assertTrue(themes.saveCustom(light).valid); assertTrue(themes.activate(light.id)); layout(1080, 1750)
            assertEquals(before, geometry())
            assertEquals(light.tokens.foreground, manager.findViewWithTag<TextView>("route_name_recorded").currentTextColor)
            snapshot("route-manager-light")
            val filter = manager.findViewWithTag<Spinner>("route_filter")
            filter.setSelection(1); shadowOf(Looper.getMainLooper()).idle(); layout(1080, 1750)
            assertEquals(1, manager.findViewWithTag<ListView>("route_list").count)
            filter.setSelection(2); shadowOf(Looper.getMainLooper()).idle(); layout(1080, 1750)
            assertEquals(1, manager.findViewWithTag<ListView>("route_list").count)
            filter.setSelection(0); shadowOf(Looper.getMainLooper()).idle(); layout(1080, 1750)
            themes.activate(BicycleThemes.red.id)
            statuses["recorded"] = RouteUploadStatus(UploadState.FAILED, "Upload fehlgeschlagen · Zugang prüfen")
            manager.submit(entries, null, false); layout(840, 1500); snapshot("route-manager-narrow")
            manager.findViewWithTag<View>("route_cloud_recorded").performClick(); assertEquals("cloud:recorded", clicked.last())
            layout(1950, 750); snapshot("route-manager-landscape")
            manager.submit(emptyList(), null, false); layout(1080, 1750); snapshot("route-manager-empty")
            assertEquals(0, manager.findViewWithTag<ListView>("route_list").count)
        } finally { controller.pause().stop().destroy() }
    }

}
