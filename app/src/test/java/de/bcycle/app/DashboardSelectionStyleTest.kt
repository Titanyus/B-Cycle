package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardSelectionStyleTest {
    @Test fun dashboardCardsMatchBikeCardChromeAndLayoutsExpandInlineWithoutChangingSelection() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            val bikes=BikeProfileStore(a,prefs)
            val strip=BikeProfileStrip(a,themes,{}, {}, { _,_->false })
            strip.show(bikes.all(),bikes.activeSlot,false)
            val picker=DashboardPresetPicker(a,0,themes)
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL;addView(strip);addView(picker) }
            a.setContentView(root)
            fun layout() {
                shadowOf(Looper.getMainLooper()).idle()
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1400,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,1400)
            }
            fun pixels(view:View,foreground:Boolean):IntArray {
                val drawable=(if(foreground) view.foreground else view.background)!!
                val image=Bitmap.createBitmap(160,154,Bitmap.Config.ARGB_8888)
                val bounds=android.graphics.Rect(drawable.bounds)
                drawable.setBounds(0,0,160,154);drawable.draw(Canvas(image));drawable.bounds=bounds
                return IntArray(160*154).also { image.getPixels(it,0,160,0,0,160,154);image.recycle() }
            }
            for(flat in listOf(false,true,false)) {
                themes.setFlatDesign(flat);layout()
                val details=picker.findViewWithTag<LinearLayout>("dashboard_layout_details")
                assertEquals(View.GONE,details.visibility)
                assertEquals(0,details.childCount)
                val before=picker.selection.toList()
                picker.findViewWithTag<View>("dashboard_layout_choice").performClick();layout()
                assertEquals(View.VISIBLE,details.visibility);assertEquals(before,picker.selection)
                assertEquals(1,details.childCount);assertTrue(details.getChildAt(0) is DashboardLayoutGrid)
                assertTrue(testDescendants(details).none { it is android.widget.ScrollView })
                val bike=strip.findViewWithTag<View>("bike_profile_${bikes.activeSlot}")
                val page=picker.findViewWithTag<View>("preset_page_0")
                val template=picker.findViewWithTag<View>("preset_${picker.selection[0].name}")
                for(card in listOf(page,template)) {
                    assertArrayEquals(pixels(bike,false),pixels(card,false))
                    assertArrayEquals(pixels(bike,true),pixels(card,true))
                }
                selectTestDashboardLayout(picker,DashboardTemplate.HERO_SEVEN);layout()
                assertEquals(DashboardTemplate.HERO_SEVEN,picker.selection[0])
                assertEquals(View.VISIBLE,details.visibility)
                val image=Bitmap.createBitmap(360,root.height,Bitmap.Config.ARGB_8888)
                Canvas(image).apply { drawColor(themes.active.tokens.secondary);root.draw(this) }
                java.io.File("build/reports/ui/dashboard-accordion-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }
                    .outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
                picker.findViewWithTag<View>("dashboard_layout_choice").performClick();layout()
                assertEquals(View.GONE,details.visibility);assertEquals(0,details.childCount)
            }
        } finally { host.pause().stop().destroy() }
    }
}
