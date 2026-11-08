package de.bcycle.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import android.util.LruCache
import java.io.*
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

/** Theme-neutral, normalized geometry. Neither FIT decoding nor terrain work belongs to a row draw. */
internal data class RouteThumbnailGeometry(val route:FloatArray,val starts:BooleanArray,val contours:List<FloatArray>)
internal object RouteThumbnails {
    internal data class Bounds(val left:Double,val top:Double,val span:Double)
    private data class Cached(val geometry:RouteThumbnailGeometry,val created:Long,val complete:Boolean)
    private data class Job(val callbacks:MutableList<(RouteThumbnailGeometry)->Unit>,var warm:Boolean)
    private val lock=Any()
    private val worker=Executors.newSingleThreadExecutor { Thread(it,"B-Cycle-Thumbnails") }
    private val main=Handler(Looper.getMainLooper())
    private val jobs=mutableMapOf<String,Job>()
    private val memory=object:LruCache<String,Cached>(4096) {
        override fun sizeOf(key:String,value:Cached)=(value.geometry.route.size*4+value.geometry.contours.sumOf { it.size*4 })/1024+1
    }
    private fun x(lon:Double)=(lon+180)/360
    private fun y(lat:Double)=(1-asinh(tan(Math.toRadians(lat.coerceIn(-85.0,85.0))))/PI)/2
    private fun unwrapped(lon:Double,origin:Double)=origin+((x(lon)-origin+.5).mod(1.0)-.5)
    internal fun bounds(points:List<MapPoint>):Bounds {
        val origin=x(points.first().longitude)
        val xs=points.map { unwrapped(it.longitude,origin) };val ys=points.map { y(it.latitude) }
        val span=(maxOf(xs.max()-xs.min(),ys.max()-ys.min())*1.25).coerceAtLeast(1e-5)
        return Bounds((xs.min()+xs.max()-span)/2,(ys.min()+ys.max()-span)/2,span)
    }
    fun outline(points:List<MapPoint>):RouteThumbnailGeometry {
        if(points.size<2) return RouteThumbnailGeometry(FloatArray(0),BooleanArray(0),emptyList())
        val box=bounds(points);val origin=x(points.first().longitude)
        return RouteThumbnailGeometry(points.flatMap { listOf(((unwrapped(it.longitude,origin)-box.left)/box.span).toFloat(),((y(it.latitude)-box.top)/box.span).toFloat()) }.toFloatArray(),BooleanArray(points.size) { it==0 || points[it].segmentStart },emptyList())
    }
    internal fun terrain(points:List<MapPoint>,sample:(Double,Double)->Float):RouteThumbnailGeometry {
        val outline=outline(points);if(points.size<2) return outline
        val box=bounds(points);val size=97
        val grid=FloatArray(size*size) { i -> sample(box.left+box.span*(i%size)/(size-1),box.top+box.span*(i/size)/(size-1)) }
        val valid=grid.filter { it.isFinite() };if(valid.size!=grid.size) return outline
        val step=listOf(5,10,20,50,100,200,500,1000,2000,5000).first { it>=(valid.max()-valid.min())/8 }
        val lines=ArrayList<FloatArray>()
        TerrainContours.trace(grid,size,step) { line ->
            val p=line.points
            val length=(2 until p.size step 2).sumOf { hypot((p[it]-p[it-2]).toDouble(),(p[it+1]-p[it-1]).toDouble()) }
            if(length>=size*.08) lines+=p
        }
        return outline.copy(contours=lines.sortedByDescending { it.size }.take(24).map { line ->
            val n=line.size/2;val count=minOf(n,192)
            FloatArray(count*2) { i -> line[((i/2).toLong()*(n-1)/(count-1)).toInt()*2+i%2]/(size-1) }
        })
    }
    private fun key(points:List<MapPoint>):String {
        val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use { out -> points.forEach { out.writeDouble(it.latitude);out.writeDouble(it.longitude);out.writeBoolean(it.segmentStart) } }
        return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    fun prepare(c:Context,points:List<MapPoint>) { request(c,points,warm=true) {} }
    fun request(c:Context,points:List<MapPoint>,warm:Boolean=false,done:(RouteThumbnailGeometry)->Unit):()->Unit {
        if(points.size<2) return {}
        val app=c.applicationContext;val key=key(points);val full=app.cacheDir.absolutePath+key
        val cancelled=AtomicBoolean(false)
        val callback:(RouteThumbnailGeometry)->Unit={ if(!cancelled.get()) done(it) }
        synchronized(lock) {
            memory.get(full)?.takeIf { it.complete || System.currentTimeMillis()-it.created<TimeUnit.MINUTES.toMillis(5) }?.let {
                callback(it.geometry);return { cancelled.set(true) }
            }
            jobs[full]?.let { it.callbacks+=callback;it.warm=it.warm || warm;return { cancelled.set(true) } }
            if(jobs.size>=32) return {}
            jobs[full]=Job(mutableListOf(callback),warm)
        }
        worker.execute {
            fun active()=synchronized(lock) { jobs[full]?.let { it.warm || it.callbacks.isNotEmpty() }==true }
            if(!active()) { synchronized(lock) { jobs.remove(full) };return@execute }
            val directory=File(app.cacheDir,"route-thumbnails-v1");val file=File(directory,"$key.bin")
            val persisted=runCatching { read(file) }.getOrNull()
            var complete=persisted!=null
            val geometry=persisted ?: runCatching {
                val network=app.getSystemService(ConnectivityManager::class.java)
                val online=network?.getNetworkCapabilities(network.activeNetwork)?.let { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }==true
                val box=bounds(points);val z=floor(log2(1/box.span)).toInt().coerceIn(0,14);val count=1 shl z
                val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(12)
                val http=MapterhornTiles.client.newBuilder().connectTimeout(4,TimeUnit.SECONDS).readTimeout(5,TimeUnit.SECONDS).callTimeout(8,TimeUnit.SECONDS).build()
                MapterhornTiles(File(app.cacheDir,"mapterhorn-v1"),http,maxDownloads=8).use { tiles ->
                    val loaded=HashMap<Pair<Int,Int>,MapterhornTiles.Dem?>()
                    var valid=true
                    val result=terrain(points) { px,py ->
                        check(System.nanoTime()<deadline)
                        val tx=floor(px*count).toInt();val ty=floor(py*count).toInt().coerceIn(0,count-1)
                        val pair=tx to ty
                        if(!loaded.containsKey(pair)) {
                            check(active())
                            loaded[pair]=tiles.load(z,tx,ty,allowNetwork=online)
                                ?: if(z>=2) tiles.load(z-2,Math.floorDiv(tx,4),Math.floorDiv(ty,4),allowNetwork=online) else null
                        }
                        val dem=loaded[pair]
                        val ix=((px*count-floor(px*count))*MapterhornTiles.SIZE).toInt().coerceIn(0,MapterhornTiles.SIZE-1)
                        val iy=((py*count-ty)*MapterhornTiles.SIZE).toInt().coerceIn(0,MapterhornTiles.SIZE-1)
                        (dem?.at(z,tx,ty,ix,iy) ?: Float.NaN).also { if(!it.isFinite()) valid=false }
                    }
                    complete=valid;result
                }
            }.getOrElse { outline(points) }
            // Flat terrain legitimately has no lines. Only persist a complete DEM result;
            // a route-only fallback is retried later when coverage/connectivity returns.
            if(persisted==null && complete) runCatching {
                directory.mkdirs();val atomic=AtomicFile(file);val stream=atomic.startWrite()
                try { val out=DataOutputStream(stream);write(out,geometry);out.flush();atomic.finishWrite(stream) }
                catch(e:Exception) { atomic.failWrite(stream);throw e }
                val files=directory.listFiles { f -> f.extension=="bin" }.orEmpty().sortedBy { it.lastModified() }
                var size=files.sumOf { it.length() }
                for(old in files) { if(size<=16*1024*1024) break;size-=old.length();old.delete() }
            }
            synchronized(lock) { if(complete || active()) memory.put(full,Cached(geometry,System.currentTimeMillis(),complete)) }
            val callbacks=synchronized(lock) { jobs.remove(full)?.callbacks.orEmpty().toList() }
            main.post { callbacks.forEach { it(geometry) } }
        }
        return { cancelled.set(true);synchronized(lock) { jobs[full]?.callbacks?.remove(callback) };Unit }
    }
    private fun write(out:DataOutputStream,g:RouteThumbnailGeometry) {
        out.writeInt(1);out.writeInt(g.starts.size);g.route.forEach(out::writeFloat);g.starts.forEach(out::writeBoolean)
        out.writeInt(g.contours.size);g.contours.forEach { line -> out.writeInt(line.size);line.forEach(out::writeFloat) }
    }
    private fun read(file:File):RouteThumbnailGeometry {
        require(file.length() in 1..128*1024)
        DataInputStream(file.inputStream().buffered()).use { input ->
            require(input.readInt()==1);val count=input.readInt();require(count in 2..128)
            val route=FloatArray(count*2) { input.readFloat().also { require(it.isFinite()) } }
            val starts=BooleanArray(count) { input.readBoolean() }
            val lines=input.readInt();require(lines in 0..24)
            val contours=List(lines) { val n=input.readInt();require(n in 4..384 && n%2==0);FloatArray(n) { input.readFloat().also { require(it.isFinite()) } } }
            return RouteThumbnailGeometry(route,starts,contours)
        }
    }
}
