package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
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
}
