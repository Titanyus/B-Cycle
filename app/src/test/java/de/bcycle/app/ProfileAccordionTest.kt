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
            assertEquals(themes.active.tokens.foreground,title.titleView.currentTextColor)
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
    @Test fun longProfileUsesFullWidthAccentSectionsAndKeepsDataVisible() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val r=RideRuntime.get(a);r.themes.activate("preset:black-ops")
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            measure(root,1080,2400)
            val categories=testDescendants(root).filterIsInstance<BicycleSectionHeader>()
            for(id in listOf(R.string.bikes_sensors,R.string.ui_recording_title,R.string.ui_settings_map,R.string.ui_design_title,R.string.training_help_title)) {
                val header=categories.single { it.contentDescription==a.getString(id) }
                assertEquals(LinearLayout.VERTICAL,header.orientation)
                assertEquals(r.themes.active.tokens.foreground,header.titleView.currentTextColor)
                assertNull(header.sectionEnd)
            }
            for(id in listOf(R.string.profiles_bike,R.string.bikes_type,R.string.ui_theme,R.string.ui_dashboard_page_choose,R.string.ui_mode_glow))
                assertFalse(categories.any { it.contentDescription==a.getString(id) })
            assertTrue(testDescendants(root).filterIsInstance<ProfileAccordion>().isEmpty())
            assertEquals(View.VISIBLE,root.findViewWithTag<View>("bike_fit_settings").visibility)
            assertEquals("Indoor",a.getString(R.string.bikes_type_indoor))
            val texts=testDescendants(root).filterIsInstance<android.widget.TextView>()
            assertFalse(texts.any { it.text==a.getString(R.string.ui_map_source) })
            assertFalse(texts.any { it.text==a.getString(R.string.ui_mode_glow_description) })
            val fixed=root.findViewWithTag<View>("bicycle_fullscreen_footer")
            assertNull(fixed.findViewWithTag<View>("dashboard_editor_open"))
            assertNotNull(root.findViewWithTag<View>("dashboard_inline_actions").findViewWithTag<View>("dashboard_editor_open"))
            val scroll=testDescendants(root).filterIsInstance<BicycleStickyScrollView>().single()
            for((id,file) in listOf(R.string.ui_design_title to "design",R.string.ui_recording_title to "fit")) {
                val heading=categories.single { it.contentDescription==a.getString(id) }
                scroll.scrollTo(0,0);measure(root,1080,2400)
                scroll.scrollTo(0,top(heading,scroll))
                val bitmap=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                java.io.File("/tmp/bcycle-profile-long-$file.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                bitmap.recycle()
            }
            r.themes.activate(BicycleThemes.red.id)
            assertTrue(categories.all { it.titleView.currentTextColor==r.themes.active.tokens.foreground })
        } finally { host.pause().stop().destroy() }
    }
}
