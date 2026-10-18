package de.bcycle.app

import android.Manifest
import android.location.Location
import android.location.LocationListener
import android.os.Looper
import android.os.SystemClock
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class ProfileMapRefinementsTest {
    @Test fun indoorHidesMapsAndBoschWithoutDeletingPreferencesAndProfileKeepsBottomGap() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a)
            runtime.preferences.edit().putBoolean("mapNight",true).commit()
            fun open()=MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            for(type in listOf(BikeType.EBIKE,BikeType.INDOOR,BikeType.EBIKE)) {
                runtime.bikes.save(runtime.bikes.current().copy(type=type));open();shadowOf(Looper.getMainLooper()).idle()
                val screen=ShadowAlertDialog.getLatestAlertDialog();val root=screen.window!!.decorView
                val body=testDescendants(root).filterIsInstance<BikesView>().single()
                assertEquals(BicycleDesign.dp(a,BicycleDesign.sectionContentGap),body.paddingBottom)
                assertEquals(type==BikeType.EBIKE,root.findViewWithTag<View>("mode_glow_enabled")!=null)
                assertEquals(type!=BikeType.INDOOR,testDescendants(root).filterIsInstance<BicycleSectionHeader>().any { it.contentDescription==a.getString(R.string.ui_settings_map) })
                assertTrue(runtime.preferences.getBoolean("mapNight",false));screen.dismiss();shadowOf(Looper.getMainLooper()).idle()
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun rideColorKeepsItsProfileAcrossSelectionAndUnknownRidesHaveNoInventedColor() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("bike",0)
        val bikes=BikeProfileStore(c,prefs);val theme=ThemeManager(prefs)
        val selected=theme.presets.last();theme.activate(selected.id)
        val snapshot=RideProfileColor.snapshot(c);RideProfileColor.confirm(c,"ride-test",snapshot)
        assertEquals(bikes.current().name,RideProfileColor.name(c,"ride-test"))
        assertEquals(selected.tokens.primary,RideProfileColor.color(c,"ride-test"))
        val second=bikes.add()!!;bikes.select(second.slot);theme.activate(theme.presets.first().id)
        assertEquals(selected.tokens.primary,RideProfileColor.color(c,"ride-test"))
        assertNull(RideProfileColor.color(c,"unassigned-import"))
    }
    @Test fun inaccurateFixCanAppearOnMapWithoutBeingAcceptedForRecordingAndExpires() {
        val c=RuntimeEnvironment.getApplication();shadowOf(c).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val ride=GpsRide();val sensor=GpsSensor(c,ride) {}
        sensor.start();assertTrue(sensor.active)
        val listener=GpsSensor::class.java.getDeclaredField("listener").apply { isAccessible=true }.get(sensor) as LocationListener
        val now=SystemClock.elapsedRealtime()
        listener.onLocationChanged(Location("gps").apply {
            latitude=48.2;longitude=10.3;accuracy=80f;elapsedRealtimeNanos=now*1_000_000
        })
        assertEquals(0,ride.accepted);assertNull(ride.snapshot(now).latitude)
        assertEquals(80.0,sensor.mapFix(now)!!.accuracyM,0.0)
        assertNull(sensor.mapFix(now+5001));sensor.stop();assertNull(sensor.mapFix(now))
    }
}
