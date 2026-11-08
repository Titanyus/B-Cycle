package de.bcycle.app

import com.garmin.fit.FitDecoder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/** Every file in these tests lives under Robolectric's isolated application temp directory. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideFinishTransfersTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private fun fixture():File {
        val journal=File(c.cacheDir,"finish.ride")
        javaClass.getResourceAsStream("/recording/legacy-recording.ride")!!.use { input -> journal.outputStream().use { input.copyTo(it) } }
        val fit=File(c.cacheDir,"finish.fit");FitWriter.encode(journal,fit)
        return FitLibrary.retainRecording(c.filesDir,fit)
    }
    private fun drain() {
        (RideFinishTransfers::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(null) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
    }
    @Test fun heldIntentDefersAllExternalWorkButKeepsInternalRideAndFeedback() {
        val fit=fixture();val id=fit.nameWithoutExtension;val bytes=fit.readBytes()
        RideFinishTransfers.hold(c,id,"planned.fit",RecordingDestination.entries.toSet(),"")
        RideAnnotations.note(c,id,"Vor dem Export");RideAnnotations.rpe(c,id,7);RideAnnotations.flush(c,id)
        var writes=0
        RideFinishTransfers.runPending(c,id,local={ _,_,_->writes++ },cloud={ _,_,_->writes++ })
        assertEquals(0,writes);assertFalse(RouteUploads.wasQueued(c,id))
        assertEquals("Vor dem Export",RideAnnotations.read(c,id).note)
        assertEquals(7,RideAnnotations.read(c,id).rpe);assertArrayEquals(bytes,fit.readBytes())
        assertTrue(c.getSharedPreferences("ride-annotations",0).getBoolean("dirty:$id",false))
        RideFinishTransfers.forget(c,id);RideAnnotations.forget(c,id)
    }
    @Test fun releasedIntentExportsLatestFeedbackAndRetriesOnlyFailedDestination() {
        val fit=fixture();val id=fit.nameWithoutExtension;val bytes=fit.readBytes()
        RideFinishTransfers.hold(c,id,"planned.fit",setOf(RecordingDestination.LOCAL_FOLDER,RecordingDestination.WEBDAV),"")
        RideAnnotations.note(c,id,"Letzte Notiz");RideAnnotations.rpe(c,id,0)
        val prefs=c.getSharedPreferences("ride-finish-transfers",0)
        prefs.edit().putString(id,JSONObject(prefs.getString(id,null)!!).put("held",false).toString()).commit()
        var cloud=0;var local=0
        RideFinishTransfers.runPending(c,id,local={ file,name,_->
            local++;assertEquals("planned.fit",name)
            val decoded=file.inputStream().use { FitDecoder().decode(it) }
            assertEquals(0,decoded.sessionMesgs.first().workoutRpe.toInt())
            throw java.io.IOException("Synthetic destination failure")
        },cloud={ _,_,_->cloud++ })
        assertEquals(1,local);assertEquals(1,cloud);assertTrue(prefs.contains(id))
        RideFinishTransfers.runPending(c,id,local={ file,_,_->local++;assertTrue(file.isFile) },cloud={ _,_,_->fail("Already queued destination") })
        assertEquals(2,local);assertFalse(prefs.contains(id));assertArrayEquals(bytes,fit.readBytes())
        RideAnnotations.forget(c,id)
    }
    @Test fun dismissReleaseIsIdempotentAndRecoveryReleasesPersistedHeldPlans() {
        val fit=fixture();val id=fit.nameWithoutExtension
        RouteExportSettings(c).write(RouteExportConfig(save=false,webdav=true,server="https://cloud.example.org",user="rider"))
        RideFinishTransfers.hold(c,id,"planned.fit",setOf(RecordingDestination.WEBDAV),"")
        RouteExportSettings(c).write(RouteExportConfig(save=false,webdav=true,server="https://other.example.org",user="rider"))
        RideFinishTransfers.release(c,id);drain()
        assertFalse("Do not send to another account after restart",RouteUploads.wasQueued(c,id))
        assertTrue(c.getSharedPreferences("ride-finish-transfers",0).contains(id))
        RouteExportSettings(c).write(RouteExportConfig(save=false,webdav=true,server="https://cloud.example.org",user="rider"))
        RideFinishTransfers.recover(c);drain()
        assertTrue(RouteUploads.wasQueued(c,id));assertFalse(RideFinishTransfers.held(c,id))
        val revision=RouteUploads.revision(c)
        RideFinishTransfers.release(c,id);drain();assertEquals(revision,RouteUploads.revision(c))
        RideFinishTransfers.hold(c,id,"planned.fit",setOf(RecordingDestination.WEBDAV),"")
        RideFinishTransfers.recover(c);drain()
        assertFalse(c.getSharedPreferences("ride-finish-transfers",0).contains(id));assertTrue(fit.isFile)
    }
    @Test fun defaultLocalExportIncludesFeedbackAndItsReceiptStillMatchesTheInternalSource() {
        val fit=fixture();val id=fit.nameWithoutExtension;val before=fit.readBytes()
        val folder=Folder(c.cacheDir).apply { attachInfo(c,android.content.pm.ProviderInfo().apply {
            authority="bcycle.finish.folder";exported=true;grantUriPermissions=true
            readPermission="android.permission.MANAGE_DOCUMENTS";writePermission=readPermission
        }) }
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("bcycle.finish.folder",folder)
        val tree=android.provider.DocumentsContract.buildTreeDocumentUri("bcycle.finish.folder","root").toString()
        RideFinishTransfers.hold(c,id,"planned.fit",setOf(RecordingDestination.LOCAL_FOLDER),tree)
        RideAnnotations.note(c,id,"Fertig vor Export");RideAnnotations.rpe(c,id,6)
        RideFinishTransfers.release(c,id);drain()
        val saved=folder.files.values.single()
        val decoded=saved.inputStream().use { FitDecoder().decode(it) }
        assertEquals(60,decoded.sessionMesgs.first().workoutRpe.toInt())
        assertTrue(LocalRouteExport.has(c,id))
        assertTrue(RouteLocalSaves.completed(c,id,RouteFileStamp(fit.length(),fit.lastModified(),WebDavClient.hash(fit))))
        assertArrayEquals(before,fit.readBytes());RideAnnotations.forget(c,id)
    }
    private class Folder(private val directory:File):android.provider.DocumentsProvider() {
        val files=linkedMapOf<String,File>()
        override fun onCreate()=true
        override fun queryRoots(projection:Array<out String>?)=android.database.MatrixCursor(arrayOf(android.provider.DocumentsContract.Root.COLUMN_ROOT_ID,android.provider.DocumentsContract.Root.COLUMN_DOCUMENT_ID))
        override fun queryDocument(id:String,projection:Array<out String>?)=android.database.MatrixCursor(arrayOf(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)).apply { addRow(arrayOf(id)) }
        override fun queryChildDocuments(parent:String,projection:Array<out String>?,sort:String?)=queryDocument(parent,projection)
        override fun isChildDocument(parent:String,id:String)=parent=="root" && files.containsKey(id)
        override fun getDocumentType(id:String)="application/vnd.ant.fit"
        override fun createDocument(parent:String,mime:String,name:String):String="doc-${files.size}".also { files[it]=File(directory,"$it.fit") }
        override fun openDocument(id:String,mode:String,signal:android.os.CancellationSignal?):android.os.ParcelFileDescriptor =
            android.os.ParcelFileDescriptor.open(files.getValue(id),if(mode.contains('w')) android.os.ParcelFileDescriptor.MODE_WRITE_ONLY or android.os.ParcelFileDescriptor.MODE_CREATE or android.os.ParcelFileDescriptor.MODE_TRUNCATE else android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        override fun deleteDocument(id:String) { files.remove(id)?.delete() }
    }

}
