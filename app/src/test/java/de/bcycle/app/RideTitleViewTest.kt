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
            val travel=animator.duration-3000L
            fun position(fraction:Double):Float {
                animator.currentPlayTime=1500L+(travel*fraction).toLong()
                return offset.getFloat(title)
            }
            val early=position(.15)-position(.05)
            val middle=position(.55)-position(.45)
            val late=position(.95)-position(.85)
            assertTrue(early>0 && middle>early*2)
            assertTrue(late>0 && middle>late*2)
            val end=position(1.0)
            animator.currentPlayTime=1500L+travel+400L
            assertEquals(end,offset.getFloat(title),.01f);assertEquals(1f,title.alpha,.01f)
            animator.currentPlayTime=1500L+travel+800L+350L
            assertEquals(end,offset.getFloat(title),.01f);assertEquals(.5f,title.alpha,.02f)
            animator.currentPlayTime=animator.duration+200L
            assertEquals(0f,offset.getFloat(title),.01f);assertEquals(1f,title.alpha,.01f)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun finalTitleCharactersBecomeVisibleAfterScrollingPastInitialViewport() {
        val prefix="Eine sehr lange Fahrradtour quer durch die ganze Stadt und noch viel weiter "
        val text=SpannableString(prefix+"ENDE").apply {
            setSpan(ForegroundColorSpan(Color.GREEN),prefix.length,length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val title=RideTitleView(RuntimeEnvironment.getApplication()).apply { setTextColor(Color.RED);this.text=text }
        title.measure(View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY))
        title.layout(0,0,240,80)
        val distance=title.paint.measureText(text.toString())-title.width
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
