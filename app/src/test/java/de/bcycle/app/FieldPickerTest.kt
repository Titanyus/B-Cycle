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
    @Test fun selectedSourceAndDropdownRowsReuseDashboardIconsWithThemeForeground() {
        val context=RuntimeEnvironment.getApplication()
        val themes=ThemeManager(context.getSharedPreferences("source-icons",0))
        val view=FieldPickerView(context,LiveData(),GpsValues(),null,PulseValues(),DashboardField("RIDER_POWER"),false,themes) {}
        val spinner=testDescendants(view).single { it.tag=="field_source" } as Spinner
        val sources=listOf("GPS","E-Bike","Pulsmesser","Wetter")
        for((index,source) in sources.withIndex()) for(dropdown in listOf(false,true)) {
            val row=(if(dropdown) spinner.adapter.getDropDownView(index,null,spinner) else spinner.adapter.getView(index,null,spinner)) as android.widget.TextView
            assertEquals(source,spinner.adapter.getItem(index));assertEquals(source,row.contentDescription)
            assertEquals("\uFFFC $source",row.text.toString())
            val text=row.text as android.text.Spanned
            val icon=text.getSpans(0,text.length,SourceIconSpan::class.java).single()
            assertEquals(source,icon.source)
            // Verify tint through native drawing without depending on vector/raster internals.
            val bitmap=android.graphics.Bitmap.createBitmap(40,40,android.graphics.Bitmap.Config.ARGB_8888)
            icon.draw(android.graphics.Canvas(bitmap),row.text,0,1,3f,0,30,40,android.graphics.Paint().apply { textSize=24f })
            val colors=(0 until 40).flatMap { y -> (0 until 40).map { x -> bitmap.getPixel(x,y) } }.filter { android.graphics.Color.alpha(it)>0 }
            assertTrue(colors.isNotEmpty());assertTrue(colors.all { it and 0x00ffffff == themes.active.tokens.foreground and 0x00ffffff })
        }
        val row=spinner.adapter.getView(0,null,spinner) as android.widget.TextView
        val reused=spinner.adapter.getView(3,row,spinner) as android.widget.TextView
        assertSame(row,reused);assertEquals("Wetter",(reused.text as android.text.Spanned).getSpans(0,reused.text.length,SourceIconSpan::class.java).single().source)
    }
    @Test fun graphChoicesHaveTheirCorrespondingDashboardColoredDotsAndKeepSelectionLimit() {
        val context=RuntimeEnvironment.getApplication();val themes=ThemeManager(context.getSharedPreferences("graph-dots-picker",0))
        val picker=GraphOverlayPicker(context,themes,emptyList())
        val choices=testDescendants(picker).filterIsInstance<android.widget.CheckBox>()
        assertFalse(choices.first().text is android.text.Spanned)
        AnalysisChart.entries.forEachIndexed { index,chart ->
            val box=choices[index+1];val text=box.text as android.text.Spanned
            assertTrue(box.contentDescription.isNotEmpty());val dot=text.getSpans(0,text.length,RecordingDotSpan::class.java).single()
            val b=android.graphics.Bitmap.createBitmap(30,30,android.graphics.Bitmap.Config.ARGB_8888)
            dot.draw(android.graphics.Canvas(b),text,0,1,0f,0,20,30,android.graphics.Paint().apply { textSize=18f })
            assertTrue((0 until 30).any { y -> (0 until 30).any { x -> b.getPixel(x,y)==chart.color(android.graphics.Color.BLACK) } })
        }
        choices[1].performClick();choices[2].performClick()
        assertEquals(listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE),picker.selected)
        assertFalse(choices[3].isEnabled);choices[1].performClick();assertTrue(choices[3].isEnabled)
    }

}
