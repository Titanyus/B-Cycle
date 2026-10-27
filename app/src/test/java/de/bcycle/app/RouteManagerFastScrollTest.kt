package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class RouteManagerFastScrollTest {
    @Test fun actualRouteCardsFollowDragAndStayAtDestinationAfterReleaseAndRelayout() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            var opened=0;var toggled=0
            val manager=RouteManagerView(c,{toggled++},themes,{opened++}).apply { fullscreen=true }
            val root=BicycleFullscreenHost(c).apply {
                orientation=android.widget.LinearLayout.VERTICAL;contentSpacingTarget=manager
                addView(manager,android.widget.LinearLayout.LayoutParams(-1,-1))
            }
            c.setContentView(root)
            val rows=(0 until 400).map { i -> ManagedRoute("r$i","Fahrt $i",false,"FIT",RouteDetails(1790000000000L-i*86400000L,40000.0,7200000,movingMs=6000000),true) }
            manager.submit(rows,null,false);manager.applyPageSpacing()
            fun layout() {
                root.forceLayout()
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,800)
            }
            layout()
            val list=manager.findViewWithTag<BicycleFastScrollList>("route_list")
            list.getChildAt(0).findViewWithTag<View>("route_eye_r0")?.requestFocus()
            org.robolectric.util.ReflectionHelpers.setField(
                org.robolectric.util.ReflectionHelpers.getField<Any>(list,"mAttachInfo"),"mInTouchMode",true)
            assertTrue(list.isInTouchMode)
            fun touch(action:Int,y:Float) {
                val e=MotionEvent.obtain(0,0,action,356f,y,0)
                try { list.dispatchTouchEvent(e) } finally { e.recycle() }
                shadowOf(Looper.getMainLooper()).idle();layout()
            }
            touch(MotionEvent.ACTION_DOWN,list.height*.2f)
            touch(MotionEvent.ACTION_MOVE,list.height*.8f)
            assertTrue("dragged first=${list.firstVisiblePosition}",list.firstVisiblePosition>250)
            val at=list.firstVisiblePosition
            touch(MotionEvent.ACTION_UP,list.height*.8f)
            repeat(3) { layout() }
            assertEquals(at,list.firstVisiblePosition)
            assertEquals(0,opened);assertEquals(0,toggled);assertFalse(manager.selecting)
            val fraction=BicycleFastScrollList::class.java.getDeclaredMethod("fraction").apply { isAccessible=true }
            var previous=(fraction.invoke(list) as Float)
            repeat(80) {
                list.scrollListBy(13);layout()
                val next=fraction.invoke(list) as Float
                assertTrue("Scroll thumb reversed from $previous to $next",next+0.00001f>=previous)
                previous=next
            }
        } finally { host.pause().stop().destroy() }
    }
}
