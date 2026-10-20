package de.bcycle.app

import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DocumentBackupDirectoryTest {
    @Test fun documentTreeCreatesAndReadsSnapshotWithoutRepeatedFullFolderScansAndCleansFailedWrites() {
        val c=RuntimeEnvironment.getApplication()
        val provider=Folder(File(c.cacheDir,"provider").apply { mkdirs() }).apply { attachInfo(c,ProviderInfo().apply {
            authority="bcycle.backup.test";exported=true;grantUriPermissions=true
            readPermission="android.permission.MANAGE_DOCUMENTS";writePermission="android.permission.MANAGE_DOCUMENTS"
        }) }
        ShadowContentResolver.registerProviderInternal("bcycle.backup.test",provider)
        val tree=DocumentsContract.buildTreeDocumentUri("bcycle.backup.test","root")
        val directory=DocumentBackupDirectory(c,tree).child("Bicycle-Backup-test",true)
        val source=File(c.cacheDir,"data.fit").apply { writeBytes(byteArrayOf(0,1,2,3,4)) }
        repeat(20) { directory.write("ride-$it.fit",source) }
        assertTrue(provider.queries<=2)
        val reopened=DocumentBackupDirectory(c,tree).child("Bicycle-Backup-test",false)
        assertEquals(20,reopened.entries().size)
        assertArrayEquals(source.readBytes(),reopened.open("ride-19.fit").use { it.readBytes() })
        assertThrows(IllegalArgumentException::class.java) { directory.write("ride-19.fit",source) }
        provider.failWrites=true
        assertThrows(IOException::class.java) { directory.write("failed.fit",source) }
        assertFalse(provider.rows.values.any { it.name=="failed.fit" })
        assertArrayEquals(byteArrayOf(0,1,2,3,4),source.readBytes())
    }
    private class Folder(private val root:File):DocumentsProvider() {
        data class Row(val parent:String?,val name:String,val mime:String,val file:File)
        val rows=linkedMapOf("root" to Row(null,"root",DocumentsContract.Document.MIME_TYPE_DIR,root))
        var queries=0;var failWrites=false
        override fun onCreate()=true
        override fun queryRoots(projection:Array<out String>?)=MatrixCursor(arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID,DocumentsContract.Root.COLUMN_DOCUMENT_ID))
        private fun cursor(ids:List<String>,projection:Array<out String>?):Cursor {
            val columns=projection ?: arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE)
            return MatrixCursor(columns).apply { ids.forEach { id ->
                val row=rows.getValue(id)
                addRow(columns.map { col -> when(col) {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID->id
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME->row.name
                    DocumentsContract.Document.COLUMN_MIME_TYPE->row.mime
                    else->null
                } }.toTypedArray())
            } }
        }
        override fun queryDocument(documentId:String,projection:Array<out String>?)=cursor(listOf(documentId),projection)
        override fun queryChildDocuments(parentDocumentId:String,projection:Array<out String>?,sortOrder:String?):Cursor {
            queries++;return cursor(rows.filterValues { it.parent==parentDocumentId }.keys.toList(),projection)
        }
        override fun isChildDocument(parentDocumentId:String,documentId:String):Boolean {
            var parent=rows[documentId]?.parent
            while(parent!=null) { if(parent==parentDocumentId) return true;parent=rows[parent]?.parent }
            return false
        }
        override fun createDocument(parentDocumentId:String,mimeType:String,displayName:String):String {
            require(rows.values.none { it.parent==parentDocumentId && it.name==displayName })
            val id=UUID.randomUUID().toString()
            rows[id]=Row(parentDocumentId,displayName,mimeType,File(root,id))
            return id
        }
        override fun openDocument(documentId:String,mode:String,signal:CancellationSignal?):ParcelFileDescriptor {
            if(failWrites && mode.contains('w')) throw IOException("write failed")
            return ParcelFileDescriptor.open(rows.getValue(documentId).file,ParcelFileDescriptor.parseMode(mode))
        }
        override fun deleteDocument(documentId:String) { rows.remove(documentId)?.file?.delete() }
    }
}
