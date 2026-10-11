package de.bcycle.app

import android.content.Context
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemeManagerTest {
    private val prefs get() = RuntimeEnvironment.getApplication().getSharedPreferences("bike", Context.MODE_PRIVATE)
    private val light = CustomTheme("custom:light", "Light", ThemeTokens(0xffeeeeee.toInt(), 0xffcccccc.toInt(), 0xffaaaaaa.toInt(), 0xff000000.toInt()))
    @Test fun customCreateEditCopyDeleteAndRestartPreserveOtherSettingsAndPresets() {
        prefs.edit().putString("address", "AA:BB:CC:DD:EE:FF").putBoolean("gpsEnabled", true).apply()
        val manager = ThemeManager(prefs)
        assertEquals(BicycleThemes.red, manager.active)
        assertTrue(manager.saveCustom(light).valid); assertTrue(manager.activate(light.id))
        assertEquals(light, ThemeManager(prefs).active)
        val changed = mutableListOf<BicycleTheme>()
        val subscription = manager.observe { changed += it }
        val edited = light.copy(name = "Edited")
        assertTrue(manager.saveCustom(edited).valid); assertEquals(edited, changed.last())
        val copy = manager.copyTheme(edited.id, "Copy")!!
        assertNotEquals(copy.id, edited.id); assertTrue(copy.isCustom); assertEquals(edited.tokens, copy.tokens)
        assertFalse(manager.deleteCustom(BicycleThemes.red.id)); assertEquals(BicycleThemes.red, manager.presets.first())
        assertTrue(manager.deleteCustom(edited.id)); assertEquals(BicycleThemes.red, changed.last())
        subscription.close(); val count = changed.size
        assertTrue(manager.activate(copy.id)); assertEquals(count, changed.size)
        assertEquals(copy, ThemeManager(prefs).active)
        assertEquals("AA:BB:CC:DD:EE:FF", prefs.getString("address", null)); assertTrue(prefs.getBoolean("gpsEnabled", false))
    }
    @Test fun invalidDraftsNeverReplaceStoredThemeAndInvalidActivationPersistsFallback() {
        val manager = ThemeManager(prefs); manager.saveCustom(light); manager.activate(light.id)
        assertFalse(manager.saveCustom(light.copy(tokens = light.tokens.copy(foreground = light.tokens.primary))).valid)
        assertEquals(light, manager.active); assertEquals(light, ThemeManager(prefs).active)
        assertFalse(manager.activate("unknown")); assertEquals(BicycleThemes.red.id, prefs.getString(ThemeManager.activeKey, null))
        assertFalse(manager.saveCustom(light.copy(id = BicycleThemes.red.id)).valid)
    }
    @Test fun corruptStorageUnknownVersionsBadColorsDuplicateIdsAndWrongPreferenceTypesFallBack() {
        val cases = listOf("{bad json", "{\"version\":9,\"themes\":[]}", "{\"version\":1,\"themes\":[{\"kind\":\"preset\"}]}")
        for (raw in cases) {
            prefs.edit().putString(ThemeManager.customsKey, raw).putString(ThemeManager.activeKey, light.id).apply()
            val manager = ThemeManager(prefs); assertEquals(BicycleThemes.red, manager.active); assertTrue(manager.customThemes.isEmpty())
        }
        val original = ThemeManager(prefs); original.saveCustom(light)
        val root = JSONObject(prefs.getString(ThemeManager.customsKey, null)!!)
        val row = root.getJSONArray("themes").getJSONObject(0)
        root.put("themes", JSONArray().put(row).put(row))
        prefs.edit().putString(ThemeManager.customsKey, root.toString()).putString(ThemeManager.activeKey, light.id).apply()
        assertEquals(BicycleThemes.red, ThemeManager(prefs).active)
        row.put("primary", "#00EEEEEE"); root.put("themes", JSONArray().put(row))
        prefs.edit().putString(ThemeManager.customsKey, root.toString()).putInt(ThemeManager.activeKey, 42).apply()
        assertEquals(BicycleThemes.red, ThemeManager(prefs).active)
        prefs.edit().putInt(ThemeManager.customsKey, 42).apply()
        assertTrue(ThemeManager(prefs).customThemes.isEmpty())
    }
    @Test fun backgroundActivationDeliversOnMainAndClosingSubscriptionCancelsQueuedCallbacks() {
        val manager = ThemeManager(prefs); manager.saveCustom(light)
        val deliveries = mutableListOf<String>()
        val subscription = manager.observe { assertEquals(Looper.getMainLooper(), Looper.myLooper()); deliveries += it.id }
        Thread { manager.activate(light.id) }.apply { start(); join() }
        assertEquals(1, deliveries.size)
        shadowOf(Looper.getMainLooper()).idle(); assertEquals(light.id, deliveries.last())
        Thread { manager.activate(BicycleThemes.red.id) }.apply { start(); join() }
        subscription.close(); shadowOf(Looper.getMainLooper()).idle(); assertEquals(2, deliveries.size)
    }
    @Test fun customCollectionIsBoundedAndObserverCannotMutateStoreThroughReturnedList() {
        val manager = ThemeManager(prefs)
        repeat(ThemeManager.maximumCustomThemes) { assertTrue(manager.saveCustom(light.copy(id = "custom:$it")).valid) }
        assertFalse(manager.saveCustom(light).valid)
        val snapshot = manager.customThemes
        (snapshot as MutableList).clear()
        assertEquals(ThemeManager.maximumCustomThemes, manager.customThemes.size)
    }
}
