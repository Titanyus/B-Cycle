package de.bcycle.app

import android.app.Activity
import android.view.View
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class InputDesignConfigurationTest {
    @Test fun compiledStylePreservesApprovedJsonAndEveryThemeOverride() {
        val spec=JSONObject(java.io.File("../docs/designs/input-design.json").readText())
        val strengths=spec.getJSONObject("chromaticByTheme")
        assertEquals(14,strengths.length())
        for(theme in BicycleThemes.presets) assertEquals(strengths.getDouble(theme.id.removePrefix("preset:")),BicycleInputStyle.strength(theme.id),0.0)
        assertEquals(spec.getDouble("chromatic"),BicycleInputStyle.strength("custom:unknown"),0.0)
        assertEquals(spec.getDouble("lighten"),BicycleDesign.inputFocusLighten.toDouble(),1e-7)
        assertEquals(spec.getDouble("darken"),BicycleDesign.inputFocusDarken.toDouble(),1e-7)
        assertEquals(spec.getJSONObject("outline").getInt("angle"),BicycleDesign.inputFocusAngle)
        assertEquals(spec.getJSONObject("outline").getDouble("stroke"),BicycleDesign.inputFocusStroke.toDouble(),0.0)
        assertEquals(spec.getJSONObject("shape").getInt("radius"),BicycleDesign.inputGlowRadius)
        for((name,radius,opacity,intensity) in listOf(
            listOf("near",BicycleDesign.inputFocusNearBlur,BicycleDesign.inputFocusNearAlpha,BicycleDesign.inputFocusNearIntensity),
            listOf("outer",BicycleDesign.inputFocusOuterBlur,BicycleDesign.inputFocusOuterAlpha,BicycleDesign.inputFocusOuterIntensity))) {
            val row=spec.getJSONObject(name as String)
            assertEquals(row.getDouble("radius"),(radius as Float).toDouble(),1e-7)
            assertEquals(row.getDouble("opacity"),(opacity as Float).toDouble(),1e-7)
            assertEquals(row.getDouble("intensity"),(intensity as Float).toDouble(),1e-7)
        }
        for((name,stops) in listOf("outline" to BicycleInputStyle.outline,"fill" to BicycleInputStyle.fill)) {
            val rows=spec.getJSONObject("derived").getJSONArray(name)
            assertEquals(rows.length(),stops.size)
            for(i in stops.indices) {
                assertEquals(rows.getJSONObject(i).getDouble("position"),stops[i].position.toDouble(),1e-7)
                assertEquals(rows.getJSONObject(i).getDouble("opacity"),stops[i].opacity.toDouble(),1e-7)
            }
        }
        val colors=BicycleAccentGradient.palette(android.graphics.Color.parseColor(spec.getString("accent")),spec.getDouble("chromatic"))
        assertEquals(android.graphics.Color.parseColor("#e5b782"),colors.light)
        assertEquals(android.graphics.Color.parseColor("#5a2236"),colors.dark)
    }
    @Test fun boundThemeIdentitySurvivesSettingsProjectionAndSamePrimaryWithDifferentStrength() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=host.get();val themes=ThemeManager(activity.getSharedPreferences("input-style-test",0))
            themes.activate("preset:sunstruck-color");themes.setSettingsLight(true)
            val input=BicycleComponents.input(activity,themes.settings);activity.setContentView(input)
            assertEquals(1.5,BicycleInputStyle.strength(input),0.0)
            themes.activate("preset:sunstruck-dark")
            assertEquals(1.48,BicycleInputStyle.strength(input),0.0)
            assertEquals("preset:sunstruck-dark",input.getTag(R.id.input_theme_id))
            val stops=input.background.javaClass.getDeclaredField("stops").apply { isAccessible=true }.get(input.background) as IntArray
            assertArrayEquals(BicycleInputFrame.colors(themes.active.tokens.primary,1.48),stops)
            assertEquals(View.VISIBLE,input.visibility)
        } finally { host.pause().stop().destroy() }
    }
}
