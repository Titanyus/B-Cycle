package de.bcycle.app

import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class UnifiedStyleTest {
    @Test fun legacyFlatIsIgnoredNotExportedAndDoesNotReturnThroughRestore() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("unified-style",0)
        prefs.edit().putBoolean(BicycleAppearance.flatKey,true).commit()
        assertFalse(ThemeManager(prefs).flatDesign);assertFalse(BicycleAppearance.flat(c))
        val backup=SettingsBackup(prefs,c.getSharedPreferences("unified-route",0),ThemeManager(prefs))
        val json=JSONObject(String(backup.export(setOf(BackupSection.DESIGN))))
        val design=json.getJSONObject("sections").getJSONObject(BackupSection.DESIGN.id)
        assertFalse(design.has("flatDesign"))
        design.put("flatDesign",true)
        val plan=backup.plan(backup.decode(json.toString().toByteArray()),setOf(BackupSection.DESIGN))
        assertNull(plan.bike[BicycleAppearance.flatKey])
        backup.apply(plan);assertFalse(prefs.contains(BicycleAppearance.flatKey))
        RetiredProfileSettings.purge(prefs);assertFalse(prefs.contains(BicycleAppearance.flatKey))
    }
    @Test fun dashboardSettingsHaveNoStyleSwitchAndFirstControlIsThemePicker() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            val page=MainActivity::class.java.getDeclaredMethod("dashboardControls",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,true) as android.view.ViewGroup
            assertNull(page.findViewWithTag<View>("flat_design"))
            assertTrue(page.getChildAt(0) is ThemePickerView)
        } finally { host.pause().stop().destroy() }
    }
}
