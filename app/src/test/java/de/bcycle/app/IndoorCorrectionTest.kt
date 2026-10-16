package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONArray

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class IndoorCorrectionTest {
    @Test fun independentFactorsUseExactTenthsAndPreserveZerosMissingAndOtherFields() {
        for (step in 0..30) {
            val c=IndoorCorrection(step,step)
            assertEquals(20.0*step,c.apply(TrainerField.POWER,200.0)!!,.001)
            assertEquals(step.toDouble(),c.apply(TrainerField.SPEED,10.0)!!,.001)
            assertEquals(12.0*step,c.apply(TrainerField.DISTANCE,120.0)!!,.001)
            assertEquals(0.0,c.apply(TrainerField.POWER,0.0)!!,0.0)
            assertNull(c.apply(TrainerField.POWER,null));assertNull(c.apply(TrainerField.SPEED,null))
        }
        val c=IndoorCorrection(15,20)
        assertEquals(300.0,c.apply(TrainerField.POWER,200.0)!!,0.0)
        assertEquals(20.0,c.apply(TrainerField.SPEED,10.0)!!,0.0)
        for(field in listOf(TrainerField.CADENCE,TrainerField.HEART,TrainerField.ELAPSED)) assertEquals(90.0,c.apply(field,90.0)!!,0.0)
        assertThrows(IllegalArgumentException::class.java) { IndoorCorrection(31,10) }
        assertThrows(IllegalArgumentException::class.java) { IndoorCorrection(10,-1) }
    }
    @Test fun oldProfilesDefaultToOneAndDifferentProfilesRetainIndependentCorrections() {
        val c=RuntimeEnvironment.getApplication()
        val prefs=c.getSharedPreferences("indoor-correction-test",0)
        val store=BikeProfileStore(c,prefs)
        store.save(store.current().copy(type=BikeType.INDOOR,name="Matrix",indoorCorrection=IndoorCorrection(18,12)))
        val second=store.add()!!;store.select(second.slot)
        assertEquals(IndoorCorrection(),store.current().indoorCorrection)
        store.save(store.current().copy(type=BikeType.INDOOR,indoorCorrection=IndoorCorrection(9,0)))
        val restarted=BikeProfileStore(c,prefs)
        assertEquals(IndoorCorrection(9,0),restarted.current().indoorCorrection)
        restarted.select(0);assertEquals(IndoorCorrection(18,12),restarted.current().indoorCorrection)
        val rows=JSONArray(prefs.getString(BikeProfileStore.KEY,null))
        rows.getJSONObject(0).remove("indoorPowerTenths");rows.getJSONObject(0).remove("indoorSpeedTenths")
        prefs.edit().putString(BikeProfileStore.KEY,rows.toString()).apply()
        assertEquals(IndoorCorrection(),BikeProfileStore(c,prefs).current().indoorCorrection)
        assertEquals("Matrix",BikeProfileStore(c,prefs).current().name)
        rows.getJSONObject(0).put("indoorPowerTenths",999).put("indoorSpeedTenths",-1)
        prefs.edit().putString(BikeProfileStore.KEY,rows.toString()).apply()
        assertEquals(IndoorCorrection(),BikeProfileStore(c,prefs).current().indoorCorrection)
    }
}
