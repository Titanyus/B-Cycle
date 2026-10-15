package de.bcycle.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.*

internal fun interface ElevationSource { fun height(latitude: Double, longitude: Double): Double }

/** On-demand DEM only. Never uploads the FIT; bounded download/cache, one tile request at a time. */
internal class MapterhornElevations(
    private val directory: File, private val cancelled: () -> Boolean = { false },
    private val client: OkHttpClient = http, private val endpoint: String = "https://tiles.mapterhorn.com",
) : ElevationSource, AutoCloseable {
    constructor(context: Context,cancelled: () -> Boolean = { false }) : this(File(context.cacheDir,"mapterhorn-v1"),cancelled)
    private val memory=object : LinkedHashMap<String,Bitmap>(8,.75f,true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String,Bitmap>?)=size>8
    }
    private val absent=mutableSetOf<String>()
    private var requests=0
    private val deadline=System.nanoTime()+TimeUnit.MINUTES.toNanos(3)
    override fun height(latitude: Double,longitude: Double): Double {
        require(latitude in -85.0..85.0 && longitude in -180.0..180.0)
        checkStopped()
        return sample(latitude,longitude,14) ?: sample(latitude,longitude,12) ?: throw IOException("DEM unavailable")
    }
    private fun sample(lat: Double,lon: Double,z: Int): Double? {
        val pixels=(1 shl z)*512.0
        val px=((lon+180)/360*pixels-.5)
        val py=((1-asinh(tan(Math.toRadians(lat)))/PI)/2*pixels-.5)
        val x=floor(px).toInt(); val y=floor(py).toInt();val fx=px-x;val fy=py-y
        fun at(dx:Int,dy:Int): Double? {
            val wx=Math.floorMod(x+dx,pixels.toInt());val wy=(y+dy).coerceIn(0,pixels.toInt()-1)
            val tile=tile(z,wx/512,wy/512) ?: return null
            val color=tile.getPixel(wx%512,wy%512)
            if(android.graphics.Color.alpha(color)!=255) return null
            val value=decode(color)
            return value.takeIf { it in -500.0..10000.0 }
        }
        val a=at(0,0) ?: return null;val b=at(1,0) ?: return null
        val c=at(0,1) ?: return null;val d=at(1,1) ?: return null
        return (a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy
    }
    private fun tile(z:Int,x:Int,y:Int): Bitmap? {
        checkStopped()
        val key="$z-$x-$y"
        memory[key]?.let { return it };if(key in absent) return null
        val bitmap=synchronized(cacheLock) {
            directory.mkdirs()
            val cache=File(directory,"$key.webp")
            if(cache.isFile && System.currentTimeMillis()-cache.lastModified()<TimeUnit.DAYS.toMillis(30)) {
                val decoded=if(cache.length()<=2*1024*1024) decodeImage(cache.readBytes()) else null
                if(decoded?.width==512 && decoded.height==512) return@synchronized decoded
                cache.delete()
            }
            if(++requests>256) throw IOException("DEM request limit")
            val request=Request.Builder().url("$endpoint/$z/$x/$y.webp")
                .header("User-Agent","B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)").build()
            val bytes=client.newCall(request).execute().use { response ->
                if(response.code==404) { absent+=key;return@synchronized null }
                if(!response.isSuccessful) throw IOException("DEM HTTP ${response.code}")
                val body=response.body ?: throw IOException("Empty DEM")
                body.byteStream().use { input ->
                    val output=java.io.ByteArrayOutputStream()
                    val buffer=ByteArray(8192)
                    while(true) { checkStopped();val count=input.read(buffer);if(count<0) break
                        if(output.size()+count>2*1024*1024) throw IOException("DEM too large")
                        output.write(buffer,0,count)
                    };output.toByteArray()
                }
            }
            val decoded=decodeImage(bytes)
            if(decoded?.width!=512 || decoded.height!=512) throw IOException("Invalid DEM image")
            val atomic=AtomicFile(cache);val out=atomic.startWrite()
            try { out.write(bytes);atomic.finishWrite(out) } catch(e:Exception) { atomic.failWrite(out);throw e }
            // Disk cache is disposable and never grows without a bound.
            val files=directory.listFiles { f -> f.extension=="webp" }.orEmpty().sortedBy { it.lastModified() }
            var size=files.sumOf { it.length() }
            for(file in files) { if(size<=64L*1024*1024) break;size-=file.length();file.delete() }
            decoded
        }
        if(bitmap!=null) memory[key]=bitmap
        return bitmap
    }
    private fun decodeImage(bytes:ByteArray):Bitmap? {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        if(bounds.outWidth!=512 || bounds.outHeight!=512) return null
        return BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply { inScaled=false;inPreferredConfig=Bitmap.Config.ARGB_8888 })
    }
    private fun checkStopped() { if(cancelled() || Thread.currentThread().isInterrupted || System.nanoTime()>deadline) throw IOException("DEM cancelled or timed out") }
    override fun close() { memory.clear();absent.clear() }
    companion object {
        private val cacheLock=Any()
        private val http=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(20,TimeUnit.SECONDS).build()
        internal fun decode(color:Int)=android.graphics.Color.red(color)*256.0+android.graphics.Color.green(color)+android.graphics.Color.blue(color)/256.0-32768
    }
}
