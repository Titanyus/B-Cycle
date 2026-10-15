package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BicycleHeaderShadowTest {
    @Test fun shadowFadesOnlyDownwardsWithoutChangingGeometry() {
        val bitmap=Bitmap.createBitmap(30,30,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        BicycleHeaderShadow(RuntimeEnvironment.getApplication()).draw(Canvas(bitmap),0f,10f,30f)
        assertEquals(Color.WHITE,bitmap.getPixel(15,9))
        assertTrue(Color.red(bitmap.getPixel(15,10))<Color.red(bitmap.getPixel(15,13)))
        assertTrue(Color.red(bitmap.getPixel(15,13))<255)
        assertTrue(Color.red(bitmap.getPixel(15,20))<255)
        assertEquals(Color.WHITE,bitmap.getPixel(15,22));bitmap.recycle()
    }
}
