package de.bcycle.app

import android.content.Context
import android.util.LruCache
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.oscim.core.MapElement
import org.oscim.core.Tile
import org.oscim.layers.tile.MapTile
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.map.Map
import org.oscim.theme.ThemeFile
import org.oscim.theme.ThemeLoader
import org.oscim.theme.internal.VtmThemes
import org.oscim.tiling.*
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** Optional real route geometries, including overview data; no raster recolouring or detail-tile fanout. */
internal class VtmCyclingRoutesLayer(context:Context,map:Map,style:MapStyleOverrides):VectorTileLayer(map,CyclingRoutesSource(VtmTileHttp.client(context))) {
    override fun getNumLoaders()=1
    init {
        setTheme(VtmLiveTheme(ThemeLoader.load(object:ThemeFile by VtmThemes.OPENMAPTILES {
            override fun getRenderThemeAsStream():java.io.InputStream {
                val rules=listOf(.45f,.55f,.7f,.9f,1.2f,1.8f,2.5f).mapIndexed { index,width ->
                    val zoom=index+6
                    "<m e=\"way\" zoom-min=\"$zoom\" zoom-max=\"${if(zoom==12) 24 else zoom}\"><line id=\"bcycle-cycle-route\" stroke=\"#d9aa2d96\" width=\"$width\" fix=\"true\" cap=\"round\" /></m>"
                }.joinToString("\n")
                return MapStyleXml.apply("<rendertheme xmlns=\"http://opensciencemap.org/rendertheme\" version=\"1\" map-background=\"#00000000\">$rules</rendertheme>",style.compiledStyles()).byteInputStream()
            }
        }) as org.oscim.theme.RenderTheme,style,allowBackground=false))
        pause()
    }
    fun applyColors(style:MapStyleOverrides) { (theme as VtmLiveTheme).applyColors(style) }
    fun resume() { isEnabled=true;resumeLoaders() }
    fun pause() { isEnabled=false;pauseLoaders(false) }
}

internal class CyclingRoutesSource(private val http:OkHttpClient):TileSource(6,12) {
    companion object {
        private const val MERCATOR=20037508.342789244
        private const val BASE="https://cycling.waymarkedtrails.org/api/v1/"
        private val cache=object:LruCache<String,List<FloatArray>>(8*1024*1024) {
            override fun sizeOf(key:String,value:List<FloatArray>)=maxOf(4096,value.sumOf { it.size*4 })
        }
        /** Waymarked Trails GeoJSON is EPSG:3857, not lon/lat. Preserve separate line parts. */
        fun decode(json:JSONObject,tile:MapTile):List<FloatArray> {
            val result=ArrayList<FloatArray>();val seen=HashSet<List<Float>>()
            val features=json.getJSONArray("features");var points=0
            fun line(coordinates:org.json.JSONArray) {
                if(coordinates.length()<2) return
                points+=coordinates.length();require(points<=250000)
                val xy=FloatArray(coordinates.length()*2)
                for(i in 0 until coordinates.length()) {
                    val p=coordinates.getJSONArray(i);val x=p.getDouble(0);val y=p.getDouble(1)
                    require(x.isFinite() && y.isFinite())
                    xy[i*2]=(((x+MERCATOR)/(2*MERCATOR)*(1 shl tile.zoomLevel.toInt())-tile.tileX)*Tile.SIZE).toFloat()
                    xy[i*2+1]=(((MERCATOR-y)/(2*MERCATOR)*(1 shl tile.zoomLevel.toInt())-tile.tileY)*Tile.SIZE).toFloat()
                }
                val key=xy.toList();val reverse=xy.indices.step(2).reversed().flatMap { listOf(xy[it],xy[it+1]) }
                if(key !in seen && reverse !in seen) { seen.add(key);result.add(xy) }
            }
            for(i in 0 until features.length()) {
                val g=features.getJSONObject(i).optJSONObject("geometry") ?: continue
                when(g.optString("type")) {
                    "LineString"->line(g.getJSONArray("coordinates"))
                    "MultiLineString"->{ val rows=g.getJSONArray("coordinates");for(j in 0 until rows.length()) line(rows.getJSONArray(j)) }
                }
            }
            return result
        }
    }
    override fun open()=OpenResult.SUCCESS
    override fun close()=Unit
    override fun getDataSource():ITileDataSource=object:ITileDataSource {
        private val generation=AtomicInteger()
        @Volatile private var call:Call?=null
        @Volatile private var disposed=false
        override fun cancel() { generation.incrementAndGet();call?.cancel() }
        override fun dispose() { disposed=true;cancel() }
        private fun check(token:Int) { if(disposed || generation.get()!=token) throw IOException("Cancelled cycling routes") }
        private fun get(path:String,token:Int):JSONObject {
            check(token)
            val request=Request.Builder().url(BASE+path).header("User-Agent","B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)").build()
            val pending=http.newCall(request);call=pending
            return pending.execute().use { response ->
                check(token);if(!response.isSuccessful) throw IOException("Cycling routes HTTP ${response.code}")
                val bytes=response.body?.byteStream()?.use { it.readNBytes(8*1024*1024+1) } ?: throw IOException("Empty cycling routes")
                if(bytes.size>8*1024*1024) throw IOException("Cycling routes too large")
                JSONObject(bytes.toString(Charsets.UTF_8))
            }
        }
        override fun query(tile:MapTile,sink:ITileDataSink) {
            val token=generation.get();val key="${tile.zoomLevel}/${tile.tileX}/${tile.tileY}"
            try {
                check(token)
                val lines=cache.get(key) ?: run {
                    val data=if(tile.zoomLevel>=12) get("tiles/12/${tile.tileX}/${tile.tileY}.json",token) else {
                        val width=2*MERCATOR/(1 shl tile.zoomLevel.toInt())
                        val left=tile.tileX*width-MERCATOR;val top=MERCATOR-tile.tileY*width
                        val bbox=listOf(left,top-width,left+width,top).joinToString(",")
                        val routes=get("list/by_area?bbox=$bbox&limit=100",token).getJSONArray("results")
                        val ids=(0 until routes.length()).map { routes.getJSONObject(it).getLong("id") }.distinct().joinToString(",")
                        if(ids.isEmpty()) JSONObject("{\"features\":[]}") else get("list/segments?bbox=$bbox&relations=$ids",token)
                    }
                    decode(data,tile).also { check(token);cache.put(key,it) }
                }
                for(line in lines) {
                    check(token);val element=MapElement();element.startLine()
                    for(i in line.indices step 2) element.addPoint(line[i],line[i+1])
                    sink.process(element)
                }
                sink.completed(QueryResult.SUCCESS)
            } catch(_:Exception) { sink.completed(QueryResult.FAILED) }
            finally { call=null }
        }
    }
}
