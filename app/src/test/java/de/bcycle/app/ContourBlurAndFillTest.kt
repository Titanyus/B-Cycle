package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.RippleDrawable
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
class ContourBlurAndFillTest {
    @Test fun blurSpreadsActualColoredHighlightsWithoutModifyingItsSourceOrFillingDistantPixels() {
        val source=Bitmap.createBitmap(48,40,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(source);val paint=Paint().apply { color=Color.RED }
        canvas.drawRect(8f,17f,24f,23f,paint);paint.color=Color.WHITE;canvas.drawRect(24f,17f,40f,23f,paint)
        val blurred=BicycleContourBlur.blur(source,6f)
        assertEquals(0,Color.alpha(source.getPixel(16,15)))
        assertTrue(Color.alpha(blurred.getPixel(16,15))>0)
        assertTrue(Color.red(blurred.getPixel(16,15))>Color.blue(blurred.getPixel(16,15)))
        assertTrue(Color.blue(blurred.getPixel(32,15))>Color.blue(blurred.getPixel(16,15)))
        assertEquals(0,Color.alpha(blurred.getPixel(0,0)))
        assertEquals(Color.RED,source.getPixel(16,20))
        source.recycle();blurred.recycle()
    }
    @Test fun designerIntensityAmplifiesOnlyBlurredAlphaAndKeepsSourceUnchanged() {
        val source=Bitmap.createBitmap(40,40,Bitmap.Config.ARGB_8888)
        Canvas(source).drawRect(12f,12f,28f,28f,Paint().apply { color=Color.WHITE })
        val normal=BicycleContourBlur.blur(source,6f)
        val boosted=BicycleContourBlur.blur(source,6f,6f)
        val alpha=Color.alpha(normal.getPixel(10,20))
        assertTrue(alpha>0);assertTrue(alpha<255)
        assertEquals((alpha*6).coerceAtMost(255),Color.alpha(boosted.getPixel(10,20)))
        assertEquals(0,Color.alpha(source.getPixel(10,20)))
        assertEquals(Color.WHITE,source.getPixel(20,20))
        listOf(source,normal,boosted).forEach(Bitmap::recycle)
    }
    @Test fun activeFillHasItsOwnAngleAndReadableTextAcrossAllThemesWhileSelectionStillWorks() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("gradient-fill",0))
            val button=BicycleSegmentButton(a,themes,multiple=true).apply { text="Mann" }
            a.setContentView(LinearLayout(a).apply { addView(button,LinearLayout.LayoutParams(156,48)) })
            button.measure(View.MeasureSpec.makeMeasureSpec(156,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(48,View.MeasureSpec.EXACTLY));button.layout(0,0,156,48)
            button.performClick();assertTrue(button.isChecked)
            for(theme in BicycleThemes.presets) {
                themes.activate(theme.id)
                val tokens=theme.tokens;val foreground=button.currentTextColor
                assertTrue(ThemeValidator.contrast(foreground,tokens.primary)>=ThemeValidator.minimumTextContrast)
                assertTrue(ThemeValidator.contrast(foreground,BicycleAccentGradient.fillDark(tokens.primary,foreground,BicycleInputStyle.strength(theme.id)))>=ThemeValidator.minimumTextContrast)
            }
            themes.activate(BicycleThemes.red.id)
            val bitmap=Bitmap.createBitmap(156,48,Bitmap.Config.ARGB_8888)
            val fill=(button.background as RippleDrawable).getDrawable(0)
            fill.setBounds(0,0,156,48);fill.draw(Canvas(bitmap))
            val top=Color.red(bitmap.getPixel(78,5));val bottom=Color.red(bitmap.getPixel(78,42))
            if(kotlin.math.sin(Math.toRadians(BicycleDesign.accentFillAngle.toDouble()))>0) assertTrue(top>bottom)
            else assertTrue(top<bottom)
            button.performClick();assertFalse(button.isChecked)
            bitmap.recycle()
        } finally { host.pause().stop().destroy() }
    }
}
