package de.bcycle.app

import android.view.View
import android.view.ViewGroup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
class FullDataDashboardTest {
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    @Test fun fullDataPresetHasThreeFullWidthRowsAndTwoBottomFieldsAndUsesWholeViewport() {
        val t=DashboardTemplate.FULL_DATA
        assertEquals(14,DashboardTemplate.entries.size);assertFalse(t.hasMap)
        assertEquals(listOf(listOf(0),listOf(1),listOf(2),listOf(3,4)),t.rows)
        assertEquals(1500,t.heightForViewport(1500,20))
        for(slot in 0..2) { assertEquals(DashboardTileStyle.ONE,t.tileStyle(slot));assertTrue(t.supportsGraph(slot)) }
        assertEquals(DashboardTileStyle.TWO,t.tileStyle(3))
        val view=DashboardView(RuntimeEnvironment.getApplication()) {}.apply { setTemplate(t) }
        view.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1500,View.MeasureSpec.EXACTLY));view.layout(0,0,960,1500)
        val tiles=views(view).filter { it.tag?.toString()?.startsWith("dashboard_tile_")==true }
        assertEquals(5,tiles.size)
        for(slot in 0..2) assertEquals(960,view.findViewWithTag<View>("dashboard_tile_$slot").width)
        assertEquals(view.findViewWithTag<View>("dashboard_tile_3").width,view.findViewWithTag<View>("dashboard_tile_4").width)
    }
    @Test fun noMapPageHidesMapKeepsToolbarAndEditorExitAndCanReturnToNormalMapPage() {
        val app=RuntimeEnvironment.getApplication()
        app.getSharedPreferences("bike",0).edit().putString("dashboardTemplate_0","FULL_DATA").commit()
        val c=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=c.get();val root=a.window.decorView
            root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2240,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,2240)
            assertEquals(View.GONE,views(root).filterIsInstance<MapPanel>().single().visibility)
            assertEquals(View.VISIBLE,views(root).single { it.tag=="ride_toolbar" }.visibility)
            val runtime=RideRuntime.get(a);val original=runtime.bikes.current()
            val map=views(root).filterIsInstance<MapPanel>().single()
            val toolbar=views(root).single { it.tag=="ride_toolbar" }
            for(type in BikeType.entries.filter { it.available }) {
                runtime.bikes.save(original.copy(type=type))
                map.controlsVisibilityChanged!!.invoke(false)
                assertEquals("Map-free dashboard keeps footer for $type",View.VISIBLE,toolbar.visibility)
            }
            runtime.bikes.save(original)
            val tiles=views(root).filter { it.tag?.toString()?.startsWith("dashboard_tile_")==true }
            val geometry=tiles.map { listOf(it.left,it.top,it.width,it.height) }
            a.setDashboardEditing(true)
            assertTrue(tiles.all { it.foreground is DashboardEditOutline })
            assertEquals(geometry,tiles.map { listOf(it.left,it.top,it.width,it.height) })
            val finish=views(root).single { it.tag=="dashboard_editor_finish_no_map" }
            assertEquals(View.VISIBLE,finish.visibility);finish.performClick();assertFalse(a.dashboardEditing)
            MainActivity::class.java.getDeclaredMethod("changeDashboard",Int::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,1)
            assertEquals(View.VISIBLE,views(root).filterIsInstance<MapPanel>().single().visibility)
            assertEquals(View.GONE,finish.visibility)
            map.controlsVisibilityChanged!!.invoke(false)
            assertEquals("Map dashboard retains auto-hide",View.GONE,toolbar.visibility)
            map.controlsVisibilityChanged!!.invoke(true)
            assertEquals(View.VISIBLE,toolbar.visibility)
        } finally { c.pause().stop().destroy() }
    }
}
