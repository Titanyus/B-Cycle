package de.bcycle.app

import android.graphics.Color
import android.content.Context
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
class RouteAnalysisUpdateTest {
    private fun all(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun measure(v: View, width: Int, height: Int) { v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)); v.layout(0, 0, width, height) }
    @Test fun transparentTilesUseSharedTranslucentWhiteUnitsAndCenterCombinedGlyphsAtAllWidths() {
        val app = RuntimeEnvironment.getApplication()
        val themes = ThemeManager(app.getSharedPreferences("bike", 0))
        for (fontScale in listOf(1f, 1.5f)) {
            val context = app.createConfigurationContext(android.content.res.Configuration(app.resources.configuration).apply { this.fontScale = fontScale })
            for (metric in AnalysisMetric.entries) for (width in listOf(230, 400)) {
                val tile = AnalysisMetricTile(context, themes, listOf(metric), mapOf(metric to 129.25))
                measure(tile, width, 500)
                assertNull(tile.background)
                val row = all(tile).filterIsInstance<AnalysisValueRow>().single()
                val children = (0 until row.childCount).map { row.getChildAt(it) as TextView }
                val number = children[0]; val unit = children[1]
                assertEquals(BicycleDesign.dashboardUnitColor, unit.currentTextColor)
                assertEquals(Color.argb(166, 255, 255, 255), unit.currentTextColor)
                if (unit.visibility == View.VISIBLE) {
                    assertEquals(number.top + number.height / 2.0, unit.top + unit.height / 2.0, 1.0)
                    val numberWidth = number.layout.getLineWidth(0)
                    val unitWidth = unit.layout.getLineWidth(0)
                    val start = number.left + (number.width - numberWidth) / 2
                    val end = unit.left + (unit.width + unitWidth) / 2
                    assertEquals(row.width / 2f, (start + end) / 2, 1f)
                    assertTrue(start >= -1 && end <= row.width + 1)
                    assertTrue(unit.textSize < number.textSize)
                }
            }
        }
    }
    @Test fun ebikeMeansAreTimeWeightedPairedAndBatteryIsConsumptionNotRemainder() {
        val m = RideAnalysisMeans()
        m.add(0, 100.0, 200.0, 80.0); m.add(1000, 300.0, 600.0, 79.0); m.add(4000, null, null, 62.0)
        assertEquals(250.0, m.riderMean!!, .001); assertEquals(500.0, m.motorMean!!, .001)
        assertEquals(.5, m.pairedRatio!!, .001); assertEquals(18.0, m.batteryUsage!!, .001)
        m.add(5000, null, 800.0, null); m.add(6000, null, null, 62.0)
        assertEquals(.5, m.pairedRatio!!, .001)
        val missing = RideAnalysisMeans(); missing.add(0, null, null, null); missing.add(120000, null, null, 80.0); missing.add(121000, null, null, 70.0)
        assertNull(missing.batteryUsage)
        val charged = RideAnalysisMeans(); charged.add(0, 200.0, 0.0, 40.0); charged.add(1000, null, null, 50.0)
        assertNull(charged.batteryUsage); assertNull(charged.pairedRatio)
    }
    @Test fun groupOrderAndPerformanceTripletAndPowerPlaceholdersAreCentralAndResourceBacked() {
        val context = RuntimeEnvironment.getApplication(); val themes = ThemeManager(context.getSharedPreferences("bike", 0))
        assertEquals(listOf(R.string.analysis_overview, R.string.analysis_ebike, R.string.analysis_cadence_heart_group,
            R.string.analysis_power_group, R.string.analysis_energy_group, R.string.analysis_performance_group), AnalysisGroups.groups.map { it.first })
        assertEquals(listOf(AnalysisMetric.FTP, AnalysisMetric.EF, AnalysisMetric.RATIO), AnalysisGroups.groups.last().second.flatten())
        val entry = ManagedRoute("id", "Ride", true, "FIT", RouteDetails(1_791_626_400_000), false)
        val analysis = RouteAnalysis(entry, mapOf(AnalysisMetric.MAX_POWER to 300.0, AnalysisMetric.AVG_MOTOR to 150.0), null, true, 1.0, emptyList())
        val screen = RouteAnalysisView(context, themes, "id") {}; screen.show(analysis); measure(screen, 960, 1800)
        val power = all(screen).filterIsInstance<AnalysisMetricTile>().filter { it.alternatives.first() in listOf(AnalysisMetric.AVG_POWER, AnalysisMetric.MAX_POWER, AnalysisMetric.NP) }
        assertEquals(3, power.size)
        assertTrue(power.filter { it.alternatives.first() != AnalysisMetric.MAX_POWER }.all { all(it).filterIsInstance<TextView>().any { v -> v.text.toString() == context.getString(R.string.analysis_missing) } })
        assertEquals(2, all(screen).count { it.tag == "analysis_vertical_divider" })
        val date = screen.findViewWithTag<TextView>("analysis_date"); assertEquals(android.view.Gravity.CENTER, date.gravity)
    }
}
