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
    @Test fun globalSwitchAndAccordionAreIndependentAndListEverySupportedFieldInTwoGroups() {
        var config=TrendIndicatorConfig()
        val picker=FieldPickerView(c,LiveData(),GpsValues(),null,PulseValues(),DashboardField("MOTOR_POWER"),false,
            trendConfig=config,trendChanged={ config=it },choose={})
        val toggle=picker.findViewWithTag<Switch>("field_trend_indicator")
        val details=picker.findViewWithTag<View>("field_trend_details")
        assertEquals(View.VISIBLE,toggle.visibility);assertTrue(toggle.isChecked)
        assertEquals(View.GONE,details.visibility)
        picker.findViewWithTag<View>("field_trend_indicator_label").performClick()
        assertEquals(View.VISIBLE,details.visibility);assertTrue(config.enabled)
        val checks=all(details).filterIsInstance<android.widget.CheckBox>()
        assertEquals(11,checks.size)
        assertEquals(TrendIndicatorSettings.fields.toSet(),checks.map { it.tag.toString().removePrefix("field_trend_") }.toSet())
        val headings=all(details).filterIsInstance<android.widget.TextView>().filter { it.isAccessibilityHeading }
        assertEquals(listOf("Aktuelle Werte","Durchschnittswerte"),headings.map { it.text.toString() })
        picker.findViewWithTag<android.widget.CheckBox>("field_trend_GPS_SPEED").performClick()
        assertFalse(config.shows(DashboardField("GPS_SPEED")))
        assertTrue(config.shows(DashboardField("GPS_MOVING_AVERAGE")))
        toggle.performClick();assertFalse(config.enabled);assertEquals(View.VISIBLE,details.visibility)
        toggle.performClick();assertTrue(config.enabled);assertFalse(config.shows(DashboardField("GPS_SPEED")))
    }
    @Test fun turningOffOneFieldTypeAffectsAllOccurrencesWithoutChangingOtherTypesOrHistory() {
        val dashboard=DashboardView(c) {}
        dashboard.setTemplate(DashboardTemplate.BALANCED)
        val layout=DashboardLayout.defaults.replace(0,DashboardField("GPS_SPEED")).replace(1,DashboardField("GPS_MOVING_AVERAGE")).replace(3,DashboardField("GPS_SPEED"))
        var flags=TrendIndicatorConfig(disabled=setOf("GPS_SPEED"))
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
        assertEquals(0,direction(0));assertEquals(0,direction(3));assertEquals(3,direction(1))
        flags=flags.select("GPS_SPEED",true);show(11)
        assertEquals(3,direction(0));assertEquals(3,direction(3));assertEquals(3,direction(1))
    }
    @Test fun globalChoicesPersistThroughBackupAndBikeScopeAndLegacyFlagsMigrateByField() {
        val old=DashboardTrendIndicators().replace(0,false)
        prefs.edit().putString("dashboard",DashboardLayout.defaults.replace(0,DashboardField("GPS_SPEED")).encode())
            .putString(DashboardTrendIndicators.key(0),old.encode()).apply()
        assertFalse(TrendIndicatorSettings.read(prefs).shows(DashboardField("GPS_SPEED")))
        val config=TrendIndicatorConfig(false,setOf("HEART_RATE","AVERAGE_POWER"))
        TrendIndicatorSettings.save(prefs,config)
        assertEquals(config,TrendIndicatorSettings.read(prefs))
        val capture=BikeUiConfigurationScope.capture(prefs).design
        assertTrue(capture.containsKey(TrendIndicatorSettings.enabledKey));assertTrue(capture.containsKey(TrendIndicatorSettings.disabledKey))
        val backup=SettingsBackup(c,ThemeManager(prefs));val bytes=backup.export(setOf(BackupSection.DASHBOARD))
        prefs.edit().clear().commit();backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.DASHBOARD)))
        assertEquals(config,TrendIndicatorSettings.read(prefs))
        val pages=DashboardPages(c,prefs);pages.select(0);pages.delete(0)
        assertEquals(config,TrendIndicatorSettings.read(prefs))
    }
    @Test fun globalSwitchAlsoControlsMapFieldsWithoutClearingTrendHistory() {
        prefs.edit().putString(MapFieldSelection.key(0),"GPS_SPEED").apply()
        val map=MapDataFieldsView(c);map.reload(prefs)
        fun direction()=(MapDataFieldsView::class.java.getDeclaredField("directions").apply { isAccessible=true }.get(map) as IntArray)[0]
        for(i in 0..5) {
            map.show(LiveData(),GpsValues(speedKmh=35.0,movingAverageKmh=25.0),null,PulseValues())
            ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        }
        assertEquals(3,direction())
        TrendIndicatorSettings.save(prefs,TrendIndicatorConfig(enabled=false));map.reload(prefs)
        map.show(LiveData(),GpsValues(speedKmh=35.0,movingAverageKmh=25.0),null,PulseValues());assertEquals(0,direction())
        TrendIndicatorSettings.save(prefs,TrendIndicatorConfig());map.reload(prefs)
        map.show(LiveData(),GpsValues(speedKmh=35.0,movingAverageKmh=25.0),null,PulseValues());assertEquals(3,direction())
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
        doc.getJSONObject("sections").getJSONObject("dashboard").remove("trendEnabled")
        doc.getJSONObject("sections").getJSONObject("dashboard").remove("trendDisabledFields")
        backup.apply(backup.plan(backup.decode(doc.toString().toByteArray()),setOf(BackupSection.DASHBOARD)))
        assertTrue(DashboardTrendIndicators.read(prefs,0).enabled.all { it })
        val store=DashboardPages(c,prefs);store.select(0);assertTrue(store.delete(0))
        assertFalse(prefs.contains(DashboardTrendIndicators.key(0)))
    }
}
