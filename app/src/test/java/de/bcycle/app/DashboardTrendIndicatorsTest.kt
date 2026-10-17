package de.bcycle.app

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DashboardTrendIndicatorsTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val prefs get()=c.getSharedPreferences("bike",0)
    private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    @Test fun switchFollowsSourceDropdownAndOnlyExistingTrendFieldsOfferIt() {
        for(id in listOf("GPS_SPEED","GPS_MOVING_AVERAGE","GPS_OVERALL_AVERAGE","SPEED","AVERAGE_SPEED","RIDER_POWER","AVERAGE_POWER","CADENCE","AVERAGE_CADENCE","HEART_RATE","HEART_AVERAGE")) {
            var selected:Boolean?=null
            val picker=FieldPickerView(c,LiveData(),GpsValues(),null,PulseValues(),DashboardField(id),false,
                trendEnabled=true,trendChanged={ selected=it },choose={})
            val toggle=picker.findViewWithTag<Switch>("field_trend_indicator")
            assertEquals(View.VISIBLE,toggle.visibility);assertTrue(toggle.isChecked)
            val parent=toggle.parent as LinearLayout
            assertTrue(parent.getChildAt(parent.indexOfChild(toggle)-1) is Spinner)
            toggle.performClick();assertEquals(false,selected)
        }
        for(id in listOf("MOTOR_POWER","HEART_MAXIMUM","GPS_DISTANCE","WEATHER_SUNRISE","BATTERIES")) {
            assertFalse(AverageTrends.supports(DashboardField(id)))
            val picker=FieldPickerView(c,LiveData(),GpsValues(),null,PulseValues(),DashboardField(id),false,trendChanged={},choose={})
            assertEquals(View.GONE,picker.findViewWithTag<Switch>("field_trend_indicator").visibility)
        }
        val map=FieldPickerView(c,LiveData(),GpsValues(),null,PulseValues(),DashboardField("GPS_SPEED"),true,trendChanged={},choose={})
        assertEquals(View.GONE,map.findViewWithTag<Switch>("field_trend_indicator").visibility)
    }
    @Test fun turningOffOneTileDoesNotChangeOtherChevronOrTheExistingTrendHistory() {
        val dashboard=DashboardView(c) {}
        dashboard.setTemplate(DashboardTemplate.BALANCED)
        val layout=DashboardLayout.defaults.replace(0,DashboardField("GPS_SPEED")).replace(1,DashboardField("GPS_MOVING_AVERAGE"))
        var flags=DashboardTrendIndicators().replace(0,false)
        fun show(second:Int) {
            dashboard.showTrendIndicators(flags)
            dashboard.show(layout,LiveData(),GpsValues(speedKmh=35.0,movingAverageKmh=25.0+second*.1))
        }
        for(second in 0..10) { show(second);ShadowSystemClock.advanceBy(Duration.ofSeconds(1)) }
        fun direction(slot:Int):Int {
            val tile=dashboard.findViewWithTag<View>("dashboard_tile_$slot")
            val row=all(tile).single { it.javaClass.simpleName=="ValueRow" }
            return row.javaClass.getDeclaredField("direction").apply { isAccessible=true }.getInt(row)
        }
        assertEquals(0,direction(0));assertEquals(1,direction(1))
        flags=flags.replace(0,true);show(11)
        assertEquals(1,direction(0));assertEquals(1,direction(1))
    }
    @Test fun flagsDefaultToEnabledAndPersistPerPageIncludingBackupAndFutureBikeScope() {
        assertTrue(DashboardTrendIndicators.read(prefs,0).enabled.all { it })
        val flags=DashboardTrendIndicators().replace(2,false)
        prefs.edit().putString(DashboardTrendIndicators.key(0),flags.encode()).apply()
        assertFalse(DashboardTrendIndicators.read(prefs,0).enabled[2]);assertTrue(DashboardTrendIndicators.read(prefs,1).enabled[2])
        assertTrue(BikeUiConfigurationScope.capture(prefs).design.containsKey(DashboardTrendIndicators.key(0)))
        val backup=SettingsBackup(c,ThemeManager(prefs));val bytes=backup.export(setOf(BackupSection.DASHBOARD))
        prefs.edit().clear().commit();backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.DASHBOARD)))
        assertEquals(flags,DashboardTrendIndicators.read(prefs,0))
        val doc=org.json.JSONObject(String(bytes));val pages=doc.getJSONObject("sections").getJSONObject("dashboard").getJSONArray("pages")
        for(i in 0 until pages.length()) pages.getJSONObject(i).remove("trendIndicators")
        backup.apply(backup.plan(backup.decode(doc.toString().toByteArray()),setOf(BackupSection.DASHBOARD)))
        assertTrue(DashboardTrendIndicators.read(prefs,0).enabled.all { it })
        val store=DashboardPages(c,prefs);store.select(0);assertTrue(store.delete(0))
        assertFalse(prefs.contains(DashboardTrendIndicators.key(0)))
    }
}
