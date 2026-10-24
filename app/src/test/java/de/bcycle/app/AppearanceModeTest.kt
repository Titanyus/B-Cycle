package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
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
class AppearanceModeTest {
    @Test fun flatAndGlowUseTheSameControlsAndPrimaryWhileAccentAndBackupRemainIndependent() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            val tokens=ThemeTokens(0xffbf281b.toInt(),0xff161522.toInt(),0xff282735.toInt(),Color.WHITE,0xff26aaff.toInt())
            assertTrue(themes.saveCustom(CustomTheme("custom:two-highlights","Two colors",tokens)).valid)
            themes.activate("custom:two-highlights")
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL;setPadding(24,24,24,24);setBackgroundColor(tokens.secondary) }
            val edit=BicycleComponents.input(a,themes).apply { setText("183") }
            val selected=BicycleSegmentButton(a,themes,true).apply { text="Ausgewählt";isChecked=true;stackedIcon(BicycleIcon.Duration) }
            val toggle=Switch(a).apply { text="Schalter";isChecked=true;BicycleDialogStyle.control(this,themes) }
            val action=BicycleComponents.textAction(a,themes,"Aktion") {}
            root.addView(edit,LinearLayout.LayoutParams(-1,48));root.addView(selected,LinearLayout.LayoutParams(-1,48).apply { topMargin=32 });root.addView(toggle);root.addView(action)
            val scroll=ScrollView(a).apply { addView(root) };val parent=android.widget.FrameLayout(a).apply { addView(scroll) };a.setContentView(parent)
            fun layout() { parent.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));parent.layout(0,0,360,600) }
            layout();edit.requestFocus();layout()
            var events=0;selected.setOnCheckedChangeListener { _,_->events++ }
            val geometry=listOf(edit.width,edit.height,edit.paddingLeft,selected.width,selected.height)
            fun fill():Bitmap { val drawable=(selected.background as android.graphics.drawable.RippleDrawable).getDrawable(0);return Bitmap.createBitmap(selected.width,selected.height,Bitmap.Config.ARGB_8888).also { drawable.setBounds(0,0,it.width,it.height);drawable.draw(Canvas(it)) } }
            fun snapshot(name:String) {
                val image=Bitmap.createBitmap(360,600,Bitmap.Config.ARGB_8888);parent.draw(Canvas(image))
                java.io.File("build/reports/ui/$name.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
            snapshot("appearance-glow")
            assertFalse(themes.flatDesign);assertTrue(selected.elevation>0)
            val glowFill=fill();assertNotEquals(glowFill.getPixel(40,10),glowFill.getPixel(40,38));glowFill.recycle()
            assertEquals(tokens.primary,action.currentTextColor)
            assertEquals(tokens.primary,toggle.thumbTintList!!.getColorForState(toggle.drawableState,0))
            themes.setFlatDesign(true);shadowOf(android.os.Looper.getMainLooper()).idle();layout()
            snapshot("appearance-flat")
            assertTrue(themes.flatDesign);assertEquals(0f,selected.elevation,0f)
            val flatFill=fill();assertEquals(tokens.primary,flatFill.getPixel(40,10));assertEquals(tokens.primary,flatFill.getPixel(40,38));flatFill.recycle()
            val image=Bitmap.createBitmap(360,600,Bitmap.Config.ARGB_8888);parent.draw(Canvas(image));assertEquals(tokens.secondary,image.getPixel(edit.left-3,edit.top+24));image.recycle()
            assertEquals(tokens.accent,themes.active.tokens.accent)
            assertEquals(geometry,listOf(edit.width,edit.height,edit.paddingLeft,selected.width,selected.height));assertEquals("183",edit.text.toString());assertTrue(selected.isChecked);assertEquals(0,events)
            val backup=SettingsBackup(a,themes);val bytes=backup.export(setOf(BackupSection.DESIGN))
            themes.setFlatDesign(false);val doc=backup.decode(bytes);backup.apply(backup.plan(doc,doc.sections.keys));shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(themes.flatDesign)
            assertEquals(tokens.accent,themes.active.tokens.accent)
            themes.setFlatDesign(false);shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(selected.elevation>0);assertEquals(0,events)
            selected.performClick();assertFalse(selected.isChecked);assertEquals(1,events)
        } finally { host.pause().stop().destroy() }
    }
}
