package de.bcycle.app

import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalRouteExportTest {
    @Test fun selectedDocumentTreeReceivesOriginalBytesAndFailureDeletesPartialFile() {
        val context = RuntimeEnvironment.getApplication()
        val provider = Folder(context.cacheDir).apply { attachInfo(context, ProviderInfo().apply {
            authority = "bcycle.test.folder"; exported = true; grantUriPermissions = true
            readPermission = "android.permission.MANAGE_DOCUMENTS"; writePermission = "android.permission.MANAGE_DOCUMENTS"
        }) }
        ShadowContentResolver.registerProviderInternal("bcycle.test.folder", provider)
        val source = File(context.cacheDir, "source.fit").apply { writeBytes(byteArrayOf(0, 1, 2, 99)) }
        val tree = DocumentsContract.buildTreeDocumentUri("bcycle.test.folder", "root").toString()
        LocalRouteExport.write(context, source, "B-Cycle-test.fit", tree)
        assertArrayEquals(source.readBytes(), provider.files.values.single().readBytes())
        assertEquals("B-Cycle-test.fit", provider.names.values.single())
        provider.fail = true
        assertThrows(IOException::class.java) { LocalRouteExport.write(context, source, "B-Cycle-error.fit", tree) }
        assertEquals(1, provider.files.size)
        assertArrayEquals(byteArrayOf(0, 1, 2, 99), source.readBytes())
    }
    private class Folder(private val root: File) : DocumentsProvider() {
        val files = linkedMapOf<String, File>(); val names = linkedMapOf<String, String>(); var fail = false
        override fun onCreate() = true
        override fun queryRoots(projection: Array<out String>?) = MatrixCursor(arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID, DocumentsContract.Root.COLUMN_DOCUMENT_ID))
        override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = MatrixCursor(arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID)).apply { addRow(arrayOf(documentId)) }
        override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor = MatrixCursor(arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
        override fun isChildDocument(parentDocumentId: String, documentId: String) = parentDocumentId == "root" && files.containsKey(documentId)
        override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
            assertEquals("root", parentDocumentId); assertEquals("application/vnd.ant.fit", mimeType)
            val id = java.util.UUID.randomUUID().toString(); files[id] = File(root, id); names[id] = displayName; return id
        }
        override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
            if (fail) throw IOException("test failure")
            return ParcelFileDescriptor.open(files.getValue(documentId), ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_READ_WRITE)
        }
        override fun deleteDocument(documentId: String) { files.remove(documentId)?.delete(); names.remove(documentId) }
    }
}
