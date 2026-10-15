package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Color
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapterhornElevationsTest {
    private fun image(height:Double):ByteArray {
        val n=height+32768;val r=kotlin.math.floor(n/256).toInt();val g=kotlin.math.floor(n-r*256).toInt();val b=kotlin.math.round((n-kotlin.math.floor(n))*256).toInt()
        val bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(r,g,b)) }
        val out=ByteArrayOutputStream();assertTrue(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS,100,out));bitmap.recycle();return out.toByteArray()
    }
    @Test fun terrariumDecodingAndRealWebpCacheWorkWithoutUploadingTrackData() {
        assertEquals(518.25,MapterhornElevations.decode(Color.rgb(130,6,64)),0.0)
        val server=MockWebServer();server.start()
        try {
            val cache=java.nio.file.Files.createTempDirectory("dem-cache").toFile()
            server.enqueue(MockResponse().setBody(Buffer().write(image(518.25))))
            MapterhornElevations(cache,endpoint=server.url("").toString().trimEnd('/')).use {
                assertEquals(518.25,it.height(48.2,10.3),.0001)
                assertEquals(518.25,it.height(48.200001,10.300001),.0001)
            }
            assertEquals(1,server.requestCount)
            val request=server.takeRequest();assertEquals("GET",request.method);assertTrue(request.path!!.matches(Regex("/14/[0-9]+/[0-9]+.webp")))
            assertEquals(0,request.bodySize)
            MapterhornElevations(cache,endpoint=server.url("").toString().trimEnd('/')).use { assertEquals(518.25,it.height(48.2,10.3),.0001) }
            assertEquals(1,server.requestCount)
        } finally { server.shutdown() }
    }
    @Test fun tileEdgesUseFourNeighborsAndEachIsFetchedOnlyOnce() {
        val server=MockWebServer();server.start()
        try {
            repeat(4) { server.enqueue(MockResponse().setBody(Buffer().write(image(120.5)))) }
            MapterhornElevations(java.nio.file.Files.createTempDirectory("dem-edges").toFile(),endpoint=server.url("").toString().trimEnd('/')).use {
                repeat(3) { _ -> assertEquals(120.5,it.height(0.0,0.0),.0001) }
            }
            assertEquals(4,server.requestCount)
        } finally { server.shutdown() }
    }
    @Test fun unavailableDetailedTileFallsBackAndNetworkOrImageFailureIsNotZeroHeight() {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(404));server.enqueue(MockResponse().setBody(Buffer().write(image(432.0))))
            MapterhornElevations(java.nio.file.Files.createTempDirectory("dem-fallback").toFile(),endpoint=server.url("").toString().trimEnd('/')).use { assertEquals(432.0,it.height(48.2,10.3),.001) }
            assertTrue(server.takeRequest().path!!.startsWith("/14/"));assertTrue(server.takeRequest().path!!.startsWith("/12/"))
            server.enqueue(MockResponse().setResponseCode(503))
            MapterhornElevations(java.nio.file.Files.createTempDirectory("dem-fail").toFile(),endpoint=server.url("").toString().trimEnd('/')).use { provider -> assertThrows(IOException::class.java) { provider.height(48.2,10.3) } }
            val before=server.requestCount
            MapterhornElevations(java.nio.file.Files.createTempDirectory("dem-cancel").toFile(),cancelled={true},endpoint=server.url("").toString().trimEnd('/')).use { provider -> assertThrows(IOException::class.java) { provider.height(48.2,10.3) } }
            assertEquals(before,server.requestCount)
        } finally { server.shutdown() }
    }
}
