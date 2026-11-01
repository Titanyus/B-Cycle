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
            assertEquals(listOf("preset_FULL_FOUR","preset_FULL_DATA","preset_FULL_SIX","preset_FULL_NINE","preset_FULL_EIGHT"),options.map { it.tag })
            picker.findViewWithTag<View>("dashboard_layout_choice").performClick()
            assertEquals("HERO",prefs.getString("dashboardTemplate_0",null))
            bikes.save(bikes.current().copy(type=BikeType.EBIKE))
            val outdoor=DashboardPresetPicker(a,0);a.setContentView(outdoor)
            assertEquals(DashboardTemplate.HERO,outdoor.selection.first())
            outdoor.findViewWithTag<View>("dashboard_layout_choice").performClick()
            assertEquals(14,nodes(outdoor.findViewWithTag<View>("dashboard_layout_details")).count { it.isClickable && it.tag?.toString()?.startsWith("preset_")==true })
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
            val cue=(button as DashboardRecordButton).recordIcon as RecordingButtonIndicator
            assertTrue(cue.running)
            button.performClick();assertEquals(RecordingService.STOP,shadowOf(a).nextStartedService.action)
            field.setBoolean(runtime,false);render.invoke(a);assertFalse(cue.running)
            assertFalse((button as DashboardRecordButton).recordIcon is RecordingButtonIndicator)
            field.setBoolean(runtime,true);render.invoke(a)
            val next=(button as DashboardRecordButton).recordIcon as RecordingButtonIndicator
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
            val cue=(button as DashboardRecordButton).recordIcon as RecordingButtonIndicator
            assertTrue(cue.running)
            button.performClick();assertEquals(RecordingService.STOP,shadowOf(a).nextStartedService.action)
            field.setBoolean(runtime,false);render.invoke(a);assertFalse(cue.running)
        } finally { field.setBoolean(runtime,false);host.pause().stop().destroy() }
    }
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun recordingCueSleepsDuringStableStatesAndCancelsPendingTransitions() {
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16))
        android.animation.ValueAnimator::class.java.getDeclaredMethod("setDurationScale",Float::class.javaPrimitiveType).invoke(null,1f)
        org.robolectric.shadows.ShadowChoreographer.setPaused(true)
        val app=RuntimeEnvironment.getApplication();var redraws=0
        val cue=RecordingButtonIndicator(app,android.graphics.Color.WHITE) { redraws++ }
        val main=shadowOf(android.os.Looper.getMainLooper())
        fun advance(ms:Long)=main.idleFor(java.time.Duration.ofMillis(ms))
        fun animator()=RecordingButtonIndicator::class.java.getDeclaredField("animator").apply { isAccessible=true }.get(cue) as android.animation.ValueAnimator
        try {
            cue.setAnimating(true);assertTrue(cue.running);assertFalse(cue.transitioning)
            val initial=redraws;advance(1400);assertEquals(initial,redraws)
            cue.setAnimating(true);advance(100);assertTrue(cue.transitioning)
            animator().currentPlayTime=250
            assertEquals(.5f,cue.dotFraction,.001f)
            advance(500);animator().end();main.idle()
            assertFalse(cue.transitioning);assertEquals(1f,cue.dotFraction,0f)
            val stable=redraws;advance(1400);assertEquals(stable,redraws)
            advance(100);assertTrue(cue.transitioning);animator().currentPlayTime=250
            assertEquals(.5f,cue.dotFraction,.001f)
            advance(500);animator().end();main.idle()
            assertFalse(cue.transitioning);assertEquals(0f,cue.dotFraction,0f)
            val next=redraws;advance(1200);assertEquals(next,redraws)
            cue.stop();assertFalse(cue.running);advance(2000);assertEquals(next,redraws)
            cue.setAnimating(true);advance(1500);assertTrue(cue.transitioning);animator().currentPlayTime=250
            cue.stop();val stopped=redraws;advance(5000)
            assertFalse(cue.running);assertFalse(cue.transitioning);assertEquals(0f,cue.dotFraction,0f);assertEquals(stopped,redraws)
        } finally { cue.stop();org.robolectric.shadows.ShadowChoreographer.setPaused(false) }
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
