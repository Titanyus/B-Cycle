package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardViewTest {
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    private fun measure(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }

    @Test fun fiveValueTemplateHasTwoTopAndThreeBottomFieldsWithCompactCaptions() {
        val clicked = mutableListOf<Int>()
        val view = DashboardView(RuntimeEnvironment.getApplication()) { clicked += it }
        view.setTemplate(DashboardTemplate.FIVE)
        view.show(DashboardLayout.defaults.replace(0, DashboardField("GPS_MOVING_AVERAGE")), LiveData())
        measure(view, 1080, 750)
        val tiles = descendants(view).filter { it.isClickable }
        assertEquals(5, tiles.size)
        assertSame(tiles[0].parent, tiles[1].parent)
        assertSame(tiles[2].parent, tiles[3].parent)
        assertSame(tiles[3].parent, tiles[4].parent)
        assertNotSame(tiles[0].parent, tiles[2].parent)
        tiles.forEach { it.performClick() }; assertEquals((0..4).toList(), clicked)
        val texts = descendants(view).filterIsInstance<TextView>().map { it.text.toString() }
        assertTrue(texts.contains("\uFFFC Ø TEMPO · FAHRT"))
        assertTrue(texts.contains("\uFFFC TEMPO"))
        assertTrue(texts.contains("KADENZ"))
        assertFalse(texts.any { it == "\uFFFC KADENZ" })
        view.show(DashboardLayout.defaults.replace(0, DashboardField("GPS_ALTITUDE")), LiveData())
        assertEquals("HÖHE", descendants(view).filterIsInstance<TextView>().first().text.toString())
        view.show(DashboardLayout.defaults.replace(0, DashboardField("GPS_MOVING_AVERAGE")), LiveData())
        val caption = descendants(view).filterIsInstance<TextView>().first().text as android.text.Spanned
        assertEquals("GPS", caption.getSpans(0, 1, SourceIconSpan::class.java).single().source)
        assertTrue(tiles.first().contentDescription.toString().contains("GPS · Ø Geschwindigkeit · Bewegung"))
    }

    @Test fun heroPresetShowsOneLargeValueAboveThreeIndependentlySelectableValues() {
        val clicked = mutableListOf<Int>()
        val view = DashboardView(RuntimeEnvironment.getApplication()) { clicked += it }
        view.setTemplate(DashboardTemplate.HERO)
        measure(view, 1080, 900)
        val tiles = descendants(view).filter { it.isClickable }
        assertEquals(4, tiles.size)
        assertTrue(tiles.first().height > tiles.last().height * 1.9)
        assertSame(tiles[1].parent, tiles[2].parent); assertSame(tiles[2].parent, tiles[3].parent)
        tiles.forEach { it.performClick() }
        assertEquals(listOf(2, 3, 4, 5), clicked)
        assertEquals(1, tiles.map { ((it as ViewGroup).getChildAt(0) as TextView).textSize }.distinct().size)
    }

    @Test fun sevenAccessibleTilesFitPortraitAndLandscapeAndEveryTileCanBeSelected() {
        val clicked = mutableListOf<Int>()
        val view = DashboardView(RuntimeEnvironment.getApplication()) { clicked += it }
        val tiles = descendants(view).filter { it.isClickable }
        assertEquals(7, tiles.size)
        for ((index, tile) in tiles.withIndex()) { tile.performClick(); assertEquals(index, clicked.last()) }
        for ((width, height) in listOf(1080 to 1950, 1950 to 750, 720 to 960)) {
            measure(view, width, height)
            for (text in descendants(view).filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }) {
                assertTrue(text.width > 0 && text.height > 0)
                val layout = requireNotNull(text.layout)
                assertTrue("${text.text} at ${width}x$height: layout=${layout.height}, cell=${text.height}", layout.height <= text.height)
                for (line in 0 until layout.lineCount) assertTrue(layout.getLineWidth(line) <= text.width + 1)
            }
        }
    }

    @Test fun compactDashboardKeepsValuesCentredAndUnitsToTheirRightWithSpaceAboveToolbar() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val content = controller.get().findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val runtime = RideRuntime.get(controller.get())
        runtime.onData(LiveData(2530, 183, mapOf(LiveMetric.CADENCE to 170L, LiveMetric.BATTERY to 96L,
            LiveMetric.ASSIST_MODE to 2L, LiveMetric.ODOMETER to 14_791_937L, LiveMetric.MOTOR_POWER to 320L),
            listOf("OFF", "ECO", "TOUR+")), true)
        for ((width, height) in listOf(1080 to 2130, 2130 to 1080, 720 to 1280)) {
            measure(content, width, height)
            val dashboard = descendants(content).filterIsInstance<DashboardView>().single()
            assertEquals(0, dashboard.top)
            assertTrue(dashboard.height < (dashboard.parent as View).height)
            val tiles = descendants(dashboard).filter { it.isClickable }.map { it as ViewGroup }
            assertEquals(7, tiles.size)
            val captionSizes = tiles.map { (it.getChildAt(0) as TextView).textSize }
            assertEquals(1, captionSizes.distinct().size)
            for (tile in tiles) {
                val row = tile.getChildAt(1) as ViewGroup
                val value = row.getChildAt(0) as TextView
                val unit = row.getChildAt(1) as TextView
                assertEquals(row.width / 2.0, (value.left + value.right) / 2.0, 1.0)
                if (unit.visibility == View.VISIBLE) {
                    assertTrue("${value.text} ${unit.text}: unit outside tile", unit.right <= row.width)
                    assertTrue(unit.left >= row.width / 2f + value.layout.getLineWidth(0) / 2f)
                    assertEquals(row.height / 2.0, (unit.top + unit.bottom) / 2.0, 1.0)
                }
            }
        }
        controller.pause().stop().destroy()
    }

    @Test fun presetsShareReferenceTileHeightsAndSmallFontsInsteadOfStretchingToFillTheirArea() {
        val context = RuntimeEnvironment.getApplication()
        val fieldLayout = DashboardLayout.decode(List(DashboardLayout.FIELD_COUNT) { "RIDER_POWER" }.joinToString("|"))
        for ((width, available) in listOf(1080 to 2100, 720 to 1440, 1950 to 750)) {
            fun rendered(template: DashboardTemplate): Pair<DashboardView, List<ViewGroup>> {
                val view = DashboardView(context) {}
                view.setTemplate(template)
                view.show(fieldLayout, LiveData(riderPowerWatts = 83))
                measure(view, width, view.heightForViewport(width, available))
                return view to descendants(view).filter { it.isClickable }.map { it as ViewGroup }
            }
            val (reference, dataTiles) = rendered(DashboardTemplate.DATA)
            val small = dataTiles.minBy { it.height }
            val big = dataTiles.maxBy { it.height }
            val smallValue = (small.getChildAt(1) as ViewGroup).getChildAt(0) as TextView
            for (template in DashboardTemplate.entries) {
                val (view, tiles) = rendered(template)
                for (tile in tiles) {
                    val isBig = template.largeTop && tile === tiles.first() ||
                        template == DashboardTemplate.DATA && tile.height > small.height + 3
                    if (isBig && template.largeTop && width > available) {
                        assertTrue("Landscape hero retains a compact large field", tile.height < big.height && tile.height > small.height)
                    } else assertEquals("${template.name} at $width", (if (isBig) big.height else small.height).toDouble(), tile.height.toDouble(), 3.0)
                    assertEquals(small.paddingBottom, tile.paddingBottom)
                    val value = (tile.getChildAt(1) as ViewGroup).getChildAt(0) as TextView
                    if (!isBig) {
                        if (template.rows.any { it.size == 3 } && tile.paddingLeft == BicycleDesign.dp(context, BicycleDesign.tripleHorizontalPadding))
                            assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, BicycleDesign.tripleValueSp, context.resources.displayMetrics), value.textSize, .01f)
                        else assertEquals("${template.name}: font at $width", smallValue.textSize, value.textSize, 1f)
                    }
                    if (width == 1080 && template.largeTop && isBig) {
                        val referenceValue = (big.getChildAt(1) as ViewGroup).getChildAt(0) as TextView
                        assertEquals(referenceValue.textSize, value.textSize, 1f)
                    }
                }
                if (template != DashboardTemplate.DATA) assertTrue(view.height < available)
                if (width == 1080) {
                    val output = File("build/reports/dashboard/uniform-${template.name.lowercase()}.png").apply { parentFile!!.mkdirs() }
                    val bitmap = Bitmap.createBitmap(width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
            assertTrue(reference.height < available)
        }
    }

    @Test fun newPresetsHaveLargeTopAndTwoCompactRowsWithIndependentFieldSelection() {
        val context = RuntimeEnvironment.getApplication()
        for ((template, counts) in listOf(DashboardTemplate.HERO_SEVEN to listOf(1, 3, 3), DashboardTemplate.HERO_FIVE to listOf(1, 2, 2))) {
            val clicked = mutableListOf<Int>()
            val view = DashboardView(context) { clicked += it }
            view.setTemplate(template)
            measure(view, 720, view.heightForViewport(720, 1440))
            val rows = (0 until view.childCount).map { view.getChildAt(it) }.filterIsInstance<android.widget.LinearLayout>()
            assertEquals(counts, rows.map { row -> descendants(row).count { it.isClickable } })
            val tiles = descendants(view).filter { it.isClickable }
            assertTrue(tiles.first().height > tiles.last().height * 1.7)
            tiles.forEach { it.performClick() }
            assertEquals(template.slots, clicked)
            assertEquals(template, DashboardTemplate.decode(template.name))
        }
    }

    @Test fun ninthPresetShowsEightIndependentFieldsInTwoThreeThreeRows() {
        val clicked = mutableListOf<Int>()
        val context = RuntimeEnvironment.getApplication()
        val view = DashboardView(context) { clicked += it }
        view.setTemplate(DashboardTemplate.EIGHT)
        for ((width, available) in listOf(720 to 1440, 1080 to 2100, 1950 to 750)) {
            measure(view, width, view.heightForViewport(width, available))
            val rows = (0 until view.childCount).map { view.getChildAt(it) }.filterIsInstance<android.widget.LinearLayout>()
            assertEquals(listOf(2, 3, 3), rows.map { row -> descendants(row).count { it.isClickable } })
            val tiles = descendants(view).filter { it.isClickable }
            assertTrue(tiles.maxOf { it.height } - tiles.minOf { it.height } <= 1)
            assertTrue(view.height < available)
            clicked.clear(); tiles.forEach { it.performClick() }
            assertEquals((0 until DashboardLayout.FIELD_COUNT).toList(), clicked)
        }
        assertEquals(DashboardTemplate.EIGHT, DashboardTemplate.decode("EIGHT"))
    }

    @Test fun allPresetCaptionsKeepExactlyTheSameSmallerSizeAndUnitGrayEvenForLongNames() {
        val context = RuntimeEnvironment.getApplication()
        val expected = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,
            18f * .75f, context.resources.displayMetrics)
        for (template in DashboardTemplate.entries) {
            val view = DashboardView(context) {}
            view.setTemplate(template)
            for (field in listOf(DashboardField("CADENCE"), DashboardField("GPS_MOVING_AVERAGE"), DashboardField("MOTOR_POWER"))) {
                view.show(DashboardLayout.decode(List(DashboardLayout.FIELD_COUNT) { field.id }.joinToString("|")), LiveData())
                for ((width, height) in listOf(720 to 1440, 1080 to 2100, 1950 to 750)) {
                    measure(view, width, view.heightForViewport(width, height))
                    descendants(view).filter { it.isClickable }.forEach { tile ->
                        val caption = (tile as ViewGroup).getChildAt(0) as TextView
                        assertEquals("${template.name}: ${caption.text}", expected, caption.textSize, .01f)
                        assertEquals(BicycleDesign.dashboardUnitColor, caption.currentTextColor)
                        assertEquals(TextView.AUTO_SIZE_TEXT_TYPE_NONE, caption.autoSizeTextType)
                        assertTrue("${template.name}: ${caption.text} fits its caption area", caption.layout.height <= caption.height)
                    }
                }
            }
        }
    }

    @Test fun ninePreviewTilesKeepExistingOrderAndNewChoicesRemainIndependentPerPage() {
        val context = RuntimeEnvironment.getApplication()
        val initial = DashboardTemplate.entries.take(4)
        val picker = DashboardPresetPicker(context, initial, 0)
        measure(picker, 960, 1400)
        val tiles = descendants(picker).filter { it.tag?.toString()?.startsWith("preset_") == true && it.isClickable && !it.tag.toString().startsWith("preset_page_") }
        assertEquals(DashboardTemplate.entries.map { "preset_${it.name}" }, tiles.map { it.tag })
        tiles.forEach { assertNotNull(descendants(it).filterIsInstance<android.widget.ImageView>().single().drawable) }
        tiles.single { it.tag == "preset_HERO_SEVEN" }.performClick()
        assertEquals(DashboardTemplate.HERO_SEVEN, picker.selection[0])
        descendants(picker).single { it.tag == "preset_page_1" }.performClick()
        tiles.single { it.tag == "preset_HERO_FIVE" }.performClick()
        assertEquals(listOf(DashboardTemplate.HERO_SEVEN, DashboardTemplate.HERO_FIVE) + initial.drop(2), picker.selection)
        assertEquals(DashboardTemplate.entries.take(4), initial)
        descendants(picker).single { it.tag == "preset_page_2" }.performClick()
        tiles.single { it.tag == "preset_EIGHT" }.performClick()
        assertEquals(DashboardTemplate.EIGHT, picker.selection[2])
    }

    @Test fun threeColumnTilesHaveEqualWidthsFixedNumbersAndFitRequiredDistancesOnNarrowPhones() {
        val context=RuntimeEnvironment.getApplication()
        val pixels=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,BicycleDesign.tripleValueSp,context.resources.displayMetrics)
        for(template in DashboardTemplate.entries.filter { it.rows.any { row->row.size==3 } }) {
            val view=DashboardView(context) {};view.setTemplate(template)
            for(width in listOf(960,1080,1440)) {
                for(distance in listOf(99990.0,999900.0)) {
                    view.show(DashboardLayout.decode(List(DashboardLayout.FIELD_COUNT) { "GPS_DISTANCE" }.joinToString("|")),LiveData(),GpsValues(distanceM=distance))
                    measure(view,width,view.heightForViewport(width,2100))
                    for(row in (0 until view.childCount).map { view.getChildAt(it) }.filterIsInstance<android.widget.LinearLayout>()) {
                        val tiles=descendants(row).filter { it.isClickable }.map { it as ViewGroup }
                        if(tiles.size!=3) continue
                        assertTrue("${template.name}: equal three-column widths",tiles.maxOf { it.width }-tiles.minOf { it.width }<=1)
                        tiles.forEach { tile->
                            val values=tile.getChildAt(1) as ViewGroup;val number=values.getChildAt(0) as TextView;val unit=values.getChildAt(1) as TextView
                            assertEquals(pixels,number.textSize,.01f);assertEquals(TextView.AUTO_SIZE_TEXT_TYPE_NONE,number.autoSizeTextType)
                            assertEquals(if(distance<100000) "99,99" else "999,9",number.text.toString())
                            assertEquals(0,number.layout.getEllipsisCount(0));assertTrue(number.layout.getLineWidth(0)<=number.width+1)
                            assertTrue(number.left>=0 && unit.right<=values.width)
                            assertEquals(values.height/2.0,(unit.top+unit.bottom)/2.0,1.0)
                        }
                    }
                }
                // Longer time/speed strings at the larger fixed font need a wider viewport.
                if (width < 1440) continue
                view.show(DashboardLayout.decode(List(DashboardLayout.FIELD_COUNT) { "GPS_ELAPSED_TIME" }.joinToString("|")),LiveData(),GpsValues(elapsedMs=7830000))
                measure(view,width,view.heightForViewport(width,2100))
                for(tile in descendants(view).filter { it.isClickable }.map { it as ViewGroup }.filter { it.paddingLeft==BicycleDesign.dp(context,BicycleDesign.tripleHorizontalPadding) }) {
                    val row=tile.getChildAt(1) as ViewGroup;val number=row.getChildAt(0) as TextView;val unit=row.getChildAt(1) as TextView
                    assertEquals("2:10:30",number.text.toString());assertEquals("h",unit.text.toString())
                    assertEquals(pixels,number.textSize,.01f);assertEquals(0,number.layout.getEllipsisCount(0));assertTrue(unit.right<=row.width)
                }
                view.show(DashboardLayout.decode(List(DashboardLayout.FIELD_COUNT) { "GPS_SPEED" }.joinToString("|")),LiveData(),GpsValues(speedKmh=999.9,movingAverageKmh=30.0))
                measure(view,width,view.heightForViewport(width,2100))
                for(tile in descendants(view).filter { it.isClickable }.map { it as ViewGroup }.filter { it.paddingLeft==BicycleDesign.dp(context,BicycleDesign.tripleHorizontalPadding) }) {
                    val row=tile.getChildAt(1) as ViewGroup;val number=row.getChildAt(0) as TextView;val unit=row.getChildAt(1) as TextView
                    assertEquals(pixels,number.textSize,.01f);assertEquals(0,number.layout.getEllipsisCount(0))
                    assertTrue(number.left>=BicycleDesign.dp(context,12));assertTrue(unit.right<=row.width)
                }
            }
        }
    }

    @Test fun bikeSelectionLivesInSensorManagerAlongsidePulseSelection() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        descendants(controller.get().findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
        val menu = ShadowAlertDialog.getLatestAlertDialog()
        val items = descendants(menu.window!!.decorView)
        assertFalse(items.filterIsInstance<TextView>().any { it.text.toString() == "Fahrrad auswählen" })
        items.single { it.tag == "settings_SENSORS" }.performClick()
        val manager = descendants(ShadowAlertDialog.getLatestAlertDialog().window!!.decorView).filterIsInstance<TextView>().map { it.text.toString() }
        assertTrue(manager.contains("Fahrrad auswählen"))
        assertTrue(manager.contains("Sensor auswählen"))
        controller.pause().stop().destroy()
    }

    @Test fun realAndroidRenderUsesPowerDistinctFromMotorAndShrinksLongValues() {
        val view = DashboardView(RuntimeEnvironment.getApplication()) {}
        val data = LiveData(2530, 183, mapOf(
            LiveMetric.CADENCE to 170L, LiveMetric.BATTERY to 96L,
            LiveMetric.ASSIST_MODE to 2L, LiveMetric.ODOMETER to 14_791_937L,
            LiveMetric.MOTOR_POWER to 320L,
        ), listOf("OFF", "ECO", "TOUR+"))
        view.show(DashboardLayout.defaults, data)
        measure(view, 1080, 1950)
        val descriptions = descendants(view).filter { it.isClickable }.map { it.contentDescription.toString() }
        assertTrue(descriptions[0].contains("Fahrerleistung: 183 W"))
        assertTrue(descriptions[6].contains("Motorleistung: 320 W"))
        val bitmap = Bitmap.createBitmap(1080, 1950, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val output = File("build/reports/dashboard/dashboard-portrait.png").apply { parentFile!!.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val configured = DashboardLayout.defaults.replace(2, DashboardField("RIDER_ENERGY_SHARE"))
        view.show(configured, data.copy(otherValues = data.otherValues + (LiveMetric.RIDER_ENERGY_SHARE to 37L)))
        measure(view, 1080, 1950)
        val layout = descendants(view).filterIsInstance<TextView>().single { it.text.toString() == "FAHRERANTEIL" }.layout
        assertNotNull(layout)
        assertTrue(layout.lineCount <= 2)
    }

    @Test fun tappingLargeFieldSavesItsChoiceAndActivityRestartRestoresIt() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("bike", 0).edit().clear().commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val content = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        RideRuntime.get(activity).onData(LiveData(2530, 183, mapOf(
            LiveMetric.CADENCE to 170L, LiveMetric.BATTERY to 96L,
            LiveMetric.ASSIST_MODE to 2L, LiveMetric.ODOMETER to 14_791_937L,
            LiveMetric.MOTOR_POWER to 320L,
        ), listOf("OFF", "ECO", "TOUR+")), true)
        measure(content, 1080, 2130)
        val bitmap = Bitmap.createBitmap(1080, 2130, Bitmap.Config.ARGB_8888)
        content.draw(Canvas(bitmap))
        val output = File("build/reports/dashboard/dashboard-app.png").apply { parentFile!!.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val dashboard = descendants(content).filterIsInstance<DashboardView>().single()
        descendants(dashboard).filter { it.isClickable }[2].performClick()
        val picker = ShadowAlertDialog.getLatestAlertDialog()
        selectTestSource(picker, "E-Bike")
        descendants(picker.window!!.decorView).single { it.tag == "RIDER_POWER" }.performClick()
        assertEquals("RIDER_POWER", DashboardLayout.decode(context.getSharedPreferences("bike", 0).getString("dashboard", null)).fields[2].id)
        controller.pause().stop().destroy()
        val restarted = Robolectric.buildActivity(MainActivity::class.java).setup()
        val next = descendants(restarted.get().findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single()
        measure(next, 1080, 1950)
        assertTrue(descendants(next).filter { it.isClickable }[2].contentDescription.toString().startsWith("Fahrerleistung:"))
        restarted.pause().stop().destroy()
    }

    @Test fun eighthFieldSelectionPersistsAfterActivityRestartWithoutChangingOlderSlots() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("bike", 0)
        val oldFields = DashboardLayout.defaults.fields.take(7).joinToString("|") { it.id }
        prefs.edit().clear().putString("dashboardTemplate_0", "EIGHT").putString("dashboard", oldFields).commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val dashboard = descendants(controller.get().findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single()
            measure(dashboard, 1080, 900)
            descendants(dashboard).filter { it.isClickable }[7].performClick()
            val picker = ShadowAlertDialog.getLatestAlertDialog()
            selectTestSource(picker, "E-Bike")
            descendants(picker.window!!.decorView).single { it.tag == "BATTERY" }.performClick()
            val saved = DashboardLayout.decode(prefs.getString("dashboard", null))
            assertEquals(oldFields, saved.fields.take(7).joinToString("|") { it.id })
            assertEquals("BATTERY", saved.fields[7].id)
        } finally { controller.pause().stop().destroy() }
        val restarted = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val dashboard = descendants(restarted.get().findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single()
            measure(dashboard, 1080, 900)
            assertEquals(DashboardTemplate.EIGHT, dashboard.template)
            assertTrue(descendants(dashboard).filter { it.isClickable }[7].contentDescription.toString().startsWith("Akku"))
        } finally { restarted.pause().stop().destroy() }
    }

    @Test fun groupedPickerSelectsGpsSeparatelyFromBikeAndRequestsLocationOnlyWhenChosen() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("bike", 0).edit().clear().commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val dashboard = descendants(controller.get().findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single()
        measure(dashboard, 1080, 1950)
        val tiles = descendants(dashboard).filter { it.isClickable }
        tiles[2].performClick()
        val picker = ShadowAlertDialog.getLatestAlertDialog()
        val all = descendants(picker.window!!.decorView)
        assertEquals(4, (all.single { it.tag == "field_source" } as android.widget.Spinner).adapter.count)
        assertEquals(0, all.count { it.tag == "GPS_DISTANCE" }) // GPS is not the currently selected source.
        selectTestSource(picker, "GPS")
        val expanded = descendants(picker.window!!.decorView)
        assertEquals(1, expanded.count { it.tag == "GPS_DISTANCE" })
        expanded.single { it.tag == "GPS_SPEED" }.performClick()
        val prefs = context.getSharedPreferences("bike", 0)
        assertEquals("GPS_SPEED", DashboardLayout.decode(prefs.getString("dashboard", null)).fields[2].id)
        measure(dashboard, 1080, 1950)
        val refreshed = descendants(dashboard).filter { it.isClickable }
        assertTrue(refreshed[2].contentDescription.toString().startsWith("GPS · Geschwindigkeit:"))
        assertTrue(refreshed[0].contentDescription.toString().startsWith("Fahrerleistung:"))
        controller.pause().stop().destroy()
    }
}
