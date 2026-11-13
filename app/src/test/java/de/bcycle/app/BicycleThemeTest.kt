package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class BicycleThemeTest {
    private val red = BicycleThemes.red
    @Test fun redAndLightThemesMeetContrastAndSurfaceRequirements() {
        assertTrue(ThemeValidator.validate(red).valid)
        val light = CustomTheme("custom:light", "Light", ThemeTokens(0xffeeeeee.toInt(), 0xffcccccc.toInt(), 0xffaaaaaa.toInt(), 0xff000000.toInt()))
        assertTrue(ThemeValidator.validate(light).valid)
        assertEquals(21.0, ThemeValidator.contrast(0xff000000.toInt(), 0xffffffff.toInt()), 0.00001)
        for (color in listOf(red.tokens.primary, red.tokens.secondary, red.tokens.tertiary))
            assertTrue(ThemeValidator.contrast(red.tokens.foreground, color) >= 4.5)
    }
    @Test fun unreadableTransparentAndIndistinguishableSurfacesAreRejected() {
        val invalid = listOf(red.tokens.copy(foreground = red.tokens.secondary),
            red.tokens.copy(primary = 0x00d00000), red.tokens.copy(tertiary = red.tokens.secondary),
            red.tokens.copy(primary = red.tokens.secondary))
        invalid.forEach { assertFalse(ThemeValidator.validate(red.copy(tokens = it)).valid) }
        assertFalse(ThemeValidator.validate(CustomTheme(red.id, "Mine", red.tokens)).valid)
        assertFalse(ThemeValidator.validate(red.copy(name = " ")).valid)
        assertFalse(ThemeValidator.validate(red.copy(name = "Name\n")).valid)
        assertFalse(ThemeValidator.validate(red.copy(id = "preset:")).valid)
    }
    @Test fun fourteenUniquePrimariesShareNormalDarkSurfacesAndMatchingAccents() {
        val rows=listOf(
            "hellfire" to "BF281B", "ember-rush" to "D84413", "solar-flare" to "F16A09", "gold-rush" to "ED8C04",
            "sunstruck" to "E9AF00", "citrus-charge" to "C9DB02", "acid-pop" to "7ACD04", "emerald-surge" to "05C121",
            "deep-dive" to "06B5A1", "tidal-blue" to "0C96C6", "blue-riot" to "145CD7", "indigo-rush" to "311CDC",
            "purple-haze" to "9224E0", "neon-bloom" to "D01F95")
        fun color(s:String)=(0xff000000L or s.toLong(16)).toInt()
        assertEquals(rows.map { "preset:${it.first}-color" },BicycleThemes.presets.map { it.id })
        assertEquals(14,BicycleThemes.presets.map { it.tokens.primary }.distinct().size)
        rows.zip(BicycleThemes.presets).forEach { (row,t) ->
            assertEquals(ThemeTokens(color(row.second),color("161522"),color("282735"),color("FFFFFF"),color(row.second)),t.tokens)
            assertFalse(t.name.contains("DARK"))
            assertTrue(ThemeValidator.validate(t).problems.toString(),ThemeValidator.validate(t).valid)
            val light=t.tokens.lightSettings()
            assertEquals(t.tokens.primary,light.primary);assertEquals(t.tokens.accent,light.accent)
            assertEquals(color("FFFFFF"),light.secondary)
        }
        val old=listOf("hellfire","solar-flare","sunstruck","acid-pop","deep-dive","blue-riot","purple-haze")
        old.forEachIndexed { i,id -> assertEquals(BicycleThemes.presets[i*2+1],BicycleThemes.findPreset("preset:$id-dark")) }
    }
    @Test fun legacyIdsResolveToNewPresetsAndLightTintHasFixedSubtleStrength() {
        val t=BicycleThemes.findPreset("preset:black-ops")!!
        assertEquals("preset:ember-rush-color",t.id)
        assertEquals("preset:hellfire-color",BicycleThemes.findPreset("preset:bicycle-red")!!.id)
        assertEquals("preset:citrus-charge-color",BicycleThemes.findPreset("preset:solar-flare")!!.id)
        assertEquals(0xff191111.toInt(),BicycleThemes.red.tokens.lightSettings().foreground)
        assertEquals(0xffcfc3c2.toInt(),BicycleThemes.red.tokens.lightSettings().tertiary)
        assertNull(BicycleThemes.findPreset("preset:unknown"))
    }
}
