package de.bcycle.app

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
class BestEffortsViewTest {
    private fun all(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun measure(v: View, width: Int) {
        v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, width, 1800)
    }
    @Test fun elevationIsSeparateAndEffortsAreCompactResourceBackedAndUseCurrentProfileWeight() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("bike", 0)
        RiderProfileStore(prefs).write(RiderProfile(weightKg = 80.0))
        val themes = ThemeManager(prefs)
        val entry = ManagedRoute("efforts", "Fahrt", true, "FIT", RouteDetails(), false)
        val analysis = RouteAnalysis(entry, mapOf(AnalysisMetric.DISTANCE to 50.0, AnalysisMetric.ASCENT to 800.0, AnalysisMetric.DESCENT to 700.0),
            null, true, 50.0, listOf(AnalysisSeries(AnalysisChart.POWER, listOf(AnalysisPoint(0.0, 0.0, 200.0)))),
            BestEfforts.select(BestEfforts.durationsMs.associateWith { 200.0 }))
        val screen = RouteAnalysisView(context, themes, entry.id) {}
        screen.show(analysis)
        for (width in listOf(720, 960)) {
            measure(screen, width)
            val tiles = all(screen).filterIsInstance<BestEffortTile>()
            assertEquals(4, tiles.size)
            assertEquals(listOf(480000L,1200000L,3600000L,7200000L).map { "analysis_effort_$it" },tiles.map { it.tag })
            assertTrue(tiles.maxOf { it.width } - tiles.minOf { it.width } <= 1)
            for (tile in tiles) {
                assertTrue(all(tile).filterIsInstance<TextView>().any { it.text == context.getString(R.string.analysis_effort_relative, context.getString(R.string.analysis_ratio_format, 2.5)) })
                all(tile).filterIsInstance<TextView>().forEach { text ->
                    val bounds = android.graphics.Rect(0, 0, text.width, text.height)
                    tile.offsetDescendantRectToMyCoords(text, bounds)
                    assertTrue("${text.text}: $bounds", bounds.left >= 0 && bounds.right <= tile.width && bounds.top >= 0 && bounds.bottom <= tile.height)
                }
            }
            val heightGroup = screen.findViewWithTag<ViewGroup>("analysis_group_${R.string.analysis_elevation}")
            val heightTiles = all(heightGroup).filterIsInstance<AnalysisMetricTile>()
            assertEquals(listOf(listOf(AnalysisMetric.ASCENT), listOf(AnalysisMetric.DESCENT)), heightTiles.map { it.alternatives })
            assertTrue(heightTiles.none { it.isClickable })
            val body = screen.getChildAt(0) as ViewGroup
            val bestIndex = (0 until body.childCount).single { body.getChildAt(it).tag == "analysis_best_efforts" }
            val chartIndex = (0 until body.childCount).single { body.getChildAt(it) is RouteChartView }
            assertTrue(bestIndex < chartIndex)
        }
        RiderProfileStore(prefs).write(RiderProfile())
        screen.show(analysis); measure(screen, 960)
        all(screen).filterIsInstance<BestEffortTile>().forEach { tile ->
            assertTrue(all(tile).filterIsInstance<TextView>().any { it.text == context.getString(R.string.analysis_effort_relative, context.getString(R.string.analysis_missing)) })
        }
        screen.show(analysis.copy(bestEfforts = emptyList()))
        assertNull(screen.findViewWithTag<View>("analysis_best_efforts"))
        screen.dispose()
    }
}
