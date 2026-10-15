package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteAnalysisViewTest {
    private fun descendants(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { descendants(v.getChildAt(it)) } else emptyList()
    private fun measure(v: View, w: Int, h: Int) {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)); v.layout(0, 0, w, h)
    }
    private fun sample() = RouteAnalysis(ManagedRoute("id", "An unusually long ride title that should remain readable", true, "FIT", RouteDetails(1_791_626_400_000), false),
        AnalysisMetric.entries.associateWith { when (it) {
            AnalysisMetric.MOVING -> 3600.0; AnalysisMetric.ELAPSED -> 4200.0; AnalysisMetric.DISTANCE -> 32.1
            AnalysisMetric.EF, AnalysisMetric.RATIO -> .21; else -> 129.0
        } }, null, true, 32.1, AnalysisChart.entries.map { chart ->
            AnalysisSeries(chart, listOf(AnalysisPoint(0.0, 0.0, 5.0), AnalysisPoint(16000.0, 20.0, null), AnalysisPoint(32100.0, 40.0, 20.0)))
        })
    @Test fun allThemesHaveStableCompactGridAndFixedChartOrderAndAlignedAxesOnNarrowScreens() {
        val controller = Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val context = controller.get(); val themes = ThemeManager(context.getSharedPreferences("bike", 0)); val sample = sample()
            val content = RouteAnalysisView(context, themes, "id") {}; content.show(sample); context.setContentView(content)
            var geometry: List<Pair<Int, Int>>? = null
            for (theme in BicycleThemes.presets) {
                themes.activate(theme.id)
                for (width in listOf(960, 1440)) {
                    measure(content, width, 1800)
                    val rows = descendants(content).filterIsInstance<LinearLayout>().filter { it.tag?.toString()?.startsWith("analysis_group_") == true }
                    assertEquals(listOf(R.string.analysis_overview,R.string.analysis_elevation,R.string.analysis_ebike,R.string.analysis_cadence_heart_group,
                        R.string.analysis_power_group,R.string.analysis_energy_group,R.string.analysis_performance_group).map { "analysis_group_$it" },rows.map { it.tag })
                    assertEquals(listOf(3, 2, 3, 2, 3, 2, 3), rows.map { row -> (0 until row.childCount).count { row.getChildAt(it) is AnalysisMetricTile } })
                    rows.forEach { row ->
                        assertTrue((0 until row.childCount).count { row.getChildAt(it) is AnalysisMetricTile } <= 4)
                        val children = (0 until row.childCount).map(row::getChildAt).filterIsInstance<AnalysisMetricTile>()
                        assertEquals(1, children.map { it.height }.distinct().size)
                        assertTrue(children.all { it.height > 0 && it.right <= row.width })
                    }
                    val charts = descendants(content).filterIsInstance<RouteChartView>()
                    assertEquals(AnalysisChart.entries, charts.map { it.series.chart })
                    assertEquals(1, charts.map { it.plotLeft }.distinct().size)
                    assertEquals(1, charts.map { it.plotRight }.distinct().size)
                    assertEquals(1, charts.map { it.screenX(16.0) }.distinct().size)
                    assertEquals(1, charts.map { it.height }.distinct().size)
                    assertTrue(charts.all { it.contentDescription.toString().contains(context.getString(it.series.chart.detailLabel)) })
                    for (chart in AnalysisChart.entries) assertTrue("${theme.name}/${chart.name}", ThemeValidator.contrast(chart.color(theme.tokens.secondary), theme.tokens.secondary) >= 3)
                    for (gray in listOf(60, 100, 128, 160, 200)) {
                        val background = android.graphics.Color.rgb(gray, gray, gray)
                        for (chart in AnalysisChart.entries) assertTrue(ThemeValidator.contrast(chart.color(background), background) >= 3)
                    }
                    val current = rows.map { it.width to it.height }
                    if (width == 960) { if (geometry == null) geometry = current else assertEquals(geometry, current) }
                    // Draw the full prepared body too: offscreen charts must render without scrolling-time work.
                    val body = content.getChildAt(0)
                    val bitmap = Bitmap.createBitmap(body.width, body.height, Bitmap.Config.ARGB_8888)
                    body.draw(Canvas(bitmap)); bitmap.recycle()
                }
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun sectionLabelsAreVerticalWithThinSeparatorsAndDateIsLargerWithoutExtraHeadings() {
        val context=RuntimeEnvironment.getApplication();val themes=ThemeManager(context.getSharedPreferences("analysis-side",0))
        val content=RouteAnalysisView(context,themes,"side") {}
        content.show(sample());measure(content,360,800)
        val labels=descendants(content).filterIsInstance<AnalysisSideLabel>()
        assertEquals(AnalysisGroups.groups.map { context.getString(it.first) },labels.map { it.title })
        labels.forEach { assertTrue(it.isAccessibilityHeading);assertEquals(BicycleDesign.dp(context,BicycleDesign.analysisSideLabelWidth),it.width);assertTrue(it.height>=BicycleDesign.dp(context,BicycleDesign.analysisTileMinHeight)) }
        val dividers=descendants(content).filter { it.tag=="analysis_section_divider" }
        assertEquals(6,dividers.size);assertTrue(dividers.all { it.height==BicycleDesign.dividerPixels && it.alpha==BicycleDesign.dividerAlpha })
        dividers.forEach {
            val margins=it.layoutParams as LinearLayout.LayoutParams
            assertEquals(BicycleDesign.dp(context,BicycleDesign.analysisDividerGap),margins.topMargin)
            assertEquals(margins.topMargin,margins.bottomMargin)
        }
        val date=content.findViewWithTag<TextView>("analysis_date")
        assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,BicycleDesign.bodySp,context.resources.displayMetrics),date.textSize,0f)
        val texts=descendants(content).filterIsInstance<TextView>().map { it.text.toString() }
        for(resource in listOf(R.string.analysis_overview,R.string.analysis_map_heading,R.string.analysis_charts)) assertFalse(texts.contains(context.getString(resource)))
        assertEquals("Leistung",context.getString(AnalysisChart.POWER.detailLabel))
        content.draw(android.graphics.Canvas(android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888)))
    }
    @Test fun largeSystemFontKeepsNarrowTilesAndHeaderWithinTheirAllocatedBounds() {
        val app = RuntimeEnvironment.getApplication()
        val config = android.content.res.Configuration(app.resources.configuration).apply { fontScale = 1.5f }
        val context = app.createConfigurationContext(config)
        val themes = ThemeManager(app.getSharedPreferences("bike", 0))
        val content = RouteAnalysisView(context, themes, "id") {}; content.show(sample()); measure(content, 960, 1800)
        descendants(content).filterIsInstance<AnalysisMetricTile>().forEach { tile ->
            descendants(tile).filterIsInstance<TextView>().forEach { text ->
                val bounds = android.graphics.Rect(0, 0, text.width, text.height)
                tile.offsetDescendantRectToMyCoords(text, bounds)
                assertTrue("${text.text}: $bounds in ${tile.height}", bounds.top >= 0 && bounds.bottom <= tile.height)
                assertTrue(bounds.left >= 0 && bounds.right <= tile.width)
            }
        }
        val header = analysisHeader(context, themes, sample().entry) {}
        measure(header, 960, BicycleDesign.dp(context, BicycleDesign.headerHeight))
        val title = header.findViewWithTag<TextView>("analysis_title")
        assertNull(header.findViewWithTag<TextView>("analysis_date"))
        assertTrue(title.height > 0 && title.bottom <= header.height)
        val date = content.findViewWithTag<TextView>("analysis_date")
        assertEquals(android.view.Gravity.CENTER, date.gravity)
        assertEquals(BicycleDesign.dp(context, BicycleDesign.headerHeight), header.height)
    }
    @Test fun tileSwitchesValueUnitLabelAndAccessibleActionWithoutChangingGeometry() {
        val context = RuntimeEnvironment.getApplication(); val themes = ThemeManager(context.getSharedPreferences("bike", 0))
        val a = sample()
        for (alternatives in AnalysisGroups.groups.flatMap { it.second }.filter { it.size > 1 }) {
            val tile = AnalysisMetricTile(context, themes, alternatives, a.metrics); measure(tile, 240, 420)
            val bounds = tile.width to tile.height
            val label = descendants(tile).filterIsInstance<TextView>().single { it.text.toString() == context.getString(alternatives[0].label) }
            assertTrue(tile.contentDescription.toString().contains(context.getString(alternatives[1].label)))
            tile.performClick(); measure(tile, 240, 420)
            assertEquals(context.getString(alternatives[1].label), label.text.toString()); assertEquals(bounds, tile.width to tile.height)
            assertTrue(tile.contentDescription.toString().contains(context.getString(alternatives[0].label)))
        }
        val only = AnalysisMetricTile(context, themes, listOf(AnalysisMetric.ELAPSED), a.metrics)
        assertFalse(only.isClickable)
    }
    @Test fun unavailableGroupsAndChartsLeaveNoEmptyRowsAndNewLabelsAreResourceBacked() {
        val context = RuntimeEnvironment.getApplication(); val themes = ThemeManager(context.getSharedPreferences("bike", 0))
        val a = sample().copy(metrics = mapOf(AnalysisMetric.DISTANCE to 4.0), series = emptyList())
        val content = RouteAnalysisView(context, themes, "id") {}; content.show(a); measure(content, 960, 1800)
        assertEquals(1, descendants(content).count { it is AnalysisMetricTile })
        assertFalse(descendants(content).any { it is RouteChartView })
        for (metric in AnalysisMetric.entries) {
            assertEquals("string", context.resources.getResourceTypeName(metric.label)); assertFalse(context.getString(metric.label).isBlank())
            assertEquals("string", context.resources.getResourceTypeName(metric.unit)); if (metric != AnalysisMetric.RIDER_BIKE_RATIO) assertFalse(context.getString(metric.unit).isBlank())
        }
        AnalysisGroups.groups.forEach { assertEquals("string", context.resources.getResourceTypeName(it.first)) }
        assertEquals("Allgemeine Daten",context.getString(R.string.analysis_overview))
        assertEquals("Kadenz & Puls",context.getString(R.string.analysis_cadence_heart_group))
        assertEquals("Leistung",context.getString(R.string.analysis_power_group))
        assertEquals("Ø Tempo",context.getString(AnalysisMetric.AVG_SPEED.label))
        assertEquals("Ø Puls",context.getString(AnalysisMetric.AVG_HEART.label))
        assertEquals("Kadenz",context.getString(AnalysisChart.CADENCE.label))
        assertEquals("Normalisierte Leistung",context.getString(AnalysisMetric.NP.label))
        AnalysisChart.entries.forEach { assertEquals("string", context.resources.getResourceTypeName(it.label)) }
        assertEquals("11.10.2026 · 14:40 h", java.time.format.DateTimeFormatter.ofPattern(context.getString(R.string.analysis_date_format)).withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.ofEpochMilli(1_791_729_600_000)))
    }
    @Test fun fullscreenMapSurvivesHostRehousingAndReleasesResourcesOnlyWhenClosed() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            val route = GpxRoute(listOf(MapPoint(50.0, 8.0, true), MapPoint(50.01, 8.01)), 2, 1)
            val analysis = sample().copy(route = route)
            MainActivity::class.java.getDeclaredMethod("routeAnalysisMap", RouteAnalysis::class.java).apply { isAccessible = true }.invoke(activity, analysis)
            shadowOf(Looper.getMainLooper()).idle()
            val screen = ShadowAlertDialog.getLatestAlertDialog()
            measure(screen.window!!.decorView, 1080, 2000)
            val map = descendants(screen.window!!.decorView).filterIsInstance<RouteAnalysisMap>().single()
            val destroyed = RouteAnalysisMap::class.java.getDeclaredField("destroyed").apply { isAccessible = true }
            assertFalse(destroyed.getBoolean(map))
            assertTrue(map.isAttachedToWindow)
            assertTrue("map size ${map.width}x${map.height}", map.width > 0 && map.height > 600)
            assertTrue(map.map.width > 0 && map.map.height > 600)
            assertTrue(map.map.layerManager.layers[1] is RouteTrackLayer)
            val zoom = map.map.model.mapViewPosition.zoomLevel
            descendants(map).single { it.contentDescription == activity.getString(R.string.analysis_zoom_in) }.performClick()
            assertEquals((zoom + 1).toByte(), map.map.model.mapViewPosition.zoomLevel)
            descendants(map).single { it.contentDescription == activity.getString(R.string.analysis_zoom_out) }.performClick()
            assertEquals(zoom, map.map.model.mapViewPosition.zoomLevel)
            val center = map.map.model.mapViewPosition.center
            map.map.model.mapViewPosition.moveCenter(100.0, 0.0)
            assertNotEquals(center, map.map.model.mapViewPosition.center)
            descendants(map).single { it.contentDescription == activity.getString(R.string.analysis_fit_route) }.performClick()
            assertEquals(center, map.map.model.mapViewPosition.center)
            assertNotNull(screen.window!!.decorView.findViewWithTag<View>("bicycle_fullscreen"))
            testBack(screen)
            assertTrue(destroyed.getBoolean(map))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun fullscreenDetailsReturnToSameFilteredManagerAndActionsDoNotOpenDetails() {
        val app = RuntimeEnvironment.getApplication()
        val gpx = GpxLibrary(app.filesDir)
        gpx.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(), "Duplicate.gpx")
        val first = gpx.entries.single().id
        gpx.add("<gpx><rte><rtept lat='51' lon='8'/><rtept lat='51.01' lon='8.01'/></rte></gpx>".byteInputStream(), "Duplicate.gpx")
        val second = gpx.entries.last().id
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            descendants(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            descendants(ShadowAlertDialog.getLatestAlertDialog().window!!.decorView).single { it.tag == "settings_ROUTES" }.performClick()
            val manager = ShadowAlertDialog.getLatestAlertDialog(); val state = RideRuntime.get(activity).mapState
            val until = System.nanoTime() + 10_000_000_000
            while (state.busy && System.nanoTime() < until) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
            assertFalse(state.busy)
            measure(manager.window!!.decorView, 1080, 2000)
            val filter = manager.window!!.decorView.findViewWithTag<Spinner>("route_filter")
            filter.setSelection(2); shadowOf(Looper.getMainLooper()).idle(); measure(manager.window!!.decorView, 1080, 2000)
            val list = manager.window!!.decorView.findViewWithTag<ListView>("route_list")
            assertEquals(2, list.adapter.count)
            val card = list.adapter.getView((0 until list.adapter.count).single { (list.adapter.getItem(it) as ManagedRoute).id == second }, null, list)
            val selectedBefore = state.selectedRoute
            card.findViewWithTag<View>("route_eye_$second").performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertSame(manager, ShadowAlertDialog.getLatestAlertDialog())
            assertNull(card.findViewWithTag<View>("route_more_$second"))
            card.performClick(); shadowOf(Looper.getMainLooper()).idle()
            val details = ShadowAlertDialog.getLatestAlertDialog()
            assertNotSame(manager, details)
            assertNotNull(details.window!!.decorView.findViewWithTag<View>("route_analysis_$second"))
            assertNull(details.window!!.decorView.findViewWithTag<View>("route_analysis_$first"))
            measure(details.window!!.decorView, 1080, 2000)
            assertEquals(BicycleDesign.dp(activity, BicycleDesign.headerHeight), details.window!!.decorView.findViewWithTag<View>("bicycle_navigation_header").height)
            assertTrue(descendants(details.window!!.decorView).joinToString { "${it.javaClass.simpleName}:${it.tag}" }, details.window!!.decorView.findViewWithTag<View>("bicycle_fullscreen") != null)
            assertEquals(descendants(details.window!!.decorView).filterIsInstance<ScrollView>().joinToString { "${it.tag} shown=${it.isShown} parent=${(it.parent as View).tag}" }, 1, descendants(details.window!!.decorView).count { it is ScrollView && it.isShown && it.tag != "bicycle_fullscreen_footer" })
            // Closing immediately also exercises cancellation while the shared worker is still decoding.
            testBack(details); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            assertEquals(selectedBefore, state.selectedRoute)
            assertEquals(2, filter.selectedItemPosition); assertSame(list, manager.window!!.decorView.findViewWithTag("route_list"))
            card.performClick(); shadowOf(Looper.getMainLooper()).idle()
            val again = ShadowAlertDialog.getLatestAlertDialog()
            again.onBackPressed(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
}
