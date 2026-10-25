package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideNamingTest {
    @Test fun exactRequestedFormatGroupsDateAndTimeAndOmitsUnknownValues() {
        val template=RideNaming("Feierabendrunde",RideNamePart.entries.toSet())
        val details=RouteDetails(Instant.parse("2026-10-21T15:30:00Z").toEpochMilli(),32400.0,4520000,movingMs=4520000)
        assertEquals("Feierabendrunde · 21.10.2026 17:30h · 32,4km · 1:15:20h · 25,8km/h",template.format(details,ZoneId.of("Europe/Berlin"),Locale.GERMANY))
        assertEquals("Feierabendrunde",template.format(RouteDetails(),ZoneId.of("UTC")))
        assertEquals("17:30h",RideNaming("",setOf(RideNamePart.TIME)).format(details,ZoneId.of("Europe/Berlin")))
        assertThrows(IllegalArgumentException::class.java) { RideNaming.decode(template.json().put("text","bad\nname")) }
    }
    @Test fun customOrderKeepsDateAndTimeTogetherAndOldTemplatesUseOriginalOrder() {
        val details=RouteDetails(Instant.parse("2026-10-21T15:30:00Z").toEpochMilli(),32400.0,4520000,movingMs=4520000)
        val order=listOf(RideNameGroup.SPEED,RideNameGroup.MOVING,RideNameGroup.DATE_TIME,RideNameGroup.DISTANCE)
        val naming=RideNaming("Tour",RideNamePart.entries.toSet(),order)
        assertEquals("Tour · 25,8km/h · 1:15:20h · 21.10.2026 17:30h · 32,4km",naming.format(details,ZoneId.of("Europe/Berlin"),Locale.GERMANY))
        assertEquals(naming,RideNaming.decode(naming.json()))
        assertEquals(RideNameGroup.entries.toList(),RideNaming.decode(naming.json().apply { remove("order") }).order)
        val preview=rideNamingPreview(RuntimeEnvironment.getApplication(),naming,details,ZoneId.of("Europe/Berlin"),Locale.GERMANY)
        assertEquals(naming.format(details,ZoneId.of("Europe/Berlin"),Locale.GERMANY).removePrefix("Tour"),preview)
        assertEquals("17:30h",naming.copy(text="",parts=setOf(RideNamePart.TIME)).format(details,ZoneId.of("Europe/Berlin")))
        assertEquals("21.10.2026",naming.copy(text="",parts=setOf(RideNamePart.DATE)).format(details,ZoneId.of("Europe/Berlin")))
        assertThrows(IllegalArgumentException::class.java) { RideNaming.decode(naming.json().put("order",org.json.JSONArray(listOf("DATE_TIME","DATE_TIME","SPEED","MOVING")))) }
    }
    @Test fun profileTemplatesAndRecordingSettingsBackupRemainIndependentAndUiSupportsMultipleChoices() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("bike",0)
        val bikes=BikeProfileStore(c,prefs);val settings=RideNamingSettings(c)
        val expected=RideNaming("Tour",setOf(RideNamePart.DATE),RideNameGroup.entries.reversed())
        val original=bikes.activeSlot;settings.write(expected)
        val second=bikes.add()!!;bikes.select(second.slot);settings.write(RideNaming("Indoor",setOf(RideNamePart.DISTANCE)))
        bikes.select(original);assertEquals("Tour",settings.read().text)
        prefs.edit().putString("recordSpeed","GPS_SPEED").putString("recordDistance","GPS_DISTANCE").commit()
        val backup=SettingsBackup(c,ThemeManager(prefs));val bytes=backup.export(setOf(BackupSection.RECORDING))
        prefs.edit().putString("recordSpeed","SPEED").putString("recordDistance","BIKE_DISTANCE").commit()
        settings.write(RideNaming());val doc=backup.decode(bytes);backup.apply(backup.plan(doc,doc.sections.keys))
        assertEquals(expected,settings.read())
        assertEquals("GPS_SPEED",prefs.getString("recordSpeed",null));assertEquals("GPS_DISTANCE",prefs.getString("recordDistance",null))
        val view=RideNamingView(c,ThemeManager(prefs)) { true }
        view.findViewWithTag<android.view.View>("ride_name_TIME").performClick()
        assertEquals(setOf(RideNamePart.DATE,RideNamePart.TIME),settings.read().parts)
        bikes.select(second.slot);assertEquals("Indoor",settings.read().text)
    }
    @Test fun recordingUsesStartSnapshotAndRetryPreservesManualRename() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("bike",0)
        BikeProfileStore(c,prefs);val settings=RideNamingSettings(c);settings.write(RideNaming("Startvorlage",emptySet()))
        RouteExportSettings(c).write(RouteExportConfig(save=false,webdav=true,server="",user="rider"))
        val results=mutableListOf<String>();val store=RecordingStore(c) { update -> if(!update.busy) results+=RecordingStatusText.text(c,update) }
        val worker=RecordingStore::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(store) as java.util.concurrent.ExecutorService
        fun settle() {
            val latch=java.util.concurrent.CountDownLatch(1);worker.execute { latch.countDown() };assertTrue(latch.await(10,java.util.concurrent.TimeUnit.SECONDS))
            shadowOf(android.os.Looper.getMainLooper()).idle()
        }
        try {
            val start=Instant.parse("2026-10-21T15:30:00Z").toEpochMilli()
            store.begin(start,RecordingConfig());settings.write(RideNaming("Andere Vorlage",emptySet()))
            store.append(RideSample(start,0,values=mapOf("SPEED" to 5.0,"BIKE_DISTANCE" to 0.0)))
            store.append(RideSample(start+1000,1000,values=mapOf("SPEED" to 5.0,"BIKE_DISTANCE" to 5.0)))
            store.finish();settle()
            val files=java.io.File(c.filesDir,"fit-library").listFiles().orEmpty().filter { it.extension=="fit" }
            val id=files.single().nameWithoutExtension
            assertEquals("Startvorlage",RideNames.name(c,id))
            assertTrue(java.io.File(c.filesDir,"pending-rides").listFiles().orEmpty().any { it.extension=="ride" })
            RideNames.manual(c,id,"Manuell geändert")
            RouteExportSettings(c).write(RouteExportConfig(save=false))
            store.retryPending();settle()
            assertEquals("Manuell geändert",RideNames.name(c,id))
            assertFalse(java.io.File(c.filesDir,"pending-rides").listFiles().orEmpty().any { it.extension=="ride" || it.name.endsWith(".naming.json") })
        } finally { worker.shutdownNow() }
    }
}
