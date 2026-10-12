package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RiderProfileTest {
    private val today = LocalDate.of(2026,10,12)
    private fun store() = RiderProfileStore(RuntimeEnvironment.getApplication().getSharedPreferences("bike",0)) { today }
    @Test fun completePartialAndEmptyProfilesRoundTripInExistingPreferencesWithoutRedundantValues() {
        val prefs=RuntimeEnvironment.getApplication().getSharedPreferences("bike",0)
        prefs.edit().putString("unrelated","preserved").commit()
        val full=RiderProfile(LocalDate.of(1972,10,13),182,76.4,55,185,245)
        assertTrue(store().write(full)); assertEquals(full,store().read())
        assertEquals("preserved",prefs.getString("unrelated",null))
        assertEquals(53,full.ageYears(today)); assertEquals(54,full.ageYears(today.plusDays(1)))
        assertEquals(245.0,full.energyProfile(today).ftpW!!,0.0)
        assertEquals(53,full.energyProfile(today).ageYears)
        val json=org.json.JSONObject(prefs.getString(RiderProfileStore.key,null)!!)
        assertFalse(json.has("age")); assertFalse(json.has("zones"))
        val partial=RiderProfile(ftpW=270)
        assertTrue(store().write(partial)); assertEquals(partial,store().read()); assertNull(partial.heartZones()); assertNull(partial.ageYears(today))
        assertTrue(store().write(RiderProfile())); assertEquals(RiderProfile(),store().read())
    }
    @Test fun strictLocaleWeightAndOptionalIntegerParsing() {
        val de=RiderProfileValidation.parse(null,mapOf(RiderField.WEIGHT to "76,4",RiderField.FTP to "245"),Locale.GERMANY,today)
        assertTrue(de.valid); assertEquals(76.4,de.profile.weightKg!!,0.0);assertEquals(245,de.profile.ftpW)
        assertTrue(RiderProfileValidation.parse(null,mapOf(RiderField.WEIGHT to "76.4"),Locale.US,today).valid)
        for(s in listOf("76,","76.4","1.234,5","76,4oops","NaN","Infinity","-75","0","401"))
            assertFalse(s,RiderProfileValidation.parse(null,mapOf(RiderField.WEIGHT to s),Locale.GERMANY,today).valid)
        for(s in listOf("55x","55.5","0","-55","999999999999999"))
            assertFalse(RiderProfileValidation.parse(null,mapOf(RiderField.RESTING_HEART_RATE to s),Locale.US,today).valid)
        assertTrue(RiderProfileValidation.parse(null,emptyMap(),Locale.US,today).valid)
    }
    @Test fun invalidFieldsAndFutureBirthDoNotReplaceSavedProfile() {
        val valid=RiderProfile(restingHeartRate=55,maximumHeartRate=185)
        assertTrue(store().write(valid))
        listOf(valid.copy(birthDate=today.plusDays(1)), valid.copy(heightCm=0),valid.copy(weightKg=Double.NaN),
            valid.copy(maximumHeartRate=55),valid.copy(ftpW=0),valid.copy(birthDate=today.minusYears(151))).forEach {
            assertFalse(store().write(it));assertEquals(valid,store().read())
        }
        assertNull(RiderProfile(birthDate=today.plusDays(1)).ageYears(today))
        assertEquals(22,RiderProfile(birthDate=LocalDate.of(2004,2,29)).ageYears(LocalDate.of(2026,3,1)))
    }
    @Test fun malformedUnknownVersionAndInvalidStoredFieldsNeverBecomeFakeDefaults() {
        val prefs=RuntimeEnvironment.getApplication().getSharedPreferences("bike",0)
        for(s in listOf("broken","{\"version\":2,\"ftpW\":250}")) {
            prefs.edit().putString(RiderProfileStore.key,s).commit();assertEquals(RiderProfile(),store().read())
        }
        prefs.edit().putString(RiderProfileStore.key,"""{"version":1,"weightKg":0,"heightCm":182,"restingHeartRate":55,"maximumHeartRate":45,"ftpW":220.5}""").commit()
        assertEquals(RiderProfile(heightCm=182,restingHeartRate=55),store().read())
    }
    @Test fun riderFtpRemainsSeparateFromExistingRideEstimatedFtp() {
        assertTrue(store().write(RiderProfile(ftpW=260)))
        val ride=PerformanceEngine(EnergyEngine(store().read().energyProfile(today)))
        for (second in 0..1200) ride.accept(PerformanceEvent(PerformanceChannel.POWER,second*1000L,200.0,source=PowerSource.BOSCH_RIDER.name))
        assertEquals(190.0,ride.snapshot().estimatedFtpW!!,0.000001)
        assertEquals(260,store().read().ftpW)
        assertTrue(store().write(RiderProfile(ftpW=300)))
        assertEquals(190.0,ride.snapshot().estimatedFtpW!!,0.000001)
        assertNull(RiderProfile().ftpW)
        assertFalse(RuntimeEnvironment.getApplication().getSharedPreferences("bike",0).all.keys.any { it.contains("estimated",true) })
    }
}
