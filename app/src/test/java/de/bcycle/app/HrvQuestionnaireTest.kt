package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import de.bcycle.hrv.*
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
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvQuestionnaireTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private fun layout(view:View) {
        view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(760,View.MeasureSpec.EXACTLY));view.layout(0,0,360,760)
    }
    @Test fun previousCalendarDaySumsRiddenTracksAcrossProfilesAndHandlesDstAndUnknownMetrics() {
        val zone=ZoneId.of("Europe/Berlin");val day=LocalDate.of(2026,10,26)
        val start=day.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();val end=day.atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(25*60*60*1000L,end-start)
        fun ride(id:String,date:Long,distance:Double?=10000.0,moving:Long?=1800000,origin:TrackOrigin=TrackOrigin.RECORDED)=
            ManagedRoute(id,id,true,"FIT",RouteDetails(date,distance,3600000,movingMs=moving),true,origin)
        val rides=listOf(ride("a",start),ride("a",start),ride("b",end-1,20000.0,null),ride("planned",start,origin=TrackOrigin.IMPORTED),ride("today",end),ride("old",start-1))
        val sum=HrvTrainingSummary.previousDay(day,zone,rides)
        assertEquals(listOf("a","b"),sum.rideIds);assertEquals(30.0,sum.km!!,0.0);assertEquals(90.0,sum.minutes!!,0.0)
        assertNull(HrvTrainingSummary.previousDay(day,zone,listOf(ride("unknown",start,null))).km)
        assertNull(HrvTrainingSummary.previousDay(day,zone,emptyList()).minutes)
    }
    @Test fun unansweredCenterIsNotSavedUntilChosenAndTrainingAmountsStayEditableSideBySide() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));val saved=mutableListOf<HrvQuestionnaire>()
            val view=HrvQuestionnaireView(a,themes,HrvQuestionnaire("2026-10-21"),HrvTrainingSummary(listOf("ride-a","ride-b"),42.5,95.0)) { value,done->saved+=value;done(true) }
            val scroll=android.widget.ScrollView(a).apply { addView(view) };a.setContentView(scroll);layout(scroll)
            val slider=view.findViewWithTag<SeekBar>("hrvq_sleep")
            val block=view.findViewWithTag<LinearLayout>("hrvq_block_sleep")
            assertEquals(BicycleDesign.dp(a,24),(block.layoutParams as LinearLayout.LayoutParams).bottomMargin)
            assertEquals(3,block.childCount) // Question, slider, scale; no value/missing status or reset action.
            for(index in 0..2) {
                val scale=view.findViewWithTag<View>("hrvq_scale_sleep_$index")
                assertEquals(.5f,scale.alpha,0f)
                assertEquals(BicycleDesign.dp(a,4),(scale.layoutParams as LinearLayout.LayoutParams).topMargin)
            }
            assertEquals(8,slider.max);assertEquals(4,slider.progress);view.flush();assertTrue(saved.isEmpty())
            assertEquals(View.GONE,view.findViewWithTag<View>("hrvq_training_details").visibility)
            // A tap that chooses the existing center must count as an explicit neutral answer.
            slider.dispatchTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,slider.width/2f,slider.height/2f,0))
            slider.dispatchTouchEvent(android.view.MotionEvent.obtain(0,10,android.view.MotionEvent.ACTION_UP,slider.width/2f,slider.height/2f,0))
            view.flush();assertEquals(4,saved.last().ratings["sleep"])
            view.findViewWithTag<View>("hrvq_training_2").performClick();view.flush();layout(scroll)
            assertEquals(42.5,saved.last().trainingKm!!,0.0);assertEquals(95.0,saved.last().trainingMinutes!!,0.0)
            assertEquals("rides",saved.last().trainingSource);assertEquals(10,view.findViewWithTag<SeekBar>("hrvq_rpe").max)
            shadowOf(Looper.getMainLooper()).idle();layout(scroll)
            val image=android.graphics.Bitmap.createBitmap(360,760,android.graphics.Bitmap.Config.ARGB_8888)
            image.eraseColor(themes.active.tokens.secondary);scroll.draw(android.graphics.Canvas(image))
            java.io.File("build/reports/ui/hrv-questionnaire.png").apply { parentFile?.mkdirs() }.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            val row=view.findViewWithTag<LinearLayout>("hrvq_training_amounts");assertEquals(2,row.childCount);assertEquals(LinearLayout.HORIZONTAL,row.orientation)
            view.findViewWithTag<EditText>("hrvq_distance").setText("43,2");view.flush()
            assertEquals(43.2,saved.last().trainingKm!!,0.0);assertEquals("manual",saved.last().trainingSource)
            view.findViewWithTag<EditText>("hrvq_distance").setText("-");view.flush();assertEquals(43.2,saved.last().trainingKm!!,0.0)
            // Hiding a question dynamically must not discard its answer or leave its group behind.
            HrvQuestionSettings(a).write(HrvQuestionConfig(enabled=setOf(HrvQuestion.DURATION)))
            shadowOf(Looper.getMainLooper()).idle();layout(scroll)
            assertNull(view.findViewWithTag<View>("hrvq_sleep"));assertNotNull(view.findViewWithTag<View>("hrvq_duration"))
            assertEquals(1,view.findViewWithTag<LinearLayout>("hrvq_training_amounts").childCount)
            view.findViewWithTag<EditText>("hrvq_duration").setText("100");view.flush();assertEquals(4,saved.last().ratings["sleep"])
        } finally { host.pause().stop().destroy() }
    }
    @Test fun manualTrainingCustomFieldsAndCycleFollowConfigurationWithoutInventingDefaults() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val settings=HrvQuestionSettings(a)
            settings.write(HrvQuestionConfig(setOf(HrvQuestion.TRAINING,HrvQuestion.DISTANCE,HrvQuestion.DURATION,HrvQuestion.CYCLE),listOf("Kaffee","","")))
            val prefs=a.getSharedPreferences("bike",0);val rider=RiderProfileStore(prefs)
            rider.write(RiderProfile(sex=RiderSex.MALE));val saved=mutableListOf<HrvQuestionnaire>()
            val view=HrvQuestionnaireView(a,ThemeManager(prefs),HrvQuestionnaire("2026-10-21"),HrvTrainingSummary(emptyList(),null,null)) { value,done->saved+=value;done(true) }
            a.setContentView(view);layout(view)
            assertNull(view.findViewWithTag<View>("hrvq_cycle"));assertNotNull(view.findViewWithTag<View>("hrvq_custom1"))
            assertNull(view.findViewWithTag<View>("hrvq_custom2"))
            view.findViewWithTag<View>("hrvq_training_1").performClick();view.flush();assertNull(saved.last().trainingKm)
            view.findViewWithTag<EditText>("hrvq_distance").setText("12.5");view.findViewWithTag<EditText>("hrvq_duration").setText("50")
            view.findViewWithTag<EditText>("hrvq_custom1").setText("2");view.flush()
            assertEquals("Kaffee",saved.last().customNames["custom1"]);assertEquals("2.0",saved.last().extras["custom1"])
            rider.write(rider.read().copy(sex=RiderSex.FEMALE));shadowOf(Looper.getMainLooper()).idle();layout(view)
            assertNotNull(view.findViewWithTag<View>("hrvq_cycle"))
            assertEquals(12.5,saved.last().trainingKm!!,0.0);assertEquals(50.0,saved.last().trainingMinutes!!,0.0)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun settingsAccordionAndRiderSexRoundTripInBackupAndPreserveOtherProfileInputs() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val rider=RiderProfileStore(prefs)
            assertNull(rider.read().sex)
            rider.write(RiderProfile(weightKg=76.0,sex=RiderSex.FEMALE))
            val themes=ThemeManager(prefs);val settings=HrvQuestionSettings(a)
            settings.write(HrvQuestionConfig(setOf(HrvQuestion.SLEEP,HrvQuestion.CYCLE),listOf("Kaffee","","")))
            val screen=HrvSettingsView(a,themes);a.setContentView(screen);layout(screen)
            val body=screen.findViewWithTag<View>("hrvq_question_settings")
            assertEquals(View.GONE,body.visibility)
            val heading=(0 until screen.childCount).map(screen::getChildAt).filterIsInstance<BicycleSectionHeader>().single()
            heading.performClick();assertEquals(View.VISIBLE,body.visibility)
            val cycle=screen.findViewWithTag<android.widget.CheckBox>("hrvq_enabled_CYCLE")
            assertTrue(cycle.isEnabled);assertTrue(cycle.isChecked)
            val backup=SettingsBackup(a,themes);val bytes=backup.export(setOf(BackupSection.PROFILE))
            rider.write(rider.read().copy(sex=RiderSex.MALE));shadowOf(Looper.getMainLooper()).idle();assertFalse(cycle.isEnabled);assertFalse(cycle.isChecked);settings.write(HrvQuestionConfig(emptySet()))
            val document=backup.decode(bytes);backup.apply(backup.plan(document,document.sections.keys))
            assertEquals(RiderSex.FEMALE,rider.read().sex);assertEquals(76.0,rider.read().weightKg!!,0.0)
            assertEquals(listOf("Kaffee","",""),settings.read().customNames);assertEquals(setOf(HrvQuestion.SLEEP,HrvQuestion.CYCLE),settings.read().enabled)
            val profile=RiderProfileView(a,themes,rider.read(),DialogNavigation()).apply { persist=rider::write }
            a.setContentView(profile);layout(profile)
            profile.findViewWithTag<View>("rider_sex_MALE").performClick()
            assertEquals(RiderSex.MALE,rider.read().sex);assertEquals(76.0,rider.read().weightKg!!,0.0)
            val male=HrvQuestionSettingsView(a,themes)
            assertFalse(male.findViewWithTag<android.widget.CheckBox>("hrvq_enabled_CYCLE").isEnabled)
            assertTrue(HrvQuestion.CYCLE in settings.read().enabled) // Configuration/history retained while unavailable.
        } finally { host.pause().stop().destroy() }
    }
    @Test fun savedMeasurementButtonOpensEditableQuestionnaireAndWorkerPersistsIt() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a);val worker=HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(runtime.hrv) as java.util.concurrent.ThreadPoolExecutor
            fun drain() { worker.submit {}.get(10,java.util.concurrent.TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle() }
            drain()
            val store=HrvStore(File(a.filesDir,"hrv-measurements"));val id=UUID.randomUUID().toString();val session=MeasurementSession(0)
            store.begin(id,java.time.Instant.parse("2026-10-21T07:00:00Z").toEpochMilli(),"Gurt",session);session.abort("USER_ABORT")
            val row=store.save(id,session,Evaluate.session(session),0)
            HrvController::class.java.getDeclaredField("state").apply { isAccessible=true }.set(runtime.hrv,HrvState(result=row))
            val view=HrvMeasurementView(a,runtime.themes,runtime.hrv,{"Gurt"},{true},{},questionnaire={ result ->
                MainActivity::class.java.getDeclaredMethod("showHrvQuestionnaire",JSONObject::class.java).apply { isAccessible=true }.invoke(a,result)
            })
            a.setContentView(view);layout(view)
            val button=view.findViewWithTag<View>("hrv_questionnaire_button");assertEquals(View.VISIBLE,button.visibility);button.performClick();drain()
            val dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val form=dialog.window!!.decorView.findViewWithTag<HrvQuestionnaireView>("hrv_questionnaire")
            assertNotNull(form)
            form.findViewWithTag<EditText>("hrvq_notes").setText("Heute gut erholt");form.flush();drain()
            assertEquals("Heute gut erholt",store.questionnaire(id)!!.notes)
            val decor=dialog.window!!.decorView;layout(decor);shadowOf(Looper.getMainLooper()).idle();layout(decor)
            val image=android.graphics.Bitmap.createBitmap(360,760,android.graphics.Bitmap.Config.ARGB_8888)
            image.eraseColor(runtime.themes.active.tokens.secondary);decor.draw(android.graphics.Canvas(image))
            java.io.File("build/reports/ui/hrv-questionnaire-dialog.png").apply { parentFile?.mkdirs() }.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            form.findViewWithTag<View>("hrvq_analysis").performClick();drain();assertFalse(dialog.isShowing)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun annotationRoundTripsThroughAnalysisAndBackupWhileOriginalMeasurementStaysByteIdentical() {
        val source=File(c.cacheDir,"hrvq-${UUID.randomUUID()}");val target=File(c.cacheDir,"hrvq-${UUID.randomUUID()}")
        try {
            val store=HrvStore(source);val id=UUID.randomUUID().toString();val session=MeasurementSession(0)
            store.begin(id,java.time.Instant.parse("2026-10-21T07:00:00Z").toEpochMilli(),"Gurt",session)
            session.abort("USER_ABORT");store.save(id,session,Evaluate.session(session),0)
            val before=File(source,"$id/result.json").readBytes();val legacy=store.backup(id)
            val annotations=HrvQuestionnaire.forMeasurement(store.read(id)).copy(ratings=mapOf("sleep" to 4),alcohol=1,training=2,trainingKm=25.5,trainingMinutes=60.0,trainingSource="manual",extras=mapOf("custom1" to "2.0"),customNames=mapOf("custom1" to "Kaffee"))
            store.saveQuestionnaire(id,annotations)
            assertArrayEquals(before,File(source,"$id/result.json").readBytes());assertEquals(annotations,HrvStore(source).questionnaire(id))
            val analysis=JSONObject(store.analysisJson(id).toString(Charsets.UTF_8))
            assertEquals(annotations.json().toString(),analysis.getJSONObject("questionnaire").toString())
            assertEquals(JSONObject(before.toString(Charsets.UTF_8)).toString(),analysis.getJSONObject("result").toString())
            val backup=store.backup(id);val restored=HrvStore(target)
            assertTrue(restored.restore(backup));assertFalse(restored.restore(backup));assertEquals(annotations,restored.questionnaire(id))
            assertArrayEquals(backup,restored.backup(id))
            val third=File(c.cacheDir,"hrvq-${UUID.randomUUID()}")
            try { val old=HrvStore(third);assertTrue(old.restore(legacy));assertNull(old.questionnaire(id));assertArrayEquals(legacy,old.backup(id)) }
            finally { third.deleteRecursively() }
            assertThrows(IllegalArgumentException::class.java) { store.saveQuestionnaire(id,annotations.copy(day="2026-10-20")) }
            assertThrows(IllegalArgumentException::class.java) { annotations.copy(ratings=mapOf("sleep" to 9)).json() }
            assertArrayEquals(before,File(source,"$id/result.json").readBytes())
        } finally { source.deleteRecursively();target.deleteRecursively() }
    }
}
