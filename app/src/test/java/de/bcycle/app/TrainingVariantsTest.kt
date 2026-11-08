package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrainingVariantsTest {
    private val zwo="""<workout_file><name>Zone 3 Crusher</name><description>FTP intervals. This is your 20%.</description><workout><Warmup Duration="900" PowerLow="0.4" PowerHigh="0.7"/><IntervalsT Repeat="5" OnDuration="180" OffDuration="180" OnPower="1.15" OffPower="0.55" Cadence="95" CadenceResting="80"/><Cooldown Duration="300" PowerLow="0.6" PowerHigh="0.4"/></workout></workout_file>"""
    private fun workout()=TrainingFileImport.read(zwo.toByteArray(),"intervals.zwo")
    private fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    @Test fun variantsChangeOnlyTheRepeatedMainSetAndRecomputeActualMetrics() {
        val base=workout();val original=base.json().toString();val profile=RiderProfile(ftpW=200)
        assertEquals(TrainingVariant.entries,TrainingVariants.available(base))
        val expected=listOf(2280.0,2640.0,3000.0,3360.0,3720.0)
        val loads=ArrayList<Double>()
        TrainingVariant.entries.forEachIndexed { i,variant ->
            val changed=TrainingVariants.apply(base,variant)
            assertEquals(expected[i],changed.seconds!!,.001)
            assertEquals(8+i*2,changed.steps.size)
            assertEquals(base.steps.first(),changed.steps.first());assertEquals(base.steps.last(),changed.steps.last())
            assertEquals(base.steps[1].targets,changed.steps[1].targets)
            assertEquals(base.id,changed.id);assertEquals(base.sourceData,changed.sourceData)
            val values=TrainingMetrics.of(changed,profile)
            assertEquals(expected[i],values.seconds!!,.001);assertEquals(changed.steps.size,values.sections)
            loads+=values.load!!
        }
        assertTrue(loads.zipWithNext().all { (a,b)->b>a })
        assertEquals(original,base.json().toString())
        val cloud=IntervalsTraining.read(JSONObject("""{"id":42,"name":"Repeated ramps","workout_doc":{"steps":[{"duration":900,"power":{"value":50,"units":"%ftp"}},{"reps":5,"steps":[{"duration":180,"ramp":true,"power":{"start":90,"end":120,"units":"%ftp"}},{"duration":180,"power":{"value":55,"units":"%ftp"}}]},{"duration":300,"power":{"value":40,"units":"%ftp"}}]}}"""))!!
        val shortened=TrainingVariants.apply(cloud,TrainingVariant.QUICK_PLUS)
        assertEquals(2280.0,shortened.seconds!!,.001);assertTrue(shortened.steps[1].target!!.ramp)
        assertEquals(cloud.sourceData,shortened.sourceData)
    }
    @Test fun ambiguousOrNonRepeatingTrainingStaysStandardAndLimitsAreRespected() {
        val base=workout()
        val steady=base.copy(steps=List(12) { TrainingStep("steady",180.0,listOf(TrainingTarget(TrainingUnit.FTP,.65))) })
        val four=base.copy(steps=base.steps.take(9)+base.steps.last())
        val unequal=base.copy(steps=base.steps.mapIndexed { i,s -> if(i==5) s.copy(seconds=181.0,durationValue=181.0) else s })
        val twoSets=base.copy(steps=base.steps+base.steps)
        listOf(steady,four,unequal,twoSets).forEach { assertEquals(listOf(TrainingVariant.STANDARD),TrainingVariants.available(it)) }
        val nearLimit=base.copy(steps=base.steps.map { it.copy(seconds=50000.0,durationValue=50000.0) })
        assertFalse(TrainingVariant.EXTENDED in TrainingVariants.available(nearLimit))
        assertSame(nearLimit,TrainingVariants.apply(nearLimit,TrainingVariant.EXTENDED_PLUS))
    }
    @Test fun overviewPillsCyclePersistAndOpenTheChosenStructureWithoutSelectingTheRow() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible();val a=host.get()
        val base=workout();var opened:TrainingWorkout?=null
        try {
            val themes=ThemeManager(a.getSharedPreferences("bike",0))
            fun manager()=TrainingManagerView(a,themes,{ RiderProfile(ftpW=200) },{}, {},{ opened=it })
            val view=manager();a.setContentView(view);view.render(listOf(base))
            fun layout(v:View) { v.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(780,View.MeasureSpec.EXACTLY));v.layout(0,0,360,780) }
            fun card():TrainingWorkoutView=nodes(view).filterIsInstance<TrainingWorkoutView>().single()
            fun click(expected:TrainingVariant,seconds:Double) {
                val button=card().findViewWithTag<Button>("training_variant")
                val list=view.findViewWithTag<ListView>("training_list")
                val bounds=android.graphics.Rect();button.getDrawingRect(bounds);list.offsetDescendantRectToMyCoords(button,bounds)
                val previous=opened
                for(action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
                    val event=android.view.MotionEvent.obtain(0,10,action,bounds.exactCenterX(),bounds.exactCenterY(),0)
                    try { assertTrue(list.dispatchTouchEvent(event)) } finally { event.recycle() }
                }
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200));layout(view)
                assertSame("Tapping the pill must not open details",previous,opened)
                assertEquals(expected,TrainingVariants.selected(a,base))
                assertEquals(expected.label,card().findViewWithTag<Button>("training_variant").text.toString())
                assertFalse(view.selecting)
                val position=(0 until list.adapter.count).first { list.adapter.getItem(it) is TrainingWorkout }
                list.performItemClick(card(),position,position.toLong())
                assertEquals(seconds,opened!!.seconds!!,.001)
            }
            layout(view)
            assertNull(card().findViewWithTag<View>("training_intensity"))
            assertEquals("Hart",card().findViewWithTag<TextView>("training_hardness").text.toString())
            assertEquals(82,card().findViewWithTag<View>("training_chart").layoutParams.height)
            click(TrainingVariant.EXTENDED,3360.0);click(TrainingVariant.EXTENDED_PLUS,3720.0)
            click(TrainingVariant.QUICK_PLUS,2280.0);click(TrainingVariant.QUICK,2640.0);click(TrainingVariant.STANDARD,3000.0)
            for(width in listOf(288,360,480)) {
                view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(780,View.MeasureSpec.EXACTLY));view.layout(0,0,width,780)
                val title=card().findViewWithTag<View>("training_name");val pill=card().findViewWithTag<View>("training_variant")
                val badge=card().findViewWithTag<View>("training_hardness")
                assertTrue(title.width>0);assertTrue(title.right<=pill.left);assertTrue(pill.right<=badge.left)
                assertTrue(kotlin.math.abs(title.top+title.height/2-pill.top-pill.height/2)<=1)
            }
            layout(view)
            val list=view.findViewWithTag<ListView>("training_list")
            val pill=card().findViewWithTag<Button>("training_variant")
            val hit=android.graphics.Rect();pill.getDrawingRect(hit);list.offsetDescendantRectToMyCoords(pill,hit)
            val beforeScroll=opened
            for((action,y) in listOf(android.view.MotionEvent.ACTION_DOWN to hit.exactCenterY(),android.view.MotionEvent.ACTION_MOVE to hit.exactCenterY()-90,android.view.MotionEvent.ACTION_UP to hit.exactCenterY()-90)) {
                val event=android.view.MotionEvent.obtain(0,10,action,hit.exactCenterX(),y,0)
                try { list.dispatchTouchEvent(event) } finally { event.recycle() }
            }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200));layout(view)
            assertSame(beforeScroll,opened);assertEquals(TrainingVariant.STANDARD,TrainingVariants.selected(a,base))
            val image=Bitmap.createBitmap(360,780,Bitmap.Config.ARGB_8888);view.draw(Canvas(image))
            File("build/reports/ui/training-variant-pills.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            click(TrainingVariant.EXTENDED,3360.0)
            val reopened=manager();a.setContentView(reopened);reopened.render(listOf(base));layout(reopened)
            assertEquals("Ext",reopened.findViewWithTag<Button>("training_variant").text.toString())
            reopened.render(listOf(base.copy(steps=base.steps.take(3)+base.steps.last())));layout(reopened)
            val fixed=reopened.findViewWithTag<Button>("training_variant")
            val fixedList=reopened.findViewWithTag<ListView>("training_list")
            val fixedBounds=android.graphics.Rect();fixed.getDrawingRect(fixedBounds);fixedList.offsetDescendantRectToMyCoords(fixed,fixedBounds)
            val previous=opened
            for(action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
                val event=android.view.MotionEvent.obtain(0,10,action,fixedBounds.exactCenterX(),fixedBounds.exactCenterY(),0)
                try { fixedList.dispatchTouchEvent(event) } finally { event.recycle() }
            }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))
            assertSame(previous,opened);assertEquals("Std",fixed.text.toString())
        } finally { TrainingVariants.forget(a,setOf(base.id));host.pause().stop().destroy() }
    }
}
