package de.bcycle.app

import android.os.Looper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideMetadataSnapshotTest {
    @Test fun largeLibrariesReuseSnapshotsAndDirectLookupWhileMetadataChangesInvalidate() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("bike",0)
        val profiles=BikeProfileStore(c,prefs);val first=profiles.current()
        val state=MapState(c) {}
        val worker=MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as ExecutorService
        try {
            worker.submit {}.get(10,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle()
            val routesField=MapState::class.java.getDeclaredField("routes").apply { isAccessible=true }
            val revisionField=MapState::class.java.getDeclaredField("routeRevision").apply { isAccessible=true }
            for(count in listOf(50,500,2000)) {
                val routes=List(count) { StoredGpx("ride-$it","Original $it",2,1,RouteDetails(dateMs=it.toLong())) }
                routesField.set(state,routes);revisionField.setLong(state,state.routeRevision+1)
                val edit=c.getSharedPreferences("ride-profile-colors",0).edit()
                val snapshot=RideProfileColor.snapshot(c)
                routes.forEach { edit.putString(it.id,snapshot) };edit.commit()
                shadowOf(Looper.getMainLooper()).idle()
                val started=System.nanoTime();val entries=state.entries
                repeat(100) { assertSame(entries,state.entries);assertSame(entries.last(),state.entry("ride-0")) }
                println("Ride metadata $count + 100 lookups: ${(System.nanoTime()-started)/1_000_000} ms")
                assertEquals(count,entries.size);assertEquals("ride-${count-1}",entries.first().id)
            }
            val before=state.entries
            profiles.save(first.copy(type=BikeType.INDOOR,name="Trainer"))
            assertNotSame(before,state.entries);assertEquals(BikeType.INDOOR,state.entry("ride-0")!!.details.bikeType)
            assertEquals("Trainer",RideProfileColor.name(c,"ride-0"))
            val catalog=RideProfileColor.catalog(c)
            val themes=ThemeManager(prefs);themes.activate("preset:black-ops")
            assertNotSame(catalog,RideProfileColor.catalog(c));assertEquals(themes.baseTokens.primary,RideProfileColor.color(c,"ride-0"))
            RideNames.imported(c,"ride-0","New title")
            assertEquals("New title",state.entry("ride-0")!!.name)
            // Direct restore does not have to manufacture a revision for the name cache.
            c.getSharedPreferences("ride-names",0).edit().putString("data",JSONObject().put("names",JSONObject()
                .put("ride-0",JSONObject().put("name","Restored title"))).toString()).commit()
            assertEquals("Restored title",state.entry("ride-0")!!.name)
            RideProfileColor.forget(c,"ride-0")
            assertNull(RideProfileColor.type(c,"ride-0"));assertNotSame(before,state.entries)
            val second=profiles.add()!!;RideProfileColor.assign(c,"ride-0",second.slot)
            val bank=JSONObject(prefs.getString(BikeUiConfigurationStore.KEY,"{}")!!)
            val stored=bank.optJSONObject(second.slot.toString()) ?: JSONObject().also { bank.put(second.slot.toString(),it) }
            val otherTheme=BicycleThemes.presets.last()
            stored.put(ThemeManager.activeKey,JSONObject().put("value",otherTheme.id))
            prefs.edit().putString(BikeUiConfigurationStore.KEY,bank.toString()).commit()
            assertEquals(second.slot,RideProfileColor.slot(c,"ride-0"))
            assertEquals(otherTheme.tokens.primary,RideProfileColor.color(c,"ride-0"))
        } finally { worker.shutdownNow() }
    }
}
