package de.bcycle.app

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.model.enums.AesKeyStrength

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsArchiveTest {
    private class Device(base:Context):ContextWrapper(base) {
        private val prefix=UUID.randomUUID().toString()
        private val directory=File(base.cacheDir,prefix).apply { mkdirs() }
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("$prefix-$name",mode)
        override fun getFilesDir()=File(directory,"files").apply { mkdirs() }
        override fun getCacheDir()=File(directory,"cache").apply { mkdirs() }
    }
    private val base get()=RuntimeEnvironment.getApplication()
    private val sourceKey:SecretKey=SecretKeySpec(ByteArray(32) { 7 },"AES")
    private val targetKey:SecretKey=SecretKeySpec(ByteArray(32) { 19 },"AES")
    private val password="Synthetische Testphrase 2026!".toCharArray()
    private val webdav="EXAMPLE-WEBDAV-PASSWORD"
    private val intervals="EXAMPLE-INTERVALS-KEY"
    private val token="drv_"+"a".repeat(64)
    private fun configure(c:Context) {
        val prefs=c.getSharedPreferences("bike",0);val bikes=BikeProfileStore(c,prefs)
        bikes.save(bikes.current().copy(name="Erstes Rad",type=BikeType.INDOOR,trainerAddress="00:11:22:33:44:55",recording=RecordingConfig(metadata=setOf(FitMetadataField.BIKE_NAME))))
        prefs.edit().putInt("mapZoom",18).putFloat("mapLatitude",48.25f).putFloat("mapLongitude",10.75f)
            .putBoolean("mapNight",true).putString(RideNamingSettings.KEY,RideNaming("Testfahrt").json().toString()).commit()
        bikes.ui.save(bikes.activeSlot)
        val second=bikes.add()!!;bikes.select(second.slot)
        bikes.save(bikes.current().copy(name="Zweites Rad",heartAddress="00:11:22:33:44:66",recording=RecordingConfig(metadata=emptySet())))
        prefs.edit().putInt("mapZoom",13).putFloat("mapLatitude",49.5f).putFloat("mapLongitude",11.5f).commit()
        bikes.ui.save(bikes.activeSlot);bikes.select(0)
        RiderProfileStore(prefs).write(RiderProfile(birthDate=LocalDate.of(1975,1,1),sex=RiderSex.FEMALE,heightCm=175,weightKg=70.0,restingHeartRate=50,maximumHeartRate=185,ftpW=220))
        HrvSettings(c).write(HrvMeasurementConfig(45,240,true,true,true))
        HrvQuestionSettings(c).write(HrvQuestionConfig(setOf(HrvQuestion.SLEEP,HrvQuestion.SLEEP_DURATION,HrvQuestion.NOTES),listOf("Testfrage","","")))
        c.getSharedPreferences("message-notifications",0).edit().putBoolean("enabled",true).putBoolean("messengerOnly",false)
            .putStringSet("packages",setOf("org.example.test")).putString("position","TOP").putInt("durationSeconds",60).commit()
        c.getSharedPreferences("ride-backup",0).edit().putBoolean("enabled",true).putString("target","WEBDAV").commit()
        RouteExportSettings(c) { sourceKey }.write(RouteExportConfig(true,"content://example.documents/tree/bcycle",true,"https://cloud.example.org","user","Rides",false),webdav)
        IntervalsSettings(c) { sourceKey }.write(IntervalsConfig(true,"i123",false),intervals)
        ActivityServiceSettings(c,ActivityService.DREEVE) { sourceKey }.write(ActivityServiceConfig(true,false,"https://dreeve.example.org"),token)
        val strava=ActivityServiceSettings(c,ActivityService.STRAVA) { sourceKey }
        strava.write(ActivityServiceConfig(clientId="1234"),"EXAMPLE-CLIENT-SECRET")
        strava.tokens("1234","EXAMPLE-CLIENT-SECRET",JSONObject().put("access_token","EXAMPLE-ACCESS-TOKEN").put("refresh_token","EXAMPLE-REFRESH-TOKEN")
            .put("expires_at",System.currentTimeMillis()/1000+3600).put("athlete",JSONObject().put("id",456)),true)
        strava.write(strava.read().copy(enabled=true,wifiOnly=false))
        MapProviderSettings.write(c,"0123456789abcdef0123456789abcdef")
        val image=Bitmap.createBitmap(20,12,Bitmap.Config.ARGB_8888)
        val photo=BikePhotos.file(c,0);photo.parentFile!!.mkdirs();photo.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.WEBP_LOSSY,85,it)) };image.recycle()
    }
    @Test fun aesZipRestoresAllConfiguredSettingsPhotosAndSecretsUnderANewDeviceKey() {
        val source=Device(base);configure(source)
        val bytes=SettingsArchive(source) { sourceKey }.export(BackupSection.entries.toSet(),password)
        assertTrue(SettingsArchive.isZip(bytes))
        val raw=String(bytes,Charsets.ISO_8859_1)
        for(secret in listOf(webdav,intervals,token,"EXAMPLE-REFRESH-TOKEN","0123456789abcdef0123456789abcdef","Erstes Rad")) assertFalse(raw.contains(secret))
        val file=File(source.cacheDir,"settings.zip").apply { writeBytes(bytes) }
        ZipFile(file).use { zip -> val header=zip.fileHeaders.single();assertEquals("settings.json",header.fileName);assertTrue(header.isEncrypted)
            assertEquals(EncryptionMethod.AES,header.encryptionMethod);assertEquals(AesKeyStrength.KEY_STRENGTH_256,header.aesExtraDataRecord.aesKeyStrength) }
        val target=Device(base);ThemeManager(target.getSharedPreferences("bike",0))
        val before=target.getSharedPreferences("bike",0).all.toMap();val archive=SettingsArchive(target) { targetKey }
        val doc=archive.read(bytes,password)
        assertEquals(before,target.getSharedPreferences("bike",0).all)
        val backup=SettingsBackup(target,ThemeManager(target.getSharedPreferences("bike",0))) { targetKey }
        val plan=backup.plan(doc,doc.sections.keys)
        assertTrue(target.getSharedPreferences("route-export",0).all.isEmpty()) // Planning never installs credentials.
        backup.apply(plan)
        assertFalse(plan.cloudNeedsPassword)
        assertEquals(RouteExportSettings(source) { sourceKey }.read(),RouteExportSettings(target) { targetKey }.read())
        assertEquals(webdav,RouteExportSettings(target) { targetKey }.password())
        assertEquals(intervals,IntervalsSettings(target) { targetKey }.apiKey())
        assertTrue(IntervalsSettings(target) { targetKey }.read().enabled)
        assertEquals(token,ActivityServiceSettings(target,ActivityService.DREEVE) { targetKey }.credentials().getString("secret"))
        val strava=ActivityServiceSettings(target,ActivityService.STRAVA) { targetKey }
        assertTrue(strava.connected());assertTrue(strava.read().enabled);assertEquals("456",strava.read().account)
        assertEquals("EXAMPLE-REFRESH-TOKEN",strava.credentials().getString("refresh"))
        assertEquals(MapProviderSettings.read(source),MapProviderSettings.read(target))
        assertNotEquals(source.getSharedPreferences("route-export",0).getString("password",null),target.getSharedPreferences("route-export",0).getString("password",null))
        val excluded=setOf(BikeProfileStore.KEY,BikeUiConfigurationStore.KEY,"riderProfile.v1","password","needsPassword","oauthGeneration","oauthState","oauthClient","oauthAt")
        for(name in listOf("bike","route-export",HrvSettings.STORE,HrvQuestionSettings.STORE,"message-notifications","ride-backup","intervals-settings","activity-service-dreeve","activity-service-strava","map-provider")) {
            val expected=source.getSharedPreferences(name,0).all.filterKeys { it !in excluded }
            expected.forEach { (key,value) -> assertEquals("$name/$key",value,target.getSharedPreferences(name,0).all[key] ?: if(value=="") "" else null) }
        }
        assertEquals(RiderProfileStore(source.getSharedPreferences("bike",0)).read(),RiderProfileStore(target.getSharedPreferences("bike",0)).read())
        val profiles=BikeProfileStore(target,target.getSharedPreferences("bike",0))
        assertEquals("Erstes Rad",profiles.current().name);assertEquals(setOf(FitMetadataField.BIKE_NAME),profiles.current().recording.metadata)
        assertArrayEquals(BikePhotos.file(source,0).readBytes(),BikePhotos.file(target,0).readBytes())
        assertEquals(18,target.getSharedPreferences("bike",0).getInt("mapZoom",0))
        profiles.select(1);assertTrue(profiles.current().recording.metadata.isEmpty());assertEquals(13,target.getSharedPreferences("bike",0).getInt("mapZoom",0))
    }
    @Test fun wrongPasswordTamperingAndCancellationNeverApplySettingsAndLegacyJsonRemainsReadable() {
        val source=Device(base);configure(source);val bytes=SettingsArchive(source) { sourceKey }.export(BackupSection.entries.toSet(),password)
        val target=Device(base);val archive=SettingsArchive(target) { targetKey }
        assertTrue(runCatching { archive.read(bytes,"wrong".toCharArray()) }.exceptionOrNull() is SettingsBackupFailure)
        assertTrue(runCatching { archive.read(bytes) }.exceptionOrNull() is SettingsPasswordRequired)
        val damaged=bytes.copyOf().apply { this[size/2]=(this[size/2].toInt() xor 0x40).toByte() }
        assertTrue(runCatching { archive.read(damaged,password) }.isFailure)
        assertTrue(target.getSharedPreferences("bike",0).all.isEmpty())
        val public=SettingsBackup(source,ThemeManager(source.getSharedPreferences("bike",0))).export(BackupSection.entries.toSet())
        assertFalse(String(public).contains(webdav));assertFalse(String(public).contains(token))
        assertNull(archive.read(public).protectedData)
    }
    @Test fun partialRestoreInstallsOnlySelectedSecretsAndKeepsOtherProfilesAndPictures() {
        val source=Device(base);configure(source)
        val bytes=SettingsArchive(source) { sourceKey }.export(BackupSection.entries.toSet(),password)
        val target=Device(base);val bikes=BikeProfileStore(target,target.getSharedPreferences("bike",0));bikes.save(bikes.current().copy(name="Behalten"))
        val backup=SettingsBackup(target,ThemeManager(target.getSharedPreferences("bike",0))) { targetKey }
        backup.apply(backup.plan(SettingsArchive(target) { targetKey }.read(bytes,password),setOf(BackupSection.MAPS)))
        assertEquals("Behalten",bikes.current().name)
        assertEquals(MapProviderSettings.read(source),MapProviderSettings.read(target))
        assertFalse(target.getSharedPreferences("route-export",0).contains("password"))
        assertEquals(48.25f,target.getSharedPreferences("bike",0).getFloat("mapLatitude",0f))
    }
    @Test fun unreadableStoredCredentialsFailTheExportInsteadOfSilentlyDroppingThem() {
        val source=Device(base);configure(source)
        source.getSharedPreferences("route-export",0).edit().putString("password","broken-ciphertext").commit()
        val error=runCatching { SettingsArchive(source) { sourceKey }.export(setOf(BackupSection.CLOUD),password) }.exceptionOrNull()
        assertEquals(BackupProblem.CREDENTIALS,(error as SettingsBackupFailure).problem)
    }
    @Test fun unexpectedZipPathsAndUnencryptedEntriesAreRejectedWithoutExtractingFiles() {
        val target=Device(base);val archive=SettingsArchive(target) { targetKey }
        for(encrypted in listOf(true,false)) {
            val output=java.io.ByteArrayOutputStream()
            net.lingala.zip4j.io.outputstream.ZipOutputStream(output,password).use { zip ->
                zip.putNextEntry(net.lingala.zip4j.model.ZipParameters().apply {
                    fileNameInZip=if(encrypted) "../settings.json" else "settings.json"
                    isEncryptFiles=encrypted;encryptionMethod=EncryptionMethod.AES;aesKeyStrength=AesKeyStrength.KEY_STRENGTH_256
                })
                zip.write("{}".toByteArray());zip.closeEntry()
            }
            assertTrue(runCatching { archive.read(output.toByteArray(),password) }.isFailure)
            assertTrue(target.getSharedPreferences("bike",0).all.isEmpty())
            assertTrue(target.filesDir.listFiles().orEmpty().isEmpty())
        }
    }

    @Test fun profileOnlyRestoreRemovesTargetOverridesAbsentInTheSavedActiveProfile() {
        val source=Device(base);BikeProfileStore(source,source.getSharedPreferences("bike",0))
        val bytes=SettingsArchive(source) { sourceKey }.export(setOf(BackupSection.PROFILE),password)
        val target=Device(base);val prefs=target.getSharedPreferences("bike",0)
        BikeProfileStore(target,prefs);prefs.edit().putInt("mapZoom",21).putBoolean(DrinkReminderSettings.enabledKey,true).commit()
        val backup=SettingsBackup(target,ThemeManager(prefs)) { targetKey }
        backup.apply(backup.plan(SettingsArchive(target) { targetKey }.read(bytes,password),setOf(BackupSection.PROFILE)))
        assertFalse(prefs.contains("mapZoom"));assertFalse(prefs.contains(DrinkReminderSettings.enabledKey))
    }

}
