package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.garmin.fit.*
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class TrainingLibraryTest {
    private val zwo="""<workout_file><name>VO2max 5×5</name><description>Five five-minute efforts with recovery.</description><sportType>bike</sportType><workout><Warmup Duration="600" PowerLow="0.4" PowerHigh="0.6"/><IntervalsT Repeat="5" OnDuration="300" OffDuration="300" OnPower="1.15" OffPower="0.55"/><Cooldown Duration="300" PowerLow="0.6" PowerHigh="0.4"/></workout></workout_file>"""
    private fun fit(heart:Boolean=false,open:Boolean=false):ByteArray {
        val file=java.io.File.createTempFile("workout",".fit")
        val encoder=FileEncoder(file,Fit.ProtocolVersion.V2_0)
        encoder.write(FileIdMesg().apply { type=com.garmin.fit.File.WORKOUT;manufacturer=1;product=1;timeCreated=DateTime(java.util.Date()) })
        encoder.write(WorkoutMesg().apply { wktName="VO2max 5×5";wktDescription="Five efforts";sport=Sport.CYCLING;numValidSteps=5 })
        fun step(index:Int,seconds:Float,power:Long) { encoder.write(WorkoutStepMesg().apply { messageIndex=index;durationType=WktStepDuration.TIME;durationTime=seconds;targetType=WktStepTarget.POWER;targetValue=0;customTargetValueLow=power;customTargetValueHigh=power
            if(heart && index==1) { targetType=WktStepTarget.HEART_RATE;customTargetValueLow=220;customTargetValueHigh=240 }
            if(open && index==1) { durationType=WktStepDuration.DISTANCE;durationDistance=1000f }
        }) }
        step(0,600f,50);step(1,300f,115);step(2,300f,55)
        encoder.write(WorkoutStepMesg().apply { messageIndex=3;durationType=WktStepDuration.REPEAT_UNTIL_STEPS_CMPLT;durationStep=1;targetType=WktStepTarget.OPEN;repeatSteps=5 })
        step(4,300f,50);encoder.close()
        return file.readBytes().also { file.delete() }
    }
    @Test fun threeFormatsPreserveDurationsRampsRepeatsAndTargets() {
        val a=TrainingFileImport.read(zwo.toByteArray(),"test.zwo")
        assertEquals(3900.0,a.seconds!!,.001);assertEquals(12,a.steps.size);assertEquals(TrainingKind.INTERVALS,a.kind)
        assertTrue(a.steps.first().target!!.ramp);assertEquals(.4,a.steps.last().target!!.high,.001)
        val b=TrainingFileImport.read(fit(),"test.fit")
        assertEquals(3900.0,b.seconds!!,.001);assertEquals(12,b.steps.size)
        assertEquals(1.15,b.steps[1].target!!.low,.001);assertEquals(1.15,b.steps[9].target!!.low,.001)
        val erg="[COURSE HEADER]\nFILE NAME=Ausdauer\nMINUTES WATTS\n[COURSE DATA]\n0 100\n10 200\n10 150\n20 150\n[END COURSE DATA]"
        val c=TrainingFileImport.read(erg.toByteArray(),"test.erg")
        assertEquals(1200.0,c.seconds!!,.001);assertEquals(2,c.steps.size)
        assertEquals(TrainingUnit.WATTS,c.steps.first().target!!.unit);assertTrue(c.steps.first().target!!.ramp)
        assertEquals(150.0,c.steps.last().target!!.low,.001)
        val chart=TrainingWorkoutChart(RuntimeEnvironment.getApplication()).apply { show(c,RiderProfile());layout(0,0,200,100) }
        val bitmap=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888);chart.draw(Canvas(bitmap))
        assertEquals(0,android.graphics.Color.alpha(bitmap.getPixel(25,50)))
        assertTrue(android.graphics.Color.alpha(bitmap.getPixel(95,50))>0);bitmap.recycle()
        val profile=RiderProfile(ftpW=200)
        assertEquals(5,a.steps[1].target!!.zone(profile));assertEquals(TrainingZoneColors.orange,a.steps[1].target!!.color(profile))
        assertEquals(a,TrainingWorkout.decode(a.json()))
        val pulse=TrainingFileImport.read(fit(heart=true,open=true),"heart.fit")
        assertNull(pulse.seconds);assertEquals("DISTANCE",pulse.steps[1].durationType)
        assertEquals(TrainingUnit.HR_BPM,pulse.steps[1].target!!.unit)
        assertEquals(120.0,pulse.steps[1].target!!.low,.001);assertEquals(140.0,pulse.steps[1].target!!.high,.001)
    }
    @Test fun corruptOrUnsupportedFilesDoNotDamageTheAtomicCollectionAndDuplicateImportUpdates() {
        val root=Files.createTempDirectory("training-store").toFile()
        try {
            val store=TrainingStore(root);val workout=TrainingFileImport.read(zwo.toByteArray(),"test.zwo")
            store.merge(listOf(workout));store.merge(listOf(workout));assertEquals(1,store.read().size)
            for((bytes,name) in listOf("<!DOCTYPE a [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><workout_file/>".toByteArray() to "bad.zwo",fit().copyOf(30) to "bad.fit","[COURSE DATA]\n5 100\n0 200".toByteArray() to "bad.erg")) {
                try { store.merge(listOf(TrainingFileImport.read(bytes,name)));fail("Invalid workout must be rejected") } catch(_:Exception) { }
                assertEquals(listOf(workout),store.read())
            }
            store.merge(listOf(workout.copy(description="Updated")));assertEquals("Updated",TrainingStore(root).read().single().description)
            store.delete(setOf(workout.id));assertTrue(store.read().isEmpty())
        } finally { root.deleteRecursively() }
    }
    @Test fun intervalsLibraryReadAndFileConversionUseExistingAccountWithoutCalendarOrLibraryWrites() {
        val server=MockWebServer();server.start()
        try {
            val row=JSONObject().put("id",42).put("name","VO2max").put("description","5x5m 115%")
            server.enqueue(MockResponse().setBody("[$row]"));server.enqueue(MockResponse().setBody(Buffer().write(fit())))
            val client=IntervalsClient(IntervalsConfig(false,"i123",false),"test-key",base=server.url("/api/v1/"))
            val workouts=client.workoutLibrary();val imported=TrainingFileImport.read(client.workoutFit(workouts.single()),"cloud.fit")
            assertEquals(12,imported.steps.size)
            val get=server.takeRequest();assertEquals("GET",get.method);assertEquals("/api/v1/athlete/i123/workouts",get.path);assertEquals(Credentials.basic("API_KEY","test-key"),get.getHeader("Authorization"))
            val convert=server.takeRequest();assertEquals("POST",convert.method);assertEquals("/api/v1/athlete/i123/download-workout.fit",convert.path)
            assertEquals(42,JSONObject(convert.body.readUtf8()).getInt("id"));assertEquals(2,server.requestCount)
        } finally { server.shutdown() }
    }
    @Test fun menuFileImportFooterCommandsAndConfirmedDeleteWorkInTheRealTrainingScreen() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        val file=java.io.File.createTempFile("training-navigation",".zwo").apply { writeText(zwo) }
        try {
            val a=host.get()
            nodes(a.findViewById(android.R.id.content)).single { it.contentDescription==a.getString(R.string.ui_menu_open) }.performClick()
            org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("settings_TRAINING").performClick()
            val screen=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val manager=screen.window!!.decorView.findViewWithTag<TrainingManagerView>("training_manager")
            fun drain() {
                val repository=MainActivity::class.java.getDeclaredField("trainingRepositoryInstance").apply { isAccessible=true }.get(a)
                (TrainingRepository::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(repository) as java.util.concurrent.ExecutorService).submit {}.get(10,java.util.concurrent.TimeUnit.SECONDS)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val decor=screen.window!!.decorView;decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,800)
            }
            drain();screen.window!!.decorView.findViewWithTag<View>("training_import").performClick()
            val popup=MainActivity::class.java.getDeclaredField("routePopup").apply { isAccessible=true }.get(a) as android.widget.ListPopupWindow
            val choices=popup.listView!!
            assertEquals(TrainingImportAction.entries.size,choices.adapter.count)
            choices.performItemClick(choices.getChildAt(1),1,1)
            assertEquals(21,org.robolectric.Shadows.shadowOf(a).nextStartedActivityForResult.requestCode)
            MainActivity::class.java.getDeclaredMethod("onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,android.content.Intent::class.java).invoke(a,21,Activity.RESULT_OK,android.content.Intent().setData(android.net.Uri.fromFile(file)))
            drain()
            val list=manager.findViewWithTag<ListView>("training_list");assertEquals(2,list.adapter.count)
            list.onItemLongClickListener!!.onItemLongClick(list,list.getChildAt(1),1,1);screen.window!!.decorView.findViewWithTag<View>("training_delete").performClick()
            val confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            confirm.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();drain()
            assertEquals(0,list.adapter.count)
        } finally { file.delete();host.pause().stop().destroy() }
    }
    @Test fun cloudImportCommitsOnlyCompleteBatchesAndCancellationKeepsTheCollection() {
        val context=RuntimeEnvironment.getApplication()
        val aes=javax.crypto.spec.SecretKeySpec(ByteArray(32) { 7 },"AES")
        val settings=IntervalsSettings(context) { aes }.apply { write(IntervalsConfig(false,"i123",false),"test-key") }
        val server=MockWebServer();server.start()
        val repository=TrainingRepository(context,settings,network={true},api={ c,k,stop -> IntervalsClient(c,k,stop,server.url("/api/v1/")) })
        fun drain() {
            (TrainingRepository::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(repository) as java.util.concurrent.ExecutorService).submit {}.get(10,java.util.concurrent.TimeUnit.SECONDS)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
        try {
            val store=TrainingStore(java.io.File(context.filesDir,"training-workouts"))
            server.enqueue(MockResponse().setBody("[{\"id\":42,\"name\":\"VO2max\"}]"))
            server.enqueue(MockResponse().setBody("{\"id\":42,\"name\":\"VO2max\"}"));server.enqueue(MockResponse().setBody(Buffer().write(fit())))
            var result:Result<List<TrainingWorkout>>?=null
            repository.download({false},{_,_->}) { result=it };drain()
            assertTrue(result!!.isSuccess);val saved=store.read();assertEquals(1,saved.size)
            server.enqueue(MockResponse().setBody("[{\"id\":42,\"name\":\"Updated\"},{\"id\":43,\"name\":\"Broken\"}]"))
            server.enqueue(MockResponse().setBody("{\"id\":42,\"name\":\"Updated\"}"));server.enqueue(MockResponse().setBody(Buffer().write(fit())))
            server.enqueue(MockResponse().setBody("{\"id\":43,\"name\":\"Broken\"}"));server.enqueue(MockResponse().setBody("invalid-fit"))
            repository.download({false},{_,_->}) { result=it };drain()
            assertTrue(result!!.isFailure);assertEquals(saved,store.read())
            val requests=server.requestCount
            repository.download({true},{_,_->}) { result=it };drain()
            assertTrue(result!!.isFailure);assertEquals(requests,server.requestCount);assertEquals(saved,store.read())
        } finally { repository.close();server.shutdown() }
    }
    private fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    @Test fun groupedCollectionShowsColoredStepsAndHasImportDisabledSortFilterAndSelectionDelete() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));var deleted=emptySet<String>();var imports=0
            val manager=TrainingManagerView(a,themes,{ RiderProfile(ftpW=200) },{ imports++ },{ deleted=it })
            val interval=TrainingFileImport.read(zwo.toByteArray(),"interval.zwo")
            manager.render(listOf(interval,interval.copy(id="a".repeat(64),name="Ausdauer",tags=listOf("endurance"),steps=listOf(TrainingStep("steady",3600.0,listOf(TrainingTarget(TrainingUnit.FTP,.65)))))))
            a.setContentView(manager)
            fun layout() { manager.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(780,View.MeasureSpec.EXACTLY));manager.layout(0,0,360,780) }
            layout()
            val list=manager.findViewWithTag<ListView>("training_list")
            assertEquals(3,list.adapter.count);assertEquals(View.GONE,manager.status.visibility);assertFalse(manager.findViewWithTag<View>("training_filter").isEnabled);assertTrue(manager.findViewWithTag<View>("training_sort").isEnabled)
            manager.findViewWithTag<View>("training_import").performClick();assertEquals(1,imports)
            val group=list.adapter.getView(0,null,list)
            assertTrue(nodes(group).filterIsInstance<TextView>().any { it.text.toString()=="Trainings" })
            assertEquals("2",nodes(group).filterIsInstance<TextView>().single { it.tag?.toString()?.startsWith("route_training_group_count_")==true }.text.toString())
            list.performItemClick(list.getChildAt(1),1,1)
            assertTrue(manager.findViewWithTag<View>("training_delete").isEnabled);manager.findViewWithTag<View>("training_delete").performClick();assertEquals(setOf("a".repeat(64)),deleted)
            val image=Bitmap.createBitmap(360,780,Bitmap.Config.ARGB_8888);manager.draw(Canvas(image))
            java.io.File("build/reports/ui/training-library.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            assertTrue(nodes(manager).filterIsInstance<TrainingWorkoutView>().any { it.contentDescription.toString().contains("60,0") || it.contentDescription.toString().contains("60.0") })
            manager.clearSelection()
            manager.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));manager.layout(0,0,360,400)
            list.setSelectionFromTop(1,-20)
            manager.forceLayout();manager.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));manager.layout(0,0,360,400)
            val pinned=Bitmap.createBitmap(list.width,list.height,Bitmap.Config.ARGB_8888);list.draw(Canvas(pinned))
            assertEquals(themes.active.tokens.tertiary,pinned.getPixel(100,4));pinned.recycle()
            for(action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
                val event=android.view.MotionEvent.obtain(0,20,action,100f,10f,0);list.dispatchTouchEvent(event);event.recycle()
            }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertFalse("Pinned group header must not select the covered training",manager.selecting)
        } finally { host.pause().stop().destroy() }
    }
}
