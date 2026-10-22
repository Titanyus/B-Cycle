package de.bcycle.app

import android.graphics.Paint
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TypographyRolesTest {
    @Test fun mixedCaseAndPasswordSurviveInputConfigurationReattachmentAndRestyling() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val edit=BicycleComponents.input(a,themes).apply {
                inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setText("Meine eBike-Fahrt äÖß");transformationMethod=PasswordTransformationMethod.getInstance()
            }
            val root=android.widget.LinearLayout(a).apply { addView(edit) };a.setContentView(root)
            assertEquals(BicycleDesign.narrowTypeface(a),edit.typeface)
            assertEquals(BicycleDesign.settingsMetricSp,edit.textSize,0f)
            assertFalse(edit.isAllCaps);assertTrue(edit.transformationMethod is PasswordTransformationMethod)
            assertEquals("Meine eBike-Fahrt äÖß",edit.text.toString())
            root.removeView(edit);root.addView(edit);BicycleDialogStyle.control(edit,themes);BicycleDialogStyle.control(edit,themes)
            assertEquals(BicycleDesign.narrowTypeface(a),edit.typeface)
            assertTrue(edit.transformationMethod is PasswordTransformationMethod)
            val paint=Paint().apply { typeface=edit.typeface;textSize=32f }
            assertTrue(paint.hasGlyph("ä"));assertTrue(paint.hasGlyph("ß"))
            assertNotEquals(paint.measureText("eBike"),paint.measureText("EBIKE"))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun headerTransformsDisplayOnlyWhileQuestionnaireAndCustomInputsShareTheNarrowRole() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));val original="Meine Straße mit dem eBike"
            val title=RideTitleView(a).apply { text=original }
            assertEquals(original,title.text.toString());assertTrue(title.isAllCaps)
            assertEquals(original.uppercase(),title.transformationMethod.getTransformation(title.text,title).toString())
            val form=HrvQuestionnaireView(a,themes,HrvQuestionnaire("2026-10-22"),null) { _,done->done(true) }
            a.setContentView(form)
            val edits=testDescendants(form).filterIsInstance<EditText>()
            assertTrue(edits.isNotEmpty())
            edits.forEach { assertEquals(BicycleDesign.narrowTypeface(a),it.typeface);assertFalse(it.isAllCaps) }
            val notes=form.findViewWithTag<EditText>("hrvq_notes");notes.setText(original);assertEquals(original,notes.text.toString())
            val custom=HrvQuestionSettingsView(a,themes);a.setContentView(custom)
            testDescendants(custom).filterIsInstance<EditText>().forEach { assertEquals(BicycleDesign.narrowTypeface(a),it.typeface);assertFalse(it.isAllCaps) }
        } finally { host.pause().stop().destroy() }
    }
}
