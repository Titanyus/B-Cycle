package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="de-rDE-mdpi")
class SettingsHelpTest {
    @Test fun allHelpPagesHaveTranslatedResourcesAndIndependentStickyAccordions() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("help-content",0)).settings
            for(page in SettingsHelpPage.entries) {
                assertTrue(a.getString(page.title).isNotBlank());assertTrue(page.sections.isNotEmpty())
                val view=SettingsHelp.body(a,themes,page)
                val headers=testDescendants(view).filterIsInstance<BicycleSectionHeader>()
                assertEquals(page.sections.size,headers.size)
                for(section in page.sections) {
                    assertTrue(a.getString(section.heading).isNotBlank());assertTrue(a.getString(section.body).isNotBlank())
                    assertEquals(View.GONE,view.findViewWithTag<View>("help_body_${section.body}").visibility)
                }
                headers.first().performClick()
                assertEquals(View.VISIBLE,view.findViewWithTag<View>("help_body_${page.sections.first().body}").visibility)
                assertTrue(headers.drop(1).all { it.expanded==false })
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun headerHelpDoesNotOverlapAppearanceAndReturnsToUnchangedProfile() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            MainActivity::class.java.getDeclaredMethod("showMenu",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,false)
            shadowOf(Looper.getMainLooper()).idle()
            val menu=ShadowAlertDialog.getLatestAlertDialog()
            val decor=menu.window!!.decorView
            decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,800)
            val info=decor.findViewWithTag<View>("settings_help")
            val light=decor.findViewWithTag<View>("settings_light_toggle")
            assertTrue(light.right<=info.left)
            info.performClick()
            val help=ShadowAlertDialog.getLatestAlertDialog()
            assertNull(help.window!!.decorView.findViewWithTag<View>("settings_help"))
            assertNotNull(help.window!!.decorView.findViewWithTag<View>("help_body_${R.string.help_appearance_body}"))
            testBack(help);assertTrue(menu.isShowing)
            decor.findViewWithTag<View>("settings_PROFILES").performClick()
            shadowOf(Looper.getMainLooper()).idle()
            val profile=ShadowAlertDialog.getLatestAlertDialog();val root=profile.window!!.decorView
            val scroll=testDescendants(root).filterIsInstance<ScrollView>().single()
            val header=testDescendants(root).filterIsInstance<BicycleSectionHeader>().first { it.contentDescription==a.getString(R.string.bikes_sensors) }
            header.performClick();val expanded=header.expanded
            val before=RideRuntime.get(a).bikes.current()
            root.findViewWithTag<View>("settings_help").performClick()
            val profileHelp=ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(profileHelp.window!!.decorView.findViewWithTag<View>("help_body_${R.string.help_record_fields_body}"))
            testBack(profileHelp);shadowOf(Looper.getMainLooper()).idle()
            assertTrue(profile.isShowing);assertEquals(expanded,header.expanded)
            assertSame(scroll,testDescendants(root).filterIsInstance<ScrollView>().single())
            assertEquals(before,RideRuntime.get(a).bikes.current())
            val description=root.findViewWithTag<TextView>("track_recording_description")
            assertNotNull(description);assertEquals(a.getString(R.string.track_recording_description),description.text.toString())
        } finally { host.pause().stop().destroy() }
    }
}
