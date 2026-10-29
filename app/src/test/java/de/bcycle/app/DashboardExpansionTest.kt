package de.bcycle.app

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardExpansionTest {
    private fun layout(view:View) { repeat(3) { view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(780,View.MeasureSpec.EXACTLY));view.layout(0,0,360,780) } }
    @Test fun expandedDashboardChoicesHaveOneGlobalGapBelowTheAccordionInBothDesigns() {
        val host=org.robolectric.Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                val picker=DashboardPresetPicker(c,0,themes);c.setContentView(picker)
                val toggle=picker.findViewWithTag<View>("dashboard_layout_choice")
                toggle.performClick();layout(picker)
                val body=picker.findViewWithTag<ViewGroup>("dashboard_layout_details")
                val grid=body.getChildAt(0) as ViewGroup
                val heading=grid.getChildAt(0)
                val a=IntArray(2);val b=IntArray(2);heading.getLocationOnScreen(a);toggle.getLocationOnScreen(b)
                assertEquals(BicycleDesign.dp(c,BicycleDesign.sectionContentGap),a[1]-b[1]-toggle.height)
                toggle.performClick();toggle.performClick();layout(picker)
                assertEquals(BicycleDesign.dp(c,BicycleDesign.sectionContentGap),body.paddingTop)
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun pickerUsesRequestedVisualOrderWithoutChangingEnumIdsOrDefaults() {
        val c=org.robolectric.RuntimeEnvironment.getApplication()
        val themes=ThemeManager(c.getSharedPreferences("dashboard-order",0))
        val grid=DashboardLayoutGrid(c,themes,DashboardTemplate.DATA) {}
        val expected=listOf(DashboardTemplate.FULL_FOUR,DashboardTemplate.FULL_DATA,DashboardTemplate.FULL_SIX,DashboardTemplate.FULL_NINE,DashboardTemplate.FULL_EIGHT,
            DashboardTemplate.MAP,DashboardTemplate.OVERVIEW,DashboardTemplate.BALANCED,DashboardTemplate.FIVE,DashboardTemplate.EIGHT,
            DashboardTemplate.HERO,DashboardTemplate.HERO_FIVE,DashboardTemplate.HERO_SEVEN,DashboardTemplate.DATA)
        fun cards(view:View):List<String> {
            val own=listOfNotNull(view.tag?.toString()?.takeIf { it.startsWith("preset_") && view.isClickable })
            return own+if(view is android.view.ViewGroup) (0 until view.childCount).flatMap { cards(view.getChildAt(it)) } else emptyList()
        }
        assertEquals(expected.map { "preset_${it.name}" },cards(grid))
        assertEquals(DashboardTemplate.entries.toSet(),expected.toSet())
        assertEquals(listOf(DashboardTemplate.DATA,DashboardTemplate.BALANCED,DashboardTemplate.MAP,DashboardTemplate.OVERVIEW),DashboardTemplate.entries.take(4))
        assertEquals(DashboardTemplate.DATA,DashboardTemplate.decode(null))
        val indoor=DashboardLayoutGrid(c,themes,DashboardTemplate.FULL_DATA,DashboardTemplate.entries.filter { !it.hasMap }) {}
        assertEquals(expected.filter { !it.hasMap }.map { "preset_${it.name}" },cards(indoor))
    }
    @Test fun newNoMapLayoutsKeepNineMapPresetsAndMigrateOldEightFields() {
        assertEquals(9,DashboardTemplate.entries.count { it.hasMap })
        assertEquals(5,DashboardTemplate.entries.count { !it.hasMap })
        assertEquals(listOf(1,1,2,2,2),DashboardTemplate.FULL_EIGHT.rows.map { it.size })
        assertEquals(listOf(1,1,1,1),DashboardTemplate.FULL_FOUR.rows.map { it.size })
        assertEquals(listOf(1,1,1,3),DashboardTemplate.FULL_SIX.rows.map { it.size })
        assertEquals(listOf(1,1,1,3,3),DashboardTemplate.FULL_NINE.rows.map { it.size })
        assertEquals(8,DashboardTemplate.EIGHT.slots.size)
        val old=List(8) { "HEART_RATE" }.joinToString("|")
        val restored=DashboardLayout.decode(old)
        assertEquals(9,restored.fields.size);assertTrue(restored.fields.take(8).all { it.id=="HEART_RATE" })
        val c=org.robolectric.RuntimeEnvironment.getApplication()
        val grid=DashboardLayoutGrid(c,ThemeManager(c.getSharedPreferences("grid",0)),DashboardTemplate.FULL_NINE) {}
        val headings=(0 until grid.childCount).map(grid::getChildAt).filterIsInstance<TextView>()
        assertEquals(listOf("Dashboards ohne Karte","Dashboards mit Karte"),headings.map { it.text.toString() })
        DashboardTemplate.entries.forEach { assertNotNull(grid.findViewWithTag<View>("preset_${it.name}")) }
    }
    @Test fun visibleTwoAndThreeColumnValuesShareSizeAndHiddenLongValuesDoNotLimitThem() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val view=DashboardView(c) {};c.setContentView(view)
            view.setTemplate(DashboardTemplate.FIVE)
            val fields=List(9) { if(it in listOf(0,3,7)) "GPS_SPEED" else "SPEED" }.joinToString("|")
            view.show(DashboardLayout.decode(fields),LiveData(speedHundredths=1000),GpsValues(speedKmh=99999.9))
            layout(view)
            fun number(index:Int):TextView {
                val tile=view.findViewWithTag<ViewGroup>("dashboard_tile_$index")
                return (tile.getChildAt(1) as ViewGroup).getChildAt(0) as TextView
            }
            assertEquals(number(0).textSize,number(1).textSize,.01f)
            assertEquals(number(2).textSize,number(3).textSize,.01f);assertEquals(number(2).textSize,number(4).textSize,.01f)
            assertTrue(number(2).textSize<=45*c.resources.displayMetrics.scaledDensity)
            val two=number(1).textSize;val three=number(2).textSize
            view.showDataVisibility(DashboardDataVisibility(List(9) { it !in listOf(0,3) }));layout(view)
            assertTrue("two: before=$two after=${number(1).textSize} text=${number(1).text}, width=${number(1).width}, height=${number(1).height}, hidden=${number(0).parent.let { it as View }.visibility}",number(1).textSize>two);assertTrue("three: before=$three after=${number(2).textSize}",number(2).textSize>three)
            assertEquals(number(2).textSize,number(4).textSize,.01f)
            assertTrue(number(2).textSize<=45*c.resources.displayMetrics.scaledDensity)
            for(template in listOf(DashboardTemplate.FULL_EIGHT,DashboardTemplate.FULL_FOUR,DashboardTemplate.FULL_NINE,DashboardTemplate.FULL_SIX)) {
                view.setTemplate(template);view.show(DashboardLayout.decode(fields),LiveData(),GpsValues(elapsedMs=360000000));layout(view)
                template.slots.forEach { assertTrue(view.findViewWithTag<View>("dashboard_tile_$it").height>0) }
                view.showDataVisibility(DashboardDataVisibility())
                view.show(DashboardLayout.defaults,LiveData(speedHundredths=2530,riderPowerWatts=183,
                    otherValues=mapOf(LiveMetric.CADENCE to 170L,LiveMetric.MOTOR_POWER to 250L,LiveMetric.BATTERY to 72L,LiveMetric.ODOMETER to 14791937L,LiveMetric.AVERAGE_SPEED to 2100L)))
                layout(view)
                val image=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(image))
                java.io.File("build/reports/ui/dashboard-${template.name.lowercase()}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun mapDashboardSwitchPausesMapLayersWithoutChangingGpsSetting() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a)
            val panel=MainActivity::class.java.getDeclaredField("mapPanel").apply { isAccessible=true }.get(a) as MapPanel
            val running=MapPanel::class.java.getDeclaredField("running").apply { isAccessible=true }
            val active=MapNightLayer::class.java.getDeclaredField("active").apply { isAccessible=true }
            val downloads=MapPanel::class.java.getDeclaredField("downloads").apply { isAccessible=true }.get(panel)
            val gps=runtime.gpsEnabled
            val set=MainActivity::class.java.getDeclaredMethod("setMapFullscreen",Boolean::class.javaPrimitiveType).apply { isAccessible=true }
            for(template in listOf(DashboardTemplate.FULL_DATA,DashboardTemplate.MAP,DashboardTemplate.FULL_NINE)) {
                runtime.preferences.edit().putString("dashboardTemplate_0",template.name).commit();set.invoke(a,false)
                assertEquals(template.hasMap,running.getBoolean(panel));assertEquals(template.hasMap,active.getBoolean(downloads))
                assertEquals(gps,runtime.gpsEnabled)
            }
        } finally { host.pause().stop().destroy() }
    }
}
