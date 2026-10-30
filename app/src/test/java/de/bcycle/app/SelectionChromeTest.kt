package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectionChromeTest {
    private fun layout(view:View,width:Int) { repeat(3) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(5000,View.MeasureSpec.AT_MOST))
        view.layout(0,0,width,view.measuredHeight)
    } }
    private fun image(view:View)=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    @Test fun fitSourceDropdownsKeepTheEntireBottomBorderInsideTheirRows() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val prefs=c.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            val profile=BikeProfileStore(c,prefs).current()
            for(flat in listOf(true,false)) {
                prefs.edit().putBoolean(BicycleAppearance.flatKey,flat).commit()
                val root=RecordingSourcesView(c,themes,profile,true,RecordingSources.defaults(profile.recording,false),{true}) {}
                c.setContentView(root);shadowOf(Looper.getMainLooper()).idle()
                for(width in listOf(960,1080,1236)) {
                    layout(root,width)
                    val rendered=image(root)
                    testDescendants(root).filterIsInstance<Spinner>().forEach { choice ->
                        val row=choice.parent as ViewGroup
                        assertTrue("${choice.tag} lower edge fits row",choice.bottom<=row.height-row.paddingBottom)
                        val bounds=Rect(0,0,choice.width,choice.height);root.offsetDescendantRectToMyCoords(choice,bounds)
                        // Compare native parent rendering with the actual drawable at the centre of its lower border.
                        val expected=Bitmap.createBitmap(choice.width,choice.height,Bitmap.Config.ARGB_8888)
                        choice.background.draw(Canvas(expected))
                        val y=choice.height-2
                        val color=expected.getPixel(choice.width/2,y)
                        assertTrue(Color.alpha(color)>0)
                        val actual=rendered.getPixel(bounds.centerX(),bounds.top+y)
                        val expectedAlpha=(Color.alpha(color)*choice.alpha).toInt()
                        assertTrue("${choice.tag} bottom border visible ($flat/$width)",kotlin.math.abs(Color.alpha(actual)-expectedAlpha)<=1)
                        // Disabled controls are composited with alpha; premultiplication can round RGB by one.
                        for(channel in listOf(Color::red,Color::green,Color::blue))
                            assertTrue(kotlin.math.abs(channel(color)-channel(actual))<=2)
                        expected.recycle()
                    }
                    rendered.recycle()
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun bikePopupHasNoOuterFillAndKeepsTheLastRowAndItsBottomBorders() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val prefs=c.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            val anchor=View(c);c.setContentView(anchor)
            for(flat in listOf(true,false)) {
                prefs.edit().putBoolean(BicycleAppearance.flatKey,flat).commit()
                val popup=BikeQuickPicker(anchor,themes,(0..5).map { BikeProfile(it,"Fahrrad $it") },2) {}
                assertEquals(Color.TRANSPARENT,(popup.background as ColorDrawable).color)
                assertNull(popup.contentView.background)
                val padding=Rect();popup.background.getPadding(padding);assertTrue(padding.isEmpty)
                val root=popup.contentView as ViewGroup
                for(width in listOf(960,1080,1236)) {
                    layout(root,width)
                    val rendered=image(root)
                    assertEquals(0,Color.alpha(rendered.getPixel(0,0)))
                    val strip=root.findViewWithTag<BikeProfileStrip>("bike_quick_strip")
                    val row=strip.getChildAt(0) as ViewGroup
                    for(i in 0 until row.childCount) {
                        val card=row.getChildAt(i)
                        assertTrue(card.bottom<=strip.height)
                        val bounds=Rect(0,0,card.width,card.height);root.offsetDescendantRectToMyCoords(card,bounds)
                        assertTrue(bounds.bottom<=root.height-root.paddingBottom)
                        val standalone=image(card)
                        assertEquals("Card $i lower border ($flat/$width)",standalone.getPixel(card.width/2,card.height-2),rendered.getPixel(bounds.centerX(),bounds.bottom-2))
                        assertTrue(Color.alpha(rendered.getPixel(bounds.centerX(),bounds.bottom-2))>0)
                        standalone.recycle()
                    }
                    rendered.recycle()
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun bothNotificationPositionGridsUseTheSharedGapBelowTheirHeading() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val prefs=c.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            for(flat in listOf(true,false)) {
                prefs.edit().putBoolean(BicycleAppearance.flatKey,flat).commit()
                for(root in listOf(MessageNotificationSettingsView(c,themes),DrinkReminderSettingsView(c,themes,prefs))) {
                    c.setContentView(root)
                    root.findViewWithTag<View>("drink_reminder_expand")?.performClick()
                    shadowOf(Looper.getMainLooper()).idle();layout(root,1080)
                    val grid=testDescendants(root).filterIsInstance<MessagePositionChoices>().single()
                    val parent=grid.parent as ViewGroup;val heading=parent.getChildAt(parent.indexOfChild(grid)-1)
                    assertEquals(true,heading.getTag(R.id.content_heading_role))
                    assertEquals(BicycleDesign.dp(c,BicycleDesign.controlGap),grid.top-heading.bottom)
                    BicycleDialogStyle.content(root,themes);layout(root,1080)
                    assertEquals(BicycleDesign.dp(c,BicycleDesign.controlGap),grid.top-heading.bottom)
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun actualDashboardLayoutHeadingUsesTheSameGapAsPositionChoices() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val c=host.get();val runtime=RideRuntime.get(c)
            for(flat in listOf(true,false)) {
                runtime.themes.setFlatDesign(flat)
                MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(c)
                val dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
                val root=dialog.window!!.decorView;shadowOf(Looper.getMainLooper()).idle();layout(root,1080)
                val heading=testDescendants(root).filterIsInstance<android.widget.TextView>().single {
                    it.getTag(R.id.content_heading_role)==true && it.text.toString()==c.getString(R.string.ui_dashboard_page_choose)
                }
                val parent=heading.parent as LinearLayout
                val picker=parent.getChildAt(parent.indexOfChild(heading)+1)
                assertTrue(picker is DashboardPresetPicker)
                assertEquals(BicycleDesign.dp(c,BicycleDesign.controlGap),picker.top-heading.bottom)
                BicycleDialogStyle.content(parent,runtime.themes.settings);layout(root,1080)
                assertEquals(BicycleDesign.dp(c,BicycleDesign.controlGap),picker.top-heading.bottom)
                dialog.dismiss()
            }
        } finally { host.pause().stop().destroy() }
    }

}
