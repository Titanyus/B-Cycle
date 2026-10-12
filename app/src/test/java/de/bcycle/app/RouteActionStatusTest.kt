package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteActionStatusTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun stamp(file: File) = RouteFileStamp(file.length(), file.lastModified(), WebDavClient.hash(file))
    @Test fun localSuccessIsIndependentPersistentAndInvalidatedByContentEvenWithUnchangedSizeAndTime() {
        val file = File(context.filesDir, "local.gpx").apply { writeText("source") }
        assertFalse(RouteLocalSaves.completed(context, "id", stamp(file)))
        var writes = 0
        RouteLocalSaves.perform(context, "id", file, "Downloads/B-Cycle") { writes++ }
        assertEquals(1, writes)
        assertTrue(RouteLocalSaves.completed(context, "id", stamp(file)))
        assertTrue(RouteLocalSaves.completed(context.createConfigurationContext(context.resources.configuration), "id", stamp(file)))
        assertEquals(UploadState.NOT_UPLOADED, RouteUploads.state(context, "id", stamp(file)).state)
        val modified = file.lastModified(); file.writeText("edited"); file.setLastModified(modified)
        assertFalse(RouteLocalSaves.completed(context, "id", stamp(file)))
    }
    @Test fun failedCancelledAndChangedDuringLocalWriteNeverConfirmSuccess() {
        val file = File(context.filesDir, "failure.gpx").apply { writeText("original") }
        RouteLocalSaves.perform(context, "id", file, "Downloads/B-Cycle") {}
        assertThrows(java.io.IOException::class.java) { RouteLocalSaves.perform(context, "id", file, "Downloads/B-Cycle") { throw java.io.IOException() } }
        assertFalse(RouteLocalSaves.completed(context, "id", stamp(file)))
        var wrote = false
        assertThrows(java.io.IOException::class.java) { RouteLocalSaves.perform(context, "id", file, "Downloads/B-Cycle", { true }) { wrote = true } }
        assertFalse(wrote)
        var cancel = false
        assertThrows(java.io.IOException::class.java) { RouteLocalSaves.perform(context, "id", file, "Downloads/B-Cycle", { cancel }) { cancel = true } }
        assertFalse(RouteLocalSaves.completed(context, "id", stamp(file)))
        assertThrows(IllegalStateException::class.java) { RouteLocalSaves.perform(context, "id", file, "Downloads/B-Cycle") { file.appendText("changed") } }
        assertFalse(RouteLocalSaves.completed(context, "id", stamp(file)))
    }
    @Test fun cloudOnlyConfirmsCompletionAndCannotTurnCancelledReturnIntoSuccess() {
        RouteExportSettings(context).write(RouteExportConfig(webdav = true, server = "https://cloud.example.org", user = "rider", wifiOnly = false))
        val file = File(context.filesDir, "fit-library/recorded-${"a".repeat(64)}.fit").apply { parentFile!!.mkdirs(); writeText("original") }
        val id = file.nameWithoutExtension
        RouteUploads.enqueue(context, file, "original.fit")
        assertEquals(UploadState.WAITING, RouteUploads.state(context, id, stamp(file)).state)
        var cancel = false
        assertTrue(RouteUploads.run(context, { cancel }) { _, _, _, _ -> cancel = true })
        assertNotEquals(UploadState.UPLOADED, RouteUploads.state(context, id, stamp(file)).state)
        assertFalse(RouteUploads.run(context, { false }) { _, _, _, _ -> throw DavFailure(false, "failure") })
        assertEquals(UploadState.FAILED, RouteUploads.state(context, id, stamp(file)).state)
        RouteUploads.retry(context, id)
        assertFalse(RouteUploads.run(context, { false }) { _, _, _, _ -> })
        assertEquals(UploadState.UPLOADED, RouteUploads.state(context, id, stamp(file)).state)
        assertFalse(RouteLocalSaves.completed(context, id, stamp(file)))
        var uploads = 0
        RouteUploads.retry(context, id); RouteUploads.run(context, { false }) { _, _, _, _ -> uploads++ }
        assertEquals(0, uploads)
        val modified = file.lastModified(); file.writeText("modified"); file.setLastModified(modified)
        assertEquals(UploadState.NOT_UPLOADED, RouteUploads.state(context, id, stamp(file)).state)
    }
}
