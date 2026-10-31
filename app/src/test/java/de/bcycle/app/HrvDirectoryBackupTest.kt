package de.bcycle.app

import android.content.Context
import android.content.ContextWrapper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvDirectoryBackupTest {
    private fun device():Context=object:ContextWrapper(RuntimeEnvironment.getApplication()) {
        val root=File(baseContext.cacheDir,UUID.randomUUID().toString()).apply { mkdirs() }
        override fun getFilesDir()=File(root,"files").apply { mkdirs() }
        override fun getCacheDir()=File(root,"cache").apply { mkdirs() }
    }
    private fun store(c:Context)=HrvStore(File(c.filesDir,"hrv-measurements"))
    private fun save(c:Context,time:String,acceptable:Boolean=true,external:Boolean=false):String {
        val id=UUID.randomUUID().toString()
        val row=JSONObject().put("format","bicycle-hrv").put("schema",1).put("id",id).put("utcMs",Instant.parse(time).toEpochMilli())
            .put("zone","UTC").put("phase","COMPLETE").put("acceptable",acceptable)
            .put("corrected",JSONObject().put("rmssdMs",42.0))
        if(external) row.put("external",JSONObject().put("provider",Hrv4TrainingCsv.PROVIDER).put("day",time.take(10))
            .put("time","08:00:00").put("hasMeasurement",true).put("metrics",JSONObject().put("rmssdMs",42.0)))
        store(c).restore(JSONObject().put("format","bicycle-hrv-backup").put("schema",1).put("result",row).put("capture","").toString().toByteArray())
        return id
    }
    @Test fun latestValidDailyWithAnswersAndAnswerOnlyDaysRoundTripWithoutOtherDomains() {
        val source=device();val target=device()
        val older=save(source,"2026-10-25T06:00:00Z")
        val chosen=save(source,"2026-10-25T07:00:00Z")
        save(source,"2026-10-25T08:00:00Z",false)
        val rejected=save(source,"2026-10-26T07:00:00Z",false)
        val journal=store(source)
        journal.saveQuestionnaire(chosen,HrvQuestionnaire.forMeasurement(journal.read(chosen)).copy(notes="Erholt",ratings=mapOf("sleep" to 7),stress=4))
        journal.saveQuestionnaire(rejected,HrvQuestionnaire.forMeasurement(journal.read(rejected)).copy(notes="Nur Tagesbefinden"))
        val parent=FileBackupDirectory(File(source.cacheDir,"backup").apply { mkdirs() })
        val exported=HrvDirectoryBackup(source).export(parent)
        assertEquals(2,exported.completed)
        val folder=parent.child(exported.folder,false)
        assertEquals(setOf(HrvDirectoryBackup.INDEX,"hrv-2026-10-25-$chosen.json","day-2026-10-26.json"),folder.entries().map { it.name }.toSet())
        assertEquals(4,journal.ids().size);assertTrue(journal.ids().contains(older))
        val imported=HrvDirectoryBackup(target).restore(folder)
        assertEquals(2,imported.completed);assertEquals(0,imported.failed)
        val restored=store(target)
        assertEquals(listOf(chosen),restored.ids())
        assertEquals("Erholt",restored.questionnaire(chosen)!!.notes)
        assertEquals(4,restored.questionnaire(chosen)!!.stress)
        assertTrue(restored.backupQuestionnaireDay("2026-10-26").toString(Charsets.UTF_8).contains("Nur Tagesbefinden"))
        assertEquals(2,HrvDirectoryBackup(target).restore(folder).duplicates)
        assertFalse(File(target.filesDir,"fit-library").exists())
    }
    @Test fun importedMeasurementsAreValidAndCorruptSnapshotsAreNotInstalled() {
        val source=device();val target=device();val id=save(source,"2026-10-25T08:00:00Z",false,true)
        val parent=FileBackupDirectory(File(source.cacheDir,"backup").apply { mkdirs() })
        val report=HrvDirectoryBackup(source).export(parent);assertEquals(1,report.completed)
        val folder=parent.child(report.folder,false)
        assertEquals(1,HrvDirectoryBackup(target).restore(folder).completed)
        assertEquals(id,store(target).ids().single())
        val root=File(source.cacheDir,"backup/${report.folder}")
        File(root,"hrv-2026-10-25-$id.json").appendText(" ")
        val empty=device();assertEquals(1,HrvDirectoryBackup(empty).restore(folder).failed)
        assertTrue(store(empty).ids().isEmpty())
        assertThrows(Exception::class.java) { HrvDirectoryBackup(empty).restore(parent) }
    }    @Test fun filenamesUseMeasurementDayAndOlderIndexedNamesStillRestore() {
        val source=device();val target=device();val id=save(source,"2026-10-25T08:00:00Z")
        val parent=FileBackupDirectory(File(source.cacheDir,"dates").apply { mkdirs() })
        val exported=HrvDirectoryBackup(source).export(parent)
        val directory=File(source.cacheDir,"dates/${exported.folder}")
        val dated="hrv-2026-10-25-$id.json";val old="hrv-$id.json"
        assertTrue(File(directory,dated).renameTo(File(directory,old)))
        val index=File(directory,HrvDirectoryBackup.INDEX)
        val value=JSONObject(index.readText());value.getJSONArray("entries").getJSONObject(0).put("name",old);index.writeText(value.toString())
        assertEquals(1,HrvDirectoryBackup(target).restore(parent.child(exported.folder,false)).completed)
        assertEquals(id,store(target).ids().single())
    }

}
