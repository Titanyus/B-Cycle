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
    @Test fun adjacentChoiceRowsUseDropdownHeightWithoutExtraGapsAndInputsUseNarrowOutlinedStyle() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("compact-choice",0))
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL }
            val choices=(0..2).map { android.widget.CheckBox(a).apply { text="Choice $it";BicycleDialogStyle.control(this,themes) } }
            choices.forEach { root.addView(it,LinearLayout.LayoutParams(-1,-2)) }
            val input=BicycleComponents.input(a,themes);root.addView(input)
            a.setContentView(root)
            root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));root.layout(0,0,360,600)
            choices.zipWithNext().forEach { (before,after) -> assertEquals(before.bottom,after.top) }
            val spinner=android.widget.Spinner(a).apply { adapter=android.widget.ArrayAdapter(a,android.R.layout.simple_spinner_dropdown_item,listOf("Choice")) }
            BicycleDialogStyle.spinner(spinner,themes)
            val dropdown=spinner.adapter.getDropDownView(0,null,root)
            dropdown.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.AT_MOST))
            choices.forEach { assertEquals(dropdown.measuredHeight,it.height);assertTrue(it.height>=BicycleDesign.dp(a,BicycleDesign.touchSize)) }
            assertEquals(a.resources.getFont(R.font.league_gothic),input.typeface)
            assertEquals(BicycleDesign.settingsMetricSp*a.resources.displayMetrics.scaledDensity,input.textSize,0.01f)
            assertEquals(android.graphics.Color.TRANSPARENT,(input.background as GradientDrawable).color!!.defaultColor)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun formSpacingSurvivesLateLayoutParametersAndReattachmentWithoutDoublingHorizontalGaps() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val themes=ThemeManager(activity.getSharedPreferences("spacing",0))
            val root=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL }
            val button=BicycleComponents.action(activity,themes,"Action") {}
            BicycleDialogStyle.control(button,themes)
            val spinner=android.widget.Spinner(activity).apply { adapter=android.widget.ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,listOf("A","B")) }
            BicycleDialogStyle.spinner(spinner,themes)
            val popupBackground=spinner.popupBackground as GradientDrawable
            assertEquals(themes.active.tokens.tertiary,popupBackground.color!!.defaultColor)
            assertEquals(BicycleDesign.dp(activity,BicycleDesign.inputRadius).toFloat(),popupBackground.cornerRadius,0f)
            val popupPadding=android.graphics.Rect();assertTrue(popupBackground.getPadding(popupPadding))
            assertEquals(BicycleDesign.dp(activity,BicycleDesign.controlBorder+BicycleDesign.popupGlowInset),popupPadding.left)
            assertEquals(popupPadding.left,popupPadding.top)

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
            assertEquals(light.tokens.primary, ((header.background as android.graphics.drawable.LayerDrawable).getDrawable(0) as GradientDrawable).color!!.defaultColor)
            assertEquals(android.graphics.Color.TRANSPARENT, (input.background as GradientDrawable).color!!.defaultColor)
            assertEquals(BicycleDesign.linePixels(activity), divider.height)
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
            assertEquals(tokens.iconColor(ThemeRole.PRIMARY) and 0xffffff, pixel(highlight) and 0xffffff)
            assertEquals(before, listOf(standard.width, standard.height, highlight.width, highlight.height))
            assertEquals(tokens.foreground, BicycleIcons.color(tokens, BicycleIcon.LocalSaved, negative = true))
            assertEquals(tokens.iconColor(ThemeRole.PRIMARY), BicycleIcons.color(tokens, BicycleIcon.CloudUploaded))
            assertEquals(tokens.foreground, BicycleIcons.color(tokens, BicycleIcon.ShowOnMap))
            manager.activate(BicycleThemes.red.id)
            assertEquals(BicycleThemes.red.tokens.primary and 0xffffff, pixel(highlight) and 0xffffff)
        } finally { controller.pause().stop().destroy() }
    }

    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun flatListChoiceSwitchesMoveThumbRemainIndependentAndFollowTheme() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();activity.getSharedPreferences("bike",0).edit().putBoolean(BicycleAppearance.flatKey,true).commit();val themes=ThemeManager(activity.getSharedPreferences("circle-ui",0))
            val root=LinearLayout(activity)
            val first=android.widget.CheckBox(activity);val second=android.widget.CheckBox(activity)
            val toggle=android.widget.Switch(activity)
            for(view in listOf(first,second,toggle)) { BicycleDialogStyle.control(view,themes);root.addView(view) }
            val thumb=toggle.thumbDrawable
            activity.setContentView(root)
            fun layoutChoices() {
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,800)
            }
            layoutChoices()
            fun draw(box: android.widget.CheckBox): android.graphics.Bitmap {
                val bitmap=android.graphics.Bitmap.createBitmap(48,24,android.graphics.Bitmap.Config.ARGB_8888)
                (box.compoundDrawablesRelative[2]!!.current as android.graphics.drawable.InsetDrawable).drawable!!.apply { setBounds(0,0,48,24);draw(android.graphics.Canvas(bitmap)) }
                return bitmap
            }
            assertEquals(0,android.graphics.Color.alpha(draw(first).getPixel(24,12)))
            assertEquals(0,android.graphics.Color.alpha(draw(first).getPixel(2,2)))
            val width=first.compoundDrawablesRelative[2]!!.intrinsicWidth
            first.performClick();second.performClick()
            assertTrue(first.isChecked);assertTrue(second.isChecked)
            assertEquals(themes.active.tokens.primary,draw(first).getPixel(36,12))
            first.performClick();assertFalse(first.isChecked);assertTrue(second.isChecked)
            assertEquals(0,android.graphics.Color.alpha(draw(first).getPixel(24,12)))
            val light=CustomTheme("custom:circle-light","Light",ThemeTokens(0xffeeeeee.toInt(),0xffcccccc.toInt(),0xffaaaaaa.toInt(),0xff000000.toInt()))
            assertTrue(themes.saveCustom(light).valid);assertTrue(themes.activate(light.id));layoutChoices()
            assertEquals(light.tokens.primary,draw(second).getPixel(36,12))
            assertEquals(width,first.compoundDrawablesRelative[2]!!.intrinsicWidth)
            assertSame(thumb,toggle.thumbDrawable)
        } finally { controller.pause().stop().destroy() }
    }

    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun flatListChoiceAndNativeSwitchThumbsUsePrimaryWhileOutlinesAndLabelsRemainForeground() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();activity.getSharedPreferences("bike",0).edit().putBoolean(BicycleAppearance.flatKey,true).commit();val themes=ThemeManager(activity.getSharedPreferences("accent-controls",0))
            val root=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL }
            val box=android.widget.CheckBox(activity)
            val radio=android.widget.RadioButton(activity)
            val radioWidth=BicycleDesign.dp(activity,BicycleDesign.switchWidth)
            val toggle=android.widget.Switch(activity)
            var changes=0;box.setOnCheckedChangeListener { _,_->changes++ }
            for(view in listOf(box,radio,toggle)) { BicycleDialogStyle.control(view,themes);root.addView(view) }
            val originalThumb=toggle.thumbDrawable
            activity.setContentView(root)
            fun layoutChoices() {
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,800)
            }
            layoutChoices()
            assertEquals(radioWidth+BicycleDesign.dp(activity,BicycleDesign.choiceTextGap),radio.compoundDrawablesRelative[2]!!.intrinsicWidth)
            fun pixel(button: android.widget.CompoundButton,x:Int=36):Int {
                val bitmap=android.graphics.Bitmap.createBitmap(48,24,android.graphics.Bitmap.Config.ARGB_8888)
                (button.compoundDrawablesRelative[2]!!.current as android.graphics.drawable.InsetDrawable).drawable!!.apply { setBounds(0,0,48,24);draw(android.graphics.Canvas(bitmap)) }
                val color=bitmap.getPixel(x,12);bitmap.recycle();return color
            }
            box.performClick();radio.performClick();toggle.performClick()
            assertEquals(1,changes)
            val light=CustomTheme("custom:accent-light","Light",ThemeTokens(0xffeeeeee.toInt(),0xffcccccc.toInt(),0xffaaaaaa.toInt(),0xff000000.toInt(),0xff002266.toInt()))
            assertTrue(themes.saveCustom(light).valid)
            for(theme in BicycleThemes.presets + light) {
                assertTrue(themes.activate(theme.id));layoutChoices()
                val colors=theme.tokens
                assertEquals(colors.primary,pixel(box));assertEquals(colors.primary,pixel(radio))
                assertEquals(colors.foreground and 0xffffff,pixel(box,0) and 0xffffff)
                assertEquals(colors.foreground,box.currentTextColor);assertEquals(colors.foreground,toggle.currentTextColor)
                assertNull(toggle.thumbTintList)
                assertNull(toggle.trackTintList)
                fun thumbPixel():Int {
                    val bitmap=android.graphics.Bitmap.createBitmap(24,24,android.graphics.Bitmap.Config.ARGB_8888)
                    toggle.thumbDrawable.setBounds(0,0,24,24);toggle.thumbDrawable.draw(android.graphics.Canvas(bitmap))
                    return bitmap.getPixel(12,12).also { bitmap.recycle() }
                }
                assertEquals(colors.primary,thumbPixel())
                fun trackPixel(x:Int,y:Int):Int {
                    val track=toggle.trackDrawable
                    val bitmap=android.graphics.Bitmap.createBitmap(48,24,android.graphics.Bitmap.Config.ARGB_8888)
                    track.setBounds(0,0,48,24);track.draw(android.graphics.Canvas(bitmap))
                    return bitmap.getPixel(x,y).also { bitmap.recycle() }
                }
                assertEquals(0,android.graphics.Color.alpha(trackPixel(24,12)))
                assertEquals(colors.foreground,trackPixel(24,0))
                assertEquals(1,changes);assertTrue(box.isChecked);assertTrue(radio.isChecked)
                box.isEnabled=false
                assertEquals(127,android.graphics.Color.alpha(pixel(box)))
                box.isEnabled=true
                assertEquals(colors.primary,pixel(box))
                toggle.isChecked=false
                assertNull(toggle.thumbTintList)
                assertEquals(colors.foreground,thumbPixel())
                assertEquals(colors.foreground,trackPixel(24,0))
                toggle.isEnabled=false
                assertEquals(127,android.graphics.Color.alpha(trackPixel(24,0)))
                toggle.isEnabled=true;toggle.isChecked=true
                assertEquals(radioWidth+BicycleDesign.dp(activity,BicycleDesign.choiceTextGap),radio.compoundDrawablesRelative[2]!!.intrinsicWidth)
                assertSame(originalThumb,toggle.thumbDrawable)
            }
            box.performClick();assertEquals(2,changes)
            assertEquals(0,android.graphics.Color.alpha(pixel(box)))
        } finally { controller.pause().stop().destroy() }
    }

}
