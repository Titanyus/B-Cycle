package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class MapZoomTest {
    @Test fun legacyFractionalAndProviderLimitsRemainCompatibleAcrossProfileSwitches() {
        val prefs=RuntimeEnvironment.getApplication().getSharedPreferences("zoom-test",0)
        prefs.edit().clear().putInt("mapZoom",2).commit()
        assertEquals(6.0,MapZoom.read(prefs,MapSource.OSM),0.0)
        prefs.edit().putFloat(MapZoom.preference,18.5f).commit()
        assertEquals(18.5,MapZoom.read(prefs,MapSource.OSM),0.0)
        assertEquals(17.0,MapZoom.read(prefs,MapSource.OPENTOPOMAP),0.0)
        val bank=BikeUiConfigurationStore(prefs)
        bank.ensure(listOf(0,1));bank.save(0)
        prefs.edit().putFloat(MapZoom.preference,12.5f).commit();bank.save(1)
        bank.apply(0);assertEquals(18.5,MapZoom.read(prefs,MapSource.OSM),0.0)
        bank.apply(1);assertEquals(12.5,MapZoom.read(prefs,MapSource.OSM),0.0)
    }
}
