package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.layers.tile.MapTile
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.map.Map
import org.oscim.tiling.ITileDataSink
import org.oscim.tiling.ITileDataSource
import org.oscim.tiling.QueryResult
import org.oscim.tiling.TileSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Prepared relief, one image worker; VTM owns the bounded visible-tile/GL texture cache. */
internal class VtmHillshadeLayer(map:Map,source:HillshadeSource=HillshadeSource()):BitmapTileLayer(map,source,32) {
    override fun getNumLoaders()=1
    fun resume() { isEnabled=true;resumeLoaders() }
    fun pause() { isEnabled=false;pauseLoaders(false) }
}

/** Online display only: no disk cache, offline export, bulk fetching or elevation calculation. */
internal class HillshadeSource(private val http:OkHttpClient=client,private val endpoint:String=ENDPOINT):TileSource(6,16) {
    private val data=Data()
    override fun open()=OpenResult.SUCCESS
    override fun close()=data.dispose()
    override fun getDataSource():ITileDataSource=data
    private inner class Data:ITileDataSource {
        private val generation=AtomicInteger()
        @Volatile private var disposed=false
        @Volatile private var call:Call?=null
        override fun cancel() { generation.incrementAndGet();call?.cancel() }
        override fun dispose() { disposed=true;cancel() }
        override fun query(tile:MapTile,sink:ITileDataSink) {
            val token=generation.get()
            fun active() { if(disposed || token!=generation.get() || Thread.currentThread().isInterrupted) throw IOException("Hillshade cancelled") }
            var bitmap:Bitmap?=null
            try {
                active()
                val z=tile.zoomLevel.toInt();if(z !in 6..16) throw IOException("Invalid hillshade zoom")
                val count=1 shl z
                if(tile.tileY !in 0 until count) throw IOException("Invalid hillshade row")
                val x=Math.floorMod(tile.tileX,count)
                // ArcGIS uses Z/Y/X, unlike the DEM source.
                val request=Request.Builder().url("${endpoint.trimEnd('/')}/$z/${tile.tileY}/$x")
                    .header("User-Agent","B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)").build()
                val next=http.newCall(request);call=next;active()
                val bytes=next.execute().use { response ->
                    active()
                    if(!response.isSuccessful) throw IOException("Hillshade HTTP ${response.code}")
                    val body=response.body ?: throw IOException("Missing hillshade")
                    body.byteStream().use { it.readNBytes(MAX_BYTES+1) }.also { if(it.size>MAX_BYTES) throw IOException("Hillshade too large") }
                }
                active();bitmap=decode(bytes);active()
                val image=AndroidBitmap(bitmap);bitmap=null // sink owns/recycles it from here
                sink.setTileImage(image)
                sink.completed(QueryResult.SUCCESS)
            } catch(_:IOException) { sink.completed(QueryResult.FAILED) }
            finally { bitmap?.recycle();call=null }
        }
    }
    companion object {
        const val ENDPOINT="https://server.arcgisonline.com/arcgis/rest/services/Elevation/World_Hillshade/MapServer/tile"
        private const val SIZE=256
        private const val MAX_BYTES=1024*1024
        // Deliberately independent of the map/DEM HTTP disk caches.
        private val client=OkHttpClient.Builder().cache(null).followRedirects(false).followSslRedirects(false)
            .connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(20,TimeUnit.SECONDS).build()
        internal fun shadow(color:Int):Int {
            val luminance=((color ushr 16 and 255)*77+(color ushr 8 and 255)*150+(color and 255)*29+128) shr 8
            // Neutral black shadows: white becomes transparent, Bright colours never whiten.
            val alpha=((255-luminance)*.6f).toInt().coerceAtMost(90)*(color ushr 24)/255
            return alpha shl 24
        }
        internal fun decode(bytes:ByteArray):Bitmap {
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            if(bounds.outWidth!=SIZE || bounds.outHeight!=SIZE) throw IOException("Invalid hillshade dimensions")
            val source=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply { inScaled=false;inPreferredConfig=Bitmap.Config.ARGB_8888 }) ?: throw IOException("Invalid hillshade image")
            return try {
                val pixels=IntArray(SIZE*SIZE);source.getPixels(pixels,0,SIZE,0,0,SIZE,SIZE)
                for(i in pixels.indices) pixels[i]=shadow(pixels[i])
                Bitmap.createBitmap(pixels,SIZE,SIZE,Bitmap.Config.ARGB_8888)
            } finally { source.recycle() }
        }
    }
}
