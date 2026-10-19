package de.bcycle.app

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RideIdentityUiTest {
    private fun layout(v:View,w:Int=360,h:Int=800) {
        repeat(2) { v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.AT_MOST));v.layout(0,0,w,v.measuredHeight) }
    }
    @Test fun listTitleAlignsWithDateAndFormatDoesNotWrapWhenProfileStripeIsPresent() {
        val c=RuntimeEnvironment.getApplication();val themes=ThemeManager(c.getSharedPreferences("bike",0))
        RideProfileColor.confirm(c,"known",RideProfileColor.snapshot(c))
        val route=ManagedRoute("known","Meine Fahrt",true,"FIT",RouteDetails(1_791_626_400_000,12300.0,movingMs=3600000,bikeType=BikeType.EBIKE),false)
        val manager=RouteManagerView(c,{},themes);manager.submit(listOf(route),null,false)
        val list=manager.findViewWithTag<ListView>("route_list")
        for(width in listOf(320,360,420)) {
            val card=list.adapter.getView(0,null,list) as ViewGroup;layout(card,width)
            val title=card.findViewWithTag<TextView>("route_name_known");val date=card.findViewWithTag<TextView>("route_date_known")
            fun left(v:TextView):Float {
                val r=android.graphics.Rect();card.offsetDescendantRectToMyCoords(v,r)
                return r.left+v.compoundPaddingLeft+(v.layout?.getLineLeft(0) ?: 0f)
            }
            assertEquals(left(date),left(title),1.1f)
            val format=card.findViewWithTag<TextView>("route_format_known")
            assertEquals(1,format.lineCount);assertTrue(format.width>=format.paint.measureText("FIT")-1)
        }
    }
    @Test fun detailsShowProfileStripeBeforeIconForItsFullHeightAndOmitUnknownProfileColor() {
        val c=RuntimeEnvironment.getApplication();val themes=ThemeManager(c.getSharedPreferences("bike",0))
        RideProfileColor.confirm(c,"known",RideProfileColor.snapshot(c))
        val entry=ManagedRoute("known","Fahrt",true,"FIT",RouteDetails(1_791_626_400_000,bikeType=BikeType.EBIKE),false)
        val analysis=RouteAnalysis(entry,emptyMap(),null,false,0.0,emptyList())
        val view=RouteAnalysisView(c,themes,"known") {}
        try {
            view.show(analysis);layout(view)
            val stripe=view.findViewWithTag<View>("analysis_profile_color")
            val icon=view.findViewWithTag<View>("analysis_origin_format")
            assertEquals(icon.height,stripe.height);assertEquals(icon.top,stripe.top);assertTrue(stripe.right<icon.left)
            assertEquals(RideProfileColor.color(c,"known"),(stripe.background as ColorDrawable).color)
        } finally { view.dispose() }
        val old=RouteAnalysisView(c,themes,"unknown") {}
        try { old.show(analysis);assertNull(old.findViewWithTag<View>("analysis_profile_color")) } finally { old.dispose() }
    }
}
