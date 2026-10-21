package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RollingGraphTest {
    @Test fun everyWindowKeepsRecentTimeSamplesWithoutDistanceAndFullUsesTimeFallback() {
        val history=GraphHistoryBuffer()
        for(i in 0..240) history.append(GraphPoint(i*1000L,i*1000L,null,List(5) { column -> if(column==2 && i==230) 0.0 else (column+1)*20.0 }))
        val f=history.frame(1,320)
        assertEquals(240000,f.elapsedMs);assertTrue(f.timeAxis)
        for(window in GraphWindow.entries.filter { it!=GraphWindow.FULL }) {
            assertEquals(5,f.windows.getValue(window).size)
            val p=f.windows.getValue(window).getValue(AnalysisChart.POWER).points
            assertEquals(240000,p.last().elapsedMs)
            assertTrue(p.first().elapsedMs in (240000-window.milliseconds-1000)..(240000-window.milliseconds))
            assertTrue(p.any { it.values[2]==0.0 })
        }
        assertTrue(f.series.getValue(AnalysisChart.HEART).points.size>2)
    }
    @Test fun rollingProviderPublishesAtOneSecondAndRetainsTheLatestSample() {
        val provider=LiveGraphProvider {}.apply { rolling=true }
        val worker=LiveGraphProvider::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(provider) as java.util.concurrent.ExecutorService
        fun drain() { worker.submit {}.get(5,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle() }
        try {
            provider.begin();drain()
            provider.append(GraphPoint(0,0,null,listOf(null,null,100.0,null,null)));drain()
            val first=provider.frame!!
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1))
            provider.append(GraphPoint(1000,1000,null,listOf(null,null,200.0,null,null)));drain()
            assertTrue(provider.frame!!.version>first.version)
            assertEquals(1000,provider.frame!!.elapsedMs)
            provider.stop();drain()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));drain()
            assertEquals(1000,provider.frame!!.series.getValue(AnalysisChart.POWER).points.last().elapsedMs)
        } finally { worker.shutdownNow() }
    }
    @Test fun configuredCapabilitiesAndWindowSettingsSurviveProfileAndBackupRoundTrip() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("bike",0)
        val bikes=BikeProfileStore(c,prefs)
        assertTrue(GraphAvailability.forProfile(bikes.current().copy(gpsEnabled=false)).isEmpty())
        val indoor=bikes.current().copy(type=BikeType.INDOOR,trainerAddress="00:11:22:33:44:55",heartAddress=null)
        assertEquals(setOf(AnalysisChart.SPEED,AnalysisChart.POWER,AnalysisChart.CADENCE,AnalysisChart.HEART),GraphAvailability.forProfile(indoor))
        assertFalse(AnalysisChart.ELEVATION in GraphAvailability.forProfile(indoor))
        val gpsOnly=bikes.current().copy(recording=RecordingConfig(speedSource="GPS_SPEED"))
        assertEquals(setOf(AnalysisChart.SPEED,AnalysisChart.ELEVATION),GraphAvailability.forProfile(gpsOnly))
        assertFalse(AnalysisChart.SPEED in GraphAvailability.forProfile(gpsOnly.copy(gpsEnabled=false,bikeAddress="00:11:22:33:44:55")))
        val first=bikes.activeSlot
        val config=GraphOverlayConfig().withWindow(0,GraphWindow.SECONDS_60)
        prefs.edit().putString(GraphOverlayConfig.windowKey(0),config.encodeWindows()).commit()
        val second=bikes.add()!!;bikes.select(second.slot)
        prefs.edit().putString(GraphOverlayConfig.windowKey(0),config.withWindow(0,GraphWindow.SECONDS_120).encodeWindows()).commit()
        bikes.select(first);assertEquals(GraphWindow.SECONDS_60,GraphOverlayConfig.read(prefs,0).windows[0])
        val backup=SettingsBackup(c,ThemeManager(prefs));val bytes=backup.export(setOf(BackupSection.DASHBOARD))
        prefs.edit().remove(GraphOverlayConfig.windowKey(0)).commit()
        val doc=backup.decode(bytes);backup.apply(backup.plan(doc,doc.sections.keys))
        assertEquals(GraphWindow.SECONDS_60,GraphOverlayConfig.read(prefs,0).windows[0])
        val view=GraphOverlayPicker(c,ThemeManager(prefs),emptyList(),GraphWindow.SECONDS_60,GraphAvailability.forProfile(indoor))
        assertFalse(view.findViewWithTag<android.widget.CheckBox>("graph_ELEVATION").isEnabled)
        view.findViewWithTag<android.view.View>("graph_window_SECONDS_120").performClick()
        assertEquals(GraphWindow.SECONDS_120,view.window)
    }
}
