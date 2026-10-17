package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DashboardTrainingHelpTest {
    private val c get()=RuntimeEnvironment.getApplication()
    @Test fun routingOnlyUsesMatchingCurrentOrAverageRiderFieldsAndRespectsVisibility() {
        val layout=DashboardLayout.decode(listOf("RIDER_POWER","HEART_RATE","AVERAGE_POWER","HEART_AVERAGE","MOTOR_POWER","MAXIMUM_POWER","CADENCE","TRAINING_HEART_ZONE").joinToString("|"))
        val current=DashboardTrainingHelp(TrainingHelpMode.ALL_ZONES).sources(layout,DashboardDataVisibility()).sources
        assertEquals(listOf(ZoneGlowSource.POWER_CURRENT,ZoneGlowSource.HEART_CURRENT,ZoneGlowSource.OFF,ZoneGlowSource.OFF,ZoneGlowSource.OFF,ZoneGlowSource.OFF,ZoneGlowSource.OFF,ZoneGlowSource.HEART_CURRENT),current)
        val average=DashboardTrainingHelp(TrainingHelpMode.ZONE_TWO,false,true).sources(layout,DashboardDataVisibility()).sources
        assertEquals(ZoneGlowSource.POWER_AVERAGE,average[2]);assertEquals(ZoneGlowSource.HEART_AVERAGE,average[3]);assertEquals(2,average.count { it!=ZoneGlowSource.OFF })
        assertEquals(ZoneGlowSource.OFF,DashboardTrainingHelp(TrainingHelpMode.ALL_ZONES).sources(layout,DashboardDataVisibility().replace(0,false)).sources[0])
        assertTrue(DashboardTrainingHelp().sources(layout,DashboardDataVisibility()).sources.all { it==ZoneGlowSource.OFF })
    }
    @Test fun zoneTwoHasWideLowerToleranceAndTightUpperWarningWithoutChangingZones() {
        val colors=ZoneTwoHelpColors
        assertEquals(colors.green,colors.color(45.0,55.0,75.0));assertEquals(colors.green,colors.color(65.0,55.0,75.0))
        assertEquals(colors.red,colors.color(35.0,55.0,75.0));assertEquals(colors.red,colors.color(75.5,55.0,75.0))
        assertNotEquals(colors.green,colors.color(40.0,55.0,75.0));assertNotEquals(colors.red,colors.color(40.0,55.0,75.0))
        assertEquals(colors.green,colors.color(50.0,60.0,70.0));assertEquals(colors.red,colors.color(70.5,60.0,70.0))
        val data=TrainingSnapshot(RiderProfile(ftpW=200,restingHeartRate=60,maximumHeartRate=190),power=100.0,heart=135.0)
        assertEquals(1,data.powerZone);assertEquals(1,data.heartZone)
        assertEquals(colors.green,data.glow(ZoneGlowSource.POWER_CURRENT,true));assertEquals(colors.green,data.glow(ZoneGlowSource.HEART_CURRENT,true))
        assertEquals(ZoneGlowColors.power(50.0),data.glow(ZoneGlowSource.POWER_CURRENT))
        assertNull(TrainingSnapshot().glow(ZoneGlowSource.POWER_CURRENT,true))
        val stale=TrainingPresentation().update(TrainingInputs(100.0,0,100.0,130.0,0,130.0),data.profile,5001)
        for(source in ZoneGlowSource.entries) assertNull(stale.glow(source,true))
    }
    @Test fun migrationBackupAndPageCleanupPreserveDashboardScope() {
        val prefs=c.getSharedPreferences("bike",0)
        assertEquals(DashboardTrainingHelp(),DashboardTrainingHelp.read(prefs,0))
        prefs.edit().putString(ZoneGlowConfig.key(0),ZoneGlowConfig().replace(0,ZoneGlowSource.POWER_AVERAGE).encode()).apply()
        assertEquals(DashboardTrainingHelp(TrainingHelpMode.ALL_ZONES,false,true),DashboardTrainingHelp.read(prefs,0))
        assertNull(DashboardTrainingHelp.decode("""{"mode":"ALL_ZONES","current":"true","average":false}"""))
        val settings=DashboardTrainingHelp(TrainingHelpMode.ZONE_TWO,true,true)
        prefs.edit().putString(DashboardTrainingHelp.key(0),settings.encode()).apply()
        assertEquals(settings,DashboardTrainingHelp.read(prefs,0));assertEquals(TrainingHelpMode.OFF,DashboardTrainingHelp.read(prefs,1).mode)
        assertTrue(BikeUiConfigurationScope.capture(prefs).design.containsKey(DashboardTrainingHelp.key(0)))
        val backup=SettingsBackup(c,ThemeManager(prefs));val bytes=backup.export(setOf(BackupSection.DASHBOARD))
        prefs.edit().clear().commit();backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.DASHBOARD)))
        assertEquals(settings,DashboardTrainingHelp.read(prefs,0))
        val invalid=JSONObject(String(bytes));invalid.getJSONObject("sections").getJSONObject("dashboard").getJSONArray("pages").getJSONObject(0).getJSONObject("trainingHelp").put("mode","INVALID")
        try { backup.plan(backup.decode(invalid.toString().toByteArray()),setOf(BackupSection.DASHBOARD));fail() } catch(_:SettingsBackupFailure) { }
        assertEquals(settings,DashboardTrainingHelp.read(prefs,0))
        DashboardPages(c,prefs).delete(0);assertFalse(prefs.contains(DashboardTrainingHelp.key(0)))
    }
}
