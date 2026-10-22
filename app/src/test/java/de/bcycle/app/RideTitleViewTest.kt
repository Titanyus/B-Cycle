package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RideTitleViewTest {
    @Test fun animationAcceleratesBrakesHoldsAndFadesBeforeRestart() {
        val host=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val title=RideTitleView(host.get()).apply { text="Ein sehr langer Fahrradtitel mit vielen sichtbaren Worten bis zum vollstaendigen Ende" }
            host.get().setContentView(title)
            title.measure(View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY))
            title.layout(0,0,240,80)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val animator=RideTitleView::class.java.getDeclaredField("motion").apply { isAccessible=true }.get(title) as android.animation.ValueAnimator
            val offset=RideTitleView::class.java.getDeclaredField("offset").apply { isAccessible=true }
            animator.currentPlayTime=0
            assertEquals(0f,title.alpha,.01f)
            animator.currentPlayTime=1500
            assertEquals(.5f,title.alpha,.02f);assertEquals(0f,offset.getFloat(title),.01f)
            animator.currentPlayTime=6500
            assertEquals(1f,title.alpha,.01f);assertEquals(0f,offset.getFloat(title),.01f)
            animator.currentPlayTime=9000
            assertTrue(offset.getFloat(title)>0)
            animator.currentPlayTime=animator.duration-1500
            assertEquals(0f,title.alpha,.01f)
            animator.currentPlayTime=animator.duration+1500
            assertEquals(0f,offset.getFloat(title),.01f);assertEquals(.5f,title.alpha,.02f)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun sharedConventionHasFixedPhasesSmoothRampsAndEighteenDpPerSecondCruise() {
        val motion=TextScrollerMotion(180f,1f)
        assertEquals(30000.0,motion.durationMs,.001)
        assertEquals(.5f,motion.frame(1500.0).alpha,.001f)
        assertEquals(0f,motion.frame(6999.0).offset,.001f)
        assertEquals(36f,motion.frame(11000.0).offset,.001f)
        assertEquals(54f,motion.frame(12000.0).offset,.001f)
        assertEquals(144f,motion.frame(17000.0).offset,.001f)
        assertEquals(180f,motion.frame(21000.0).offset,.001f)
        assertEquals(1f,motion.frame(24999.0).alpha,.001f)
        assertEquals(.5f,motion.frame(26500.0).alpha,.001f)
        assertEquals(0f,motion.frame(28500.0).alpha,.001f)
        assertEquals(.5f,motion.frame(31500.0).alpha,.001f)
        val small=TextScrollerMotion(12f,1f)
        assertEquals(8000.0,small.travelMs,.001)
        for(ms in 0..24000 step 100) assertTrue(small.frame(ms.toDouble()).offset in 0f..12f)
        // At both joins, speed approaches the cruise speed with no jump.
        assertEquals(.018f,motion.frame(11001.0).offset-motion.frame(11000.0).offset,.0001f)
        assertTrue(motion.frame(7001.0).offset<.0001f)
        assertTrue(180f-motion.frame(20999.0).offset<.0001f)
    }
    @Test fun finalTitleCharactersBecomeVisibleAfterScrollingPastInitialViewport() {
        val prefix="Eine sehr lange Fahrradtour quer durch die ganze Stadt und noch viel weiter "
        val text=SpannableString(prefix+"ENDE").apply {
            setSpan(ForegroundColorSpan(Color.GREEN),prefix.length,length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val title=RideTitleView(RuntimeEnvironment.getApplication()).apply { setTextColor(Color.RED);this.text=text }
        title.measure(View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY))
        title.layout(0,0,240,80)
        val distance=title.paint.measureText(title.transformationMethod.getTransformation(text,title).toString())-title.width
        assertTrue(distance>title.width)
        fun greenPixels():Int {
            val image=Bitmap.createBitmap(title.width,title.height,Bitmap.Config.ARGB_8888)
            try {
                title.draw(Canvas(image))
                val pixels=IntArray(image.width*image.height);image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
                return pixels.count { Color.alpha(it)>100 && Color.green(it)>120 && Color.red(it)<70 }
            } finally { image.recycle() }
        }
        assertEquals(0,greenPixels())
        RideTitleView::class.java.getDeclaredField("offset").apply { isAccessible=true }.setFloat(title,distance)
        assertTrue("The end marker must be painted after scrolling the complete title",greenPixels()>10)
    }
}
