package de.bcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

internal data class RideBackupResult(val completed:Int=0,val duplicates:Int=0,val failed:Int=0,val folder:String="",val hrvCompleted:Int=0,val hrvDuplicates:Int=0,val hrvFailed:Int=0)

/** Library-worker operation. Checked data first, metadata last; never overwrite a destination. */
internal class RideDirectoryBackup(private val context:Context,private val fits:FitLibrary,
    private val gpx:GpxLibrary,private val summaries:DreeveSummaryLibrary) {
    companion object {
        const val INDEX="bicycle-backup.json"
        const val MAX_JSON=4*1024*1024
        private val rideId=Regex("(recorded|imported)-[a-f0-9]{64}|[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")
    }
    private fun checkStop() { if(Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException() }
    private fun descriptor(name:String,file:File)=JSONObject().put("name",backupName(name)).put("bytes",file.length()).put("sha256",WebDavClient.hash(file))
    private fun jsonFile(root:File,name:String,json:JSONObject)=File(root,name).apply { writeText(json.toString(),Charsets.UTF_8) }
    private fun send(directory:BackupDirectory,name:String,source:File):JSONObject {
        checkStop();require(source.isFile && source.length() in 1..FitRead.MAX_BYTES)
        directory.write(name,source);return descriptor(name,source)
    }
    fun export(parent:BackupDirectory,progress:(Int)->Unit={}):RideBackupResult {
        val folder="Bicycle-Backup-"+Instant.now().toString().replace(':','-').replace('.','-')+"-"+UUID.randomUUID().toString().take(8)
        val directory=parent.child(folder,true)
        val temporary=java.nio.file.Files.createTempDirectory(context.cacheDir.toPath(),"ride-backup-").toFile()
        val entries=JSONArray();val failed=JSONArray()
        try {
            val settings=File(temporary,"settings.json").apply { writeBytes(SettingsBackup(context,ThemeManager(context.getSharedPreferences("bike",0))).export(BackupSection.entries.toSet())) }
            val settingsInfo=send(directory,"settings.json",settings)
            val names=RideNames.allNames(context)
            fun row(id:String,name:String,kind:String,origin:TrackOrigin,details:RouteDetails,files:()->JSONObject) {
                checkStop()
                try {
                    require(rideId.matches(id))
                    val payloads=files()
                    val links=JSONArray(RideServiceLinks.links(context,id).map { link -> JSONObject().put("service",link.service.name)
                        .put("account",link.accountId).put("activity",link.activityId).put("hash",link.fileHash) })
                    val json=JSONObject().put("format","bicycle-ride").put("schema",1).put("id",id).put("kind",kind)
                        .put("name",names[id] ?: name).put("origin",origin.name)
                        .put("details",details.copy(bikeType=RideProfileColor.type(context,id) ?: details.bikeType).json())
                        .put("bike",RideProfileColor.backup(context,id) ?: JSONObject.NULL).put("links",links).put("files",payloads)
                    val metadata=jsonFile(temporary,"$id.ride.json",json);require(metadata.length()<=65536)
                    entries.put(send(directory,metadata.name,metadata))
                } catch(e:Exception) {
                    if(e is java.io.InterruptedIOException || Thread.currentThread().isInterrupted) throw e
                    failed.put(id)
                }
                progress(entries.length()+failed.length())
            }
            fits.entries.forEach { entry -> row(entry.id,entry.name,"FIT",entry.origin,entry.details) {
                val source=fits.analysisFile(entry.id);val type=RideProfileColor.type(context,entry.id)
                val full=if(type!=null && FitRead.read(source).details.bikeType!=type) File(temporary,"${entry.id}.typed.fit").also { FitBikeType.write(source,it,type) } else source
                JSONObject().put("full",send(directory,"${entry.id}.fit",full))
                    .put("original",send(directory,"${entry.id}.original.fit",fits.originalFile(entry.id))).apply {
                        if(fits.corrected(entry.id)) put("corrected",send(directory,"${entry.id}.corrected.fit",fits.file(entry.id)))
                    }
            } }
            gpx.entries.forEach { entry -> row(entry.id,entry.name,"GPX",entry.origin,entry.details) {
                val fit=File(temporary,"${entry.id}.fit");GpxFitBackup.write(gpx.file(entry.id),fit,
                    entry.copy(details=entry.details.copy(bikeType=RideProfileColor.type(context,entry.id) ?: entry.details.bikeType)))
                JSONObject().put("gpx",send(directory,"${entry.id}.gpx",gpx.file(entry.id))).put("full",send(directory,fit.name,fit))
            } }
            summaries.entries.forEach { entry -> row(entry.id,entry.activity.name,"SUMMARY",entry.origin,entry.details) {
                JSONObject().put("summary",send(directory,"${entry.id}.summary.json",summaries.file(entry.id)))
            } }
            val photos=JSONArray()
            BikeProfileStore(context,context.getSharedPreferences("bike",0)).all().forEach { profile ->
                val photo=BikePhotos.file(context,profile.slot)
                if(photo.isFile) {
                    val id=BikeIdentities.id(context.getSharedPreferences("bike",0),profile.slot)
                    photos.put(JSONObject().put("bikeId",id).put("file",send(directory,"bike-$id.webp",photo)))
                }
            }
            val hrv=JSONArray();val hrvFailed=JSONArray()
            val hrvStore=HrvStore(File(context.filesDir,"hrv-measurements"))
            hrvStore.ids().forEach { id ->
                checkStop()
                try {
                    val snapshot=File(temporary,"hrv-$id.json").apply { writeBytes(hrvStore.backup(id)) }
                    hrv.put(send(directory,snapshot.name,snapshot))
                } catch(e:Exception) {
                    if(e is java.io.InterruptedIOException || Thread.currentThread().isInterrupted) throw e
                    hrvFailed.put(id)
                }
            }
            val index=JSONObject().put("format","bicycle-ride-backup").put("schema",1).put("createdAt",Instant.now().toString())
                .put("settings",settingsInfo).put("rides",entries).put("failed",failed).put("photos",photos)
                .put("hrv",hrv).put("hrvFailed",hrvFailed)
            val file=jsonFile(temporary,INDEX,index);require(file.length()<=MAX_JSON);send(directory,INDEX,file)
            return RideBackupResult(entries.length(),failed=failed.length(),folder=folder,hrvCompleted=hrv.length(),hrvFailed=hrvFailed.length())
        } finally { temporary.deleteRecursively() }
    }
    private fun fetch(directory:BackupDirectory,info:JSONObject,target:File,max:Long):File {
        checkStop()
        val name=backupName(info.getString("name"));val expected=info.getLong("bytes")
        require(expected in 1..max && info.getString("sha256").matches(Regex("[a-f0-9]{64}")))
        var total=0L
        directory.open(name).use { input -> target.outputStream().use { out ->
            val buffer=ByteArray(16384)
            while(true) { checkStop();val n=input.read(buffer);if(n<0) break;total+=n;require(total<=expected);out.write(buffer,0,n) }
        } }
        require(total==expected && WebDavClient.hash(target)==info.getString("sha256")) { "Prüfsumme stimmt nicht" }
        return target
    }
    private fun index(directory:BackupDirectory):JSONObject=directory.open(INDEX).use { input ->
        val bytes=input.readNBytes(MAX_JSON+1);require(bytes.size<=MAX_JSON)
        BackupJson.objectValue(bytes,MAX_JSON).also {
            require(it.getString("format")=="bicycle-ride-backup" && it.getInt("schema")==1)
            require(it.getJSONArray("rides").length()<=20000)
        }
    }
    fun settings(directory:BackupDirectory):SettingsBackupDocument {
        val temporary=File.createTempFile("backup-settings-",".json",context.cacheDir)
        try {
            fetch(directory,index(directory).getJSONObject("settings"),temporary,SettingsBackup.MAX_BYTES.toLong())
            return SettingsBackup(context,ThemeManager(context.getSharedPreferences("bike",0))).decode(temporary.readBytes())
        } finally { temporary.delete() }
    }
    fun restore(directory:BackupDirectory,progress:(Int)->Unit={}):RideBackupResult {
        val index=index(directory);val rows=index.getJSONArray("rides")
        val missing=index.optJSONArray("failed") ?: JSONArray();require(missing.length()<=20000)
        val seen=mutableSetOf<String>();var done=0;var duplicates=0;var failed=missing.length()
        repeat(rows.length()) { position ->
            checkStop()
            val temporary=java.nio.file.Files.createTempDirectory(context.cacheDir.toPath(),"ride-restore-").toFile()
            var createdId:String?=null;var kind=""
            try {
                val metadata=fetch(directory,rows.getJSONObject(position),File(temporary,"metadata.json"),65536)
                val row=BackupJson.objectValue(metadata.readBytes(),65536);val id=row.getString("id")
                require(row.getString("format")=="bicycle-ride" && row.getInt("schema")==1 && rideId.matches(id) && seen.add(id))
                val name=row.getString("name");require(name.isNotBlank() && name.length<=120 && name.none(Char::isISOControl))
                val origin=TrackOrigin.valueOf(row.getString("origin"));kind=row.getString("kind")
                val details=RouteDetails.from(row.getJSONObject("details"));require(details.dateMs>=0)
                val bike=row.optJSONObject("bike")
                bike?.let {
                    require(BikeIdentities.valid(it.getString("bikeId")) && it.getInt("slot")>=0)
                    it.getInt("primary");require(it.optString("name").let { s -> s.length<=120 && s.none(Char::isISOControl) })
                }
                val links=row.getJSONArray("links");require(links.length()<=50)
                val restoredLinks=(0 until links.length()).map { i ->
                    val link=links.getJSONObject(i);val hash=link.getString("hash");require(hash.matches(Regex("[a-f0-9]{64}")))
                    val account=link.getString("account");val activity=link.getString("activity")
                    require(listOf(account,activity).all { it.isNotBlank() && it.length<=512 && it.none(Char::isISOControl) })
                    val service=RideService.valueOf(link.getString("service"))
                    require(RideServiceLinks.forAccount(context,service,account).none { it.activityId==activity && it.rideId!=id })
                    RideServiceLink(id,service,account,activity,hash)
                }
                val files=row.getJSONObject("files")
                fun get(role:String,extension:String)=fetch(directory,files.getJSONObject(role),File(temporary,"$id$extension"),FitRead.MAX_BYTES)
                val restored=when(kind) {
                    "FIT" -> {
                        val original=get("original",".original.fit");val full=get("full",".fit")
                        val corrected=if(files.has("corrected")) get("corrected",".corrected.fit") else null
                        fits.restoreBackup(id,name,origin,details,original,full,corrected)
                    }
                    "GPX" -> {
                        FitRead.read(get("full",".fit"))
                        gpx.restoreBackup(id,name,origin,details,get("gpx",".gpx"))
                    }
                    "SUMMARY" -> {
                        val file=get("summary",".json");BackupJson.objectValue(file.readBytes(),65536)
                        summaries.restoreBackup(DreeveSummaryLibrary.read(file).also { require(it.id==id && it.origin==origin) })
                    }
                    else -> error("Unbekanntes Fahrtenformat")
                }
                if(restored) {
                    createdId=id;bike?.let { RideProfileColor.confirm(context,id,it.toString()) }
                    restoredLinks.forEach { RideServiceLinks.confirm(context,it) };RideNames.imported(context,id,name)
                    done++;createdId=null
                } else duplicates++
            } catch(e:Exception) {
                createdId?.let { id ->
                    when(kind) { "FIT"->{ fits.delete(id);RideAnalysisFiles.file(context.filesDir,id).delete() }; "GPX"->gpx.delete(id); "SUMMARY"->summaries.delete(id) }
                    RideProfileColor.forget(context,id);RideServiceLinks.forget(context,id);RideNames.forget(context,id)
                }
                if(e is java.io.InterruptedIOException || Thread.currentThread().isInterrupted) throw e
                failed++
            } finally { temporary.deleteRecursively();progress(position+1) }
        }
        var hrvDone=0;var hrvDuplicates=0;var hrvFailed=index.optJSONArray("hrvFailed")?.length() ?: 0
        val hrvRows=index.optJSONArray("hrv") ?: JSONArray();require(hrvRows.length()<=20000 && hrvFailed<=20000)
        val hrvStore=HrvStore(File(context.filesDir,"hrv-measurements"))
        repeat(hrvRows.length()) { i ->
            checkStop();val temporary=File.createTempFile("hrv-restore-",".json",context.cacheDir)
            try {
                fetch(directory,hrvRows.getJSONObject(i),temporary,HrvStore.MAX_BYTES.toLong())
                if(hrvStore.restore(temporary.readBytes())) hrvDone++ else hrvDuplicates++
            } catch(e:Exception) {
                if(e is java.io.InterruptedIOException || Thread.currentThread().isInterrupted) throw e
                hrvFailed++
            } finally { temporary.delete() }
        }
        val photos=index.optJSONArray("photos") ?: JSONArray();require(photos.length()<=BikeProfileStore.MAX_COUNT)
        repeat(photos.length()) { i ->
            val row=photos.getJSONObject(i);val id=row.getString("bikeId");require(BikeIdentities.valid(id))
            val photo=File(context.filesDir,"bike-photos/by-id-$id.webp")
            if(!photo.isFile) {
                val temporary=File.createTempFile("backup-photo-",".webp",context.cacheDir)
                try {
                    fetch(directory,row.getJSONObject("file"),temporary,25L*1024*1024)
                    val options=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true }
                    android.graphics.BitmapFactory.decodeFile(temporary.path,options)
                    require(options.outWidth in 1..1200 && options.outHeight in 1..1200)
                    photo.parentFile!!.mkdirs();val atomic=android.util.AtomicFile(photo);val out=atomic.startWrite()
                    try { temporary.inputStream().use { it.copyTo(out) };atomic.finishWrite(out) }
                    catch(e:Exception) { atomic.failWrite(out);throw e }
                } finally { temporary.delete() }
            }
        }
        return RideBackupResult(done,duplicates,failed,hrvCompleted=hrvDone,hrvDuplicates=hrvDuplicates,hrvFailed=hrvFailed)
    }
}
