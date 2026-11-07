package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Color
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.MapElement
import org.oscim.layers.tile.MapTile
import org.oscim.layers.tile.vector.labeling.BCycleLabelLayer
import org.oscim.tiling.ITileDataSink
import org.oscim.tiling.QueryResult
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HillshadeTest {
    private fun jpeg():ByteArray {
        val bitmap=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(160,160,160)) }
        return try { ByteArrayOutputStream().also { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,100,it)) }.toByteArray() }
        finally { bitmap.recycle() }
    }
    private class Sink:ITileDataSink {
        var result:QueryResult?=null
        var pixels:IntArray?=null
        override fun process(element:MapElement) { fail("Prepared image, no geometry") }
        override fun setTileImage(bitmap:org.oscim.backend.canvas.Bitmap) {
            assertEquals(256,bitmap.width);assertEquals(256,bitmap.height)
            pixels=bitmap.pixels;bitmap.recycle()
        }
        override fun completed(query:QueryResult) { assertNull("One completion",result);result=query }
    }
    @Test fun preparedImagesBecomeNeutralTransparentShadowsAndUseArcgisCoordinates() {
        assertEquals(0,HillshadeSource.shadow(Color.WHITE));assertEquals(0,HillshadeSource.shadow(Color.TRANSPARENT))
        assertEquals(90,Color.alpha(HillshadeSource.shadow(Color.BLACK)))
        val server=MockWebServer();server.start()
        val source=HillshadeSource(endpoint=server.url("tile").toString())
        try {
            repeat(2) {
                server.enqueue(MockResponse().setHeader("Cache-Control","public,max-age=86400").setBody(Buffer().write(jpeg())))
                val sink=Sink();source.dataSource.query(MapTile(null,12,23,6),sink)
                assertEquals(QueryResult.SUCCESS,sink.result)
                assertTrue(sink.pixels!!.all { pixel -> Color.alpha(pixel) in 55..58 && (pixel and 0xffffff)==0 })
                val request=server.takeRequest();assertEquals("GET",request.method);assertEquals("/tile/6/23/12",request.path)
                assertEquals(0,request.bodySize)
            }
            assertEquals("No persistent HTTP cache, even when server allows caching",2,server.requestCount)
        } finally { source.close();server.shutdown() }
    }
    @Test fun failedOrInvalidImagesDoNotPublishAnOpaqueTileAndDisposePreventsRequests() {
        val server=MockWebServer();server.start();val source=HillshadeSource(endpoint=server.url("tile").toString())
        try {
            server.enqueue(MockResponse().setResponseCode(503));server.enqueue(MockResponse().setBody("not an image"))
            repeat(2) {
                val sink=Sink();source.dataSource.query(MapTile(null,12,23,6),sink)
                assertEquals(QueryResult.FAILED,sink.result);assertNull(sink.pixels)
            }
            source.close()
            val sink=Sink();source.dataSource.query(MapTile(null,12,23,6),sink)
            assertEquals(QueryResult.FAILED,sink.result);assertEquals(2,server.requestCount)
        } finally { source.close();server.shutdown() }
    }
    @Test fun cancellationInterruptsInFlightDownloadAndAllowsFollowingVisibleTile() {
        val server=MockWebServer();server.start();val source=HillshadeSource(endpoint=server.url("tile").toString())
        val executor=Executors.newSingleThreadExecutor()
        try {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val sink=Sink();val task=executor.submit { source.dataSource.query(MapTile(null,12,23,6),sink) }
            assertNotNull(server.takeRequest(3,TimeUnit.SECONDS));source.dataSource.cancel();task.get(3,TimeUnit.SECONDS)
            assertEquals(QueryResult.FAILED,sink.result);assertNull(sink.pixels)
            server.enqueue(MockResponse().setBody(Buffer().write(jpeg())))
            val next=Sink();source.dataSource.query(MapTile(null,13,23,6),next)
            assertEquals(QueryResult.SUCCESS,next.result)
        } finally { source.close();executor.shutdownNow();server.shutdown() }
    }
    @Test fun reliefStaysBelowContoursRoadsBuildingsLabelsAndDisappearsWhenDisabled() {
        val view=BicycleMapView(RuntimeEnvironment.getApplication());val map=view.map();map.viewport().maxZoomLevel=MapSource.VECTOR.maximumZoom
        try {
            for(contours in listOf(false,true)) {
                val options=MapVectorLayers(MapVectorLayers().visible+MapVectorLayer.HILLSHADE+if(contours) setOf(MapVectorLayer.CONTOURS) else emptySet())
                val base=VtmBaseMap(view.context,map,MapSource.VECTOR,"",false,view::queueEvent,options)
                try {
                    val layers=map.layers();val ground=layers.filterIsInstance<VtmStyledVectorLayer>().single { it.pass==VtmMapPass.GROUND }
                    val relief=layers.filterIsInstance<VtmHillshadeLayer>().single()
                    val front=layers.filterIsInstance<VtmStyledVectorLayer>().single { it.pass==VtmMapPass.FOREGROUND }
                    val labels=layers.filterIsInstance<BCycleLabelLayer>().single()
                    assertTrue(layers.indexOf(ground)<layers.indexOf(relief));assertTrue(layers.indexOf(relief)<layers.indexOf(front))
                    assertTrue(layers.indexOf(front)<layers.indexOf(labels))
                    if(contours) assertTrue(layers.indexOf(relief)<layers.indexOf(layers.filterIsInstance<VtmContourLayer>().single()))
                } finally { base.onDetach() }
                assertTrue(map.layers().none { it is VtmHillshadeLayer })
            }
            val plain=VtmBaseMap(view.context,map,MapSource.VECTOR,"",false,view::queueEvent)
            try { assertEquals(1,map.layers().filterIsInstance<VtmStyledVectorLayer>().size);assertTrue(map.layers().none { it is VtmHillshadeLayer }) }
            finally { plain.onDetach() }
        } finally { view.disposeMap() }
    }
}
