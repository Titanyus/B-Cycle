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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteTrackLayerTest {
    private fun size(view:View) { view.measure(View.MeasureSpec.makeMeasureSpec(512,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(512,View.MeasureSpec.EXACTLY));view.layout(0,0,512,512) }
    private fun ready(layer:RouteTrackLayer) {
        val done=CountDownLatch(1)
        val worker=RouteTrackLayer::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(layer) as ThreadPoolExecutor
        assertTrue(worker.queue.offer(Runnable { done.countDown() },5,TimeUnit.SECONDS))
        assertTrue(done.await(5,TimeUnit.SECONDS));shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun threeStylesAndLiveTrackPreserveAlphaAndGapsAtFractionalZoom() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val map=BicycleMapView(host.get());val overlay=RouteTrackLayer(host.get(),map)
        try {
            size(map);size(overlay);map.setCenter(0.0,0.0);map.setZoom(10.5)
            fun point(x:Double,y:Double):MapPoint {
                val p=org.oscim.core.Point();map.map().viewport().fromScreenPoint(x,y,p)
                return MapPoint(org.oscim.core.MercatorProjection.toLatitude(p.y),org.oscim.core.MercatorProjection.toLongitude(p.x))
            }
            val colors=listOf(Color.RED,Color.GREEN,Color.BLUE)
            val routes=(0..2).map { i -> StyledMapRoute(GpxRoute(listOf(point(80.0,80.0+i*80),point(430.0,80.0+i*80)),2,1),MapLineStyle(colors[i],4f,30+i*30)) }
            val live=listOf(point(80.0,350.0),point(200.0,350.0),point(310.0,350.0).copy(segmentStart=true),point(430.0,350.0))
            overlay.update(null,live,GpsValues(),true,true,overlays=routes);ready(overlay)
            val native=map.map().layers().filterIsInstance<VtmRouteLayer>().single()
            val snapshot=VtmRouteLayer::class.java.getDeclaredField("snapshot").apply { isAccessible=true }.get(native)
            @Suppress("UNCHECKED_CAST") val lines=snapshot.javaClass.getDeclaredField("lines").apply { isAccessible=true }.get(snapshot) as List<GlRouteLine>
            assertEquals(4,lines.size)
            val batches=GlRouteProjection.batches(lines,map.camera,4000.0)
            for(i in 0..2) {
                assertEquals(colors[i],batches[i].style.color)
                assertEquals(30+i*30,batches[i].style.opacity)
                assertEquals(1,batches[i].pieces.size);assertEquals(4,batches[i].endpoints.size)
            }
            assertEquals("GPS gap stays separate",2,batches.last().pieces.size)
            assertFalse(batches.last().planned)
            val image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
            overlay.draw(Canvas(image))
            assertEquals("route no longer belongs to the delayed Android view",0,Color.alpha(image.getPixel(256,80)))
            image.recycle()
        } finally { overlay.dispose();map.disposeMap();host.pause().stop().destroy() }
    }
    @Test fun routeProjectionUsesTheFrozenGlFrameEvenWhenTheUiCameraIsAlreadyAhead() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val map=BicycleMapView(host.get())
        class Frame:org.oscim.renderer.GLViewport() { fun capture() { copy(map.map().viewport()) } }
        try {
            size(map);map.setCenter(51.0,10.0);map.setZoom(16.5);map.setAnchor(.4f,.72f);map.rotate(-90f)
            val reference=map.camera;val frame=Frame().apply { capture() }
            val original=map.screenPoint(51.0,10.0)
            map.panBy(300f,-180f)
            fun drawAtOrigin():org.oscim.core.Point {
                GlRouteProjection.setFrameMatrix(frame,reference)
                val p=floatArrayOf(0f,0f,0f);frame.mvp.prj(p)
                return org.oscim.core.Point((p[0]+1)*512/2.0,(1-p[1])*512/2.0)
            }
            val old=drawAtOrigin()
            assertEquals("GL frame keeps its own camera",original.x,old.x,.01);assertEquals(original.y,old.y,.01)
            assertTrue(kotlin.math.abs(map.screenPoint(51.0,10.0).x-old.x)>100)
            frame.capture()
            val moved=drawAtOrigin();val expected=map.screenPoint(51.0,10.0)
            assertEquals(expected.x,moved.x,.01);assertEquals(expected.y,moved.y,.01)
            assertFalse("normal pan reuses uploaded geometry",GlRouteProjection.needsRebase(reference,map.camera))
            map.panBy(800f,0f);assertTrue(GlRouteProjection.needsRebase(reference,map.camera))
        } finally { map.disposeMap();host.pause().stop().destroy() }
    }
    @Test fun clippingKeepsOffscreenSegmentsSeparateAndWrapsAcrossTheDateLine() {
        val reference=org.oscim.core.MapPosition(0.0,0.0,1024.0)
        val scale=256*reference.scale
        val xs=listOf(-10000.0,-500.0,500.0,10000.0)
        val geometry=GlRouteGeometry(xs.flatMap { listOf(reference.x+it/scale,reference.y) }.toDoubleArray(),booleanArrayOf(true,false,true,false))
        val style=MapLineStyle(Color.RED,4f,40)
        val batch=GlRouteProjection.batches(listOf(GlRouteLine(geometry,style,false)),reference,1000.0).single()
        assertEquals(2,batch.pieces.size)
        assertArrayEquals(floatArrayOf(-1000f,0f,-500f,0f),batch.pieces[0],.01f)
        assertArrayEquals(floatArrayOf(500f,0f,1000f,0f),batch.pieces[1],.01f)
        reference.x=.9995
        val dateline=GlRouteGeometry(doubleArrayOf(.999,reference.y,.0001,reference.y),booleanArrayOf(true,false))
        val crossing=GlRouteProjection.batches(listOf(GlRouteLine(dateline,style,true)),reference,1000.0).single()
        assertEquals(1,crossing.pieces.size)
        assertTrue(crossing.pieces.single()[0]<0);assertTrue(crossing.pieces.single()[2]>0)
    }
    @Test fun gpsCompassAndUprightHeightUseTheGlFrameAndKeepTheirScreenSize() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val map=BicycleMapView(host.get());val overlay=RouteTrackLayer(host.get(),map)
        class Frame:org.oscim.renderer.GLViewport() { fun capture() { copy(map.map().viewport()) } }
        try {
            size(map);size(overlay);map.setCenter(51.0,10.0)
            overlay.update(null,emptyList(),GpsValues(latitude=51.0,longitude=10.0,altitudeM=512.0,accuracyM=20.0),false,false,phoneHeading=0.0)
            val gps=map.map().layers().filterIsInstance<VtmGpsLayer>().single()
            val snapshot=checkNotNull(gps.snapshot)
            val glyph=gps.heightGlyph(checkNotNull(snapshot.height))
            try {
                var ink=0
                for(y in 0 until glyph.height) for(x in 0 until glyph.width) if(glyph.getPixel(x,y)==Color.BLACK) ink++
                assertTrue("height label remains readable",ink>30)
                for(zoom in listOf(15.0,15.5)) for(rotation in listOf(0f,-90f)) {
                    map.setZoom(zoom);map.rotate(rotation);map.setAnchor(.4f,.72f)
                    val frame=Frame().apply { capture() };val pose=GlGpsProjection.pose(frame,snapshot)
                    assertEquals(512*.4-256,pose.x.toDouble(),.01);assertEquals(512*.72-256,pose.y.toDouble(),.01)
                    val north=map.screenPoint(51.001,10.0);val center=map.screenPoint(51.0,10.0)
                    val angle=Math.toDegrees(kotlin.math.atan2(north.x-center.x,-(north.y-center.y)))
                    assertEquals("compass follows geographic north",angle,checkNotNull(pose.heading).toDouble(),.01)
                    assertTrue(checkNotNull(pose.accuracyRadius)>0)
                    map.panBy(200f,-100f)
                    assertEquals("UI camera must not move a frozen GL marker",pose,GlGpsProjection.pose(frame,snapshot))
                    frame.capture();val moved=GlGpsProjection.pose(frame,snapshot);val expected=map.screenPoint(51.0,10.0)
                    assertEquals(expected.x-256,moved.x.toDouble(),.01);assertEquals(expected.y-256,moved.y.toDouble(),.01)
                    map.setCenter(51.0,10.0)
                }
                val image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
                overlay.draw(Canvas(image));assertEquals("GPS/height no longer use the delayed Android surface",0,Color.alpha(image.getPixel(256,256)));image.recycle()
                overlay.update(null,emptyList(),GpsValues(latitude=51.0,longitude=10.0,accuracyM=12.0),false,false,showHeight=false)
                assertNotNull(gps.snapshot?.accuracy);assertNull(gps.snapshot?.height)
                overlay.update(null,emptyList(),GpsValues(latitude=51.0,longitude=10.0,accuracyM=9.0),false,false)
                assertNull(gps.snapshot?.accuracy)
                overlay.update(null,emptyList(),GpsValues(),false,false);assertNull(gps.snapshot)
            } finally { glyph.recycle() }
        } finally { overlay.dispose();map.disposeMap();host.pause().stop().destroy() }
    }
    @Test fun growingLiveTrackKeepsItsLastGeometryWhileWorkerPreparesTheNewPoints() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val map=BicycleMapView(host.get());val overlay=RouteTrackLayer(host.get(),map)
        val blocked=CountDownLatch(1);val release=CountDownLatch(1)
        try {
            size(map);map.setCenter(51.0,10.0)
            val track=listOf(MapPoint(51.0,10.0,true),MapPoint(51.001,10.001))
            overlay.update(null,track,GpsValues(),false,true,trackGeneration=1);ready(overlay)
            val native=map.map().layers().filterIsInstance<VtmRouteLayer>().single()
            fun lines():List<GlRouteLine> {
                val snapshot=VtmRouteLayer::class.java.getDeclaredField("snapshot").apply { isAccessible=true }.get(native)
                @Suppress("UNCHECKED_CAST") return snapshot.javaClass.getDeclaredField("lines").apply { isAccessible=true }.get(snapshot) as List<GlRouteLine>
            }
            val old=lines().single().geometry
            val worker=RouteTrackLayer::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(overlay) as ThreadPoolExecutor
            worker.execute { blocked.countDown();release.await(5,TimeUnit.SECONDS) };assertTrue(blocked.await(5,TimeUnit.SECONDS))
            val grown=track+MapPoint(51.002,10.002)
            overlay.update(null,grown,GpsValues(),false,true,trackGeneration=1)
            assertSame("existing trail stays visible during preparation",old,lines().single().geometry)
            release.countDown();ready(overlay)
            assertEquals(3,lines().single().geometry.starts.size)
            overlay.update(null,grown+MapPoint(51.003,10.003,true),GpsValues(),false,true,trackGeneration=2)
            assertTrue("new recording cannot inherit old fallback",lines().isEmpty())
            overlay.update(null,emptyList(),GpsValues(),false,false,trackGeneration=2);ready(overlay)
            assertTrue("old worker callback cannot resurrect a hidden track",lines().isEmpty())
        } finally { release.countDown();overlay.dispose();map.disposeMap();host.pause().stop().destroy() }
    }
    @Test fun nativeVtmProjectionKeepsTheGpsAtTheRequestedAnchorAfterRotation() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val map=BicycleMapView(host.get())
        try {
            size(map);map.setZoom(16.5);map.setCenter(51.0,10.0)
            assertTrue("VTM raster shader is available",org.oscim.backend.AssetAdapter.readTextFile("shaders/texture_layer.glsl")!!.contains("$$"))
            for(angle in listOf(0f,-90f,-170f)) {
                map.rotate(angle);map.setAnchor(.4f,.72f)
                val p=map.screenPoint(51.0,10.0)
                assertEquals(512*.4,p.x,1.0);assertEquals(512*.72,p.y,1.0)
                assertEquals(angle.toDouble(),map.bearing.toDouble(),.001)
                if(angle==-90f) {
                    assertTrue("east must be up when riding east",map.screenPoint(51.0,10.001).y<p.y)
                    assertTrue("north must be left when riding east",map.screenPoint(51.001,10.0).x<p.x)
                }
            }
        } finally { map.disposeMap();host.pause().stop().destroy() }
    }
}
