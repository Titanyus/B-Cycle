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
                assertEquals(BicycleDesign.dp(context,BicycleDesign.sectionHeaderTopSpace),header.getChildAt(0).height)
                assertEquals(themes.active.tokens.secondary,(header.getChildAt(0).background as ColorDrawable).color)
                val label=header.getChildAt(2) as android.widget.TextView
                assertEquals(android.view.Gravity.CENTER,label.gravity)
                assertEquals(BicycleDesign.strongTypeface,label.typeface)
                for(index in listOf(1,3)) {
                    assertEquals(1,header.getChildAt(index).height)
                    assertEquals(themes.active.tokens.foreground,(header.getChildAt(index).background as ColorDrawable).color)
                }
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun bikesUseFullWidthHeadingsAndSelectedTilesUseAccent() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            runtime.themes.activate("preset:black-ops")
            testDescendants(activity.findViewById(android.R.id.content)).single { it.contentDescription==activity.getString(R.string.ui_menu_open) }.performClick()
            var dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            dialog.window!!.decorView.findViewWithTag<View>("settings_SENSORS").performClick()
            dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val decor=dialog.window!!.decorView;measure(decor,1080,2100)
            assertNotNull(decor.findViewWithTag<View>("bicycle_fullscreen"))
            val scroll=testDescendants(decor).filterIsInstance<BicycleStickyScrollView>().single()
            val body=decor.findViewWithTag<View>("bikes_manager")
            val headings=testDescendants(body).filterIsInstance<BicycleSectionHeader>()
            assertEquals(listOf(activity.getString(R.string.bikes_type),activity.getString(R.string.bikes_sensors)),headings.map { it.contentDescription })
            assertTrue(headings.all { it.width==scroll.width && it.left==0 })
            for(sensor in listOf("GPS","EBIKE","POWER","HEART")) {
                val toggle=decor.findViewWithTag<android.widget.Switch>("bike_sensor_$sensor")
                assertTrue(toggle.isAccessibilityHeading)
                assertEquals(BicycleDesign.strongTypeface,toggle.typeface)
                assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,BicycleDesign.titleSp,activity.resources.displayMetrics),toggle.textSize,.01f)
            }
            fun border(view: View):Int {
                val bitmap=Bitmap.createBitmap(300,300,Bitmap.Config.ARGB_8888)
                view.background.setBounds(0,0,300,300);view.background.draw(Canvas(bitmap))
                val color=bitmap.getPixel(150,0);bitmap.recycle();return color
            }
            assertEquals(runtime.themes.active.tokens.accent,border(decor.findViewWithTag("bike_profile_0")))
            assertEquals(runtime.themes.active.tokens.foreground,border(decor.findViewWithTag("bike_profile_1")))
            val palettes=ThemePickerView(activity,runtime.themes);measure(palettes,1080,1000)
            assertEquals(runtime.themes.active.tokens.accent,border(palettes.findViewWithTag("preset:black-ops")))
            val dashboards=DashboardPresetPicker(activity,List(4) { DashboardTemplate.HERO },0,runtime.themes)
            assertEquals(runtime.themes.active.tokens.accent,border(dashboards.findViewWithTag("preset_HERO")))
            val top=Bitmap.createBitmap(decor.width,decor.height,Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(top))
            java.io.File("/tmp/bcycle-bikes-sections.png").outputStream().use { top.compress(Bitmap.CompressFormat.PNG,100,it) }
            top.recycle()
            // A genuine native screenshot of the prototype, with a pinned header and live theme.
            scroll.scrollTo(0,headings.first().top+60)
            val nav=decor.findViewWithTag<View>("bicycle_navigation_header")
            val location=IntArray(2);headings.first().getLocationOnScreen(location)
            val origin=IntArray(2);scroll.getLocationOnScreen(origin)
            assertEquals("pinned heading location",origin[1],location[1])
            assertEquals(nav.bottom,scroll.top)
            val screenshot=Bitmap.createBitmap(decor.width,decor.height,Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(screenshot))
            assertEquals(runtime.themes.active.tokens.primary,screenshot.getPixel(decor.width-20,nav.height/2))
            java.io.File("/tmp/bcycle-bikes-sticky.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
            screenshot.recycle()
            // The actual native button can be raised/pressed. It must remain behind the
            // opaque pinned heading, including the new top separation band.
            val sensorHeader=headings.last()
            val search=decor.findViewWithTag<View>("bike_sensor_search")
            search.elevation=BicycleDesign.dp(activity,2).toFloat()
            search.translationZ=BicycleDesign.dp(activity,6).toFloat()
            search.isPressed=true
            scroll.scrollTo(0,search.top-40)
            assertTrue(sensorHeader.translationY>0)
            assertTrue(sensorHeader.z>search.z)
            val pinned=Bitmap.createBitmap(decor.width,decor.height,Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(pinned))
            val pinnedOrigin=IntArray(2);sensorHeader.getLocationOnScreen(pinnedOrigin)
            val decorOrigin=IntArray(2);decor.getLocationOnScreen(decorOrigin)
            val x=decor.width/2
            val y=pinnedOrigin[1]-decorOrigin[1]
            val gap=BicycleDesign.dp(activity,BicycleDesign.sectionHeaderTopSpace)
            assertEquals(runtime.themes.active.tokens.secondary,pinned.getPixel(x,y+gap/2))
            assertEquals(runtime.themes.active.tokens.tertiary,pinned.getPixel(x,y+gap+5))
            java.io.File("/tmp/bcycle-bikes-sticky-sensors.png").outputStream().use { pinned.compress(Bitmap.CompressFormat.PNG,100,it) }
            pinned.recycle()
        } finally { controller.pause().stop().destroy() }
    }
}
