package de.bcycle.app

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.oscim.core.MapPosition
import org.oscim.event.Event
import org.oscim.core.MapElement
import org.oscim.core.Tag
import org.oscim.core.Tile
import org.oscim.layers.tile.MapTile
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.map.Map
import org.oscim.theme.ThemeFile
import org.oscim.theme.ThemeLoader
import org.oscim.theme.internal.VtmThemes
import org.oscim.tiling.ITileDataSink
import org.oscim.tiling.ITileDataSource
import org.oscim.tiling.QueryResult
import org.oscim.tiling.TileSource
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** Optional native vector overlay. DEM decoding/contours only run on its one tile worker. */
internal class VtmContourLayer(context:Context,map:Map,style:MapStyleOverrides=MapStyleOverrides()):VectorTileLayer(map,ContourSource(VtmTileHttp.client(context))) {
    override fun getNumLoaders()=1
    init { setTheme(VtmLiveTheme(ThemeLoader.load(ContourTheme(style.compiledStyles())) as org.oscim.theme.RenderTheme,style,allowBackground=false));pause() }
    fun applyColors(style:MapStyleOverrides) { (theme as VtmLiveTheme).applyColors(style) }
    override fun onMapEvent(event:Event,position:MapPosition) {
        tileRenderer().setBitmapAlpha(if(position.zoomLevel>=10) 1f else 0f)
        super.onMapEvent(event,position)
    }
    fun resume() { isEnabled=true;resumeLoaders() }
    fun pause() { isEnabled=false;pauseLoaders(false) }
    private class ContourTheme(private val style:MapStyleOverrides):ThemeFile by VtmThemes.OPENMAPTILES {
        override fun getRenderThemeAsStream()=MapStyleXml.apply("""
            <rendertheme xmlns="http://opensciencemap.org/rendertheme" version="1" map-background="#00000000">
              <m e="way" k="contour" v="minor"><line id="contour-minor" stroke="#668b7355" width="0.6" fix="true" /></m>
              <m e="way" k="contour" v="major"><line id="contour-major" stroke="#998b7355" width="1" fix="true" /></m>
            </rendertheme>
        """.trimIndent(),style).byteInputStream()
    }
}

/** Terrain Tiles Terrarium RGB, hosted publicly on AWS. Limit detail to native DEM zoom 13. */
internal class ContourSource(private val http:OkHttpClient):TileSource(10,13) {
    private val cache=LruCache<String,FloatArray>(16) // 4 MiB maximum decoded DEMs, shared by adjacent tiles.
    override fun open()=OpenResult.SUCCESS
    override fun close() { cache.evictAll() }
    override fun getDataSource():ITileDataSource=object:ITileDataSource {
        private val generation=AtomicInteger()
        @Volatile private var call:Call?=null
        @Volatile private var disposed=false
        override fun cancel() { generation.incrementAndGet();call?.cancel() }
        override fun dispose() { disposed=true;cancel() }
        private fun dem(z:Int,x:Int,y:Int,token:Int):FloatArray {
            val count=1 shl z;val key="$z/${Math.floorMod(x,count)}/${y.coerceIn(0,count-1)}"
            cache.get(key)?.let { return it }
            if(disposed || generation.get()!=token) throw IOException("Cancelled terrain tile")
            val request=Request.Builder().url("https://s3.amazonaws.com/elevation-tiles-prod/terrarium/$key.png")
                .header("User-Agent","B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)").build()
            val requestCall=http.newCall(request);call=requestCall
            if(disposed || generation.get()!=token) requestCall.cancel()
            val bytes=try { requestCall.execute().use { response ->
                if(!response.isSuccessful) throw IOException("Terrain HTTP ${response.code}")
                val body=response.body ?: throw IOException("Missing terrain tile")
                body.byteStream().use { it.readNBytes(1024*1024+1) }.also { if(it.size>1024*1024) throw IOException("Terrain tile too large") }
            } } finally { call=null }
            val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size) ?: throw IOException("Invalid terrain PNG")
            try {
                if(bitmap.width!=256 || bitmap.height!=256) throw IOException("Invalid terrain size")
                val pixels=IntArray(256*256);bitmap.getPixels(pixels,0,256,0,0,256,256)
                return FloatArray(pixels.size) { TerrainContours.elevation(pixels[it]) }.also { cache.put(key,it) }
            } finally { bitmap.recycle() }
        }
        override fun query(tile:MapTile,sink:ITileDataSink) {
            val token=generation.get()
            fun cancelled()=disposed || generation.get()!=token
            try {
                val z=tile.zoomLevel.toInt();val size=257
                // One shared sample on the east/south edge makes adjacent contour endpoints identical.
                val center=dem(z,tile.tileX,tile.tileY,token)
                val east=dem(z,tile.tileX+1,tile.tileY,token)
                val south=dem(z,tile.tileX,tile.tileY+1,token)
                val corner=dem(z,tile.tileX+1,tile.tileY+1,token)
                val grid=FloatArray(size*size) { i -> val x=i%size;val y=i/size
                    when { x==256 && y==256 -> corner[0];x==256 -> east[y*256];y==256 -> south[x];else -> center[y*256+x] }
                }
                val element=MapElement()
                TerrainContours.trace(grid,size,TerrainContours.interval(z),::cancelled) { line ->
                    element.clear();element.tags.clear();element.tags.add(Tag("contour",if(line.elevation%100==0) "major" else "minor"))
                    element.layer=5;element.startLine()
                    for(i in line.points.indices step 2) element.addPoint(line.points[i]*Tile.SIZE/256f,line.points[i+1]*Tile.SIZE/256f)
                    sink.process(element)
                }
                sink.completed(if(cancelled()) QueryResult.FAILED else QueryResult.SUCCESS)
            } catch(_:IOException) { sink.completed(QueryResult.FAILED) }
        }
    }
}
