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
    @Test fun onlyEmptyPhotoOpensPickerAndDeletingEnablesPickingAgainInBothDesigns() {
        val host=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val input=File(a.cacheDir,"photo-choice.png")
            Bitmap.createBitmap(80,40,Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.BLUE)
                input.outputStream().use { compress(Bitmap.CompressFormat.PNG,100,it) };recycle()
            }
            var chosen=0
            val view=BikePhotoView(a,themes,0,BikeType.EBIKE) { chosen++ }
            a.setContentView(view)
            fun drain() {
                repeat(2) {
                    BikePhotos.worker.submit {}.get(10,java.util.concurrent.TimeUnit.SECONDS)
                    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                }
            }
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat);BikePhotos.delete(a,0);view.refresh();drain()
                val select=view.findViewWithTag<android.view.View>("bike_photo_choose")
                val remove=view.findViewWithTag<android.view.View>("bike_photo_remove")
                assertEquals(android.view.View.VISIBLE,select.visibility)
                val before=chosen;select.performClick();assertEquals(before+1,chosen)
                BikePhotos.import(a,0,Uri.fromFile(input));view.refresh();drain()
                assertEquals(android.view.View.GONE,select.visibility)
                assertEquals(android.view.View.VISIBLE,remove.visibility)
                assertFalse(view.getChildAt(0).performClick());assertEquals(before+1,chosen)
                assertTrue(BikePhotos.file(a,0).isFile)
                remove.performClick();drain()
                assertFalse(BikePhotos.file(a,0).exists())
                assertEquals(android.view.View.VISIBLE,select.visibility)
                select.performClick();assertEquals(before+2,chosen)
            }
        } finally { host.pause().stop().destroy() }
    }
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
