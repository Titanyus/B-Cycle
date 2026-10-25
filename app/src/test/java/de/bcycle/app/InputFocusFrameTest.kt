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
    @Test fun derivedGradientUsesApprovedSortedStopsAndPreservesPrimary() {
        for(primary in listOf(Color.RED,Color.BLUE,Color.GREEN,Color.WHITE,Color.BLACK,0xffe7aa22.toInt())) {
            val stops=BicycleInputFrame.colors(primary)
            assertEquals(6,stops.size);assertEquals(primary,stops[2]);assertEquals(primary,stops[4])
            assertTrue(stops.all { Color.alpha(it)==255 })
            assertEquals(stops[0] and 0xffffff,stops[3] and 0xffffff)
            assertEquals(stops[1] and 0xffffff,stops[5] and 0xffffff)
            assertTrue(BicycleInputFrame.positions().toList().zipWithNext().all { it.first<=it.second })
        }
    }
    @Test fun focusedContourAndTwoBlursFollowScrollThemeAndEnabledStateWithoutChangingEdits() {
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
            val viewport=android.widget.FrameLayout(a).apply { addView(scroll,android.widget.FrameLayout.LayoutParams(-1,-1)) };a.setContentView(viewport)
            fun layout() { repeat(2) {
                viewport.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));viewport.layout(0,0,360,400)
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
            fun draw()=Bitmap.createBitmap(360,400,Bitmap.Config.ARGB_8888).also { viewport.draw(Canvas(it)) }
            val before=draw();val old=location();val geometry=listOf(focused.width,focused.height,focused.paddingLeft,focused.paddingRight)
            assertTrue(focused.requestFocus());layout()
            val active=draw()
            val glow=focused.getTag(R.id.input_frame)
            val cached=glow.javaClass.getDeclaredField("cache").apply { isAccessible=true }
            val firstCache=cached.get(glow);assertNotNull(firstCache)
            draw().recycle();assertSame(firstCache,cached.get(glow))
            val p=location();val x=p[0]-3;val y=p[1]+24
            assertEquals(red.tokens.secondary,before.getPixel(x,y))
            assertNotEquals(red.tokens.secondary,active.getPixel(x,y))
            assertNotEquals(before.getPixel(p[0]+2,y),active.getPixel(p[0]+2,y)) // Narrow blur also reaches just inside.
            assertEquals(before.getPixel(p[0]+8,y),active.getPixel(p[0]+8,y)) // Interior stays clear away from the contour.
            val background=Bitmap.createBitmap(focused.width,focused.height,Bitmap.Config.ARGB_8888)
            focused.background.draw(Canvas(background))
            assertEquals(0,Color.alpha(background.getPixel(focused.width/2,focused.height/2)))
            assertNotEquals(before.getPixel(p[0]+40,p[1]),active.getPixel(p[0]+40,p[1]))
            java.io.File("build/reports/ui/input-focus-frame.png").apply { parentFile!!.mkdirs() }.outputStream().use { active.compress(Bitmap.CompressFormat.PNG,100,it) }
            scroll.scrollTo(0,40);layout()
            val shifted=draw();val next=location()
            assertEquals(p[1]-40,next[1])
            // Draw via a parent, as Android does: the ScrollView canvas already carries -scrollY.
            for(offset in listOf(-3,-6)) assertEquals(active.getPixel(p[0]+offset,p[1]+24),shifted.getPixel(next[0]+offset,next[1]+24))
            assertEquals(red.tokens.secondary,shifted.getPixel(x,old[1]+24))
            themes.activate(blue.id);scroll.setBackgroundColor(blue.tokens.secondary);layout()
            val recolored=draw();val color=recolored.getPixel(next[0]-3,next[1]+24)
            assertTrue(Color.blue(color)>Color.red(color))
            focused.isEnabled=false
            val disabled=draw();assertNull(cached.get(glow));assertEquals(blue.tokens.secondary,disabled.getPixel(next[0]-3,next[1]+24))
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
            val viewport=android.widget.FrameLayout(a).apply { addView(scroll,android.widget.FrameLayout.LayoutParams(-1,-1)) };a.setContentView(viewport)
            fun layout() { repeat(2) {
                viewport.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));viewport.layout(0,0,360,400)
            } }
            layout();edit.requestFocus();layout()
            fun position():IntArray {
                val field=IntArray(2);val owner=IntArray(2);edit.getLocationInWindow(field);scroll.getLocationInWindow(owner)
                return intArrayOf(field[0]-owner[0],field[1]-owner[1])
            }
            scroll.scrollTo(0,position()[1]-header.height+2);layout()
            val p=position();assertEquals(header.height-2,p[1])
            val image=Bitmap.createBitmap(360,400,Bitmap.Config.ARGB_8888);viewport.draw(Canvas(image))
            assertEquals(themes.active.tokens.tertiary,image.getPixel(p[0]-3,header.height-4))
            assertNotEquals(themes.active.tokens.secondary,image.getPixel(p[0]-3,p[1]+24))
            image.recycle()
        } finally { host.pause().stop().destroy() }
    }

}
