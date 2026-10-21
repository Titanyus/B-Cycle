package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrendChevronsTest {
    private fun image(chevrons:TrendChevrons)=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888).also {
        chevrons.draw(Canvas(it),40f,50f,20f*TrendChevrons.SCALE,Color.WHITE)
    }
    private fun maxAlpha(bitmap:Bitmap)=(0 until 100).maxOf { y -> (0 until 100).maxOf { x -> Color.alpha(bitmap.getPixel(x,y)) } }
    @Test fun oneTwoAndThreeChevronsRenderAsSeparateStackedGlyphsInBothDirections() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val chevrons=TrendChevrons(View(host.get()))
            for(sign in listOf(-1,1)) for(n in 1..3) {
                chevrons.update(sign*n);val bitmap=image(chevrons)
                val occupied=(0 until 100).flatMap { y -> (0 until 100).filter { x -> Color.alpha(bitmap.getPixel(x,y))>0 } }
                assertTrue(occupied.max()-occupied.min()+1 in 15..19) // Former 20 px width, now 15 px plus stroke.
                val tips=(0..2).map { 50f-sign*it*7.8f-sign*3.75f }
                val expected=listOf(255,216,178)
                for(i in 0 until n) {
                    val tip=tips[i].toInt()
                    val alpha=(tip-1..tip+1).maxOf { y -> (47..48).maxOf { x -> Color.alpha(bitmap.getPixel(x,y)) } }
                    assertTrue("sign=$sign level=$n glyph=$i alpha=$alpha",kotlin.math.abs(alpha-expected[i])<=3)
                }
                bitmap.recycle()
            }
            chevrons.clear();assertEquals(0,maxAlpha(image(chevrons)))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun attachedGlyphsFadeWithinHalfASecondAndFadeOutWithoutLingeringAnimation() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val view=View(host.get());host.get().setContentView(view)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            assertTrue(view.isAttachedToWindow && view.isShown)
            assertTrue(android.animation.ValueAnimator.areAnimatorsEnabled())
            org.robolectric.shadows.ShadowChoreographer.setPaused(true)
            org.robolectric.shadows.ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
            val chevrons=TrendChevrons(view)
            fun animator()=(TrendChevrons::class.java.getDeclaredField("animator").apply { isAccessible=true }.get(chevrons) as android.animation.ValueAnimator)
            chevrons.update(3)
            val fadeIn=animator()
            assertEquals(500L,fadeIn.duration)
            fadeIn.pause();fadeIn.currentPlayTime=250
            val intermediate=maxAlpha(image(chevrons));assertTrue("Intermediate alpha: $intermediate",intermediate in 1..254)
            fadeIn.end()
            assertEquals(255,maxAlpha(image(chevrons)))
            chevrons.update(0);assertTrue(chevrons.visible)
            val fadeOut=animator();fadeOut.pause();fadeOut.currentPlayTime=250
            assertTrue(maxAlpha(image(chevrons)) in 1..254)
            fadeOut.end()
            assertFalse(chevrons.visible);assertEquals(0,maxAlpha(image(chevrons)))
            chevrons.update(-2);host.get().setContentView(View(host.get()))
            assertEquals(255,maxAlpha(image(chevrons)))
        } finally { host.pause().stop().destroy() }
    }
}
