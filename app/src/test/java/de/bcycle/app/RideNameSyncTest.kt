package de.bcycle.app

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideNameSyncTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val ride="recorded-"+"a".repeat(64)
    private val account="https://dreeve.example/"
    private fun link(service:RideService=RideService.DREEVE,activity:String="activity-1")=RideServiceLink(ride,service,if(service==RideService.DREEVE) account else "i1",activity,"b".repeat(64))
    private fun setup() {
        c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address",account).commit()
        c.getSharedPreferences("intervals-settings",0).edit().putString("athlete","i1").commit()
        RideServiceLinks.confirm(c,link())
    }
    @Test fun successfulDreeveImportNameIsFetchedAfterThreeSecondsAndPropagatedWithoutUpload() {
        setup();val intervals=link(RideService.INTERVALS,"987");RideServiceLinks.confirm(c,intervals)
        RideNames.confirmed(c,link())
        val writes=mutableListOf<Pair<RideService,String>>();val waits=mutableListOf<Long>();var reads=0
        assertFalse(RideNames.run(c,{false},read={ reads++;"Abendrunde · 42 km" },write={ service,name-> writes.add(service.service to name) },wait={ delay,_->waits.add(delay);true }))
        assertEquals(1,reads);assertEquals(1,waits.size);assertTrue(waits.single() in 1..3000)
        assertEquals("Abendrunde · 42 km",RideNames.name(c,ride));assertEquals(listOf(RideService.INTERVALS to "Abendrunde · 42 km"),writes)
        assertFalse(RideNames.run(c,{false},read={ fail("Already fetched");"" },write={ _,_->fail("Already applied") }))
    }
    @Test fun manualNamesWinOverInflightAutomaticResponseAndCoalescePerRemoteActivity() {
        setup();RideNames.confirmed(c,link())
        val writes=mutableListOf<String>()
        RideNames.run(c,{false},read={ RideNames.manual(c,ride,"Mein Titel");"Automatischer Titel" },write={ _,name->writes.add(name) },wait={ _,_->true })
        assertEquals("Mein Titel",RideNames.name(c,ride));assertEquals(listOf("Mein Titel"),writes)
        RideNames.manual(c,ride,"Alt");RideNames.manual(c,ride,"Neu")
        writes.clear();assertFalse(RideNames.run(c,{false},write={ _,name->writes.add(name) }))
        assertEquals(listOf("Neu"),writes)
    }
    @Test fun failedDestinationDoesNotBlockOthersAndAccountSwitchPausesTheQueue() {
        setup();RideServiceLinks.confirm(c,link(RideService.INTERVALS,"42"));RideNames.manual(c,ride,"Neuer Titel")
        val visited=mutableListOf<RideService>()
        assertTrue(RideNames.run(c,{false},write={ target,_->visited.add(target.service);if(target.service==RideService.DREEVE) throw java.io.IOException() }))
        assertEquals(RideService.entries.filter { it!=RideService.STRAVA }.toSet(),visited.toSet())
        c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address","https://other.example/").commit()
        assertFalse(RideNames.run(c,{false},write={ _,_->fail("Wrong account") }))
        c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address",account).commit()
        assertFalse(RideNames.run(c,{false},write={ target,name->assertEquals(RideService.DREEVE,target.service);assertEquals("Neuer Titel",name) }))
    }
    @Test fun explicitDreevePullUsesOnlyExistingLinkedRidesAndCancelsStaleWritesWithoutPropagating() {
        setup();RideNames.manual(c,ride,"Eigener Name")
        RideServiceLinks.confirm(c,link().copy(activityId="newest"))
        val imported="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        val deleted="bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee"
        RideServiceLinks.confirm(c,link().copy(rideId=imported,activityId="imported"))
        RideServiceLinks.confirm(c,link().copy(rideId=deleted,activityId="deleted"))
        RideServiceLinks.confirm(c,link().copy(accountId="https://other.example/",activityId="wrong-account"))
        val calls=mutableListOf<String>();val progress=mutableListOf<Pair<Int,Int>>()
        val result=RideNames.pullDreeve(c,account,mapOf(ride to "Eigener Name",imported to "Unverändert",deleted to "Alt"),
            { it!=deleted },{false},{ target -> calls+=target.activityId;if(target.rideId==ride) "Dreeve-Name" else "Unverändert" },{ done,total -> progress+=done to total })
        assertEquals(DreeveNameSyncResult(1,1,0),result)
        assertEquals(setOf("newest","imported"),calls.toSet())
        assertEquals("Dreeve-Name",RideNames.name(c,ride));assertEquals("Unverändert",RideNames.name(c,imported))
        assertEquals(2 to 2,progress.last())
        RideNames.run(c,{false},read={ fail("No automatic reads");"" },write={ _,_->fail("No stale writes or new propagation") })
    }
    @Test fun explicitPullKeepsConcurrentLocalEditsAndCountsIndividualFailures() {
        setup();RideNames.manual(c,ride,"Vorher")
        val second="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        RideServiceLinks.confirm(c,link().copy(rideId=second,activityId="failed"))
        val result=RideNames.pullDreeve(c,account,mapOf(ride to "Vorher",second to "Alt"),{true},{false},{ target ->
            if(target.rideId==second) throw java.io.IOException()
            RideNames.manual(c,ride,"Zwischenzeitlich geändert");"Dreeve"
        })
        assertEquals(DreeveNameSyncResult(0,0,2),result)
        assertEquals("Zwischenzeitlich geändert",RideNames.name(c,ride))
        assertEquals("pending",RideNames.pending(c,ride,RideService.DREEVE))
    }
    @Test fun accountSwitchOrCancellationDuringFetchDoesNotApplyTheOldResponse() {
        setup();RideNames.manual(c,ride,"Behalten")
        val result=RideNames.pullDreeve(c,account,mapOf(ride to "Behalten"),{true},{false},{
            c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address","https://other.example/").commit();"Alter Server"
        })
        assertTrue(result.interrupted);assertEquals(0,result.updated);assertEquals("Behalten",RideNames.name(c,ride))
        c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address",account).commit()
        var stopped=false
        val cancelled=RideNames.pullDreeve(c,account,mapOf(ride to "Behalten"),{true},{stopped},{ stopped=true;"Zu spät" })
        assertTrue(cancelled.interrupted);assertEquals("Behalten",RideNames.name(c,ride))
    }
    @Test fun importedLinkIsVisibleWithUploadsDisabledAndHttpRenameEndpointsAreCorrect() {
        setup()
        val imported="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        RideServiceLinks.confirm(c,link().copy(rideId=imported,activityId="activity-import"))
        val row=RouteTransferRows.read(c,ManagedRoute(imported,"Test",false,"GPX",RouteDetails(),true,TrackOrigin.IMPORTED,true),false)
            .single { it.target==RouteTransferTarget.DREEVE }
        assertTrue(row.completed);assertFalse(row.canStart);assertEquals(R.string.dreeve_linked,row.text)
        val server=MockWebServer();server.start()
        try {
            val api=DreeveClient(ActivityServiceConfig(),"key",base=server.url("/"))
            server.enqueue(MockResponse().setBody("{\"name\":\"Neue Runde\"}"));assertEquals("Neue Runde",api.activityName("activity/a"))
            assertEquals("/api/v1/activities/activity%2Fa",server.takeRequest().path)
            server.enqueue(MockResponse().setBody("{}"));api.rename("activity/a","Neuer Titel")
            val patch=server.takeRequest();assertEquals("PATCH",patch.method);assertEquals("Neuer Titel",JSONObject(patch.body.readUtf8()).getString("name"))
            server.enqueue(MockResponse().setBody("{}"));IntervalsClient(IntervalsConfig(athlete="i1"),"key",base=server.url("/api/v1/")).rename("42","Titel")
            val put=server.takeRequest();assertEquals("PUT",put.method);assertEquals("/api/v1/activity/42",put.path)
            val key=KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            val settings=ActivityServiceSettings(c,ActivityService.STRAVA) { key }
            settings.write(ActivityServiceConfig(clientId="123"),"secret")
            settings.tokens("123","secret",JSONObject().put("access_token","access").put("refresh_token","refresh").put("expires_at",System.currentTimeMillis()/1000+3600).put("athlete",JSONObject().put("id",123)),true)
            server.enqueue(MockResponse().setBody("{}"));StravaClient(settings,base=server.url("/")).rename("1234","Titel")
            val strava=server.takeRequest();assertEquals("PUT",strava.method);assertEquals("/api/v3/activities/1234",strava.path);assertTrue(strava.body.readUtf8().contains("name=Titel"))
        } finally { server.shutdown() }
    }
}
