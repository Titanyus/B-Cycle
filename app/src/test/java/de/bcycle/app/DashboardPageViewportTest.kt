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
    private fun touch(view:View,action:Int,x:Float,y:Float=50f,time:Long=0L) {
        val event=MotionEvent.obtain(0,time,action,x,y,0)
        try { view.dispatchTouchEvent(event) } finally { event.recycle() }
    }
    @Test fun asynchronousMapSnapshotKeepsTheOldPageAndIgnoresLateResultsAfterCancellation() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val viewport=DashboardPageViewport(a) { true }
            val page=View(a).apply { setBackgroundColor(Color.RED) }
            viewport.setPage(page);a.setContentView(viewport);prepare(viewport)
            var ready:((Boolean)->Unit)?=null;var changes=0;var releases=0
            viewport.preparePageSnapshot={ ready=it };viewport.releasePageSnapshot={ releases++ }
            viewport.slide(1) { changes++;page.setBackgroundColor(Color.BLUE) }
            assertTrue(viewport.running);assertEquals(0,changes)
            checkNotNull(ready).invoke(true)
            assertEquals(1,changes);assertTrue(viewport.running)
            assertNotNull(viewport.findViewWithTag<View>("dashboard_page_outgoing"));assertTrue(releases>0)
            viewport.cancelTransition()
            viewport.slide(1) { changes++ }
            val late=checkNotNull(ready);viewport.cancelTransition();late(true)
            assertEquals(1,changes);assertFalse(viewport.running);assertNull(viewport.findViewWithTag<View>("dashboard_page_outgoing"))
            viewport.slide(1) { changes++ };checkNotNull(ready).invoke(false)
            assertEquals("unavailable GL snapshot switches once without a broken animation",2,changes);assertFalse(viewport.running)
        } finally { org.robolectric.shadows.ShadowChoreographer.setPaused(false);host.pause().stop().destroy() }
    }
    @Test fun velocityHandoffStopsSmoothlyWithoutOvershootingAtAnySupportedInitialSpeed() {
        for(slope in listOf(0f,.2f,1f,2f,3f,10f)) {
            val easing=pageHandoffInterpolator(slope)
            assertEquals(0f,easing.getInterpolation(0f),0f);assertEquals(1f,easing.getInterpolation(1f),.00001f)
            var previous=0f
            for(i in 1..1000) {
                val value=easing.getInterpolation(i/1000f)
                assertTrue("slope=$slope t=$i value=$value",value>=previous-.000001f && value<=1.000001f)
                previous=value
            }
            assertEquals(slope.coerceAtMost(3f),easing.getInterpolation(.0001f)/.0001f,.002f)
            assertTrue(1-easing.getInterpolation(.9999f)<.00001f)
        }
    }
    @Test fun dragStaysUnderFingerUntilLiftThenHandsOffAtItsCurrentPosition() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val viewport=DashboardPageViewport(a) { true }
            val page=View(a).apply { isClickable=true;setBackgroundColor(Color.RED) }
            viewport.setPage(page);a.setContentView(viewport);prepare(viewport)
            var changes=0
            viewport.swipeEnabledAt={ _,y -> y<300 }
            viewport.changePage={ delta -> assertEquals(1,delta);viewport.slide(delta) { changes++ } }
            touch(viewport,MotionEvent.ACTION_DOWN,180f)
            touch(viewport,MotionEvent.ACTION_MOVE,160f,time=30)
            assertEquals(-4f,page.translationX,.01f);assertEquals(0,changes)
            touch(viewport,MotionEvent.ACTION_MOVE,109f,time=80)
            assertEquals(0,changes);assertFalse(viewport.running);assertEquals(-55f,page.translationX,.01f)
            touch(viewport,MotionEvent.ACTION_MOVE,70f,time=110)
            assertEquals(0,changes);assertFalse(viewport.running);assertEquals(-94f,page.translationX,.01f)
            touch(viewport,MotionEvent.ACTION_UP,70f,time=120)
            assertEquals(1,changes);assertTrue(viewport.running)
            val picture=viewport.findViewWithTag<ImageView>("dashboard_page_outgoing")
            assertEquals(-94f,picture.translationX,.01f);assertEquals(266f,page.translationX,.01f)
            viewport.viewTreeObserver.dispatchOnPreDraw()
            assertTrue(animator(viewport).duration<=300)
            assertTrue("handoff must start moving, not restart at zero speed",animator(viewport).interpolator.getInterpolation(.001f)>.0001f)
            animator(viewport).currentPlayTime=100
            assertEquals(360f,page.translationX-picture.translationX,.01f)
            animator(viewport).end();assertFalse(viewport.running);assertEquals(0f,page.translationX,0f)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun slowDragCanLeaveTheDisplayCompletelyBeforeRelease() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val viewport=DashboardPageViewport(a) { true };val page=View(a).apply { isClickable=true }
            viewport.setPage(page);a.setContentView(viewport);prepare(viewport)
            var changes=0
            viewport.swipeEnabledAt={ _,_ -> true };viewport.changePage={ delta -> viewport.slide(delta) { changes++ } }
            touch(viewport,MotionEvent.ACTION_DOWN,350f)
            touch(viewport,MotionEvent.ACTION_MOVE,320f,time=300)
            touch(viewport,MotionEvent.ACTION_MOVE,100f,time=1800)
            assertEquals(-234f,page.translationX,.01f);assertFalse(viewport.running);assertEquals(0,changes)
            touch(viewport,MotionEvent.ACTION_MOVE,-30f,time=2600)
            assertEquals(-360f,page.translationX,.01f);assertFalse(viewport.running);assertEquals(0,changes)
            touch(viewport,MotionEvent.ACTION_UP,-30f,time=2900)
            assertEquals(1,changes);assertEquals(0f,page.translationX,.01f)
            viewport.viewTreeObserver.dispatchOnPreDraw()
            assertFalse("no residual slide after a complete manual swipe",viewport.running);assertEquals(1,viewport.childCount)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun releaseUsesSpeedAndDistanceAndAllowsReversingPastHalfway() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val viewport=DashboardPageViewport(a) { true };val page=View(a).apply { isClickable=true }
            viewport.setPage(page);a.setContentView(viewport);prepare(viewport)
            var changes=0
            viewport.swipeEnabledAt={ _,_ -> true };viewport.changePage={ delta -> viewport.slide(delta) { changes++ } }
            // The same 64 px offset returns when slow and changes page when fast.
            touch(viewport,MotionEvent.ACTION_DOWN,180f)
            touch(viewport,MotionEvent.ACTION_MOVE,160f,time=300)
            touch(viewport,MotionEvent.ACTION_MOVE,100f,time=1600)
            touch(viewport,MotionEvent.ACTION_UP,100f,time=1900)
            assertEquals(0,changes);animator(viewport).end()
            touch(viewport,MotionEvent.ACTION_DOWN,180f,time=3000)
            touch(viewport,MotionEvent.ACTION_MOVE,160f,time=3020)
            touch(viewport,MotionEvent.ACTION_MOVE,100f,time=3050)
            assertEquals(0,changes)
            touch(viewport,MotionEvent.ACTION_UP,100f,time=3060)
            assertEquals(1,changes);viewport.viewTreeObserver.dispatchOnPreDraw();animator(viewport).end()
            // A slow release past halfway commits even without fling speed.
            touch(viewport,MotionEvent.ACTION_DOWN,340f,time=4000)
            touch(viewport,MotionEvent.ACTION_MOVE,320f,time=4300)
            touch(viewport,MotionEvent.ACTION_MOVE,100f,time=5500)
            assertEquals(1,changes)
            touch(viewport,MotionEvent.ACTION_UP,100f,time=5800)
            assertEquals(2,changes);viewport.viewTreeObserver.dispatchOnPreDraw();animator(viewport).end()
            // Pull back quickly while the page is still more than halfway out: return.
            touch(viewport,MotionEvent.ACTION_DOWN,340f,time=6000)
            touch(viewport,MotionEvent.ACTION_MOVE,320f,time=6050)
            touch(viewport,MotionEvent.ACTION_MOVE,60f,time=6600)
            touch(viewport,MotionEvent.ACTION_MOVE,130f,time=6650)
            assertEquals(-194f,page.translationX,.01f);assertEquals(2,changes)
            touch(viewport,MotionEvent.ACTION_UP,130f,time=6660)
            assertEquals(2,changes);animator(viewport).end();assertEquals(0f,page.translationX,0f)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun adjacentPageMovesWithTheFingerAndStaysVisibleDuringReturn() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val viewport=DashboardPageViewport(a) { true };val page=View(a).apply { isClickable=true;setBackgroundColor(Color.RED) }
            viewport.setPage(page);a.setContentView(viewport);prepare(viewport)
            var changes=0;var releases=0
            viewport.swipeEnabledAt={ _,_ -> true };viewport.changePage={ changes++ }
            viewport.prepareAdjacentPage={ direction,ready -> ready(PagePreview(View(a).apply { tag="neighbor";setBackgroundColor(if(direction>0) Color.BLUE else Color.GREEN) }) { releases++ }) }
            touch(viewport,MotionEvent.ACTION_DOWN,180f)
            touch(viewport,MotionEvent.ACTION_MOVE,100f,time=800)
            layout(viewport)
            val neighbor=viewport.findViewWithTag<View>("neighbor")
            assertEquals(-64f,page.translationX,.01f);assertEquals(296f,neighbor.translationX,.01f);assertEquals(0,changes)
            val image=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888)
            viewport.draw(android.graphics.Canvas(image));assertEquals(Color.RED,image.getPixel(100,100));assertEquals("next page is already visible before UP",Color.BLUE,image.getPixel(330,100));image.recycle()
            touch(viewport,MotionEvent.ACTION_MOVE,240f,time=1600);layout(viewport)
            assertEquals(1,releases);assertEquals(-284f,viewport.findViewWithTag<View>("neighbor").translationX,.01f)
            touch(viewport,MotionEvent.ACTION_CANCEL,240f,time=1700)
            animator(viewport).currentPlayTime=90
            assertEquals(-360f,viewport.findViewWithTag<View>("neighbor").translationX-page.translationX,.01f)
            animator(viewport).end();assertEquals(2,releases);assertEquals(1,viewport.childCount);assertEquals(0,changes)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun lateAdjacentPageIsReleasedAfterCancellationInsteadOfReappearing() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val viewport=DashboardPageViewport(a) { true };viewport.setPage(View(a).apply { isClickable=true })
            a.setContentView(viewport);prepare(viewport)
            var pending:((PagePreview?)->Unit)?=null;var releases=0
            viewport.swipeEnabledAt={ _,_ -> true };viewport.changePage={ fail("cancelled") }
            viewport.prepareAdjacentPage={ _,ready -> pending=ready }
            touch(viewport,MotionEvent.ACTION_DOWN,180f);touch(viewport,MotionEvent.ACTION_MOVE,100f,time=300)
            viewport.cancelTransition()
            checkNotNull(pending)(PagePreview(View(a)) { releases++ })
            assertEquals(1,releases);assertEquals(1,viewport.childCount);assertFalse(viewport.running)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun maplessDashboardPreviewRehousesOnlyOneMapAndRestoresItOnCancelOrCommit() {
        val c=org.robolectric.RuntimeEnvironment.getApplication();val runtime=RideRuntime.get(c);val prefs=runtime.preferences
        c.getSharedPreferences(HrvSettings.STORE,0).edit().putBoolean("showOnFirstStart",false).commit()
        SensorSetupLaunch(c).later(runtime.bikes.current())
        prefs.edit().putInt("dashboardPage",0)
            .putString("dashboardTemplate_0",DashboardTemplate.entries.first { !it.hasMap }.name)
            .putString("dashboardTemplate_1",DashboardTemplate.entries.first { it.hasMap }.name).commit()
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val root=host.get().window.decorView;prepare(root)
            val viewport=root.findViewWithTag<DashboardPageViewport>("dashboard_page_viewport")
            val map=testDescendants(root).filterIsInstance<MapPanel>().single();val original=map.parent
            assertEquals(View.GONE,map.visibility)
            touch(viewport,MotionEvent.ACTION_DOWN,180f);touch(viewport,MotionEvent.ACTION_MOVE,100f,time=800);layout(root)
            assertEquals(0,prefs.getInt("dashboardPage",-1));assertNotSame(original,map.parent);assertEquals(View.VISIBLE,map.visibility)
            assertEquals(1,testDescendants(root).filterIsInstance<BicycleMapView>().size)
            touch(viewport,MotionEvent.ACTION_CANCEL,100f,time=900);animator(viewport).end()
            assertSame(original,map.parent);assertEquals(View.GONE,map.visibility)
            touch(viewport,MotionEvent.ACTION_DOWN,180f,time=1000)
            touch(viewport,MotionEvent.ACTION_MOVE,100f,time=1050);touch(viewport,MotionEvent.ACTION_UP,100f,time=1060);layout(root)
            assertEquals(1,prefs.getInt("dashboardPage",-1));assertSame(original,map.parent);assertEquals(View.VISIBLE,map.visibility)
            assertEquals(1,testDescendants(root).filterIsInstance<BicycleMapView>().size)
            viewport.viewTreeObserver.dispatchOnPreDraw();animator(viewport).end()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun shortUncommittedAndCancelledDragsReturnWhileMapAndVerticalGesturesRemainUntouched() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val viewport=DashboardPageViewport(a) { true };val page=View(a).apply { isClickable=true }
            viewport.setPage(page);a.setContentView(viewport);prepare(viewport)
            viewport.swipeEnabledAt={ _,y -> y<300 };viewport.changePage={ fail("unexpected page change") }
            touch(viewport,MotionEvent.ACTION_DOWN,180f)
            touch(viewport,MotionEvent.ACTION_MOVE,160f,time=100)
            touch(viewport,MotionEvent.ACTION_UP,160f,time=500)
            assertTrue(viewport.running);assertEquals(1,viewport.childCount)
            animator(viewport).end();assertEquals(0f,page.translationX,0f)
            touch(viewport,MotionEvent.ACTION_DOWN,180f)
            touch(viewport,MotionEvent.ACTION_MOVE,160f,time=100)
            touch(viewport,MotionEvent.ACTION_CANCEL,160f,time=110)
            animator(viewport).end();assertEquals(0f,page.translationX,0f)
            touch(viewport,MotionEvent.ACTION_DOWN,180f,400f)
            touch(viewport,MotionEvent.ACTION_MOVE,80f,400f,100)
            touch(viewport,MotionEvent.ACTION_UP,80f,400f,200)
            assertFalse(viewport.running);assertEquals(0f,page.translationX,0f)
            touch(viewport,MotionEvent.ACTION_DOWN,180f)
            touch(viewport,MotionEvent.ACTION_MOVE,175f,150f,100)
            touch(viewport,MotionEvent.ACTION_UP,175f,150f,200)
            assertFalse(viewport.running);assertEquals(0f,page.translationX,0f)
        } finally { host.pause().stop().destroy() }
    }
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
            // This integration case checks page motion. PixelCopy has its own test; Robolectric
            // has no live GL surface from which a successful system snapshot could be taken.
            viewport.preparePageSnapshot={ ready -> ready(true) }
            val change=MainActivity::class.java.getDeclaredMethod("changeDashboard",Int::class.javaPrimitiveType).apply { isAccessible=true }
            touch(viewport,MotionEvent.ACTION_DOWN,180f)
            touch(viewport,MotionEvent.ACTION_MOVE,160f,time=30)
            assertEquals(-4f,viewport.getChildAt(0).translationX,.01f)
            touch(viewport,MotionEvent.ACTION_MOVE,100f,time=60)
            assertEquals(0,prefs.getInt("dashboardPage",-1))
            assertNotNull(viewport.findViewWithTag<View>("dashboard_adjacent_preview"))
            touch(viewport,MotionEvent.ACTION_UP,100f,time=70)
            layout(root)
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
