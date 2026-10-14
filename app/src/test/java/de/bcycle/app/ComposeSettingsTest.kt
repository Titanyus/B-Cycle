package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComposeSettingsTest {
    @get:Rule val ui=createComposeRule()
    private val context get()=RuntimeEnvironment.getApplication()
    private fun themes()=ThemeManager(context.getSharedPreferences("compose-test",0))
    @Test fun menuRowsUseExistingActionsAndAdvancedIsASeparateDestination() {
        val chosen=mutableListOf<SettingsAction>()
        ui.setContent { BicycleComposeTheme(themes()) { ComposeSettingsMenu(false) { chosen+=it } } }
        ui.onNodeWithTag("settings_PROFILE").assertExists().performClick()
        ui.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_ADVANCED"))
        ui.onNodeWithTag("settings_ADVANCED").performClick()
        assertEquals(listOf(SettingsAction.PROFILE,SettingsAction.ADVANCED),chosen)
        ui.onNodeWithTag("settings_FONT_LICENSE").assertDoesNotExist()
    }
    @Test fun fieldSourceChangesPreserveGraphChoiceAndModeAndLimitToTwo() {
        var graphs=listOf(AnalysisChart.SPEED);var mode=GraphDisplayMode.WITH_DATA
        ui.setContent { BicycleComposeTheme(themes()) {
            ComposeFieldPicker(LiveData(),GpsValues(),null,PulseValues(),DashboardField("SPEED"),false,graphs,mode,{}, { selected,display -> graphs=selected;mode=display })
        } }
        ui.onNodeWithTag("field_list").performScrollToNode(hasTestTag("graph_CADENCE"))
        ui.onNodeWithTag("graph_CADENCE").performClick()
        assertEquals(listOf(AnalysisChart.SPEED,AnalysisChart.CADENCE),graphs)
        ui.onNodeWithTag("field_list").performScrollToNode(hasTestTag("graph_POWER"))
        ui.onNodeWithTag("graph_POWER").assertIsNotEnabled()
        ui.onNodeWithTag("field_list").performScrollToNode(hasTestTag("graph_display_mode"))
        ui.onNodeWithTag("graph_display_mode").performClick()
        ui.onNodeWithTag("graph_display_mode:1").performClick();assertEquals(GraphDisplayMode.GRAPH_ONLY,mode)
        ui.onNodeWithTag("field_source").performClick();ui.onNodeWithTag("field_source:0").performClick()
        assertEquals(2,graphs.size);assertEquals(GraphDisplayMode.GRAPH_ONLY,mode)
    }
    @Test fun validProfileEditsPersistAndInvalidInputDoesNotOverwriteLastValue() {
        val store=RiderProfileStore(context.getSharedPreferences("compose-profile",0))
        ui.setContent { BicycleComposeTheme(themes()) { ComposeRiderProfile(store) { _,_-> } } }
        ui.onNodeWithTag("rider_height").performScrollTo().performTextInput("180")
        assertEquals(180,store.read().heightCm)
        ui.onNodeWithTag("rider_height").performTextReplacement("9999")
        assertEquals(180,store.read().heightCm)
        ui.onNodeWithTag("rider_height").performTextReplacement("181")
        assertEquals(181,store.read().heightCm)
    }
    @Test fun exposingStoredPasswordDoesNotTriggerPersistenceAndTogglesVisibility() {
        val draft=ComposeExportDraft(RouteExportConfig())
        var writes=0
        ui.setContent { BicycleComposeTheme(themes()) { ComposeExportSettings(draft,RouteExportPart.CLOUD,true,{ "test-only-password" },{},{}) { _,_->writes++ } } }
        ui.onNodeWithTag("export_password_visibility").performScrollTo().performClick()
        assertTrue(draft.visible);assertEquals(0,writes)
        ui.onNodeWithTag("export_password_visibility").performClick()
        assertFalse(draft.visible);assertEquals(0,writes)
    }
    @Test fun mapOptionsUseExistingKeysAndNotifyOnlyOnUserSelection() {
        val prefs=context.getSharedPreferences("compose-map",0);var refreshes=0
        ui.setContent { BicycleComposeTheme(themes()) { ComposeMapSettings(prefs,{ refreshes++ },{},{}) } }
        assertEquals(0,refreshes)
        ui.onNodeWithTag("mapFollow").performClick()
        assertFalse(prefs.getBoolean("mapFollow",true));assertEquals(1,refreshes)
        ui.onNodeWithTag("map_source").performClick();ui.onNodeWithTag("map_source:1").performClick()
        assertEquals(MapSource.entries[1].name,prefs.getString("mapSource",null))
    }
    @Test fun backupSelectionRetainsAvailableSectionsAndAllNoneAreRealActions() {
        var selected=BackupSection.entries.toSet()
        val available=setOf(BackupSection.DESIGN,BackupSection.DASHBOARD)
        val state=androidx.compose.runtime.mutableStateOf(available)
        ui.setContent { BicycleComposeTheme(themes()) { ComposeBackupSelection(available,state.value) { state.value=it;selected=it } } }
        ui.onNodeWithTag("backup_none").performClick();assertTrue(selected.isEmpty())
        ui.onNodeWithTag("backup_all").performClick();assertEquals(available,selected)
        ui.onNodeWithTag("backup_cloud").assertDoesNotExist()
    }
    @Test fun dashboardPresetsAndThemesKeepIndependentPageSettingsAndUseTheExistingManager() {
        val prefs=context.getSharedPreferences("compose-design",0);val manager=ThemeManager(prefs)
        ui.setContent { BicycleComposeTheme(manager) { ComposeDesignSettings(prefs,manager,0,{},{}) } }
        ui.onNodeWithTag("preset_HERO").performScrollTo().performClick()
        assertEquals("HERO",prefs.getString("dashboardTemplate_0",null))
        ui.onNodeWithTag("preset_page_1").performScrollTo().performClick()
        ui.onNodeWithTag("preset_HERO_SEVEN").performScrollTo().performClick()
        assertEquals("HERO",prefs.getString("dashboardTemplate_0",null));assertEquals("HERO_SEVEN",prefs.getString("dashboardTemplate_1",null))
        ui.onNodeWithTag("preset:black-ops").performScrollTo().performClick()
        assertEquals("preset:black-ops",manager.active.id)
        ui.onNodeWithTag("theme_reserved").assertIsNotEnabled()
    }
    @Test fun emptyMapFieldAndNoGraphsRemainAvailableWithoutASecondDialog() {
        var chosen:DashboardField?=DashboardField("SPEED");var selected=listOf(AnalysisChart.SPEED)
        ui.setContent { BicycleComposeTheme(themes()) { ComposeFieldPicker(LiveData(),GpsValues(),null,PulseValues(),chosen,true,null,GraphDisplayMode.WITH_DATA,{ chosen=it },{ _,_-> }) } }
        ui.onNodeWithTag("map_field_empty").performClick();assertNull(chosen)
        ui.onNodeWithTag("graph_none").assertDoesNotExist()
    }

    @Test fun sectionTypographyAndForegroundFollowTheExistingTokensAfterThemeSwitch() {
        val manager=themes();val label=context.getString(R.string.rider_personal)
        ui.setContent { BicycleComposeTheme(manager) { BicycleSection(label) } }
        fun layout():androidx.compose.ui.text.TextLayoutResult {
            val values=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            ui.onNodeWithText(label).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { assertTrue(it(values)) }
            return values.single()
        }
        assertEquals(23f,layout().layoutInput.style.fontSize.value,.01f)
        assertEquals(900,layout().layoutInput.style.fontWeight!!.weight)
        ui.runOnIdle { assertTrue(manager.activate("preset:electric-blue")) }
        assertEquals(androidx.compose.ui.graphics.Color(manager.active.tokens.foreground),layout().layoutInput.style.color)
    }

}
