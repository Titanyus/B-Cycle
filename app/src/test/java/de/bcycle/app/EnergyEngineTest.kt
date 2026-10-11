package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class EnergyEngineTest {
    @Test fun metabolicEnergyIsWorkDividedByEfficiencyAndJoulesPerCalorieNotMechanicalWorkItself() {
        val value = EnergyEngine().estimate(EnergyInput(720.0, 150.0, 3600000, 1.0, 1.0))!!
        assertEquals(717.0172084130019, value.kcal, .00001)
        assertEquals(.24, value.grossEfficiency!!, 0.0)
        assertEquals(EnergyEngine.MODEL_ID, value.modelId)
        assertNull(EnergyEngine().estimate(EnergyInput(null, 150.0, 3600000, 0.0, 1.0)))
    }
    @Test fun physiologyParametersAreOptionalAndNeverInventedForFallbackOrMixedWithPower() {
        val profile = EnergyProfile()
        assertNull(profile.weightKg); assertNull(profile.ageYears); assertNull(profile.ftpW)
        val estimate = EnergyEngine(profile).estimate(EnergyInput(100.0, 180.0, 600000, 1.0, 1.0))!!
        val withoutPulse = EnergyEngine(profile).estimate(EnergyInput(100.0, null, 600000, 1.0, 0.0))!!
        assertEquals(estimate, withoutPulse)
        assertNull(EnergyEngine(profile).estimate(EnergyInput(null, 180.0, 600000, 0.0, 1.0)))
    }
    @Test fun prioritizedModelsNeverAddTheirSeparateCalorieEstimates() {
        var fallbackCalled = false
        val fallback = object : EnergyModel {
            override val id = "future-test-model"
            override fun estimate(input: EnergyInput, profile: EnergyProfile): EnergyEstimate {
                fallbackCalled = true; return EnergyEstimate(500.0, id, null)
            }
        }
        val engine = EnergyEngine(models = listOf(PowerEnergyModel(), fallback))
        assertEquals(99.5857233906947, engine.estimate(EnergyInput(100.0, null, 1000, 1.0, 0.0))!!.kcal, .00001)
        assertFalse(fallbackCalled)
        assertEquals(500.0, engine.estimate(EnergyInput(null, null, 1000, 0.0, 0.0))!!.kcal, 0.0)
        assertTrue(fallbackCalled)
        assertThrows(IllegalArgumentException::class.java) { PowerEnergyModel(Double.NaN) }
    }
}
