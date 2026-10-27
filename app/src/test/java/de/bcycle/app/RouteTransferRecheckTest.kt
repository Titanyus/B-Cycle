package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RouteTransferRecheckTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private fun fixture():Pair<File,ManagedRoute> {
        val journal=File(c.cacheDir,"recheck.ride")
        RideJournal(journal,1791626400000L,RecordingConfig(setOf("HEART_RATE"))).use {
            it.append(RideSample(1791626400000L,0,values=mapOf("HEART_RATE" to 120.0)))
            it.append(RideSample(1791626412000L,12000,values=mapOf("HEART_RATE" to 130.0)))
        }
        val export=File(c.cacheDir,"recheck.fit");FitWriter.encode(journal,export)
        val file=FitLibrary.retainRecording(c.filesDir,export)
        return file to ManagedRoute(file.nameWithoutExtension,"Test",true,"FIT",RouteDetails(),false,TrackOrigin.RECORDED,true,
            RouteFileStamp(file.length(),file.lastModified(),WebDavClient.hash(file)))
    }
    private fun configure() {
        c.getSharedPreferences("activity-service-dreeve",0).edit().putBoolean("enabled",true).putBoolean("wifi",false).putString("address","https://dreeve.example/").commit()
        c.getSharedPreferences("activity-service-strava",0).edit().putBoolean("enabled",true).putBoolean("wifi",false).putString("account","123").commit()
        c.getSharedPreferences("intervals-settings",0).edit().putBoolean("enabled",true).putBoolean("wifi",false).putString("athlete","i1").commit()
    }
    private fun complete(target:RouteTransferTarget,id:String,remote:String) {
        when(target) {
            RouteTransferTarget.INTERVALS->assertFalse(IntervalsUploads.run(c,{false}) { _,_,_,_->remote })
            else->assertFalse(ActivityServiceUploads.forService(if(target==RouteTransferTarget.DREEVE) ActivityService.DREEVE else ActivityService.STRAVA)
                .run(c,{false}) { _,_,_,_,job,_->assertTrue("A replacement must have its own upload attempt",job.isEmpty());UploadProgress("accepted",remote) })
        }
    }
    @Test fun confirmedActivitiesAreCheckedAndOnlyMissingTargetsAreUploadedAgain() {
        configure();val (_,entry)=fixture()
        val targets=listOf(RouteTransferTarget.DREEVE,RouteTransferTarget.INTERVALS)
        targets.forEachIndexed { index,target -> target.retry(c,entry.id);complete(target,entry.id,"old-$index") }
        for(target in targets) {
            assertTrue(target.status(c,entry).state==UploadState.UPLOADED)
            val links=RideServiceLinks.links(c,entry.id)
            RouteTransferRecheck.run(c,entry,target) { link,stopped ->assertTrue(link in links);assertFalse(stopped());true }
            assertEquals(UploadState.UPLOADED,target.status(c,entry).state)
            assertEquals(links,RideServiceLinks.links(c,entry.id))
            RideNames.manual(c,entry.id,"Meine Fahrt")
            RouteTransferRecheck.run(c,entry,target) { _,_->false }
            assertEquals(UploadState.WAITING,target.status(c,entry).state)
            assertEquals(links.size-1,RideServiceLinks.links(c,entry.id).size)
            assertEquals("Meine Fahrt",RideNames.name(c,entry.id))
            assertNull(RideNames.pending(c,entry.id,links.single { it.activityId=="old-${targets.indexOf(target)}" }.service))
            complete(target,entry.id,"replacement-${target.name}")
            assertEquals(UploadState.UPLOADED,target.status(c,entry).state)
            assertEquals(links.size,RideServiceLinks.links(c,entry.id).size)
        }
    }
    @Test fun changedExportIsQueuedEvenWhenCloudStillHasPreviousFileAndOldLinkIsRetained() {
        configure();val (file,entry)=fixture();val target=RouteTransferTarget.DREEVE
        target.retry(c,entry.id);complete(target,entry.id,"original-remote")
        val links=RideServiceLinks.links(c,entry.id)
        val regenerated=FitReexport.file(c.filesDir,entry.id).apply { parentFile!!.mkdirs() }
        FitReexport.encode(file,regenerated,RecordingConfig(emptySet()))
        RouteTransferRecheck.run(c,entry,target) { _,_->true }
        assertEquals(links,RideServiceLinks.links(c,entry.id))
        val stamp=RouteFileStamp(regenerated.length(),regenerated.lastModified(),WebDavClient.hash(regenerated))
        assertEquals(UploadState.WAITING,ActivityServiceUploads.dreeve.state(c,entry.id,stamp).state)
    }
    @Test fun networkAndAccountChangesNeverEraseReceiptsOrQueueAnUpload() {
        configure();val (_,entry)=fixture();val target=RouteTransferTarget.DREEVE
        target.retry(c,entry.id);complete(target,entry.id,"remote")
        val links=RideServiceLinks.links(c,entry.id)
        assertThrows(java.io.IOException::class.java) { RouteTransferRecheck.run(c,entry,target) { _,_->throw java.io.IOException("offline") } }
        assertEquals(UploadState.UPLOADED,target.status(c,entry).state)
        assertEquals(links,RideServiceLinks.links(c,entry.id))
        assertThrows(IllegalStateException::class.java) {
            RouteTransferRecheck.run(c,entry,target) { _,stopped ->
                c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address","https://other.example/").commit()
                assertTrue(stopped());false
            }
        }
        assertEquals(links,RideServiceLinks.links(c,entry.id))
        assertEquals(UploadState.NOT_UPLOADED,target.status(c,entry).state)
        configure();assertEquals(UploadState.UPLOADED,target.status(c,entry).state)
    }
    @Test fun changedSourceDuringCheckCannotQueueTheWrongFile() {
        configure();val (file,entry)=fixture();val target=RouteTransferTarget.DREEVE
        target.retry(c,entry.id);complete(target,entry.id,"remote")
        val links=RideServiceLinks.links(c,entry.id)
        assertThrows(IllegalStateException::class.java) { RouteTransferRecheck.run(c,entry,target) { _,_->file.appendText("changed");false } }
        assertEquals(links,RideServiceLinks.links(c,entry.id))
        assertEquals(UploadState.UPLOADED,target.status(c,entry).state)
    }
    @Test fun webdavRechecksTheSameFilenameRatherThanCreatingAnotherRemoteFile() {
        val (_,entry)=fixture()
        RouteExportSettings(c).write(RouteExportConfig(webdav=true,server="https://cloud.example",user="user",wifiOnly=false))
        RouteUploads.retry(c,entry.id)
        val names=mutableListOf<String>()
        assertFalse(RouteUploads.run(c,{false}) { _,_,name,_->names+=name })
        assertEquals(UploadState.UPLOADED,RouteUploads.state(c,entry.id,entry.fileStamp).state)
        RouteTransferRecheck.run(c,entry,RouteTransferTarget.WEBDAV)
        assertEquals(UploadState.WAITING,RouteUploads.state(c,entry.id,entry.fileStamp).state)
        assertFalse(RouteUploads.run(c,{false}) { _,_,name,_->names+=name })
        assertEquals(2,names.size);assertEquals(names[0],names[1])
    }
    @Test fun missingDreeveProvenanceCanBeClearedWithoutUploadingAPlannedTrack() {
        configure()
        val library=GpxLibrary(c.filesDir);library.load()
        library.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(),"Geplant")
        val route=library.entries.single()
        val entry=ManagedRoute(route.id,route.name,false,"GPX",RouteDetails(),true,TrackOrigin.IMPORTED,true)
        val link=RideServiceLink(route.id,RideService.DREEVE,"https://dreeve.example/","deleted",WebDavClient.hash(library.file(route.id)))
        RideServiceLinks.confirm(c,link)
        assertTrue(RouteTransferRows.read(c,entry,false).single { it.target==RouteTransferTarget.DREEVE }.completed)
        RouteTransferRecheck.run(c,entry,RouteTransferTarget.DREEVE) { _,_->false }
        assertTrue(RideServiceLinks.links(c,entry.id).isEmpty())
        val row=RouteTransferRows.read(c,entry,false).single { it.target==RouteTransferTarget.DREEVE }
        assertFalse(row.completed);assertEquals(R.string.route_transfer_ineligible,row.text)
    }

    @Test fun stravaExplicitlyRepeatsWithWritePermissionsAndKeepsTheUnverifiedOldLink() {
        configure();val (_,entry)=fixture();val target=RouteTransferTarget.STRAVA
        target.retry(c,entry.id);complete(target,entry.id,"42")
        val before=RideServiceLinks.links(c,entry.id)
        RouteTransferRecheck.run(c,entry,target) { _,_->fail("Do not mistake limited Strava permissions for missing activities");false }
        assertEquals(UploadState.WAITING,target.status(c,entry).state)
        assertEquals(before,RideServiceLinks.links(c,entry.id))
        complete(target,entry.id,"43")
        assertEquals(UploadState.UPLOADED,target.status(c,entry).state)
        assertEquals(setOf("42","43"),RideServiceLinks.links(c,entry.id).map { it.activityId }.toSet())
    }

}
