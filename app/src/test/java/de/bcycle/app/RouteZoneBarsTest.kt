package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteZoneBarsTest {
    private fun all(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun measure(v: View, width: Int, height: Int = 1800) {
        v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, width, height)
    }
    @Test fun barsHaveFullWidthFaintTrackAndProportionalOpaqueForegroundIncludingZero() {
        val context = RuntimeEnvironment.getApplication()
        for (percent in listOf(0.0, 25.0, 100.0)) {
            val color = ZoneGlowColors.powerColors[1]
            val bar = RouteZoneBar(context, color, percent); measure(bar, 400, 20)
            val bitmap = Bitmap.createBitmap(400, 20, Bitmap.Config.ARGB_8888)
            bar.draw(Canvas(bitmap))
            assertEquals((400 * percent / 100).toFloat(), bar.foregroundWidth(), .001f)
            if (percent > 0) assertEquals(color, bitmap.getPixel(10, 10))
            assertEquals(if (percent < 100) (255 * BicycleDesign.analysisZoneTrackAlpha).toInt() else 255, Color.alpha(bitmap.getPixel(390, 10)))
            bitmap.recycle()
        }
    }
    @Test fun bothDistributionsStayInsideTheirGroupsAcrossThemesAndPortraitWidths() {
        val context = RuntimeEnvironment.getApplication()
        val themes = ThemeManager(context.getSharedPreferences("zone-bars", 0))
        val analysis = RouteAnalysis(ManagedRoute("zones", "Fahrt", true, "FIT", RouteDetails(), false),
            mapOf(AnalysisMetric.AVG_POWER to 200.0, AnalysisMetric.AVG_HEART to 130.0), null, true, 1.0, emptyList(),
            zones = RouteZoneDistributions(ZoneDistanceDistribution(listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)), ZoneDistanceDistribution(listOf(1.0, 2.0, 3.0, 4.0, 5.0))))
        val screen = RouteAnalysisView(context, themes, "zones") {}
        for (theme in BicycleThemes.presets) {
            themes.activate(theme.id); screen.show(analysis)
            for (width in listOf(720, 960)) {
                measure(screen, width)
                val power = screen.findViewWithTag<RouteZoneBars>("analysis_zones_${R.string.analysis_power_group}")
                val heart = screen.findViewWithTag<RouteZoneBars>("analysis_zones_${R.string.analysis_cadence_heart_group}")
                assertEquals(ZoneGlowColors.powerColors, all(power).filterIsInstance<RouteZoneBar>().map { it.zoneColor })
                assertEquals(TrainingZoneColors.values, all(heart).filterIsInstance<RouteZoneBar>().map { it.zoneColor })
                assertEquals(7, power.childCount); assertEquals(5, heart.childCount)
                for (group in listOf(power, heart)) {
                    for (text in all(group).filterIsInstance<TextView>()) {
                        assertEquals(themes.active.tokens.foreground, text.currentTextColor)
                        val rect = android.graphics.Rect(0, 0, text.width, text.height)
                        group.offsetDescendantRectToMyCoords(text, rect)
                        assertTrue("${text.text}: $rect", rect.left >= 0 && rect.right <= group.width && rect.top >= 0 && rect.bottom <= group.height)
                        assertTrue(text.layout.getEllipsisCount(0) == 0)
                    }
                    assertTrue(all(group).filterIsInstance<RouteZoneBar>().all { it.width > 0 })
                    assertTrue((group.getChildAt(0) as ViewGroup).contentDescription.toString().contains(context.getString(R.string.analysis_zone_label, 1).takeLast(1)))
                }
            }
        }
        screen.show(analysis.copy(zones = RouteZoneDistributions()))
        assertTrue(all(screen).none { it is RouteZoneBars })
        screen.dispose()
    }
}
