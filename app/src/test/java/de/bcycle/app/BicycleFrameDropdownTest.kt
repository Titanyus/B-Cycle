package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
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
class BicycleFrameDropdownTest {
    private fun layout(v:View) {
        v.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST))
        v.layout(0,0,v.measuredWidth,v.measuredHeight)
    }
    private fun image(v:View)=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888).also { v.draw(Canvas(it)) }
    @Test fun oneExistingOutlineEnclosesExpandedContentAndChevronChangesInBothDesigns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            for(flat in listOf(true,false)) {
                themes.setFlatDesign(flat);var changes=0
                val dropdown=BicycleFrameDropdown(c,themes,"Kette",open=false) { changes++ }
                dropdown.content.addView(View(c),LinearLayout.LayoutParams(-1,80))
                c.setContentView(dropdown);layout(dropdown)
                val closed=image(dropdown);val height=dropdown.height
                assertEquals(c.getString(R.string.zone_collapsed),dropdown.header.stateDescription)
                dropdown.header.performClick();layout(dropdown)
                assertTrue(dropdown.height>height+80)
                assertEquals(c.getString(R.string.zone_expanded),dropdown.header.stateDescription)
                val opened=image(dropdown)
                var different=0
                for(y in 10 until 38) for(x in 16 until 40) if(opened.getPixel(x,y)!=closed.getPixel(x,y)) different++
                assertTrue("Chevron must turn",different>10)
                val reference=Bitmap.createBitmap(dropdown.width,dropdown.height,Bitmap.Config.ARGB_8888)
                BicycleComponents.controlBackground(c,themes.active.tokens).apply {
                    setBounds(0,0,reference.width,reference.height);draw(Canvas(reference))
                }
                assertEquals(reference.getPixel(0,dropdown.height/2),opened.getPixel(0,dropdown.height/2))
                assertEquals(themes.active.tokens.secondary,opened.getPixel(dropdown.width/2,dropdown.header.bottom))
                assertEquals(BicycleDesign.dp(c,BicycleDesign.dropdownFrameHeaderGap),dropdown.content.paddingTop)
                BicycleDialogStyle.content(dropdown,themes);layout(dropdown)
                assertEquals(1,changes)
                dropdown.header.performClick();layout(dropdown)
                assertEquals(height,dropdown.height);assertEquals(View.GONE,dropdown.content.visibility)
                assertEquals(2,changes)
                closed.recycle();opened.recycle();reference.recycle()
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun rightHandSwitchAndSpecialFilledActionAreIndependentOfExpansion() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val root=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL };c.setContentView(root)
            val toggle=Switch(c);BicycleDialogStyle.control(toggle,themes)
            val first=BicycleFrameDropdown(c,themes,"Option",trailing=toggle,leadingIcon=BicycleIcon.Heart)
            root.addView(first);layout(root)
            assertEquals("Option",toggle.contentDescription)
            toggle.performClick();assertTrue(toggle.isChecked);assertTrue(first.expanded)
            first.header.performClick();assertFalse(first.expanded);assertTrue(toggle.isChecked)
            assertTrue(toggle.left>=first.header.right)
            assertTrue(first.expandControl.left>=first.header.right)
            assertTrue(toggle.left>=first.expandControl.right)
            first.expandControl.performClick();assertTrue(first.expanded);assertTrue(toggle.isChecked)
            var actions=0
            val action=BicycleComponents.specialFilled(c,themes,"Tauschen") { actions++ } as BicycleSpecialButton
            val second=BicycleFrameDropdown(c,themes,"Bauteil",trailing=action)
            root.addView(second);layout(root)
            action.performClick();assertEquals(1,actions);assertTrue(second.expanded);assertTrue(action.filled)
            second.header.performClick();assertFalse(second.expanded);assertEquals(1,actions)
        } finally { host.pause().stop().destroy() }
    }
}
