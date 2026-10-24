package de.bcycle.app

import android.os.SystemClock
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class DrinkReminderDashboardTest {
    @Test fun allEnabledBikeTypesOfferReminderAndIndoorCueStopsWithRecording() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        val a=host.get();val runtime=RideRuntime.get(a)
        val recording=RideRuntime::class.java.getDeclaredField("recording").apply { isAccessible=true }
        try {
            val controls=MainActivity::class.java.getDeclaredMethod("dashboardControls",Boolean::class.javaPrimitiveType).apply { isAccessible=true }
            for(type in BikeType.entries.filter { it.available }) {
                runtime.bikes.save(runtime.bikes.current().copy(type=type))
                val page=controls.invoke(a,false) as View
                val reminder=page.findViewWithTag<View>("drink_reminder_enabled")
                assertNotNull(reminder)
                assertTrue(testDescendants(page.findViewWithTag<TrainingHelpView>("training_help_controls")).contains(reminder))
                if(type==BikeType.EBIKE) assertNotNull(page.findViewWithTag<View>("mode_glow_enabled"))
            }
            val render=MainActivity::class.java.getDeclaredMethod("render").apply { isAccessible=true }
            val panel=a.window.decorView.findViewWithTag<DrinkReminderPanel>("drink_reminder_panel")
            val config=DrinkReminderConfig(true,10)
            DrinkReminderSettings.save(runtime.preferences,config)
            render.invoke(a);assertEquals(View.GONE,panel.visibility)
            recording.setBoolean(runtime,true)
            RideRuntime::class.java.getDeclaredField("startElapsed").apply { isAccessible=true }.setLong(runtime,SystemClock.elapsedRealtime()-600000)
            runtime.drinkReminder.reset();runtime.drinkReminder.update(true,0,config,false)
            render.invoke(a)
            val decor=a.window.decorView
            repeat(2) {
                decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                decor.layout(0,0,360,800)
            }
            assertEquals(View.VISIBLE,panel.visibility)
            assertTrue(panel.height>0 && panel.top>=0 && panel.bottom<=800)
            for(position in MessagePosition.entries) {
                DrinkReminderSettings.save(runtime.preferences,config.copy(position=position))
                MainActivity::class.java.getDeclaredMethod("placeDrinkReminder").apply { isAccessible=true }.invoke(a)
                val params=panel.layoutParams as android.widget.FrameLayout.LayoutParams
                val expected=when(position) { MessagePosition.BOTTOM->android.view.Gravity.BOTTOM;MessagePosition.CENTER->android.view.Gravity.CENTER_VERTICAL;MessagePosition.TOP->android.view.Gravity.TOP }
                assertEquals(expected,params.gravity and android.view.Gravity.VERTICAL_GRAVITY_MASK)
            }
            recording.setBoolean(runtime,false);render.invoke(a);assertEquals(View.GONE,panel.visibility)
        } finally { recording.setBoolean(runtime,false);host.pause().stop().destroy() }
    }
}
