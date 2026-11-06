package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrainingChartAxesTest {
    private fun workout(vararg targets:TrainingTarget)=TrainingWorkout("0".repeat(64),"Test","","Ride","Test","zwo",targets.map { TrainingStep("",300.0,listOf(it)) })
    @Test fun wattsUseActualFtpAndOnePhysicalScaleAcrossAbsoluteAndRelativeTargets() {
        val profile=RiderProfile(ftpW=200)
        val ramp=TrainingTarget(TrainingUnit.FTP,1.2,.4,true)
        assertEquals(240.0,TrainingChartAxes.targetWatts(ramp,profile)!!,0.0)
        assertEquals(80.0,TrainingChartAxes.targetWatts(ramp,profile,true)!!,0.0)
        assertEquals(300.0,TrainingChartAxes.watts(workout(ramp,TrainingTarget(TrainingUnit.WATTS,300.0)),profile)!!,0.0)
        assertNull(TrainingChartAxes.watts(workout(ramp),RiderProfile()))
        assertNull(TrainingChartAxes.watts(workout(TrainingTarget(TrainingUnit.HR_BPM,130.0)),profile))
        assertNull(TrainingChartAxes.watts(workout(TrainingTarget(TrainingUnit.POWER_ZONE,4.0)),profile))
        assertEquals(200.0,TrainingChartAxes.targetWatts(TrainingTarget(TrainingUnit.WATTS,180.0,220.0),profile)!!,0.0)
    }
    @Test fun ticksAreRegularRoundAndIncludeTheExactWorkoutEnd() {
        val ticks=TrainingChartAxes.timeTicks(3480.0,4f)
        assertEquals(listOf(0.0,900.0,1800.0,2700.0,3480.0),ticks)
        assertEquals(listOf(0.0,15.0,30.0,45.0),TrainingChartAxes.timeTicks(45.0,4f))
        assertEquals(50.0,TrainingChartAxes.wattStep(150.0),0.0)
        assertEquals(100.0,TrainingChartAxes.wattStep(350.0),0.0)
        assertEquals(500.0,TrainingChartAxes.wattStep(1500.0),0.0)
    }
    @Test fun detailPlotUsesWattHeightsAndLeavesRoomForTimeLabels() {
        val context=RuntimeEnvironment.getApplication()
        val chart=TrainingWorkoutChart(context,ThemeManager(context.getSharedPreferences("training-axes",0)),true)
        val training=workout(TrainingTarget(TrainingUnit.WATTS,100.0),TrainingTarget(TrainingUnit.FTP,.5,1.0,true))
        chart.show(training,RiderProfile(ftpW=200));chart.layout(0,0,360,180)
        val bitmap=Bitmap.createBitmap(360,180,Bitmap.Config.ARGB_8888)
        chart.draw(Canvas(bitmap))
        val color=training.steps.first().target!!.color(RiderProfile(ftpW=200))
        assertEquals(color,bitmap.getPixel(100,130))
        assertEquals(0,bitmap.getPixel(100,60))
        assertNotEquals(color,bitmap.getPixel(100,175))
        bitmap.recycle()
    }
}
