package de.bcycle.app

import android.content.Context
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.*

internal fun interface ElevationSource { fun height(latitude: Double, longitude: Double): Double }

/** On-demand DEM only. Never uploads the FIT; bounded download/cache, one tile request at a time. */
internal class MapterhornElevations(
    private val directory: File, private val cancelled: () -> Boolean = { false },
    private val client: OkHttpClient = MapterhornTiles.client, private val endpoint: String = MapterhornTiles.ENDPOINT,
) : ElevationSource, AutoCloseable {
    constructor(context: Context,cancelled: () -> Boolean = { false }) : this(File(context.cacheDir,"mapterhorn-v1"),cancelled)
    private val tiles=MapterhornTiles(directory,client,endpoint,maxDownloads=256)
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
            val tile=tiles.load(z,wx/512,wy/512,stopped=cancelled,checkActive=::checkStopped) ?: return null
            val value=tile.heights[(wy%512)*512+wx%512].toDouble()
            return value.takeIf { it in -500.0..10000.0 }
        }
        val a=at(0,0) ?: return null;val b=at(1,0) ?: return null
        val c=at(0,1) ?: return null;val d=at(1,1) ?: return null
        return (a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy
    }
    private fun checkStopped() { if(cancelled() || Thread.currentThread().isInterrupted || System.nanoTime()>deadline) throw IOException("DEM cancelled or timed out") }
    override fun close() { tiles.close() }
    companion object {
        internal fun decode(color:Int)=MapterhornTiles.elevation(color)
    }
}
