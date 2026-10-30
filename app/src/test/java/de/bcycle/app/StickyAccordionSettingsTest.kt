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
    @Test fun profileSectionsCollapseIndependentlyKeepGlowInsetsAndFitSelections() {
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
                    assertSame(bikes,h.parent);val inset=BicycleDesign.dp(a,BicycleDesign.sectionCardInset);assertEquals(bikes.width-2*inset,h.width);assertEquals(inset,h.left)
                    assertEquals(true,h.expanded)
                    assertEquals(r.themes.active.tokens.tertiary,(h.background as GradientDrawable).color!!.defaultColor)
                    assertNotNull(h.titleView.compoundDrawablesRelative[2])
                }
            }
            testDescendants(root).filterIsInstance<Spinner>().forEach { v ->
                val parent=v.parent as View
                assertTrue(v.bottom<=parent.height)
                if(parent.tag==BicycleDesign.inlineControlRowTag) {
                    assertEquals(parent.height/2f,v.top+v.height/2f,1f)
                }
            }
            val trackBody=body(track) as ViewGroup
            val metadata=root.findViewWithTag<View>("fit_metadata")
            assertSame(trackBody,metadata.parent)
            assertNull(body(fit).findViewWithTag<View>("fit_metadata"))
            val items=(0 until trackBody.childCount).map(trackBody::getChildAt).filter {
                it.tag=="fit_metadata" || it.tag?.toString()?.startsWith("fit_source_")==true
            }
            assertSame(metadata,items.first())
            val fields=testDescendants(trackBody).filter { it.tag?.toString()?.startsWith("record_")==true }
            assertTrue(fields.isNotEmpty());assertTrue(fields.all { it is android.widget.Switch })
            val checkbox=root.findViewWithTag<android.widget.Switch>("record_NORMALIZED_POWER")
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
            assertEquals(true,name.expanded);root.findViewWithTag<View>("bike_profile_name").performClick()
            root.findViewWithTag<EditText>("bike_profile_name_edit").apply { setText("Mein Indoor-Rad");onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE) }
            assertEquals("Mein Indoor-Rad",r.bikes.current().name);assertEquals(View.VISIBLE,name.visibility);assertEquals(true,name.expanded)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun outlinedControlsAndInputsFollowThemeWithTwoDpQuarterOpacityBorders() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("new-control-style",0))
            val root=LinearLayout(a)
            val button=BicycleComponents.action(a,themes,"Aktion") {}
            BicycleDialogStyle.control(button,themes)
            val spinner=Spinner(a).apply { adapter=ArrayAdapter(a,android.R.layout.simple_spinner_dropdown_item,listOf("Eins","Zwei")) }
            BicycleDialogStyle.spinner(spinner,themes)
            val input=BicycleComponents.input(a,themes);BicycleDialogStyle.control(input,themes)
            val number=EditText(a);BicycleForm.outlinedInput(number,themes,numeric=true)
            root.addView(button);root.addView(spinner);root.addView(input);root.addView(number);a.setContentView(root)
            for(theme in listOf("preset:black-ops","preset:solar-flare")) {
                themes.activate(theme)
                for(v in listOf(button,spinner,input,number)) {
                    val bitmap=Bitmap.createBitmap(600,160,Bitmap.Config.ARGB_8888)
                    val base=(v.background as? GradientDrawable) ?: ((v.background as android.graphics.drawable.LayerDrawable).getDrawable(0) as GradientDrawable)
                    val fill=base.color?.defaultColor ?: android.graphics.Color.TRANSPARENT
                    val underlay=themes.active.tokens.secondary
                    bitmap.eraseColor(underlay)
                    v.background.setBounds(0,0,600,160);v.background.draw(Canvas(bitmap))
                    val border=BicycleDesign.dp(a,BicycleDesign.controlBorder)
                    assertEquals(BicycleDesign.dp(a,2),border)
                    val foreground=themes.active.tokens.foreground
                    val expected=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
                    val center=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
                    Canvas(center).apply { drawColor(underlay);drawColor(fill) }
                    Canvas(expected).apply { drawColor(underlay);drawColor(fill);drawColor((foreground and 0xffffff) or (63 shl 24)) }
                    for(x in listOf(1,border-1)) for(shift in listOf(0,8,16,24)) {
                        assertEquals(((expected.getPixel(0,0) ushr shift) and 255).toDouble(),
                            ((bitmap.getPixel(x,80) ushr shift) and 255).toDouble(),1.0)
                    }
                    val centerColor=center.getPixel(0,0);center.recycle()
                    expected.recycle()
                    assertEquals(centerColor,bitmap.getPixel(border+2,80))
                    assertEquals(centerColor,bitmap.getPixel(300,80))
                    assertEquals(underlay,bitmap.getPixel(0,0))
                    bitmap.recycle()
                }
                assertEquals(themes.active.tokens.tertiary,(spinner.popupBackground as GradientDrawable).color!!.defaultColor)
                val bitmap=Bitmap.createBitmap(600,160,Bitmap.Config.ARGB_8888)
                input.background.setBounds(0,0,600,160);input.background.draw(Canvas(bitmap))
                assertEquals(android.graphics.Color.TRANSPARENT,bitmap.getPixel(300,80));bitmap.recycle()
            }
        } finally { host.pause().stop().destroy() }
    }
}
