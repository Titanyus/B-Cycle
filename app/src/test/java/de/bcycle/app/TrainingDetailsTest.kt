package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class TrainingDetailsTest {
    private fun original()=JSONObject("""{
        "id":42,"name":"Rampenfahrt","description":"Warm up and cool down","type":"Ride","moving_time":600,
        "icu_training_load":23.5,"updated":"2026-11-06T15:00:00Z","tags":["test"],"folder_id":4,
        "future_field":{"enabled":false,"optional":null},
        "workout_doc":{"steps":[
            {"duration":300,"ramp":true,"power":{"start":40,"end":120,"units":"%ftp"}},
            {"duration":300,"ramp":true,"power":{"start":120,"end":40,"units":"%ftp"}}
        ]}}""")
    private fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    private fun drain(repository:TrainingRepository) {
        (TrainingRepository::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(repository) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }
    @Test fun nativeRampDirectionSurvivesCloudStorageAndIsDrawnAsASlopeWithoutFitConversion() {
        val context=RuntimeEnvironment.getApplication();val server=MockWebServer();server.start()
        val settings=IntervalsSettings(context) { javax.crypto.spec.SecretKeySpec(ByteArray(32) { 7 },"AES") }.apply { write(IntervalsConfig(false,"i123",false),"test-key") }
        val repository=TrainingRepository(context,settings,network={true},api={ c,k,stop -> IntervalsClient(c,k,stop,server.url("/api/v1/")) })
        try {
            val row=original();server.enqueue(MockResponse().setBody("[{\"id\":42,\"name\":\"Summary only\"}]"));server.enqueue(MockResponse().setBody(row.toString()))
            var result:Result<List<TrainingWorkout>>?=null
            repository.download({false},{_,_->}) { result=it };drain(repository)
            assertTrue(result.toString(),result!!.isSuccess)
            assertEquals("Library and complete details, no lossy FIT conversion",2,server.requestCount)
            assertEquals("GET",server.takeRequest().method)
            val detailRequest=server.takeRequest();assertEquals("GET",detailRequest.method);assertEquals("/api/v1/athlete/i123/workouts/42",detailRequest.path)
            val workout=TrainingStore(File(context.filesDir,"training-workouts")).read().single()
            assertEquals("intervals-native",workout.structureSource);assertEquals(600.0,workout.seconds!!,0.0)
            assertEquals(.4,workout.steps[0].target!!.low,.001);assertEquals(1.2,workout.steps[0].target!!.high,.001)
            assertEquals(1.2,workout.steps[1].target!!.low,.001);assertEquals(.4,workout.steps[1].target!!.high,.001)
            assertTrue(workout.steps.all { it.target!!.ramp })
            val preserved=JSONObject(workout.sourceData!!)
            assertEquals(row.keys().asSequence().toSet(),preserved.keys().asSequence().toSet())
            assertFalse(preserved.getJSONObject("future_field").getBoolean("enabled"));assertTrue(preserved.getJSONObject("future_field").isNull("optional"))
            assertEquals(23.5,preserved.getDouble("icu_training_load"),.001)
            val chart=TrainingWorkoutChart(context).apply { show(workout,RiderProfile(ftpW=200));layout(0,0,200,100) }
            val image=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888);chart.draw(Canvas(image))
            assertEquals(0,Color.alpha(image.getPixel(15,50)));assertTrue(Color.alpha(image.getPixel(85,50))>0)
            assertTrue(Color.alpha(image.getPixel(115,50))>0);assertEquals(0,Color.alpha(image.getPixel(185,50)));image.recycle()
        } finally { repository.close();server.shutdown() }
    }
    @Test fun targetRangesStayFlatAndNativeRepeatsAndPulseUnitsArePreserved() {
        val row=original();row.getJSONObject("workout_doc").put("steps",JSONArray("""[
            {"duration":300,"power":{"value":60,"max_value":80,"units":"%ftp"}},
            {"reps":2,"steps":[{"duration":30,"hr":{"start":60,"end":90,"units":"%hr"},"ramp":true},{"duration":30,"hr":{"value":75,"units":"%lthr"}}]}
        ]"""))
        val workout=IntervalsTraining.read(row)!!
        assertEquals(5,workout.steps.size);assertEquals(420.0,workout.seconds!!,.001)
        assertFalse(workout.steps[0].target!!.ramp);assertEquals(.6,workout.steps[0].target!!.low,.001)
        assertEquals(TrainingUnit.HR_MAX_PERCENT,workout.steps[1].target!!.unit);assertTrue(workout.steps[1].target!!.ramp)
        assertEquals(TrainingUnit.HR_THRESHOLD_PERCENT,workout.steps[2].target!!.unit)
        val legacy=workout.json().apply { remove("intervalsData");remove("structureSource") }
        assertNull(TrainingWorkout.decode(legacy).sourceData)
    }
    @Test fun detailsListOriginalFieldsAndNestedUnknownFieldsWithoutDroppingPagedEntries() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));val row=original()
            val workout=IntervalsTraining.read(row)!!
            val view=TrainingDetailsView(a,themes,workout,RiderProfile(ftpW=200),row)
            a.setContentView(view)
            assertTrue(nodes(view).filterIsInstance<TextView>().any { it.text.toString().contains("Rampe: 40") })
            val future=view.findViewWithTag<BicycleFrameDropdown>("training_original_future_field");future.setExpanded(true)
            val raw=nodes(future).filterIsInstance<TextView>().map { it.text.toString() }
            assertTrue(raw.containsAll(listOf("enabled","false","optional","null")))
            val fields=TrainingJsonView(a,themes,JSONArray((0..69).toList()))
            repeat(2) { nodes(fields).filterIsInstance<android.widget.Button>().last { it.text==a.getString(R.string.training_details_more_fields) }.performClick() }
            assertTrue(nodes(fields).filterIsInstance<TextView>().any { it.text.toString()=="69" })
        } finally { host.pause().stop().destroy() }
    }
    @Test fun aTrainingTapOpensDetailsAndLongPressStillSelectsForDelete() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val workout=IntervalsTraining.read(original())!!
            TrainingStore(File(a.filesDir,"training-workouts")).merge(listOf(workout))
            MainActivity::class.java.getDeclaredMethod("showTraining").apply { isAccessible=true }.invoke(a)
            val managerScreen=ShadowAlertDialog.getLatestAlertDialog()
            val repository=MainActivity::class.java.getDeclaredField("trainingRepositoryInstance").apply { isAccessible=true }.get(a) as TrainingRepository
            drain(repository)
            val manager=managerScreen.window!!.decorView.findViewWithTag<TrainingManagerView>("training_manager")
            fun layout(view:View) { view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));view.layout(0,0,360,800) }
            layout(managerScreen.window!!.decorView)
            val list=manager.findViewWithTag<ListView>("training_list")
            list.performItemClick(list.getChildAt(1),1,1);drain(repository)
            val details=ShadowAlertDialog.getLatestAlertDialog();assertNotSame(managerScreen,details)
            assertNotNull(details.window!!.decorView.findViewWithTag<View>("training_detail_view"));assertFalse(manager.selecting)
            layout(details.window!!.decorView)
            val picture=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888);details.window!!.decorView.draw(Canvas(picture))
            File("build/reports/ui/training-details.png").apply { parentFile!!.mkdirs() }.outputStream().use { picture.compress(Bitmap.CompressFormat.PNG,100,it) };picture.recycle()
            details.dismiss()
            list.onItemLongClickListener!!.onItemLongClick(list,list.getChildAt(1),1,1)
            assertTrue(manager.selecting);assertTrue(managerScreen.window!!.decorView.findViewWithTag<View>("training_delete").isEnabled)
        } finally { host.pause().stop().destroy() }
    }
}
