package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardPresentationTest {
    private val context get()=RuntimeEnvironment.getApplication()
    @Test fun independentSwitchAccordionsRemainScrollableAndDataToggleIsAboveTheSourceDropdown() {
        val themes=ThemeManager(context.getSharedPreferences("sections",0))
        val graph=GraphOverlayPicker(context,themes,emptyList());val glow=ZoneGlowPicker(context,themes,ZoneGlowSource.OFF)
        var chosen: DashboardField?=DashboardField("SPEED")
        val picker=FieldPickerView(context,LiveData(),GpsValues(),null,PulseValues(),chosen,false,themes,graph,glow) { chosen=it }
        assertEquals(View.GONE,picker.findViewWithTag<View>("field_data_content").visibility)
        assertFalse(testDescendants(picker).any { it.tag=="graph_display_mode" })
        picker.findViewWithTag<View>("field_data_toggle").performClick()
        graph.findViewWithTag<View>("graph_overlay_toggle").performClick()
        glow.findViewWithTag<View>("zone_glow_toggle").performClick()
        picker.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY));picker.layout(0,0,360,640)
        val scroll=picker.findViewWithTag<ScrollView>("field_picker_scroll")
        assertTrue(scroll.height<=640);assertTrue(scroll.getChildAt(0).height>scroll.height)
        val list=picker.findViewWithTag<LinearLayout>("field_list")
        val source=picker.findViewWithTag<Spinner>("field_source")
        val none=picker.findViewWithTag<android.widget.Switch>("field_data_enabled")
        assertNull(picker.findViewWithTag<View>("dashboard_field_empty"));assertTrue(source.bottom<=list.top)
        assertEquals(1,testDescendants(picker).filterIsInstance<ScrollView>().size)
        assertTrue(list.getChildAt(list.childCount-1).bottom<=list.height)
        graph.findViewWithTag<View>("graph_SPEED").performClick()
        glow.findViewWithTag<View>("zone_glow_POWER_CURRENT").performClick()
        none.performClick();assertNull(chosen)
        assertEquals(listOf(AnalysisChart.SPEED),graph.selected);assertEquals(ZoneGlowSource.POWER_CURRENT,glow.selected)
        assertFalse(testDescendants(list).any { it===graph || it===glow })
    }
    @Test fun actualEditorKeepsDataGraphAndDashboardTrainingHelpIndependentAndRemembersHiddenMetric() {
        val prefs=context.getSharedPreferences("bike",0)
        prefs.edit().putString(GraphDisplaySettings.key,GraphDisplayMode.GRAPH_ONLY.name).commit()
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();activity.setDashboardEditing(true)
            fun choose():android.app.AlertDialog {
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("dashboard_tile_2").performClick()
                shadowOf(Looper.getMainLooper()).idle();return ShadowAlertDialog.getLatestAlertDialog()
            }
            val screen=choose();val root=screen.window!!.decorView
            root.findViewWithTag<View>("graph_SPEED").performClick()
            assertTrue(DashboardDataVisibility.read(prefs,0).visible[2]) // old global flag cannot hide data when adding a graph
            root.findViewWithTag<RadioButton>("training_help_all").isChecked=true
            root.findViewWithTag<View>("field_data_enabled").performClick()
            assertFalse(DashboardDataVisibility.read(prefs,0).visible[2])
            assertEquals("SPEED",DashboardLayout.decode(prefs.getString("dashboard",null)).fields[2].id)
            assertEquals(listOf(AnalysisChart.SPEED),GraphOverlayConfig.read(prefs,0).slots[2])
            assertEquals(TrainingHelpMode.ALL_ZONES,DashboardTrainingHelp.read(prefs,0).mode)
            val reopened=choose()
            assertFalse(reopened.window!!.decorView.findViewWithTag<android.widget.Switch>("field_data_enabled").isChecked)
            val graph=testDescendants(reopened.window!!.decorView).filterIsInstance<GraphOverlayPicker>().single()
            graph.findViewWithTag<Switch>("graph_overlay_enabled").performClick()
            assertFalse(DashboardDataVisibility.read(prefs,0).visible[2]);assertTrue(GraphOverlayConfig.read(prefs,0).slots[2].isEmpty())
            assertEquals(TrainingHelpMode.ALL_ZONES,DashboardTrainingHelp.read(prefs,0).mode)
            testBack(reopened)
            val dashboard=testDescendants(activity.findViewById(android.R.id.content)).filterIsInstance<DashboardView>().single()
            val tile=dashboard.findViewWithTag<View>("dashboard_tile_2")
            val value=testDescendants(tile).filterIsInstance<TextView>().single { it.text.toString()=="—" && it.parent is android.view.ViewGroup && it.visibility==View.VISIBLE }
            assertEquals(View.INVISIBLE,(value.parent as View).visibility)
            val again=choose();again.window!!.decorView.findViewWithTag<View>("SPEED").performClick()
            assertTrue(DashboardDataVisibility.read(prefs,0).visible[2]);assertTrue(GraphOverlayConfig.read(prefs,0).slots[2].isEmpty())
            assertEquals(TrainingHelpMode.ALL_ZONES,DashboardTrainingHelp.read(prefs,0).mode)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun dataAndTrendCategoriesKeepCompactHeadingsAndSharedSeparatorsAfterDialogStyling() {
        val themes=ThemeManager(context.getSharedPreferences("compact-field-categories",0))
        val picker=FieldPickerView(context,LiveData(),GpsValues(),null,PulseValues(),DashboardField("GPS_SPEED"),false,themes,
            trendChanged={}) {}
        fun verify(group:LinearLayout) {
            val children=(0 until group.childCount).map { group.getChildAt(it) }
            val headings=children.filterIsInstance<TextView>().filter { it.tag=="bicycle_content_heading" }
            assertTrue(headings.isNotEmpty())
            assertEquals(headings.size-1,children.count { it.tag=="bicycle_section_divider" })
            headings.forEach { heading ->
                assertEquals(BicycleDesign.contentHeadingSp,heading.textSize/context.resources.displayMetrics.scaledDensity,.01f)
                assertEquals(BicycleDesign.strongTypeface,heading.typeface)
            }
        }
        BicycleDialogStyle.content(picker,themes)
        val source=picker.findViewWithTag<Spinner>("field_source")
        for(i in 0 until source.adapter.count) {
            source.setSelection(i);shadowOf(Looper.getMainLooper()).idle()
            BicycleDialogStyle.content(picker,themes)
            verify(picker.findViewWithTag("field_list"))
        }
        verify(picker.findViewWithTag("field_trend_details"))
    }
    @Test fun legacyGraphOnlyMigratesOnlyWhereGraphsWereVisibleAndExplicitChoicesSurvivePresets() {
        val prefs=context.getSharedPreferences("visibility-migration",0)
        prefs.edit().putString(GraphDisplaySettings.key,GraphDisplayMode.GRAPH_ONLY.name).commit()
        assertTrue(DashboardDataVisibility.read(prefs,0).visible.all { it })
        prefs.edit().putString(GraphOverlayConfig.key(0),GraphOverlayConfig().replace(2,listOf(AnalysisChart.SPEED)).encode()).commit()
        assertFalse(DashboardDataVisibility.read(prefs,0).visible[2])
        assertTrue(DashboardDataVisibility.read(prefs,0).visible[0])
        prefs.edit().putString("dashboardTemplate_0",DashboardTemplate.BALANCED.name).commit()
        assertTrue(DashboardDataVisibility.read(prefs,0).visible[2])
        val config=DashboardDataVisibility().replace(2,false)
        prefs.edit().putString(DashboardDataVisibility.key(0),config.encode()).commit()
        DashboardTemplate.entries.forEach { template -> prefs.edit().putString("dashboardTemplate_0",template.name).commit();assertEquals(config,DashboardDataVisibility.read(prefs,0)) }
        assertTrue(DashboardDataVisibility.read(prefs,1).visible.all { it })
    }
    @Test fun backupRoundTripsVisibilityAndRejectsNonbooleanValuesWithoutChangingPreferences() {
        val prefs=context.getSharedPreferences("visibility-backup",0);val route=context.getSharedPreferences("visibility-route",0)
        val config=DashboardDataVisibility().replace(2,false)
        prefs.edit().putString(DashboardDataVisibility.key(0),config.encode()).commit()
        val backup=SettingsBackup(prefs,route,ThemeManager(prefs))
        val bytes=backup.export(setOf(BackupSection.DASHBOARD))
        val doc=backup.decode(bytes);val plan=backup.plan(doc,doc.sections.keys)
        assertEquals(config.encode(),plan.bike[DashboardDataVisibility.key(0)])
        prefs.edit().remove(DashboardDataVisibility.key(0)).commit();backup.apply(plan)
        assertEquals(config,DashboardDataVisibility.read(prefs,0))
        val json=JSONObject(String(bytes));val pages=json.getJSONObject("sections").getJSONObject("dashboard").getJSONArray("pages")
        repeat(pages.length()) { pages.getJSONObject(it).remove("dataVisible") }
        val old=backup.decode(json.toString().toByteArray())
        assertFalse(backup.plan(old,old.sections.keys).bike.keys.any { it.startsWith("dashboardDataVisible") })
        pages.getJSONObject(0).put("dataVisible",JSONArray(List(8) { "false" }))
        try { backup.decode(json.toString().toByteArray());fail("Nonboolean visibility accepted") } catch(e:SettingsBackupFailure) { assertEquals(BackupProblem.INVALID,e.problem) }
        assertEquals(config,DashboardDataVisibility.read(prefs,0))
    }
}
