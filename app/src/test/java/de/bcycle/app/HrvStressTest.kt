package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.SeekBar
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
import java.time.LocalDate
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvStressTest {
    @Test fun oneEightStepSliderReplacesBothQuestionsAndPreservesOriginalAnswers() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));var saved:HrvQuestionnaire?=null
            HrvQuestionSettings(c).write(HrvQuestionConfig(setOf(HrvQuestion.STRESS,HrvQuestion.LIFESTYLE)))
            val initial=HrvQuestionnaire("2026-10-31",ratings=mapOf("lifestyle" to 2))
            val form=HrvQuestionnaireView(c,themes,initial,null) { value,done -> saved=value;done(true) }
            val scroll=ScrollView(c).apply { addView(form) };c.setContentView(scroll)
            scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,800)
            val slider=form.findViewWithTag<SeekBar>("hrvq_stress")
            assertEquals(7,slider.max);assertEquals(6,slider.progress)
            assertEquals(c.getString(R.string.hrvq_stress),slider.contentDescription)
            assertNull(form.findViewWithTag<View>("hrvq_lifestyle"));assertNull(form.findViewWithTag<View>("hrvq_stress_0"))
            form.flush();assertEquals(6,saved!!.stressRating);assertEquals(2,saved!!.ratings["lifestyle"])
            val decoded=HrvQuestionnaire.decode(saved!!.json())
            val again=HrvQuestionnaireView(c,themes,decoded,null) { _,done->done(true) }
            assertEquals(6,again.findViewWithTag<SeekBar>("hrvq_stress").progress)
            val settings=HrvQuestionSettingsView(c,themes)
            assertEquals(c.getString(R.string.hrvq_stress),settings.findViewWithTag<CheckBox>("hrvq_enabled_LIFESTYLE").text)
            assertNull(settings.findViewWithTag<View>("hrvq_enabled_STRESS"))
            HrvQuestionSettings(c).write(HrvQuestionConfig(emptySet()));shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
            assertNull(form.findViewWithTag<View>("hrvq_stress"));assertEquals(6,saved!!.stressRating)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun settingsCollapseLegacyChoicesAndQuestionnaireValidatesEightSteps() {
        val empty=HrvQuestionnaire("2026-10-31")
        assertNull(empty.stressRating);assertNull(empty.intervalsStress)
        for(value in listOf(-1,8)) assertThrows(IllegalArgumentException::class.java) { empty.copy(ratings=mapOf("stress" to value)).json() }
        for(value in listOf(0,5)) assertThrows(IllegalArgumentException::class.java) { empty.copy(stress=value).json() }
        assertThrows(IllegalArgumentException::class.java) { HrvQuestionnaire.decode(empty.json().put("stress",1.5)) }
        val malformed=empty.json().put("ratings",JSONObject().put("stress",2.5))
        assertThrows(IllegalArgumentException::class.java) { HrvQuestionnaire.decode(malformed) }
        val old=JSONObject("""{"schema":2,"enabled":["SLEEP","STRESS","LIFESTYLE"],"customNames":["","",""]}""")
        val migrated=HrvQuestionConfig.decode(old)
        assertEquals(setOf(HrvQuestion.SLEEP,HrvQuestion.LIFESTYLE),migrated.enabled)
        assertEquals(3,migrated.json().getInt("schema"));assertFalse(migrated.json().getJSONArray("enabled").toString().contains("STRESS"))
        val optedOut=migrated.copy(enabled=migrated.enabled-HrvQuestion.LIFESTYLE)
        assertEquals(setOf(HrvQuestion.SLEEP),HrvQuestionConfig.decode(optedOut.json()).enabled)
        assertEquals(setOf(HrvQuestion.SLEEP),HrvQuestionConfig.decode(old.put("schema",1).put("enabled",org.json.JSONArray(listOf("SLEEP")))).enabled)
    }
    @Test fun reversedHistoryAndEightNewStepsMapToIntervalsWithoutDoubleInversion() {
        val day=LocalDate.of(2026,10,31);val row=HrvHistorySummary("id",0,true,42.25,importedDay=day.toString())
        for(level in 0..7) {
            val q=HrvQuestionnaire(day.toString(),ratings=mapOf("stress" to level,"lifestyle" to 10),stress=4)
            val decoded=HrvQuestionnaire.decode(q.json())
            assertEquals(level,decoded.stressRating);assertEquals(1+level/2,IntervalsWellness.from(row,decoded)!!.values.getInt("stress"))
            assertEquals(10,decoded.ratings["lifestyle"]);assertEquals(4,decoded.stress)
        }
        for(old in 0..10) {
            val legacy=HrvQuestionnaire(day.toString(),ratings=mapOf("lifestyle" to old)).json().put("schema",2)
            val q=HrvQuestionnaire.decode(legacy);val expected=((10-old)*7.0/10).roundToInt()
            assertEquals(expected,q.stressRating);assertEquals(old,q.ratings["lifestyle"])
            assertEquals(1+expected/2,IntervalsWellness.from(row,q)!!.values.getInt("stress"))
            assertEquals(expected,HrvQuestionnaire.decode(q.json()).stressRating)
        }
        for(old in 1..4) assertEquals(old,HrvQuestionnaire(day.toString(),stress=old).intervalsStress)
    }
}
