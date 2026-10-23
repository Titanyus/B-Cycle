package de.bcycle.app

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.SecretKey

internal class SettingsPasswordRequired(val payload:ByteArray):Exception()

/** Standard WinZip AES-256 ZIP, one encrypted JSON entry. No extraction paths or plaintext temp files. */
internal class SettingsArchive(private val context:Context,private val keys:((String)->SecretKey)?=null) {
    companion object {
        const val MAX_BYTES=32*1024*1024
        const val MAX_PHOTO=4*1024*1024
        fun isZip(bytes:ByteArray)=bytes.size>=4 && bytes[0]==0x50.toByte() && bytes[1]==0x4b.toByte() && bytes[2]==3.toByte() && bytes[3]==4.toByte()
    }
    private fun backup()=SettingsBackup(context,ThemeManager(context.getSharedPreferences("bike",0)),keys)
    fun export(selected:Set<BackupSection>,password:CharArray):ByteArray {
        require(password.size in 12..1024)
        val prefs=context.getSharedPreferences("bike",0)
        val profiles=if(BackupSection.PROFILE in selected) BikeProfileStore(context,prefs,initialize=!prefs.contains(BikeProfileStore.KEY)).all() else emptyList()
        val settings=JSONObject(String(backup().export(selected),Charsets.UTF_8))
        val photos=JSONObject()
        if(BackupSection.PROFILE in selected) {
            profiles.forEach { profile ->
                val file=BikePhotos.file(context,profile.slot)
                if(file.isFile) {
                    require(file.length() in 1..MAX_PHOTO.toLong());val bytes=file.readBytes();validatePhoto(bytes)
                    photos.put(BikeIdentities.id(prefs,profile.slot),Base64.encodeToString(bytes,Base64.NO_WRAP))
                }
            }
        }
        val payload=JSONObject().put("format","bicycle-settings-package").put("schema",1).put("settings",settings)
            .put("credentials",SettingsPrivateData(context,keys).export(selected)).put("photos",photos).toString().toByteArray(Charsets.UTF_8)
        require(payload.size<=MAX_BYTES)
        try {
            val bytes=ByteArrayOutputStream()
            ZipOutputStream(bytes,password).use { zip ->
                zip.putNextEntry(ZipParameters().apply { fileNameInZip="settings.json";isEncryptFiles=true;encryptionMethod=EncryptionMethod.AES;aesKeyStrength=AesKeyStrength.KEY_STRENGTH_256 })
                zip.write(payload);zip.closeEntry()
            }
            return bytes.toByteArray().also { require(it.size<=MAX_BYTES) }
        } finally { payload.fill(0) }
    }
    fun read(bytes:ByteArray,password:CharArray?=null):SettingsBackupDocument {
        if(bytes.size>MAX_BYTES) throw SettingsBackupFailure(BackupProblem.TOO_LARGE)
        if(!isZip(bytes)) return backup().decode(bytes)
        if(password==null) throw SettingsPasswordRequired(bytes)
        val raw=try {
            ZipInputStream(ByteArrayInputStream(bytes),password).use { zip ->
                val entry=zip.nextEntry ?: throw SettingsBackupFailure(BackupProblem.FORMAT)
                require(entry.fileName=="settings.json" && entry.isEncrypted && entry.encryptionMethod==EncryptionMethod.AES && entry.aesExtraDataRecord.aesKeyStrength==AesKeyStrength.KEY_STRENGTH_256)
                val content=zip.readNBytes(MAX_BYTES+1);require(content.size<=MAX_BYTES && zip.nextEntry==null);content
            }
        } catch(e:SettingsBackupFailure) { throw e } catch(_:Exception) { throw SettingsBackupFailure(BackupProblem.PASSWORD) }
        try {
            val json=BackupJson.objectValue(raw,MAX_BYTES)
            require(json.getString("format")=="bicycle-settings-package" && json.getInt("schema")==1)
            require(json.keys().asSequence().all { it in setOf("format","schema","settings","credentials","photos") })
            val public=backup().decode(json.getJSONObject("settings").toString().toByteArray(Charsets.UTF_8))
            val credentials=json.getJSONObject("credentials");SettingsPrivateData(context,keys).validate(credentials,public.sections.keys)
            val imageJson=json.getJSONObject("photos");require(imageJson.length()<=BikeProfileStore.MAX_COUNT)
            val photos=imageJson.keys().asSequence().associateWith { id ->
                require(BikeIdentities.valid(id));Base64.decode(imageJson.getString(id),Base64.NO_WRAP).also(::validatePhoto)
            }
            return public.copy(protectedData=ProtectedSettingsData(credentials,photos))
        } catch(e:SettingsBackupFailure) { throw e } catch(_:Exception) { throw SettingsBackupFailure(BackupProblem.INVALID) }
        finally { raw.fill(0) }
    }
    private fun validatePhoto(bytes:ByteArray) {
        require(bytes.size in 1..MAX_PHOTO)
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        require(bounds.outWidth in 1..1200 && bounds.outHeight in 1..1200 && bounds.outMimeType=="image/webp")
    }
}
