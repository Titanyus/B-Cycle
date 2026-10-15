package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
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
class RouteDistributionViewTest {
    private fun all(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun measure(v: View, width: Int) {
        v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.EXACTLY)); v.layout(0, 0, width, 1800)
    }
    @Test fun squareDistributionsFollowExistingChartsAcrossThemesAndPortraitWidths() {
        val context = RuntimeEnvironment.getApplication()
        val themes = ThemeManager(context.getSharedPreferences("distribution-view", 0))
        val accumulator = RouteZoneAccumulator(RiderProfile(ftpW = 200, restingHeartRate = 60, maximumHeartRate = 185))
        val random = java.util.Random(42)
        for (i in 0..2400) accumulator.add(i * 1000L, i * 10.0,
            kotlin.math.round(170 + 35 * random.nextGaussian()).coerceAtLeast(0.0),
            kotlin.math.round(145 + 12 * random.nextGaussian()).coerceIn(60.0, 185.0))
        val distributions = accumulator.timeDistributions()
        val entry = ManagedRoute("curves", "Fahrt", true, "FIT", RouteDetails(), false)
        val analysis = RouteAnalysis(entry, emptyMap(), null, true, 6.0,
            listOf(AnalysisSeries(AnalysisChart.POWER, listOf(AnalysisPoint(0.0, 0.0, 200.0)))), distributions = distributions)
        val screen = RouteAnalysisView(context, themes, "curves") {}
        for (theme in BicycleThemes.presets) {
            themes.activate(theme.id); screen.show(analysis)
            for (width in listOf(720, 960)) {
                measure(screen, width)
                val charts = all(screen).filterIsInstance<RouteDistributionView>()
                assertEquals(listOf(AnalysisChart.POWER, AnalysisChart.HEART), charts.map { it.distribution.chart })
                charts.forEach { chart ->
                    assertEquals(chart.width, chart.height)
                    assertTrue(chart.plotBounds().width() > chart.width * .75f)
                    assertTrue(chart.contentDescription.toString().contains(context.getString(R.string.analysis_watts)) || chart.contentDescription.toString().contains(context.getString(R.string.analysis_bpm)))
                    val bitmap = Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)
                    chart.draw(Canvas(bitmap))
                    val plot = chart.plotBounds()
                    for (band in chart.distribution.bands) {
                        val value = (band.lower + band.upper) / 2
                        val x = (plot.left + ((value - chart.distribution.minimum) / (chart.distribution.maximum - chart.distribution.minimum)) * plot.width()).toInt()
                        // Above the curve/fill/glow: both heights retain the exact opaque zone color.
                        assertEquals(band.color, bitmap.getPixel(x, plot.top.toInt() + 5))
                        assertEquals(band.color, bitmap.getPixel(x, plot.top.toInt() + 15))
                    }
                    bitmap.recycle()
                }
                if (theme.id == "preset:black-ops" && width == 960) {
                    val gap = BicycleDesign.dp(context, BicycleDesign.smallGap)
                    val preview = Bitmap.createBitmap(charts.first().width, charts.sumOf { it.height } + gap, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(preview); canvas.drawColor(theme.tokens.secondary)
                    charts.forEach { chart -> chart.draw(canvas); canvas.translate(0f, chart.height + gap.toFloat()) }
                    java.io.File("/tmp/bcycle-track-distributions.png").outputStream().use { preview.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    preview.recycle()
                }
                val body = screen.getChildAt(0) as ViewGroup
                assertTrue((0 until body.childCount).single { body.getChildAt(it) is RouteChartView } < body.indexOfChild(charts.first()))
            }
        }
        screen.show(analysis.copy(distributions = emptyList()))
        assertTrue(all(screen).none { it is RouteDistributionView })
        screen.dispose()
    }
}
