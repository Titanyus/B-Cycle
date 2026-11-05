package de.bcycle.app

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.graphics.Insets
import android.widget.ImageView
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class ProfileSettingsRevisionTest {
    private fun call(a:MainActivity,name:String) { MainActivity::class.java.getDeclaredMethod(name).apply { isAccessible=true }.invoke(a) }
    @Test fun profileOrderAndFramedControlsApplyInBothDesignsAndSupportedBikeTypes() {
        org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences(HrvSettings.STORE,0).edit().putBoolean("showOnFirstStart",false).commit()
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val runtime=RideRuntime.get(a)
            for(flat in listOf(true,false)) for(type in listOf(BikeType.EBIKE,BikeType.INDOOR)) {
                runtime.themes.setFlatDesign(flat);runtime.changeBikeType(type)
                runtime.bikes.save(runtime.bikes.current().copy(heartAddress="00:11:22:33:44:55"))
                call(a,"showSensorManager")
                shadowOf(android.os.Looper.getMainLooper()).idle()
                val dialog=ShadowAlertDialog.getLatestAlertDialog();val root=dialog.window!!.decorView
                val manager=root.findViewWithTag<ViewGroup>("bikes_manager")
                val headers=(0 until manager.childCount).map(manager::getChildAt).filterIsInstance<BicycleSectionHeader>()
                val order=listOf(R.string.bikes_sensors,R.string.ui_design_title,R.string.training_help_title,R.string.ui_settings_map,R.string.bike_recording_title)
                    .filter { it!=R.string.ui_settings_map || type==BikeType.EBIKE }.map(a::getString)
                assertEquals("bike_name_header",headers.first().tag)
                assertEquals(order,headers.map { it.contentDescription.toString() }.filter { it in order })
                assertNull(root.findViewWithTag<View>("bike_autostart_divider"))
                val sensors=root.findViewWithTag<ViewGroup>("profile_section_content_${a.getString(R.string.bikes_sensors)}")
                assertEquals("bike_auto_start_recording",sensors.getChildAt(0).tag)
                assertEquals(a.getString(R.string.bikes_autoconnect),root.findViewWithTag<Switch>("bike_auto_connect").text)
                val heart=root.findViewWithTag<Switch>("bike_sensor_HEART")
                val frame=heart.parent.parent as BicycleFrameDropdown
                assertFalse(frame.expanded);val before=heart.isChecked;heart.performClick()
                assertEquals(!before,runtime.heartEnabled);assertFalse(frame.expanded)
                frame.header.performClick();assertTrue(frame.expanded);heart.performClick();assertTrue(frame.expanded)
                val appearance=root.findViewWithTag<View>("flat_design").parent as ViewGroup
                assertEquals("flat_design",appearance.getChildAt(0).tag)
                assertTrue(appearance.getChildAt(1) is ThemePickerView)
                assertTrue(appearance.getChildAt(2) is DashboardStatusSettingsView)
                assertEquals("design_dashboard_divider",appearance.getChildAt(3).tag)
                assertNull(root.findViewWithTag<View>("design_appearance_divider"))
                val recording=root.findViewWithTag<android.widget.LinearLayout>("bike_recording_settings")
                assertEquals("map_elevation_correction",recording.getChildAt(0).tag)
                assertTrue(recording.getChildAt(1) is RideNamingView)
                assertEquals("recording_options_divider",recording.getChildAt(2).tag)
                val primary=recording.getChildAt(3) as BicycleFrameDropdown
                val stored=recording.getChildAt(4) as BicycleFrameDropdown
                assertEquals(a.getString(R.string.fit_primary_values_title),primary.header.text.toString())
                assertEquals(a.getString(R.string.fit_stored_values_title),stored.header.text.toString())
                assertEquals(a.getString(R.string.bikes_fit_explanation),(primary.content.getChildAt(0) as android.widget.TextView).text.toString())
                assertEquals(6,primary.content.findViewWithTag<RecordingSourcesView>("recording_sources").childCount)
                assertEquals(a.getString(R.string.fit_stored_values_description),(stored.content.getChildAt(0) as android.widget.TextView).text.toString())
                primary.setExpanded(true);stored.setExpanded(true)
                val elevation=recording.getChildAt(0) as Switch
                elevation.performClick()
                assertEquals("root attached=${root.isAttachedToWindow}, manager=${manager.isAttachedToWindow}, elevation attached=${elevation.isAttachedToWindow}, slot=${runtime.bikes.activeSlot}, type=$type, flat=$flat, dialog=${dialog.isShowing}",elevation.isChecked,runtime.preferences.getBoolean(FitElevationCorrection.preference,false))
                val mapSettings=root.findViewWithTag<ViewGroup>("profile_section_content_${a.getString(R.string.ui_settings_map)}")
                assertNull(mapSettings?.findViewWithTag<View>("map_elevation_correction"))
                val naming=root.findViewWithTag<View>("ride_naming_toggle").parent.parent as BicycleFrameDropdown
                naming.header.performClick();assertEquals(View.VISIBLE,naming.content.visibility)
                assertNotNull(naming.content.findViewWithTag<View>("ride_naming_text"))
                val metadata=root.findViewWithTag<View>("fit_metadata_toggle").parent.parent as BicycleFrameDropdown
                val wasOpen=metadata.expanded;metadata.header.performClick();assertEquals(!wasOpen,metadata.expanded);metadata.setExpanded(true)
                assertTrue(metadata.content.paddingBottom>0);assertTrue(metadata.content.paddingTop>metadata.content.paddingBottom)
                assertNull(root.findViewWithTag<View>("axs_warning_enabled"))
                root.measure(View.MeasureSpec.makeMeasureSpec(420,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY))
                root.layout(0,0,420,900)
                val sensorList=root.findViewWithTag<ViewGroup>("bike_sensor_list")
                val sensorFrames=(0 until sensorList.childCount).map(sensorList::getChildAt).filter { it.visibility==View.VISIBLE }
                assertTrue(sensorFrames.all { it is BicycleFrameDropdown })
                sensorFrames.zipWithNext().forEach { (before,next) ->
                    assertEquals("sensor accordions use shared small gap",BicycleDesign.dp(a,BicycleDesign.controlGap),next.top-before.bottom)
                }
                val sources=primary.content.findViewWithTag<RecordingSourcesView>("recording_sources")
                val rows=(0 until sources.childCount).map(sources::getChildAt)
                rows.zipWithNext().forEach { (before,next) ->
                    assertEquals("source dropdowns use shared small gap",BicycleDesign.dp(a,BicycleDesign.controlGap),next.top-before.bottom)
                }
                assertEquals("outer accordions use shared small gap",BicycleDesign.dp(a,BicycleDesign.controlGap),stored.top-primary.bottom)
                dialog.dismiss();shadowOf(android.os.Looper.getMainLooper()).idle()
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun bikeLightUsesStatusStripOrSafeDashboardCornerAndFollowsSensorState() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val runtime=RideRuntime.get(a);val prefs=runtime.preferences
            val phase=runtime.connection.javaClass.getDeclaredField("phase").apply { isAccessible=true }
            phase.set(runtime.connection,phase.type.enumConstants!!.single { it.toString()=="LIVE" })
            fun light(value:Long) {
                runtime.onSamples(listOf(McspSample(LiveMetric.BIKE_LIGHT.address,value,true)))
                runtime.onData(LiveData.sample(LiveMetric.BIKE_LIGHT,value),true);call(a,"render")
            }
            val corner=a.window.decorView.findViewWithTag<ImageView>("dashboard_bike_light")
            val strip=testDescendants(a.window.decorView).filterIsInstance<DashboardStatusStrip>().single()
            prefs.edit().putString("bikeLightPlacement","BOTTOM").putBoolean(DashboardFullscreen.key,false).commit()
            light(1);assertEquals(View.VISIBLE,corner.visibility);assertFalse(strip.bikeLightOn)
            val root=(corner.parent as ViewGroup).getChildAt(0)
            root.dispatchApplyWindowInsets(WindowInsets.Builder().setInsets(WindowInsets.Type.statusBars(),Insets.of(0,30,0,0)).build())
            val p=corner.layoutParams as android.widget.FrameLayout.LayoutParams
            assertEquals(30+BicycleDesign.horizontalPadding,p.topMargin);assertEquals(BicycleDesign.horizontalPadding,p.marginEnd)
            prefs.edit().putBoolean(DashboardFullscreen.key,true).commit();call(a,"render")
            assertEquals(View.GONE,corner.visibility);assertTrue(strip.bikeLightOn)
            light(0);assertFalse(strip.bikeLightOn);assertEquals(View.GONE,corner.visibility)
            prefs.edit().putBoolean(DashboardFullscreen.key,false).commit();light(1)
            runtime.connection.stop();call(a,"render");assertEquals(View.GONE,corner.visibility)
        } finally { host.pause().stop().destroy() }
    }
}
