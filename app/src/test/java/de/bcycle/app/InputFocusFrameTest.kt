package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
class InputFocusFrameTest {
    @Test fun derivedGradientKeepsAccentStopsAndHasPredictableDarkAndLightExtremes() {
        for(accent in listOf(Color.RED,Color.BLUE,Color.GREEN,Color.WHITE,Color.BLACK,0xffe7aa22.toInt())) {
            val stops=BicycleInputFrame.colors(accent)
            assertEquals(5,stops.size);assertEquals(stops[0],stops[4]);assertEquals(accent,stops[1]);assertEquals(accent,stops[3])
            for(channel in listOf<(Int)->Int>(Color::red,Color::green,Color::blue)) {
                assertTrue(channel(stops[0])<=channel(accent));assertTrue(channel(stops[2])>=channel(accent))
            }
            assertTrue(stops.all { Color.alpha(it)==255 })
        }
    }
    @Test fun focusedBorderAndOuterOnlyGlowFollowScrollThemeAndEnabledStateWithoutChangingEdits() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("focus-frame",0))
            val red=CustomTheme("custom:focus-red","Red",ThemeTokens(0xffaa1616.toInt(),0xff14141c.toInt(),0xff2a2933.toInt(),Color.WHITE,0xffff3030.toInt()))
            val blue=CustomTheme("custom:focus-blue","Blue",red.tokens.copy(primary=0xff193caa.toInt(),accent=0xff26aaff.toInt()))
            assertTrue(themes.saveCustom(red).valid);assertTrue(themes.saveCustom(blue).valid);themes.activate(red.id)
            val body=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL;minimumHeight=800;isFocusableInTouchMode=true;setPadding(24,24,24,24) }
            val normal=BicycleComponents.input(a,themes).apply { setText("183");setSingleLine() }
            val focused=BicycleComponents.input(a,themes).apply { setText("183");setSingleLine() }
            for(edit in listOf(normal,focused)) {
                body.addView(BicycleForm.numberRow(edit,themes,R.string.rider_resting,R.string.analysis_bpm),LinearLayout.LayoutParams(-1,48).apply { topMargin=48 })
            }
            val scroll=ScrollView(a).apply { setBackgroundColor(red.tokens.secondary);addView(body) }
            a.setContentView(scroll)
            fun layout() { repeat(2) {
                scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,400)
            } }
            layout();body.requestFocus()
            var changes=0;focused.addTextChangedListener(object:android.text.TextWatcher {
                override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
                override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { changes++ }
                override fun afterTextChanged(s:android.text.Editable?)=Unit
            })
            val listener=View.OnFocusChangeListener { _,_-> };focused.onFocusChangeListener=listener
            fun location():IntArray {
                val field=IntArray(2);val owner=IntArray(2);focused.getLocationInWindow(field);scroll.getLocationInWindow(owner)
                return intArrayOf(field[0]-owner[0],field[1]-owner[1])
            }
            fun draw()=Bitmap.createBitmap(360,400,Bitmap.Config.ARGB_8888).also { scroll.draw(Canvas(it)) }
            val before=draw();val old=location();val geometry=listOf(focused.width,focused.height,focused.paddingLeft,focused.paddingRight)
            assertTrue(focused.requestFocus());layout()
            val active=draw();val p=location();val x=p[0]-3;val y=p[1]+24
            assertEquals(red.tokens.secondary,before.getPixel(x,y))
            assertNotEquals(red.tokens.secondary,active.getPixel(x,y))
            assertEquals(before.getPixel(p[0]+8,y),active.getPixel(p[0]+8,y)) // Interior receives no halo/fill.
            val background=Bitmap.createBitmap(focused.width,focused.height,Bitmap.Config.ARGB_8888)
            focused.background.draw(Canvas(background))
            assertEquals(0,Color.alpha(background.getPixel(focused.width/2,focused.height/2)))
            assertNotEquals(before.getPixel(p[0]+40,p[1]),active.getPixel(p[0]+40,p[1]))
            java.io.File("build/reports/ui/input-focus-frame.png").apply { parentFile!!.mkdirs() }.outputStream().use { active.compress(Bitmap.CompressFormat.PNG,100,it) }
            scroll.scrollTo(0,40);layout()
            val shifted=draw();val next=location()
            assertEquals(p[1]-40,next[1]);assertNotEquals(red.tokens.secondary,shifted.getPixel(next[0]-3,next[1]+24))
            assertEquals(red.tokens.secondary,shifted.getPixel(x,old[1]+24))
            themes.activate(blue.id);scroll.setBackgroundColor(blue.tokens.secondary);layout()
            val recolored=draw();val color=recolored.getPixel(next[0]-3,next[1]+24)
            assertTrue(Color.blue(color)>Color.red(color))
            focused.isEnabled=false
            val disabled=draw();assertEquals(blue.tokens.secondary,disabled.getPixel(next[0]-3,next[1]+24))
            assertEquals(geometry,listOf(focused.width,focused.height,focused.paddingLeft,focused.paddingRight))
            assertEquals("183",focused.text.toString());assertEquals(0,changes);assertSame(listener,focused.onFocusChangeListener)
            listOf(before,active,background,shifted,recolored,disabled).forEach(Bitmap::recycle)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun glowCannotPaintOverAnOpaquePinnedSectionHeader() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("sticky-focus",0))
            val body=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL;minimumHeight=800 }
            val header=BicycleForm.section(body,themes,R.string.rider_training)
            val edit=BicycleComponents.input(a,themes).apply { setText("183") }
            body.addView(BicycleForm.numberRow(edit,themes,R.string.rider_resting,R.string.analysis_bpm),LinearLayout.LayoutParams(-1,48).apply { topMargin=20 })
            val scroll=BicycleStickyScrollView(a).apply { setBackgroundColor(themes.active.tokens.secondary);addView(body) }
            a.setContentView(scroll)
            fun layout() { repeat(2) {
                scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,400)
            } }
            layout();edit.requestFocus();layout()
            fun position():IntArray {
                val field=IntArray(2);val owner=IntArray(2);edit.getLocationInWindow(field);scroll.getLocationInWindow(owner)
                return intArrayOf(field[0]-owner[0],field[1]-owner[1])
            }
            scroll.scrollTo(0,position()[1]-header.height+2);layout()
            val p=position();assertEquals(header.height-2,p[1])
            val image=Bitmap.createBitmap(360,400,Bitmap.Config.ARGB_8888);scroll.draw(Canvas(image))
            assertEquals(themes.active.tokens.tertiary,image.getPixel(p[0]-3,header.height-4))
            assertNotEquals(themes.active.tokens.secondary,image.getPixel(p[0]-3,p[1]+24))
            image.recycle()
        } finally { host.pause().stop().destroy() }
    }

}
