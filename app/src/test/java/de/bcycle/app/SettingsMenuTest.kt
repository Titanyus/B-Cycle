package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
class SettingsMenuTest {
    private fun descendants(v: View): List<View> = listOf(v) + if (v is ViewGroup)
        (0 until v.childCount).flatMap { descendants(v.getChildAt(it)) } else emptyList()
    private fun open(activity: MainActivity): AlertDialog {
        descendants(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
        return ShadowAlertDialog.getLatestAlertDialog()
    }
    private fun views(dialog: AlertDialog) = descendants(dialog.window!!.decorView)
    @Test fun sixTilesHaveThreeColumnsBrandAndCollapsedAdvancedWithLicensesLast() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val dialog = open(controller.get())
            val menu = views(dialog).filterIsInstance<SettingsMenuView>().single()
            for ((width, height) in listOf(960 to 1700, 720 to 1500)) {
                menu.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
                menu.layout(0, 0, width, menu.measuredHeight)
                for (i in 0..1) {
                    val row = views(dialog).single { it.tag == "settings_row_$i" } as ViewGroup
                    assertEquals(3, row.childCount)
                    assertEquals(3, (0..2).count { row.getChildAt(it).isClickable })
                    for (column in 0..2) assertTrue(row.getChildAt(column).width >= width / 4)
                }
                for (text in descendants(menu).filterIsInstance<TextView>()) {
                    val layout = requireNotNull(text.layout)
                    assertTrue("Clipped ${text.text}", layout.height <= text.height)
                    for (line in 0 until layout.lineCount) assertTrue("Too wide ${text.text} at $width: ${layout.getLineWidth(line)} > ${text.width}", layout.getLineWidth(line) <= text.width + 1)
                }
            }
            val brand = descendants(menu).single { it.tag == "settings_brand" } as android.widget.ImageView
            assertEquals("B|Cycle", brand.contentDescription)
            assertEquals(menu.width, brand.width)
            assertEquals(android.widget.ImageView.ScaleType.FIT_CENTER, brand.scaleType)
            assertEquals(controller.get().getColor(R.color.start_red), (brand.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
            val sensorIcon = controller.get().getDrawable(R.drawable.ic_settings_sensors)!!
            val iconPixels = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
            sensorIcon.setBounds(0, 0, 192, 192); sensorIcon.draw(Canvas(iconPixels))
            assertEquals(android.graphics.Color.WHITE, iconPixels.getPixel(96, 96))
            for (y in 0 until 192) {
                assertEquals("Sensor arc clipped on left", 0, android.graphics.Color.alpha(iconPixels.getPixel(0, y)))
                assertEquals("Sensor arc clipped on right", 0, android.graphics.Color.alpha(iconPixels.getPixel(191, y)))
            }
            val advanced = views(dialog).single { it.tag == "settings_advanced_content" } as ViewGroup
            assertEquals(View.GONE, advanced.visibility); assertEquals(0, advanced.childCount)
            val toggle = views(dialog).single { it.tag == "settings_ADVANCED" }
            toggle.performClick(); assertEquals(View.VISIBLE, advanced.visibility); assertEquals(6, advanced.childCount)
            assertEquals("Ausgeklappt", toggle.stateDescription)
            toggle.performClick(); assertEquals(View.GONE, advanced.visibility)
            assertEquals("settings_FONT_LICENSE", advanced.getChildAt(advanced.childCount - 2).tag)
            assertEquals("settings_MAP_LICENSE", advanced.getChildAt(advanced.childCount - 1).tag)
            assertEquals(1, views(dialog).single { it.tag == "settings_separator" }.layoutParams.height)
            // Produce a native Android render for visual review of colour, symbols and spacing.
            menu.measure(View.MeasureSpec.makeMeasureSpec(960, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            menu.layout(0, 0, 960, menu.measuredHeight)
            val image = Bitmap.createBitmap(menu.width, menu.height, Bitmap.Config.ARGB_8888)
            Canvas(image).apply { drawColor(controller.get().getColor(R.color.settings_red)); menu.draw(this) }
            val output = File("build/reports/settings/menu.png").apply { parentFile!!.mkdirs() }
            output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun closeCancelAndBackRestoreSameParentWithExpandedAdvancedSection() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = open(controller.get())
            views(root).single { it.tag == "settings_ADVANCED" }.performClick()
            views(root).single { it.tag == "settings_FONT_LICENSE" }.performClick()
            val license = ShadowAlertDialog.getLatestAlertDialog()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.GONE, root.window!!.decorView.visibility)
            license.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, root.window!!.decorView.visibility)
            assertEquals(View.VISIBLE, views(root).single { it.tag == "settings_advanced_content" }.visibility)
            views(root).single { it.tag == "settings_MAP" }.performClick()
            val map = ShadowAlertDialog.getLatestAlertDialog()
            map.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, root.window!!.decorView.visibility)
            views(root).single { it.tag == "settings_DIAGNOSTIC" }.performClick()
            ShadowAlertDialog.getLatestAlertDialog().cancel(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, root.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun graphicalPresetGridEditsPagesAsDraftAndSavePersistsTheNewLayout() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val prefs = activity.getSharedPreferences("bike", 0)
            val root = open(activity)
            views(root).single { it.tag == "settings_DASHBOARDS" }.performClick()
            val cancelled = ShadowAlertDialog.getLatestAlertDialog()
            views(cancelled).single { it.tag == "preset_HERO" }.performClick()
            cancelled.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertNull(prefs.getString("dashboardTemplate_0", null))
            views(root).single { it.tag == "settings_DASHBOARDS" }.performClick()
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            for (i in 0..1) assertEquals(3, (views(dialog).single { it.tag == "preset_row_$i" } as ViewGroup).childCount)
            views(dialog).single { it.tag == "preset_HERO" }.performClick()
            assertTrue(views(dialog).single { it.tag == "preset_HERO" }.isSelected)
            views(dialog).single { it.tag == "preset_page_1" }.performClick()
            views(dialog).single { it.tag == "preset_FIVE" }.performClick()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals("HERO", prefs.getString("dashboardTemplate_0", null))
            assertEquals("FIVE", prefs.getString("dashboardTemplate_1", null))
            assertEquals(DashboardTemplate.HERO, descendants(activity.findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single().template)
            assertEquals(View.VISIBLE, root.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun everyMainAndAdvancedEntryOpensItsOwnRedDialogWithoutExecutingActions() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            val prefs = activity.getSharedPreferences("bike", 0)
            val custom = DashboardLayout.defaults.replace(0, DashboardField("HEART_RATE")).encode()
            prefs.edit().putString("dashboard", custom).commit()
            for (action in SettingsAction.entries.filter { it != SettingsAction.ADVANCED }) {
                val menu = open(activity)
                if (action.ordinal in SettingsAction.RECOVERY.ordinal..SettingsAction.MAP_LICENSE.ordinal)
                    views(menu).single { it.tag == "settings_ADVANCED" }.performClick()
                views(menu).single { it.tag == "settings_${action.name}" }.performClick()
                val child = ShadowAlertDialog.getLatestAlertDialog()
                assertNotSame(menu, child); assertTrue(child.isShowing)
                val colour = android.util.TypedValue()
                assertTrue(child.context.theme.resolveAttribute(android.R.attr.colorBackground, colour, true))
                assertEquals(activity.getColor(R.color.settings_red), colour.data)
                if (action == SettingsAction.RESET) {
                    assertEquals(custom, prefs.getString("dashboard", null))
                    views(child).single { it.tag == "reset_execute" }.performClick()
                    assertEquals(DashboardLayout.defaults.encode(), prefs.getString("dashboard", null))
                } else child.dismiss()
                shadowOf(Looper.getMainLooper()).idle()
                menu.dismiss(); shadowOf(Looper.getMainLooper()).idle()
            }
        } finally { controller.pause().stop().destroy() }
    }
}
