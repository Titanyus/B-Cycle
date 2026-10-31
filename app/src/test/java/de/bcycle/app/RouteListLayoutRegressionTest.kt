package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.ListView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteListLayoutRegressionTest {
    private fun entries()=(0 until 80).map { i -> ManagedRoute("r$i",
        "Eine sehr lange Nachmittagsrunde über Ichenhausen und Roggenburg mit dem Fahrrad Nummer $i",true,
        if(i%2==0) "FIT" else "GPX",RouteDetails(1793400000000L-i*86400000L,(i+1)*1000.0,movingMs=(i+1)*600000L),true) }
    private fun layout(view:View,w:Int,h:Int=800) {
        view.forceLayout();view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h)
    }
    @Test fun longRideNamesRemainEllipsizedBeforeEyeAfterRealScrollAndRecycling() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val manager=RouteManagerView(c,{},themes).apply { fullscreen=true }
            c.setContentView(manager);manager.submit(entries(),null,false);manager.applyPageSpacing();layout(manager,360)
            val list=manager.findViewWithTag<ListView>("route_list")
            org.robolectric.util.ReflectionHelpers.setField(org.robolectric.util.ReflectionHelpers.getField<Any>(list,"mAttachInfo"),"mInTouchMode",true)
            for(width in listOf(320,360,420)) {
                for(position in listOf(1,45,1,65,1)) {
                    list.setSelectionFromTop(position,0);layout(manager,width)
                    for(i in 0 until list.childCount) {
                        val row=list.getChildAt(i);val entry=list.adapter.getItem(list.firstVisiblePosition+i) as? ManagedRoute ?: continue
                        val title=row.findViewWithTag<TextView>("route_name_${entry.id}")
                        val eye=row.findViewWithTag<View>("route_eye_${entry.id}")
                        assertEquals(1,title.lineCount)
                        assertTrue("${entry.id} at $width",title.layout.getEllipsisCount(0)>0)
                        assertTrue(title.right<=eye.left);assertEquals(0,title.scrollX)
                        assertEquals(entry.name,title.contentDescription)
                    }
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun initialAndFastScrollHeadersHaveNoExtraTopGapAndSelectionFillsWholeRideRow() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val manager=RouteManagerView(c,{},themes).apply { fullscreen=true }
            c.setContentView(manager);manager.submit(entries(),null,false);manager.applyPageSpacing();layout(manager,360)
            val list=manager.findViewWithTag<ListView>("route_list")
            org.robolectric.util.ReflectionHelpers.setField(org.robolectric.util.ReflectionHelpers.getField<Any>(list,"mAttachInfo"),"mInTouchMode",true)
            assertEquals(0,list.paddingTop)
            RouteSort.entries.forEach { sort ->
                manager.query.sort=sort;manager.refreshQuery();layout(manager,360)
                val indexer=list.adapter as android.widget.SectionIndexer
                for(section in (indexer.sections.indices).take(3)) {
                    list.setSelectionFromTop(indexer.getPositionForSection(section),0);layout(manager,360)
                    val header=list.getChildAt(0)
                    assertTrue(header is RouteListGroupHeader);assertEquals(0,header.top)
                    val image=Bitmap.createBitmap(header.width,header.height,Bitmap.Config.ARGB_8888)
                    header.draw(Canvas(image));assertEquals(themes.active.tokens.tertiary,image.getPixel(1,1));image.recycle()
                }
            }
            val position=(0 until list.adapter.count).first { list.adapter.getItem(it) is ManagedRoute }
            var row=list.adapter.getView(position,null,list);layout(row,344,180)
            row.performLongClick();row=list.adapter.getView(position,row,list);layout(row,344,180)
            val image=Bitmap.createBitmap(row.width,row.height,Bitmap.Config.ARGB_8888)
            row.draw(Canvas(image))
            assertEquals(themes.active.tokens.tertiary,image.getPixel(1,1))
            assertEquals(themes.active.tokens.tertiary,image.getPixel(row.width-1,row.height-1));image.recycle()
        } finally { host.pause().stop().destroy() }
    }
}
