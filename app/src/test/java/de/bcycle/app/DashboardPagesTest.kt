package de.bcycle.app

import android.view.View
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DashboardPagesTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val prefs get()=c.getSharedPreferences("bike",0)
    @Test fun existingFourPagesMigrateAndNewPagesAreLimitedToSix() {
        prefs.edit().putString("dashboard_2","existing-fields").putString("dashboardGraphs_2","POWER").apply()
        val pages=DashboardPages(c,prefs)
        assertEquals(listOf(0,1,2,3),pages.all().map { it.id })
        assertTrue(pages.add());assertTrue(pages.add());assertFalse(pages.add())
        assertTrue(pages.rename(5,"Indoor Training"));assertEquals(5,pages.selected())
        assertFalse(pages.rename(5," "))
        assertEquals("Indoor Training",DashboardPages(c,prefs).all().last().name)
        assertEquals("existing-fields",prefs.getString("dashboard_2",null))
        assertEquals("POWER",prefs.getString("dashboardGraphs_2",null))
    }
    @Test fun deletingNeverRenumbersAndReusingSlotStartsFresh() {
        val pages=DashboardPages(c,prefs)
        prefs.edit().putString("dashboard_1","removed").putString("dashboardGraphs_1","old").putString("dashboard_2","retained").apply()
        pages.select(1);assertTrue(pages.delete(1))
        assertEquals(listOf(0,2,3),pages.all().map { it.id });assertEquals(0,pages.selected())
        assertEquals("retained",prefs.getString("dashboard_2",null));assertFalse(prefs.contains("dashboard_1"))
        assertTrue(pages.add());assertEquals(1,pages.selected());assertFalse(prefs.contains("dashboardGraphs_1"))
        while(pages.all().size>1) assertTrue(pages.delete(pages.selected()))
        assertFalse(pages.delete(pages.selected()))
    }
    @Test fun pickerShowsNamedCardsAndTenPreviewLayoutsWithIndependentSelection() {
        val picker=DashboardPresetPicker(c,0)
        val spinner=picker.findViewWithTag<Spinner>("dashboard_layout_choice")
        assertEquals(10,spinner.count)
        picker.findViewWithTag<View>("preset_page_1").performClick()
        spinner.setSelection(DashboardTemplate.HERO_SEVEN.ordinal);shadowOf(Looper.getMainLooper()).idle()
        assertEquals("HERO_SEVEN",prefs.getString("dashboardTemplate_1",null))
        assertNull(prefs.getString("dashboardTemplate_0",null))
        repeat(2) { picker.findViewWithTag<View>("dashboard_page_add").performClick() }
        assertNull(picker.findViewWithTag<View>("dashboard_page_add"))
        assertNotNull(spinner.adapter.getDropDownView(0,null,picker))
    }
    @Test fun sixNamedDashboardsRoundTripThroughSettingsBackup() {
        val pages=DashboardPages(c,prefs);pages.add();pages.add();pages.rename(5,"Trainer")
        prefs.edit().putString("dashboardTemplate_5","HERO_SEVEN")
            .putString("dashboard_5",DashboardLayout.defaults.replace(2,DashboardField("HEART_RATE")).encode())
            .putString(ZoneGlowConfig.key(5),ZoneGlowConfig().encode()).apply()
        val backup=SettingsBackup(c,ThemeManager(prefs))
        val bytes=backup.export(setOf(BackupSection.DASHBOARD))
        val saved=pages.all();prefs.edit().clear().commit()
        backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.DASHBOARD)))
        assertEquals(saved,pages.all());assertEquals(5,pages.selected())
        assertEquals("HERO_SEVEN",prefs.getString("dashboardTemplate_5",null))
        assertEquals("HEART_RATE",DashboardLayout.decode(prefs.getString("dashboard_5",null)).fields[2].id)
    }
    @Test fun recordLongPressSelectsBikeWithoutStartingRecording() {
        val controller=org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            assertTrue(runtime.addBike());val active=runtime.bikes.activeSlot
            val button=activity.window.decorView.findViewWithTag<android.widget.Button>("record_bike_button")
            assertTrue(button.performLongClick())
            val popup=org.robolectric.shadows.ShadowPopupMenu.getLatestPopupMenu()
            assertEquals(runtime.bikes.all().size,popup.menu.size())
            val first=runtime.bikes.all().first()
            popup.menu.performIdentifierAction(first.slot,0)
            assertNotEquals(active,runtime.bikes.activeSlot);assertEquals(first.slot,runtime.bikes.activeSlot)
            assertFalse(runtime.recording);assertTrue(button.text.contains(first.name))
            assertTrue(button.text is android.text.Spanned)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun futureBikePayloadIncludesThemeDashboardAndMapSettingsButNoAccountsOrRecording() {
        prefs.edit().putString(ThemeManager.activeKey,"embers").putString("dashboard_5","fields")
            .putString("mapSource","OPENTOPOMAP").putBoolean("mapNight",true).putString("mapDataField_0","HEART_RATE")
            .putString("recordSpeed","SPEED").putString("oauthSecret","secret").apply()
        val snapshot=BikeUiConfigurationScope.capture(prefs)
        assertEquals("embers",snapshot.design[ThemeManager.activeKey]);assertEquals("fields",snapshot.design["dashboard_5"])
        assertEquals("HEART_RATE",snapshot.design[MapFieldSelection.key(0)])
        assertEquals("OPENTOPOMAP",snapshot.maps["mapSource"]);assertEquals(true,snapshot.maps["mapNight"])
        assertFalse(snapshot.design.containsKey("oauthSecret"));assertFalse(snapshot.maps.containsKey("recordSpeed"))
        assertEquals("embers",prefs.getString(ThemeManager.activeKey,null))
    }
}
