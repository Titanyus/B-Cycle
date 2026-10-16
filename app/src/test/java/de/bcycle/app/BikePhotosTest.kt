package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BikePhotosTest {
    @Test fun photosArePrivateBoundedPerBikeAndFailedImportsPreserveThePreviousImage() {
        val c=RuntimeEnvironment.getApplication()
        val input=File(c.cacheDir,"chosen.png")
        Bitmap.createBitmap(2000,1000,Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLUE)
            input.outputStream().use { assertTrue(compress(Bitmap.CompressFormat.PNG,100,it)) };recycle()
        }
        BikePhotos.import(c,0,Uri.fromFile(input))
        BikePhotos.import(c,1,Uri.fromFile(input))
        input.delete() // The picker URI/file need not remain available.
        val stored=BikePhotos.file(c,0);val before=stored.readBytes()
        BitmapFactory.decodeFile(stored.path).apply { assertEquals(1200,width);assertEquals(600,height);recycle() }
        val invalid=File(c.cacheDir,"not-an-image.txt").apply { writeText("invalid") }
        assertThrows(Exception::class.java) { BikePhotos.import(c,0,Uri.fromFile(invalid)) }
        assertArrayEquals(before,stored.readBytes())
        assertTrue(c.cacheDir.listFiles()!!.none { it.name.startsWith("bike-photo-") })
        BikePhotos.delete(c,0)
        assertFalse(stored.exists());assertTrue(BikePhotos.file(c,1).exists())
    }
}
