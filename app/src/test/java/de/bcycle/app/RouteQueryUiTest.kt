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
    @Test fun popupGroupsApplyImmediatelyKeepInvalidDraftOutAndRetainStateAcrossReopen() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs);val nav=DialogNavigation()
            val anchor=Button(a);a.setContentView(anchor);layout(anchor)
            val query=RouteQuery();var updates=0
            val popup=RouteQueryPopup(a,themes,nav,query) { updates++ }
            popup.filters {};popup.show(anchor);shadowOf(Looper.getMainLooper()).idle();layout(popup.contentView,320,popup.height)
            assertTrue(popup.isFocusable);assertTrue(popup.height<a.resources.displayMetrics.heightPixels)
            val headers=nodes(popup.contentView).filterIsInstance<BicycleSectionHeader>();assertEquals(6,headers.size)
            assertTrue(headers.all { it.expanded==false })
            headers.first().performClick()
            nodes(popup.contentView).filterIsInstance<CheckBox>().first().performClick()
            assertEquals(setOf(RouteKind.IMPORTED),query.kinds)
            val from=popup.contentView.findViewWithTag<EditText>("route_range_${R.string.route_filter_distance}_0")
            val to=popup.contentView.findViewWithTag<EditText>("route_range_${R.string.route_filter_distance}_1")
            headers[3].performClick();from.setText("10");to.setText("5")
            assertEquals(RouteRange(10.0,null),query.distance);assertNotNull(to.error)
            to.setText("20");assertEquals(RouteRange(10.0,20.0),query.distance);assertNull(to.error)
            assertTrue(from.requestFocus());assertNotNull(from.onCreateInputConnection(EditorInfo()))
            popup.contentView.findViewWithTag<EditText>("route_range_${R.string.route_filter_moving}_0").setText("1:30")
            popup.contentView.findViewWithTag<EditText>("route_range_${R.string.route_filter_moving}_1").setText("2:45")
            assertEquals(RouteRange(1.5,2.75),query.moving)
            nodes(popup.contentView).filterIsInstance<Button>().single { it.text==a.getString(R.string.route_week_this) }.performClick()
            assertEquals(java.time.DayOfWeek.MONDAY,query.fromDate!!.dayOfWeek);assertEquals(query.fromDate!!.plusDays(6),query.toDate)
            assertTrue(updates>=4);popup.dismiss()
            val sort=RouteQueryPopup(a,themes,nav,query) {};sort.sorting();sort.show(anchor)
            nodes(sort.contentView).filterIsInstance<RadioButton>().single { it.text==a.getString(R.string.route_filter_distance) }.isChecked=true
            assertEquals(RouteSort.DISTANCE,query.sort);assertEquals(RouteRange(10.0,20.0),query.distance);sort.dismiss()
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
