package de.bcycle.app

import android.app.job.JobScheduler
import android.content.Context
import android.os.Looper
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteExportTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @Test fun passwordIsEncryptedAuthenticatedAndReusableWhileNonSecretSettingsRemainIndependent() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val settings = RouteExportSettings(context) { key }
        val config = RouteExportConfig(save = false, webdav = true, server = "https://cloud.example.org", user = "rider")
        settings.write(config, "private-app-password")
        val raw = context.getSharedPreferences("route-export", 0).getString("password", "")!!
        assertFalse(raw.contains("private-app-password")); assertTrue(settings.hasPassword())
        assertEquals("private-app-password", RouteExportSettings(context) { key }.password())
        settings.write(config.copy(webdav = false)); assertEquals("private-app-password", settings.password())
        assertFalse(settings.read().save); assertFalse(settings.read().webdav)
        context.getSharedPreferences("route-export", 0).edit().putString("password", raw.dropLast(4) + "AAAA").commit()
        assertThrows(Exception::class.java) { settings.password() }
    }
    @Test fun passwordEyePreservesDraftCursorAndMaskingAndLoadsStoredSecretOnlyOnExplicitClick() {
        var reads = 0
        val config = RouteExportConfig(tree = "content://chosen/tree", folder = "Sport/Fahrten")
        val view = RouteExportView(context, config, true, {}, {}, storedPassword = { reads++; "saved-test-password" })
        fun descendants(node: View): List<View> = listOf(node) + if (node is android.view.ViewGroup)
            (0 until node.childCount).flatMap { descendants(node.getChildAt(it)) } else emptyList()
        assertFalse(descendants(view).filterIsInstance<android.widget.Button>().any { it.text.toString() == "Downloads / B-Cycle verwenden" })
        val eye = descendants(view).single { it.tag == "export_password_visibility" }
        val inputType = view.password.inputType
        assertTrue(view.password.text.isEmpty()); assertEquals(0, reads)
        assertTrue(view.password.transformationMethod is android.text.method.PasswordTransformationMethod)
        assertFalse(view.password.isSaveEnabled)
        view.password.setText("typed-test-password"); view.password.setSelection(5)
        eye.performClick()
        assertNull(view.password.transformationMethod); assertEquals(5, view.password.selectionStart)
        assertEquals("typed-test-password", view.password.text.toString()); assertEquals(0, reads)
        assertEquals(inputType, view.password.inputType); assertEquals("Passwort verbergen", eye.contentDescription)
        eye.performClick()
        assertTrue(view.password.transformationMethod is android.text.method.PasswordTransformationMethod)
        assertEquals(5, view.password.selectionStart); assertEquals("Passwort anzeigen", eye.contentDescription)
        view.password.setText(""); eye.performClick()
        assertEquals(1, reads); assertEquals("saved-test-password", view.password.text.toString())
        assertNull(view.password.transformationMethod)
        eye.performClick(); assertTrue(view.password.transformationMethod is android.text.method.PasswordTransformationMethod)
        assertEquals(config.tree, view.read().tree); assertEquals(config.folder, view.read().folder)
        val reopened = RouteExportView(context, config, true, {}, {}, storedPassword = { reads++; "saved-test-password" })
        assertTrue(reopened.password.text.isEmpty()); assertEquals(1, reads)
        assertTrue(reopened.password.transformationMethod is android.text.method.PasswordTransformationMethod)
    }
    @Test fun durableJobsRetryTransientFailuresPreserveTargetAndNeverRequeueCompletedUpload() {
        val settings = RouteExportSettings(context)
        val config = RouteExportConfig(save = false, webdav = true, server = "https://cloud.example.org", user = "rider", wifiOnly = false)
        settings.write(config)
        val file = File(context.filesDir, "fit-library/recorded-${"a".repeat(64)}.fit").apply { parentFile!!.mkdirs(); writeText("FIT") }
        RouteUploads.enqueue(context, file, "B-Cycle-test.fit")
        assertEquals(1, context.getSystemService(JobScheduler::class.java).allPendingJobs.size)
        assertTrue(RouteUploads.run(context, { false }) { _, _, _, _ -> throw DavFailure(true, "Netzwerk vorübergehend nicht erreichbar") })
        assertTrue(RouteUploads.status(context, file.nameWithoutExtension).contains("ausstehend"))
        assertFalse(RouteUploads.run(context, { false }) { _, source, name, _ -> assertEquals(file, source); assertEquals("B-Cycle-test.fit", name) })
        assertEquals("Nextcloud gesichert", RouteUploads.status(context, file.nameWithoutExtension))
        RouteUploads.enqueue(context, file, "B-Cycle-test.fit")
        var count = 0
        assertFalse(RouteUploads.run(context, { false }) { _, _, _, _ -> count++ }); assertEquals(0, count)
        file.appendText("changed")
        RouteUploads.retry(context, file.nameWithoutExtension)
        settings.write(config.copy(server = "https://other.example.org"))
        assertFalse(RouteUploads.run(context, { false }) { _, _, _, _ -> fail("Changed target must not receive queued FIT") })
        assertEquals(UploadState.NOT_UPLOADED, RouteUploads.state(context, file.nameWithoutExtension).state)
        assertTrue(file.isFile)
        settings.write(config.copy(webdav = false)); RouteUploads.settingsChanged(context)
        assertTrue(context.getSystemService(JobScheduler::class.java).allPendingJobs.isEmpty())
    }
    @Test fun bothOutputsDisabledStillFinalizesAndRetainsOriginalActivityWithoutDownloadsOrNetwork() {
        RouteExportSettings(context).write(RouteExportConfig(save = false, webdav = false))
        var status = ""
        val store = RecordingStore(context) { update -> status = RecordingStatusText.text(context, update) }
        val start = 1791626400000L
        store.begin(start, RecordingConfig(setOf("HEART_RATE")))
        store.append(RideSample(start, 0, values = mapOf("HEART_RATE" to 123.0)))
        store.finish()
        val deadline = System.currentTimeMillis() + 5000
        while (!status.startsWith("Gespeichert:") && System.currentTimeMillis() < deadline) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
        assertTrue(status.startsWith("Gespeichert:"))
        val library = FitLibrary(context.filesDir); library.load()
        assertEquals(1, library.entries.size); assertTrue(library.entries.single().recorded)
        assertTrue(File(context.filesDir, "pending-rides").listFiles()!!.isEmpty())
        assertTrue(context.getSystemService(JobScheduler::class.java).allPendingJobs.isEmpty())
    }
    @Test fun cloudOnlyStopEnqueuesDurableFitAndLocalFolderFailureDoesNotPreventCloudQueue() {
        val config = RouteExportConfig(save = false, webdav = true, server = "https://cloud.example.org", user = "rider", wifiOnly = false)
        RouteExportSettings(context).write(config)
        fun stop(): String {
            var status = ""
            val store = RecordingStore(context) { update -> status = RecordingStatusText.text(context, update) }
            val start = 1791626400000L
            store.begin(start, RecordingConfig(setOf("HEART_RATE")))
            store.append(RideSample(start, 0, values = mapOf("HEART_RATE" to 123.0)))
            store.finish()
            val deadline = System.currentTimeMillis() + 5000
            while (!(status.startsWith("Gespeichert:") || status.startsWith("FIT lokal gespeichert")) && System.currentTimeMillis() < deadline) {
                Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle()
            }
            return status
        }
        assertTrue(stop().startsWith("Gespeichert:"))
        val first = FitLibrary(context.filesDir).apply { load() }.entries.single()
        assertTrue(RouteUploads.status(context, first.id).contains("ausstehend"))
        assertTrue(File(context.filesDir, "pending-rides").listFiles()!!.isEmpty())
        RouteExportSettings(context).write(config.copy(save = true, tree = "content://missing/tree/root"))
        assertTrue(stop().startsWith("FIT lokal gespeichert"))
        assertEquals(1, File(context.filesDir, "pending-rides").listFiles { f -> f.extension == "ride" }!!.size)
        assertTrue(RouteUploads.status(context, first.id).contains("ausstehend"))
        assertEquals(1, context.getSystemService(JobScheduler::class.java).allPendingJobs.size)
    }
    @Test fun jobsAddedDuringUploadRemainScheduledAndDisabledCloudDoesNotUpload() {
        val config = RouteExportConfig(webdav = true, server = "https://cloud.example.org", user = "rider", wifiOnly = false)
        RouteExportSettings(context).write(config)
        fun fit(letter: String) = File(context.filesDir, "fit-library/recorded-${letter.repeat(64)}.fit").apply { parentFile!!.mkdirs(); writeText(letter) }
        val first = fit("a"); val second = fit("b")
        RouteUploads.enqueue(context, first, "B-Cycle-first.fit")
        assertTrue(RouteUploads.run(context, { false }) { _, _, _, _ -> RouteUploads.enqueue(context, second, "B-Cycle-second.fit") })
        assertEquals("Nextcloud gesichert", RouteUploads.status(context, first.nameWithoutExtension))
        assertTrue(RouteUploads.status(context, second.nameWithoutExtension).contains("ausstehend"))
        RouteExportSettings(context).write(config.copy(webdav = false))
        assertFalse(RouteUploads.run(context, { false }) { _, _, _, _ -> fail("Disabled upload must not run") })
        assertEquals("Upload pausiert", RouteUploads.status(context, second.nameWithoutExtension))
    }
    @Test fun managerHasNoConfigurationEntryAndViewsKeepBothOutputFlagsIndependent() {
        val theme = android.view.ContextThemeWrapper(context, R.style.SettingsDialogTheme)
        val manager = RouteManagerView(theme, {})
        assertNull(manager.findViewWithTag<View>("route_settings"))
        val view = RouteExportView(theme, RouteExportConfig(save = false, webdav = true), false, {}, {})
        assertFalse(view.read().save); assertTrue(view.read().webdav)
        (view.findViewWithTag<View>("export_save") as android.widget.Switch).isChecked = true
        assertTrue(view.read().save); assertTrue(view.read().webdav)
        (view.findViewWithTag<View>("export_webdav") as android.widget.Switch).isChecked = false
        assertTrue(view.read().save); assertFalse(view.read().webdav)
        val wifi = testDescendants(view).filterIsInstance<android.widget.Switch>().single { it.text == context.getString(R.string.route_export_wifi) }
        wifi.isChecked = false; assertFalse(view.read().wifiOnly)
    }
}
