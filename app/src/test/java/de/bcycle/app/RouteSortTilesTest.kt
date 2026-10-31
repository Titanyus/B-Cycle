package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.LinearLayout
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
class RouteSortTilesTest {
    @Test fun fiveIconTilesFitOneRowAndApplySortWithoutChangingFiltersInBothDesigns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));val query=RouteQuery().apply { distance=RouteRange(10.0,50.0) }
            var changed=0
            val popup=RouteQueryPopup(c,themes,DialogNavigation(),query) { changed++ }.apply { sorting() }
            val root=popup.contentView
            c.setContentView(root)
            fun layout(width:Int) {
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.AT_MOST))
                root.layout(0,0,width,root.measuredHeight)
            }
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                for(width in listOf(320,360,480)) {
                    layout(width)
                    val row=root.findViewWithTag<LinearLayout>("route_sort_choices")
                    assertEquals(5,row.childCount)
                    val buttons=(0 until row.childCount).map { row.getChildAt(it) as BicycleSegmentButton }
                    assertEquals(1,buttons.map { it.top }.distinct().size)
                    assertEquals(1,buttons.map { it.height }.distinct().size)
                    assertTrue(buttons.last().right<=row.width)
                    RouteSort.entries.forEach { sort ->
                        val button=root.findViewWithTag<BicycleSegmentButton>("route_sort_${sort.name}")
                        button.performClick();layout(width)
                        assertEquals(sort,query.sort);assertTrue(button.isChecked)
                        assertEquals(1,buttons.take(4).count { it.isChecked })
                    }
                    val direction=buttons.last();val old=query.descending
                    repeat(2) {
                        direction.performClick();layout(width)
                        buttons.forEach { button ->
                            assertNotNull(button.compoundDrawables[1]);assertEquals(1,button.lineCount)
                            assertTrue("${button.text} at $width",button.layout.getLineWidth(0)<=button.width-button.compoundPaddingLeft-button.compoundPaddingRight+1)
                        }
                    }
                    assertEquals(old,query.descending);assertEquals(RouteRange(10.0,50.0),query.distance)
                    if(width==360) {
                        val image=Bitmap.createBitmap(width,root.height,Bitmap.Config.ARGB_8888);root.draw(Canvas(image))
                        java.io.File("build/reports/ui/route-sort-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
                        image.recycle()
                    }
                }
            }
            assertEquals(36,changed)
        } finally { host.pause().stop().destroy() }
    }
}
