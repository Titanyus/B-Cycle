package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
class ProfileAccordionTest {
    private fun measure(view:View,w:Int=1080,h:Int=2100) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
        view.layout(0,0,w,h)
    }
    private fun top(view:View,scroll:View):Int {
        var y=view.top;var p=view.parent
        while(p is View && p!==scroll) { y+=p.top;p=p.parent }
        return y
    }
    @Test fun connectedPanelAndInlineRulesKeepTheirGeometryAndFollowTheme() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("panel-theme",0))
            val body=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL }
            val title=BicycleSectionHeader(c,themes,"Aufzuzeichnende Werte",inlineRules=true)
            body.addView(title)
            body.addView(View(c),LinearLayout.LayoutParams(-1,600))
            val group=ProfileAccordion(c,themes,"FIT-Aufzeichnung","test",BicycleIcon.RecordingSettings,body,true) {}
            host.get().setContentView(group)
            for(width in listOf(960,1080)) {
                measure(group,width,1500)
                val frame=group.findViewWithTag<View>("profile_content_test")
                assertTrue(frame.left>group.header.left)
                assertTrue(frame.top<group.header.bottom)
                assertTrue(title.sectionEnd!=null)
                assertEquals(LinearLayout.HORIZONTAL,title.orientation)
                assertTrue(title.getChildAt(0).width>0);assertTrue(title.getChildAt(2).width>0)
                assertEquals(title.height/2f,title.getChildAt(0).top+title.getChildAt(0).height/2f,1f)
                assertEquals(title.width/2f,(title.titleView.left+title.titleView.right)/2f,1f)
                assertEquals(BicycleDesign.strongTypeface,title.titleView.typeface)
                assertTrue(title.titleView.width<=title.width)
                assertEquals(1,testDescendants(group).count { it.tag=="profile_accordion_test" })
            }
            themes.activate("preset:black-ops")
            val foreground=themes.active.tokens.foreground
            assertEquals(foreground,title.titleView.currentTextColor)
            val bitmap=Bitmap.createBitmap(group.header.width,group.header.height,Bitmap.Config.ARGB_8888)
            group.header.draw(Canvas(bitmap))
            assertEquals(foreground,bitmap.getPixel(1,bitmap.height/2))
            assertEquals(themes.active.tokens.tertiary,bitmap.getPixel(bitmap.width/2,bitmap.height-15))
            assertEquals(themes.active.tokens.tertiary,bitmap.getPixel(bitmap.width-1,bitmap.height/2))
            assertEquals(themes.active.tokens.tertiary,bitmap.getPixel(bitmap.width/2,bitmap.height-1))
            bitmap.recycle()
            group.header.performClick();assertFalse(group.isOpen);assertEquals(View.GONE,body.visibility)
            group.header.performClick();assertTrue(group.isOpen);assertEquals(View.VISIBLE,body.visibility)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun pinnedCategoryStopsAtItsOwnAccordionEvenWhenNextAccordionIsClosed() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("panel-scope",0))
            val content=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL }
            val category=BicycleSectionHeader(c,themes,"Sensoren",inlineRules=true)
            content.addView(category);content.addView(View(c),LinearLayout.LayoutParams(-1,1800))
            val group=ProfileAccordion(c,themes,"Bike","bike",BicycleIcon.Bikes,content,true) {}
            val root=LinearLayout(c).apply {
                orientation=LinearLayout.VERTICAL
                addView(group)
                addView(ProfileAccordion(c,themes,"Karten","maps",BicycleIcon.Map,View(c),false) {})
                addView(View(c),LinearLayout.LayoutParams(-1,1800))
            }
            val scroll=BicycleStickyScrollView(c).apply { addView(root) };host.get().setContentView(scroll)
            measure(scroll,1080,1200)
            val natural=top(category,scroll);val end=top(category.sectionEnd!!,scroll)
            scroll.scrollTo(0,natural+100)
            assertEquals(scroll.scrollY.toFloat(),natural+category.translationY,1f)
            scroll.scrollTo(0,end-category.height/2)
            assertTrue(natural+category.translationY+category.height<=end+1)
            scroll.scrollTo(0,end+20);assertEquals(0f,category.translationY,0f)
            group.header.performClick();measure(scroll,1080,1200)
            assertEquals(0f,category.translationY,0f)
            assertFalse(group.isOpen)
            group.header.performClick();measure(scroll,1080,1200)
            scroll.scrollTo(0,top(category,scroll)+100)
            assertTrue(category.translationY>0)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun realProfileHasRequestedStickyCategoriesAndPreviewKeepsSingleFooter() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val r=RideRuntime.get(a);r.themes.activate("preset:black-ops")
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            val categories=testDescendants(root).filterIsInstance<BicycleSectionHeader>()
            for(id in listOf(R.string.bikes_type,R.string.bikes_sensors,R.string.bikes_fit_heading,R.string.bikes_fit_additional)) {
                val category=categories.single { it.contentDescription==a.getString(id) }
                assertEquals(LinearLayout.HORIZONTAL,category.orientation)
                assertNotNull(category.sectionEnd)
            }
            assertEquals(5,testDescendants(root).filterIsInstance<ProfileAccordion>().size)
            root.findViewWithTag<View>("profile_accordion_profiles_bike").performClick()
            root.findViewWithTag<View>("profile_accordion_profiles_design").performClick()
            shadowOf(Looper.getMainLooper()).idle();measure(root,1080,2400)
            assertEquals("Indoor",a.getString(R.string.bikes_type_indoor))
            assertFalse(testDescendants(root).filterIsInstance<android.widget.TextView>().any { it.text==a.getString(R.string.ui_map_source) })
            val owner=root.findViewWithTag<BikesView>("bikes_manager")
            val design=root.findViewWithTag<ProfileAccordion>("profile_group_profiles_design")
            val next=root.findViewWithTag<ProfileAccordion>("profile_group_training_help_title")
            assertTrue(next.top>design.bottom)
            val bridge=Bitmap.createBitmap(owner.width,4,Bitmap.Config.ARGB_8888)
            val bridgeCanvas=Canvas(bridge)
            bridgeCanvas.drawColor(r.themes.active.tokens.secondary)
            bridgeCanvas.translate(0f,-((design.bottom+next.top)/2f-2f))
            owner.draw(bridgeCanvas)
            val axis=(design.left+design.connectionX).toInt()
            assertEquals(r.themes.active.tokens.foreground,bridge.getPixel(axis,2))
            assertEquals(r.themes.active.tokens.secondary,bridge.getPixel(axis+12,2))
            bridge.recycle()
            val scroll=testDescendants(root).filterIsInstance<BicycleStickyScrollView>().single()
            scroll.scrollTo(0,top(root.findViewWithTag("profile_accordion_ui_recording_title"),scroll))
            assertNotNull(root.findViewWithTag<View>("bicycle_fullscreen_footer").findViewWithTag<View>("profile_copy"))
            val bitmap=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            java.io.File("/tmp/bcycle-profile-panels.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
            root.findViewWithTag<View>("profile_accordion_profiles_design").performClick()
            root.findViewWithTag<View>("profile_accordion_ui_recording_title").performClick()
            shadowOf(Looper.getMainLooper()).idle();measure(root,1080,2400)
            scroll.scrollTo(0,top(root.findViewWithTag("profile_accordion_ui_recording_title"),scroll))
            val fit=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
            root.draw(Canvas(fit))
            java.io.File("/tmp/bcycle-profile-fit-panel.png").outputStream().use { fit.compress(Bitmap.CompressFormat.PNG,100,it) }
            fit.recycle()
        } finally { host.pause().stop().destroy() }
    }
}
