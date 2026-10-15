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
    @Test fun sevenRowsUseCompactHeightAndAdvancedHasNormalRowsWithLicensesLast() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val menu = open(controller.get())
            shadowOf(Looper.getMainLooper()).idle()
            val body = views(menu).filterIsInstance<SettingsMenuView>().single()
            val buildInfo = descendants(body).filterIsInstance<TextView>().single { it.tag == "settings_build_info" }
            assertEquals(controller.get().getString(R.string.settings_build_info, AppVersion.name, AppVersion.buildDate()), buildInfo.text.toString())
            assertEquals(android.view.Gravity.CENTER, buildInfo.gravity)
            assertTrue(AppVersion.buildTimestamp > 0)

            for (width in listOf(960,720)) {
                body.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                body.layout(0,0,width,body.measuredHeight)
                val rows=descendants(body).filter { it.isClickable && it.tag?.toString()?.startsWith("settings_")==true }
                assertEquals(listOf("settings_PROFILE", "settings_SENSORS", "settings_ROUTES", "settings_MAP", "settings_DASHBOARDS", "settings_DATA", "settings_ADVANCED"), rows.map { it.tag })
                assertEquals(listOf("Fahrer", "Bikes", "Strecken", "Karten", "Design & UI", "Daten & Speicher", "Erweitert"), rows.map { it.contentDescription.toString() })
                rows.forEach { assertEquals(width, it.width);assertTrue(it.height>=BicycleDesign.dp(controller.get(),BicycleDesign.settingsRowHeight)) }
            }
            views(menu).single { it.tag=="settings_ADVANCED" }.performClick()
            val advanced=ShadowAlertDialog.getLatestAlertDialog();shadowOf(Looper.getMainLooper()).idle()
            assertNotSame(menu,advanced);assertEquals(View.GONE,menu.window!!.decorView.visibility)
            assertFalse(views(advanced).any { it.tag == "settings_build_info" })
            val rows=views(advanced).filter { it.isClickable && it.tag?.toString()?.startsWith("settings_")==true }
            assertEquals(6,rows.size)
            assertEquals("settings_FONT_LICENSE",rows[4].tag);assertEquals("settings_MAP_LICENSE",rows[5].tag)
            rows.forEach { assertEquals(BicycleDesign.dp(controller.get(),BicycleDesign.touchSize),it.minimumHeight) }
            testBack(advanced);assertEquals(View.VISIBLE,menu.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun backRestoresSameAdvancedAndMainScreens() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root=open(controller.get())
            views(root).single { it.tag=="settings_ADVANCED" }.performClick()
            val advanced=ShadowAlertDialog.getLatestAlertDialog()
            views(advanced).single { it.tag=="settings_FONT_LICENSE" }.performClick()
            testBack(ShadowAlertDialog.getLatestAlertDialog())
            assertEquals(View.VISIBLE,advanced.window!!.decorView.visibility)
            testBack(advanced);assertEquals(View.VISIBLE,root.window!!.decorView.visibility)
            views(root).single { it.tag=="settings_MAP" }.performClick()
            testBack(ShadowAlertDialog.getLatestAlertDialog());assertEquals(View.VISIBLE,root.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun graphicalPresetGridPersistsEachSelectionAndBackKeepsIt() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val prefs = activity.getSharedPreferences("bike", 0)
            val root = open(activity)
            views(root).single { it.tag == "settings_DASHBOARDS" }.performClick()
            val cancelled = ShadowAlertDialog.getLatestAlertDialog()
            views(cancelled).single { it.tag == "preset_HERO" }.performClick()
            testBack(cancelled)
            assertEquals("HERO",prefs.getString("dashboardTemplate_0",null))
            views(root).single { it.tag == "settings_DASHBOARDS" }.performClick()
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            for (i in 0..2) assertEquals(3, (views(dialog).single { it.tag == "preset_row_$i" } as ViewGroup).childCount)
            views(dialog).single { it.tag == "preset_HERO_SEVEN" }.performClick()
            assertTrue(views(dialog).single { it.tag == "preset_HERO_SEVEN" }.isSelected)
            views(dialog).single { it.tag == "preset_page_1" }.performClick()
            views(dialog).single { it.tag == "preset_EIGHT" }.performClick()
            testBack(dialog)
            assertEquals("HERO_SEVEN", prefs.getString("dashboardTemplate_0", null))
            assertEquals("EIGHT", prefs.getString("dashboardTemplate_1", null))
            assertEquals(DashboardTemplate.HERO_SEVEN, descendants(activity.findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single().template)
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
                var parent=menu
                if (action.ordinal in SettingsAction.RECOVERY.ordinal..SettingsAction.MAP_LICENSE.ordinal)
                    { views(menu).single { it.tag == "settings_ADVANCED" }.performClick();parent=ShadowAlertDialog.getLatestAlertDialog() }
                views(parent).single { it.tag == "settings_${action.name}" }.performClick()
                val child = ShadowAlertDialog.getLatestAlertDialog()
                assertNotSame(menu, child); assertTrue(child.isShowing)
                val colour = android.util.TypedValue()
                assertTrue(child.context.theme.resolveAttribute(android.R.attr.colorBackground, colour, true))
                assertEquals(activity.getColor(R.color.settings_red), colour.data)
                if (action == SettingsAction.RESET) {
                    assertEquals(custom, prefs.getString("dashboard", null))
                    views(child).single { it.tag == "reset_execute" }.performClick()
                    assertEquals(custom,prefs.getString("dashboard",null))
                    val confirmation=ShadowAlertDialog.getLatestAlertDialog()
                    confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle()
                    assertEquals(DashboardLayout.defaults.encode(),prefs.getString("dashboard",null))
                } else child.dismiss()
                shadowOf(Looper.getMainLooper()).idle()
                parent.dismiss();menu.dismiss(); shadowOf(Looper.getMainLooper()).idle()
            }
        } finally { controller.pause().stop().destroy() }
    }
}
