package de.bcycle.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SettingsInputTest {
    private fun nodes(v:View):Sequence<View> = sequence {
        yield(v)
        if(v is ViewGroup) for(i in 0 until v.childCount) yieldAll(nodes(v.getChildAt(i)))
    }
    private fun imeAllowed(screen:AlertDialog) {
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("Dialog must allow the Android keyboard",0,screen.window!!.attributes.flags and
            (WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE))
    }
    private fun editor(edit:EditText) {
        assertTrue("${edit.tag}: focusable",edit.isFocusableInTouchMode)
        assertTrue("${edit.tag}: soft input",edit.showSoftInputOnFocus)
        assertTrue("${edit.tag}: editor",edit.onCheckIsTextEditor())
        assertNotNull("${edit.tag}: input connection",edit.onCreateInputConnection(EditorInfo()))
        assertTrue("${edit.tag}: focus",edit.requestFocus())
        assertTrue(edit.hasFocus())
        edit.clearFocus()
    }
    @Test fun inlineUploadFormsAddedAfterShowingAllowKeyboardForEveryService() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            nodes(a.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
            ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("settings_DATA").performClick()
            val screen=ShadowAlertDialog.getLatestAlertDialog();val root=screen.window!!.decorView
            imeAllowed(screen)
            var count=0
            for(service in listOf("CLOUD","INTERVALS","DREEVE","STRAVA")) {
                root.findViewWithTag<View>("data_expand_$service").performClick()
                shadowOf(Looper.getMainLooper()).idle()
                val inputs=nodes(root.findViewWithTag("data_details_$service")).filterIsInstance<EditText>().toList()
                assertFalse(inputs.isEmpty())
                inputs.forEach(::editor);count+=inputs.size
                assertSame(screen,ShadowAlertDialog.getLatestAlertDialog())
                root.findViewWithTag<View>("data_expand_$service").performClick()
            }
            assertEquals(10,count)
            for(tag in listOf("export_password_visibility","intervals_key_visibility")) {
                root.findViewWithTag<View>(tag).performClick()
            }
            imeAllowed(screen)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun profileRiderMapKeyAndRenameInputsKeepNativeEditing() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            nodes(a.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
            val menu=ShadowAlertDialog.getLatestAlertDialog()
            menu.window!!.decorView.findViewWithTag<View>("settings_PROFILE").performClick()
            val rider=ShadowAlertDialog.getLatestAlertDialog();imeAllowed(rider)
            val riderInputs=nodes(rider.window!!.decorView).filterIsInstance<EditText>().toList()
            assertEquals(6,riderInputs.size)
            riderInputs.filter { it.isEnabled }.forEach(::editor)
            rider.dismiss();shadowOf(Looper.getMainLooper()).idle()
            menu.window!!.decorView.findViewWithTag<View>("settings_PROFILES").performClick()
            val profiles=ShadowAlertDialog.getLatestAlertDialog();val root=profiles.window!!.decorView
            imeAllowed(profiles)
            editor(root.findViewWithTag("bike_wheel_mm"))
            root.findViewWithTag<View>("bike_profile_name").performClick()
            editor(root.findViewWithTag("bike_profile_name_edit"))
            // API key starts hidden until the map source requires it. Check its native editor
            // after revealing the containing section; no credentials or network access involved.
            val key=root.findViewWithTag<EditText>("map_thunderforest_key")
            var ancestor=key.parent
            while(ancestor is View && ancestor!==root) { ancestor.visibility=View.VISIBLE;ancestor=ancestor.parent }
            editor(key)
            root.findViewWithTag<View>("dashboard_rename").performClick()
            val rename=ShadowAlertDialog.getLatestAlertDialog();imeAllowed(rename)
            editor(rename.window!!.decorView.findViewWithTag("dashboard_name_edit"))
            rename.dismiss();shadowOf(Looper.getMainLooper()).idle()
            imeAllowed(profiles)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun fullAndCompactDialogsAlsoAcceptEditorsInsertedAfterCreationAndReturnFromChild() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("input-test",0));val navigation=DialogNavigation()
            for(fullscreen in listOf(true,false)) {
                val body=LinearLayout(a)
                val screen=BicycleDialogBuilder(a,themes,navigation,fullscreen).setTitle("Input").setView(body).show()
                val input=EditText(a).apply { tag="late_input" };body.addView(input)
                imeAllowed(screen);editor(input)
                val child=BicycleDialogBuilder(a,themes,navigation,fullscreen=false).setTitle("Info").setMessage("Info").show()
                child.dismiss();shadowOf(Looper.getMainLooper()).idle()
                assertTrue(screen.isShowing);imeAllowed(screen);editor(input)
                screen.dismiss();shadowOf(Looper.getMainLooper()).idle()
            }
        } finally { host.pause().stop().destroy() }
    }
}
