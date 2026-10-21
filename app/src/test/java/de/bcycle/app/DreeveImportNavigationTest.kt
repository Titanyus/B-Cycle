package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.ListPopupWindow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class DreeveImportNavigationTest {
    @Test fun managerKeepsUnconfiguredDreeveVisibleButDisabledAndImportScreenHasNoConfigurationAction() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val state=RideRuntime.get(a).mapState
            val deadline=System.nanoTime()+10_000_000_000L
            while(state.busy && System.nanoTime()<deadline) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5) }
            assertFalse(state.busy)
            MainActivity::class.java.getDeclaredMethod("routeSettings").apply { isAccessible=true }.invoke(a)
            while(state.busy && System.nanoTime()<deadline) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5) }
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(state.busy)
            val manager=ShadowAlertDialog.getLatestAlertDialog()
            manager.window!!.decorView.findViewWithTag<View>("route_import").performClick()
            val popup=MainActivity::class.java.getDeclaredField("routePopup").apply { isAccessible=true }.get(a) as ListPopupWindow
            val list=popup.listView!!
            assertEquals(4,list.adapter.count)
            val dreeve=list.adapter.getView(3,null,list)
            assertEquals("route_import_dreeve",dreeve.tag)
            assertFalse(list.adapter.isEnabled(3));assertFalse(dreeve.isEnabled);assertEquals(.5f,dreeve.alpha,0f)
            assertTrue((0..2).all { list.adapter.isEnabled(it) })
            list.performItemClick(list.adapter.getView(3,null,list),3,3)
            assertSame(manager,ShadowAlertDialog.getLatestAlertDialog())
            MainActivity::class.java.getDeclaredMethod("showDreeveImport").apply { isAccessible=true }.invoke(a)
            val import=ShadowAlertDialog.getLatestAlertDialog()
            assertNotSame(manager,import)
            assertNotNull(import.window!!.decorView.findViewWithTag<View>("dreeve_import_screen"))
            assertNull(import.window!!.decorView.findViewWithTag<View>("dreeve_import_configure"))
            val decor=import.window!!.decorView
            decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,800)
            shadowOf(Looper.getMainLooper()).idle()
            val content=decor.findViewWithTag<View>("dreeve_import_screen")
            val screen=content as DreeveImportView
            screen.loaded(DreeveActivityPage(listOf(
                DreeveActivity("old","Alt","",null,"Ride",true),
                DreeveActivity("new","Neu","",null,"Ride",true)),1,1,2),setOf("old"))
            val listView=screen.findViewWithTag<android.widget.ListView>("dreeve_import_list")
            assertEquals(3,listView.adapter.count)
            assertEquals("2 Fahrten geladen",screen.findViewWithTag<android.widget.TextView>("dreeve_import_status").text.toString())
            assertTrue(screen.getChildAt(0) is android.widget.TextView)
            assertFalse((screen.getChildAt(0) as android.widget.TextView).text.contains("GPX"))
            decor.findViewWithTag<View>("dreeve_filters").performClick()
            val queryPopup=DreeveImportView::class.java.getDeclaredField("queryPopup").apply { isAccessible=true }.get(content) as RouteQueryPopup
            assertTrue(queryPopup.isShowing)
            val importStatus=queryPopup.contentView.findViewWithTag<View>("route_query_group_${R.string.dreeve_filter_import_status}")
            assertEquals(0,(importStatus.parent as android.view.ViewGroup).indexOfChild(importStatus))
            importStatus.performClick()
            queryPopup.groupPopup!!.contentView.findViewWithTag<View>("dreeve_filter_new").performClick()
            assertEquals(2,listView.adapter.count) // One new ride and the continuation footer.
            assertEquals("new",(listView.adapter.getItem(0) as DreeveActivity).id)
            screen.progress(DreeveItemResult("new",DreeveImportState.IMPORTED))
            assertEquals(1,listView.adapter.count)
            queryPopup.dismiss()
            decor.findViewWithTag<View>("dreeve_sort").performClick()
            val sorting=DreeveImportView::class.java.getDeclaredField("queryPopup").apply { isAccessible=true }.get(content) as RouteQueryPopup
            assertTrue(sorting.isShowing);sorting.dismiss()
            import.dismiss();manager.dismiss()
        } finally { host.pause().stop().destroy() }
    }
}
