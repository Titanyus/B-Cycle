package de.bcycle.app

import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class TrainingSortTest {
    private fun workout(id:Char,seconds:Double,power:Double,count:Int)=TrainingWorkout(id.toString().repeat(64),id.toString(),"","Ride","Test","zwo",List(count) { TrainingStep("",seconds/count,listOf(TrainingTarget(TrainingUnit.FTP,power))) })
    @Test fun everyMetricSortsInBothDirectionsWithUnknownValuesLast() {
        val short=workout('a',600.0,1.2,4);val long=workout('b',3600.0,.4,2)
        val unknown=workout('c',100.0,.5,1).copy(steps=listOf(TrainingStep("",null,emptyList(),"OPEN",null)))
        val profile=RiderProfile();val input=listOf(unknown,long,short)
        for(sort in TrainingSort.entries) for(down in listOf(false,true)) {
            val result=sort.ordered(input,profile,down)
            val known=result.map { sort.value(TrainingMetrics.of(it,profile)) }.filterNotNull()
            assertEquals(if(down) known.sortedDescending() else known.sorted(),known)
            if(sort!=TrainingSort.SECTIONS) assertEquals(unknown.id,result.last().id)
        }
        assertEquals(short.id,TrainingSort.DURATION.ordered(input,profile,false).first().id)
        assertEquals(long.id,TrainingSort.LOAD.ordered(input,profile,false).first().id)
        assertEquals(short.id,TrainingSort.INTENSITY.ordered(input,profile,true).first().id)
    }
    @Test fun fiveTilesFitAndSwitchMetricAndDirectionInBothDesigns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("training-sort-test",0))
            var selected=TrainingSort.DURATION;var down=false
            val popup=TrainingSortPopup(c,themes,selected,down,{selected=it},{down=it})
            val root=popup.contentView;c.setContentView(root)
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                for(width in listOf(320,360,480)) {
                    root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.AT_MOST));root.layout(0,0,width,root.measuredHeight)
                    val row=root.findViewWithTag<LinearLayout>("training_sort_choices")
                    assertEquals(5,row.childCount);assertTrue(row.getChildAt(4).right<=row.width)
                    for(sort in TrainingSort.entries) {
                        root.findViewWithTag<View>("training_sort_${sort.name}").performClick()
                        assertEquals(sort,selected)
                        assertEquals(1,(0..3).count { (row.getChildAt(it) as BicycleSegmentButton).isChecked })
                    }
                    val before=down;root.findViewWithTag<View>("training_sort_direction").performClick();assertEquals(!before,down)
                }
            }
        } finally { host.pause().stop().destroy() }
    }
}
