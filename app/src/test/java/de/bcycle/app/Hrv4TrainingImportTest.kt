package de.bcycle.app

import android.app.Activity
import android.net.Uri
import android.os.Looper
import android.view.View
import android.widget.ScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Hrv4TrainingImportTest {
    private val zone=ZoneId.of("Europe/Berlin")
    private val context get()=RuntimeEnvironment.getApplication()
    private val sample="date, local, time, HR,rMSSD,SDNN,pNN50,LF,HF,HRV4T_Recovery_Points,training,trainingDistance,trainingTime,trainingRPE,note,mental_energy\n"+
        "2024-16-10,2024-16-10,07:15,60,40,55,,0.2,0.1,7.5,easy,12500,3600,3,Test,6.4,\n"+
        "2024-17-10,2024-17-10,,,,,,,,,rest,,,,Nur Tagesangaben,,\n"
    private fun parse(csv:String=sample,order:HrvCsvDateOrder?=null)=Hrv4TrainingCsv.preview(csv.byteInputStream(),zone,order)
    private fun root()=File(context.cacheDir,"csv-import-${UUID.randomUUID()}")
    private fun drain(controller:HrvController) {
        (HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ExecutorService)
            .submit {}.get(10,TimeUnit.SECONDS)
        shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun spacedHeadersTrailingEmptyColumnsAndQuotedMultilineFieldsKeepOriginalsWithoutInventingMetrics() {
        val csv="\uFEFFdate, local, time, rMSSD,note,LF,HF,mental_energy,unknown_column\r\n"+
            "2024-07-08,2024-07-08,06:15,25,\"Komma, und \"\"Zitat\"\"\nzweite Zeile\",0.03,0.02,6.4,original,\r\n"+
            "2024-16-08,2024-16-08,,,,,,,\r\n"
        val preview=parse(csv)
        assertEquals(HrvCsvDateOrder.YEAR_DAY_MONTH,preview.dateOrder);assertFalse(preview.dateOrderAssumed)
        assertEquals(1,preview.measurements);assertEquals(1,preview.contextDays)
        val external=preview.entries.first().result.getJSONObject("external")
        assertEquals("2024-08-07",external.getString("day"));assertEquals("06:15",external.getString("localTime"))
        assertEquals("Europe/Berlin",preview.entries.first().result.getString("zone"))
        assertEquals("Komma, und \"Zitat\"\nzweite Zeile",preview.entries.first().questionnaire.notes)
        assertEquals("6.4",external.getJSONObject("originalColumns").getString("mental_energy"))
        assertEquals("original",external.getJSONObject("originalColumns").getString("unknown_column"))
        assertFalse(external.getJSONObject("metrics").has("pnn50Percent"))
        for(key in listOf("rrTicks","nnMs","coverage","frequency","corrected","measureMs")) assertFalse(preview.entries.first().result.has(key))
        assertFalse(preview.entries.first().result.getBoolean("acceptable"))
        assertFalse(preview.entries.first().questionnaire.ratings.containsKey("energy"))
    }
    @Test fun wholeFileDateOrderDisambiguatesEarlierDatesAndExplicitChoiceHandlesSmallExports() {
        val short="date,time,rMSSD\n2024-07-08,06:15,25\n"
        assertTrue(parse(short).dateOrderAssumed)
        assertEquals("2024-08-07",parse(short).days.single())
        assertEquals("2024-07-08",parse(short,HrvCsvDateOrder.YEAR_MONTH_DAY).days.single())
        val iso=short+"2024-07-16,06:15,25\n"
        assertEquals(HrvCsvDateOrder.YEAR_MONTH_DAY,parse(iso).dateOrder)
        assertEquals("2024-07-08",parse(iso).days.first())
        assertEquals(listOf(4),parse(iso+"2024-32-10,06:15,25\n").invalidRecords)
        assertThrows(HrvCsvException::class.java) { parse(iso+"2024-16-07,06:15,25\n") }
        val invalid=parse("date,time,rMSSD\n2024-16-10,06:15,25\n2024-32-10,06:15,25\n2024-18-10,06:15,Infinity\n")
        assertEquals(listOf(3,4),invalid.invalidRecords);assertEquals(1,invalid.entries.size)
    }
    @Test fun malformedAndOversizedFilesAreRejectedBeforeAnyWrites() {
        for(csv in listOf("not,csv\nhello,world", "date,rMSSD\n\"broken,25", "date,rMSSD\n2024-16-10,\"25\"x", "date,rMSSD,rMSSD\n2024-16-10,20,30"))
            assertThrows(HrvCsvException::class.java) { parse(csv) }
        assertThrows(HrvCsvException::class.java) { Hrv4TrainingCsv.preview(byteArrayOf(0xc3.toByte(),0x28).inputStream(),zone) }
        assertThrows(HrvCsvException::class.java) { parse("x".repeat(Hrv4TrainingCsv.MAX_BYTES+1)) }
        assertThrows(HrvCsvException::class.java) { parse((",".repeat(200)+"\n").repeat(1300)) }
    }
    @Test fun idempotentImportsPreserveEditedAnswersOriginalFieldsAndPortableBackupWithoutRawPackets() {
        val from=root();val to=root()
        try {
            val store=HrvStore(from);val preview=parse()
            assertEquals(HrvCsvImportReport(2,0,0,0),Hrv4TrainingCsv.import(preview,store))
            val first=preview.entries.first();val id=first.result.getString("id")
            assertEquals(12.5,store.questionnaire(id)!!.trainingKm!!,0.0);assertEquals(60.0,store.questionnaire(id)!!.trainingMinutes!!,0.0)
            store.saveQuestionnaire(id,store.questionnaire(id)!!.copy(notes="Bearbeitet"))
            val unchanged=store.read(id).toString()
            val changed=parse(sample.replace(",40,55,",",80,55,"))
            assertEquals(HrvCsvImportReport(0,2,0,0),Hrv4TrainingCsv.import(changed,store))
            assertEquals(unchanged,store.read(id).toString());assertEquals("Bearbeitet",store.questionnaire(id)!!.notes)
            val rows=store.list();assertEquals(2,rows.size);assertTrue(rows.all { it.importedDay!=null && !it.acceptable })
            assertEquals(40.0,rows.last().rmssdMs!!,0.0);assertNull(rows.first().rmssdMs)
            val restored=HrvStore(to);assertTrue(restored.restore(store.backup(id)))
            assertEquals(store.read(id).toString(),restored.read(id).toString())
            val analysis=org.json.JSONObject(restored.analysisJson(id).toString(Charsets.UTF_8))
            assertEquals(0,analysis.getJSONArray("packets").length())
            assertEquals("6.4",analysis.getJSONObject("result").getJSONObject("external").getJSONObject("originalColumns").getString("mental_energy"))
            assertEquals("Bearbeitet",restored.questionnaire(id)!!.notes)
            val result=hrvResult(context,ThemeManager(context.getSharedPreferences("bike",0)),restored.read(id))
            assertEquals("hrv4_imported_result",result.tag)
            assertFalse(testDescendants(result).filterIsInstance<android.widget.TextView>().any { it.text.contains("ms²") })
            val staging=File(from,".hrv4-import-${UUID.randomUUID()}").apply { mkdirs();resolve("capture.jsonl").writeText("") }
            store.recover();assertFalse(staging.exists());assertEquals(2,store.list().size)
        } finally { from.deleteRecursively();to.deleteRecursively() }
    }
    @Test fun previewAndDataButtonAreReadOnlyUntilTheExplicitImportActionRunsOnWorker() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val controller=HrvController(host.get())
        val file=File(context.cacheDir,"sample.csv").apply { writeText(sample) }
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));var action:DataAction?=null
            DataSettingsView(a,themes) { action=it }.findViewWithTag<View>("data_HRV_IMPORT").performClick()
            assertEquals(DataAction.HRV_IMPORT,action)
            val view=HrvCsvImportView(a,themes,controller,Uri.fromFile(file))
            a.setContentView(ScrollView(a).apply { addView(view) });repeat(3) { drain(controller) }
            val store=HrvStore(File(a.filesDir,"hrv-measurements"));assertTrue(store.ids().isEmpty())
            assertTrue(view.findViewWithTag<View>("hrv4_import_start").isEnabled)
            view.findViewWithTag<View>("hrv4_import_start").performClick();repeat(2) { drain(controller) }
            assertEquals(2,store.ids().size);assertFalse(view.findViewWithTag<View>("hrv4_import_start").isEnabled)
            assertTrue(view.findViewWithTag<android.widget.TextView>("hrv4_import_status").text.contains("Importiert: 2"))
        } finally { host.pause().stop().destroy();controller.close();file.delete() }
    }
    @Test fun storageFailureReportsAlreadyImportedEntriesAndLeavesTheRestUntouched() {
        val root=root()
        try {
            val store=HrvStore(root);val preview=parse(sample+"2024-18-10,2024-18-10,07:15,60,40,55,,0.2,0.1,7.5,easy,12500,3600,3,Test,6.4,\n")
            val collision=File(root,preview.entries[1].result.getString("id")).apply { mkdirs();resolve("keep.txt").writeText("preserve") }
            assertEquals(HrvCsvImportReport(1,0,0,2),Hrv4TrainingCsv.import(preview,store))
            assertEquals(1,store.list().size);assertEquals("preserve",collision.resolve("keep.txt").readText())
            assertFalse(File(root,preview.entries[2].result.getString("id")).exists())
            assertFalse(root.listFiles().orEmpty().any { it.name.startsWith(".hrv4-import-") })
        } finally { root.deleteRecursively() }
    }
    @Test fun dataSettingsLaunchCsvPickerAndItsResultOpensPreviewWithoutImporting() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        val file=File(context.cacheDir,"picker-sample.csv").apply { writeText(sample) }
        try {
            val a=host.get()
            MainActivity::class.java.getDeclaredMethod("dataSettings",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,false)
            org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
                .findViewWithTag<View>("data_HRV_IMPORT").performClick()
            val request=shadowOf(a).nextStartedActivityForResult
            assertEquals(19,request.requestCode);assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT,request.intent.action)
            assertTrue(request.intent.getStringArrayExtra(android.content.Intent.EXTRA_MIME_TYPES)!!.contains("text/csv"))
            MainActivity::class.java.getDeclaredMethod("onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,android.content.Intent::class.java)
                .apply { isAccessible=true }.invoke(a,19,Activity.RESULT_OK,android.content.Intent().setData(Uri.fromFile(file)))
            repeat(3) { drain(RideRuntime.get(a).hrv) }
            val preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            assertNotNull(preview.findViewWithTag<View>("hrv4_import_start"))
            assertTrue(HrvStore(File(a.filesDir,"hrv-measurements")).ids().isEmpty())
            repeat(2) {
                preview.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                preview.layout(0,0,360,800);shadowOf(Looper.getMainLooper()).idle()
            }
            val image=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888)
            preview.draw(android.graphics.Canvas(image))
            File("build/reports/ui/hrv4-import-preview.png").apply { parentFile!!.mkdirs() }.outputStream().use {
                image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            };image.recycle()
        } finally { host.pause().stop().destroy();file.delete() }
    }
}
