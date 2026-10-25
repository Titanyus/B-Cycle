package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SensorSetupTest {
    @Test fun missingMeansRequiredAssignmentAndDailyDeferralIsProfileScopedAndPersistent() {
        val a=RuntimeEnvironment.getApplication();val prefs=a.getSharedPreferences("bike",0)
        val bikes=BikeProfileStore(a,prefs);val launch=SensorSetupLaunch(a)
        val ebike=bikes.current();val today=LocalDate.of(2026,10,25)
        assertTrue(launch.shouldShow(ebike,today))
        assertTrue(launch.shouldShow(ebike.copy(heartAddress="AA:BB:CC:DD:EE:01"),today))
        assertFalse(launch.shouldShow(ebike.copy(bikeAddress="AA:BB:CC:DD:EE:02",bikeEnabled=false),today))
        val indoor=bikes.add()!!.copy(type=BikeType.INDOOR)
        assertTrue(launch.shouldShow(indoor,today))
        assertFalse(launch.shouldShow(indoor.copy(trainerAddress="AA:BB:CC:DD:EE:03",trainerEnabled=false),today))
        launch.later(ebike,today)
        assertFalse(SensorSetupLaunch(a).shouldShow(ebike.copy(name="Neuer Name"),today))
        assertTrue(launch.shouldShow(indoor,today))
        assertTrue(launch.shouldShow(ebike,today.plusDays(1)))
        assertFalse(SensorSetupLaunch.missing(ebike.copy(type=BikeType.BIKE)))
    }
    @Test fun tertiaryCardFitsSmallPortraitAndKeepsSearchAndLaterDistinctInBothModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val root=FrameLayout(a);val dashboard=View(a)
            root.addView(dashboard,FrameLayout.LayoutParams(-1,-1));a.setContentView(root)
            var later=0;var searches=0
            fun layout() {
                root.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY))
                root.layout(0,0,320,480)
            }
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                val prompt=SensorSetupPrompt(a,root,dashboard,themes,{later++},{searches++})
                layout();val card=root.findViewWithTag<View>("sensor_setup_card")
                assertEquals(themes.active.tokens.tertiary,(card.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
                assertTrue(card.elevation>0f)
                assertEquals(android.graphics.Color.BLACK,card.outlineAmbientShadowColor)
                assertEquals(android.graphics.Color.BLACK,card.outlineSpotShadowColor)
                assertTrue(card.left>=16 && card.right<=304)
                assertTrue(card.top>=16 && card.bottom<=464)
                assertNull(root.findViewWithTag<View>("bicycle_navigation_header"))
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,dashboard.importantForAccessibility)
                val image=Bitmap.createBitmap(320,480,Bitmap.Config.ARGB_8888)
                Canvas(image).apply { drawColor(themes.active.tokens.secondary);root.draw(this) }
                java.io.File("build/reports/ui/sensor-setup-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }
                    .outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
                root.findViewWithTag<View>("sensor_setup_search").performClick()
                assertEquals(0,later);assertFalse(prompt.showing)
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,dashboard.importantForAccessibility)
            }
            assertEquals(2,searches)
            val prompt=SensorSetupPrompt(a,root,dashboard,themes,{later++},{searches++})
            root.findViewWithTag<View>("sensor_setup_later").performClick()
            assertEquals(1,later);assertEquals(2,searches);prompt.close();assertEquals(1,later)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun launchShowsCardSearchOpensCurrentProfileAndConfigurationOrRecordingSuppressesIt() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        val a=host.get();val runtime=RideRuntime.get(a)
        val recording=RideRuntime::class.java.getDeclaredField("recording").apply { isAccessible=true }
        val render=MainActivity::class.java.getDeclaredMethod("render").apply { isAccessible=true }
        fun refresh() { render.invoke(a);shadowOf(Looper.getMainLooper()).idle() }
        try {
            shadowOf(Looper.getMainLooper()).idle();refresh()
            val root=a.window.decorView
            assertNotNull(root.findViewWithTag<View>("sensor_setup_prompt"))
            root.findViewWithTag<View>("sensor_setup_search").performClick();shadowOf(Looper.getMainLooper()).idle()
            val manager=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            assertTrue(manager.isShowing);assertNotNull(manager.window!!.decorView.findViewWithTag<View>("bike_sensor_search"))
            assertEquals(runtime.bikes.current().name,manager.window!!.decorView.findViewWithTag<android.widget.TextView>("bike_profile_name").text.toString())
            manager.dismiss();shadowOf(Looper.getMainLooper()).idle();refresh()
            assertNotNull(root.findViewWithTag<View>("sensor_setup_prompt"))
            root.findViewWithTag<View>("sensor_setup_later").performClick();refresh()
            assertNull(root.findViewWithTag<View>("sensor_setup_prompt"))
            host.pause().stop();host.start().resume();refresh()
            assertNull(root.findViewWithTag<View>("sensor_setup_prompt"))
            val second=runtime.bikes.add()!!;assertTrue(runtime.switchBike(second.slot));refresh();refresh()
            assertNotNull(root.findViewWithTag<View>("sensor_setup_prompt"))
            recording.setBoolean(runtime,true);refresh();assertNull(root.findViewWithTag<View>("sensor_setup_prompt"))
            recording.setBoolean(runtime,false)
            runtime.bikes.save(runtime.bikes.current().copy(bikeAddress="AA:BB:CC:DD:EE:02"));refresh()
            assertNull(root.findViewWithTag<View>("sensor_setup_prompt"))
            runtime.bikes.save(runtime.bikes.current().copy(bikeAddress=null));refresh()
            assertNotNull(root.findViewWithTag<View>("sensor_setup_prompt"))
        } finally { recording.setBoolean(runtime,false);host.pause().stop().destroy() }
    }
}
