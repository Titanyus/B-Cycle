package de.bcycle.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.ZoneOffset
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvRatingImportTest {
    @Test fun legacyScaleConvertsOncePreservesOriginalsAndKeepsRpeAndMissingAnswers() {
        val old=HrvQuestionnaire("2024-10-16",ratings=mapOf("sleep" to 4,"motivation" to 8,"soreness" to 0,"rpe" to 7)).json()
            .put("schema",1).apply { remove("legacyRatings") }
        val converted=HrvQuestionnaire.decode(old)
        assertEquals(mapOf("sleep" to 5,"motivation" to 10,"soreness" to 0,"rpe" to 7),converted.ratings)
        assertEquals(mapOf("sleep" to 4,"motivation" to 8,"soreness" to 0),converted.legacyRatings)
        assertEquals(2,converted.json().getInt("schema"))
        assertEquals(converted,HrvQuestionnaire.decode(converted.json()))
        assertFalse(converted.ratings.containsKey("energy"))
        assertThrows(IllegalArgumentException::class.java) { HrvQuestionnaire.decode(JSONObject(old.toString()).put("ratings",JSONObject().put("sleep",9))) }
        assertThrows(IllegalArgumentException::class.java) { HrvQuestionnaire.decode(converted.json().put("schema",3)) }
    }
    @Test fun allMatchingQuestionsRoundWithoutInvertingTheirMeaningAndRejectOutOfRangeValues() {
        val source=mapOf("sleep_quality" to "6.4","trainingMotivation" to "6.5","physical_condition" to "10",
            "mental_energy" to "0","muscle_soreness" to "2.5","fatigue" to "8,7","lifestyle" to "4.9","trainingRPE" to "3.5")
        assertEquals(mapOf("sleep" to 6,"motivation" to 7,"condition" to 10,"energy" to 0,"soreness" to 3,"fatigue" to 9,"lifestyle" to 5,"rpe" to 4),Hrv4TrainingCsv.ratings(source))
        for(invalid in listOf("","NaN","Infinity","-1","10.1","unknown"))
            assertTrue(Hrv4TrainingCsv.ratings(mapOf("sleep_quality" to invalid)).isEmpty())
    }
    @Test fun archivedImportsFillOnlyMissingAnswersAndBackupRetainsRoundedAndOriginalValues() {
        val c=RuntimeEnvironment.getApplication();val root=File(c.cacheDir,"rating-${UUID.randomUUID()}");val other=File(c.cacheDir,"rating-${UUID.randomUUID()}")
        try {
            val csv="date,time,rMSSD,sleep_quality,mental_energy,fatigue,lifestyle\n2024-16-10,07:00,40,6.4,6.5,2.5,7\n"
            val preview=Hrv4TrainingCsv.preview(csv.byteInputStream(),ZoneOffset.UTC);val store=HrvStore(root)
            Hrv4TrainingCsv.import(preview,store)
            val id=preview.entries.single().result.getString("id");val original=File(root,"$id/result.json").readBytes()
            val old=HrvQuestionnaire.decode(JSONObject(File(root,"$id/questionnaire.json").readText())).copy(ratings=mapOf("sleep" to 2),notes="Manuell bearbeitet",updatedAtMs=1234).json().put("schema",1)
            old.remove("legacyRatings");File(root,"$id/questionnaire.json").writeText(old.toString())
            val merged=store.questionnaireForEditing(id)!!
            assertEquals(3,merged.ratings["sleep"]);assertEquals(7,merged.ratings["energy"]);assertEquals(3,merged.ratings["fatigue"])
            assertEquals("Manuell bearbeitet",merged.notes);assertEquals(1234L,merged.updatedAtMs)
            assertEquals(merged,store.questionnaireForEditing(id));assertEquals(merged,store.questionnaire(id))
            assertArrayEquals(original,File(root,"$id/result.json").readBytes())
            val changed=Hrv4TrainingCsv.preview(csv.replace(",6.5,",",1.0,").byteInputStream(),ZoneOffset.UTC)
            assertEquals(HrvCsvImportReport(0,1,0,0),Hrv4TrainingCsv.import(changed,store))
            assertEquals(7,store.questionnaireForEditing(id)!!.ratings["energy"])
            val restored=HrvStore(other);assertTrue(restored.restore(store.backup(id)))
            assertEquals(merged,restored.questionnaire(id))
            val export=JSONObject(restored.analysisJson(id).toString(Charsets.UTF_8))
            assertEquals("6.5",export.getJSONObject("result").getJSONObject("external").getJSONObject("originalColumns").getString("mental_energy"))
            assertEquals(7,export.getJSONObject("questionnaire").getJSONObject("ratings").getInt("energy"))
        } finally { root.deleteRecursively();other.deleteRecursively() }
    }
}
