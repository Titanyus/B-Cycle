package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.*
import android.os.Looper
import android.view.View
import android.view.Gravity
import android.widget.*
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
class GlowChromeTest {
    private fun layout(view:View,w:Int=360,h:Int=800) { repeat(2) { view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h) } }
    @Test fun newChromePreservesNativeActionsAndRestoresSquareSegmentsInBothModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL;setPadding(24,24,24,24);setBackgroundColor(themes.active.tokens.secondary) }
            val header=BicycleComponents.header(a,themes).apply { addView(BicycleComponents.heading(a,themes,"HEADER")) };root.addView(header)
            val section=BicycleSectionHeader(a,themes,"ABSCHNITT");root.addView(section)
            val spinner=Spinner(a).apply { adapter=ArrayAdapter(a,android.R.layout.simple_spinner_dropdown_item,listOf("GPS","E-Bike")) }
            BicycleDialogStyle.spinner(spinner,themes);root.addView(spinner,LinearLayout.LayoutParams(-1,48))
            val toggle=Switch(a).apply { text="Schalter";isChecked=true;BicycleDialogStyle.control(this,themes) };root.addView(toggle,LinearLayout.LayoutParams(-1,48))
            val listToggle=CheckBox(a).apply { text="Listenauswahl";isChecked=true;BicycleDialogStyle.control(this,themes) };root.addView(listToggle,LinearLayout.LayoutParams(-1,48))
            val segments=LinearLayout(a);val selected=BicycleSegmentButton(a,themes,true).apply { text="Alle Zonen";isChecked=true };segments.addView(selected,LinearLayout.LayoutParams(-1,48));root.addView(segments)
            var clicks=0
            val filled=BicycleComponents.specialFilled(a,themes,"Special-Füllung") { clicks++ }
            val outline=BicycleComponents.specialOutline(a,themes,"Special-Rahmen") { clicks++ }
            for(button in listOf(filled,outline)) { root.addView(button,LinearLayout.LayoutParams(-1,48));BicycleDialogStyle.control(button,themes) }
            val viewport=ScrollView(a).apply { isFillViewport=true;addView(root) };a.setContentView(viewport);layout(viewport)
            val geometry=listOf(spinner.width,spinner.height,selected.width,selected.height,filled.width,filled.height)
            for(flat in listOf(false,true,false)) {
                themes.setFlatDesign(flat);shadowOf(Looper.getMainLooper()).idle();layout(viewport)
                val fill=((selected.background as RippleDrawable).getDrawable(0) as LayerDrawable).getDrawable(0) as GradientDrawable
                assertEquals(6f,fill.cornerRadius,0f)
                assertEquals(if(flat) ColorDrawable::class.java else LayerDrawable::class.java,header.background.javaClass)
                assertTrue(section.background is GradientDrawable)
                val circle=(toggle.thumbDrawable as LayerDrawable).getDrawable(1).current
                assertEquals(!flat,circle is BicycleChromaticSurface)
                val choice=((listToggle.compoundDrawablesRelative[2].current as InsetDrawable).drawable as LayerDrawable).getDrawable(1)
                assertEquals(!flat,choice is BicycleChromaticSurface)
                assertEquals(!flat,spinner.popupBackground is BicycleChromaticSurface)
                val row=spinner.getChildAt(0) as TextView
                assertTrue("selected row bottom ${row.bottom}, spinner ${spinner.height}",row.bottom<=spinner.height-spinner.paddingBottom)
                assertTrue(row.baseline+row.paint.fontMetrics.descent<=row.height)
                for(button in listOf(filled,outline)) { assertEquals(Color.WHITE,button.currentTextColor);assertEquals(BicycleDesign.strongTypeface,button.typeface);button.performClick() }
                assertEquals(geometry,listOf(spinner.width,spinner.height,selected.width,selected.height,filled.width,filled.height))
                val fillImage=Bitmap.createBitmap(filled.width,filled.height,Bitmap.Config.ARGB_8888)
                filled.background.setBounds(0,0,filled.width,filled.height);filled.background.draw(Canvas(fillImage))
                if(flat) assertEquals(fillImage.getPixel(filled.width/4,24),fillImage.getPixel(filled.width*3/4,24))
                else assertNotEquals(fillImage.getPixel(filled.width/4,24),fillImage.getPixel(filled.width*3/4,24))
                fillImage.recycle()
                val image=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888);viewport.draw(Canvas(image))
                java.io.File("build/reports/ui/chrome-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
            assertEquals(6,clicks);toggle.performClick();assertFalse(toggle.isChecked);listToggle.performClick();assertFalse(listToggle.isChecked)
            spinner.setSelection(1);assertEquals(1,spinner.selectedItemPosition)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun specialPillsReduceOnlyTheirVisualHeightInBothModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            var clicks=0
            val buttons=listOf(BicycleComponents.specialFilled(a,themes,"Füllung") { clicks++ },
                BicycleComponents.specialOutline(a,themes,"Rahmen") { clicks++ })
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                for(button in buttons) {
                    val padding=listOf(button.paddingLeft,button.paddingTop,button.paddingRight,button.paddingBottom)
                    layout(button,240,100)
                    button.background.setBounds(0,0,240,100)
                    val inset=(button.background as RippleDrawable).getDrawable(0) as InsetDrawable
                    assertEquals(Rect(0,10,240,90),inset.drawable!!.bounds)
                    val image=Bitmap.createBitmap(240,100,Bitmap.Config.ARGB_8888)
                    button.background.draw(Canvas(image))
                    assertEquals(0,Color.alpha(image.getPixel(120,5)))
                    assertEquals(0,Color.alpha(image.getPixel(120,95)))
                    if((button as BicycleSpecialButton).filled) assertTrue(Color.alpha(image.getPixel(120,50))>0)
                    image.recycle()
                    assertEquals(240,button.width);assertEquals(100,button.height)
                    assertEquals(padding,listOf(button.paddingLeft,button.paddingTop,button.paddingRight,button.paddingBottom))
                    button.performClick()
                }
            }
            assertEquals(4,clicks)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun footerUsesMirroredHeaderShadowWithoutSpacingAndFlatRemovesIt() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val root=FrameLayout(a).apply { setBackgroundColor(Color.WHITE) }
            val footer=bicycleFooter(a,themes);root.addView(footer,FrameLayout.LayoutParams(-1,48,Gravity.BOTTOM));a.setContentView(root);layout(root,180,180)
            fun image()=Bitmap.createBitmap(180,180,Bitmap.Config.ARGB_8888).also { root.draw(Canvas(it)) }
            val glow=image();assertTrue(Color.red(glow.getPixel(90,131))<255);assertEquals(Color.WHITE,glow.getPixel(90,119))
            val sample=Bitmap.createBitmap(180,180,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
            BicycleHeaderShadow(a).drawUp(Canvas(sample),0f,132f,180f)
            for(y in 120..131) assertEquals(sample.getPixel(90,y),glow.getPixel(90,y))
            themes.setFlatDesign(true);shadowOf(Looper.getMainLooper()).idle();layout(root,180,180)
            val flat=image();assertEquals(Color.WHITE,flat.getPixel(90,131));assertEquals(132,footer.top)
            listOf(glow,flat,sample).forEach(Bitmap::recycle)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun popupKeepsTertiaryInteriorAndDrawsItsOutlineAndHalo() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val spinner=Spinner(a).apply { adapter=ArrayAdapter(a,android.R.layout.simple_spinner_dropdown_item,listOf("A","B")) };BicycleDialogStyle.spinner(spinner,themes)
            val background=spinner.popupBackground;background.setBounds(0,0,240,160)
            val image=Bitmap.createBitmap(240,160,Bitmap.Config.ARGB_8888);background.draw(Canvas(image))
            assertEquals(themes.active.tokens.tertiary,image.getPixel(120,80))
            assertTrue(Color.alpha(image.getPixel(2,80))>0)
            java.io.File("build/reports/ui/chrome-popup.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
        } finally { host.pause().stop().destroy() }
    }
}
