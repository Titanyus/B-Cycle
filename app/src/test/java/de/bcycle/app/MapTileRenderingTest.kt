package de.bcycle.app

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition
import org.mapsforge.core.model.Rotation
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.controller.FrameBufferController
import org.mapsforge.map.layer.cache.FileSystemTileCache
import org.mapsforge.map.layer.cache.InMemoryTileCache
import org.mapsforge.map.layer.cache.TwoLevelTileCache
import org.mapsforge.map.layer.download.DownloadJob
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.model.MapViewPosition
import org.mapsforge.map.util.LayerUtil
import org.mapsforge.map.util.MapPositionUtil
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Real Mapsforge raster drawing from disk-only tiles; no server or download thread. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapTileRenderingTest {
    private val tileColor = Color.BLUE

    @Test fun fixed48TileCacheReproducesMissingTilesEvenWhenEveryTileIsOnDisk() {
        render(InMemoryTileCache(48), expectComplete = false)
    }

    @Test fun viewportCacheDrawsAllStoredTilesRepeatedlyAcrossZoomPanAndRotation() {
        render(ViewportTileCache(), expectComplete = true)
    }

    private fun render(memory: InMemoryTileCache, expectComplete: Boolean) {
        val app = RuntimeEnvironment.getApplication()
        AndroidGraphicFactory.createInstance(app)
        val factory = AndroidGraphicFactory.INSTANCE
        val model = DisplayModel().apply { setFixedTileSize(256) }
        val position = MapViewPosition(model)
        val disk = FileSystemTileCache(512, File(app.cacheDir, "render-${expectComplete}").apply { mkdirs() }, factory)
        val cache = TwoLevelTileCache(memory, disk)
        val source = MapSource.CYCLOSM.tiles()
        val layer = TileDownloadLayer(cache, position, source, factory).apply { setDisplayModel(model) }
        val size = FrameBufferController.calculateFrameBufferDimension(Dimension(1080, 1800), 1.2)
        val target = factory.createBitmap(size.width, size.height, false)
        val canvas = factory.createCanvas().apply { setBitmap(target) }
        val tile = factory.createTileBitmap(256, false).apply { setBackgroundColor(tileColor) }
        try {
            val scenes = if (expectComplete) listOf(15 to 0f, 16 to 0f, 16 to 45f, 17 to 90f, 15 to 0f) else listOf(15 to 0f)
            for ((index, scene) in scenes.withIndex()) {
                val zoom = scene.first.toByte()
                val center = LatLong(51 + index * .001, 10 + index * .001)
                val rotation = Rotation(scene.second, size.width / 2f, size.height / 2f)
                position.setMapPosition(MapPosition(center, zoom), false)
                val bounds = MapPositionUtil.getBoundingBox(center, zoom, rotation, 256, size, .5f, .68f)
                val origin = MapPositionUtil.getTopLeftPoint(center, zoom, size, 256)
                val tiles = LayerUtil.getTilePositions(bounds, zoom, origin, 256)
                assertTrue("Fixture must exceed the old cache", tiles.size > 48)
                val jobs = tiles.map { DownloadJob(it.tile, source) }
                jobs.forEach { disk.put(it, tile) }
                assertTrue(jobs.all { disk.containsKey(it) })
                repeat(3) {
                    layer.draw(bounds, zoom, canvas, origin, rotation)
                    val resident = jobs.count { job ->
                        val bitmap = cache.getImmediately(job)
                        bitmap?.decrementRefCount()
                        bitmap != null
                    }
                    if (expectComplete) {
                        assertEquals("Missing cached tiles after draw: zoom=$zoom angle=${scene.second}", jobs.size, resident)
                        // Actual pixels, not only the calculated cache capacity.
                        val bitmap = AndroidGraphicFactory.getBitmap(target)
                        for (p in tiles) {
                            val x = p.point.x.toInt() + 128; val y = p.point.y.toInt() + 128
                            if (x in 0 until size.width && y in 0 until size.height)
                                assertEquals("Blank tile at $x/$y", tileColor, bitmap.getPixel(x, y))
                        }
                    } else {
                        assertTrue("Old code must reproduce the gap", resident < jobs.size)
                        val bitmap = AndroidGraphicFactory.getBitmap(target)
                        assertTrue("Fixed cache must produce actual blank pixels", tiles.any { p ->
                            val x = p.point.x.toInt() + 128; val y = p.point.y.toInt() + 128
                            x in 0 until size.width && y in 0 until size.height && bitmap.getPixel(x, y) != tileColor
                        })
                    }
                }
            }
        } finally {
            layer.onDestroy(); cache.destroy(); position.destroy(); canvas.destroy()
            tile.decrementRefCount(); target.decrementRefCount()
        }
    }

    @Test fun cacheShrinksAgainForSmallViewport() {
        val cache = ViewportTileCache()
        val source = MapSource.CYCLOSM.tiles()
        val large = (0 until 150).map { DownloadJob(org.mapsforge.core.model.Tile(it, 0, 16, 256), source) }.toSet()
        cache.setWorkingSet(large)
        assertTrue(cache.capacity >= large.size)
        cache.setWorkingSet(large.take(12).toSet())
        assertEquals(16, cache.capacity)
        cache.destroy()
    }
}
