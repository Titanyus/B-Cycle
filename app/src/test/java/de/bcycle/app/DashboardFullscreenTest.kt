package de.bcycle.app

import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DashboardFullscreenTest {
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    // Inspect the platform controller's requested visibility; Robolectric has no physical status bar.
    private fun visible(window:Window,type:String):Boolean {
        val controller=window.insetsController!!
        val value=Class.forName("android.view.InsetsState").getField(type).getInt(null)
        return controller.javaClass.getMethod("isRequestedVisible",Int::class.javaPrimitiveType).invoke(controller,value) as Boolean
    }
    @Test fun statusBarIsHiddenOnDashboardButVisibleInDialogsAndNavigationIsPreserved() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();activity.onWindowFocusChanged(true)
            assertFalse(visible(activity.window,"ITYPE_STATUS_BAR"))
            assertTrue(visible(activity.window,"ITYPE_NAVIGATION_BAR"))
            assertEquals(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,activity.window.insetsController!!.systemBarsBehavior)
            views(activity.window.decorView).single { it.contentDescription==activity.getString(R.string.ui_menu_open) }.performClick()
            val menu=ShadowAlertDialog.getLatestAlertDialog()
            assertTrue(visible(menu.window!!,"ITYPE_STATUS_BAR"))
            menu.dismiss();activity.onWindowFocusChanged(true)
            assertFalse(visible(activity.window,"ITYPE_STATUS_BAR"))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun designSwitchPersistsAndDisablingSurvivesReturnFromAnotherApp() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val prefs=activity.getSharedPreferences("bike",0)
            assertTrue(DashboardFullscreen.enabled(prefs))
            views(activity.window.decorView).single { it.contentDescription==activity.getString(R.string.ui_menu_open) }.performClick()
            val menu=ShadowAlertDialog.getLatestAlertDialog()
            views(menu.window!!.decorView).single { it.tag=="settings_DASHBOARDS" }.performClick()
            val screen=ShadowAlertDialog.getLatestAlertDialog()
            val toggle=screen.window!!.decorView.findViewWithTag<Switch>("dashboard_fullscreen")
            assertTrue(toggle.isChecked);toggle.performClick()
            assertFalse(DashboardFullscreen.enabled(prefs))
            assertTrue(visible(screen.window!!,"ITYPE_STATUS_BAR"))
            screen.dismiss();menu.dismiss();activity.onWindowFocusChanged(true)
            assertTrue(visible(activity.window,"ITYPE_STATUS_BAR"))
            controller.pause().stop().start().resume().visible();activity.onWindowFocusChanged(true)
            assertTrue(visible(activity.window,"ITYPE_STATUS_BAR"))
            controller.recreate();controller.get().onWindowFocusChanged(true)
            assertTrue(visible(controller.get().window,"ITYPE_STATUS_BAR"))
        } finally { controller.pause().stop().destroy() }
    }
}
