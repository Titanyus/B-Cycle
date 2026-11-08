package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.garmin.fit.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
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
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RideFeedbackTest {
    private val id="recorded-"+"e".repeat(64)
    private fun nodes(view:View):List<View> = listOf(view)+if(view is ViewGroup) (0 until view.childCount).flatMap { nodes(view.getChildAt(it)) } else emptyList()
    @Test fun noteSavesWithoutConfirmationAndHeaderEditReturnsToPlainTextWithIndependentRideRpe() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
        try {
            val view=RouteAnalysisView(c,themes,id) {}
            val entry=ManagedRoute(id,"Fahrt",true,"FIT",RouteDetails(1792483200000L),false)
            view.show(RouteAnalysis(entry,emptyMap(),null,false,0.0,emptyList()));c.setContentView(view)
            assertEquals(View.GONE,view.findViewWithTag<View>("analysis_note").visibility)
            assertNull(RideAnnotations.read(c,id).rpe)
            RideAnnotations.rpe(c,id,0);view.refreshFeedback()
            assertEquals(0,RideAnnotations.read(c,id).rpe)
            assertTrue(nodes(view.findViewWithTag("analysis_rpe")).filterIsInstance<TextView>().any { it.text.toString()=="0" })
            view.editNote();val editor=view.findViewWithTag<RideNoteEditor>("ride_note_editor")
            assertEquals(3,editor.minLines);assertEquals(3,editor.maxLines)
            BicycleDialogStyle.control(editor,themes);assertEquals(BicycleDesign.bodyTypeface,editor.typeface)
            editor.setText("Schöne Runde\nGegenwind auf dem Rückweg")
            assertEquals(editor.text.toString(),RideAnnotations.read(c,id).note)
            editor.clearFocus();shadowOf(Looper.getMainLooper()).idle()
            assertEquals("Schöne Runde\nGegenwind auf dem Rückweg",view.findViewWithTag<TextView>("analysis_note_text").text.toString())
            assertNull(view.findViewWithTag<View>("ride_note_editor"))
            view.editNote();view.findViewWithTag<RideNoteEditor>("ride_note_editor").setText("")
            c.setContentView(View(c));assertEquals("",RideAnnotations.read(c,id).note)
            view.dispose()
        } finally { RideAnnotations.forget(c,id);host.pause().stop().destroy() }
    }
    @Test fun localOnlyFeedbackReleasesCompletedJobsAndDoesNotRewriteCloudQueue() {
        val context=RuntimeEnvironment.getApplication();val id=java.util.UUID.randomUUID().toString()
        val revision=RideNames.revision(context)
        try {
            RideAnnotations.note(context,id,"Lokal gespeichert")
            RideAnnotations.flushAsync(context,id)
            val worker=RideAnnotations::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(null) as java.util.concurrent.ScheduledExecutorService
            worker.submit {}.get(5,java.util.concurrent.TimeUnit.SECONDS)
            assertEquals("Lokal gespeichert",RideAnnotations.read(context,id).note)
            assertFalse(context.getSharedPreferences("ride-annotations",0).getBoolean("dirty:$id",false))
            assertEquals("No cloud queue write without a linked service",revision,RideNames.revision(context))
            val pending=RideAnnotations::class.java.getDeclaredField("pending").apply { isAccessible=true }.get(null) as Map<*,*>
            assertFalse("Completed autosave must not remain retained for every edited ride",pending.containsKey(id))
        } finally { RideAnnotations.forget(context,id) }
    }
    @Test fun fitExportsContainScaledRpeAndUtf8MemoWithoutChangingMeasurementOrOriginalBytes() {
        val c=RuntimeEnvironment.getApplication();val original=File(c.cacheDir,"annotation-original.fit")
        val encoder=FileEncoder(original,Fit.ProtocolVersion.V2_0)
        try {
            encoder.write(FileIdMesg().apply { setType(com.garmin.fit.File.ACTIVITY);setManufacturer(255) })
            encoder.write(RecordMesg().apply { setTimestamp(DateTime(1000000000));setPower(231) })
            encoder.write(SessionMesg().apply { setSport(Sport.CYCLING);setTotalTimerTime(60f) })
        } finally { encoder.close() }
        val before=original.readBytes();val output=File(c.cacheDir,"annotation-export.fit")
        val note="Wind & Sonne, schön! "+"ä🙂".repeat(110)
        RideAnnotationFiles.fit(original,output,RideAnnotation(note,9))
        var rpe:Short?=null;var power:Int?=null;val bytes=ArrayList<Byte>()
        assertTrue(output.inputStream().use { Decode().read(it,MesgListener { m ->
            when(m.num) { 18->rpe=SessionMesg(m).getWorkoutRpe();20->power=RecordMesg(m).getPower();145->MemoGlobMesg(m).getMemo().forEach { b->bytes.add(b) } }
        }) })
        assertEquals(90.toShort(),rpe);assertEquals(231,power)
        assertEquals("B|CYCLE note\n"+note,bytes.toByteArray().toString(Charsets.UTF_8))
        assertArrayEquals(before,original.readBytes())
    }
    @Test fun gpxExportKeepsGeometryNamespacesAndEscapedTextAndAddsFeedback() {
        val c=RuntimeEnvironment.getApplication();val key=java.util.UUID.randomUUID().toString()
        val original=File(c.cacheDir,"feedback-source.gpx").apply { writeText("""<gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:test="https://example.test"><metadata><name>A &amp; B</name></metadata><trk><trkseg><trkpt lat="50" lon="8"/><trkpt lat="50.01" lon="8.01"/></trkseg></trk><extensions><test:keep>vorhanden</test:keep></extensions></gpx>""") }
        val before=original.readBytes()
        RideAnnotations.restore(c,key,RideAnnotation("Sonne & schön 🙂",0).json())
        val exported=RideAnnotationFiles.exported(c,key,original)
        assertEquals(original.inputStream().use { GpxImport.read(it) }.points,exported.inputStream().use { GpxImport.read(it) }.points)
        val document=javax.xml.parsers.DocumentBuilderFactory.newInstance().apply { isNamespaceAware=true }.newDocumentBuilder().parse(exported)
        assertEquals("vorhanden",document.getElementsByTagNameNS("https://example.test","keep").item(0).textContent)
        assertEquals("A & B",document.getElementsByTagNameNS("http://www.topografix.com/GPX/1/1","name").item(0).textContent)
        assertEquals("Sonne & schön 🙂",document.getElementsByTagNameNS("https://bcycle.app/feedback/1","note").item(0).textContent)
        assertEquals("0",document.getElementsByTagNameNS("https://bcycle.app/feedback/1","rpe").item(0).textContent)
        assertArrayEquals(before,original.readBytes());RideAnnotations.forget(c,key)
    }
    @Test fun cloudAdaptersPreserveOtherDescriptionAndUseOnlySupportedNativeFields() {
        val c=RuntimeEnvironment.getApplication();val server=MockWebServer();server.start()
        try {
            val value=RideAnnotation("Gegenwind & Regen",0)
            val old="Schon vorhandene Beschreibung"
            server.enqueue(MockResponse().setBody(JSONObject().put("id","activity-1").put("description",old).toString()));server.enqueue(MockResponse().setBody("{}"))
            DreeveClient(ActivityServiceConfig(address=server.url("/").toString()),"test",base=server.url("/")).annotations("activity-1",value)
            assertEquals("GET",server.takeRequest().method)
            val dreeve=server.takeRequest();assertEquals("PATCH",dreeve.method)
            val description=JSONObject(dreeve.body.readUtf8()).getString("description")
            assertTrue(description.startsWith(old));assertTrue(description.contains("RPE: 0/10"))
            server.enqueue(MockResponse().setBody(JSONObject().put("id","42").put("description",old).toString()));server.enqueue(MockResponse().setBody("{}"))
            IntervalsClient(IntervalsConfig(athlete="i1"),"test",base=server.url("/api/v1/")).annotations("42",value)
            server.takeRequest();val intervals=server.takeRequest();assertEquals("PUT",intervals.method);assertEquals("/api/v1/activity/42",intervals.path)
            val patch=JSONObject(intervals.body.readUtf8());assertEquals(0,patch.getInt("icu_rpe"));assertFalse(patch.has("session_rpe"));assertTrue(patch.getString("description").contains(old))
            val key=KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            val settings=ActivityServiceSettings(c,ActivityService.STRAVA) { key }
            settings.write(ActivityServiceConfig(clientId="123"),"secret")
            settings.tokens("123","secret",JSONObject().put("access_token","access").put("refresh_token","refresh").put("expires_at",System.currentTimeMillis()/1000+3600).put("athlete",JSONObject().put("id",123)),true)
            server.enqueue(MockResponse().setBody(JSONObject().put("id",1234).put("description",old).toString()));server.enqueue(MockResponse().setBody("{}"))
            StravaClient(settings,base=server.url("/")).annotations("1234",value)
            server.takeRequest();val strava=server.takeRequest();assertEquals("PUT",strava.method)
            val body=java.net.URLDecoder.decode(strava.body.readUtf8(),"UTF-8")
            assertTrue(body.contains("RPE: 0/10"));assertFalse(body.contains("perceived_exertion="))
            assertEquals(description,RideAnnotations.description(description,value))
            assertEquals(old,RideAnnotations.description(description,RideAnnotation()))
        } finally { server.shutdown() }
    }
}
