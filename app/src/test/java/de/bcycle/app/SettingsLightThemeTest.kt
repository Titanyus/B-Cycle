package de.bcycle.app

import android.os.Looper
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsLightThemeTest {
    @Test fun lightBirthCalendarUsesReadableNativeTextAndWhitePrimaryHeader() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("light-calendar",0)
            val themes=ThemeManager(prefs);themes.setSettingsLight(true)
            val screen=showRiderProfile(a,themes.settings,DialogNavigation(),RiderProfileStore(prefs))
            screen.window!!.decorView.findViewWithTag<android.widget.Button>("rider_birth").performClick()
            val date=ShadowAlertDialog.getLatestAlertDialog()
            val picker=date.window!!.decorView.findViewWithTag<android.widget.DatePicker>("rider_birth_picker")
            val value=android.util.TypedValue()
            assertTrue(picker.context.theme.resolveAttribute(android.R.attr.textColorPrimary,value,true))
            val text=if(value.type in android.util.TypedValue.TYPE_FIRST_COLOR_INT..android.util.TypedValue.TYPE_LAST_COLOR_INT) value.data else picker.context.getColorStateList(value.resourceId).defaultColor
            assertTrue(ThemeValidator.contrast(text,android.graphics.Color.WHITE)>4.5)
            for(name in listOf("date_picker_header_year","date_picker_header_date")) {
                val label=picker.findViewById<android.widget.TextView>(picker.resources.getIdentifier(name,"id","android"))
                assertEquals(0x00ffffff,label.currentTextColor and 0x00ffffff)
            }
            picker.updateDate(1990,6,13)
            assertEquals(java.time.LocalDate.of(1990,7,13),RiderProfileStore(prefs).read().birthDate)
            date.dismiss();screen.dismiss()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun projectionPreservesDashboardColorsAndSurvivesProfileAndBackupRoundTrip() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("light-projection",0)
        val base=ThemeManager(prefs);val settings=base.settings
        val updates=mutableListOf<BicycleTheme>();val subscription=settings.observe { updates+=it }
        settings.setSettingsLight(true);shadowOf(Looper.getMainLooper()).idle()
        for(theme in base.presets) {
            assertTrue(settings.activate(theme.id))
            assertEquals(theme,base.active)
            val colored=theme.id.endsWith("-color")
            val expected=theme.tokens.lightSettings(if(colored) 0xfff2f2f2.toInt() else 0xffd0d0d0.toInt())
            assertEquals(if(colored) expected.copy(accent=theme.tokens.primary) else expected,settings.active.tokens)
            assertEquals(theme.tokens.primary,BicycleComponents.selectionBorder(settings.active.tokens,true))
            if(colored) {
                assertNotEquals(settings.active.tokens.secondary,settings.active.tokens.accent)
                val previous=theme.tokens.lightSettings(0xffe3e3e3.toInt())
                assertTrue(ThemeValidator.luminance(settings.active.tokens.tertiary)>ThemeValidator.luminance(previous.tertiary))
            }
            assertEquals(settings.active,updates.last())
        }
        val store=BikeUiConfigurationStore(prefs);store.ensure(listOf(0,1));store.save(0)
        settings.setSettingsLight(false);store.save(1);store.apply(0);base.reloadFromPreferences()
        assertFalse(settings.settingsLight) // Profile switching must not restore a per-profile light flag.
        settings.setSettingsLight(true)
        assertEquals(base.active.tokens.lightSettings().foreground,settings.active.tokens.foreground)
        val backup=SettingsBackup(prefs,c.getSharedPreferences("light-export",0),base)
        val bytes=backup.export(setOf(BackupSection.DESIGN))
        settings.setSettingsLight(false)
        backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.DESIGN)));settings.reloadFromPreferences()
        assertTrue(settings.settingsLight)
        assertEquals(settings.active,ThemeManager(prefs).settings.active)
        assertNotEquals(settings.active.tokens,base.active.tokens)
        settings.setSettingsLight(false);shadowOf(Looper.getMainLooper()).idle()
        assertEquals(base.active,settings.active);subscription.close()
    }
    @Test fun headerButtonUpdatesAttachedSettingsImmediatelyWithoutChangingDashboard() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a);val original=runtime.themes.active
            MainActivity::class.java.getDeclaredMethod("showMenu",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,false)
            shadowOf(Looper.getMainLooper()).idle()
            val screen=ShadowAlertDialog.getLatestAlertDialog();val root=screen.window!!.decorView
            val toggle=root.findViewWithTag<View>("settings_light_toggle")
            assertEquals(a.getString(R.string.settings_dark_mode),toggle.contentDescription)
            toggle.performClick();shadowOf(Looper.getMainLooper()).idle()
            assertEquals(a.getString(R.string.settings_light_mode),toggle.contentDescription)
            assertEquals(android.graphics.Color.WHITE,root.findViewWithTag<android.widget.TextView>("bicycle_dialog_title").currentTextColor)
            for(tag in listOf("dialog_back","settings_light_toggle")) {
                val icon=root.findViewWithTag<android.widget.ImageButton>(tag).drawable
                val bitmap=android.graphics.Bitmap.createBitmap(48,48,android.graphics.Bitmap.Config.ARGB_8888)
                icon.setBounds(0,0,48,48);icon.draw(android.graphics.Canvas(bitmap))
                val pixels=IntArray(48*48);bitmap.getPixels(pixels,0,48,0,0,48,48)
                assertTrue(pixels.any { android.graphics.Color.alpha(it)>200 })
                assertTrue(pixels.filter { android.graphics.Color.alpha(it)>200 }.all { it and 0xffffff==0xffffff })
                bitmap.recycle()
            }
            val body=root.findViewWithTag<View>("bicycle_fullscreen_content")
            val bg=body.background as android.graphics.drawable.ColorDrawable
            assertEquals(0xffffffff.toInt(),bg.color)
            assertEquals(original,runtime.themes.active)
            root.findViewWithTag<View>("settings_PROFILES").performClick()
            val profiles=ShadowAlertDialog.getLatestAlertDialog()
            assertNull(profiles.window!!.decorView.findViewWithTag<View>("settings_light_toggle"))
            assertEquals(android.graphics.Color.WHITE,profiles.window!!.decorView.findViewWithTag<android.widget.TextView>("bicycle_dialog_title").currentTextColor)
            assertTrue(runtime.themes.settingsLight)
            profiles.dismiss();shadowOf(Looper.getMainLooper()).idle()
            toggle.performClick();shadowOf(Looper.getMainLooper()).idle()
            assertEquals(original.tokens,runtime.themes.settings.active.tokens)
        } finally { host.pause().stop().destroy() }
    }
}
