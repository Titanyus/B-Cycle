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
            if (i % 2 == 0) "FIT" else "GPX", RouteDetails(1_791_626_400_000, 52300.0, 8280000, preview), i != 0) }
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
        val distance = firstCard.findViewWithTag<TextView>("route_distance_route-1")
        val duration = firstCard.findViewWithTag<TextView>("route_duration_route-1")
        val eye = firstCard.findViewWithTag<View>("route_eye_route-1")
        val more = firstCard.findViewWithTag<View>("route_more_route-1")
        assertSame(eye.parent, more.parent)
        assertEquals(android.widget.LinearLayout.HORIZONTAL, (eye.parent as android.widget.LinearLayout).orientation)
        assertTrue(eye.paddingLeft > 3 * context.resources.displayMetrics.density)
        assertEquals(1, title.maxLines)
        assertEquals(distance.textSize, date.textSize, 0f); assertEquals(duration.textSize, date.textSize, 0f)
        assertNotNull(date.compoundDrawablesRelative[0]); assertNotNull(distance.compoundDrawablesRelative[0]); assertNotNull(duration.compoundDrawablesRelative[0])
        assertTrue(list.height > 0)
        assertEquals(View.GONE, view.findViewWithTag<View>("route_eye_route-0").visibility)
        view.findViewWithTag<View>("route_more_route-0").performClick(); assertEquals(entries.first(), clicked.last())
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
}
