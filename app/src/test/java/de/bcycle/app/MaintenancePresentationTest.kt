package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="de-rDE-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MaintenancePresentationTest {
    private fun tasks()=DreeveMaintenanceTask.decode(JSONObject(javaClass.getResource("/dreeve-maintenance.json")!!.readText()))
    @Test fun groupingUsesStableComponentAndBikeIdsAndRetainsSharedAndUnassignedParts() {
        val tasks=tasks();val grouped=MaintenanceGrouping.bikes(tasks)
        assertEquals(listOf("gear-bike-a","gear-bike-b",null),grouped.keys.toList())
        assertEquals(5,grouped.getValue("gear-bike-a").size);assertEquals(1,grouped.getValue("gear-bike-b").size)
        val components=MaintenanceGrouping.components(grouped.getValue("gear-bike-a"))
        assertEquals(3,components.size);assertEquals(3,components.getValue("id:gearComponent-chain").size)
        val original=tasks.first()
        assertEquals(2,MaintenanceGrouping.components(listOf(original,original.copy(id="another",componentId="different-id"))).size)
        assertEquals(2,MaintenanceGrouping.components(listOf(original.copy(componentId=null),original.copy(id="another",componentId=null))).size)
    }
    @Test fun overdueBarMarksActualTargetAndUsesTwoRedsDerivedFromTheSharedZonePalette() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val original=tasks().first().copy(interval=2000.0,elapsed=3806.0,remaining=-1806.0)
            val bar=MaintenanceProgress(c,themes,original)
            bar.layout(0,0,320,18)
            val bitmap=Bitmap.createBitmap(320,18,Bitmap.Config.ARGB_8888);bar.draw(Canvas(bitmap))
            assertEquals(2000.0/3806,bar.targetFraction,.00001)
            assertEquals(MaintenancePalette.normalLimit(themes.active.tokens.foreground),bitmap.getPixel(40,9))
            assertEquals(MaintenancePalette.exceeded(),bitmap.getPixel(280,9))
            assertEquals(TrainingZoneColors.values[1],MaintenancePalette.green)
            assertEquals(TrainingZoneColors.values[4],MaintenancePalette.red)
            assertEquals(MaintenancePalette.green,ZoneGlowColors.powerColors[2])
            assertEquals(MaintenancePalette.red,ZoneGlowColors.powerColors[5])
            val green=MaintenanceProgress(c,themes,original.copy(elapsed=1000.0,remaining=1000.0,overdue=false,due=false))
            green.layout(0,0,320,18);bitmap.eraseColor(Color.TRANSPARENT);green.draw(Canvas(bitmap))
            assertEquals(MaintenancePalette.green,bitmap.getPixel(40,9));assertNotEquals(MaintenancePalette.green,bitmap.getPixel(280,9))
            bitmap.recycle()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun realBikeSectionsPinAndPushAndComponentsStayOrdinaryAccordionsInBothDesigns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            for(flat in listOf(true,false)) {
                themes.setFlatDesign(flat)
                val view=DreeveMaintenanceView(c,themes) {}
                val scroll=BicycleStickyScrollView(c).apply { addView(view) };c.setContentView(scroll)
                val base=tasks().first()
                val data=(0..9).map { base.copy(id="task-$it",componentId="part-$it",component="Bauteil $it",gearIds=listOf("a")) }+
                    (10..13).map { base.copy(id="task-$it",componentId="part-$it",gearIds=listOf("b")) }
                view.loaded(data,mapOf("a" to "E-Bike","b" to "Indoor"))
                BicycleContentSpacing.page(scroll)
                scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY))
                scroll.layout(0,0,360,640)
                val headers=testDescendants(view).filterIsInstance<BicycleSectionHeader>()
                assertEquals(2,headers.size)
                val action=view.findViewWithTag<BicycleSpecialButton>("maintenance_action_task-0")
                assertFalse(action.filled);assertFalse(action.isEnabled)
                val text=view.findViewWithTag<View>("maintenance_text_task-0")
                assertEquals(text.top+text.height/2f,action.top+action.height/2f,1f)
                assertTrue(action.left>=text.right)
                assertEquals(2,(text as android.view.ViewGroup).childCount)
                assertEquals(14,testDescendants(view).filterIsInstance<BicycleFrameDropdown>().size)
                assertSame(view.findViewWithTag<View>("maintenance_tasks"),view.getChildAt(0))
                assertSame(view,view.findViewWithTag<View>("maintenance_refresh").parent.parent)
                fun top(v:View):Int { var y=v.top;var p=v.parent;while(p is View && p!==scroll) { y+=p.top;p=p.parent };return y }
                assertEquals(if(flat) 0 else BicycleDesign.dp(c,BicycleDesign.controlGap),top(headers.first()))
                scroll.scrollTo(0,top(headers.first())+100);assertTrue(headers.first().translationY>0)
                assertEquals(0f,top(headers.first())+headers.first().translationY-scroll.scrollY,0f)
                scroll.scrollTo(0,top(headers.last())-10)
                assertTrue(headers.first().y+headers.first().height<=headers.last().top+1)
                scroll.scrollTo(0,0)
                val image=Bitmap.createBitmap(360,640,Bitmap.Config.ARGB_8888);image.eraseColor(themes.active.tokens.secondary);scroll.draw(Canvas(image))
                java.io.File("build/reports/ui/maintenance-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
        } finally { host.pause().stop().destroy() }
    }
}
