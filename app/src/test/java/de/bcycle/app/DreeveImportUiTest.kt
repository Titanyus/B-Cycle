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
    @Test fun selectionAcrossPagesIncludesSummariesSkipsExistingRowsAndShowsPerRideResults() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val pages=mutableListOf<Int>();var chosen=emptyList<DreeveActivity>()
            val view=DreeveImportView(a,themes,{ pages.add(it) },{ chosen=it },{});a.setContentView(view)
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
    @Test fun missingConfigurationAndEmptyListKeepReloadAndConfigurationReachable() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();var configure=false
            val view=DreeveImportView(a,ThemeManager(a.getSharedPreferences("bike",0)),{},{},{ configure=true });a.setContentView(view)
            view.refresh();view.failed(R.string.dreeve_import_config_missing)
            assertFalse(view.findViewWithTag<View>("dreeve_import_selected").isEnabled)
            view.findViewWithTag<View>("dreeve_import_configure").performClick();assertTrue(configure)
            assertTrue(view.findViewWithTag<View>("dreeve_import_reload").isEnabled)
            assertTrue(view.findViewWithTag<TextView>("dreeve_import_status").text.contains("API-Schlüssel"))
            view.loaded(DreeveActivityPage(emptyList(),1,0,0),emptySet())
            assertFalse(view.findViewWithTag<View>("dreeve_import_selected").isEnabled)
            assertEquals(View.GONE,view.findViewWithTag<View>("dreeve_import_more").visibility)
        } finally { host.pause().stop().destroy() }
    }
}
