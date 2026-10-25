package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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
class BicycleSectionCardsTest {
    @Test fun glowCardsJoinHeaderAndContentWithoutOutlineAndKeepFlatAndPinning() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val root=LinearLayout(a).apply {
                orientation=LinearLayout.VERTICAL;setPadding(16,0,16,0)
                BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
            }
            val first=BicycleForm.section(root,themes,R.string.rider_training)
            val content=LinearLayout(a).apply {
                orientation=LinearLayout.VERTICAL
                BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
                addView(TextView(a).apply { text="Trainingsparameter";setTextColor(Color.WHITE) },LinearLayout.LayoutParams(-1,80))
                addView(TextView(a).apply { text="Ruhepuls                 65 bpm";setTextColor(Color.WHITE) },LinearLayout.LayoutParams(-1,80))
            }
            root.addView(content,LinearLayout.LayoutParams(-1,220))
            first.accordion(content)
            val second=BicycleForm.section(root,themes,R.string.rider_personal)
            val other=View(a);root.addView(other,LinearLayout.LayoutParams(-1,500));second.accordion(other,false)
            root.addView(View(a),LinearLayout.LayoutParams(-1,700))
            val scroll=BicycleStickyScrollView(a).apply { addView(root) };a.setContentView(scroll)
            fun layout() {
                shadowOf(Looper.getMainLooper()).idle()
                scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(520,View.MeasureSpec.EXACTLY))
                scroll.layout(0,0,360,520)
            }
            fun image():Bitmap {
                layout();return Bitmap.createBitmap(360,520,Bitmap.Config.ARGB_8888).also { scroll.draw(Canvas(it)) }
            }
            val originalParent=content.parent
            for(flat in listOf(false,true,false)) {
                themes.setFlatDesign(flat);layout()
                val bitmap=image();val tokens=themes.active.tokens
                if(!flat) {
                    assertEquals(tokens.tertiary,bitmap.getPixel(180,first.bottom+4))
                    assertEquals(tokens.secondary,bitmap.getPixel(1,first.top))
                    assertEquals(tokens.tertiary,bitmap.getPixel(180,content.bottom-1))
                    assertEquals(tokens.secondary,bitmap.getPixel(1,content.bottom-1))
                    for(index in listOf(0,2)) assertEquals(0f,first.getChildAt(index).alpha,0f)
                } else {
                    assertEquals(tokens.secondary,bitmap.getPixel(180,first.bottom+4))
                    assertEquals(0f,(first.background as android.graphics.drawable.GradientDrawable).cornerRadii!![0],0f)
                }
                java.io.File("build/reports/ui/sticky-cards-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }
                    .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
                assertSame(originalParent,content.parent)
                first.performClick();layout();assertEquals(View.GONE,content.visibility)
                first.performClick();layout();assertEquals(View.VISIBLE,content.visibility)
                scroll.scrollTo(0,40);layout()
                assertEquals(0f,first.top+first.translationY-scroll.scrollY,0f)
                scroll.scrollTo(0,0)
            }
            themes.activate("preset:black-ops");layout()
            image().also { assertEquals(themes.active.tokens.tertiary,it.getPixel(180,first.bottom+4));it.recycle() }
        } finally { host.pause().stop().destroy() }
    }
}
