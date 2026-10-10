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
                    if (Color.red(color) > 100 && Color.blue(color) > 100 && Color.green(color) < 80) pixels += y
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
}
