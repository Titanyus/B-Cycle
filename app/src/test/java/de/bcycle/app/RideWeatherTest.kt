package de.bcycle.app

import android.view.View
import android.widget.TextView
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideWeatherTest {
    private val start=1791806400000L
    private fun response(time:Long)=JSONObject("""{"hourly":{"time":[${time/1000},${time/1000+3600}],"temperature_2m":[-5.5,12],"relative_humidity_2m":[0,null],"weather_code":[61,3],"wind_speed_10m":[0,10],"wind_direction_10m":[315,180]}}""")
    @Test fun midpointUsesElapsedTimeAndNearbyPositionAndRefusesLongGpsGaps() {
        val buffer=WeatherTargetBuffer()
        buffer.add(start,MapPoint(48.0,10.0));buffer.add(start+3600000,MapPoint(48.1,10.1));buffer.add(start+7200000,MapPoint(49.0,12.0))
        assertEquals(WeatherTarget(start+3600000,48.1,10.1),buffer.middle(start,7200000))
        assertNull(buffer.middle(start,3600000))
        assertNull(buffer.middle(0,7200000));assertNull(WeatherTargetBuffer().middle(start,7200000))
    }
    @Test fun hourlyParsingPreservesRealZeroAndNeverTurnsMissingValuesIntoZero() {
        val a=RideWeatherClient.parse(response(start),start+600000)
        assertEquals(-5.5,a.temperature!!,0.0);assertEquals(0.0,a.humidity!!,0.0);assertEquals(0.0,a.wind!!,0.0)
        val b=RideWeatherClient.parse(response(start),start+3600000)
        assertNull(b.humidity);assertEquals(3,b.code)
        assertEquals(R.string.weather_rain,WeatherSky.label(a.code))
        assertThrows(IllegalArgumentException::class.java) { RideWeatherClient.parse(response(start),start+24*3600000) }
    }
    @Test fun clientChoosesArchiveOrRecentWeatherAndUsesUtcHourlyMetricValues() {
        val server=MockWebServer();server.start()
        try {
            val client=RideWeatherClient(server.url("/forecast").toString(),server.url("/archive").toString(),{start+10*86400000L})
            server.enqueue(MockResponse().setBody(response(start).toString()))
            assertEquals(-5.5,client.read(WeatherTarget(start,48.12345,10.23456)).temperature!!,0.0)
            val archive=server.takeRequest();assertEquals("/archive",archive.requestUrl!!.encodedPath)
            assertEquals("48.123",archive.requestUrl!!.queryParameter("latitude"))
            assertEquals("UTC",archive.requestUrl!!.queryParameter("timezone"));assertEquals("kmh",archive.requestUrl!!.queryParameter("wind_speed_unit"))
            val recent=start+9*86400000L
            server.enqueue(MockResponse().setBody(response(recent).toString()))
            client.read(WeatherTarget(recent,48.1,10.1))
            assertEquals("/forecast",server.takeRequest().requestUrl!!.encodedPath)
        } finally { server.shutdown() }
    }
    @Test fun storedWeatherSurvivesReopenAndCacheDeletionAndLoadsWithoutNetwork() {
        val c=RuntimeEnvironment.getApplication()
        val target=WeatherTarget(start,48.123,10.234)
        val data=RideWeatherData(0,13.5,50.0,6.0,315.0)
        RideWeatherStore(c).write("recorded-test",target,data)
        c.cacheDir.deleteRecursively()
        assertEquals(data,RideWeatherStore(c).read("recorded-test",target))
        var loaded:RideWeatherData?=null
        RideWeather.load(c,"recorded-test",target) { loaded=it }.get(5,java.util.concurrent.TimeUnit.SECONDS)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(data,loaded)
        assertNull(RideWeatherStore(c).read("recorded-test",target.copy(timeMs=start+1000)))
        RideWeather.forget(c,"recorded-test").get(5,java.util.concurrent.TimeUnit.SECONDS)
        assertNull(RideWeatherStore(c).read("recorded-test",target))
    }
    @Test fun compactRowKeepsWeatherHumidityAndWindVisibleInPortrait() {
        val c=RuntimeEnvironment.getApplication();val themes=ThemeManager(c.getSharedPreferences("bike",0))
        val row=RideWeatherRow(c,themes,RideWeatherData(0,13.5,50.0,6.0,315.0))
        row.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
        row.layout(0,0,320,row.measuredHeight)
        assertEquals(3,row.childCount)
        (0 until row.childCount).forEach { assertTrue(row.getChildAt(it).width>0);assertEquals(1,(row.getChildAt(it) as TextView).maxLines) }
        assertTrue(row.contentDescription.toString().contains("50"));assertTrue(row.contentDescription.toString().contains("NW"))
        assertEquals("analysis_weather",row.tag)
    }
}
