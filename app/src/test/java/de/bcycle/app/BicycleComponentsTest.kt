package de.bcycle.app

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BicycleComponentsTest {
    @Test fun formSpacingSurvivesLateLayoutParametersAndReattachmentWithoutDoublingHorizontalGaps() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val themes=ThemeManager(activity.getSharedPreferences("spacing",0))
            val root=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL }
            val button=BicycleComponents.action(activity,themes,"Action") {}
            BicycleDialogStyle.control(button,themes)
            val spinner=android.widget.Spinner(activity).apply { adapter=android.widget.ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,listOf("A","B")) }
            BicycleDialogStyle.spinner(spinner,themes)
            val toggle=android.widget.Switch(activity);BicycleDialogStyle.control(toggle,themes)
            val gap=BicycleDesign.dp(activity,BicycleDesign.controlGap)
            val height=BicycleDesign.dp(activity,BicycleDesign.touchSize)
            listOf(button,spinner,toggle).forEach { root.addView(it,LinearLayout.LayoutParams(-1,height)) }
            val row=LinearLayout(activity).apply { orientation=LinearLayout.HORIZONTAL }
            val left=BicycleComponents.action(activity,themes,"Left") {};val right=BicycleComponents.action(activity,themes,"Right") {}
            row.addView(left,LinearLayout.LayoutParams(0,height,1f).apply { marginEnd=gap })
            row.addView(right,LinearLayout.LayoutParams(0,height,1f))
            root.addView(row);activity.setContentView(root)
            for(view in listOf(button,spinner,toggle,left,right)) {
                val margins=view.layoutParams as android.view.ViewGroup.MarginLayoutParams
                assertEquals(gap,margins.topMargin);assertEquals(height,margins.height)
            }
            assertEquals(0,(right.layoutParams as LinearLayout.LayoutParams).marginStart)
            assertEquals(1f,(right.layoutParams as LinearLayout.LayoutParams).weight,0f)
            root.removeView(button);root.addView(button);assertEquals(gap,(button.layoutParams as LinearLayout.LayoutParams).topMargin)
            row.removeView(right);row.addView(right);assertEquals(0,(right.layoutParams as LinearLayout.LayoutParams).marginStart)
            (left.layoutParams as LinearLayout.LayoutParams).marginEnd=0
            BicycleDialogStyle.control(right,themes)
            assertEquals(gap,(right.layoutParams as LinearLayout.LayoutParams).marginStart)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun paletteChangesOnlyColorsAndDetachedComponentsStopObservingUntilReattached() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            val manager = ThemeManager(activity.getSharedPreferences("bike", 0))
            val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            val header = BicycleComponents.header(activity, manager)
            val body = BicycleComponents.dialogBody(activity, manager)
            val grid = BicycleComponents.menuGrid(activity, manager)
            val scroll = BicycleComponents.scrollBody(activity, manager)
            val text = BicycleComponents.text(activity, manager, "Test", title = true)
            val input = BicycleComponents.input(activity, manager)
            val button = BicycleComponents.action(activity, manager, "Action") {}
            val icon = BicycleComponents.icon(activity, manager, BicycleIcons.drawable(activity, BicycleIcon.MoreOptions, manager.active.tokens.foreground), "More") {}
            val divider = BicycleComponents.divider(activity, manager)
            body.addView(input); body.addView(button); body.addView(icon); body.addView(divider)
            root.addView(header); root.addView(text); root.addView(body); root.addView(grid); root.addView(scroll)
            activity.setContentView(root)
            fun layout() {
                root.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, 720, 1200)
            }
            fun geometry(view: View) = listOf(view.left, view.top, view.width, view.height, view.paddingLeft, view.paddingTop,
                view.paddingRight, view.paddingBottom, view.minimumWidth, view.minimumHeight, view.layoutParams.width, view.layoutParams.height)
            layout()
            val views = listOf(header, body, text, input, button, icon, divider, grid, scroll)
            val before = views.map(::geometry); val font = text.textSize
            val radius = (input.background as GradientDrawable).cornerRadius
            val light = CustomTheme("custom:light", "Light", ThemeTokens(0xffeeeeee.toInt(), 0xffcccccc.toInt(), 0xffaaaaaa.toInt(), 0xff000000.toInt()))
            assertTrue(manager.saveCustom(light).valid); assertTrue(manager.activate(light.id)); layout()
            assertEquals(before, views.map(::geometry)); assertEquals(font, text.textSize, 0f)
            assertEquals(radius, (input.background as GradientDrawable).cornerRadius, 0f)
            assertEquals(light.tokens.foreground, text.currentTextColor)
            assertEquals(light.tokens.primary, (header.background as GradientDrawable).color!!.defaultColor)
            assertEquals(light.tokens.tertiary, (input.background as GradientDrawable).color!!.defaultColor)
            assertEquals(BicycleDesign.dividerPixels, divider.height)
            assertEquals(BicycleDesign.gridColumns, grid.columnCount); assertEquals(BicycleDesign.gridRows, grid.rowCount)
            assertEquals(BicycleDesign.scrollFillViewport, scroll.isFillViewport)
            assertEquals(BicycleDesign.scrollClipToPadding, scroll.clipToPadding)
            root.removeView(text); manager.activate(BicycleThemes.red.id)
            assertEquals(light.tokens.foreground, text.currentTextColor)
            root.addView(text); assertEquals(BicycleThemes.red.tokens.foreground, text.currentTextColor)
        } finally { controller.pause().stop().destroy() }
    }    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun semanticHighlightIconsUseAccentWhileStandardIconsAndGeometryStayUnchanged() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get(); val manager = ThemeManager(activity.getSharedPreferences("accent-ui", 0))
            val root = LinearLayout(activity)
            val standard = BicycleComponents.icon(activity, manager, BicycleIcon.MoreOptions, "Standard") {}
            val highlight = BicycleComponents.icon(activity, manager, BicycleIcon.ActiveOnMap, "Active") {}
            root.addView(standard); root.addView(highlight); activity.setContentView(root)
            fun layout() {
                root.measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, 720, 200)
            }
            fun pixel(view: android.widget.ImageButton): Int {
                val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                return (0 until bitmap.width).flatMap { x -> (0 until bitmap.height).map { y -> bitmap.getPixel(x, y) } }
                    .maxBy { android.graphics.Color.alpha(it) }
            }
            layout(); val before = listOf(standard.width, standard.height, highlight.width, highlight.height)
            val tokens = ThemeTokens(0xff535353.toInt(), 0xff292929.toInt(), 0xff000000.toInt(), 0xffffffff.toInt(), 0xffffaa55.toInt())
            assertTrue(manager.saveCustom(CustomTheme("custom:accent", "Accent", tokens)).valid)
            assertTrue(manager.activate("custom:accent")); layout()
            assertEquals(tokens.foreground and 0xffffff, pixel(standard) and 0xffffff)
            assertEquals(tokens.accent and 0xffffff, pixel(highlight) and 0xffffff)
            assertEquals(before, listOf(standard.width, standard.height, highlight.width, highlight.height))
            assertEquals(tokens.foreground, BicycleIcons.color(tokens, BicycleIcon.LocalSaved, negative = true))
            assertEquals(tokens.accent, BicycleIcons.color(tokens, BicycleIcon.CloudUploaded))
            assertEquals(tokens.foreground, BicycleIcons.color(tokens, BicycleIcon.ShowOnMap))
            manager.activate(BicycleThemes.red.id)
            assertEquals(BicycleThemes.red.tokens.foreground and 0xffffff, pixel(highlight) and 0xffffff)
        } finally { controller.pause().stop().destroy() }
    }

    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun checkboxCirclesFillOnSelectionRemainIndependentAndFollowTheme() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val themes=ThemeManager(activity.getSharedPreferences("circle-ui",0))
            val root=LinearLayout(activity)
            val first=android.widget.CheckBox(activity);val second=android.widget.CheckBox(activity)
            val toggle=android.widget.Switch(activity);val thumb=toggle.thumbDrawable
            for(view in listOf(first,second,toggle)) { BicycleDialogStyle.control(view,themes);root.addView(view) }
            activity.setContentView(root)
            fun draw(box: android.widget.CheckBox): android.graphics.Bitmap {
                val bitmap=android.graphics.Bitmap.createBitmap(48,48,android.graphics.Bitmap.Config.ARGB_8888)
                box.buttonDrawable!!.apply { setBounds(0,0,48,48);draw(android.graphics.Canvas(bitmap)) }
                return bitmap
            }
            assertEquals(0,android.graphics.Color.alpha(draw(first).getPixel(24,24)))
            assertEquals(0,android.graphics.Color.alpha(draw(first).getPixel(6,6)))
            val width=first.buttonDrawable!!.intrinsicWidth
            first.performClick();second.performClick()
            assertTrue(first.isChecked);assertTrue(second.isChecked)
            assertEquals(themes.active.tokens.foreground,draw(first).getPixel(24,24))
            first.performClick();assertFalse(first.isChecked);assertTrue(second.isChecked)
            assertEquals(0,android.graphics.Color.alpha(draw(first).getPixel(24,24)))
            val light=CustomTheme("custom:circle-light","Light",ThemeTokens(0xffeeeeee.toInt(),0xffcccccc.toInt(),0xffaaaaaa.toInt(),0xff000000.toInt()))
            assertTrue(themes.saveCustom(light).valid);assertTrue(themes.activate(light.id))
            assertEquals(light.tokens.foreground,draw(second).getPixel(24,24))
            assertEquals(width,first.buttonDrawable!!.intrinsicWidth)
            assertSame(thumb,toggle.thumbDrawable)
        } finally { controller.pause().stop().destroy() }
    }

    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun selectionCentersAndEnabledSwitchesUseAccentWhileOutlinesAndLabelsRemainForeground() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val themes=ThemeManager(activity.getSharedPreferences("accent-controls",0))
            val root=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL }
            val box=android.widget.CheckBox(activity)
            val radio=android.widget.RadioButton(activity)
            val radioWidth=radio.buttonDrawable!!.intrinsicWidth
            val toggle=android.widget.Switch(activity)
            val originalThumb=toggle.thumbDrawable
            var changes=0;box.setOnCheckedChangeListener { _,_->changes++ }
            for(view in listOf(box,radio,toggle)) { BicycleDialogStyle.control(view,themes);root.addView(view) }
            activity.setContentView(root)
            assertEquals(radioWidth,radio.buttonDrawable!!.intrinsicWidth)
            fun pixel(button: android.widget.CompoundButton,x:Int=24):Int {
                val bitmap=android.graphics.Bitmap.createBitmap(48,48,android.graphics.Bitmap.Config.ARGB_8888)
                button.buttonDrawable!!.apply { setBounds(0,0,48,48);draw(android.graphics.Canvas(bitmap)) }
                val color=bitmap.getPixel(x,24);bitmap.recycle();return color
            }
            box.performClick();radio.performClick();toggle.performClick()
            assertEquals(1,changes)
            val light=CustomTheme("custom:accent-light","Light",ThemeTokens(0xffeeeeee.toInt(),0xffcccccc.toInt(),0xffaaaaaa.toInt(),0xff000000.toInt(),0xff002266.toInt()))
            assertTrue(themes.saveCustom(light).valid)
            for(theme in BicycleThemes.presets + light) {
                assertTrue(themes.activate(theme.id))
                val colors=theme.tokens
                assertEquals(colors.accent,pixel(box));assertEquals(colors.accent,pixel(radio))
                assertEquals(colors.foreground,pixel(box,6))
                assertEquals(colors.foreground,box.currentTextColor);assertEquals(colors.foreground,toggle.currentTextColor)
                assertEquals(colors.accent,toggle.thumbTintList!!.getColorForState(toggle.drawableState,0))
                val track=toggle.trackTintList!!.getColorForState(toggle.drawableState,0)
                assertEquals(colors.accent and 0x00ffffff,track and 0x00ffffff)
                assertEquals(102,android.graphics.Color.alpha(track))
                assertEquals(1,changes);assertTrue(box.isChecked);assertTrue(radio.isChecked)
                box.isEnabled=false
                assertEquals(127,android.graphics.Color.alpha(pixel(box)))
                box.isEnabled=true
                assertEquals(colors.accent,pixel(box))
                toggle.isChecked=false
                assertEquals(colors.foreground,toggle.thumbTintList!!.getColorForState(toggle.drawableState,0))
                toggle.isChecked=true
                assertEquals(radioWidth,radio.buttonDrawable!!.intrinsicWidth)
                assertSame(originalThumb,toggle.thumbDrawable)
            }
            box.performClick();assertEquals(2,changes)
            assertEquals(0,android.graphics.Color.alpha(pixel(box)))
        } finally { controller.pause().stop().destroy() }
    }

}
