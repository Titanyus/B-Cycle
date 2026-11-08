package de.bcycle.app

import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

/** Exact hashes of our generated FIT copies, linked to an existing immutable original. */
internal object FitExportIdentities {
    private const val MAX_BYTES=8*1024*1024
    private val validId=Regex("(recorded|imported)-[a-f0-9]{64}")
    private val validHash=Regex("[a-f0-9]{64}")
    private data class Cached(val length:Long,val modified:Long,val owners:Map<String,String>)
    private val cache=object:LinkedHashMap<String,Cached>(4,.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,Cached>?)=size>4
    }
    private fun file(root:File)=AtomicFile(File(root,"fit-export-identities.json"))
    private fun read(root:File):Map<String,String> {
        val atomic=file(root);val base=atomic.baseFile;val path=base.absolutePath
        val backup=File(base.path+".bak").exists()
        cache[path]?.takeIf { !backup && it.length==base.length() && it.modified==base.lastModified() }?.let { return it.owners }
        if(!base.exists() && !backup) return emptyMap()
        val bytes=atomic.openRead().use { it.readNBytes(MAX_BYTES+1) };require(bytes.size<=MAX_BYTES)
        val data=JSONObject(bytes.toString(Charsets.UTF_8));require(data.getInt("schema")==1)
        val rows=data.getJSONObject("owners")
        val owners=rows.keys().asSequence().associateWith { key ->
            require(validHash.matches(key));rows.getString(key).also { require(validId.matches(it)) }
        }
        cache[path]=Cached(base.length(),base.lastModified(),owners)
        return owners
    }
    @Synchronized fun owner(root:File,hash:String):String?=read(root)[hash]
    /** Persist before publishing a Downloads row or replacing an already exported document. */
    @Synchronized fun remember(root:File,id:String,source:File) {
        if(!validId.matches(id) || !source.extension.equals("fit",true)) return
        if(!File(root,"fit-library/$id.fit").isFile) return
        val hash=WebDavClient.hash(source)
        if(id.endsWith(hash)) return // Byte-identical originals already deduplicate normally.
        val before=read(root)
        if(before[hash]==id) return
        // Never overwrite another established identity on an ambiguous identical export.
        if(before.containsKey(hash)) return
        val owners=before+(hash to id)
        val data=JSONObject().put("schema",1).put("owners",JSONObject(owners)).toString().toByteArray()
        require(data.size<=MAX_BYTES)
        val atomic=file(root);val out=atomic.startWrite()
        try { out.write(data);atomic.finishWrite(out) }
        catch(e:Exception) { atomic.failWrite(out);throw e }
        cache[atomic.baseFile.absolutePath]=Cached(atomic.baseFile.length(),atomic.baseFile.lastModified(),owners)
    }
}
