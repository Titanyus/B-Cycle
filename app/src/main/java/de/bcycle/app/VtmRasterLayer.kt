package de.bcycle.app

import android.content.Context
import okhttp3.Cache
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.oscim.tiling.source.mvt.OpenMapTilesMvtTileSource
import java.io.IOException
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.tiling.source.HttpEngine
import org.oscim.tiling.source.OkHttpEngine
import org.oscim.tiling.source.bitmap.BitmapTileSource
import java.io.File
import java.util.concurrent.TimeUnit

/** Same online raster providers, bounded shared HTTP cache, at most two tile loaders per view. */
internal class VtmRasterLayer(map:org.oscim.map.Map,source:BitmapTileSource):BitmapTileLayer(map,source,96) {
    override fun getNumLoaders()=2
    fun resume() { isEnabled=true;resumeLoaders();map().updateMap(true) }
    fun pause() { isEnabled=false;pauseLoaders(false) }
}
internal object VtmTileHttp {
    private val vectorLocks=Array(32) { Any() }
    private var client:OkHttpClient?=null
    @Synchronized internal fun client(context:Context):OkHttpClient {
        return client ?: OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir,"vtm-raster-http"),64L*1024*1024))
            .connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS)
            .build().also { client=it }
    }
    fun source(context:Context,provider:MapSource,key:String):BitmapTileSource {
        val http=client(context)
        return provider.tiles(key).apply {
            setHttpEngine(HttpEngine.Factory { source -> OkHttpEngine(http,source) })
        }
    }
    /** Metadata and tile downloads run on VTM loader threads, never during view creation. */
    fun vector(context:Context,sharePasses:Boolean=false):OpenMapTilesMvtTileSource {
        val http=client(context)
        val vectorHttp=http.newBuilder().addInterceptor { chain ->
            val parts=chain.request().url.encodedPath.removePrefix("/bcycle-vector/").removeSuffix(".pbf").split("/")
            if(parts.size!=3 || parts.any { it.toIntOrNull()==null }) throw IOException("Invalid vector tile")
            val url=OpenFreeMapTiles.template(http).replace("{z}",parts[0]).replace("{x}",parts[1]).replace("{y}",parts[2])
            if(!sharePasses) return@addInterceptor chain.proceed(chain.request().newBuilder().url(url).build())
            synchronized(vectorLocks[(url.hashCode() and Int.MAX_VALUE)%vectorLocks.size]) {
                // Consume before releasing the lock so the shared disk cache is committed
                // before a second pass requests this same tile.
                chain.proceed(chain.request().newBuilder().url(url).build()).use { response ->
                    val body=response.body ?: throw IOException("Missing vector tile")
                    val bytes=body.byteStream().use { it.readNBytes(8*1024*1024+1) }
                    if(bytes.size>8*1024*1024) throw IOException("Vector tile too large")
                    response.newBuilder().body(bytes.toResponseBody(body.contentType())).build()
                }
            }
        }.build()
        return VtmClippedVectorSource(OpenMapTilesMvtTileSource.builder().url("https://tiles.openfreemap.org")
            .tilePath("/bcycle-vector/{Z}/{X}/{Y}.pbf").locale("de").zoomMin(0).zoomMax(MapSource.VECTOR.maximumZoom).overZoom(14))
            .apply {
                setHttpRequestHeaders(mapOf("User-Agent" to "B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)"))
                setHttpEngine(HttpEngine.Factory { source -> OkHttpEngine(vectorHttp,source) })
            }
    }
}

/** OpenFreeMap rotates dated tile directories; resolve TileJSON rather than pinning one release. */
internal object OpenFreeMapTiles {
    private var cached:String?=null
    private var expires=0L
    @Synchronized fun template(http:OkHttpClient):String {
        val now=android.os.SystemClock.elapsedRealtime()
        cached?.let { if(now<expires) return it }
        val request=Request.Builder().url("https://tiles.openfreemap.org/planet")
            .header("User-Agent","B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)").build()
        fun read(request:Request):String = http.newCall(request).execute().use { response ->
            if(!response.isSuccessful) throw IOException("Vector map source HTTP ${response.code}")
            val json=response.body?.string() ?: throw IOException("Missing vector map source")
            try { validate(JSONObject(json).getJSONArray("tiles").getString(0)) }
            catch(e:Exception) { throw IOException("Invalid vector map source",e) }
        }
        val value=try { read(request) } catch(e:IOException) {
            cached ?: read(request.newBuilder().cacheControl(CacheControl.Builder().onlyIfCached().maxStale(7,TimeUnit.DAYS).build()).build())
        }
        cached=value;expires=now+TimeUnit.HOURS.toMillis(1)
        return value
    }
    internal fun validate(template:String):String {
        require(template.contains("{z}") && template.contains("{x}") && template.contains("{y}"))
        val url=template.replace("{z}","0").replace("{x}","0").replace("{y}","0").toHttpUrl()
        require(url.isHttps && url.host=="tiles.openfreemap.org" && url.port==443 && url.username.isEmpty() && url.password.isEmpty())
        require(url.encodedPath.startsWith("/planet/") && url.encodedPath.endsWith(".pbf") && url.query==null && url.fragment==null)
        return template
    }
}
