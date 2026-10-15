package de.bcycle.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal data class WeatherTarget(val timeMs:Long,val latitude:Double,val longitude:Double)
/** Bounded display-only location sampling, separate from FIT and sensor statistics. */
internal class WeatherTargetBuffer {
    private var points=ArrayList<Pair<Long,MapPoint>>()
    fun add(time:Long?,point:MapPoint?) {
        if(time==null || time<=0 || point==null) return
        if(points.lastOrNull()?.let { time-it.first<30_000 }==true) return
        points.add(time to point)
        if(points.size>2048) points=ArrayList(points.filterIndexed { i,_->i%2==0 || i==points.lastIndex })
    }
    fun middle(start:Long,durationMs:Long?):WeatherTarget? {
        if(start<=0 || durationMs==null || durationMs<=0) return null
        val time=start+durationMs/2
        val nearest=points.minByOrNull { abs(it.first-time) } ?: return null
        if(abs(nearest.first-time)>300_000) return null
        return WeatherTarget(time,nearest.second.latitude,nearest.second.longitude)
    }
}
internal data class RideWeatherData(val code:Int?,val temperature:Double?,val humidity:Double?,val wind:Double?,val direction:Double?) {
    fun json()=JSONObject().put("code",code ?: JSONObject.NULL).put("temperature",temperature ?: JSONObject.NULL)
        .put("humidity",humidity ?: JSONObject.NULL).put("wind",wind ?: JSONObject.NULL).put("direction",direction ?: JSONObject.NULL)
    companion object {
        fun from(j:JSONObject):RideWeatherData {
            fun value(key:String,range:ClosedFloatingPointRange<Double>)=j.optDouble(key).takeIf { it.isFinite() && it in range }
            return RideWeatherData(value("code",0.0..99.0)?.toInt(),value("temperature",-100.0..70.0),value("humidity",0.0..100.0),value("wind",0.0..400.0),value("direction",0.0..360.0))
        }
    }
}
/** One hourly weather lookup for a rounded location, never the entire track. */
internal class RideWeatherClient(private val forecast:String="https://api.open-meteo.com/v1/forecast",
    private val archive:String="https://archive-api.open-meteo.com/v1/archive",private val now:()->Long=System::currentTimeMillis,
    private val http:OkHttpClient=client) {
    fun read(target:WeatherTarget):RideWeatherData {
        require(target.timeMs>0 && target.timeMs<=now()+3_600_000 && target.latitude in -85.0..85.0 && target.longitude in -180.0..180.0)
        val day=Instant.ofEpochMilli(target.timeMs).atZone(ZoneOffset.UTC).toLocalDate()
        val recent=day>=Instant.ofEpochMilli(now()).atZone(ZoneOffset.UTC).toLocalDate().minusDays(5)
        val url=(if(recent) forecast else archive).toHttpUrl().newBuilder()
            .addQueryParameter("latitude",String.format(java.util.Locale.ROOT,"%.3f",target.latitude))
            .addQueryParameter("longitude",String.format(java.util.Locale.ROOT,"%.3f",target.longitude))
            .addQueryParameter("start_date",day.toString()).addQueryParameter("end_date",day.toString())
            .addQueryParameter("hourly","temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m")
            .addQueryParameter("timezone","UTC").addQueryParameter("timeformat","unixtime").addQueryParameter("wind_speed_unit","kmh").build()
        val text=http.newCall(Request.Builder().url(url).header("User-Agent","B-Cycle/${AppVersion.name}").build()).execute().use {
            check(it.isSuccessful)
            val bytes=it.body?.byteStream()?.use { stream->stream.readNBytes(1024*1024+1) } ?: error("No weather")
            require(bytes.size<=1024*1024);String(bytes,Charsets.UTF_8)
        }
        return parse(JSONObject(text),target.timeMs)
    }
    companion object {
        private val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(20,TimeUnit.SECONDS).build()
        internal fun parse(json:JSONObject,timeMs:Long):RideWeatherData {
            val hourly=json.getJSONObject("hourly");val times=hourly.getJSONArray("time")
            val index=(0 until times.length()).filter { !times.isNull(it) }.minByOrNull { abs(times.getLong(it)*1000-timeMs) } ?: error("No weather time")
            require(abs(times.getLong(index)*1000-timeMs)<=3_600_000)
            fun value(name:String)=hourly.optJSONArray(name)?.opt(index) ?: JSONObject.NULL
            val data=RideWeatherData.from(JSONObject().put("code",value("weather_code")).put("temperature",value("temperature_2m"))
                .put("humidity",value("relative_humidity_2m")).put("wind",value("wind_speed_10m")).put("direction",value("wind_direction_10m")))
            require(data.temperature!=null || data.humidity!=null || data.wind!=null || data.code!=null)
            return data
        }
    }
}
/** Durable per-ride metadata, outside Android's disposable cache directory. */
internal class RideWeatherStore(context:Context) {
    private val root=File(context.filesDir,"ride-weather-v1")
    private fun file(id:String):AtomicFile {
        val hash=java.security.MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(root,"$hash.json"))
    }
    fun read(id:String,target:WeatherTarget):RideWeatherData?=runCatching {
        val text=file(id).openRead().use { String(it.readNBytes(8193),Charsets.UTF_8) }
        require(text.length<=8192)
        val j=JSONObject(text)
        require(j.getInt("schema")==1 && j.getLong("time")==target.timeMs &&
            abs(j.getDouble("latitude")-target.latitude)<.000001 && abs(j.getDouble("longitude")-target.longitude)<.000001)
        RideWeatherData.from(j.getJSONObject("data")).also { data ->
            require(data.code!=null || data.temperature!=null || data.humidity!=null || data.wind!=null)
        }
    }.getOrNull()
    fun write(id:String,target:WeatherTarget,data:RideWeatherData) {
        root.mkdirs()
        val json=JSONObject().put("schema",1).put("time",target.timeMs).put("latitude",target.latitude).put("longitude",target.longitude)
            .put("retrieved",System.currentTimeMillis()).put("source","Open-Meteo").put("data",data.json())
        val atomic=file(id);val out=atomic.startWrite()
        try { out.write(json.toString().toByteArray());atomic.finishWrite(out) } catch(e:Exception) { atomic.failWrite(out);throw e }
    }
    fun delete(id:String) { file(id).delete() }
}
internal object RideWeather {
    private val worker=Executors.newSingleThreadExecutor { Thread(it,"B-Cycle-weather").apply { isDaemon=true } }
    private val main=Handler(Looper.getMainLooper())
    fun load(context:Context,id:String,target:WeatherTarget,complete:(RideWeatherData?)->Unit):Future<*> = worker.submit {
        val result=runCatching {
            val store=RideWeatherStore(context)
            store.read(id,target)?.let { return@runCatching it }
            val network=context.getSystemService(ConnectivityManager::class.java)
            check(network.getNetworkCapabilities(network.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true)
            val data=RideWeatherClient().read(target)
            store.write(id,target,data)
            data
        }.getOrNull()
        if(!Thread.currentThread().isInterrupted) main.post { complete(result) }
    }
    fun forget(context:Context,id:String):Future<*> = worker.submit { RideWeatherStore(context).delete(id) }
}
