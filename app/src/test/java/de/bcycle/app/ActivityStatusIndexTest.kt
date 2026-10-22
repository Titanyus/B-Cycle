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
class ActivityStatusIndexTest {
    @Test fun indexedHistoryStillChecksLiveAccountProfileAndExactSourceVersion() {
        val c=RuntimeEnvironment.getApplication();val profiles=BikeProfileStore(c,c.getSharedPreferences("bike",0))
        val profile=profiles.current();val stamp=RouteFileStamp(100,200,"source")
        RideProfileColor.assign(c,"ride",profile.slot)
        for(service in ActivityService.entries) {
            val target=if(service==ActivityService.DREEVE) "https://dreeve.example.org/" else "123"
            val config=c.getSharedPreferences("activity-service-${service.name.lowercase()}",0)
            config.edit().putString(if(service==ActivityService.DREEVE) "address" else "account",target).commit()
            val prefs=c.getSharedPreferences("activity-uploads-${service.name.lowercase()}",0)
            val queue=if(service==ActivityService.DREEVE) ActivityServiceUploads.dreeve else ActivityServiceUploads.strava
            fun row(ride:String,sequence:Int,state:String)=JSONObject().put("ride",ride).put("target",target)
                .put("sequence",sequence).put("state",state).put("length",100).put("modified",200).put("hash","source")
                .put("sourceBikeType",profile.type.name)
            val rows=JSONObject()
            repeat(2000) { rows.put("unrelated-$it",row("other-$it",it,"saved")) }
            rows.put("matching",row("ride",2001,"saved"))
            rows.put("different-version",row("ride",2002,"failed").put("hash","different").put("problem","auth"))
            fun save() { prefs.edit().putString("entries",rows.toString()).commit() }
            save()
            val started=System.nanoTime()
            repeat(100) { assertEquals(UploadState.UPLOADED,queue.state(c,"ride",stamp).state) }
            println("$service 2002 jobs + 100 lookups: ${(System.nanoTime()-started)/1_000_000} ms")
            assertEquals(R.string.service_auth_failed,queue.problem(c))
            for((state,expected) in listOf("pending" to UploadState.WAITING,"processing" to UploadState.UPLOADING,"failed" to UploadState.FAILED,"saved" to UploadState.UPLOADED)) {
                rows.getJSONObject("matching").put("state",state);save()
                assertEquals(expected,queue.state(c,"ride",stamp).state)
            }
            // A converted payload compares the original source stamp, not its generated FIT bytes.
            rows.getJSONObject("matching").put("length",999).put("sourceLength",100).put("sourceModified",200).put("sourceHash","source")
            save();assertEquals(UploadState.UPLOADED,queue.state(c,"ride",stamp).state)
            profiles.save(profile.copy(type=BikeType.INDOOR))
            assertEquals(UploadState.NOT_UPLOADED,queue.state(c,"ride",stamp).state)
            profiles.save(profile)
            config.edit().putString(if(service==ActivityService.DREEVE) "address" else "account",if(service==ActivityService.DREEVE) "https://other.example.org/" else "456").commit()
            assertEquals(UploadState.NOT_UPLOADED,queue.state(c,"ride",stamp).state);assertNull(queue.problem(c))
        }
    }
}
