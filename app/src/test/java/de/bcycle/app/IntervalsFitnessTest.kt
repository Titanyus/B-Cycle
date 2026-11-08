package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.LinearLayout
import okhttp3.Credentials
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException
import java.time.LocalDate
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IntervalsFitnessTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private val end=LocalDate.of(2026,11,4)
    private fun configured():IntervalsSettings {
        val aes=KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return IntervalsSettings(context) { aes }.apply { write(IntervalsConfig(true,"i123",false),"secret") }
    }
    @Test fun dailyProjectionPreservesPrecisionAndGapsAndRejectsInvalidDatesAndValues() {
        val server=MockWebServer();server.start()
        try {
            val api=IntervalsClient(IntervalsConfig(true,"i123",false),"secret",base=server.url("/api/v1/"))
            server.enqueue(MockResponse().setBody("""[{"id":"2026-11-04","ctl":80.125,"atl":45.25,"fatigue":4,"ctlLoad":900},{"id":"2026-11-03","ctl":0,"atl":null}]"""))
            val rows=api.fitness(end.minusDays(1),end)
            assertEquals(34.875,rows.last().form!!,0.0);assertEquals(80.125,rows.last().fitness!!,0.0)
            assertEquals(0.0,rows.first().fitness!!,0.0);assertNull(rows.first().fatigue);assertNull(rows.first().formPercent)
            val request=server.takeRequest();assertEquals("GET",request.method)
            assertEquals(Credentials.basic("API_KEY","secret"),request.getHeader("Authorization"))
            assertEquals("id,ctl,atl",request.requestUrl!!.queryParameter("fields"))
            assertEquals("2026-11-03",request.requestUrl!!.queryParameter("oldest"))
            for(body in listOf("""[{"id":"2026-11-04","ctl":"80"}]""","""[{"id":"2026-11-04","atl":-1}]""",
                """[{"id":"2026-11-05","ctl":1}]""","""[{"id":"2026-11-04"},{"id":"2026-11-04"}]""")) {
                server.enqueue(MockResponse().setBody(body));assertEquals(R.string.intervals_fitness_response_failed,assertThrows(IntervalsFailure::class.java) { api.fitness(end.minusDays(1),end) }.problem)
                server.takeRequest()
            }
            server.enqueue(MockResponse().setResponseCode(401));assertEquals(R.string.intervals_auth_failed,assertThrows(IntervalsFailure::class.java) { api.fitness(end,end) }.problem)
        } finally { server.shutdown() }
    }
    @Test fun threeMonthsLoadInBoundedBatchesAndCacheSurvivesFailuresButNeverMixesAccounts() {
        val settings=configured();val store=IntervalsFitnessStore(File(context.cacheDir,"fitness-test"));val server=MockWebServer();server.start()
        val start=IntervalsFitnessPeriod.THREE_MONTHS.oldest(end)
        server.dispatcher=object:Dispatcher() {
            override fun dispatch(request:RecordedRequest):MockResponse {
                val day=request.requestUrl!!.queryParameter("oldest")!!
                return MockResponse().setBody("""[{"id":"$day","ctl":80.125,"atl":95.25}]""")
            }
        }
        val repository=IntervalsFitnessRepository(context,settings,store,{ true },{ cfg,key,stop -> IntervalsClient(cfg,key,stop,server.url("/api/v1/")) },{ 123456789L })
        try {
            val history=repository.refresh(IntervalsFitnessPeriod.THREE_MONTHS,end)
            assertEquals(90,history.days.size);assertEquals(3,server.requestCount)
            assertEquals(start,history.days.first().day);assertEquals(end,history.days.last().day)
            assertEquals(-15.125,history.days.first().form!!,0.0)
            assertNull(history.days[1].fitness);assertNull(history.days[1].form)
            assertEquals(history,store.cached("i123",start,end))
            assertEquals(7,repository.cached(IntervalsFitnessPeriod.WEEK,end)!!.days.size)
            assertEquals(30,repository.cached(IntervalsFitnessPeriod.MONTH,end)!!.days.size)
            val offline=IntervalsFitnessRepository(context,settings,store,{ false })
            assertThrows(IntervalsFailure::class.java) { offline.refresh(start,end) }
            assertEquals(history,repository.cached(start,end))
            val aborted=IntervalsFitnessRepository(context,settings,store,{ true },{ cfg,key,stop -> IntervalsClient(cfg,key,stop,server.url("/api/v1/")) })
            var requests=0
            server.dispatcher=object:Dispatcher() {
                override fun dispatch(request:RecordedRequest):MockResponse {
                    requests++;return if(requests==1) MockResponse().setBody("[]") else MockResponse().setResponseCode(500)
                }
            }
            assertThrows(IntervalsFailure::class.java) { aborted.refresh(start,end) }
            assertEquals("Partial downloads cannot overwrite the last complete snapshot",history,store.cached("i123",start,end))
            assertThrows(IOException::class.java) { repository.refresh(start,end) { true } }
            settings.write(settings.read().copy(athlete="i456"))
            assertNull(repository.cached(start,end));assertNotNull(store.cached("i123",start,end))
        } finally { server.shutdown() }
    }
    @Test fun statisticsIncludesFitnessLastEvenWithoutLocalRidesAndChartPansSelectsAndDrawsInBothDesigns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val statistics=RideStatisticsView(c,themes)
            statistics.show(RideStatistics(2026,emptyMap(),emptyMap(),emptyMap(),0))
            assertEquals("statistics_fitness",statistics.getChildAt(statistics.childCount-1).tag)
            val chart=IntervalsFitnessChart(c,themes) { end };var selected:IntervalsFitnessDay?=null;chart.selected={ selected=it }
            val from=end.minusDays(200);val days=(0..200).map { i -> IntervalsFitnessDay(from.plusDays(i.toLong()),if(i==198) null else 80.125+i/10.0,100.0-i/15.0) }
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat);c.setContentView(LinearLayout(c).apply { addView(chart,LinearLayout.LayoutParams(-1,600)) })
                chart.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));chart.layout(0,0,1000,600)
                val history=IntervalsFitnessHistory(from,end,days,1)
                chart.show(history)
                val curves=IntervalsFitnessChart::class.java.getDeclaredField("curves").apply { isAccessible=true }
                val prepared=curves.get(chart);chart.show(history)
                assertSame("Cache result must not rebuild twice",prepared,curves.get(chart))
                chart.goToday()
                assertEquals(end,selected!!.day)
                chart.period(IntervalsFitnessPeriod.THREE_MONTHS)
                assertTrue(chart.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null))
                assertEquals(end.minusDays(89),chart.newest());assertEquals(chart.newest(),selected!!.day)
                val bitmap=Bitmap.createBitmap(1000,600,Bitmap.Config.ARGB_8888);chart.draw(Canvas(bitmap))
                File("build/reports/ui/fitness-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
                (chart.parent as android.view.ViewGroup).removeView(chart)
            }
        } finally { host.pause().stop().destroy() }
    }
}
