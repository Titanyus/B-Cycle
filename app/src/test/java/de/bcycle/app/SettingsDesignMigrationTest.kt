package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsDesignMigrationTest {
    private fun views(view: View): List<View> = listOf(view) + if (view is ViewGroup)
        (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun hasThemePickerParent(view: View): Boolean {
        var parent = view.parent
        while (parent is View) {
            if (parent is ThemePickerView) return true
            parent = parent.parent
        }
        return false
    }
    private fun views(dialog: AlertDialog) = views(dialog.window!!.decorView)
    private fun open(activity: MainActivity): AlertDialog {
        views(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
        return ShadowAlertDialog.getLatestAlertDialog()
    }
    private fun measure(dialog: AlertDialog, width: Int = 1080, height: Int = 1800) {
        val decor = dialog.window!!.decorView
        decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        decor.layout(0, 0, width, height)
    }
    // Native AlertController contains hidden spacer views whose stale rectangles are not UI geometry.
    private fun geometry(dialog: AlertDialog) = views(dialog).filter { it.isShown }.map {
        listOf(it.left, it.top, it.width, it.height, it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom)
    }
    private fun snapshot(dialog: AlertDialog, name: String) {
        val decor = dialog.window!!.decorView
        val image = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(image))
        File("build/reports/settings/phase3-$name.png").apply { parentFile!!.mkdirs(); outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    private fun light() = CustomTheme("custom:phase3-light", "Test hell", ThemeTokens(0xffeeeeee.toInt(), 0xffcccccc.toInt(), 0xffaaaaaa.toInt(), 0xff000000.toInt()))

    @Test fun mainScreensAndAdvancedDialogsRecolorWithoutChangingGeometryOrNavigation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val themes = RideRuntime.get(activity).themes
            assertTrue(themes.saveCustom(light()).valid)
            val root = open(activity)
            for (action in SettingsAction.entries.filter { it != SettingsAction.ADVANCED && it != SettingsAction.ROUTES }) {
                themes.activate(BicycleThemes.red.id)
                if (action.ordinal >= SettingsAction.RECOVERY.ordinal && views(root).single { it.tag == "settings_advanced_content" }.visibility != View.VISIBLE)
                    views(root).single { it.tag == "settings_ADVANCED" }.performClick()
                views(root).single { it.tag == "settings_${action.name}" }.performClick()
                val child = ShadowAlertDialog.getLatestAlertDialog()
                shadowOf(Looper.getMainLooper()).idle(); measure(child)
                val header = views(child).single { it.tag == "bicycle_dialog_header" }
                val title = views(child).filterIsInstance<TextView>().single { it.tag == "bicycle_dialog_title" }
                assertEquals(BicycleDesign.headerHeight * activity.resources.displayMetrics.density, title.height.toFloat(), 1f)
                assertEquals(BicycleThemes.red.tokens.primary, (header.background as GradientDrawable).color!!.defaultColor)
                assertTrue(title.isAccessibilityHeading)
                assertTrue(views(child).filterIsInstance<TextView>().filter { it !== title && !hasThemePickerParent(it) }.all { it.typeface == Typeface.DEFAULT || it.tag == "rider_unit" || it.tag?.toString()?.startsWith("rider_zone_value_") == true })
                if (action in listOf(SettingsAction.SENSORS, SettingsAction.MAP, SettingsAction.DASHBOARDS)) snapshot(child, action.name.lowercase())
                val before = geometry(child)
                assertTrue(themes.activate(light().id)); shadowOf(Looper.getMainLooper()).idle(); measure(child)
                assertEquals("Theme changed geometry in $action", before, geometry(child))
                assertEquals(light().tokens.primary, (header.background as GradientDrawable).color!!.defaultColor)
                assertTrue(views(child).filterIsInstance<TextView>().all { it.currentTextColor == light().tokens.foreground || (it.tag == "rider_unit" && it.currentTextColor == BicycleDesign.dashboardUnitColor) || !it.isEnabled })
                child.getButton(AlertDialog.BUTTON_NEGATIVE).takeIf { it.visibility == View.VISIBLE }?.performClick() ?: child.dismiss()
                shadowOf(Looper.getMainLooper()).idle()
                assertEquals(View.VISIBLE, root.window!!.decorView.visibility)
            }
            themes.activate(BicycleThemes.red.id)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun webdavInputsKeepDraftFocusAndScrollWithShortKeyboardViewportAndThemeChanges() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val themes = RideRuntime.get(activity).themes
            val root = open(activity)
            views(root).single { it.tag == "settings_DATA" }.performClick()
            val manager = ShadowAlertDialog.getLatestAlertDialog()
            views(manager).single { it.tag == "data_CLOUD" }.performClick()
            val screen = ShadowAlertDialog.getLatestAlertDialog()
            val server = views(screen).single { it.tag == "export_server" } as EditText
            val folderButton = views(screen).single { it.tag == "export_test" }
            assertEquals((server.layoutParams as ViewGroup.MarginLayoutParams).topMargin,
                (folderButton.layoutParams as ViewGroup.MarginLayoutParams).topMargin)
            server.setText("https://example.test"); server.requestFocus()
            measure(screen, 720, 950)
            val scroll = views(screen).filterIsInstance<ScrollView>().single { views(it).any { child -> child is RouteExportView } }
            assertTrue(scroll.height > 0); assertTrue(scroll.getChildAt(0).height > scroll.height)
            assertTrue(screen.getButton(AlertDialog.BUTTON_POSITIVE).height > 0)
            scroll.fullScroll(View.FOCUS_DOWN); shadowOf(Looper.getMainLooper()).idle(); assertTrue(scroll.scrollY > 0)
            server.requestFocus(); measure(screen, 720, 950)
            assertTrue(themes.saveCustom(light()).valid)
            val before = geometry(screen)
            themes.activate(light().id); shadowOf(Looper.getMainLooper()).idle(); measure(screen, 720, 950)
            assertEquals(before, geometry(screen)); assertEquals("https://example.test", server.text.toString()); assertTrue(server.hasFocus())
            assertEquals(light().tokens.foreground, server.currentTextColor)
            screen.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            assertEquals("", RouteExportSettings(activity).read().server)
            themes.activate(BicycleThemes.red.id)
            views(manager).single { it.tag == "data_CLOUD" }.performClick()
            val fresh = ShadowAlertDialog.getLatestAlertDialog(); shadowOf(Looper.getMainLooper()).idle(); measure(fresh); snapshot(fresh, "webdav")
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun fieldDropdownAndNewRowsUseActiveThemeWhileSourceSelectionRemainsFixed() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val themes = RideRuntime.get(activity).themes
            val selected = mutableListOf<DashboardField?>()
            val content = FieldPickerView(activity, LiveData(), GpsValues(), null, PulseValues(), DashboardField("RIDER_POWER"), true, themes) { selected += it }
            val screen = BicycleDialogBuilder(activity, themes, DialogNavigation()).setTitle("Ein besonders langer Titel zur Auswahl der Karten-Datenfelder")
                .setView(content).setNegativeButton("Abbrechen", null).show()
            measure(screen, 720, 1200)
            assertTrue(themes.saveCustom(light()).valid); themes.activate(light().id); shadowOf(Looper.getMainLooper()).idle()
            val selector = content.findViewWithTag<Spinner>("field_source")
            selector.setSelection(0); shadowOf(Looper.getMainLooper()).idle(); measure(screen, 720, 1200)
            val choice = views(content).single { it.tag == "GPS_SPEED" } as RadioButton
            assertEquals(light().tokens.foreground, choice.currentTextColor)
            val opened = selector.adapter.getDropDownView(0, null, content) as TextView
            assertEquals(light().tokens.foreground, opened.currentTextColor)
            assertEquals(light().tokens.tertiary, (opened.background as android.graphics.drawable.ColorDrawable).color)
            choice.performClick(); assertEquals(DashboardField("GPS_SPEED"), selected.single())
            themes.activate(BicycleThemes.red.id); measure(screen, 720, 1200); snapshot(screen, "field-picker-small")
            screen.dismiss()
            val dashboard = views(activity.findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single()
            views(dashboard).first { it.isClickable }.performClick()
            val normal = ShadowAlertDialog.getLatestAlertDialog()
            shadowOf(Looper.getMainLooper()).idle(); measure(normal)
            val title = views(normal).single { it.tag == "bicycle_dialog_title" } as TextView
            fun pixels(sp: Float) = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, sp, activity.resources.displayMetrics)
            assertEquals(pixels(BicycleDesign.headingSp), title.textSize, 1f)
            assertTrue(views(normal).filterIsInstance<RadioButton>().all {
                it.textSize == pixels(BicycleDesign.bodySp)
            })
            snapshot(normal, "field-picker")
        } finally { controller.pause().stop().destroy() }
    }
}
