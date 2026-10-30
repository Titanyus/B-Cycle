package de.bcycle.app

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardPageViewportTest {
    @org.junit.Before fun useExplicitAnimationFrames() {
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
        android.animation.ValueAnimator::class.java.getDeclaredMethod("setDurationScale",Float::class.javaPrimitiveType).invoke(null,1f)
    }
    private fun layout(v:View) {
        v.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
        v.layout(0,0,360,800)
    }
    private fun prepare(view:View) {
        shadowOf(Looper.getMainLooper()).idle();layout(view)
        view.viewTreeObserver.dispatchOnPreDraw();shadowOf(Looper.getMainLooper()).idle()
        org.robolectric.shadows.ShadowChoreographer.setPaused(true)
        android.animation.ValueAnimator::class.java.getDeclaredMethod("setDurationScale",Float::class.javaPrimitiveType).invoke(null,1f)
    }
    private fun animator(view:DashboardPageViewport)=DashboardPageViewport::class.java.getDeclaredField("animator").apply { isAccessible=true }.get(view) as android.animation.ValueAnimator
    @Test fun livePageAndOutgoingPictureSlideTogetherWhileFooterStaysFixedAndPictureIsReleased() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val c=host.get();val viewport=DashboardPageViewport(c) { true }
            val page=View(c).apply { setBackgroundColor(Color.RED) };viewport.setPage(page)
            val footer=View(c)
            val root=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL;addView(viewport,LinearLayout.LayoutParams(-1,0,1f));addView(footer,LinearLayout.LayoutParams(-1,64)) }
            c.setContentView(root);prepare(root)
            val footerTop=footer.top;var changes=0
            assertTrue("attached",viewport.isAttachedToWindow);assertTrue("laid out",viewport.isLaidOut)
            viewport.slide(1) { changes++;page.setBackgroundColor(Color.BLUE) }
            assertTrue(viewport.running);assertEquals(1,changes)
            val outgoing=viewport.findViewWithTag<ImageView>("dashboard_page_outgoing")
            val bitmap=(outgoing.drawable as BitmapDrawable).bitmap
            assertEquals(Color.RED,bitmap.getPixel(100,100));assertTrue(page.translationX>0)
            viewport.slide(-1) { changes++ };assertEquals(1,changes)
            viewport.viewTreeObserver.dispatchOnPreDraw()
            assertEquals(300L,animator(viewport).duration);animator(viewport).currentPlayTime=140L
            assertTrue("width=${viewport.width}, x=${page.translationX}, scale=${android.animation.ValueAnimator.getDurationScale()}",viewport.running);assertTrue("x=${page.translationX}",page.translationX>=-.01f && page.translationX<=360.01f)
            assertTrue(outgoing.translationX<0);assertEquals(360f,page.translationX-outgoing.translationX,.01f);assertEquals(footerTop,footer.top);assertEquals(0f,footer.translationX,0f)
            animator(viewport).end()
            assertFalse(viewport.running);assertEquals(0f,page.translationX,0f)
            assertEquals(1,viewport.childCount);assertNull(outgoing.parent);assertNull(outgoing.drawable);assertTrue(bitmap.isRecycled)
            assertEquals(footerTop,footer.top)
            viewport.slide(-1) {};assertTrue(page.translationX<0);viewport.cancelTransition()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun cancellationBeforePreDrawOrDuringMotionRestoresTheLivePageWithoutStaleCallbacks() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val c=host.get();val viewport=DashboardPageViewport(c) { true };val page=View(c);viewport.setPage(page)
            c.setContentView(viewport);prepare(viewport)
            repeat(2) { attempt ->
                viewport.slide(1) {};val image=viewport.findViewWithTag<ImageView>("dashboard_page_outgoing")
                val bitmap=(image.drawable as BitmapDrawable).bitmap
                if(attempt==1) { viewport.viewTreeObserver.dispatchOnPreDraw();animator(viewport).currentPlayTime=80L }
                viewport.cancelTransition();viewport.viewTreeObserver.dispatchOnPreDraw()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
                assertFalse(viewport.running);assertEquals(0f,page.translationX,0f);assertTrue(bitmap.isRecycled);assertEquals(1,viewport.childCount)
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun disabledAnimationsAndUnmeasuredViewsSwitchImmediatelyWithoutSnapshot() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val viewport=DashboardPageViewport(c) { false };viewport.setPage(View(c));c.setContentView(viewport);prepare(viewport)
            var changes=0;viewport.slide(1) { changes++ }
            assertEquals(1,changes);assertFalse(viewport.running);assertEquals(1,viewport.childCount)
            val unmeasured=DashboardPageViewport(c) { true };unmeasured.setPage(View(c));unmeasured.slide(-1) { changes++ }
            assertEquals(2,changes);assertFalse(unmeasured.running);assertEquals(1,unmeasured.childCount)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun onlyTheMovingContentBlocksTouchesAndDetachingReleasesItsPicture() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val c=host.get();val viewport=DashboardPageViewport(c) { true };viewport.setPage(View(c));c.setContentView(viewport);prepare(viewport)
            viewport.slide(1) {};val image=viewport.findViewWithTag<ImageView>("dashboard_page_outgoing")
            val bitmap=(image.drawable as BitmapDrawable).bitmap
            val event=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,50f,50f,0)
            assertTrue(viewport.onInterceptTouchEvent(event));event.recycle()
            c.setContentView(View(c));assertFalse(viewport.running);assertTrue(bitmap.isRecycled)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun actualDashboardSwitchIncludesTheSameMapAndChangesItsHeightWithoutMovingTheFooter() {
        val c=org.robolectric.RuntimeEnvironment.getApplication()
        val runtime=RideRuntime.get(c);val prefs=runtime.preferences
        c.getSharedPreferences(HrvSettings.STORE,0).edit().putBoolean("showOnFirstStart",false).commit()
        SensorSetupLaunch(c).later(runtime.bikes.current())
        prefs.edit().putInt("dashboardPage",0)
            .putString("dashboardTemplate_0",DashboardTemplate.entries.first { it.hasMap }.name)
            .putString("dashboardTemplate_1",DashboardTemplate.entries.first { !it.hasMap }.name).commit()
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val root=a.window.decorView
            prepare(root)
            val viewport=root.findViewWithTag<DashboardPageViewport>("dashboard_page_viewport")
            val map=testDescendants(root).filterIsInstance<MapPanel>().single()
            val footer=root.findViewWithTag<View>("ride_toolbar");val footerTop=footer.top
            assertEquals(View.VISIBLE,map.visibility)
            val change=MainActivity::class.java.getDeclaredMethod("changeDashboard",Int::class.javaPrimitiveType).apply { isAccessible=true }
            change.invoke(a,1);layout(root)
            val dialogs=MainActivity::class.java.getDeclaredField("dialogs").apply { isAccessible=true }.get(a) as DialogNavigation
            assertTrue("visible=${runtime.visible}, editing=${a.dashboardEditing}, empty=${dialogs.empty}, attached=${viewport.isAttachedToWindow}, laid=${viewport.isLaidOut}, size=${viewport.width}x${viewport.height}, scale=${android.animation.ValueAnimator.getDurationScale()}",viewport.running)
            assertEquals(1,prefs.getInt("dashboardPage",-1));assertEquals(View.GONE,map.visibility)
            viewport.viewTreeObserver.dispatchOnPreDraw();animator(viewport).currentPlayTime=140L
            assertEquals(footerTop,footer.top);assertEquals(0f,footer.translationX,0f)
            val screenshot=android.graphics.Bitmap.createBitmap(root.width,root.height,android.graphics.Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(screenshot))
            java.io.File("build/reports/ui/dashboard-page-transition.png").apply { parentFile!!.mkdirs() }.outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };screenshot.recycle()
            animator(viewport).end();assertFalse(viewport.running)
            assertEquals(1,viewport.childCount);assertSame(map,testDescendants(root).filterIsInstance<MapPanel>().single())
            change.invoke(a,-1);layout(root)
            assertEquals(View.VISIBLE,map.visibility);assertEquals(0,prefs.getInt("dashboardPage",-1))
            host.pause().stop();assertFalse(viewport.running);assertEquals(1,viewport.childCount)
        } finally { host.destroy() }
    }

}
