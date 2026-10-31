package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.widget.RadioGroup
import android.widget.ScrollView
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvStressTest {
    @Test fun fourChoicesStartUnansweredPersistAndReopenForTheSameDay() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));var saved:HrvQuestionnaire?=null
            HrvQuestionSettings(c).write(HrvQuestionConfig(setOf(HrvQuestion.STRESS)))
            val form=HrvQuestionnaireView(c,themes,HrvQuestionnaire("2026-10-31"),null) { value,done -> saved=value;done(true) }
            val scroll=ScrollView(c).apply { addView(form) };c.setContentView(scroll)
            scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,800)
            val row=form.findViewWithTag<RadioGroup>("hrvq_stress")
            assertEquals(-1,row.checkedRadioButtonId);assertNull(saved)
            assertEquals(listOf("Wenig","Durchschnittlich","Sehr","Extrem"),(0..3).map { (row.getChildAt(it) as BicycleSegmentButton).text.toString() })
            for(index in 0..3) {
                val button=row.getChildAt(index) as BicycleSegmentButton
                button.performClick();form.flush()
                assertEquals(index+1,saved!!.stress)
                val text=button.layout
                assertEquals(button.text.length,text.getLineEnd(text.lineCount-1))
            }
            val decoded=HrvQuestionnaire.decode(saved!!.json())
            val again=HrvQuestionnaireView(c,themes,decoded,null) { _,done->done(true) }
            assertTrue(again.findViewWithTag<BicycleSegmentButton>("hrvq_stress_3").isChecked)
            assertEquals(4,decoded.stress)
            HrvQuestionSettings(c).write(HrvQuestionConfig(emptySet()));shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
            assertNull(form.findViewWithTag<View>("hrvq_stress"))
            assertEquals(4,saved!!.stress)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun stressIsOptionalValidatedAndOldQuestionSettingsGainItOnce() {
        val empty=HrvQuestionnaire("2026-10-31")
        val legacy=empty.json().apply { remove("stress") }
        assertNull(HrvQuestionnaire.decode(legacy).stress)
        for(value in listOf(0,5)) assertThrows(IllegalArgumentException::class.java) { empty.copy(stress=value).json() }
        assertThrows(IllegalArgumentException::class.java) { HrvQuestionnaire.decode(empty.json().put("stress",1.5)) }
        val old=JSONObject("""{"schema":1,"enabled":["SLEEP"],"customNames":["","",""]}""")
        val migrated=HrvQuestionConfig.decode(old)
        assertEquals(setOf(HrvQuestion.SLEEP,HrvQuestion.STRESS),migrated.enabled)
        val optedOut=migrated.copy(enabled=migrated.enabled-HrvQuestion.STRESS)
        assertEquals(2,optedOut.json().getInt("schema"))
        assertEquals(setOf(HrvQuestion.SLEEP),HrvQuestionConfig.decode(optedOut.json()).enabled)
    }
}
