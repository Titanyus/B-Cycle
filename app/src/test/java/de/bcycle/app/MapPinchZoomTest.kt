package de.bcycle.app

import android.os.Looper
import android.view.MotionEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class MapPinchZoomTest {
    private fun event(action:Int,time:Long,span:Float):MotionEvent {
        val count=if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_UP) 1 else 2
        val properties=Array(count) { i -> MotionEvent.PointerProperties().apply { id=i;toolType=MotionEvent.TOOL_TYPE_FINGER } }
        val coords=Array(count) { i -> MotionEvent.PointerCoords().apply { x=180f+(if(i==0) -span/2 else span/2);y=200f;pressure=1f;size=1f } }
        return MotionEvent.obtain(1000,time,action,count,properties,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
    }
    @Test fun pinchContinuesBelowPlatformSpanThresholdAndCoastsUntilNewTouch() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val map=testDescendants(a.window.decorView).filterIsInstance<MapPanel>().single().mapView
            val pinch=MapPinchZoom(map) { 19 };map.model.mapViewPosition.setZoom(16.0,false)
            fun send(action:Int,time:Long,span:Float):Boolean = event(action,time,span).let { e -> try { pinch.onTouch(e) } finally { e.recycle() } }
            assertFalse(send(MotionEvent.ACTION_DOWN,1000,300f))
            assertTrue(send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),1010,300f))
            var previous=16.0
            for((i,span) in listOf(200f,100f,50f,25f,12f).withIndex()) {
                assertTrue(send(MotionEvent.ACTION_MOVE,1030+i*20L,span))
                val zoom=map.model.mapViewPosition.zoom
                assertTrue("Zoom must continue at span $span",zoom<previous);previous=zoom
            }
            send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),1120,12f)
            send(MotionEvent.ACTION_UP,1130,12f)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
            assertTrue("Zoom continues briefly after release",map.model.mapViewPosition.zoom<previous)
            send(MotionEvent.ACTION_DOWN,1300,12f)
            val stopped=map.model.mapViewPosition.zoom
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertEquals(stopped,map.model.mapViewPosition.zoom,0.0)
            pinch.cancel()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun boundsCancelAndSingleFingerOwnershipArePreserved() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val map=testDescendants(host.get().window.decorView).filterIsInstance<MapPanel>().single().mapView
            map.setZoomLevelMax(17);val pinch=MapPinchZoom(map) { 17 }
            fun send(action:Int,time:Long,span:Float)=event(action,time,span).let { e -> try { pinch.onTouch(e) } finally { e.recycle() } }
            map.model.mapViewPosition.setZoom(16.0,false)
            assertFalse(send(MotionEvent.ACTION_DOWN,1000,100f))
            send(MotionEvent.ACTION_POINTER_DOWN or (1 shl 8),1010,100f)
            send(MotionEvent.ACTION_MOVE,1030,10000f);assertEquals(17.0,map.model.mapViewPosition.zoom,0.0)
            send(MotionEvent.ACTION_MOVE,1050,1f);assertEquals(6.0,map.model.mapViewPosition.zoom,0.0)
            send(MotionEvent.ACTION_CANCEL,1060,1f)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(6.0,map.model.mapViewPosition.zoom,0.0)
            assertFalse(send(MotionEvent.ACTION_MOVE,1080,100f))
        } finally { host.pause().stop().destroy() }
    }
}
