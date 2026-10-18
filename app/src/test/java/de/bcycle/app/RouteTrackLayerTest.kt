package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.controller.FrameBufferController
import org.mapsforge.map.model.Model
import org.mapsforge.map.view.FrameBufferHA3
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteTrackLayerTest {
    @Test fun routeUsesMapFramebufferScaleAroundGesturePivotBeforeZoomLevelChanges() {
        AndroidGraphicFactory.createInstance(RuntimeEnvironment.getApplication())
        val model = Model()
        model.displayModel.setFixedTileSize(256)
        model.frameBufferModel.setOverdrawFactor(1.0)
        val framebuffer = FrameBufferHA3(model.frameBufferModel, model.displayModel, AndroidGraphicFactory.INSTANCE)
        val controller = FrameBufferController.create(framebuffer, model)
        try {
            model.mapViewPosition.setCenter(LatLong(0.0, 0.0)); model.mapViewPosition.setZoomLevel(10, false)
            model.mapViewDimension.setDimension(Dimension(512, 512))
            val mapSize = MercatorProjection.getMapSize(10, 256)
            val dimension = framebuffer.dimension
            val origin = mapSize / 2.0 - dimension.width / 2.0
            val inset = (dimension.width - 512) / 2.0
            fun location(x: Double, y: Double) = MapPoint(MercatorProjection.pixelYToLatitude(origin + inset + y, mapSize), MercatorProjection.pixelXToLongitude(origin + inset + x, mapSize))
            val layer = RouteTrackLayer(1f)
            layer.setDisplayModel(model.displayModel)
            layer.update(GpxRoute(listOf(location(100.0, 150.0), location(400.0, 150.0)), 2, 1), emptyList(), GpsValues(), true, false)
            val drawing = AndroidGraphicFactory.getBitmap(framebuffer.drawingBitmap)
            val graphicCanvas = AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(drawing))
            layer.draw(BoundingBox(-1.0, -1.0, 1.0, 1.0), 10, graphicCanvas, Point(origin, origin), Rotation.NULL_ROTATION)
            val sourceFile = java.io.File("build/reports/maps/route-source.png").apply { parentFile!!.mkdirs() }
            sourceFile.outputStream().use { drawing.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertEquals(1024.0, model.mapViewPosition.scaleFactor, 0.001)
            framebuffer.frameFinished(MapPosition(LatLong(0.0, 0.0), 10.0, Rotation.NULL_ROTATION))
            fun routeY(): Double {
                val output = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                framebuffer.draw(AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(output)), Rotation.NULL_ROTATION)
                val pixels = mutableListOf<Int>()
                for (y in 0 until 512) for (x in 0 until 512) {
                    val color = output.getPixel(x, y)
                    if (Color.blue(color) > Color.red(color) + 30 && Color.blue(color) > Color.green(color) + 20) pixels += y
                }
                val debug = java.io.File("build/reports/maps/route-scale.png").apply { parentFile!!.mkdirs() }
                debug.outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
                assertTrue("Route must be present in the map framebuffer (${framebuffer.dimension})", pixels.isNotEmpty())
                return pixels.average()
            }
            val before = routeY()
            val pivot = location(350.0, 300.0)
            model.mapViewPosition.setPivot(LatLong(pivot.latitude, pivot.longitude))
            model.mapViewPosition.setScaleFactorAdjustment(1.5)
            assertEquals(10, model.mapViewPosition.zoomLevel.toInt()) // Fingers have not been released.
            val during = routeY()
            assertEquals(300 + (before - 300) * 1.5, during, 3.0)
            assertTrue(before - during > 50)
        } finally { controller.destroy(); framebuffer.destroy(); model.mapViewPosition.destroy() }
    }
    @Test fun plannedRouteHasSixPixelWidthAtDensityOneAndThirtyFivePercentOpacity() {
        AndroidGraphicFactory.createInstance(RuntimeEnvironment.getApplication())
        val model = Model(); model.displayModel.setFixedTileSize(256)
        val mapSize = MercatorProjection.getMapSize(10, 256)
        val origin = mapSize / 2.0 - 256
        fun point(x: Double, y: Double) = MapPoint(MercatorProjection.pixelYToLatitude(origin + y, mapSize),
            MercatorProjection.pixelXToLongitude(origin + x, mapSize))
        val layer = RouteTrackLayer(1f); layer.setDisplayModel(model.displayModel)
        layer.update(GpxRoute(listOf(point(100.0, 256.0), point(400.0, 256.0)), 2, 1), emptyList(), GpsValues(), true, false)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        layer.draw(BoundingBox(-1.0, -1.0, 1.0, 1.0), 10,
            AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(bitmap)), Point(origin, origin), Rotation.NULL_ROTATION)
        val color = bitmap.getPixel(250, 256)
        assertEquals(128, Color.alpha(color)); assertEquals(18.0, Color.red(color).toDouble(), 2.0)
        assertEquals(59.0, Color.green(color).toDouble(), 2.0); assertEquals(117.0, Color.blue(color).toDouble(), 2.0)
        assertEquals(6, (0 until 512).count { Color.alpha(bitmap.getPixel(250, it)) > 0 })
        layer.update(GpxRoute(listOf(point(100.0, 256.0), point(400.0, 256.0)), 2, 1), emptyList(), GpsValues(), true, false, night = true)
        bitmap.eraseColor(Color.TRANSPARENT)
        layer.draw(BoundingBox(-1.0, -1.0, 1.0, 1.0), 10,
            AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(bitmap)), Point(origin, origin), Rotation.NULL_ROTATION)
        val night = bitmap.getPixel(250, 256)
        assertEquals(128, Color.alpha(night)); assertTrue(Color.blue(night) > Color.blue(color))
        assertEquals(6, (0 until 512).count { Color.alpha(bitmap.getPixel(250, it)) > 0 })
        val custom=MapLineStyle(Color.MAGENTA,8f,25)
        layer.update(GpxRoute(listOf(point(100.0,256.0),point(400.0,256.0)),2,1),emptyList(),GpsValues(),true,false,savedStyle=custom)
        bitmap.eraseColor(Color.TRANSPARENT)
        layer.draw(BoundingBox(-1.0,-1.0,1.0,1.0),10,
            AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(bitmap)),Point(origin,origin),Rotation.NULL_ROTATION)
        assertEquals(64,Color.alpha(bitmap.getPixel(250,256)))
        assertEquals(8,(0 until 512).count { Color.alpha(bitmap.getPixel(250,it))>0 })
        layer.update(null,listOf(point(100.0,256.0),point(400.0,256.0)),GpsValues(),false,true,
            recordedStyle=MapLineStyle(Color.GREEN,8f,50))
        bitmap.eraseColor(Color.TRANSPARENT)
        layer.draw(BoundingBox(-1.0,-1.0,1.0,1.0),10,
            AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(bitmap)),Point(origin,origin),Rotation.NULL_ROTATION)
        assertEquals(128,Color.alpha(bitmap.getPixel(250,256))) // Outline and colored stroke share one opacity.
        assertEquals(255,Color.green(bitmap.getPixel(250,256)))
        model.mapViewPosition.destroy()
    }
    @Test fun positionDotHasSmallPhoneDirectionArrowIndependentOfGpsCourseAndHidesUnknownHeading() {
        AndroidGraphicFactory.createInstance(RuntimeEnvironment.getApplication())
        val model=Model();model.displayModel.setFixedTileSize(256)
        val origin=MercatorProjection.getMapSize(10,256)/2.0-256
        val layer=RouteTrackLayer(1f);layer.setDisplayModel(model.displayModel)
        val gps=GpsValues(latitude=0.0,longitude=0.0,bearingDegrees=90.0)
        val bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
        fun draw(direction:Double?) {
            bitmap.eraseColor(Color.TRANSPARENT)
            layer.update(null,emptyList(),gps,false,false,phoneHeading=direction)
            layer.draw(BoundingBox(-1.0,-1.0,1.0,1.0),10,
                AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(bitmap)),Point(origin,origin),Rotation.NULL_ROTATION)
        }
        val blue=Color.rgb(0,100,245)
        draw(0.0)
        assertEquals(blue,bitmap.getPixel(256,256));assertEquals(blue,bitmap.getPixel(256,238))
        assertEquals(0,Color.alpha(bitmap.getPixel(274,256)))
        draw(90.0)
        assertEquals(blue,bitmap.getPixel(274,256));assertEquals(0,Color.alpha(bitmap.getPixel(256,238)))
        draw(null);assertEquals(0,Color.alpha(bitmap.getPixel(274,256)))
        assertEquals(blue,bitmap.getPixel(256,256))
        draw(Double.NaN);assertEquals(0,Color.alpha(bitmap.getPixel(256,238)))
        bitmap.recycle();model.mapViewPosition.destroy()
    }
    @Test fun attachedHeightLabelIncludesValidZeroAndHidesUnavailableHeight() {
        AndroidGraphicFactory.createInstance(RuntimeEnvironment.getApplication())
        val model = Model(); model.displayModel.setFixedTileSize(256)
        val origin = MercatorProjection.getMapSize(10, 256) / 2.0 - 256
        val layer = RouteTrackLayer(1f); layer.setDisplayModel(model.displayModel)
        fun labelPixels(height: Double?): Int {
            layer.update(null, emptyList(), GpsValues(latitude = 0.0, longitude = 0.0, altitudeM = height), false, false)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            layer.draw(BoundingBox(-1.0, -1.0, 1.0, 1.0), 10,
                AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(bitmap)), Point(origin, origin), Rotation.NULL_ROTATION)
            return (245..270).sumOf { y -> (274..340).count { x -> Color.alpha(bitmap.getPixel(x, y)) > 0 } }
        }
        assertEquals(0, labelPixels(null)); assertEquals(0, labelPixels(Double.NaN))
        assertTrue(labelPixels(0.0) > 100); assertTrue(labelPixels(487.0) > 100)
        model.mapViewPosition.destroy()
    }

}
