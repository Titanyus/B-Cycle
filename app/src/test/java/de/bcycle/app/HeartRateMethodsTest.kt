package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HeartRateMethodsTest {
    @Test fun exactBoundariesAndCurrentZoneTwoFollowChosenModelWhileReserveKeepsItsMeaning() {
        val original=RiderProfile(restingHeartRate=50,maximumHeartRate=190,thresholdHeartRate=170)
        assertEquals(Karvonen.calculate(50,190)!!.zones,original.heartZones()!!.zones)
        val maximum=original.copy(heartRateMethod=HeartRateMethod.MAXIMUM)
        val threshold=original.copy(heartRateMethod=HeartRateMethod.THRESHOLD)
        assertEquals(listOf(95,114,133,152,171),maximum.heartZones()!!.zones.map { it.lowerBpm })
        assertEquals(listOf(1,138,153,160,170),threshold.heartZones()!!.zones.map { it.lowerBpm })
        for(profile in listOf(original,maximum,threshold)) {
            val model=profile.heartZones()!!
            for(bpm in 1..250) assertEquals(model.zoneFor(bpm)?.index,TrainingSnapshot(profile,heart=bpm.toDouble()).heartZone)
            for(boundary in model.boundariesPercent) {
                val bpm=model.offset+model.scale*boundary/100.0
                assertEquals(model.boundariesPercent.indexOf(boundary)+2,model.zoneFor(bpm)!!.index)
                assertEquals(model.boundariesPercent.indexOf(boundary)+1,model.zoneFor(bpm-.000001)!!.index)
            }
        }
        assertEquals(3,TrainingSnapshot(original,heart=150.0).heartZone)
        val selected=TrainingSnapshot(threshold,heart=150.0)
        assertEquals(2,selected.heartZone)
        assertEquals(100.0*100/140,selected.heartPercent!!,.00001)
        assertEquals(ZoneTwoHelpColors.green,selected.glow(ZoneGlowSource.HEART_CURRENT,true))
        val histogram=RouteTimeHistogram(250)
        histogram.observe(200.0);histogram.add(200.0,1000)
        val distribution=histogram.result(AnalysisChart.HEART,threshold)!!
        assertTrue(distribution.maximum>=200)
        assertEquals(5,distribution.bands.size)
        assertTrue(distribution.bands.all { it.lower<it.upper })
        for(value in listOf(59,251)) assertNull(RiderProfile(heartRateMethod=HeartRateMethod.THRESHOLD,thresholdHeartRate=value).heartZones())
        for(edge in listOf(78.0,84.0,88.0,92.0,96.0,97.0,103.0)) {
            val a=ZoneGlowColors.heart(edge-.00001,threshold.heartZones()!!.boundariesPercent)
            val b=ZoneGlowColors.heart(edge+.00001,threshold.heartZones()!!.boundariesPercent)
            for(shift in listOf(0,8,16)) assertTrue(kotlin.math.abs((a ushr shift and 255)-(b ushr shift and 255))<=1)
        }
        assertNull(RiderProfile(heartRateMethod=HeartRateMethod.THRESHOLD).heartZones())
        assertNull(RiderProfile(maximumHeartRate=190,heartRateMethod=HeartRateMethod.THRESHOLD).heartZones())
        assertNotNull(RiderProfile(maximumHeartRate=190,heartRateMethod=HeartRateMethod.MAXIMUM).heartZones())
    }
    @Test fun methodControlsEnableOnlyRequiredFieldsAndPreserveOthersAcrossBackup() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("method-profile",0);val store=RiderProfileStore(prefs)
            assertTrue(store.write(RiderProfile(restingHeartRate=50,maximumHeartRate=190,ftpW=250,thresholdHeartRate=170)))
            val themes=ThemeManager(prefs)
            val screen=showRiderProfile(a,themes.settings,DialogNavigation(),store)
            val root=screen.window!!.decorView
            val choice=root.findViewWithTag<Spinner>("rider_heart_method")
            fun input(tag:String)=root.findViewWithTag<EditText>("rider_$tag")
            fun select(method:HeartRateMethod) { choice.setSelection(method.ordinal);shadowOf(Looper.getMainLooper()).idle() }
            select(HeartRateMethod.MAXIMUM)
            assertFalse(input("resting_heart_rate").isEnabled);assertTrue(input("maximum_heart_rate").isEnabled);assertFalse(input("threshold_heart_rate").isEnabled)
            input("maximum_heart_rate").setText("200")
            assertEquals(50,store.read().restingHeartRate);assertEquals(200,store.read().maximumHeartRate)
            select(HeartRateMethod.THRESHOLD)
            assertFalse(input("maximum_heart_rate").isEnabled);assertTrue(input("threshold_heart_rate").isEnabled);assertTrue(input("ftp").isEnabled)
            input("threshold_heart_rate").setText("175")
            input("threshold_heart_rate").setText("999")
            assertEquals(175,store.read().thresholdHeartRate)
            input("threshold_heart_rate").setText("175")
            val expected=store.read();assertEquals(HeartRateMethod.THRESHOLD,expected.heartRateMethod)
            assertEquals(175,expected.thresholdHeartRate);assertEquals(200,expected.maximumHeartRate)
            assertTrue(root.findViewWithTag<TextView>("rider_zone_value_5").text.toString().contains("175"))
            val backup=SettingsBackup(prefs,a.getSharedPreferences("method-export",0),themes)
            val bytes=backup.export(setOf(BackupSection.PROFILE))
            screen.dismiss();assertTrue(store.write(expected.copy(heartRateMethod=HeartRateMethod.KARVONEN)))
            backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.PROFILE)))
            assertEquals(expected,store.read())
            prefs.edit().putString(RiderProfileStore.key,"""{"version":1,"restingHeartRate":50,"maximumHeartRate":190}""").commit()
            assertEquals(HeartRateMethod.KARVONEN,store.read().heartRateMethod)
        } finally { host.pause().stop().destroy() }
    }
}
