package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
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
class UnifiedSelectionFooterTest {
    @Test fun positionTilesMatchBikeCardsAndFooterIsTertiaryWithoutFrameInBothModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val store=BikeProfileStore(a,a.getSharedPreferences("bike",0))
            val bikes=BikeProfileStrip(a,themes,{}, {}, { _,_->false }).apply { show(store.all(),store.activeSlot,false) }
            var changed=0;var enabled=true
            val positions=MessagePositionChoices(a,themes,MessagePosition.CENTER,"position",{enabled}) { changed++ }
            val footer=bicycleFooter(a,themes)
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL;addView(bikes);addView(positions);addView(footer) }
            a.setContentView(root)
            fun layout() {
                shadowOf(Looper.getMainLooper()).idle()
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,800)
            }
            fun pixels(view:View,foreground:Boolean):IntArray {
                val d=(if(foreground) view.foreground else view.background)!!;val bounds=Rect(d.bounds)
                val image=Bitmap.createBitmap(120,154,Bitmap.Config.ARGB_8888)
                d.setBounds(0,0,120,154);d.draw(Canvas(image));d.bounds=bounds
                return IntArray(120*154).also { image.getPixels(it,0,120,0,0,120,154);image.recycle() }
            }
            for(flat in listOf(false,true,false)) {
                themes.setFlatDesign(flat);layout()
                val tile=positions.findViewWithTag<View>("position_CENTER")
                val bike=bikes.findViewWithTag<View>("bike_profile_${store.activeSlot}")
                assertEquals(bike.height,tile.height)
                assertArrayEquals(pixels(bike,false),pixels(tile,false));assertArrayEquals(pixels(bike,true),pixels(tile,true))
                assertNull(footer.foreground)
                assertEquals(themes.active.tokens.tertiary,(footer.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
                enabled=false;positions.findViewWithTag<View>("position_TOP").performClick();assertEquals(0,changed);assertTrue(tile.isSelected)
                enabled=true
                val image=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888)
                Canvas(image).apply { drawColor(themes.active.tokens.secondary);root.draw(this) }
                java.io.File("build/reports/ui/position-footer-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }
                    .outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
        } finally { host.pause().stop().destroy() }
    }
}
