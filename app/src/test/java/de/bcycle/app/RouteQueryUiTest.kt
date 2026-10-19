package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class RouteQueryUiTest {
    private fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    private fun layout(v:View,w:Int=360,h:Int=800) {
        v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h)
    }
    @Test fun sixBikesUseThreeColumnsAndRangeHandlesCannotCross() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            val profiles=BikeProfileStore(a,prefs);repeat(5) { assertNotNull(profiles.add()) }
            val grid=BikeFilterGrid(a,themes,profiles.all(),emptySet()) { _,_-> }
            a.setContentView(grid);layout(grid,320,400)
            assertEquals(6,grid.childCount)
            assertEquals(grid.getChildAt(0).top,grid.getChildAt(2).top)
            assertTrue(grid.getChildAt(3).top>grid.getChildAt(0).top)
            assertTrue(grid.getChildAt(2).right<=320)
            var pair=0.0 to 0.0
            val slider=BicycleRangeSlider(a,themes,"Range",{ it.toString() }) { from,to -> pair=from to to }
            slider.show(10.0,.1,2.0,8.0)
            val args=android.os.Bundle().apply { putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,9f) }
            slider.accessibilityNodeProvider!!.performAction(0,android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,args)
            assertEquals(8.0 to 8.0,pair)
            slider.accessibilityNodeProvider!!.performAction(1,android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null)
            assertEquals(8.0 to 8.0,pair)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun popupGroupsApplyImmediatelyKeepInvalidDraftOutAndRetainStateAcrossReopen() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs);val nav=DialogNavigation()
            val anchor=Button(a);a.setContentView(anchor);layout(anchor)
            val query=RouteQuery();var updates=0
            val popup=RouteQueryPopup(a,themes,nav,query) { updates++ }
            popup.filters {};popup.show(anchor);shadowOf(Looper.getMainLooper()).idle();layout(popup.contentView,320,popup.height)
            assertTrue(popup.isFocusable);assertTrue(popup.height<a.resources.displayMetrics.heightPixels)
            val headers=nodes(popup.contentView).filterIsInstance<Button>().filter { it.tag?.toString()?.startsWith("route_query_group_")==true };assertEquals(6,headers.size)
            assertTrue(headers.all { it.stateDescription==a.getString(R.string.zone_collapsed) })
            assertTrue(popup.height<500)
            assertEquals(1,(popup.contentView as LinearLayout).childCount)
            fun group(index:Int):View {
                headers[index].performClick();shadowOf(Looper.getMainLooper()).idle()
                return popup.groupPopup!!.contentView
            }
            var choices=group(0)
            assertNull((choices as ScrollView).getChildAt(0).findViewWithTag<View>(BicycleDesign.inlineControlRowTag).background)
            choices.findViewWithTag<View>("route_kind_IMPORTED").performClick()
            assertEquals(setOf(RouteKind.IMPORTED),query.kinds)
            choices.findViewWithTag<View>("route_kind_EBIKE").performClick()
            assertEquals(setOf(RouteKind.IMPORTED,RouteKind.EBIKE),query.kinds)
            choices.findViewWithTag<View>("route_kind_IMPORTED").performClick()
            assertEquals(setOf(RouteKind.EBIKE),query.kinds)
            choices=group(1)
            choices.findViewWithTag<View>("route_bike_0").performClick()
            assertEquals(setOf(0),query.bikes)
            choices.findViewWithTag<View>("route_bike_0").performClick();assertTrue(query.bikes.isEmpty())
            choices=group(3)
            val from=choices.findViewWithTag<EditText>("route_range_${R.string.route_filter_distance}_0")
            val to=choices.findViewWithTag<EditText>("route_range_${R.string.route_filter_distance}_1")
            val rangeContent=from.parent.parent as View
            assertEquals(BicycleDesign.dp(a,BicycleDesign.controlGap),rangeContent.paddingTop)
            assertEquals(BicycleDesign.dp(a,BicycleDesign.controlGap),rangeContent.paddingBottom)
            fun fill(view:View)=(view.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor
            assertEquals(themes.active.tokens.secondary,fill(headers[3]))
            assertEquals(themes.active.tokens.tertiary,(rangeContent.background as android.graphics.drawable.ColorDrawable).color)
            assertEquals(themes.active.tokens.secondary,fill(from))
            from.setText("10");to.setText("5")
            assertEquals(RouteRange(10.0,null),query.distance);assertNotNull(to.error)
            to.setText("20");assertEquals(RouteRange(10.0,20.0),query.distance);assertNull(to.error)
            val slider=choices.findViewWithTag<BicycleRangeSlider>("route_range_slider_${R.string.route_filter_distance}")
            assertEquals(10.0,slider.lower,0.0001);assertEquals(20.0,slider.upper,0.0001)
            val args=android.os.Bundle().apply { putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,15f) }
            assertTrue(slider.accessibilityNodeProvider!!.performAction(0,android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,args))
            assertEquals(RouteRange(15.0,20.0),query.distance);assertEquals("15",from.text.toString())
            assertTrue(from.requestFocus());assertNotNull(from.onCreateInputConnection(EditorInfo()))
            choices=group(4)
            choices.findViewWithTag<EditText>("route_range_${R.string.route_filter_moving}_0").setText("1:30")
            choices.findViewWithTag<EditText>("route_range_${R.string.route_filter_moving}_1").setText("2:45")
            assertEquals(RouteRange(1.5,2.75),query.moving)
            choices=group(2)
            nodes(choices).filterIsInstance<Button>().single { it.text==a.getString(R.string.route_week_this) }.performClick()
            assertEquals(java.time.DayOfWeek.MONDAY,query.fromDate!!.dayOfWeek);assertEquals(query.fromDate!!.plusDays(6),query.toDate)
            assertTrue(updates>=4);popup.dismiss()
            val sort=RouteQueryPopup(a,themes,nav,query) {};sort.sorting();sort.show(anchor)
            shadowOf(Looper.getMainLooper()).idle()
            layout(sort.contentView,sort.width,sort.height)
            val menuScroll=(sort.contentView as LinearLayout).getChildAt(0) as ScrollView
            assertTrue("Menu content ${menuScroll.getChildAt(0).height}, viewport ${menuScroll.height}",menuScroll.getChildAt(0).height<=menuScroll.height)
            val sortHeader=sort.contentView.findViewWithTag<View>("route_query_group_${R.string.route_sort_field}")
            sortHeader.performClick()
            val sortChoice=nodes(sort.groupPopup!!.contentView).filterIsInstance<RadioButton>().single { it.text==a.getString(R.string.route_filter_distance) }
            val sortContent=sortChoice.parent.parent as View
            assertEquals(BicycleDesign.dp(a,BicycleDesign.controlGap),sortContent.paddingBottom)
            sortChoice.isChecked=true
            assertEquals(RouteSort.DISTANCE,query.sort);assertEquals(RouteRange(15.0,20.0),query.distance)
            sort.groupPopup!!.dismiss()
            sort.contentView.findViewWithTag<View>("route_query_group_${R.string.route_sort_direction}").performClick()
            val directionPopup=sort.groupPopup!!
            layout(directionPopup.contentView,directionPopup.width,directionPopup.height)
            val directionScroll=directionPopup.contentView as ScrollView
            assertTrue("Direction content ${directionScroll.getChildAt(0).height}, viewport ${directionScroll.height}",directionScroll.getChildAt(0).height<=directionScroll.height)
            sort.dismiss();assertNull(sort.groupPopup)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun stickyHeadingsBecomeAccordionsOnceAndRenameHeadingKeepsItsClick() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("accordion-test",0))
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL }
            val name=BicycleSectionHeader(a,themes,"Bike").apply { tag="bike_name_header" }
            var renamed=false;name.setOnClickListener { renamed=true };root.addView(name)
            val header=BicycleForm.section(root,themes,R.string.data_routes)
            val input=EditText(a).apply { setText("keep") };root.addView(input)
            val scroll=BicycleStickyScrollView(a).apply { addView(root) };a.setContentView(scroll);layout(scroll)
            assertEquals(true,header.expanded);assertNull(name.expanded)
            val parent=input.parent;header.performClick();assertEquals(View.GONE,(input.parent as View).visibility)
            layout(scroll);header.performClick();assertSame(parent,input.parent);assertEquals("keep",input.text.toString())
            name.performClick();assertTrue(renamed);assertNull(name.expanded)
        } finally { host.pause().stop().destroy() }
    }
}
