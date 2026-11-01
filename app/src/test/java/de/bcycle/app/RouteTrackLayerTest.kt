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
        worker.execute { done.countDown() };assertTrue(done.await(5,TimeUnit.SECONDS));shadowOf(Looper.getMainLooper()).idle()
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
            val image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
            overlay.draw(Canvas(image))
            for(i in 0..2) {
                val pixel=image.getPixel(256,80+i*80)
                assertEquals(colors[i] and 0xffffff,pixel and 0xffffff)
                assertEquals(((30+i*30)*255/100f).toInt().toDouble(),Color.alpha(pixel).toDouble(),2.0)
            }
            assertEquals("GPS gap stays empty",0,Color.alpha(image.getPixel(256,350)))
            assertTrue(Color.alpha(image.getPixel(150,350))>200);image.recycle()
        } finally { overlay.dispose();map.disposeMap();host.pause().stop().destroy() }
    }
    @Test fun gpsCompassAndUprightHeightLabelKeepTheirScreenSizeAcrossZoomAndRotation() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val map=BicycleMapView(host.get());val overlay=RouteTrackLayer(host.get(),map)
        try {
            size(map);size(overlay);map.setCenter(51.0,10.0)
            overlay.update(null,emptyList(),GpsValues(latitude=51.0,longitude=10.0,altitudeM=512.0,accuracyM=20.0),false,false,phoneHeading=0.0)
            var expectedInk:Int?=null
            for(zoom in listOf(15.0,15.5)) for(rotation in listOf(0f,-90f)) {
                map.setZoom(zoom);map.rotate(rotation)
                val image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
                try {
                    overlay.draw(Canvas(image));assertEquals(Color.rgb(0,100,245),image.getPixel(256,256))
                    val north=map.screenPoint(51.001,10.0)
                    val length=kotlin.math.hypot(north.x-256,north.y-256)
                    val x=(256+(north.x-256)*16/length).toInt();val y=(256+(north.y-256)*16/length).toInt()
                    assertTrue("compass follows geographic north",(x-1..x+1).any { px -> (y-1..y+1).any { py -> image.getPixel(px,py)==Color.rgb(0,100,245) } })
                    var ink=0
                    for(py in 240..272) for(px in 274..390) if(image.getPixel(px,py)==Color.BLACK) ink++
                    assertTrue("height remains readable",ink>30)
                    expectedInk?.let { assertEquals("height text stays upright and same size",it,ink) };expectedInk=ink
                } finally { image.recycle() }
            }
        } finally { overlay.dispose();map.disposeMap();host.pause().stop().destroy() }
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
