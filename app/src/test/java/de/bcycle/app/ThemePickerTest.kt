package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemePickerTest {
    private fun views(view: View): List<View> = listOf(view) + if (view is ViewGroup)
        (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun measure(view: View, width: Int, height: Int = 1800) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    @Test fun allSevenPresetsValidateAndSurviveRestartWithoutChangingDashboardPreferences() {
        val app = RuntimeEnvironment.getApplication()
        val prefs = app.getSharedPreferences("theme-picker", 0)
        val manager = ThemeManager(prefs)
        assertEquals(7, manager.presets.size)
        assertEquals(7, manager.presets.map { it.id }.distinct().size)
        prefs.edit().putString("dashboardTemplate_0", "FOUR").putString("address", "test-bike").apply()
        for (preset in manager.presets) {
            assertTrue("${preset.name}: ${ThemeValidator.validate(preset).problems}", ThemeValidator.validate(preset).valid)
            assertTrue(manager.activate(preset.id))
            assertEquals(preset, ThemeManager(prefs).active)
            assertEquals("FOUR", prefs.getString("dashboardTemplate_0", null))
            assertEquals("test-bike", prefs.getString("address", null))
        }
        assertFalse(manager.activate("preset:missing"))
        assertEquals(BicycleThemes.red, ThemeManager(prefs).active)
    }
    @Test fun liveDialogSwitchesPaletteAndSelectionWhilePreviewsGeometryAndDashboardDraftStayIntact() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            val themes = RideRuntime.get(activity).themes
            val toolbar = views(activity.findViewById(android.R.id.content)).single { it.tag == "ride_toolbar" } as LinearLayout
            val start = toolbar.getChildAt(0) as android.widget.Button
            val settings = toolbar.getChildAt(2) as android.widget.Button
            val toolbarGeometry = views(toolbar).map { listOf(it.width, it.height, it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom) }
            views(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            val menu = ShadowAlertDialog.getLatestAlertDialog()
            assertEquals("Design & UI", SettingsAction.DASHBOARDS.label(activity))
            views(menu.window!!.decorView).single { it.tag == "settings_PROFILES" }.performClick()
            val design = ShadowAlertDialog.getLatestAlertDialog()
            assertEquals("Profile", views(design.window!!.decorView).filterIsInstance<TextView>().single { it.tag == "bicycle_dialog_title" }.text.toString())
            design.window!!.decorView.findViewWithTag<View>("profile_accordion_profiles_design").performClick()
            val draft = views(design.window!!.decorView).filterIsInstance<DashboardPresetPicker>().single().selection.toList()
            val scroll = views(design.window!!.decorView).filterIsInstance<BicycleStickyScrollView>().single()
            measure(design.window!!.decorView, 1080, 950)
            assertTrue(scroll.getChildAt(0).height > scroll.height)
            scroll.fullScroll(View.FOCUS_DOWN); shadowOf(Looper.getMainLooper()).idle()
            assertTrue(scroll.scrollY > 0)
            scroll.scrollTo(0, 0)
            val dialog = design
            val decor = dialog.window!!.decorView
            shadowOf(Looper.getMainLooper()).idle(); measure(decor, 1080)
            val picker = views(decor).filterIsInstance<ThemePickerView>().single()
            assertEquals(3, picker.childCount) // 2 rows + global footer divider
            val before = views(picker).map { listOf(it.left, it.top, it.width, it.height) }
            for (preset in themes.presets) {
                views(picker).single { it.tag == preset.id }.performClick()
                shadowOf(Looper.getMainLooper()).idle(); measure(decor, 1080)
                assertTrue(dialog.isShowing)
                assertEquals(preset.id, themes.active.id)
                assertEquals(preset.tokens.primary, (start.background as GradientDrawable).color!!.defaultColor)
                assertEquals(preset.tokens.secondary, (settings.background as GradientDrawable).color!!.defaultColor)
                assertEquals(preset.tokens.foreground, start.currentTextColor)
                assertEquals(preset.tokens.foreground, settings.currentTextColor)
                assertEquals(toolbarGeometry, views(toolbar).map { listOf(it.width, it.height, it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom) })
                assertEquals(preset.tokens.primary, (views(design.window!!.decorView).single { it.tag == "bicycle_navigation_header" }.background as GradientDrawable).color!!.defaultColor)
                assertEquals(preset.tokens.primary, (views(menu.window!!.decorView).single { it.tag == "bicycle_navigation_header" }.background as GradientDrawable).color!!.defaultColor)
                assertEquals(before, views(picker).map { listOf(it.left, it.top, it.width, it.height) })
                assertEquals(1, views(picker).count { it.tag in themes.presets.map { theme -> theme.id } && it.isSelected })
                for (candidate in themes.presets) {
                    val card = views(picker).single { it.tag == candidate.id }
                    assertEquals(preset.tokens.secondary, (card.background as GradientDrawable).color!!.defaultColor)
                    assertEquals(candidate.id == preset.id, card.isSelected)
                    val colors = listOf(candidate.tokens.primary, candidate.tokens.secondary, candidate.tokens.tertiary)
                    colors.forEachIndexed { i, color ->
                        val strip = views(card).single { it.tag == "${candidate.id}:strip:$i" }
                        assertEquals(color, (strip.background as ColorDrawable).color)
                    }
                }
            }
            val reserved = views(picker).single { it.tag == "theme_reserved" }
            assertFalse(reserved.isClickable); assertFalse(reserved.performClick())
            assertEquals(themes.presets.last().id, themes.active.id)
            themes.activate(BicycleThemes.red.id); shadowOf(Looper.getMainLooper()).idle(); measure(decor, 1080)
            val image = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(image))
            File("build/reports/settings/theme-picker.png").apply { parentFile!!.mkdirs(); outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            testBack(dialog)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, menu.window!!.decorView.visibility)
            assertEquals(draft, views(design.window!!.decorView).filterIsInstance<DashboardPresetPicker>().single().selection.toList())
            assertEquals(BicycleThemes.red, ThemeManager(activity.getSharedPreferences("bike", 0)).active)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun smallWidthAndLargeFontReflowWithoutClippingNamesOrTouchTargets() {
        val app = RuntimeEnvironment.getApplication()
        val config = android.content.res.Configuration(app.resources.configuration).apply { fontScale = 2f }
        val context = app.createConfigurationContext(config)
        val picker = ThemePickerView(context, ThemeManager(app.getSharedPreferences("theme-size", 0)))
        for (width in listOf(720, 1080)) {
            measure(picker, width, 10000)
            val rows = (0 until picker.childCount).map { picker.getChildAt(it) }.filterIsInstance<LinearLayout>()
            assertTrue(rows.size >= 4)
            val cards = views(picker).filter { it.tag == "theme_reserved" || it.tag in BicycleThemes.presets.map { preset -> preset.id } }
            assertEquals(8, cards.size)
            for (card in cards) {
                assertTrue(card.width >= BicycleDesign.dp(context, BicycleDesign.touchSize))
                assertTrue(card.right <= (card.parent as View).width)
                val label = views(card).filterIsInstance<TextView>().single()
                assertTrue(label.layout.lineCount <= 2)
                assertEquals(label.text.length, label.layout.getLineEnd(label.layout.lineCount - 1))
                assertEquals(0, label.layout.getEllipsisCount(label.layout.lineCount - 1))
                assertTrue(label.layout.height <= label.height)
            }
        }
    }
}
