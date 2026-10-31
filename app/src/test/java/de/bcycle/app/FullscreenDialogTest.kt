package de.bcycle.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Insets
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35, 36], qualifiers = "mdpi")
class FullscreenDialogTest {
    private fun views(v: View): List<View> = listOf(v) + if (v is ViewGroup)
        (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun measure(dialog: AlertDialog, width: Int, height: Int) {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val decor = dialog.window!!.decorView
        decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        decor.layout(0, 0, width, height)
    }
    @Test fun shortAndLongContentsFillWindowWithFixedHeaderFooterAndOriginalActions() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get(); val themes = ThemeManager(activity.getSharedPreferences("full", 0))
            for ((width, height) in listOf(360 to 640, 800 to 1200, 480 to 800)) for (count in listOf(1, 60)) {
                var saved = false; var cancelled = false
                val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL
                    repeat(count) { addView(TextView(activity).apply { text = "Eintrag $it"; minHeight = 48 }) } }
                val scroll = ScrollView(activity).apply { isSmoothScrollingEnabled=false; addView(body) }
                val dialog = BicycleDialogBuilder(activity, themes, DialogNavigation()).setTitle("Referenz")
                    .setView(scroll).setNegativeButton("Abbrechen") { _, _ -> cancelled = true }
                    .setPositiveButton("Exportieren") { _, _ -> saved = true }.show()
                measure(dialog, width, height)
                val root = views(dialog.window!!.decorView).single { it.tag == "bicycle_fullscreen" } as ViewGroup
                val footer = views(root).single { it.tag == "bicycle_fullscreen_footer" }
                assertEquals(width, root.width); assertEquals(height, root.height)
                assertEquals(height, footer.bottom); assertEquals(root.getChildAt(0).bottom, scroll.top)
                assertEquals(footer.top, scroll.bottom)
                assertEquals(BicycleDesign.scrollFillViewport, scroll.isFillViewport)
                assertEquals(BicycleDesign.scrollClipToPadding, scroll.clipToPadding)
                assertEquals(BicycleDesign.dp(activity, BicycleDesign.scrollbarSize), scroll.scrollBarSize)
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, dialog.window!!.attributes.width)
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, dialog.window!!.attributes.height)
                assertEquals(0, dialog.window!!.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                assertEquals(0f, (dialog.window!!.decorView.background as GradientDrawable).cornerRadius, 0f)
                val oldHeaderTop = root.getChildAt(0).top; val oldFooterTop = footer.top
                scroll.fullScroll(View.FOCUS_DOWN); measure(dialog, width, height)
                if (count == 60) assertTrue(scroll.scrollY > 0)
                assertEquals(oldHeaderTop, root.getChildAt(0).top); assertEquals(oldFooterTop, footer.top)
                val save = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                assertTrue(save.height >= 48); assertEquals(View.GONE,cancel.visibility)
                save.performClick(); org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                assertTrue(saved); assertFalse(cancelled)
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun imeAndNavigationInsetsAreAppliedOnceAndContentRemainsScrollable() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get(); val themes = ThemeManager(activity.getSharedPreferences("insets", 0))
            val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            repeat(30) { body.addView(EditText(activity).apply { setText("Feld $it"); minHeight = 48 }) }
            val scroll = ScrollView(activity).apply { isSmoothScrollingEnabled=false; addView(body) }
            val dialog = BicycleDialogBuilder(activity, themes, DialogNavigation()).setTitle("Eingaben")
                .setView(scroll).setNegativeButton("Abbrechen", null).setPositiveButton("Speichern", null).show()
            measure(dialog, 800, 1200)
            val root = views(dialog.window!!.decorView).single { it.tag == "bicycle_fullscreen" }
            val bars = WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(), Insets.of(0, 24, 0, 24))
                .setInsets(WindowInsets.Type.displayCutout(), Insets.of(10, 0, 0, 0)).build()
            root.dispatchApplyWindowInsets(bars); measure(dialog, 800, 1200)
            assertFalse(views(root).any { it.tag=="bicycle_fullscreen_footer" })
            assertEquals(24, root.paddingBottom); assertEquals(24, root.paddingTop); assertEquals(10, root.paddingLeft)
            assertEquals(android.graphics.Color.BLACK, (root.background as android.graphics.drawable.ColorDrawable).color)
            assertEquals(1200 - 24, scroll.bottom)
            val previous = scroll.height
            root.dispatchApplyWindowInsets(WindowInsets.Builder(bars).setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, 320)).build())
            measure(dialog, 800, 1200)
            assertEquals(320, root.paddingBottom); assertEquals(880, scroll.bottom); assertTrue(scroll.height < previous)
            val input = body.getChildAt(29) as EditText
            input.requestFocus(); scroll.fullScroll(View.FOCUS_DOWN); measure(dialog, 800, 1200)
            assertTrue(scroll.scrollY > 0); assertEquals("Feld 29", input.text.toString())
            root.dispatchApplyWindowInsets(bars); measure(dialog, 800, 1200)
            assertEquals(previous, scroll.height); assertEquals(24, root.paddingBottom)
            dialog.dismiss()
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun mainMenuAndRouteManagerReuseTheirHeadersAndOwnScrollingAreas() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            views(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            val menu = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            measure(menu, 800, 1200)
            val menuRoot = views(menu.window!!.decorView).single { it.tag == "bicycle_fullscreen" } as ViewGroup
            assertEquals("bicycle_navigation_header", menuRoot.getChildAt(0).tag)
            assertFalse(views(menuRoot).any { it.tag=="bicycle_fullscreen_footer" })
            views(menuRoot).single { it.tag == "settings_ROUTES" }.performClick()
            val route = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            measure(route, 800, 1200)
            val root = views(route.window!!.decorView).single { it.tag == "bicycle_fullscreen" } as ViewGroup
            assertEquals("bicycle_navigation_header", root.getChildAt(0).tag)
            assertEquals(1, views(root).filterIsInstance<android.widget.ListView>().size)
            val body = views(root).filterIsInstance<RouteManagerView>().single()
            assertEquals(0,body.paddingTop)
            val routeList=body.findViewWithTag<android.widget.ListView>("route_list")
            assertEquals(BicycleDesign.dp(activity,BicycleDesign.pageContentGap),routeList.paddingTop)
            assertFalse(routeList.clipToPadding)
            // The native buttonPanel may itself scroll; the route body must not wrap its ListView.
            assertTrue(views(body).none { it is ScrollView })
            val footer = views(root).single { it.tag == "bicycle_fullscreen_footer" }
            assertEquals(View.GONE,footer.visibility)
            route.window!!.decorView.findViewWithTag<View>("route_footer_toggle").performClick()
            measure(route,800,1200)
            assertEquals(View.VISIBLE,footer.visibility)
            assertEquals(footer.top - root.getChildAt(0).bottom, body.height)
            assertEquals(1200, footer.bottom)
            assertTrue(body.height > BicycleDesign.managerMaxHeight)
            val imported = route.window!!.decorView.findViewWithTag<View>("route_import")
            assertTrue(imported.isShown)
            testBack(route)
            assertEquals(View.VISIBLE, menu.window!!.decorView.visibility)
            menu.dismiss()
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun nestedFullscreenKeepsParentDraftAndCompactConfirmationsRemainCompact() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get(); val themes = ThemeManager(activity.getSharedPreferences("nav-full", 0))
            val navigation = DialogNavigation(); val draft = EditText(activity).apply { setText("ungespeichert") }
            val parent = BicycleDialogBuilder(activity, themes, navigation).setTitle("Eltern").setView(draft)
                .setNegativeButton("Abbrechen", null).show()
            measure(parent, 360, 640)
            val child = BicycleDialogBuilder(activity, themes, navigation).setTitle("Kind").setMessage("Inhalt")
                .setPositiveButton("Schließen", null).show()
            measure(child, 360, 640)
            assertEquals(View.GONE, parent.window!!.decorView.visibility)
            @Suppress("DEPRECATION")
            child.onBackPressed()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue(parent.isShowing); assertEquals(View.VISIBLE, parent.window!!.decorView.visibility)
            assertEquals("ungespeichert", draft.text.toString())
            val confirm = BicycleDialogBuilder(activity, themes, navigation, fullscreen = false).setTitle("Löschen?")
                .setMessage("Bestätigung").setNegativeButton("Abbrechen", null).setPositiveButton("Löschen", null).show()
            measure(confirm, 360, 400)
            assertFalse(views(confirm.window!!.decorView).any { it.tag == "bicycle_fullscreen" })
            assertEquals(View.VISIBLE, parent.window!!.decorView.visibility) // Settings, not the dashboard, remain behind the compact overlay.
            testBack(confirm)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue(parent.isShowing); parent.dismiss()
        } finally { controller.pause().stop().destroy() }
    }
}
