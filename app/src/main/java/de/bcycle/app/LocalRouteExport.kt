package de.bcycle.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

internal object LocalRouteExport {
    private fun prefs(c:Context)=c.getSharedPreferences("ride-local-documents",0)
    private fun remember(c:Context,id:String,uri:Uri) { prefs(c).edit().putString(id,uri.toString()).apply() }
    fun has(c:Context,id:String)=prefs(c).contains(id)
    fun update(c:Context,id:String,file:File) {
        val target=prefs(c).getString(id,null)?.let(Uri::parse) ?: return
        FitExportIdentities.remember(c.filesDir,id,file)
        c.contentResolver.openOutputStream(target,"wt")?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: error("Notiz konnte nicht in der exportierten Datei gespeichert werden")
    }
    fun write(context: Context, source: File, name: String, tree: String) {
        FitExportIdentities.remember(context.filesDir,source.nameWithoutExtension,source)
        if (tree.isEmpty()) { remember(context,source.nameWithoutExtension,DownloadFiles.write(context, source, name));return }
        val resolver = context.contentResolver
        val uri = Uri.parse(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val document = DocumentsContract.createDocument(resolver, parent, "application/vnd.ant.fit", name)
            ?: error("Zielordner ist nicht beschreibbar")
        try {
            resolver.openOutputStream(document, "wt")?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("Datei kann nicht geschrieben werden")
            remember(context,source.nameWithoutExtension,document)
        } catch (e: Exception) { runCatching { DocumentsContract.deleteDocument(resolver, document) }; throw e }
    }
}
