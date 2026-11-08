package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RideSummaryExitTest {
    @Test fun saveBackOutsideCancelAndBackgroundAllReleaseTheRetainedRide() {
        for(exit in listOf("save","back","outside","background")) {
            val context=RuntimeEnvironment.getApplication()
            context.getSharedPreferences(HrvSettings.STORE,0).edit().putBoolean("showOnFirstStart",false).commit()
            val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
            val a=host.get();val runtime=RideRuntime.get(a)
            fun drain() {
                (MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(runtime.mapState) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
                shadowOf(Looper.getMainLooper()).idle()
            }
            try {
                val journal=File(a.cacheDir,"summary-exit.ride")
                javaClass.getResourceAsStream("/recording/legacy-recording.ride")!!.use { input -> journal.outputStream().use { input.copyTo(it) } }
                val encoded=File(a.cacheDir,"summary-exit.fit");FitWriter.encode(journal,encoded)
                val fit=FitLibrary.retainRecording(a.filesDir,encoded);val id=fit.nameWithoutExtension
                RideAnalysisFiles.retain(a.filesDir,id,journal)
                a.getSharedPreferences("route-uploads",0).edit().putString("entries","{}").putLong("revision",System.nanoTime()).commit()
                RouteExportSettings(a).write(RouteExportConfig(save=false,webdav=true,server="https://cloud.example.org",user="rider",wifiOnly=false))
                RideFinishTransfers.hold(a,id,"exit.fit",setOf(RecordingDestination.WEBDAV),"")
                runtime.recordingChanged(RecordingUpdate.Finished(RideCompletion.Completed(id,setOf(RecordingDestination.WEBDAV))))
                repeat(4) { drain() }
                val dialog=ShadowAlertDialog.getLatestAlertDialog()
                val content=dialog.window!!.decorView.findViewWithTag<RideSummaryView>("ride_summary")
                assertNotNull(content);assertTrue(RideFinishTransfers.held(a,id));assertFalse(RouteUploads.wasQueued(a,id))
                val note=content.findViewWithTag<EditText>("ride_note_editor")
                note.requestFocus();note.setText("Text vor $exit")
                content.finishNoteOutside(-1f,-1f);assertFalse(note.hasFocus())
                assertEquals("Text vor $exit",RideAnnotations.read(a,id).note)
                when(exit) {
                    "save"->content.findViewWithTag<View>("ride_summary_save").performClick()
                    "back"->dialog.onBackPressed()
                    "outside"->dialog.cancel() // Android outside-touch routes through cancel/dismiss.
                    else->host.pause().stop()
                }
                shadowOf(Looper.getMainLooper()).idle()
                (RideFinishTransfers::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(null) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
                assertFalse("Every exit releases: $exit",RideFinishTransfers.held(a,id))
                assertTrue("Every exit queues: $exit",RouteUploads.wasQueued(a,id))
                assertTrue(fit.isFile);assertTrue(RideAnalysisFiles.file(a.filesDir,id).isFile)
                RideAnnotations.forget(a,id)
            } finally { host.pause().stop().destroy() }
        }
    }
}
