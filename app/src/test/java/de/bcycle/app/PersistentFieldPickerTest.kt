package de.bcycle.app

import android.view.View
import android.widget.RadioButton
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class PersistentFieldPickerTest {
    @Test fun upperAndMapSelectionsStayOpenUntilBackAndKeepTheirCheckedStateAndTrendAvailability() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val root=activity.window.decorView;val prefs=RideRuntime.get(activity).preferences
            root.findViewWithTag<View>("dashboard_tile_2").performLongClick()
            root.findViewWithTag<View>("dashboard_tile_2").performClick()
            val picker=ShadowAlertDialog.getLatestAlertDialog();val content=picker.window!!.decorView
            content.findViewWithTag<View>("field_data_toggle").performClick()
            selectTestSource(picker,"E-Bike")
            val cadence=content.findViewWithTag<RadioButton>("CADENCE")
            cadence.performClick();assertTrue(picker.isShowing);assertTrue(cadence.isChecked)
            assertEquals("CADENCE",DashboardLayout.decode(prefs.getString("dashboard",null)).fields[2].id)
            content.findViewWithTag<RadioButton>("ODOMETER").performClick()
            assertTrue(picker.isShowing);assertFalse(cadence.isChecked)
            assertEquals(View.VISIBLE,content.findViewWithTag<Switch>("field_trend_indicator").visibility)
            val empty=content.findViewWithTag<RadioButton>("dashboard_field_empty")
            empty.performClick();assertTrue(picker.isShowing);assertTrue(empty.isChecked)
            assertFalse(DashboardDataVisibility.read(prefs,0).visible[2])
            selectTestSource(picker,"Pulsmesser")
            content.findViewWithTag<RadioButton>("HEART_RATE").performClick()
            assertTrue(picker.isShowing);assertFalse(empty.isChecked)
            assertEquals(View.VISIBLE,content.findViewWithTag<Switch>("field_trend_indicator").visibility)
            selectTestSource(picker,"E-Bike")
            assertFalse(content.findViewWithTag<RadioButton>("CADENCE").isChecked)
            selectTestSource(picker,"Pulsmesser")
            assertTrue(content.findViewWithTag<RadioButton>("HEART_RATE").isChecked)
            content.findViewWithTag<Switch>("field_trend_indicator").performClick()
            assertTrue(picker.isShowing);assertFalse(TrendIndicatorSettings.read(prefs).enabled)
            content.findViewWithTag<View>("graph_SPEED").performClick()
            assertTrue(picker.isShowing);assertEquals(listOf(AnalysisChart.SPEED),GraphOverlayConfig.read(prefs,0).slots[2])
            testBack(picker);assertFalse(picker.isShowing)
            root.findViewWithTag<View>("map_value_0").performClick()
            val map=ShadowAlertDialog.getLatestAlertDialog();val fields=map.window!!.decorView
            selectTestSource(map,"E-Bike")
            fields.findViewWithTag<RadioButton>("CADENCE").performClick()
            assertTrue(map.isShowing);assertEquals("CADENCE",MapFieldSelection.read(prefs,0)?.id)
            fields.findViewWithTag<RadioButton>("map_field_empty").performClick()
            assertTrue(map.isShowing);assertNull(MapFieldSelection.read(prefs,0))
            assertFalse(fields.findViewWithTag<RadioButton>("CADENCE").isChecked)
            selectTestSource(map,"Pulsmesser")
            fields.findViewWithTag<RadioButton>("HEART_RATE").performClick()
            assertTrue(map.isShowing);assertEquals("HEART_RATE",MapFieldSelection.read(prefs,0)?.id)
            testBack(map);assertFalse(map.isShowing)
        } finally { controller.pause().stop().destroy() }
    }
}
