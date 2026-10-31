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
import java.time.Duration

@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="de-rDE-mdpi")
class ProfileContextFooterTest {
    @Test fun scrollingSwitchesExactActionsWithHysteresisAndCollapsingDesignRestoresProfile() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,360,800)
            shadowOf(Looper.getMainLooper()).idle()
            val body=testDescendants(root).filterIsInstance<BikesView>().single()
            val scroll=testDescendants(root).filterIsInstance<ScrollView>().single()
            val footer=testDescendants(root).filterIsInstance<ProfileContextFooter>().single()
            assertEquals(View.GONE,footer.visibility)
            root.findViewWithTag<View>("route_footer_toggle").performClick()
            root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,360,800)
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(footer.dashboardMode)
            assertTrue(footer.findViewWithTag<View>("bike_rename").isShown)
            assertFalse(footer.findViewWithTag<View>("dashboard_rename").isShown)
            assertNull(body.findViewWithTag<View>("dashboard_footer_actions"))
            fun top():Int {
                val header=body.designHeader!!;var y=header.top;var parent=header.parent
                while(parent is View && parent!==scroll) { y+=parent.top;parent=parent.parent }
                return y
            }
            fun position(headerY:Int) {
                scroll.scrollTo(0,top()-headerY)
                shadowOf(Looper.getMainLooper()).idle()
                footer.update(scroll,body.designHeader)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
            }
            position(scroll.height/2)
            assertTrue("scroll=${scroll.scrollY}, height=${scroll.height}, content=${body.height}, root=${root.height}, header=${top()}, design=${root.findViewWithTag<View>("profile_section_content_${a.getString(R.string.ui_design_title)}").height}, expanded=${body.designHeader!!.expanded}",footer.dashboardMode)
            val actions=testDescendants(footer).filterIsInstance<BicycleFooterAction>().filter { it.isShown }
            assertEquals(listOf("Dashboard umbenennen","Dashboard bearbeiten","Dashboard löschen"),actions.map { (it.getChildAt(1) as TextView).text.toString() })
            assertFalse(footer.findViewWithTag<View>("bike_rename").isShown)
            position(scroll.height/2+12);assertTrue("scroll=${scroll.scrollY}, height=${scroll.height}, content=${body.height}, root=${root.height}, header=${top()}, design=${root.findViewWithTag<View>("profile_section_content_${a.getString(R.string.ui_design_title)}").height}, expanded=${body.designHeader!!.expanded}",footer.dashboardMode)
            position(scroll.height/2+30);assertFalse(footer.dashboardMode)
            position(scroll.height/2);assertTrue("scroll=${scroll.scrollY}, height=${scroll.height}, content=${body.height}, root=${root.height}, header=${top()}, design=${root.findViewWithTag<View>("profile_section_content_${a.getString(R.string.ui_design_title)}").height}, expanded=${body.designHeader!!.expanded}",footer.dashboardMode)
            val following=(0 until body.childCount).map(body::getChildAt).filterIsInstance<BicycleSectionHeader>()
                .first { it.contentDescription==a.getString(R.string.ui_recording_title) }
            var target=following.top;var outer=following.parent
            while(outer is View && outer!==scroll) { target+=outer.top;outer=outer.parent }
            scroll.scrollTo(0,target);footer.update(scroll,body.designHeader)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
            assertFalse("Later profile sections must restore profile actions",footer.dashboardMode)
            position(scroll.height/2);assertTrue(footer.dashboardMode)
            body.designHeader!!.performClick()
            footer.update(scroll,body.designHeader)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
            assertFalse(footer.dashboardMode)
            assertTrue(footer.findViewWithTag<View>("bike_rename").isShown)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun rebuildingProfileReplacesDashboardActionsWithoutChangingFixedFooter() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a)
            runtime.bikes.add()
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            val body=testDescendants(root).filterIsInstance<BikesView>().single()
            val footer=testDescendants(root).filterIsInstance<ProfileContextFooter>().single()
            assertEquals(View.GONE,footer.visibility)
            root.findViewWithTag<View>("route_footer_toggle").performClick()
            val old=footer.findViewWithTag<View>("dashboard_footer_actions")
            val other=runtime.bikes.all().first { it.slot!=runtime.bikes.activeSlot }
            assertTrue(runtime.switchBike(other.slot));body.refresh()
            assertNotSame(old,footer.findViewWithTag<View>("dashboard_footer_actions"))
            assertSame(footer,testDescendants(root).filterIsInstance<ProfileContextFooter>().single())
            assertNull(old.parent)
            assertTrue(footer.findViewWithTag<View>("bike_rename").isShown)
        } finally { host.pause().stop().destroy() }
    }
}
