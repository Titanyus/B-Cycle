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
    @Test fun unreadableTransparentFlatAndReversedSurfacesAreRejected() {
        val invalid = listOf(red.tokens.copy(foreground = red.tokens.secondary),
            red.tokens.copy(primary = 0x00d00000), red.tokens.copy(tertiary = red.tokens.secondary),
            red.tokens.copy(primary = red.tokens.tertiary))
        invalid.forEach { assertFalse(ThemeValidator.validate(red.copy(tokens = it)).valid) }
        assertFalse(ThemeValidator.validate(CustomTheme(red.id, "Mine", red.tokens)).valid)
        assertFalse(ThemeValidator.validate(red.copy(name = " ")).valid)
        assertFalse(ThemeValidator.validate(red.copy(name = "Name\n")).valid)
        assertFalse(ThemeValidator.validate(red.copy(id = "preset:")).valid)
    }
}
