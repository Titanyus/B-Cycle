package de.bcycle.app

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SummaryPresentationTest {
    @Test fun onlyEbikeGetsSavedBatteryMetricsAndMissingValuesStayMissing() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val view=RideSummaryView(c,themes,RideCompletion.Completed("ride"))
            for(type in listOf(BikeType.EBIKE,BikeType.INDOOR,BikeType.BIKE)) {
                val entry=ManagedRoute("ride","Fahrt",true,"FIT",RouteDetails(bikeType=type),false)
                val metrics=mapOf(AnalysisMetric.BATTERY_LEVEL to 34.0,AnalysisMetric.BATTERY_TEMPERATURE to 23.5)
                view.show(RouteAnalysis(entry,metrics,null,false,0.0,emptyList()))
                val battery=view.findViewWithTag<ViewGroup>("ride_summary_battery")
                if(type==BikeType.EBIKE) {
                    assertEquals(3,battery.childCount)
                    assertTrue(battery.getChildAt(1).contentDescription.contains("34"))
                    assertTrue(battery.getChildAt(2).contentDescription.contains("23"))
                    view.show(RouteAnalysis(entry,emptyMap(),null,false,0.0,emptyList()))
                    assertTrue(view.findViewWithTag<ViewGroup>("ride_summary_battery").getChildAt(0).contentDescription.contains(c.getString(R.string.analysis_missing)))
                } else assertNull(battery)
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun summaryBatteryThresholdsAreInclusiveAndShareDashboardColors() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val entry=ManagedRoute("ride","Fahrt",true,"FIT",RouteDetails(bikeType=BikeType.EBIKE),false)
            for(level in listOf(0,10,11,20,21,100)) {
                val view=RideSummaryView(c,themes,RideCompletion.Completed("ride",shiftBatteryAtEnd=level))
                view.show(RouteAnalysis(entry,mapOf(AnalysisMetric.BATTERY_LEVEL to level.toDouble()),null,false,0.0,emptyList()))
                c.setContentView(view)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val row=view.findViewWithTag<ViewGroup>("ride_summary_battery")
                for(index in 0..1) {
                    val text=testDescendants(row.getChildAt(index)).filterIsInstance<android.widget.TextView>().single { it.text.toString()==level.toString() }
                    val expected=when { level<=10->0xffff3030.toInt();level<=20->0xffffb000.toInt();else->themes.active.tokens.foreground }
                    assertEquals(expected,text.currentTextColor)
                }
                assertTrue(row.getChildAt(0).contentDescription.contains(level.toString()))
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun statusGroupsWrapAndStayCenteredAndUseTheSharedStorageIcons() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val view=RideSummaryTransfers(c,themes)
            view.show(RouteTransferTarget.entries.map { SummaryTransfer(it,c.getString(it.title),UploadState.WAITING) })
            for(width in listOf(240,288,328,380)) {
                view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));view.layout(0,0,width,view.measuredHeight)
                val rows=(0 until view.childCount).map(view::getChildAt).groupBy { it.top }
                rows.values.forEach { row ->
                    assertTrue(row.all { it.left>=0 && it.right<=width })
                    assertEquals(row.first().left,width-row.last().right)
                    row.zipWithNext().forEach { (a,b)->assertTrue(b.left-a.right>=BicycleDesign.dp(c,BicycleDesign.rideSummaryStatusGap)) }
                }
            }
            assertEquals(DataAction.LOCAL.icon,RouteTransferTarget.LOCAL.icon)
            assertEquals(DataAction.CLOUD.icon,RouteTransferTarget.WEBDAV.icon)
            assertEquals(DataAction.DREEVE.icon,ActivityService.DREEVE.icon)
            assertEquals(DataAction.DREEVE.icon,RouteTransferTarget.DREEVE.icon)
            assertEquals(DataAction.INTERVALS.icon,RouteTransferTarget.INTERVALS.icon)
            assertEquals(DataAction.STRAVA.icon,RouteTransferTarget.STRAVA.icon)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun blurSurvivesDialogHandoverAndScopedShadowsRemainInBothDesignModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));val root=FrameLayout(c)
            c.setContentView(root)
            val first=DashboardModal.blur(root);val next=DashboardModal.blur(root)
            first.close();first.close();assertTrue(DashboardModal.blurred(root))
            next.close();assertFalse(DashboardModal.blurred(root))
            val screen=BicycleDialogBuilder(c,themes,DialogNavigation(),fullscreen=false,headerRole=ThemeRole.SECONDARY,plainHeader=true,dashboardSurface=true)
                .setTitle(R.string.ride_summary_title).setView(View(c)).show()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            for(flat in listOf(true,false)) {
                themes.setFlatDesign(flat)
                assertEquals(BicycleDesign.dp(c,BicycleDesign.dashboardModalShadow).toFloat(),screen.window!!.decorView.elevation,0f)
                assertEquals(Color.BLACK,screen.window!!.decorView.outlineAmbientShadowColor)
                assertEquals(themes.active.tokens.secondary,(screen.window!!.decorView.findViewWithTag<View>("bicycle_navigation_header").background as ColorDrawable).color)
            }
            screen.dismiss()
        } finally { host.pause().stop().destroy() }
    }
}
