package de.bcycle.app

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class BikeQuickPickerTest {
    @Test fun sixManagerCardsFitWithoutScrollingAndScaleIconsTypeAndTwoLineNamesTogether() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("theme",0))
            val profiles=(0..5).map { BikeProfile(it,"Fahrrad mit langem Namen $it",type=if(it%2==0) BikeType.EBIKE else BikeType.INDOOR) }
            var chosen:Int?=null
            val anchor=View(c);host.get().setContentView(anchor)
            val popup=BikeQuickPicker(anchor,themes,profiles,2) { chosen=it }
            val content=popup.contentView
            val strip=content.findViewWithTag<BikeProfileStrip>("bike_quick_strip")
            for(width in listOf(320,360,480)) {
                content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.AT_MOST))
                content.layout(0,0,width,content.measuredHeight)
                val row=strip.getChildAt(0) as ViewGroup
                assertEquals(6,row.childCount);assertTrue(row.width<=strip.width);assertEquals(0,strip.scrollX)
                assertNull(content.findViewWithTag<View>("bike_profile_add"))
                for(i in 0 until row.childCount) {
                    val tile=row.getChildAt(i) as ViewGroup
                    assertFalse(tile.isLongClickable);assertEquals(i==2,tile.isSelected)
                    assertTrue(tile.width>0);assertTrue(tile.height>=BicycleDesign.touchSize)
                    val type=tile.getChildAt(0) as TextView
                    val image=tile.getChildAt(1) as ImageView
                    val name=tile.getChildAt(2) as TextView
                    assertEquals(c.getString(profiles[i].type.label),type.text.toString())
                    assertEquals(profiles[i].name,name.text.toString());assertEquals(2,name.maxLines)
                    assertEquals(BicycleDesign.bodySp*BicycleDesign.bikeQuickScale,type.textSize,.01f)
                    assertEquals(type.textSize,name.textSize,.01f)
                    assertEquals((BicycleDesign.bikePreviewHeight*BicycleDesign.bikeQuickScale).toInt(),image.height)
                    assertTrue(tile.right<=strip.width)
                }
            }
            content.findViewWithTag<View>("bike_profile_5").performClick();assertEquals(5,chosen)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun bottomAnchorOpensPickerAboveButtonInsideVisibleScreen() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get()
            val root=android.widget.FrameLayout(c)
            val anchor=View(c)
            root.addView(anchor,android.widget.FrameLayout.LayoutParams(-1,58,android.view.Gravity.BOTTOM))
            c.setContentView(root)
            root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY))
            root.layout(0,0,360,640)
            val popup=BikeQuickPicker(anchor,ThemeManager(c.getSharedPreferences("theme",0)),listOf(BikeProfile(0,"Bike")),0) {}
            popup.show(anchor)
            try {
                assertTrue(popup.isShowing)
                val params=popup.contentView.rootView.layoutParams as android.view.WindowManager.LayoutParams
                val location=IntArray(2);anchor.getLocationOnScreen(location)
                val frame=android.graphics.Rect().also(anchor::getWindowVisibleDisplayFrame)
                assertTrue(params.y>=frame.top)
                assertTrue(params.y+popup.height<=location[1])
                assertTrue(params.x>=frame.left)
                assertTrue(params.x+popup.width<=frame.right)
            } finally { popup.dismiss() }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun shortListOnlyOffersExistingBikesAndManagerKeepsItsNormalScaleAndAddTile() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("theme",0));val profile=BikeProfile(0,"Bike")
            val popup=BikeQuickPicker(View(c),themes,listOf(profile),0) {}
            val quick=popup.contentView.findViewWithTag<BikeProfileStrip>("bike_quick_strip")
            assertEquals(1,(quick.getChildAt(0) as ViewGroup).childCount)
            val manager=BikeProfileStrip(c,themes,{}, {}, { _,_->false })
            manager.show(listOf(profile),0,false)
            val row=manager.getChildAt(0) as ViewGroup
            assertEquals(2,row.childCount);assertNotNull(manager.findViewWithTag<View>("bike_profile_add"))
            assertEquals(BicycleDesign.bodySp,((row.getChildAt(0) as ViewGroup).getChildAt(0) as TextView).textSize,.01f)
        } finally { host.pause().stop().destroy() }
    }
}
