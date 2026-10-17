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
    @Test fun compactDropdownSelectsEveryThemeAndKeepsDashboardDraft() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();val runtime=RideRuntime.get(a);val themes=runtime.themes
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            val dialog=ShadowAlertDialog.getLatestAlertDialog();val root=dialog.window!!.decorView
            shadowOf(Looper.getMainLooper()).idle();measure(root,1080,2400)
            val picker=views(root).filterIsInstance<ThemePickerView>().single()
            val choice=picker.findViewWithTag<View>("theme_choice")
            val draft=views(root).filterIsInstance<DashboardPresetPicker>().single().selection.toList()
            val size=listOf(picker.width,picker.height)
            assertEquals(1,picker.childCount)
            for((index,preset) in themes.presets.withIndex()) {
                choice.performClick()
                val popup=picker.popup!!;val list=popup.listView!!;val adapter=list.adapter
                assertEquals(8,adapter.count);assertFalse(adapter.isEnabled(7))
                val row=adapter.getView(index,null,list) as LinearLayout
                measure(row,1080,200)
                val sample=row.getChildAt(1)
                val bitmap=Bitmap.createBitmap(sample.width,sample.height,Bitmap.Config.ARGB_8888)
                sample.draw(Canvas(bitmap))
                val colors=preset.tokens.let { listOf(it.primary,it.secondary,it.tertiary,it.foreground,it.accent) }
                colors.forEachIndexed { i,color -> assertEquals(color,bitmap.getPixel(((i+.5f)*sample.width/5f).toInt(),sample.height/2)) }
                bitmap.recycle()
                list.performItemClick(row,index,index.toLong())
                shadowOf(Looper.getMainLooper()).idle();measure(root,1080,2400)
                assertEquals(preset.id,themes.active.id);assertFalse(popup.isShowing)
                assertTrue(dialog.isShowing)
                assertEquals(size,listOf(picker.width,picker.height))
                assertEquals(draft,views(root).filterIsInstance<DashboardPresetPicker>().single().selection.toList())
                assertEquals(preset.name,choice.contentDescription)
            }
            choice.performClick()
            val list=picker.popup!!.listView!!;measure(list,1080,1600)
            val bitmap=Bitmap.createBitmap(list.width,list.height,Bitmap.Config.ARGB_8888)
            list.draw(Canvas(bitmap))
            File("/tmp/bcycle-theme-dropdown.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun selectorKeepsTouchHeightOnNarrowPortraitAndLargeFont() {
        val app=RuntimeEnvironment.getApplication();val config=android.content.res.Configuration(app.resources.configuration).apply { fontScale=1.3f }
        val c=app.createConfigurationContext(config)
        val picker=ThemePickerView(c,ThemeManager(app.getSharedPreferences("theme-size",0)))
        for(width in listOf(720,1080)) {
            measure(picker,width,1000)
            val choice=picker.getChildAt(0) as LinearLayout
            assertTrue(choice.height>=BicycleDesign.dp(c,BicycleDesign.touchSize))
            assertTrue(choice.getChildAt(1).width>0)
            assertEquals(BicycleDesign.settingsThemeNameSp*c.resources.displayMetrics.scaledDensity,(choice.getChildAt(0) as TextView).textSize,.01f)
        }
    }
}
