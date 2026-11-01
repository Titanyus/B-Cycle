package de.bcycle.app

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
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
    private var client:OkHttpClient?=null
    @Synchronized fun source(context:Context,provider:MapSource,key:String):BitmapTileSource {
        val http=client ?: OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir,"vtm-raster-http"),64L*1024*1024))
            .connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS)
            .build().also { client=it }
        return provider.tiles(key).apply {
            setHttpEngine(HttpEngine.Factory { source -> OkHttpEngine(http,source) })
        }
    }
}
