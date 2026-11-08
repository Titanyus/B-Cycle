package de.bcycle.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import android.util.LruCache
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.floor

/** One DEM format, download path and bounded cache for map contours and ride correction. Worker only. */
internal class MapterhornTiles(
    directory:File,private val http:OkHttpClient=client,private val endpoint:String=ENDPOINT,
    private val maxDownloads:Int=Int.MAX_VALUE
):AutoCloseable {
    constructor(context:Context):this(File(context.cacheDir,"mapterhorn-v1"))
    private val directory=if(endpoint.trimEnd('/')==ENDPOINT) directory else File(directory,"source-"+java.security.MessageDigest.getInstance("SHA-256").digest(endpoint.toByteArray()).take(8).joinToString("") { "%02x".format(it) })
    private val absent=LruCache<String,Long>(64)
    private var downloads=0
    @Volatile private var call:Call?=null
    data class Dem(val zoom:Int,val x:Int,val y:Int,val heights:FloatArray) {
        /** Query-pixel centers mapped into a coarser parent, when detailed coverage is absent. */
        fun at(queryZoom:Int,queryX:Int,queryY:Int,pixelX:Int,pixelY:Int):Float {
            if(zoom==queryZoom) return heights[pixelY*SIZE+pixelX]
            val scale=(1 shl (queryZoom-zoom)).toDouble();val count=1 shl queryZoom
            val px=(((Math.floorMod(queryX,count)*SIZE.toDouble()+pixelX+.5)/scale)-x*SIZE-.5).coerceIn(0.0,SIZE-1.0)
            val py=(((queryY.coerceIn(0,count-1)*SIZE.toDouble()+pixelY+.5)/scale)-y*SIZE-.5).coerceIn(0.0,SIZE-1.0)
            val ix=floor(px).toInt();val iy=floor(py).toInt();val fx=px-ix;val fy=py-iy
            val a=heights[iy*SIZE+ix];val b=heights[iy*SIZE+minOf(ix+1,SIZE-1)]
            val c=heights[minOf(iy+1,SIZE-1)*SIZE+ix];val d=heights[minOf(iy+1,SIZE-1)*SIZE+minOf(ix+1,SIZE-1)]
            if(!a.isFinite() || !b.isFinite() || !c.isFinite() || !d.isFinite()) return Float.NaN
            return ((a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy).toFloat()
        }
    }
    fun load(z:Int,x:Int,y:Int,stopped:()->Boolean={false},checkActive:()->Unit={},allowNetwork:Boolean=true):Dem? {
        require(z in 0..17)
        fun active() { checkActive();if(stopped() || Thread.currentThread().isInterrupted) throw IOException("DEM cancelled") }
        active()
        val count=1 shl z;val tx=Math.floorMod(x,count);val ty=y.coerceIn(0,count-1)
        val key="$z-$tx-$ty";val memoryKey=directory.absolutePath+"/"+key
        val now=System.currentTimeMillis()
        memory.get(memoryKey)?.let { if(now-it.created<TTL) return Dem(z,tx,ty,it.heights) else memory.remove(memoryKey) }
        absent.get(key)?.let { if(now-it<TimeUnit.MINUTES.toMillis(5)) return null else absent.remove(key) }
        // Waiting for the other consumer's download remains cancellable.
        try { while(!diskLock.tryLock(100,TimeUnit.MILLISECONDS)) active() }
        catch(_:InterruptedException) { Thread.currentThread().interrupt();throw IOException("DEM cancelled") }
        try {
            active()
            memory.get(memoryKey)?.let { if(now-it.created<TTL) return Dem(z,tx,ty,it.heights) }
            if(!directory.isDirectory && !directory.mkdirs()) throw IOException("DEM cache unavailable")
            val cache=File(directory,"$key.webp")
            if(cache.isFile && now-cache.lastModified()<TTL) {
                val decoded=if(cache.length()<=MAX_BYTES) decodeImage(cache.readBytes()) else null
                if(decoded!=null) { memory.put(memoryKey,Cached(decoded,cache.lastModified()));return Dem(z,tx,ty,decoded) }
                cache.delete()
            }
            if(!allowNetwork) return null
            if(++downloads>maxDownloads) throw IOException("DEM request limit")
            val request=Request.Builder().url("${endpoint.trimEnd('/')}/$z/$tx/$ty.webp")
                .header("User-Agent","B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)").build()
            val next=http.newCall(request);call=next
            val bytes=try {
                active()
                next.execute().use { response ->
                    active()
                    if(response.code==404) { absent.put(key,now);return null }
                    if(!response.isSuccessful) throw IOException("DEM HTTP ${response.code}")
                    val body=response.body ?: throw IOException("Empty DEM")
                    body.byteStream().use { input ->
                        val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                        while(true) { active();val n=input.read(buffer);if(n<0) break
                            if(output.size()+n>MAX_BYTES) throw IOException("DEM too large")
                            output.write(buffer,0,n)
                        };output.toByteArray()
                    }
                }
            } finally { call=null }
            val decoded=decodeImage(bytes) ?: throw IOException("Invalid DEM image")
            active()
            val atomic=AtomicFile(cache);val out=atomic.startWrite()
            try { out.write(bytes);atomic.finishWrite(out) } catch(e:Exception) { atomic.failWrite(out);throw e }
            memory.put(memoryKey,Cached(decoded,now))
            val files=directory.listFiles { f->f.extension=="webp" }.orEmpty().sortedBy { it.lastModified() }
            var size=files.sumOf { it.length() }
            for(file in files) { if(size<=DISK_BYTES) break;size-=file.length();file.delete() }
            return Dem(z,tx,ty,decoded)
        } finally { diskLock.unlock() }
    }
    fun cancel() { call?.cancel() }
    override fun close() { cancel();absent.evictAll() }
    companion object {
        const val ENDPOINT="https://tiles.mapterhorn.com"
        const val SIZE=512
        private const val MAX_BYTES=2*1024*1024
        private const val DISK_BYTES=64L*1024*1024
        private val TTL=TimeUnit.DAYS.toMillis(30)
        private val diskLock=ReentrantLock()
        private data class Cached(val heights:FloatArray,val created:Long)
        private val memory=object:LruCache<String,Cached>(8*1024) {
            override fun sizeOf(key:String,value:Cached)=value.heights.size*4/1024
        }
        internal val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(20,TimeUnit.SECONDS).build()
        internal fun elevation(argb:Int)=((argb ushr 16 and 255)*256.0+(argb ushr 8 and 255)+(argb and 255)/256.0)-32768
        private fun decodeImage(bytes:ByteArray):FloatArray? {
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            if(bounds.outWidth!=SIZE || bounds.outHeight!=SIZE) return null
            val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply { inScaled=false;inPreferredConfig=Bitmap.Config.ARGB_8888 }) ?: return null
            try {
                val pixels=IntArray(SIZE*SIZE);bitmap.getPixels(pixels,0,SIZE,0,0,SIZE,SIZE)
                return FloatArray(pixels.size) { index -> val color=pixels[index];val height=elevation(color)
                    if(color ushr 24==255 && height in -12000.0..10000.0) height.toFloat() else Float.NaN
                }
            } finally { bitmap.recycle() }
        }
    }
}
