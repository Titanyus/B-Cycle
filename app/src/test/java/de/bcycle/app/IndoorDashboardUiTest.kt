package de.bcycle.app

import android.app.Activity
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class IndoorDashboardUiTest {
    private fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    @Test fun indoorPickerOnlyOffersMapFreeLayoutsWithoutErasingOutdoorPreferences() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val bikes=BikeProfileStore(a,prefs)
            prefs.edit().putString("dashboardTemplate_0","HERO").commit()
            bikes.save(bikes.current().copy(type=BikeType.INDOOR))
            val picker=DashboardPresetPicker(a,0);a.setContentView(picker)
            assertTrue(picker.selection.all { !it.hasMap })
            picker.findViewWithTag<View>("dashboard_layout_choice").performClick()
            val options=nodes(picker.findViewWithTag<View>("dashboard_layout_details")).filter { it.isClickable && it.tag?.toString()?.startsWith("preset_")==true }
            assertEquals(listOf("preset_FULL_DATA"),options.map { it.tag })
            picker.findViewWithTag<View>("dashboard_layout_choice").performClick()
            assertEquals("HERO",prefs.getString("dashboardTemplate_0",null))
            bikes.save(bikes.current().copy(type=BikeType.EBIKE))
            val outdoor=DashboardPresetPicker(a,0);a.setContentView(outdoor)
            assertEquals(DashboardTemplate.HERO,outdoor.selection.first())
            outdoor.findViewWithTag<View>("dashboard_layout_choice").performClick()
            assertEquals(10,nodes(outdoor.findViewWithTag<View>("dashboard_layout_details")).count { it.isClickable && it.tag?.toString()?.startsWith("preset_")==true })
            outdoor.findViewWithTag<View>("dashboard_layout_choice").performClick()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun indoorKeepsToolbarAndRecordingCueStopsWithRecordingAndActivity() {
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("bike",0)
        val bikes=BikeProfileStore(app,prefs);bikes.save(bikes.current().copy(type=BikeType.INDOOR,autoConnect=false))
        prefs.edit().putString("dashboardTemplate_0","HERO").commit()
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        val runtime=RideRuntime.get(host.get())
        val field=RideRuntime::class.java.getDeclaredField("recording").apply { isAccessible=true }
        try {
            val a=host.get();val root=a.window.decorView
            val map=nodes(root).filterIsInstance<MapPanel>().single()
            assertEquals(View.GONE,map.visibility)
            map.controlsVisibilityChanged!!.invoke(false)
            assertEquals(View.VISIBLE,root.findViewWithTag<View>("ride_toolbar").visibility)
            val render=MainActivity::class.java.getDeclaredMethod("render").apply { isAccessible=true }
            field.setBoolean(runtime,true);render.invoke(a)
            val button=root.findViewWithTag<Button>("record_bike_button")
            val cue=(button.text as Spanned).getSpans(0,button.text.length,ImageSpan::class.java).first().drawable as RecordingButtonIndicator
            assertTrue(cue.running)
            button.performClick();assertEquals(RecordingService.STOP,shadowOf(a).nextStartedService.action)
            field.setBoolean(runtime,false);render.invoke(a);assertFalse(cue.running)
            assertFalse((button.text as Spanned).getSpans(0,button.text.length,ImageSpan::class.java).first().drawable is RecordingButtonIndicator)
            field.setBoolean(runtime,true);render.invoke(a)
            val next=(button.text as Spanned).getSpans(0,button.text.length,ImageSpan::class.java).first().drawable as RecordingButtonIndicator
            host.pause().stop();assertFalse(next.running)
            host.start().resume();assertTrue(next.running)
        } finally { field.setBoolean(runtime,false);host.pause().stop().destroy() }
    }
    @Test fun outdoorRecordingUsesTheSameAnimatedStopCue() {
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("bike",0)
        val bikes=BikeProfileStore(app,prefs);bikes.save(bikes.current().copy(type=BikeType.EBIKE,autoConnect=false))
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        val runtime=RideRuntime.get(host.get())
        val field=RideRuntime::class.java.getDeclaredField("recording").apply { isAccessible=true }
        try {
            val a=host.get()
            val render=MainActivity::class.java.getDeclaredMethod("render").apply { isAccessible=true }
            field.setBoolean(runtime,true);render.invoke(a)
            val button=a.window.decorView.findViewWithTag<Button>("record_bike_button")
            val cue=(button.text as Spanned).getSpans(0,button.text.length,ImageSpan::class.java).first().drawable as RecordingButtonIndicator
            assertTrue(cue.running)
            button.performClick();assertEquals(RecordingService.STOP,shadowOf(a).nextStartedService.action)
            field.setBoolean(runtime,false);render.invoke(a);assertFalse(cue.running)
        } finally { field.setBoolean(runtime,false);host.pause().stop().destroy() }
    }
    @Test fun recordingCueHasTwoSecondStatesWithSoftTransitions() {
        assertEquals(0f,RecordingButtonIndicator.dotAt(0),0f)
        assertEquals(0f,RecordingButtonIndicator.dotAt(1_500),0f)
        assertEquals(.5f,RecordingButtonIndicator.dotAt(1_750),.001f)
        assertEquals(1f,RecordingButtonIndicator.dotAt(2_000),0f)
        assertEquals(1f,RecordingButtonIndicator.dotAt(3_500),0f)
        assertEquals(.5f,RecordingButtonIndicator.dotAt(3_750),.001f)
        assertEquals(0f,RecordingButtonIndicator.dotAt(4_000),0f)
    }
}
