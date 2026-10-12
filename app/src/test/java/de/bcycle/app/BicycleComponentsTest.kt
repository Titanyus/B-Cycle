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

}
