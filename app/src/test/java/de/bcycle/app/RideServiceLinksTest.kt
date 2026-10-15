package de.bcycle.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideServiceLinksTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val id="recorded-"+"a".repeat(64)
    private val hash="b".repeat(64)
    @Test fun idsSurviveFileVersionsAndRemainScopedToAccountAndService() {
        val first=RideServiceLink(id,RideService.INTERVALS,"i1","a1",hash)
        RideServiceLinks.confirm(c,first)
        RideServiceLinks.confirm(c,first)
        RideServiceLinks.confirm(c,first.copy(fileHash="c".repeat(64),activityId="a2"))
        RideServiceLinks.confirm(c,first.copy(service=RideService.STRAVA,accountId="123"))
        RideServiceLinks.confirm(c,first.copy(service=RideService.DREEVE,accountId="rider"))
        RideServiceLinks.confirm(c,first.copy(accountId="i2"))
        assertEquals(5,RideServiceLinks.links(c,id).size)
        // Read through persisted JSON, without depending on any upload-job in-memory state.
        assertEquals(first,RideServiceLinks.links(c,id).first())
        assertFalse(c.getSharedPreferences("ride-service-links",0).getString("entries","")!!.contains("password"))
    }
    @Test fun remoteIdentityCannotSilentlyPointToTwoLocalRides() {
        val first=RideServiceLink(id,RideService.STRAVA,"123","456",hash)
        RideServiceLinks.confirm(c,first)
        assertThrows(IllegalArgumentException::class.java) {
            RideServiceLinks.confirm(c,first.copy(rideId="recorded-"+"d".repeat(64)))
        }
        assertEquals(listOf(first),RideServiceLinks.links(c,id))
        assertThrows(IllegalArgumentException::class.java) { RideServiceLinks.confirm(c,first.copy(activityId="")) }
        RideServiceLinks.forget(c,id)
        assertTrue(RideServiceLinks.links(c,id).isEmpty())
    }
    @Test fun confirmedLegacyIntervalsReceiptMigratesBeforeQueueReplacement() {
        val old=JSONObject().put("state","saved").put("remoteId","a42").put("athlete","i9").put("hash",hash)
        val receipts=c.getSharedPreferences("intervals-uploads",0)
        receipts.edit().putString("entries",JSONObject().put(id,old).toString()).commit()
        val link=RideServiceLinks.links(c,id).single()
        receipts.edit().putString("entries",JSONObject().put(id,old.put("state","pending").put("remoteId","")).toString()).commit()
        assertEquals(link,RideServiceLinks.links(c,id).single())
        assertEquals("a42",link.activityId)
    }
}
