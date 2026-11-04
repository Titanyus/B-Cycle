package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
class InputFocusFrameTest {
    @Test fun plainFocusAndSelectionKeepNativeStateWithoutGlowInBothModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL;isFocusableInTouchMode=true;setPadding(24,24,24,24) }
            val edit=BicycleComponents.input(a,themes).apply { setText("183");setSingleLine() }
            root.addView(edit,LinearLayout.LayoutParams(-1,48))
            val card=View(a);root.addView(card,LinearLayout.LayoutParams(-1,60))
            a.setContentView(root)
            val listener=View.OnFocusChangeListener { _,_-> };edit.onFocusChangeListener=listener
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat);shadowOf(android.os.Looper.getMainLooper()).idle()
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));root.layout(0,0,360,400)
                assertTrue(edit.requestFocus());assertSame(listener,edit.onFocusChangeListener)
                assertEquals("183",edit.text.toString());assertNull(edit.getTag(R.id.input_frame))
                val image=Bitmap.createBitmap(edit.width,edit.height,Bitmap.Config.ARGB_8888)
                edit.background.setBounds(0,0,edit.width,edit.height);edit.background.draw(Canvas(image))
                assertEquals(themes.active.tokens.primary,image.getPixel(edit.width/2,0))
                assertEquals(image.getPixel(edit.width/2,0),image.getPixel(edit.width/4,0));image.recycle()
                BicycleSelectionCard.apply(card,themes.active.tokens,true)
                assertNull(card.getTag(R.id.input_frame));assertNotNull(card.foreground)
                edit.isEnabled=false;assertFalse(edit.isEnabled);edit.isEnabled=true
                root.requestFocus();assertFalse(edit.hasFocus())
            }
        } finally { host.pause().stop().destroy() }
    }
}
