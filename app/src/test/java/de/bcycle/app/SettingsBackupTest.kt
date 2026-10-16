package de.bcycle.app

import android.content.SharedPreferences
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Proxy
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SettingsBackupTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private val bike get()=context.getSharedPreferences("bike",0)
    private val route get()=context.getSharedPreferences("route-export",0)
    private fun backup(b:SharedPreferences=bike,r:SharedPreferences=route)=SettingsBackup(b,r,ThemeManager(b))
    private fun seed() {
        bike.edit().putString("address","AA:BB:CC:DD:EE:FF").putString("heartAddress","SENSOR-PRIVATE").putString("name","PRIVATE BIKE")
            .putBoolean("recordPermissionsAsked",true).putFloat("mapLatitude",51f).putFloat("mapLongitude",10f).putString("displayFitId","PRIVATE FIT")
            .putStringSet("recordFields",setOf("RIDER_POWER","ESTIMATED_FTP")).putString("recordSpeed","GPS_SPEED").putString("recordDistance","GPS_DISTANCE")
            .putString("dashboard",DashboardLayout.defaults.replace(0,DashboardField("HEART_RATE")).encode()).putString("dashboardTemplate_0","HERO")
            .putString("mapDataField_1","GPS_ELAPSED_TIME").putBoolean("mapNight",true).putBoolean(ModeGlowSettings.key,true).commit()
        RiderProfileStore(bike).write(RiderProfile(LocalDate.of(1972,10,12),180,75.5,55,185,245))
        route.edit().putBoolean("save",false).putString("tree","content://provider/tree/DEVICE-SPECIFIC").putString("password","SECRET ENCRYPTED CIPHER")
            .putString("server","https://cloud.example.test").putString("user","bernd").putString("folder","rides").putBoolean("webdav",true).putBoolean("wifi",false).commit()
    }
    private fun json(bytes:ByteArray)=JSONObject(String(bytes,Charsets.UTF_8))
    private fun failAs(reason:BackupProblem,action:()->Unit) {
        try { action();fail("Expected $reason") } catch(e:SettingsBackupFailure) { assertEquals(reason,e.problem) }
    }
    @Test fun compositeBatteryFieldsAndFullDataTemplateRoundTripThroughDashboardBackup() {
        val field=BatteryFields.field(listOf(BatterySource.EBIKE,BatterySource.HEART,BatterySource.PHONE))
        bike.edit().putString("dashboard",DashboardLayout.defaults.replace(2,field).encode())
            .putString("dashboardTemplate_0","FULL_DATA").putString("mapDataField_0",field.id).commit()
        val b=backup();val saved=b.export(setOf(BackupSection.DASHBOARD))
        bike.edit().remove("dashboard").remove("dashboardTemplate_0").remove("mapDataField_0").commit()
        b.apply(b.plan(b.decode(saved),setOf(BackupSection.DASHBOARD)))
        assertEquals(field,DashboardLayout.decode(bike.getString("dashboard",null)).fields[2])
        assertEquals(field,MapFieldSelection.read(bike,0));assertEquals("FULL_DATA",bike.getString("dashboardTemplate_0",null))
    }
    @Test fun heightCorrectionPreferenceRoundTripsAndLegacyMapsDoNotResetIt() {
        bike.edit().putBoolean(FitElevationCorrection.preference,true).commit()
        val b=backup();val bytes=b.export(setOf(BackupSection.MAPS))
        bike.edit().putBoolean(FitElevationCorrection.preference,false).commit()
        b.apply(b.plan(b.decode(bytes),setOf(BackupSection.MAPS)))
        assertTrue(FitElevationCorrection.enabled(context))
        val legacy=json(bytes)
        legacy.getJSONObject("sections").getJSONObject(BackupSection.MAPS.id).remove("elevationCorrection")
        b.apply(b.plan(b.decode(legacy.toString().toByteArray()),setOf(BackupSection.MAPS)))
        assertTrue(FitElevationCorrection.enabled(context))
    }
    @Test fun exportsEachOrAllSectionsWithPublicMetadataAndNoFilesDevicesPermissionsSecretsOrPaths() {
        seed();val b=backup()
        for(section in BackupSection.entries) {
            val root=json(b.export(setOf(section)));assertEquals(setOf(section.id),root.getJSONObject("sections").keys().asSequence().toSet())
            assertEquals(SettingsBackup.FORMAT,root.getString("format"));assertEquals(1,root.getInt("schemaVersion"))
            assertTrue(root.has("createdAt"));assertEquals(AppVersion.name,root.getString("appVersion"))
        }
        val bytes=b.export(BackupSection.entries.toSet());val text=String(bytes,Charsets.UTF_8)
        for(secret in listOf("AA:BB","SENSOR-PRIVATE","PRIVATE BIKE","PRIVATE FIT","SECRET","DEVICE-SPECIFIC","permissions","Latitude","Longitude","password","token"))
            assertFalse(secret,text.contains(secret,ignoreCase=true))
        assertEquals(6,b.decode(bytes).sections.size)
        assertEquals(75.5,json(bytes).getJSONObject("sections").getJSONObject("riderProfile").getDouble("weightKg"),0.0)
        assertEquals(245,json(bytes).getJSONObject("sections").getJSONObject("riderProfile").getInt("ftpW"))
    }
    @Test fun selectiveDashboardRestoreLeavesEveryOtherSettingUnchangedAndSurvivesReload() {
        seed();val source=backup();val bytes=source.export(BackupSection.entries.toSet())
        bike.edit().putString("dashboard",DashboardLayout.defaults.encode()).putString("dashboardTemplate_0","MAP").putString("mapDataField_1","HEART_RATE")
            .putBoolean("mapNight",false).putBoolean(ModeGlowSettings.key,false).commit()
        RiderProfileStore(bike).write(RiderProfile(ftpW=300))
        val before=bike.all.toMap();val routesBefore=route.all.toMap()
        val b=backup();b.apply(b.plan(b.decode(bytes),setOf(BackupSection.DASHBOARD)))
        assertEquals("HERO",bike.getString("dashboardTemplate_0",null));assertEquals("HEART_RATE",DashboardLayout.decode(bike.getString("dashboard",null)).fields[0].id)
        assertEquals("GPS_ELAPSED_TIME",bike.getString("mapDataField_1",null));assertEquals(routesBefore,route.all)
        before.filterKeys { !it.startsWith("dashboard") && !it.startsWith("mapDataField") }.forEach { (k,v)->assertEquals(k,v,bike.all[k]) }
        assertEquals(300,RiderProfileStore(context.getSharedPreferences("bike",0)).read().ftpW)
    }
    @Test fun eightFieldDashboardBackupRoundTripsAndLegacyBackupPreservesTheNewSlot() {
        seed()
        val original=DashboardLayout.defaults.replace(7,DashboardField("GPS_MOVING_TIME"))
        bike.edit().putString("dashboard",original.encode()).putString("dashboardTemplate_0","EIGHT").commit()
        val b=backup();val bytes=b.export(setOf(BackupSection.DASHBOARD))
        bike.edit().putString("dashboard",DashboardLayout.defaults.encode()).putString("dashboardTemplate_0","DATA").commit()
        val doc=b.decode(bytes);b.apply(b.plan(doc,doc.sections.keys))
        assertEquals(original.encode(),bike.getString("dashboard",null))
        assertEquals("EIGHT",bike.getString("dashboardTemplate_0",null))
        val old=json(bytes)
        old.getJSONObject("sections").getJSONObject("dashboard").getJSONArray("pages").getJSONObject(0)
            .put("fields",JSONArray(original.fields.take(7).map { it.id })).put("layout","DATA")
        val updated=original.replace(7,DashboardField("HEART_AVERAGE"))
        bike.edit().putString("dashboard",updated.encode()).commit()
        val legacy=b.decode(old.toString().toByteArray());b.apply(b.plan(legacy,legacy.sections.keys))
        assertEquals(updated.encode(),bike.getString("dashboard",null))
        val invalid=json(bytes)
        invalid.getJSONObject("sections").getJSONObject("dashboard").getJSONArray("pages").getJSONObject(0)
            .getJSONArray("fields").put("HEART_RATE")
        failAs(BackupProblem.INVALID) { b.decode(invalid.toString().toByteArray()) }
    }

    @Test fun multiSectionRestoreRecolorsThemeAndRestoresProfileZonesButKeepsLocalUriAndSecrets() {
        seed();val theme=ThemeManager(bike)
        val custom=CustomTheme("custom:backup-light","My theme",ThemeTokens(0xffeeeeee.toInt(),0xffcccccc.toInt(),0xffaaaaaa.toInt(),0xff000000.toInt()))
        assertTrue(theme.saveCustom(custom).valid);assertTrue(theme.activate(custom.id))
        val b=SettingsBackup(bike,route,theme);val bytes=b.export(BackupSection.entries.toSet())
        theme.activate(BicycleThemes.red.id);RiderProfileStore(bike).write(RiderProfile())
        route.edit().putBoolean("save",true).commit();bike.edit().putBoolean("mapNight",false).commit()
        b.apply(b.plan(b.decode(bytes),BackupSection.entries.toSet()));theme.reloadFromPreferences()
        assertEquals(custom,theme.active);assertEquals(55,RiderProfileStore(bike).read().restingHeartRate)
        assertEquals(2,RiderProfileStore(bike).read().heartZones()!!.zoneFor(133)!!.index)
        assertFalse(route.getBoolean("save",true));assertEquals("content://provider/tree/DEVICE-SPECIFIC",route.getString("tree",null))
        assertEquals("SECRET ENCRYPTED CIPHER",route.getString("password",null));assertTrue(route.getBoolean("webdav",false))
        assertTrue(bike.getBoolean("mapNight",false));assertEquals(setOf("RIDER_POWER","ESTIMATED_FTP"),bike.getStringSet("recordFields",null))
    }
    @Test fun cloudImportNeverImportsSecretsAndDisablesAutomaticUploadForNewAccountOrDevice() {
        seed();val b=backup();val data=json(b.export(setOf(BackupSection.CLOUD)))
        data.getJSONObject("sections").getJSONObject("cloudSettings").put("server","https://other.example.test").put("password","ATTACKER PASSWORD").put("tree","/arbitrary/path")
        val doc=b.decode(data.toString().toByteArray());val plan=b.plan(doc,setOf(BackupSection.CLOUD));assertTrue(plan.cloudNeedsPassword)
        val before=bike.all.toMap();b.apply(plan)
        assertEquals(before,bike.all);assertEquals("SECRET ENCRYPTED CIPHER",route.getString("password",null));assertFalse(route.getBoolean("webdav",true))
        assertTrue(route.getBoolean("needsPassword",false));assertFalse(RouteExportSettings(context).hasPassword());assertEquals("",RouteExportSettings(context).password())
        assertEquals("content://provider/tree/DEVICE-SPECIFIC",route.getString("tree",null))
        route.edit().remove("password").remove("needsPassword").commit()
        val newPlan=b.plan(doc,setOf(BackupSection.CLOUD));assertTrue(newPlan.cloudNeedsPassword);assertEquals(false,newPlan.route["webdav"])
    }
    @Test fun supportedVersionOneMissingFieldsAndUnknownFieldsMergeWithoutClearingNewerValues() {
        seed();val b=backup();val root=json(b.export(setOf(BackupSection.PROFILE,BackupSection.DESIGN)))
        val sections=root.getJSONObject("sections")
        sections.put("riderProfile",JSONObject().put("ftpW",260).put("futureSetting",true))
        sections.put("designUi",JSONObject().put("modeGlow",false).put("futureColor","ignored"));sections.put("unknownArea",JSONObject().put("password","ignored"))
        val doc=b.decode(root.toString().toByteArray());assertEquals(setOf(BackupSection.PROFILE,BackupSection.DESIGN),doc.sections.keys)
        b.apply(b.plan(doc,doc.sections.keys))
        assertEquals(260,RiderProfileStore(bike).read().ftpW);assertEquals(75.5,RiderProfileStore(bike).read().weightKg!!,0.0)
        assertEquals(55,RiderProfileStore(bike).read().restingHeartRate);assertFalse(ModeGlowSettings.enabled(bike))
        assertFalse(bike.contains("futureSetting"));failAs(BackupProblem.SELECTION) { b.plan(doc,setOf(BackupSection.DASHBOARD)) }
    }
    @Test fun malformedUntrustedBackupsNeverChangePreferences() {
        seed();val b=backup();val bytes=b.export(BackupSection.entries.toSet());val beforeB=bike.all.toMap();val beforeR=route.all.toMap()
        failAs(BackupProblem.SELECTION) { b.export(emptySet()) }
        failAs(BackupProblem.INVALID) { b.decode("broken".toByteArray()) }
        failAs(BackupProblem.INVALID) { b.decode(bytes+"trailing".toByteArray()) }
        failAs(BackupProblem.FORMAT) { b.decode(json(bytes).put("format","other").toString().toByteArray()) }
        failAs(BackupProblem.VERSION) { b.decode(json(bytes).put("schemaVersion",99).toString().toByteArray()) }
        failAs(BackupProblem.TOO_LARGE) { b.decode(ByteArray(SettingsBackup.MAX_BYTES+1)) }
        failAs(BackupProblem.INVALID) { b.decode(("[".repeat(33)+"]".repeat(33)).toByteArray()) }
        for((section,key,value) in listOf(Triple("mapsNavigation","night","yes"),Triple("cloudSettings","server","http://evil.test"),
            Triple("cloudSettings","folder","../private"),Triple("riderProfile","weightKg",-1),Triple("riderProfile","ftpW","250"))) {
            val root=json(bytes);root.getJSONObject("sections").getJSONObject(section).put(key,value)
            failAs(BackupProblem.INVALID) { b.decode(root.toString().toByteArray()) }
        }
        val fields=json(bytes);fields.getJSONObject("sections").getJSONObject("recordingFit").put("fields",JSONArray().put("DOES_NOT_EXIST"))
        failAs(BackupProblem.INVALID) { b.decode(fields.toString().toByteArray()) }
        assertEquals(beforeB,bike.all);assertEquals(beforeR,route.all)
    }
    @Test fun failureInSecondPreferencesCommitRollsBackFirstAndRestoresAbsentKeys() {
        seed();val b=backup();val doc=b.decode(b.export(BackupSection.entries.toSet()))
        bike.edit().remove("mapDataField_1").putBoolean("mapNight",false).commit();route.edit().putBoolean("save",true).commit()
        val oldB=bike.all.toMap();val oldR=route.all.toMap();var commits=0
        val failing=Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,arrayOf(SharedPreferences::class.java)) { _,method,args ->
            if(method.name=="edit") {
                val editor=route.edit()
                Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,arrayOf(SharedPreferences.Editor::class.java)) { proxy,m,a ->
                    if(m.name=="commit" && commits++==0) { editor.commit();false }
                    else { val result=m.invoke(editor,*(a?:emptyArray()));if(result is SharedPreferences.Editor) proxy else result }
                }
            } else method.invoke(route,*(args?:emptyArray()))
        } as SharedPreferences
        val restoring=backup(r=failing)
        failAs(BackupProblem.IO) { restoring.apply(restoring.plan(doc,doc.sections.keys)) }
        assertEquals(oldB,bike.all);assertEquals(oldR,route.all);assertFalse(bike.contains("mapDataField_1"))
    }
    @Test fun firstCommitFailureRestoresValuesAndRollbackFailureHasExplicitError() {
        seed();val b=backup();val doc=b.decode(b.export(setOf(BackupSection.DASHBOARD)))
        bike.edit().putString("dashboard",DashboardLayout.defaults.encode()).commit();val before=bike.all.toMap()
        fun failing(attempts:Set<Int>):SharedPreferences {
            var count=0
            return Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,arrayOf(SharedPreferences::class.java)) { _,method,args ->
                if(method.name=="edit") {
                    val editor=bike.edit()
                    Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,arrayOf(SharedPreferences.Editor::class.java)) { proxy,m,a ->
                        if(m.name=="commit" && count++ in attempts) { editor.commit();false }
                        else { val value=m.invoke(editor,*(a?:emptyArray()));if(value is SharedPreferences.Editor) proxy else value }
                    }
                } else method.invoke(bike,*(args?:emptyArray()))
            } as SharedPreferences
        }
        val once=backup(b=failing(setOf(0)));failAs(BackupProblem.IO) { once.apply(once.plan(doc,doc.sections.keys)) }
        assertEquals(before,bike.all)
        val twice=backup(b=failing(setOf(0,1)));failAs(BackupProblem.ROLLBACK) { twice.apply(twice.plan(doc,doc.sections.keys)) }
    }
    @Test fun olderFourColorCustomThemesAndInvalidSecretsInUrlsAreHandledSafely() {
        seed();val manager=ThemeManager(bike)
        val custom=CustomTheme("custom:old-palette","Old palette",ThemeTokens(0xffeeeeee.toInt(),0xffcccccc.toInt(),0xffaaaaaa.toInt(),0xff000000.toInt(),0xff000088.toInt()))
        assertTrue(manager.saveCustom(custom).valid);assertTrue(manager.activate(custom.id))
        val b=SettingsBackup(bike,route,manager)
        val root=json(b.export(setOf(BackupSection.DESIGN)))
        root.getJSONObject("sections").getJSONObject("designUi").getJSONArray("customThemes").getJSONObject(0).remove("accent")
        val doc=b.decode(root.toString().toByteArray());b.apply(b.plan(doc,doc.sections.keys));manager.reloadFromPreferences()
        assertEquals(custom.tokens.accent,manager.active.tokens.accent)
        route.edit().putString("server","https://user:SECRET@host.test").commit()
        failAs(BackupProblem.INVALID) { b.export(setOf(BackupSection.CLOUD)) }
        assertFalse(String(b.export(setOf(BackupSection.DASHBOARD))).contains("SECRET"))
    }
    @Test fun streamsCloseBeforeSuccessAndCloseFailureIsNotSuccess() {
        seed();val b=backup();val bytes=b.export(setOf(BackupSection.PROFILE));var closed=false
        val output=object:ByteArrayOutputStream() { override fun close() { closed=true;super.close() } }
        b.write(output,bytes);assertTrue(closed);assertArrayEquals(bytes,output.toByteArray())
        val failed=object:ByteArrayOutputStream() { override fun close() { throw IOException("close failed") } }
        try { b.write(failed,bytes);fail() } catch(_:IOException) {}
        var readClosed=false
        val input=object:java.io.ByteArrayInputStream(bytes) { override fun close() { readClosed=true;super.close() } }
        b.read(input);assertTrue(readClosed)
    }
}
