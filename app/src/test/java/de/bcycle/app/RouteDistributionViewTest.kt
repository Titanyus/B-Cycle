package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    @Test fun zoneColoredBarsAndNeutralValuesStayVisibleWithGapsAndThemeChanges() {
        val host=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().visible()
        try {
            val context=host.get();val base=ThemeManager(context.getSharedPreferences("distribution-light",0))
            val themes=base.settings;themes.setSettingsLight(true)
            for(kind in listOf(AnalysisChart.POWER,AnalysisChart.HEART)) {
                val bars=List(10) { i -> DistributionBar(i*20.0,(i+1)*20.0,100.0) }
                val distribution=RouteValueDistribution(kind,0.0,200.0,List(10) { 100.0 },1000.0,
                    listOf(DistributionBand(1,50.0,200.0,android.graphics.Color.GREEN)),200.0,bars)
                val chart=RouteDistributionView(context,themes,distribution);context.setContentView(chart,ViewGroup.LayoutParams(720,720))
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                chart.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY));chart.layout(0,0,720,720)
                val bounds=chart.plotBounds()
                fun x(value:Double)=(bounds.left+bounds.width()*value/200).toInt()
                fun render(background:Int)=Bitmap.createBitmap(720,720,Bitmap.Config.ARGB_8888).also {
                    Canvas(it).apply { drawColor(background);chart.draw(this) }
                }
                for(lightMode in listOf(true,false,true)) {
                    themes.setSettingsLight(lightMode);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                    val background=if(lightMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    val image=render(background);val y=(bounds.top+bounds.height()*.6f).toInt()
                    assertTrue(kotlin.math.abs(ThemeValidator.luminance(image.getPixel(x(30.0),y))-ThemeValidator.luminance(background))>.1)
                    assertEquals(android.graphics.Color.GREEN,image.getPixel(x(130.0),y))
                    assertTrue(image.getPixel(x(46.0),y)!=android.graphics.Color.GREEN)
                    assertEquals(android.graphics.Color.GREEN,image.getPixel(x(54.0),y)) // Exact boundary within one equal-width bin.
                    assertEquals(background,image.getPixel(x(40.0),y)) // Empty gap, no curve or filled area.
                    assertEquals(background,image.getPixel(x(130.0),bounds.top.toInt()+2))
                    assertEquals(android.graphics.Color.GREEN,image.getPixel(x(130.0),(bounds.top-BicycleDesign.dp(context,BicycleDesign.controlGap)).toInt()))
                    image.recycle()
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun fiveHistoryChartsKeepExactlyTheirDarkModeColorsInLightMode() {
        val host=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().visible()
        try {
            val context=host.get();val base=ThemeManager(context.getSharedPreferences("history-colors",0))
            base.activate("preset:hellfire-dark");val themes=base.settings
            for(kind in AnalysisChart.entries) {
                val series=AnalysisSeries(kind,listOf(AnalysisPoint(0.0,0.0,80.0),AnalysisPoint(distance=10000.0,seconds=10.0,value=160.0)))
                val analysis=RouteAnalysis(ManagedRoute("colors","Test",true,"FIT",RouteDetails(),false),emptyMap(),null,true,10.0,listOf(series))
                themes.setSettingsLight(false)
                val chart=RouteChartView(context,themes,analysis,series)
                context.setContentView(chart,ViewGroup.LayoutParams(720,360))
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                chart.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY));chart.layout(0,0,720,360)
                fun color():Int {
                    val bitmap=Bitmap.createBitmap(720,360,Bitmap.Config.ARGB_8888)
                    chart.draw(Canvas(bitmap));val pixel=bitmap.getPixel(((chart.plotLeft+chart.plotRight)/2).toInt(),200)
                    bitmap.recycle();return pixel
                }
                val expected=kind.color(base.active.tokens.secondary)
                assertEquals(expected,color())
                themes.setSettingsLight(true);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                assertEquals(expected,color())
            }
        } finally { host.pause().stop().destroy() }
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
                        // Zone colours belong to the legend and bars, never a full plot backdrop.
                        assertEquals(band.color,bitmap.getPixel(x,(plot.top-BicycleDesign.dp(context,BicycleDesign.controlGap)).toInt()))
                        assertEquals(0,Color.alpha(bitmap.getPixel(x,plot.top.toInt()+5)))
                    }
                    bitmap.recycle()
                }
                if (theme.id == "preset:hellfire-dark" && width == 960) {
                    val gap = BicycleDesign.dp(context, BicycleDesign.smallGap)
                    val preview = Bitmap.createBitmap(charts.first().width, charts.sumOf { it.height } + gap, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(preview); canvas.drawColor(theme.tokens.secondary)
                    charts.forEach { chart -> chart.draw(canvas); canvas.translate(0f, chart.height + gap.toFloat()) }
                    java.io.File("build/reports/ui/track-distributions.png").apply { parentFile!!.mkdirs() }.outputStream().use { preview.compress(Bitmap.CompressFormat.PNG, 100, it) }
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
