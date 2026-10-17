package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="de-rDE-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StickyAccordionSettingsTest {
    private fun layout(v:View,width:Int=1080) {
        v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2400,View.MeasureSpec.EXACTLY))
        v.layout(0,0,width,2400)
    }
    @Test fun profileSectionsCollapseIndependentlyRemainFullWidthAndKeepFitSelections() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val r=RideRuntime.get(a);r.themes.activate("preset:black-ops")
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            layout(root)
            val bikes=root.findViewWithTag<BikesView>("bikes_manager")
            fun header(id:Int)=testDescendants(root).filterIsInstance<BicycleSectionHeader>().single { it.contentDescription==a.getString(id) }
            fun body(h:BicycleSectionHeader)=(h.parent as ViewGroup).let { it.getChildAt(it.indexOfChild(h)+1) }
            val fit=header(R.string.ui_recording_title);val track=header(R.string.ui_track_recording)
            for(width in listOf(720,1080)) {
                layout(root,width)
                for(h in listOf(fit,track,header(R.string.bikes_sensors),header(R.string.ui_settings_map))) {
                    assertSame(bikes,h.parent);assertEquals(bikes.width,h.width);assertEquals(0,h.left)
                    assertEquals(true,h.expanded)
                    assertEquals(r.themes.active.tokens.tertiary,(h.background as GradientDrawable).color!!.defaultColor)
                    assertEquals(h.titleView.compoundDrawablesRelative[0]!!.bounds.width(),h.titleView.compoundDrawablesRelative[2]!!.bounds.width())
                }
            }
            testDescendants(root).filterIsInstance<Spinner>().forEach { v ->
                val parent=v.parent as View
                assertTrue(v.bottom<=parent.height)
                if(parent.tag==BicycleDesign.inlineControlRowTag) {
                    assertEquals(parent.height/2f,v.top+v.height/2f,1f)
                }
            }
            val checkbox=root.findViewWithTag<CheckBox>("record_NORMALIZED_POWER")
            checkbox.isChecked=true
            val expected=r.config
            fit.performClick();layout(root)
            assertEquals(false,fit.expanded);assertEquals(View.GONE,body(fit).visibility)
            assertEquals(View.VISIBLE,body(track).visibility);assertEquals(expected,r.config)
            track.performClick();layout(root)
            assertEquals(View.GONE,body(track).visibility)
            fit.performClick();track.performClick();layout(root)
            assertTrue(checkbox.isChecked);assertEquals(expected,r.config)
            // A currently pinned header remains reachable when its content is folded away.
            val scroll=testDescendants(root).filterIsInstance<BicycleStickyScrollView>().single()
            val sensor=header(R.string.bikes_sensors)
            scroll.scrollTo(0,sensor.top+100);assertTrue(sensor.translationY>0)
            sensor.performClick();shadowOf(Looper.getMainLooper()).idle();layout(root)
            assertEquals(View.GONE,body(sensor).visibility)
            val visibleTop=sensor.top+sensor.translationY-scroll.scrollY
            assertTrue(visibleTop>=0f);assertTrue(visibleTop+sensor.height<=scroll.height)
            val folded=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
            root.draw(Canvas(folded));java.io.File("/tmp/bcycle-sticky-accordions-folded.png").outputStream().use { folded.compress(Bitmap.CompressFormat.PNG,100,it) };folded.recycle()
            assertTrue(r.addBike());bikes.refresh();shadowOf(Looper.getMainLooper()).idle();layout(root)
            assertEquals(false,header(R.string.bikes_sensors).expanded)
            val name=root.findViewWithTag<BicycleSectionHeader>("bike_name_header")
            assertNull(name.expanded);name.performClick()
            root.findViewWithTag<EditText>("bike_profile_name_edit").apply { setText("Mein Indoor-Rad");onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE) }
            assertEquals("Mein Indoor-Rad",r.bikes.current().name);assertEquals(View.VISIBLE,name.visibility);assertNull(name.expanded)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun outlinedButtonsAndDropdownsFollowThemeWithoutChangingPlainInputs() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("new-control-style",0))
            val root=LinearLayout(a)
            val button=BicycleComponents.action(a,themes,"Aktion") {}
            BicycleDialogStyle.control(button,themes)
            val spinner=Spinner(a).apply { adapter=ArrayAdapter(a,android.R.layout.simple_spinner_dropdown_item,listOf("Eins","Zwei")) }
            BicycleDialogStyle.spinner(spinner,themes)
            val input=BicycleComponents.input(a,themes);BicycleDialogStyle.control(input,themes)
            root.addView(button);root.addView(spinner);root.addView(input);a.setContentView(root)
            for(theme in listOf("preset:black-ops","preset:solar-flare")) {
                themes.activate(theme)
                for(v in listOf(button,spinner)) {
                    val bitmap=Bitmap.createBitmap(600,160,Bitmap.Config.ARGB_8888)
                    v.background.setBounds(0,0,600,160);v.background.draw(Canvas(bitmap))
                    val border=BicycleDesign.dp(a,BicycleDesign.controlBorder)
                    assertEquals(themes.active.tokens.foreground,bitmap.getPixel(1,80))
                    assertEquals(themes.active.tokens.foreground,bitmap.getPixel(border-1,80))
                    assertEquals(themes.active.tokens.secondary,bitmap.getPixel(border+2,80))
                    assertEquals(themes.active.tokens.secondary,bitmap.getPixel(300,80))
                    bitmap.recycle()
                }
                assertEquals(themes.active.tokens.secondary,(spinner.popupBackground as GradientDrawable).color!!.defaultColor)
                val bitmap=Bitmap.createBitmap(600,160,Bitmap.Config.ARGB_8888)
                input.background.setBounds(0,0,600,160);input.background.draw(Canvas(bitmap))
                assertEquals(themes.active.tokens.tertiary,bitmap.getPixel(300,80));bitmap.recycle()
            }
        } finally { host.pause().stop().destroy() }
    }
}
