package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BicycleStickySectionsTest {
    private fun measure(view: View,w: Int,h: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h)
    }
    @Test fun headersPinPushAndReleaseWithoutDuplicatesAndKeepNativeDragScrolling() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val context=controller.get();val themes=ThemeManager(context.getSharedPreferences("sticky",0))
            val body=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
            fun space(height: Int)=View(context).also { body.addView(it,LinearLayout.LayoutParams(-1,height)) }
            space(600)
            val first=BicycleSectionHeader(context,themes,context.getString(R.string.bikes_type));body.addView(first)
            space(900)
            val second=BicycleSectionHeader(context,themes,context.getString(R.string.bikes_sensors));body.addView(second)
            space(900)
            val end=space(1500);second.sectionEnd=end
            val scroll=BicycleStickyScrollView(context).apply { addView(body) };context.setContentView(scroll)
            for(width in listOf(720,1080)) {
                measure(scroll,width,1200)
                scroll.scrollTo(0,first.top+100)
                assertEquals(0f,first.top+first.translationY-scroll.scrollY,0f)
                val push=second.top-first.height/2;scroll.scrollTo(0,push)
                assertEquals((second.top-scroll.scrollY-first.height).toFloat(),first.top+first.translationY-scroll.scrollY,0f)
                scroll.scrollTo(0,second.top+50)
                assertEquals(0f,second.top+second.translationY-scroll.scrollY,0f)
                assertEquals(0f,first.translationY,0f)
                scroll.scrollTo(0,end.top)
                assertEquals(0f,second.translationY,0f)
                scroll.scrollTo(0,0);assertEquals(0f,first.translationY,0f)
                assertEquals(2,testDescendants(body).filterIsInstance<BicycleSectionHeader>().size)
            }
            scroll.scrollTo(0,first.top+50)
            val before=scroll.scrollY;val now=SystemClock.uptimeMillis()
            val start=first.height*.75f
            for((index,move) in listOf(MotionEvent.ACTION_DOWN to start,MotionEvent.ACTION_MOVE to start-120,
                MotionEvent.ACTION_MOVE to start-240,MotionEvent.ACTION_UP to start-240).withIndex()) {
                val (action,y)=move
                val event=MotionEvent.obtain(now,now+index*16L,action,300f,y,0)
                scroll.dispatchTouchEvent(event);event.recycle()
            }
            assertTrue("Drag starting over the pinned header must scroll",scroll.scrollY>before)
            themes.activate("preset:black-ops")
            assertEquals(themes.active.tokens.tertiary,(first.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
            for(header in listOf(first,second)) {
                assertTrue(header.isAccessibilityHeading)
                assertEquals(3,header.childCount)
                val label=header.getChildAt(1) as android.widget.TextView
                assertEquals(android.view.Gravity.CENTER,label.gravity)
                assertEquals(BicycleDesign.narrowTypeface(context),label.typeface)
                assertEquals(BicycleDesign.sectionTitleSp,label.textSize/context.resources.displayMetrics.scaledDensity,.01f)
                assertEquals(label.text.toString().uppercase(java.util.Locale.getDefault()),label.transformationMethod.getTransformation(label.text,label).toString())
                for(index in listOf(0,2)) {
                    assertEquals(BicycleDesign.linePixels(context,BicycleDesign.sectionRuleDp),header.getChildAt(index).height)
                    assertEquals(themes.active.tokens.foreground,(header.getChildAt(index).background as ColorDrawable).color)
                }
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun dialogStylingCannotOverrideGlobalStickyTypographyOrAlterStoredTitles() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val labels=listOf("Strecken-Aufzeichnung","FIT-Aufzeichnung","Tagesbefinden","Apps auswählen","Fahrten sichern","HRV-Pulsdaten","Fahrrad Müller")
            val body=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL }
            val headers=labels.map { title -> BicycleSectionHeader(c,themes,title).also {
                if(title=="FIT-Aufzeichnung") it.titleView.tag="fit_primary_heading"
                body.addView(it)
            } }
            val scroll=BicycleStickyScrollView(c).apply { addView(body) };c.setContentView(scroll)
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                repeat(2) { BicycleDialogStyle.content(scroll,themes) }
                headers.zip(labels).forEach { (header,title) ->
                    val label=header.titleView
                    assertEquals(BicycleDesign.narrowTypeface(c),label.typeface)
                    assertEquals(BicycleDesign.sectionTitleSp,label.textSize/c.resources.displayMetrics.scaledDensity,.01f)
                    assertEquals(title,label.text.toString())
                    assertEquals(title.uppercase(java.util.Locale.getDefault()),label.transformationMethod.getTransformation(label.text,label).toString())
                    assertEquals(title,header.contentDescription)
                    for(index in listOf(0,2)) assertEquals(0f,header.getChildAt(index).alpha,0f)
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun bikesUseFullWidthHeadingsAndSelectedTilesUseAccent() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            runtime.bikes.add()
            runtime.themes.activate("preset:black-ops")
            testDescendants(activity.findViewById(android.R.id.content)).single { it.contentDescription==activity.getString(R.string.ui_menu_open) }.performClick()
            var dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            dialog.window!!.decorView.findViewWithTag<View>("settings_PROFILES").performClick()
            dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val decor=dialog.window!!.decorView;measure(decor,1080,2100)
            assertNotNull(decor.findViewWithTag<View>("bicycle_fullscreen"))
            val scroll=testDescendants(decor).filterIsInstance<BicycleStickyScrollView>().single()
            val body=decor.findViewWithTag<View>("bikes_manager")
            val fit=body.findViewWithTag<android.widget.TextView>("fit_primary_heading")
            assertNotNull(fit);assertEquals(BicycleDesign.narrowTypeface(activity),fit.typeface)
            assertEquals(BicycleDesign.sectionTitleSp,fit.textSize/activity.resources.displayMetrics.scaledDensity,.01f)
            val headings=testDescendants(body).filterIsInstance<BicycleSectionHeader>()
            assertTrue(headings.any { it.contentDescription==runtime.bikes.current().name })
            assertEquals(0,testDescendants(body).filterIsInstance<ProfileAccordion>().size)
            for(sensor in listOf("GPS","EBIKE","POWER","HEART")) {
                val toggle=decor.findViewWithTag<android.widget.Switch>("bike_sensor_$sensor")
                assertFalse(toggle.isAccessibilityHeading)
                assertEquals(android.graphics.Typeface.DEFAULT,toggle.typeface)
                assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,BicycleDesign.bodySp,activity.resources.displayMetrics),toggle.textSize,.01f)
            }
            fun border(view: View):Int {
                val bitmap=Bitmap.createBitmap(300,300,Bitmap.Config.ARGB_8888)
                view.background.setBounds(0,0,300,300);view.background.draw(Canvas(bitmap))
                val color=bitmap.getPixel(150,0);bitmap.recycle();return color
            }
            assertEquals(runtime.themes.active.tokens.primary,border(decor.findViewWithTag("bike_profile_0")))
            assertEquals(runtime.themes.active.tokens.foreground,border(decor.findViewWithTag("bike_profile_1")))
            val palettes=ThemePickerView(activity,runtime.themes);measure(palettes,1080,1000)
            assertEquals(127,android.graphics.Color.alpha(border(palettes.findViewWithTag("theme_choice"))))
            val dashboards=DashboardPresetPicker(activity,0,runtime.themes)
            assertEquals(runtime.themes.active.tokens.primary,border(dashboards.findViewWithTag("preset_page_0")))
            val top=Bitmap.createBitmap(decor.width,decor.height,Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(top))
            java.io.File("/tmp/bcycle-bikes-sections.png").outputStream().use { top.compress(Bitmap.CompressFormat.PNG,100,it) }
            top.recycle()
            // Bike headings are now ordinary headings inside an accordion, not sticky.
            scroll.scrollTo(0,60)
            val title=decor.findViewWithTag<View>("bike_profile_name")
            assertTrue(title.parent is BicycleSectionHeader)
            assertEquals(0f,title.translationY,0f)
            val screenshot=Bitmap.createBitmap(decor.width,decor.height,Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(screenshot))
            java.io.File("/tmp/bcycle-profiles.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
            screenshot.recycle()
        } finally { controller.pause().stop().destroy() }
    }
}
