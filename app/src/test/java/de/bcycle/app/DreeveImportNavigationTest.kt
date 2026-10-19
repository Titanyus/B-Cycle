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
    @Test fun managerImportOffersDreeveAndOpensItsOwnScreenWithConfiguration() {
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
            assertEquals("route_import_dreeve",list.adapter.getView(3,null,list).tag)
            list.performItemClick(list.adapter.getView(3,null,list),3,3)
            val import=ShadowAlertDialog.getLatestAlertDialog()
            assertNotSame(manager,import)
            assertNotNull(import.window!!.decorView.findViewWithTag<View>("dreeve_import_screen"))
            assertNotNull(import.window!!.decorView.findViewWithTag<View>("dreeve_import_configure"))
            import.dismiss();manager.dismiss()
        } finally { host.pause().stop().destroy() }
    }
}
