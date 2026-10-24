package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FormUnitsAndChoicesTest {
    private fun layout(view:View,width:Int=320,height:Int=760) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
        view.layout(0,0,width,height)
    }
    @Test fun unitsAreHalfOpaqueInsideFieldsWithoutEnteringTextOrFiringEditsAcrossThemesAndRestyling() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("unit-fields",0))
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL }
            val edit=BicycleComponents.input(a,themes).apply { setText("182") }
            var changes=0;edit.addTextChangedListener(object:android.text.TextWatcher {
                override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
                override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { changes++ }
                override fun afterTextChanged(s:android.text.Editable?)=Unit
            })
            val row=BicycleForm.numberRow(edit,themes,R.string.rider_height,R.string.rider_cm)
            root.addView(row);a.setContentView(root);layout(root)
            assertEquals(0,changes);assertEquals("182",edit.text.toString());assertEquals(2,row.childCount)
            assertEquals("cm",edit.stateDescription)
            val image=Bitmap.createBitmap(edit.width,edit.height,Bitmap.Config.ARGB_8888)
            val light=CustomTheme("custom:unit-light","Light",ThemeTokens(0xffeeeeee.toInt(),0xffcccccc.toInt(),0xffaaaaaa.toInt(),0xff000000.toInt(),0xff002266.toInt()))
            assertTrue(themes.saveCustom(light).valid)
            for(theme in BicycleThemes.presets+light) {
                themes.activate(theme.id);BicycleDialogStyle.control(edit,themes);layout(root)
                var drawn=false
                val canvas=object:Canvas(image) {
                    override fun drawText(text:String,x:Float,y:Float,paint:Paint) {
                        if(text=="cm") {
                            drawn=true
                            assertEquals(127,android.graphics.Color.alpha(paint.color))
                            assertEquals(theme.tokens.foreground and 0xffffff,paint.color and 0xffffff)
                            assertTrue(x>=edit.paddingLeft)
                            assertTrue("unit=$text x=$x measured=${paint.measureText(text)} width=${edit.width} padding=${edit.paddingRight} compound=${edit.compoundPaddingRight} layout=${edit.layout.width} left=${edit.layout.getLineLeft(0)} right=${edit.layout.getLineRight(0)} scroll=${edit.scrollX}",x+paint.measureText(text)<=edit.width-edit.paddingRight+.1f)
                            assertEquals(edit.baseline.toFloat()-edit.scrollY,y,0f)
                        }
                        super.drawText(text,x,y,paint)
                    }
                }
                edit.draw(canvas);assertTrue(drawn);assertEquals(0,changes)
                assertEquals("182",edit.text.toString())
            }
            edit.setText("190");assertEquals(1,changes);assertEquals("190",edit.text.toString())
            edit.setText("");assertEquals(2,changes);assertTrue(edit.text.isEmpty());assertEquals("cm",BicycleInputUnit.unit(edit))
            image.recycle()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun listSwitchesKeepSingleAndMultipleSelectionAndDisabledStateWhileSegmentsStaySegments() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("choice-switches",0))
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL }
            val group=RadioGroup(a)
            val radios=(0..1).map { RadioButton(a).apply {
                id=View.generateViewId();text="Option $it";BicycleDialogStyle.control(this,themes)
                group.addView(this,RadioGroup.LayoutParams(-1,-2))
            } }
            root.addView(group)
            radios[0].performClick();assertTrue(radios[0].isChecked)
            radios[1].performClick();assertFalse(radios[0].isChecked);assertTrue(radios[1].isChecked)
            val config=HrvQuestionSettings(a);config.write(HrvQuestionConfig(emptySet()))
            val questions=HrvQuestionSettingsView(a,themes);root.addView(questions)
            a.setContentView(root);layout(root)
            assertEquals(group.width,radios[0].width)
            for(key in listOf("SLEEP","CONDITION")) {
                val choice=questions.findViewWithTag<CompoundButton>("hrvq_enabled_$key")
                choice.performClick()
                assertTrue(choice.isChecked)
                assertEquals(Switch::class.java.name,choice.createAccessibilityNodeInfo().className)
                assertNull(choice.buttonDrawable)
                assertNull(choice.compoundDrawablesRelative[0])
                assertEquals(choice.paddingLeft,choice.compoundPaddingLeft)
                assertTrue(choice.compoundPaddingRight>choice.paddingRight)
                assertEquals(android.view.Gravity.LEFT,choice.gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK)
                assertEquals(BicycleDesign.switchWidth+BicycleDesign.choiceTextGap,choice.compoundDrawablesRelative[2]!!.intrinsicWidth)
            }
            assertTrue(config.read().enabled.containsAll(setOf(HrvQuestion.SLEEP,HrvQuestion.CONDITION)))
            assertFalse(questions.findViewWithTag<CompoundButton>("hrvq_enabled_CYCLE").isEnabled)
            val segment=BicycleSegmentButton(a,themes)
            BicycleDialogStyle.control(segment,themes)
            assertNull(segment.buttonDrawable)
            val bitmap=Bitmap.createBitmap(320,760,Bitmap.Config.ARGB_8888);root.draw(Canvas(bitmap))
            java.io.File("build/reports/ui/list-choice-switches.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        } finally { host.pause().stop().destroy() }
    }
}
