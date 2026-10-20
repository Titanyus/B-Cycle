package de.bcycle.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.InputStream

internal data class BackupDirectoryEntry(val name:String,val directory:Boolean)
internal interface BackupDirectory {
    fun entries():List<BackupDirectoryEntry>
    fun isSnapshot()=entries().any { it.name==RideDirectoryBackup.INDEX && !it.directory }
    fun child(name:String,create:Boolean):BackupDirectory
    fun open(name:String):InputStream
    fun write(name:String,source:File)
}
internal fun backupName(name:String):String=name.also {
    require(it.matches(Regex("[A-Za-z0-9][A-Za-z0-9_.-]{0,180}")) && it!="." && it!="..")
}
internal class FileBackupDirectory(private val root:File):BackupDirectory {
    init { require(root.isDirectory) }
    private fun file(name:String)=File(root,backupName(name)).also { require(it.canonicalFile.parentFile==root.canonicalFile) }
    override fun entries()=root.listFiles()?.map { BackupDirectoryEntry(it.name,it.isDirectory) } ?: error("Verzeichnis nicht lesbar")
    override fun child(name:String,create:Boolean):BackupDirectory {
        val path=file(name);if(create) require(path.mkdir()) { "Sicherungsverzeichnis existiert bereits" }
        return FileBackupDirectory(path)
    }
    override fun open(name:String)=file(name).inputStream()
    override fun write(name:String,source:File) {
        val target=file(name);require(!target.exists()) { "Datei existiert bereits" }
        val temporary=File.createTempFile("backup-",".tmp",root)
        try {
            source.inputStream().use { input -> temporary.outputStream().use { input.copyTo(it) } }
            require(WebDavClient.hash(temporary)==WebDavClient.hash(source))
            java.nio.file.Files.move(temporary.toPath(),target.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        } finally { temporary.delete() }
    }
}
internal class DocumentBackupDirectory(private val context:Context,private val tree:Uri,private val document:Uri=
    DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree))):BackupDirectory {
    private val resolver get()=context.contentResolver
    private var cachedChildren:List<Pair<BackupDirectoryEntry,Uri>>?=null
    private fun children():List<Pair<BackupDirectoryEntry,Uri>> {
        cachedChildren?.let { return it }
        val children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(document))
        return resolver.query(children,arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE),null as android.os.Bundle?,null)?.use { cursor ->
            buildList {
                while(cursor.moveToNext()) {
                    require(size<100000) { "Zu viele Dateien" }
                    add(BackupDirectoryEntry(cursor.getString(1),cursor.getString(2)==DocumentsContract.Document.MIME_TYPE_DIR) to
                        DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0)))
                }
            }
        }?.also { cachedChildren=it } ?: error("Verzeichnis nicht lesbar")
    }
    override fun entries()=children().map { it.first }
    override fun child(name:String,create:Boolean):BackupDirectory {
        backupName(name)
        val existing=children().singleOrNull { it.first.name==name }
        val target=if(create) {
            require(existing==null) { "Sicherungsverzeichnis existiert bereits" }
            DocumentsContract.createDocument(resolver,document,DocumentsContract.Document.MIME_TYPE_DIR,name) ?: error("Ordner nicht angelegt")
        } else existing?.takeIf { it.first.directory }?.second ?: error("Ordner fehlt")
        if(create) cachedChildren=children()+ (BackupDirectoryEntry(name,true) to target)
        return DocumentBackupDirectory(context,tree,target).apply { if(create) cachedChildren=emptyList() }
    }
    override fun open(name:String):InputStream {
        backupName(name)
        val uri=children().single { it.first.name==name && !it.first.directory }.second
        return resolver.openInputStream(uri) ?: error("Datei nicht lesbar")
    }
    override fun write(name:String,source:File) {
        backupName(name);require(children().none { it.first.name==name }) { "Datei existiert bereits" }
        val mime=when(name.substringAfterLast('.')) { "fit"->"application/vnd.ant.fit";"json"->"application/json";"gpx"->"application/gpx+xml";"webp"->"image/webp";else->"application/octet-stream" }
        val uri=DocumentsContract.createDocument(resolver,document,mime,name) ?: error("Datei nicht angelegt")
        try {
            resolver.openOutputStream(uri,"wt")?.use { out -> source.inputStream().use { it.copyTo(out) } } ?: error("Datei nicht schreibbar")
            val actual=resolver.openInputStream(uri)?.use { input ->
                val digest=java.security.MessageDigest.getInstance("SHA-256")
                val buffer=ByteArray(16384);var size=0L
                while(true) { val n=input.read(buffer);if(n<0) break;size+=n;require(size<=source.length());digest.update(buffer,0,n) }
                require(size==source.length());digest.digest().joinToString("") { "%02x".format(it) }
            }
            require(actual==WebDavClient.hash(source)) { "Sicherungsdatei konnte nicht geprüft werden" }
            cachedChildren=children()+ (BackupDirectoryEntry(name,false) to uri)
        } catch(e:Exception) { runCatching { DocumentsContract.deleteDocument(resolver,uri) };throw e }
    }
}
internal class DavBackupDirectory(private val client:WebDavClient):BackupDirectory {
    override fun entries()=client.listBackupDirectory()
    override fun isSnapshot()=client.backupIndexPresent()
    override fun child(name:String,create:Boolean)=DavBackupDirectory(client.backupChild(backupName(name),create))
    override fun open(name:String)=client.openBackupFile(backupName(name))
    override fun write(name:String,source:File)=client.upload(source,backupName(name))
}
