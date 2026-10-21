package de.bcycle.app

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Worker-only immutable transfer versions. Original route identity and source bytes survive. */
internal object RouteTransferPayload {
    private fun gpxId(id:String)=runCatching { UUID.fromString(id).toString()==id }.getOrDefault(false)
    private fun directory(c:Context,id:String)=File(File(c.filesDir,"route-transfer-payloads"),id)
    private fun gpx(c:Context,id:String):StoredGpx? {
        if(!gpxId(id)) return null
        val index=AtomicFile(File(c.filesDir,"gpx-library/index.json"))
        if(!index.baseFile.isFile) return null
        val rows=index.openRead().bufferedReader().use { JSONObject(it.readText()).getJSONArray("entries") }
        for(i in 0 until rows.length()) {
            val row=rows.getJSONObject(i)
            if(row.getString("id")==id) return StoredGpx(id,row.getString("name"),row.getInt("points"),row.getInt("segments"),
                RouteDetails.from(row.optJSONObject("details")),runCatching { TrackOrigin.valueOf(row.getString("origin")) }.getOrDefault(TrackOrigin.IMPORTED))
        }
        return null
    }
    fun eligibleGpx(c:Context,id:String)=gpx(c,id)?.origin==TrackOrigin.RECORDED
    private fun source(c:Context,id:String)=if(gpxId(id)) File(c.filesDir,"gpx-library/$id.gpx") else FitLibrary.activeFile(c.filesDir,id)
    fun file(c:Context,id:String):File {
        val original=source(c,id);require(original.isFile)
        val entry=gpx(c,id)
        val type=RideProfileColor.type(c,id) ?: entry?.details?.bikeType
        if(entry==null && (type==null || FitRead.read(original).details.bikeType==type)) return original
        require(entry==null || entry.origin==TrackOrigin.RECORDED)
        val key=MessageDigest.getInstance("SHA-256").digest((WebDavClient.hash(original)+":"+type?.name+":"+GpxActivityFit.VERSION).toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val target=File(File(directory(c,id),key),"$id.fit")
        if(target.isFile) return target
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
        val temporary=File.createTempFile("payload-",".fit",target.parentFile)
        try {
            if(entry!=null) GpxActivityFit.write(original,temporary,entry,type) else FitBikeType.write(original,temporary,checkNotNull(type))
            val atomic=AtomicFile(target);val stream=atomic.startWrite()
            try { temporary.inputStream().use { it.copyTo(stream) };atomic.finishWrite(stream) }
            catch(e:Exception) { atomic.failWrite(stream);throw e }
        } finally { temporary.delete() }
        return target
    }
    fun annotate(c:Context,file:File,row:JSONObject):JSONObject {
        val id=file.nameWithoutExtension
        runCatching { FitRead.read(file).details.bikeType }.getOrNull()?.let { row.put("sourceBikeType",it.name) }
        if(file.canonicalPath.startsWith(directory(c,id).canonicalPath+"/")) {
            val original=source(c,id)
            row.put("sourceLength",original.length()).put("sourceModified",original.lastModified()).put("sourceHash",WebDavClient.hash(original))
        }
        return row
    }
    fun matches(row:JSONObject,stamp:RouteFileStamp):Boolean {
        val converted=row.has("sourceLength")
        return row.optLong(if(converted) "sourceLength" else "length",-1)==stamp.length &&
            row.optLong(if(converted) "sourceModified" else "modified",-1)==stamp.modified &&
            (stamp.hash==null || row.optString(if(converted) "sourceHash" else "hash")==stamp.hash)
    }
    fun metadataMatches(c:Context,id:String,row:JSONObject):Boolean {
        val current=RideProfileColor.type(c,id) ?: return true
        val before=row.optString("sourceBikeType").takeIf { it.isNotEmpty() } ?: RideProfileColor.snapshotType(c,id)?.name ?: return true
        return current.name==before
    }
    fun forget(c:Context,id:String) { require(id.matches(Regex("(recorded|imported)-[a-f0-9]{64}|[a-f0-9-]{36}")));directory(c,id).deleteRecursively() }
}
