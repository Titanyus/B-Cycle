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
    @Test fun topGlowUsesWholeWindowIncludingStatusCutoutSpaceWhileContentKeepsSafeInsets() {
        val app=org.robolectric.RuntimeEnvironment.getApplication()
        val prefs=app.getSharedPreferences("bike",0)
        prefs.edit().putString(ModeGlowSettings.placementKey,"TOP").commit()
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get()
            val host=activity.window.decorView.findViewWithTag<android.widget.FrameLayout>("dashboard_host")
            val root=host.getChildAt(0)
            val dashboard=views(host).filterIsInstance<DashboardView>().single()
            val insets=android.view.WindowInsets.Builder()
                .setInsets(android.view.WindowInsets.Type.statusBars(),android.graphics.Insets.NONE)
                .setInsets(android.view.WindowInsets.Type.displayCutout(),android.graphics.Insets.of(0,84,0,0))
                .setInsets(android.view.WindowInsets.Type.navigationBars(),android.graphics.Insets.of(0,0,0,30)).build()
            root.dispatchApplyWindowInsets(insets)
            fun draw() {
                host.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2240,View.MeasureSpec.EXACTLY))
                host.layout(0,0,1080,2240)
                val bitmap=android.graphics.Bitmap.createBitmap(1080,2240,android.graphics.Bitmap.Config.ARGB_8888)
                host.draw(android.graphics.Canvas(bitmap));bitmap.recycle()
            }
            draw()
            assertSame(dashboard.modeGlow,host.background);assertNull(dashboard.background)
            assertEquals(84,root.paddingTop);assertEquals(30,root.paddingBottom)
            assertEquals(android.graphics.Rect(0,0,1080,2240),dashboard.modeGlow.bounds)
            assertEquals(android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,activity.window.attributes.layoutInDisplayCutoutMode)
            prefs.edit().putString(ModeGlowSettings.placementKey,"SIDES").commit()
            RideRuntime.get(activity).onData(LiveData(),false);draw()
            assertSame(dashboard.modeGlow,dashboard.background);assertNotSame(dashboard.modeGlow,host.background)
            assertEquals(dashboard.height,dashboard.modeGlow.bounds.height())
            assertEquals(84,root.paddingTop)
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
