package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mapsforge.map.android.graphics.AndroidBitmap
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapNightTest {
    @Test fun paletteKeepsTerrainWaterPathsAndLabelsDistinctAndPreservesTransparency() {
        val label = MapNightPalette.color(Color.BLACK)
        val ground = MapNightPalette.color(Color.rgb(242, 242, 242))
        val road = MapNightPalette.color(Color.WHITE)
        assertTrue(Color.red(label) > 180)
        assertTrue(Color.red(ground) < 45)
        assertTrue(Color.red(road) > Color.red(ground) + 50)
        val forest = MapNightPalette.color(Color.rgb(170, 220, 140))
        val water = MapNightPalette.color(Color.rgb(150, 200, 230))
        assertTrue(Color.green(forest) > Color.red(forest))
        assertTrue(Color.blue(water) > Color.green(water))
        val path = MapNightPalette.color(Color.rgb(220, 180, 100))
        assertTrue(Color.red(path) > Color.blue(path))
        assertEquals(123, Color.alpha(MapNightPalette.color(Color.argb(123, 150, 200, 230))))
    }
    @Test fun derivedTileIsCachedOriginalIsUntouchedAndMutationInvalidatesOnlyItsCopy() {
        AndroidGraphicFactory.createInstance(RuntimeEnvironment.getApplication())
        val raw = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val source = AndroidBitmap(raw); val cache = MapNightTiles { 16 }
        val first = cache.convert(source)
        assertSame(first, cache.convert(source)); assertEquals(Color.WHITE, raw.getPixel(1, 1))
        assertEquals(MapNightPalette.color(Color.WHITE), AndroidGraphicFactory.getBitmap(first).getPixel(1, 1))
        raw.setPixel(1, 1, Color.BLACK)
        val second = cache.convert(source)
        assertNotSame(first, second)
        assertEquals(MapNightPalette.color(Color.BLACK), AndroidGraphicFactory.getBitmap(second).getPixel(1, 1))
        cache.clear(); assertEquals(0, cache.size); assertFalse(source.isDestroyed())
        source.decrementRefCount()
    }
    @Test fun derivedTileCacheIsBoundedAndReleasesCopiesWithoutDestroyingOwnedDayTiles() {
        AndroidGraphicFactory.createInstance(RuntimeEnvironment.getApplication())
        val cache = MapNightTiles { 16 }
        val sources = List(40) { AndroidBitmap(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)) }
        sources.forEach { cache.convert(it); assertTrue(cache.size <= 16) }
        cache.clear(); assertEquals(0, cache.size)
        sources.forEach { assertFalse(it.isDestroyed()); it.decrementRefCount() }
    }
}
