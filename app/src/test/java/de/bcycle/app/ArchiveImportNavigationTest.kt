package de.bcycle.app

import android.net.Uri
import android.os.Looper
import android.view.View
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class ArchiveImportNavigationTest {
    @Test fun selectionAppearsBeforeBackgroundUnpackingAndLibraryRemainsUntouchedUntilSelection() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup();val release=CountDownLatch(1)
        try {
            val a=host.get();val state=RideRuntime.get(a).mapState
            fun waitFor(test:()->Boolean) { val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
                while(!test() && System.nanoTime()<deadline) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5) };assertTrue(test()) }
            waitFor { !state.busy }
            val worker=MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as java.util.concurrent.ExecutorService
            val entered=CountDownLatch(1);worker.execute { entered.countDown();release.await(10,TimeUnit.SECONDS) }
            assertTrue(entered.await(3,TimeUnit.SECONDS))
            val zip=File(a.cacheDir,"selection-test.zip")
            ZipOutputStream(zip.outputStream()).use { out -> out.putNextEntry(ZipEntry("ride.gpx"));out.write("<gpx><trk><trkseg><trkpt lat='50' lon='8'/><trkpt lat='50.01' lon='8'/></trkseg></trk></gpx>".toByteArray());out.closeEntry() }
            MainActivity::class.java.getDeclaredMethod("showArchiveImport",Uri::class.java).apply { isAccessible=true }.invoke(a,Uri.fromFile(zip))
            val screen=ShadowAlertDialog.getLatestAlertDialog();val root=screen.window!!.decorView
            val processing=root.findViewWithTag<TextView>("archive_preparing")
            assertEquals(View.VISIBLE,processing.visibility);assertTrue(processing.text.contains("ZIP"))
            assertFalse(root.findViewWithTag<View>("dreeve_import_selected").isEnabled)
            assertEquals(SettingsHelpPage.ARCHIVE_IMPORT,SettingsHelp.resolve(screen))
            assertTrue(state.entries.isEmpty())
            release.countDown();waitFor { !state.busy && processing.visibility!=View.VISIBLE }
            assertTrue(state.entries.isEmpty());assertEquals(View.GONE,processing.visibility)
            screen.dismiss();zip.delete()
        } finally { release.countDown();host.pause().stop().destroy() }
    }
}
