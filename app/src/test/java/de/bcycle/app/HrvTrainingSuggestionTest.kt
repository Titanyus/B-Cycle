package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class HrvTrainingSuggestionTest {
    private val profile=RiderProfile(restingHeartRate=50,maximumHeartRate=200)
    private fun analysis(id:String,bpm:Double,time:Long):RouteAnalysis {
        val histogram=RouteTimeHistogram(250).apply { observe(bpm);add(bpm,time) }
        return RouteAnalysis(ManagedRoute(id,id,true,"FIT",RouteDetails(),false,TrackOrigin.RECORDED),emptyMap(),null,false,1.0,emptyList(),
            distributions=listOfNotNull(histogram.result(AnalysisChart.HEART,profile)))
    }
    @Test fun averageWeightsRealHeartDurationsAndUsesTheRidersFiveZones() {
        val summary=HrvTrainingSummary(listOf("one","two"),50.0,120.0)
        val zones=profile.heartZones()!!.zones
        for(zone in zones) {
            val bpm=(zone.lowerBpm+zone.upperExclusiveBpm-1)/2.0
            val result=summary.withHeart(profile,listOf(analysis("one",bpm.toInt().toDouble(),10000)))
            assertEquals(if(zone.index==1) 1 else if(zone.index<=3) 2 else 3,result.intensity)
        }
        val easy=zones.first().lowerBpm.toDouble()
        val hard=(zones.last().lowerBpm+zones.last().upperExclusiveBpm-1)/2.0
        assertEquals(1,summary.withHeart(profile,listOf(analysis("one",easy,3600000),analysis("two",hard.toInt().toDouble(),1000))).intensity)
        assertNull(summary.withHeart(profile,emptyList()).intensity)
        assertNull(summary.withHeart(RiderProfile(),listOf(analysis("one",150.0,60000))).intensity)
        assertEquals(0,HrvTrainingSummary(emptyList(),null,null).withHeart(profile,emptyList()).intensity)
    }
    @Test fun savedFitIsReadOnTheExistingRouteWorkerWithoutChangingTheDisplayedRoute() {
        val c=org.robolectric.RuntimeEnvironment.getApplication()
        RiderProfileStore(c.getSharedPreferences("bike",0)).write(profile)
        val start=java.time.Instant.parse("2026-10-29T10:00:00Z").toEpochMilli()
        val journal=java.io.File(c.cacheDir,"training-suggestion.ride")
        RideJournal(journal,start,RecordingConfig(setOf("HEART_RATE"))).use { writer ->
            for(second in 0..60) writer.append(RideSample(start+second*1000,second*1000L,values=mapOf("HEART_RATE" to 150.0)))
        }
        val fit=java.io.File(c.cacheDir,"training-suggestion.fit");FitWriter.encode(journal,fit)
        val saved=FitLibrary.retainRecording(c.filesDir,fit);val before=saved.readBytes()
        val state=MapState(c) {}
        val worker=MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as java.util.concurrent.ExecutorService
        fun drain() { worker.submit {}.get(10,java.util.concurrent.TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle() }
        try {
            drain();val route=state.route;val selected=state.selectedRoute
            var result:HrvTrainingSummary?=null
            state.loadHrvTraining(java.time.LocalDate.of(2026,10,30),java.time.ZoneId.of("Europe/Berlin")) { result=it }
            drain();assertEquals(listOf(saved.nameWithoutExtension),result!!.rideIds);assertEquals(2,result!!.intensity)
            assertSame(route,state.route);assertEquals(selected,state.selectedRoute);assertArrayEquals(before,saved.readBytes())
        } finally { worker.shutdownNow() }
    }
    @Test fun detectedTrainingPrefillsSelectionAndAmountsButManualRestRemovesAllExerciseInputs() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));val saved=mutableListOf<HrvQuestionnaire>()
            val form=HrvQuestionnaireView(c,themes,HrvQuestionnaire("2026-10-30"),HrvTrainingSummary(listOf("ride"),40.0,100.0,2)) { value,done -> saved+=value;done(true) }
            c.setContentView(form);form.flush()
            assertEquals(2,saved.last().training);assertEquals(40.0,saved.last().trainingKm!!,0.0)
            assertNotNull(form.findViewWithTag<View>("hrvq_rpe"));assertTrue(saved.last().ratings.containsKey("rpe"))
            form.findViewWithTag<View>("hrvq_training_0").performClick();form.flush()
            assertEquals(0,saved.last().training);assertNull(saved.last().trainingKm);assertNull(saved.last().trainingMinutes)
            assertTrue(saved.last().rideIds.isEmpty());assertFalse(saved.last().ratings.containsKey("rpe"))
            assertNull(form.findViewWithTag<View>("hrvq_rpe"));assertNull(form.findViewWithTag<EditText>("hrvq_distance"))
            form.findViewWithTag<View>("hrvq_training_1").performClick();form.flush()
            assertEquals(1,saved.last().training);assertEquals(40.0,saved.last().trainingKm!!,0.0)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun subjectiveEffortStaysHiddenWithoutEvidenceEvenIfTheTrainingQuestionIsDisabled() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            HrvQuestionSettings(c).write(HrvQuestionConfig(setOf(HrvQuestion.RPE,HrvQuestion.DISTANCE)))
            val empty=HrvQuestionnaireView(c,themes,HrvQuestionnaire("2026-10-30"),null) { _,done -> done(true) }
            c.setContentView(empty);assertNull(empty.findViewWithTag<View>("hrvq_rpe"))
            val detected=HrvQuestionnaireView(c,themes,HrvQuestionnaire("2026-10-30"),HrvTrainingSummary(listOf("ride"),10.0,30.0)) { _,done -> done(true) }
            c.setContentView(detected);assertNotNull(detected.findViewWithTag<View>("hrvq_rpe"))
            val rest=HrvQuestionnaireView(c,themes,HrvQuestionnaire("2026-10-30",training=0),HrvTrainingSummary(listOf("ride"),10.0,30.0)) { _,done -> done(true) }
            c.setContentView(rest);assertNull(rest.findViewWithTag<View>("hrvq_rpe"))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun existingDayAnswerWinsAndNoRidePreselectsRestWithoutInventingAnEffort() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));val saved=mutableListOf<HrvQuestionnaire>()
            val existing=HrvQuestionnaireView(c,themes,HrvQuestionnaire("2026-10-30",training=1),HrvTrainingSummary(listOf("ride"),20.0,50.0,3)) { value,done -> saved+=value;done(true) }
            c.setContentView(existing);existing.flush();assertEquals(1,saved.last().training)
            val empty=HrvQuestionnaireView(c,themes,HrvQuestionnaire("2026-10-30"),HrvTrainingSummary(emptyList(),null,null)) { value,done -> saved+=value;done(true) }
            c.setContentView(empty);empty.flush();assertEquals(0,saved.last().training);assertNull(empty.findViewWithTag<View>("hrvq_rpe"))
            assertFalse(saved.last().ratings.containsKey("rpe"))
        } finally { shadowOf(Looper.getMainLooper()).idle();host.pause().stop().destroy() }
    }
}
