package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteGroupingUiTest {
    private fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    private fun layout(v:View,width:Int,height:Int=0) {
        v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,if(height==0) View.MeasureSpec.UNSPECIFIED else View.MeasureSpec.EXACTLY));v.layout(0,0,width,v.measuredHeight)
    }
    private fun route(id:String,date:String?,km:Double,hours:Double)=ManagedRoute(id,id,true,"FIT",RouteDetails(
        date?.let { LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() } ?: 0,
        distanceM=km*1000,movingMs=(hours*3600000).toLong()),false)
    @Test fun sectionHeadersUseSharedTypographyAndValuesRecycleAndRespectFilters() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        val locale=Locale.getDefault();Locale.setDefault(Locale.GERMANY)
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));var opened=0
            val manager=RouteManagerView(c,{},themes,{opened++}).apply { fullscreen=true }
            val entries=listOf(route("a","2026-10-31",20.0,1.0),route("b","2026-10-30",100.0,4.0),route("c","2026-09-30",10.0,.5),route("d",null,30.0,1.0))
            host.get().setContentView(manager);manager.submit(entries,null,false);layout(manager,360,800)
            val list=manager.findViewWithTag<ListView>("route_list")
            assertEquals(7,list.adapter.count)
            var card=list.adapter.getView(1,null,list);layout(card,344)
            var header=list.adapter.getView(0,null,list);layout(header,344)
            val title=header.findViewWithTag<TextView>("route_group_title")
            assertEquals("Oktober 2026",title.text.toString());assertEquals(BicycleDesign.narrowTypeface(c),title.typeface)
            assertTrue(title.isAccessibilityHeading)
            assertEquals("2",header.findViewWithTag<TextView>("route_group_count_0").text.toString())
            assertEquals("120,0",header.findViewWithTag<TextView>("route_group_distance_0").text.toString())
            assertEquals("5:00",header.findViewWithTag<TextView>("route_group_moving_0").text.toString())
            assertEquals("24,0",header.findViewWithTag<TextView>("route_group_average_0").text.toString())
            for(reading in nodes(header).filterIsInstance<RouteListReading>()) {
                assertEquals(BicycleDesign.narrowTypeface(c),reading.number.typeface)
                if(reading.unit.visibility==View.VISIBLE) {
                    assertTrue(reading.unit.textSize<reading.number.textSize)
                    assertTrue(reading.unit.left>=reading.number.right)
                    assertEquals(reading.number.top+reading.number.height/2f,reading.unit.top+reading.unit.height/2f,1f)
                }
            }
            for(width in listOf(288,344,420)) {
                layout(header,width)
                for(view in nodes(header)) { assertTrue(view.width>=0);if(view is RouteListReading) assertTrue(view.number.right<=view.width) }
            }
            for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                MotionEvent.obtain(0,10,action,100f,header.top+8f,0).also { header.dispatchTouchEvent(it);it.recycle() }
            }
            assertEquals(0,opened);card.performClick();assertEquals(1,opened)
            assertFalse(list.adapter.isEnabled(0));assertTrue(list.adapter.isEnabled(1))
            assertFalse(header.isLongClickable);assertFalse(manager.selecting)
            assertNull(card.findViewWithTag<View>("route_group_title"))
            card=list.adapter.getView(2,card,list)
            assertNull(card.findViewWithTag<View>("route_group_title"))
            header=list.adapter.getView(3,header,list);layout(header,344)
            assertEquals("September 2026",header.findViewWithTag<TextView>("route_group_title").text.toString())
            manager.query.fromDate=LocalDate.of(2026,1,1);manager.query.distance=RouteRange(from=25.0);manager.refreshQuery()
            assertEquals(2,list.adapter.count)
            card=list.adapter.getView(0,header,list)
            assertEquals("1",card.findViewWithTag<TextView>("route_group_count_0").text.toString())
            assertEquals("100,0",card.findViewWithTag<TextView>("route_group_distance_0").text.toString())
            assertEquals("25,0",card.findViewWithTag<TextView>("route_group_average_0").text.toString())
        } finally { Locale.setDefault(locale);host.pause().stop().destroy() }
    }
    @Test fun selectionLeavesMonthHeaderUnpaintedAndFooterVisibilityFollowsExplicitAndTemporaryChoices() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val manager=RouteManagerView(c,{},themes).apply { fullscreen=true }
            manager.submit(listOf(route("a","2026-10-31",20.0,1.0),route("b","2026-09-30",10.0,.5)),null,false)
            val footer=bicycleFooter(c,themes)
            manager.bindFooter(footer)
            val root=android.widget.LinearLayout(c).apply {
                orientation=android.widget.LinearLayout.VERTICAL
                addView(manager,android.widget.LinearLayout.LayoutParams(-1,0,1f))
                addView(footer,android.widget.LinearLayout.LayoutParams(-1,48))
            }
            val screen=BicycleDialogBuilder(c,themes,DialogNavigation()).setView(root).create()
            try {
                screen.show()
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val decor=screen.window!!.decorView
                layout(decor,360,800)
                val toggle=decor.findViewWithTag<View>("route_footer_toggle")
                val help=decor.findViewWithTag<View>("settings_help")
                assertNotNull("Footer toggle missing: "+nodes(decor).map { it.tag },toggle);assertNotNull("Info missing",help)
                assertTrue(toggle.right<=help.left)
                assertEquals(View.GONE,footer.visibility)
                assertEquals(c.getString(R.string.route_footer_show),toggle.contentDescription)
                val list=manager.findViewWithTag<ListView>("route_list")
                var row=list.adapter.getView(3,null,list);layout(row,344)
                val header=list.adapter.getView(2,null,list);layout(header,344)
                val before=Bitmap.createBitmap(header.width,header.height,Bitmap.Config.ARGB_8888)
                header.draw(Canvas(before))
                row.performLongClick();row=list.adapter.getView(3,row,list);layout(row,344)
                assertEquals(setOf("b"),manager.checkedIds)
                assertTrue(row.isActivated);assertNotNull(row.background)
                assertNull(row.findViewWithTag<View>("route_selection_body").background)
                val after=Bitmap.createBitmap(header.width,header.height,Bitmap.Config.ARGB_8888)
                header.draw(Canvas(after));assertTrue(before.sameAs(after));before.recycle();after.recycle()
                assertEquals(View.VISIBLE,footer.visibility)
                manager.clearSelection();assertEquals(View.GONE,footer.visibility)
                toggle.performClick();assertEquals(View.VISIBLE,footer.visibility)
                row.performLongClick();manager.clearSelection();assertEquals(View.VISIBLE,footer.visibility)
                toggle.performClick();assertEquals(View.GONE,footer.visibility)
                row.performLongClick();assertEquals(View.VISIBLE,footer.visibility)
                toggle.performClick();assertEquals(View.GONE,footer.visibility)
                manager.clearSelection();assertEquals(View.GONE,footer.visibility)
                // Recycling a selected first-of-month row must clear the next row's selection paint.
                row=list.adapter.getView(1,row,list)
                assertNull(row.findViewWithTag<View>("route_selection_body").background)
            } finally { screen.dismiss() }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun numericTitlesAndMonthPreviewRenderInBothDesignModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));val manager=RouteManagerView(c,{},themes).apply { fullscreen=true }
            c.setContentView(manager);manager.submit(listOf(route("a","2026-10-31",20.0,1.75)),null,false)
            val list=manager.findViewWithTag<ListView>("route_list")
            for((sort,title) in listOf(RouteSort.DISTANCE to "Distanz bis 25 km",RouteSort.MOVING to "Fahrzeit bis 2 h",RouteSort.SPEED to "Ø-Tempo bis 20 km/h")) {
                manager.query.sort=sort;manager.refreshQuery()
                for(flat in listOf(false,true)) {
                    themes.setFlatDesign(flat);layout(manager,360,800)
                    assertEquals(title,(list.adapter as android.widget.SectionIndexer).sections.single())
                    assertEquals(title,list.getChildAt(0).findViewWithTag<TextView>("route_group_title").text.toString())
                    val image=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888)
                    try {
                        manager.draw(Canvas(image))
                        if(sort==RouteSort.DISTANCE) java.io.File("build/reports/ui/route-groups-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
                    } finally { image.recycle() }
                }
            }
        } finally { host.pause().stop().destroy() }
    }
}
