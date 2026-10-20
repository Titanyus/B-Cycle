package de.bcycle.app

import android.app.Activity
import android.view.View
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class DreeveImportUiTest {
    private fun activity(id:String,gpx:Boolean=true)=DreeveActivity(id,"Fahrt $id","2026-10-19T08:00:00",12000.0,"Ride",gpx)
    @Test fun all398ActivitiesAcrossEightApiPagesRemainScrollableAndSelectableInFullscreen() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        val server=okhttp3.mockwebserver.MockWebServer();server.start()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            var chosen=emptyList<DreeveActivity>()
            val view=DreeveImportView(a,themes,{},{ chosen=it })
            val dialog=android.app.AlertDialog.Builder(a).setTitle(R.string.dreeve_import_title).setView(view).create()
            dialog.show();BicycleDialogStyle.apply(dialog,themes);BicycleFullscreen.apply(dialog,themes)
            val list=view.findViewWithTag<ListView>("dreeve_import_list")
            val styled=list.adapter
            BicycleDialogStyle.apply(dialog,themes)
            assertSame(styled,list.adapter)
            var updates=0
            list.adapter.registerDataSetObserver(object:android.database.DataSetObserver() {
                override fun onChanged() { updates++ }
            })
            val decor=dialog.window!!.decorView
            fun layout() {
                decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                decor.layout(0,0,360,800)
            }
            for(page in 1..8) {
                val rows=org.json.JSONArray()
                for(id in ((page-1)*50+1)..minOf(page*50,398)) rows.put(org.json.JSONObject()
                    .put("id",id.toString().padStart(3,'0')).put("name","Fahrt $id").put("startDateLocal","2026-10-19T08:00:00")
                    .put("sportType",if(id%2==0) "VirtualRide" else "Ride").put("hasGpx",id%2!=0))
                server.enqueue(okhttp3.mockwebserver.MockResponse().setBody(org.json.JSONObject().put("activities",rows)
                    .put("pagination",org.json.JSONObject().put("page",page).put("size",50).put("totalPages",8).put("total",398)).toString()))
            }
            val handler=android.os.Handler(android.os.Looper.getMainLooper())
            DreeveClient(ActivityServiceConfig(),"test-key",base=server.url("/")).allActivities { page ->
                handler.post { view.loaded(page,emptySet(),continuing=page.page<page.totalPages) }
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if(page.page==1) layout()
            }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(8,updates)
            assertEquals(399,list.adapter.count) // 398 rides plus exactly one continuation footer.
            assertEquals(398,(0 until list.adapter.count).count { list.adapter.getItem(it) is DreeveActivity })
            assertEquals("398 von 398 Fahrten geladen",view.findViewWithTag<TextView>("dreeve_import_status").text.toString())
            layout()
            repeat(400) { list.scrollListBy(400) }
            val last=list.findViewWithTag<CheckBox>("dreeve_activity_398")
            assertNotNull(last);assertTrue(last.isEnabled);last.performClick()
            decor.findViewWithTag<View>("dreeve_import_selected").performClick()
            assertEquals(listOf("398"),chosen.map { it.id })
            view.cancelImport()
            decor.findViewWithTag<View>("dreeve_select_filtered").performClick()
            decor.findViewWithTag<View>("dreeve_import_selected").performClick()
            assertEquals(398,chosen.size);assertEquals(398,chosen.map { it.id }.toSet().size)
            for(page in 1..8) assertEquals(page.toString(),server.takeRequest().requestUrl!!.queryParameter("pagination[page]"))
            dialog.dismiss()
        } finally { server.shutdown();host.pause().stop().destroy() }
    }
    @Test fun selectionAcrossPagesIncludesSummariesSkipsExistingRowsAndShowsPerRideResults() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val pages=mutableListOf<Int>();var chosen=emptyList<DreeveActivity>()
            val view=DreeveImportView(a,themes,{ pages.add(it) },{ chosen=it });a.setContentView(view)
            view.refresh();assertEquals(listOf(1),pages)
            view.loaded(DreeveActivityPage(listOf(activity("a"),activity("no-gpx",false),activity("old")),1,2,4),setOf("old"))
            val list=view.findViewWithTag<ListView>("dreeve_import_list")
            fun row(index:Int)=list.adapter.getView(index,null,list) as CheckBox
            assertTrue(row(0).isEnabled);assertTrue(row(1).isEnabled);assertFalse(row(2).isEnabled)
            row(0).performClick();row(1).performClick()
            view.findViewWithTag<View>("dreeve_import_more").performClick();assertEquals(listOf(1,2),pages)
            view.loaded(DreeveActivityPage(listOf(activity("b")),2,2,4),emptySet())
            assertTrue(row(0).isChecked);row(1).performClick()
            view.findViewWithTag<View>("dreeve_import_selected").performClick()
            assertEquals(listOf("a","no-gpx","b"),chosen.map { it.id });assertFalse(row(0).isEnabled)
            view.progress(DreeveItemResult("a",DreeveImportState.IMPORTED,setOf(DreeveDataField.HEART)))
            view.complete(Result.success(DreeveImportResult(listOf(DreeveItemResult("a",DreeveImportState.IMPORTED,setOf(DreeveDataField.HEART)),
                DreeveItemResult("b",DreeveImportState.FAILED,problem=R.string.dreeve_import_download_failed)))))
            assertFalse(row(0).isEnabled);assertTrue(row(0).text.contains("Puls"))
            assertTrue(row(1).isEnabled && row(1).isChecked)
            assertTrue(view.findViewWithTag<TextView>("dreeve_import_status").text.contains("1 importiert"))
            assertEquals(View.GONE,view.findViewWithTag<View>("dreeve_import_more").visibility)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun missingConfigurationAndEmptyListKeepReloadReachableWithoutConfigurationAction() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get()
            val view=DreeveImportView(a,ThemeManager(a.getSharedPreferences("bike",0)),{},{});a.setContentView(view)
            view.refresh();view.failed(R.string.dreeve_import_config_missing)
            assertFalse(view.findViewWithTag<View>("dreeve_import_selected").isEnabled)
            assertNull(view.findViewWithTag<View>("dreeve_import_configure"))
            assertTrue(view.findViewWithTag<View>("dreeve_import_reload").isEnabled)
            assertTrue(view.findViewWithTag<TextView>("dreeve_import_status").text.contains("API-Schlüssel"))
            view.loaded(DreeveActivityPage(emptyList(),1,0,0),emptySet())
            assertFalse(view.findViewWithTag<View>("dreeve_import_selected").isEnabled)
            assertEquals(View.GONE,view.findViewWithTag<View>("dreeve_import_more").visibility)
        } finally { host.pause().stop().destroy() }
    }
}
