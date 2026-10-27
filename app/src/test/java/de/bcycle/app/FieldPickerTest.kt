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
    @Test fun oneScrollHostShowsEveryValueBeforeTrainingHelpAndSwitchingSourcesReplacesList() {
        val selected = mutableListOf<DashboardField?>()
        val themes=ThemeManager(RuntimeEnvironment.getApplication().getSharedPreferences("bike",0)).apply { setFlatDesign(false) }
        val view = FieldPickerView(RuntimeEnvironment.getApplication(), LiveData(), GpsValues(), null,
            PulseValues(), DashboardField("RIDER_POWER"), true,themes=themes,
            trainingHelp=TrainingHelpView(RuntimeEnvironment.getApplication(),ThemeManager(RuntimeEnvironment.getApplication().getSharedPreferences("theme",0)),DashboardTrainingHelp()) {}) { selected += it }
        view.findViewWithTag<View>("field_data_toggle").performClick()
        fun measure() {
            view.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.AT_MOST))
            view.layout(0, 0, 720, view.measuredHeight)
        }
        measure(); shadowOf(Looper.getMainLooper()).idle(); measure()
        val dropdown = testDescendants(view).single { it.tag == "field_source" } as Spinner
        val list = testDescendants(view).single { it.tag == "field_list" } as android.widget.LinearLayout
        val scroll=view.findViewWithTag<ScrollView>("field_picker_scroll")
        assertEquals(1,testDescendants(view).filterIsInstance<ScrollView>().size)
        assertEquals("E-Bike", dropdown.selectedItem)
        assertTrue(list.height > 0)
        assertEquals(BicycleDesign.dp(view.context,BicycleDesign.sectionContentGap),list.top-dropdown.bottom)
        val last=list.getChildAt(list.childCount-1)
        assertTrue(last.bottom<=list.height)
        val training=view.findViewWithTag<View>("training_help_controls")
        val lastBounds=android.graphics.Rect(0,0,last.width,last.height)
        val trainingBounds=android.graphics.Rect(0,0,training.width,training.height)
        scroll.offsetDescendantRectToMyCoords(last,lastBounds)
        scroll.offsetDescendantRectToMyCoords(training,trainingBounds)
        assertTrue(lastBounds.bottom<=trainingBounds.top)
        val header=testDescendants(view).filterIsInstance<BicycleSectionHeader>().single()
        val bounds=android.graphics.Rect(0,0,header.width,header.height)
        scroll.offsetDescendantRectToMyCoords(header,bounds)
        assertEquals(BicycleDesign.dp(view.context,BicycleDesign.sectionCardInset),bounds.left)
        assertEquals(720-BicycleDesign.dp(view.context,BicycleDesign.sectionCardInset),bounds.right)
        scroll.isSmoothScrollingEnabled=false
        scroll.fullScroll(View.FOCUS_DOWN);shadowOf(Looper.getMainLooper()).idle()
        assertTrue(scroll.scrollY>0)
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
        val enabled=view.findViewWithTag<android.widget.Switch>("field_data_enabled")
        enabled.performClick();assertNull(selected.last());assertFalse(enabled.isChecked)
        enabled.performClick();assertEquals(DashboardField("WEATHER_DAYLIGHT"),selected.last())
        enabled.performClick();assertNull(selected.last())
        testDescendants(list).single { it.tag=="WEATHER_DAYLIGHT" }.performClick()
        assertTrue(enabled.isChecked);assertEquals(DashboardField("WEATHER_DAYLIGHT"),selected.last())
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
        val toggle=picker.findViewWithTag<View>("graph_overlay_toggle")
        val enabled=picker.findViewWithTag<android.widget.Switch>("graph_overlay_enabled")
        assertFalse(enabled.isChecked)
        toggle.performClick()
        picker.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1800,View.MeasureSpec.AT_MOST))
        picker.layout(0,0,picker.measuredWidth,picker.measuredHeight)
        val choices=testDescendants(picker).filterIsInstance<android.widget.CheckBox>()
        val content=picker.findViewWithTag<View>("graph_overlay_content")
        assertTrue(content.top+choices.first().top-toggle.bottom>=BicycleDesign.dp(context,BicycleDesign.controlGap))
        assertEquals(AnalysisChart.entries.size,choices.size)
        AnalysisChart.entries.forEachIndexed { index,chart ->
            val box=choices[index];val text=box.text as android.text.Spanned
            assertTrue(box.contentDescription.isNotEmpty());val dot=text.getSpans(0,text.length,RecordingDotSpan::class.java).single()
            assertEquals(kotlin.math.ceil(BicycleDesign.graphChoiceDotDiameter*context.resources.displayMetrics.density).toInt(),
                dot.getSize(android.graphics.Paint(),text,0,1,null))
            val b=android.graphics.Bitmap.createBitmap(40,40,android.graphics.Bitmap.Config.ARGB_8888)
            dot.draw(android.graphics.Canvas(b),text,0,1,0f,0,20,30,android.graphics.Paint().apply { textSize=18f })
            assertTrue((0 until 30).any { y -> (0 until 30).any { x -> b.getPixel(x,y)==chart.color(android.graphics.Color.BLACK) } })
        }
        choices[0].performClick();choices[1].performClick()
        assertEquals(listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE),picker.selected)
        assertTrue(enabled.isChecked)
        assertFalse(choices[2].isEnabled);choices[0].performClick();assertTrue(choices[2].isEnabled)
        enabled.performClick();assertTrue(picker.selected.isEmpty());assertFalse(enabled.isChecked)
        enabled.performClick();assertEquals(listOf(AnalysisChart.CADENCE),picker.selected)
        enabled.performClick();choices[2].performClick();assertTrue(enabled.isChecked)
        assertEquals(listOf(AnalysisChart.POWER),picker.selected)
    }

}
