package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.ScrollView
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldPickerTest {
    @Test fun sourceDropdownStaysFixedWhileOnlyTheValuesScrollAndSwitchingSourcesReplacesList() {
        val selected = mutableListOf<DashboardField?>()
        val view = FieldPickerView(RuntimeEnvironment.getApplication(), LiveData(), GpsValues(), null,
            PulseValues(), DashboardField("RIDER_POWER"), true) { selected += it }
        fun measure() {
            view.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.AT_MOST))
            view.layout(0, 0, 720, view.measuredHeight)
        }
        measure(); shadowOf(Looper.getMainLooper()).idle(); measure()
        val dropdown = testDescendants(view).single { it.tag == "field_source" } as Spinner
        val list = testDescendants(view).single { it.tag == "field_list" } as ScrollView
        assertEquals("E-Bike", dropdown.selectedItem)
        assertTrue(list.height > 0)
        assertTrue(list.getChildAt(0).height > list.height)
        assertFalse(testDescendants(list).contains(dropdown))
        val top = dropdown.top
        list.fullScroll(View.FOCUS_DOWN); shadowOf(Looper.getMainLooper()).idle()
        assertTrue(list.scrollY > 0); assertEquals(top, dropdown.top)
        dropdown.setSelection(0); shadowOf(Looper.getMainLooper()).idle(); measure()
        assertTrue(testDescendants(list).any { it.tag == "GPS_SPEED" })
        assertFalse(testDescendants(list).any { it.tag == "RIDER_POWER" })
        testDescendants(list).single { it.tag == "GPS_DISTANCE" }.performClick()
        assertEquals(DashboardField("GPS_DISTANCE"), selected.last())
        dropdown.setSelection(2); shadowOf(Looper.getMainLooper()).idle(); measure()
        assertTrue(testDescendants(list).any { it.tag == "HEART_RATE" })
        assertFalse(testDescendants(list).any { it.tag == "GPS_SPEED" })
        dropdown.setSelection(3); shadowOf(Looper.getMainLooper()).idle(); measure()
        assertEquals("Wetter", dropdown.selectedItem)
        assertTrue(testDescendants(list).any { it.tag == "WEATHER_SUNRISE" })
        testDescendants(list).single { it.tag == "WEATHER_DAYLIGHT" }.performClick()
        assertEquals(DashboardField("WEATHER_DAYLIGHT"), selected.last())
        testDescendants(view).single { it.tag == "map_field_empty" }.performClick()
        assertNull(selected.last())
    }
}
