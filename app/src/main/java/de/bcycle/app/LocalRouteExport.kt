package de.bcycle.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

internal object LocalRouteExport {
    fun write(context: Context, source: File, name: String, tree: String) {
        if (tree.isEmpty()) { DownloadFiles.write(context, source, name); return }
        val resolver = context.contentResolver
        val uri = Uri.parse(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val document = DocumentsContract.createDocument(resolver, parent, "application/vnd.ant.fit", name)
            ?: error("Zielordner ist nicht beschreibbar")
        try {
            resolver.openOutputStream(document, "wt")?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("Datei kann nicht geschrieben werden")
        } catch (e: Exception) { runCatching { DocumentsContract.deleteDocument(resolver, document) }; throw e }
    }
}
