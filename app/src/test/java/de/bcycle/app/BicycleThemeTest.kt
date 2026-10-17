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
    @Test fun accentsDefaultToForegroundAndRequireOpaqueContrastingColors() {
        BicycleThemes.presets.forEach {
            if (it.id !in setOf("preset:black-ops", "preset:embers-inv", "preset:solar-flare", "preset:electric-blue", "preset:acid-lime")) assertEquals(it.tokens.foreground, it.tokens.accent)
            assertTrue(ThemeValidator.validate(it).valid)
        }
        val base = ThemeTokens(0xff535353.toInt(), 0xff292929.toInt(), 0xff000000.toInt(), 0xffffffff.toInt())
        assertEquals(base.foreground, base.accent)
        val theme = CustomTheme("custom:accent", "Accent", base.copy(accent = 0xffffaa55.toInt()))
        assertTrue(ThemeValidator.validate(theme).problems.toString(), ThemeValidator.validate(theme).valid)
        assertFalse(ThemeValidator.validate(theme.copy(tokens = base.copy(accent = base.secondary))).valid)
        assertFalse(ThemeValidator.validate(theme.copy(tokens = base.copy(accent = 0x00ffaa55))).valid)
        assertFalse(ThemeValidator.validate(theme.copy(tokens = base.copy(accent = 0xffd1261b.toInt()))).valid)
    }
    @Test fun blueAndLimeUseTheirOriginalPrimaryAsAccentWithNeutralSurfaces() {
        for ((id, primary) in mapOf("preset:electric-blue" to 0xff1168b8.toInt(), "preset:acid-lime" to 0xff587812.toInt())) {
            val theme = BicycleThemes.presets.single { it.id == id }
            assertEquals(ThemeTokens(primary, 0xff000000.toInt(), 0xff292929.toInt(), 0xffffffff.toInt(), primary), theme.tokens)
            assertTrue(theme.approvedLowContrastSurfaces.isEmpty())
            assertTrue(ThemeValidator.validate(theme).valid)
        }
    }
    @Test fun fireKeepsExactApprovedColorsAndOnlyItsHeaderContrastException() {
        val fire = BicycleThemes.presets.single { it.id == "preset:solar-flare" }
        assertEquals("FIRE", fire.name)
        assertEquals(ThemeTokens(0xffffb400.toInt(), 0xff000000.toInt(), 0xff292929.toInt(), 0xffffffff.toInt(), 0xffffb400.toInt()), fire.tokens)
        assertEquals(setOf(ThemeRole.PRIMARY), fire.approvedLowContrastSurfaces)
        assertTrue(ThemeValidator.validate(fire).valid)
        assertFalse(ThemeValidator.validate(fire.copy(approvedLowContrastSurfaces = emptySet())).valid)
        assertFalse(ThemeValidator.validate(fire.copy(tokens = fire.tokens.copy(secondary = 0xffffffff.toInt()))).valid)
        assertFalse(ThemeValidator.validate(fire.copy(tokens = fire.tokens.copy(tertiary = 0x00292929))).valid)
        assertFalse(ThemeValidator.validate(CustomTheme("custom:fire", "Mine", fire.tokens)).valid)
    }
    @Test fun embersPreservesOldIdAndUsesRedHeaderAndAccentOnlyOnReadableSurfaces() {
        val theme = BicycleThemes.presets.single { it.id == "preset:black-ops" }
        val t = theme.tokens
        assertEquals(0xff000000.toInt(), t.secondary)
        assertEquals("EMBERS", theme.name)
        assertEquals(0xff292929.toInt(), t.tertiary)
        assertEquals(0xffbf281b.toInt(), t.primary)
        assertEquals(0xffbf281b.toInt(), t.accent)
        assertTrue(ThemeValidator.validate(theme).valid)
        assertEquals(t.accent, t.iconColor(ThemeRole.ACCENT, ThemeRole.SECONDARY))
        assertEquals(t.foreground, t.iconColor(ThemeRole.ACCENT, ThemeRole.TERTIARY))
        assertEquals(t.foreground, t.iconColor(ThemeRole.ACCENT, ThemeRole.PRIMARY))
        assertEquals(t.foreground, t.iconColor(ThemeRole.FOREGROUND))
    }

    @Test fun invertedEmbersKeepsExactPaletteAndUsesExistingReadableIconFallback() {
        val base=BicycleThemes.presets.single { it.id=="preset:black-ops" }
        val inverted=BicycleThemes.presets.single { it.id=="preset:embers-inv" }
        assertEquals("Embers Inv.",inverted.name)
        assertEquals(base.tokens.copy(secondary=0xff1d1d1d.toInt(),tertiary=0xff000000.toInt()),inverted.tokens)
        assertTrue(ThemeValidator.validate(inverted).valid)
        assertFalse(ThemeValidator.validate(inverted.copy(approvedLowContrastAccent=false)).valid)
        assertFalse(ThemeValidator.validate(CustomTheme("custom:inv","Inv",inverted.tokens)).valid)
        assertEquals(inverted.tokens.foreground,inverted.tokens.iconColor(ThemeRole.ACCENT,ThemeRole.SECONDARY))
    }

}
