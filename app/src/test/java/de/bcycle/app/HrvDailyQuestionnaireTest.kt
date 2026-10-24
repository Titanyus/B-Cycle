package de.bcycle.app

import android.app.Activity
import android.widget.EditText
import android.text.style.ForegroundColorSpan
import de.bcycle.hrv.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class HrvDailyQuestionnaireTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private fun measurement(store:HrvStore,time:String):String {
        val id=UUID.randomUUID().toString();val session=MeasurementSession(0)
        store.begin(id,Instant.parse(time).toEpochMilli(),"Gurt",session)
        session.abort("USER_ABORT");store.save(id,session,Evaluate.session(session),0)
        return id
    }
    @Test fun answersBelongToDaySurviveRemeasurementDeletionAndPortableBackup() {
        val root=File(context.cacheDir,"days-${UUID.randomUUID()}")
        val target=File(context.cacheDir,"days-${UUID.randomUUID()}")
        val zip=File(context.cacheDir,"days-${UUID.randomUUID()}.zip")
        try {
            val store=HrvStore(root);val first=measurement(store,"2026-10-21T07:00:00Z")
            val q=HrvQuestionnaire.forMeasurement(store.read(first)).copy(ratings=mapOf("condition" to 5),notes="Erholt",updatedAtMs=100)
            store.saveQuestionnaire(first,q)
            val second=measurement(store,"2026-10-21T09:00:00Z")
            val next=measurement(store,"2026-10-22T09:00:00Z")
            val before=File(root,"$second/result.json").readBytes()
            assertEquals(q,store.questionnaireForEditing(second));assertNull(store.questionnaire(next))
            val changed=q.copy(notes="Aktualisiert",updatedAtMs=200)
            store.saveQuestionnaire(second,changed)
            assertEquals(changed,HrvStore(root).questionnaire(first))
            assertArrayEquals(before,File(root,"$second/result.json").readBytes())
            val restored=HrvStore(target)
            assertTrue(restored.restore(store.backup(first)));assertEquals(changed,restored.questionnaire(first))
            store.delete(first);store.delete(second)
            assertEquals(listOf(q.day),store.questionnaireDays())
            assertEquals(2,HrvDiagnosticExport.write(store,zip)) // Other day's measurement plus orphaned answers.
            java.util.zip.ZipFile(zip).use { assertNotNull(it.getEntry("day-${q.day}.json")) }
            store.delete(next)
            assertEquals(1,HrvDiagnosticExport.write(store,zip)) // A day-only archive is still exportable.
            java.util.zip.ZipFile(zip).use { assertEquals(1,it.size()) }
            assertFalse(restored.restore(store.backupQuestionnaireDay(q.day)))
            val fresh=HrvStore(File(target,"fresh"))
            assertTrue(fresh.restore(store.backupQuestionnaireDay(q.day)))
            val sameDay=measurement(fresh,"2026-10-21T11:00:00Z")
            assertEquals(changed,fresh.questionnaire(sameDay))
            val dayAfter=measurement(store,"2026-10-21T12:00:00Z")
            assertEquals(changed,store.questionnaire(dayAfter))
        } finally { root.deleteRecursively();target.deleteRecursively();zip.delete() }
    }
    @Test fun legacyAnswersMergeDeterministicallyWithoutRewritingMeasurementOrLegacyAnswers() {
        val root=File(context.cacheDir,"days-${UUID.randomUUID()}")
        try {
            val store=HrvStore(root);val a=measurement(store,"2026-10-21T07:00:00Z");val b=measurement(store,"2026-10-21T09:00:00Z")
            val q=HrvQuestionnaire.forMeasurement(store.read(a))
            val old=File(root,"$a/questionnaire.json").apply { writeText(q.copy(ratings=mapOf("sleep" to 7,"energy" to 4),updatedAtMs=10).json().toString()) }
            val newer=File(root,"$b/questionnaire.json").apply { writeText(q.copy(ratings=mapOf("sleep" to 5),notes="Neu",updatedAtMs=20).json().toString()) }
            val before=old.readBytes();val next=newer.readBytes()
            val restoredRoot=File(root,"restored")
            val restored=HrvStore(restoredRoot)
            for((id,file) in listOf(a to old,b to newer)) {
                val backup=org.json.JSONObject(store.backup(id).toString(Charsets.UTF_8)).put("questionnaire",org.json.JSONObject(file.readText()))
                backup.remove("questionnaireScope") // Older portable backups used per-measurement answers.
                assertTrue(restored.restore(backup.toString().toByteArray()))
            }
            val merged=store.questionnaire(b)!!
            assertEquals(merged,restored.questionnaire(b))
            assertEquals(mapOf("sleep" to 5,"energy" to 4),merged.ratings);assertEquals("Neu",merged.notes)
            assertEquals(merged,store.questionnaire(a))
            store.saveQuestionnaire(a,merged.copy(ratings=emptyMap(),notes="",updatedAtMs=30))
            assertTrue(store.questionnaire(b)!!.ratings.isEmpty()) // Migration must never resurrect cleared answers.
            assertArrayEquals(before,old.readBytes());assertArrayEquals(next,newer.readBytes())
        } finally { root.deleteRecursively() }
    }
    @Test fun deletingAnUnopenedLegacyMeasurementPreservesItsDayAnswers() {
        val root=File(context.cacheDir,"days-${UUID.randomUUID()}")
        try {
            val store=HrvStore(root);val id=measurement(store,"2026-10-21T07:00:00Z")
            val q=HrvQuestionnaire.forMeasurement(store.read(id)).copy(notes="Altbestand",updatedAtMs=10)
            File(root,"$id/questionnaire.json").writeText(q.json().toString())
            store.delete(id)
            assertTrue(store.ids().isEmpty());assertEquals(listOf(q.day),store.questionnaireDays())
            val newId=measurement(store,"2026-10-21T10:00:00Z")
            assertEquals(q,store.questionnaire(newId))
        } finally { root.deleteRecursively() }
    }
    @Test fun fourOClockBoundaryUsesLocalCivilTimeIncludingBothDstChanges() {
        HrvSettings(context).write(HrvMeasurementConfig(showOnFirstStart=true))
        val launch=HrvDailyLaunch(context)
        for(day in listOf("2026-03-29","2026-10-25")) {
            val before=java.time.LocalDate.parse(day).atTime(3,59,59).atZone(ZoneId.of("Europe/Berlin"))
            val at=before.withHour(4).withMinute(0).withSecond(0)
            launch.acknowledge(HrvDailyLaunch.day(before))
            assertFalse(launch.shouldShow(HrvDailyLaunch.day(before)))
            assertTrue(launch.shouldShow(HrvDailyLaunch.day(at)))
            assertEquals(at.toLocalDate(),HrvDailyLaunch.day(at))
            assertEquals(at.toLocalDate().minusDays(1),HrvDailyLaunch.day(at.withHour(1)))
        }
        val utc=Instant.parse("2026-10-24T02:00:00Z")
        assertEquals(java.time.LocalDate.parse("2026-10-24"),HrvDailyLaunch.day(utc.atZone(ZoneId.of("Europe/Berlin"))))
        assertEquals(java.time.LocalDate.parse("2026-10-23"),HrvDailyLaunch.day(utc.atZone(ZoneId.of("UTC"))))
    }
    @Test fun timeMaskTypesDigitsSkipsColonKeepsGhostDecorationAndDoesNotInventEmptyAnswers() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));val edit=BicycleComponents.input(a,themes)
            val answers=mutableListOf<String?>();val mask=BicycleTimeInput(edit,themes,"Aufgewacht","") { answers+=it }
            a.setContentView(edit)
            assertEquals("Aufgewacht · 00:00 h",edit.text.toString());assertEquals("",mask.value);assertTrue(answers.isEmpty())
            val start=edit.text.indexOf("00:00")
            for(digit in "06") edit.text.insert(edit.selectionStart,digit.toString())
            assertEquals(start+3,edit.selectionStart)
            for(digit in "30") edit.text.insert(edit.selectionStart,digit.toString())
            assertEquals("06:30",answers.last());assertEquals("Aufgewacht · 06:30 h",edit.text.toString())
            val spans=edit.text.getSpans(0,edit.length(),ForegroundColorSpan::class.java)
            assertTrue(spans.any { edit.text.getSpanStart(it)==0 && android.graphics.Color.alpha(it.foregroundColor)==127 })
            assertTrue(spans.any { edit.text.getSpanEnd(it)==edit.length() && android.graphics.Color.alpha(it.foregroundColor)==127 })
            edit.text.replace(start,start+2,"07");assertEquals("07:30",answers.last())
            edit.text.delete(start+2,start+3);assertNull(answers.last());assertTrue(edit.text.toString().contains(':'))
            edit.setText("1:05");assertEquals("01:05",answers.last())
            edit.text.delete(0,1);assertEquals("01:05",answers.last()) // Labels cannot delete a digit.
            edit.text.delete(edit.length()-1,edit.length());assertEquals("01:05",answers.last()) // Nor can the unit.
            edit.setText("");assertEquals("",answers.last());assertEquals("Aufgewacht · 00:00 h",edit.text.toString())
        } finally { host.pause().stop().destroy() }
    }
}
