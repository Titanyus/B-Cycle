package de.bcycle.app

import android.app.Activity
import android.view.View
import android.widget.Button
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
class SettingsPasswordViewTest {
    @Test fun passwordRequiresConfirmationAndIsNotSavedOrRetainedAfterClosing() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=host.get();var received:CharArray?=null
            val view=SettingsPasswordView(activity,ThemeManager(activity.getSharedPreferences("password-test",0)),true) { value,_ -> received=value }
            activity.setContentView(view)
            val password=view.findViewWithTag<EditText>("backup_password")
            val confirm=view.findViewWithTag<EditText>("backup_password_confirm")
            val submit=view.findViewWithTag<Button>("backup_password_submit")
            assertFalse(password.isSaveEnabled);assertFalse(confirm.isSaveEnabled)
            assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,password.importantForAutofill)
            password.setText("short");confirm.setText("short");submit.performClick()
            assertNotNull(password.error);assertNull(received)
            password.setText("Lange Testphrase äöü 2026");confirm.setText("different");submit.performClick()
            assertNotNull(confirm.error);assertNull(received)
            confirm.setText(password.text);submit.performClick()
            assertArrayEquals("Lange Testphrase äöü 2026".toCharArray(),received);assertFalse(submit.isEnabled)
            activity.setContentView(View(activity))
            assertEquals(0,password.length());assertEquals(0,confirm.length())
            received!!.fill('\u0000')
        } finally { host.pause().stop().destroy() }
    }
}
