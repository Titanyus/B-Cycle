package de.bcycle.app

import android.content.Context
import android.content.ContextWrapper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideDirectoryBackupTest {
    private class IsolatedContext(base:Context,val prefix:String):ContextWrapper(base) {
        private val root=File(base.cacheDir,prefix).apply { mkdirs() }
        override fun getFilesDir()=File(root,"files").apply { mkdirs() }
        override fun getCacheDir()=File(root,"cache").apply { mkdirs() }
        override fun getSharedPreferences(name:String,mode:Int)=baseContext.getSharedPreferences(prefix+"-"+name,mode)
    }
    private class Library(val c:Context) {
        val fits=FitLibrary(c.filesDir).apply { load() }
        val gpx=GpxLibrary(c.filesDir).apply { load() }
        val summaries=DreeveSummaryLibrary(c.filesDir).apply { load() }
        val backup=RideDirectoryBackup(c,fits,gpx,summaries)
    }
    private fun context()=IsolatedContext(RuntimeEnvironment.getApplication(),"test-"+UUID.randomUUID())
    private fun recording(lib:Library):Pair<String,File> {
        val journal=File(lib.c.cacheDir,"complete.ride")
        val start=1792483200000L
        RideJournal(journal,start,RecordingConfig(setOf("HEART_RATE"))).use { writer ->
            for(second in 0..5) writer.append(RideSample(start+second*1000,second*1000L,50.0+second*.00001,8.0,
                mapOf("HEART_RATE" to 130.0,"RIDER_POWER" to 220.0,"MOTOR_POWER" to 300.0,"BATTERY" to 90.0,"SPEED" to 5.0,"BIKE_DISTANCE" to second*5.0)))
        }
        val reduced=File(lib.c.cacheDir,"reduced.fit");FitWriter.encode(journal,reduced)
        val retained=FitLibrary.retainRecording(lib.c.filesDir,reduced);lib.fits.load()
        val id=retained.nameWithoutExtension;RideAnalysisFiles.retain(lib.c.filesDir,id,journal);lib.fits.refresh()
        lib.fits.rename(id,"Meine vollständige Fahrt")
        return id to journal
    }
    private fun parent(c:Context)=FileBackupDirectory(File(c.cacheDir,"destination-"+UUID.randomUUID()).apply { mkdirs() })
    @Test fun completeFitGpxSummarySettingsAndBikeIdentitiesRoundTripWithoutInventingRecordsOrUploads() {
        val source=Library(context());val target=Library(context())
        val prefs=source.c.getSharedPreferences("bike",0);val bikes=BikeProfileStore(source.c,prefs)
        val first=bikes.current();bikes.save(first.copy(name="Gleicher Name"))
        val second=bikes.add()!!;bikes.select(second.slot);bikes.save(bikes.current().copy(name="Gleicher Name",type=BikeType.INDOOR))
        val (fitId,_)=recording(source)
        RideProfileColor.assign(source.c,fitId,first.slot)
        val gpxId=UUID.randomUUID().toString()
        val xml="<gpx><trk><trkseg>"+(0..3).joinToString("") { i ->
            "<trkpt lat='${50+i*.00001}' lon='8'><ele>100</ele><time>${java.time.Instant.ofEpochMilli(1792483200000+i*1000)}</time><extensions><power>210</power></extensions></trkpt>"
        }+"</trkseg></trk></gpx>"
        source.gpx.add(xml.byteInputStream(),"GPX mit Leistung",select=false,stableId=gpxId,preserveDate=true,bikeType=BikeType.BIKE)
        RideProfileColor.assign(source.c,gpxId,second.slot)
        val summaryId=UUID.randomUUID().toString()
        source.summaries.add(summaryId,DreeveActivity("remote-summary","Nur Zusammenfassung","2026-10-20T10:00:00",12000.0,"VirtualRide",false,
            DreeveSummary(mapOf(AnalysisMetric.DISTANCE to 12.0,AnalysisMetric.AVG_POWER to 200.0))),origin=TrackOrigin.RECORDED)
        RideServiceLinks.confirm(source.c,RideServiceLink(gpxId,RideService.DREEVE,"https://dreeve.example/","external-1",WebDavClient.hash(source.gpx.file(gpxId))))
        val destination=parent(source.c);val exported=source.backup.export(destination)
        assertEquals(3,exported.completed);assertEquals(0,exported.failed)
        val directory=destination.child(exported.folder,false)
        assertTrue(directory.entries().any { it.name=="$fitId.fit" })
        assertFalse(directory.entries().any { it.name=="$summaryId.fit" })
        val allSettings=target.backup.settings(directory)
        val settings=SettingsBackup(target.c,ThemeManager(target.c.getSharedPreferences("bike",0)))
        settings.apply(settings.plan(allSettings,allSettings.sections.keys))
        val result=target.backup.restore(directory)
        assertEquals(RideBackupResult(3),result)
        assertArrayEquals(source.fits.originalFile(fitId).readBytes(),target.fits.originalFile(fitId).readBytes())
        assertArrayEquals(source.fits.analysisFile(fitId).readBytes(),target.fits.analysisFile(fitId).readBytes())
        assertArrayEquals(source.gpx.file(gpxId).readBytes(),target.gpx.file(gpxId).readBytes())
        assertEquals(1,target.summaries.entries.size)
        assertEquals(source.summaries.entries.single().origin,target.summaries.entries.single().origin)
        assertEquals(200.0,target.summaries.entries.single().activity.summary.metrics[AnalysisMetric.AVG_POWER]!!,0.0)
        assertEquals(first.slot,RideProfileColor.slot(target.c,fitId))
        assertEquals(second.slot,RideProfileColor.slot(target.c,gpxId))
        assertNotEquals(BikeIdentities.id(prefs,first.slot),BikeIdentities.id(prefs,second.slot))
        assertEquals("external-1",RideServiceLinks.links(target.c,gpxId).single().activityId)
        assertEquals(3,target.backup.restore(directory).duplicates)
        assertEquals(1,target.fits.entries.size);assertEquals(1,target.gpx.entries.size)
        val body=JSONObject(target.c.getSharedPreferences("ride-names",0).getString("data","{}")!!)
        assertEquals(0,body.optJSONObject("tasks")?.length() ?: 0)
    }
    @Test fun unrelatedBikeWithSameSlotAndNameIsNeverAssignedAndBackupMetadataRemainsResolvable() {
        val source=Library(context());val target=Library(context())
        val a=BikeProfileStore(source.c,source.c.getSharedPreferences("bike",0))
        val b=BikeProfileStore(target.c,target.c.getSharedPreferences("bike",0))
        a.save(a.current().copy(name="Mein Rad"));b.save(b.current().copy(name="Mein Rad"))
        BikeIdentities.id(target.c.getSharedPreferences("bike",0),b.activeSlot)
        val (id,_)=recording(source);RideProfileColor.assign(source.c,id,a.activeSlot)
        val destination=parent(source.c);val report=source.backup.export(destination);val folder=destination.child(report.folder,false)
        assertEquals(1,target.backup.restore(folder).completed)
        assertNull(RideProfileColor.slot(target.c,id));assertEquals("Mein Rad",RideProfileColor.name(target.c,id))
        val settings=SettingsBackup(target.c,ThemeManager(target.c.getSharedPreferences("bike",0)))
        val doc=target.backup.settings(folder);settings.apply(settings.plan(doc,setOf(BackupSection.PROFILE)))
        assertEquals(a.activeSlot,RideProfileColor.slot(target.c,id))
    }
    @Test fun corruptFullFitIsReportedAndDoesNotInstallAReducedOrPartiallyRestoredRide() {
        val source=Library(context());val target=Library(context())
        val (id,_)=recording(source);val destination=parent(source.c);val report=source.backup.export(destination)
        val folder=destination.child(report.folder,false)
        val root=source.c.cacheDir.walkTopDown().single { it.name==report.folder }
        val fit=File(root,"$id.fit");val bytes=fit.readBytes();bytes[20]=(bytes[20].toInt() xor 1).toByte();fit.writeBytes(bytes)
        assertEquals(1,target.backup.restore(folder).failed)
        assertTrue(target.fits.entries.isEmpty())
        assertFalse(target.fits.originalFile(id).isFile)
        assertTrue(source.fits.originalFile(id).isFile)
    }
    @Test fun restoringProfilesMigratesExistingLegacyRideAndPhotoAwayFromAReusedSlot() {
        val source=Library(context());val target=Library(context())
        val sourceBikes=BikeProfileStore(source.c,source.c.getSharedPreferences("bike",0))
        val targetPrefs=target.c.getSharedPreferences("bike",0)
        val targetBikes=BikeProfileStore(target.c,targetPrefs)
        val oldId="recorded-"+"1".repeat(64)
        target.c.getSharedPreferences("ride-profile-colors",0).edit().putString(oldId,
            JSONObject().put("slot",targetBikes.activeSlot).put("name","Altes Rad").put("primary",0xff123456.toInt()).toString()).commit()
        val photo=BikePhotos.file(target.c,targetBikes.activeSlot);photo.parentFile!!.mkdirs();photo.writeBytes(byteArrayOf(1,2,3))
        val backup=SettingsBackup(source.c,ThemeManager(source.c.getSharedPreferences("bike",0)))
        val bytes=backup.export(setOf(BackupSection.PROFILE))
        val restore=SettingsBackup(target.c,ThemeManager(targetPrefs))
        val doc=restore.decode(bytes);restore.apply(restore.plan(doc,doc.sections.keys))
        assertNull(RideProfileColor.slot(target.c,oldId))
        assertEquals("Altes Rad",RideProfileColor.name(target.c,oldId))
        assertEquals(0xff123456.toInt(),RideProfileColor.color(target.c,oldId))
        assertFalse(BikePhotos.file(target.c,sourceBikes.activeSlot).isFile)
        assertTrue(File(target.c.filesDir,"bike-photos").listFiles().orEmpty().any { it.name.startsWith("by-id-") })
    }
    @Test fun photosInTheDirectoryBackupFollowThePortableBikeIdentity() {
        val source=Library(context());val target=Library(context())
        val bikes=BikeProfileStore(source.c,source.c.getSharedPreferences("bike",0))
        val photo=BikePhotos.file(source.c,bikes.activeSlot);photo.parentFile!!.mkdirs()
        val bitmap=android.graphics.Bitmap.createBitmap(8,8,android.graphics.Bitmap.Config.ARGB_8888)
        try { photo.outputStream().use { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSLESS,100,it)) } }
        finally { bitmap.recycle() }
        val destination=parent(source.c);val result=source.backup.export(destination);val folder=destination.child(result.folder,false)
        val settings=SettingsBackup(target.c,ThemeManager(target.c.getSharedPreferences("bike",0)))
        val doc=source.backup.settings(folder);settings.apply(settings.plan(doc,doc.sections.keys))
        target.backup.restore(folder)
        assertArrayEquals(photo.readBytes(),BikePhotos.file(target.c,bikes.activeSlot).readBytes())
    }
    @Test fun interruptedExportHasNoCommittedIndexAndNoExistingFileIsOverwritten() {
        val source=Library(context());recording(source)
        val root=parent(source.c)
        val broken=object:BackupDirectory by root {
            override fun child(name:String,create:Boolean):BackupDirectory {
                val actual=root.child(name,create)
                return object:BackupDirectory by actual {
                    override fun write(name:String,source:File) {
                        if(name==RideDirectoryBackup.INDEX) throw java.io.IOException("full")
                        actual.write(name,source)
                    }
                }
            }
        }
        assertThrows(java.io.IOException::class.java) { source.backup.export(broken) }
        val folder=root.child(root.entries().single().name,false)
        assertFalse(folder.entries().any { it.name==RideDirectoryBackup.INDEX })
        assertThrows(Exception::class.java) { source.backup.restore(folder) }
    }
    @Test fun fullDirectoryAlsoRestoresPersonLevelHrvMeasurementsAndRawCapture() {
        val source=Library(context());val target=Library(context())
        val store=HrvStore(File(source.c.filesDir,"hrv-measurements"))
        val id=UUID.randomUUID().toString();val session=de.bcycle.hrv.MeasurementSession(0)
        store.begin(id,1000,"Gurt",session)
        session.accept(1000,byteArrayOf(0x16,75,0,4));store.append(id,session.packets.last())
        session.abort("USER_ABORT");store.save(id,session,de.bcycle.hrv.Evaluate.session(session),1000)
        val destination=parent(source.c);val exported=source.backup.export(destination)
        assertEquals(1,exported.hrvCompleted);assertEquals(0,exported.hrvFailed)
        val folder=destination.child(exported.folder,false)
        val imported=target.backup.restore(folder)
        assertEquals(1,imported.hrvCompleted);assertEquals(0,imported.hrvFailed)
        val restored=HrvStore(File(target.c.filesDir,"hrv-measurements"))
        assertArrayEquals(store.backup(id),restored.backup(id))
        assertEquals(1,target.backup.restore(folder).hrvDuplicates)
    }

}
