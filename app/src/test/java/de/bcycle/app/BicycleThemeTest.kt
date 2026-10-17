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
    @Test fun fixedPalettesMatchAllFourteenRequestedSetsAndSharePrimaryByFamily() {
        val rows=listOf(
            listOf("hellfire","BF281B","812218","550E08","0F0A09","211918"),
            listOf("solar-flare","F16A09","B64E01","863701","100B08","221A15"),
            listOf("sunstruck","E9AF00","B78901","8F6A00","100E07","242014"),
            listOf("acid-pop","7ACD04","5EA000","477B01","0A0F07","192215"),
            listOf("deep-dive","06B5A1","018979","006559","07100F","142221"),
            listOf("blue-riot","145CD7","003998","00215E","080B10","151B23"),
            listOf("purple-haze","9224E0","6601A1","400068","0B0910","1A1624"))
        fun color(s:String)=(0xff000000L or s.toLong(16)).toInt()
        assertEquals(14,BicycleThemes.presets.size)
        rows.forEach { row ->
            for(dark in listOf(false,true)) {
                val t=BicycleThemes.presets.single { it.id=="preset:${row[0]}-${if(dark) "dark" else "color"}" }
                assertEquals(ThemeTokens(color(row[1]),color(row[if(dark) 4 else 2]),color(row[if(dark) 5 else 3]),color("FFFFFF"),color(if(dark) row[1] else "FFFFFF")),t.tokens)
                assertTrue(ThemeValidator.validate(t).problems.toString(),ThemeValidator.validate(t).valid)
                val light=t.tokens.lightSettings()
                assertEquals(t.tokens.primary,light.primary);assertEquals(t.tokens.accent,light.accent)
                assertEquals(color("FFFFFF"),light.secondary)
                assertTrue(ThemeValidator.contrast(light.foreground,light.secondary)>15.0)
            }
        }
    }
    @Test fun legacyIdsResolveToNewPresetsAndLightTintHasFixedSubtleStrength() {
        val t=BicycleThemes.findPreset("preset:black-ops")!!
        assertEquals("preset:hellfire-dark",t.id)
        assertEquals("preset:hellfire-color",BicycleThemes.findPreset("preset:bicycle-red")!!.id)
        assertEquals("preset:sunstruck-dark",BicycleThemes.findPreset("preset:solar-flare")!!.id)
        assertEquals(0xff191111.toInt(),t.tokens.lightSettings().foreground)
        assertEquals(0xffcfc3c2.toInt(),t.tokens.lightSettings().tertiary)
        assertNull(BicycleThemes.findPreset("preset:unknown"))
    }
}
