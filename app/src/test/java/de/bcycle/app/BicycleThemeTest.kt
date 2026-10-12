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
            if (it.id != "preset:black-ops") assertEquals(it.tokens.foreground, it.tokens.accent)
            assertTrue(ThemeValidator.validate(it).valid)
        }
        val base = ThemeTokens(0xff535353.toInt(), 0xff292929.toInt(), 0xff000000.toInt(), 0xffffffff.toInt())
        val theme = CustomTheme("custom:accent", "Accent", base.copy(accent = 0xffffaa55.toInt()))
        assertTrue(ThemeValidator.validate(theme).problems.toString(), ThemeValidator.validate(theme).valid)
        assertFalse(ThemeValidator.validate(theme.copy(tokens = base.copy(accent = base.secondary))).valid)
        assertFalse(ThemeValidator.validate(theme.copy(tokens = base.copy(accent = 0x00ffaa55))).valid)
        assertFalse(ThemeValidator.validate(theme.copy(tokens = base.copy(accent = 0xffd1261b.toInt()))).valid)
    }
    @Test fun blackOpsUsesBlackBodyGrayFieldsAndAccentOnlyOnReadableSurfaces() {
        val theme = BicycleThemes.presets.single { it.id == "preset:black-ops" }
        val t = theme.tokens
        assertEquals(0xff000000.toInt(), t.secondary)
        assertEquals(0xff292929.toInt(), t.tertiary)
        assertEquals(0xff535353.toInt(), t.primary)
        assertEquals(0xffd1261b.toInt(), t.accent)
        assertTrue(ThemeValidator.validate(theme).valid)
        assertEquals(t.accent, t.iconColor(ThemeRole.ACCENT, ThemeRole.SECONDARY))
        assertEquals(t.foreground, t.iconColor(ThemeRole.ACCENT, ThemeRole.TERTIARY))
        assertEquals(t.foreground, t.iconColor(ThemeRole.ACCENT, ThemeRole.PRIMARY))
        assertEquals(t.foreground, t.iconColor(ThemeRole.FOREGROUND))
    }

}
