package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapRouteStyleTest {
    @Test fun independentStylesFollowProfileCopyAndBackupAndRejectInvalidRanges() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("line-style-store",0)
        assertEquals(3f,MapRouteStyles.read(prefs,MapLineKind.RECORDED).widthDp,0f)
        assertEquals(50,MapRouteStyles.read(prefs,MapLineKind.SAVED).opacity)
        val styles=BikeUiConfigurationStore(prefs);styles.ensure(listOf(0,1))
        val custom=MapLineStyle(Color.MAGENTA,.5f,25)
        MapRouteStyles.write(prefs,MapLineKind.RECORDED,custom);styles.save(0);styles.apply(1)
        assertEquals(100,MapRouteStyles.read(prefs,MapLineKind.RECORDED).opacity)
        styles.apply(0);assertEquals(custom,MapRouteStyles.read(prefs,MapLineKind.RECORDED))
        assertEquals(6f,MapRouteStyles.read(prefs,MapLineKind.SAVED).widthDp,0f)
        styles.copy(0,1);assertEquals(custom,MapRouteStyles.read(prefs,MapLineKind.RECORDED))
        val backup=SettingsBackup(prefs,c.getSharedPreferences("line-route-export",0),ThemeManager(prefs))
        val bytes=backup.export(setOf(BackupSection.MAPS))
        prefs.edit().remove(MapRouteStyles.key).commit()
        backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.MAPS)))
        assertEquals(custom,MapRouteStyles.read(prefs,MapLineKind.RECORDED))
        val legacy=JSONObject(String(bytes));legacy.getJSONObject("sections").getJSONObject(BackupSection.MAPS.id).remove("routeStyles")
        backup.apply(backup.plan(backup.decode(legacy.toString().toByteArray()),setOf(BackupSection.MAPS)))
        assertEquals(custom,MapRouteStyles.read(prefs,MapLineKind.RECORDED))
        for(width in listOf(0f,.75f,8.5f,Float.NaN)) {
            assertTrue(runCatching { MapRouteStyles.write(prefs,MapLineKind.SAVED,custom.copy(widthDp=width)) }.isFailure)
        }
        assertTrue(runCatching { MapRouteStyles.write(prefs,MapLineKind.SAVED,custom.copy(opacity=101)) }.isFailure)
        assertEquals(custom,MapRouteStyles.read(prefs,MapLineKind.RECORDED))
    }
    @Test fun inlineStyleAndFixedPaletteImmediatelyPersistWithoutAffectingOtherLine() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("line-style-ui",0);val themes=ThemeManager(prefs)
            var changes=0
            val view=MapLineStyleView(a,prefs,themes,DialogNavigation()) { changes++ };a.setContentView(view)
            val body=view.findViewWithTag<View>("map_line_body_recorded")
            assertEquals(View.GONE,body.visibility)
            view.findViewWithTag<View>("map_line_toggle_recorded").performClick();assertEquals(View.VISIBLE,body.visibility)
            val bar=view.findViewWithTag<SeekBar>("map_line_width_recorded")
            assertEquals(15,bar.max);assertEquals(5,bar.progress)
            bar.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT))
            assertEquals(3.5f,MapRouteStyles.read(prefs,MapLineKind.RECORDED).widthDp,0f)
            view.findViewWithTag<EditText>("map_line_hex_recorded").setText("#00FF00")
            assertEquals(Color.GREEN,MapRouteStyles.read(prefs,MapLineKind.RECORDED).color)
            val opacity=view.findViewWithTag<SeekBar>("map_line_opacity_recorded")
            opacity.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT))
            assertEquals(99,MapRouteStyles.read(prefs,MapLineKind.RECORDED).opacity)
            view.findViewWithTag<View>("map_line_swatch_recorded").performClick()
            val dialog=ShadowAlertDialog.getLatestAlertDialog();shadowOf(Looper.getMainLooper()).idle()
            val root=dialog.window!!.decorView
            fun nodes(v:View):List<View> = listOf(v)+if(v is android.view.ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
            val palette=root.findViewWithTag<View>("map_color_palette")
            assertFalse(nodes(palette).any { it is EditText })
            assertEquals(9,palette.findViewWithTag<SeekBar>("map_palette_brightness").max)
            assertEquals(91,nodes(palette).filterIsInstance<android.widget.ImageButton>().size)
            val color=FixedColorPalette.spectrum.first().first()
            palette.findViewWithTag<View>("map_palette_${String.format(java.util.Locale.ROOT,"%06X",color and 0xffffff)}").performClick()
            assertTrue(dialog.isShowing)
            assertEquals(Color.GREEN,MapRouteStyles.read(prefs,MapLineKind.RECORDED).color)
            val grid=palette.findViewWithTag<android.view.ViewGroup>("map_palette_grid")
            val chosenIndex=(0 until grid.childCount).single { grid.getChildAt(it).isSelected }
            val brightness=palette.findViewWithTag<SeekBar>("map_palette_brightness")
            brightness.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT))
            assertTrue(grid.getChildAt(chosenIndex).isSelected)
            assertEquals(1,(0 until grid.childCount).count { grid.getChildAt(it).isSelected })
            val draft=(palette as FixedColorPalette).selectedColor
            assertEquals(FixedColorPalette.withBrightness(color,brightness.progress),draft)
            assertTrue(dialog.isShowing)
            dialog.dismiss();shadowOf(Looper.getMainLooper()).idle()
            assertEquals(draft,MapRouteStyles.read(prefs,MapLineKind.RECORDED).color)
            assertEquals(50,MapRouteStyles.read(prefs,MapLineKind.SAVED).opacity)
            view.findViewWithTag<EditText>("map_line_hex_recorded").setText("#GGGGGG")
            assertEquals(draft,MapRouteStyles.read(prefs,MapLineKind.RECORDED).color)
            view.findViewWithTag<View>("map_line_toggle_saved").performClick()
            val savedOpacity=view.findViewWithTag<SeekBar>("map_line_opacity_saved")
            savedOpacity.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT))
            assertEquals(51,MapRouteStyles.read(prefs,MapLineKind.SAVED).opacity)
            assertEquals(99,MapRouteStyles.read(prefs,MapLineKind.RECORDED).opacity)
            assertTrue(changes>=5)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun regularSixEdgePaletteFitsNarrowScreensAndBrightnessProvidesTenNeutralLevels() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("palette-geometry",0))
            val chosen=mutableListOf<Int>()
            val palette=FixedColorPalette(a,themes,Color.WHITE) { chosen.add(it) };a.setContentView(palette)
            val grid=palette.findViewWithTag<android.view.ViewGroup>("map_palette_grid")
            assertEquals(listOf(6,7,8,9,10,11,10,9,8,7,6),FixedColorPalette.spectrum.map { it.size })
            assertEquals(Color.WHITE,FixedColorPalette.spectrum.flatten()[45])
            for(width in listOf(288,320,360)) {
                palette.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.AT_MOST))
                palette.layout(0,0,width,palette.measuredHeight)
                for(i in 0 until grid.childCount) {
                    val chip=grid.getChildAt(i)
                    assertTrue(chip.left>=0);assertTrue(chip.right<=grid.width)
                    val drawable=chip.background
                    drawable.setBounds(0,0,chip.width,chip.height)
                    val path=HexColorDrawable::class.java.getDeclaredField("path").apply { isAccessible=true }.get(drawable) as android.graphics.Path
                    val points=path.approximate(.01f)
                    val lengths=(3 until points.size step 3).map { n -> kotlin.math.hypot(points[n+1]-points[n-2],points[n+2]-points[n-1]) }.filter { it>.1f }
                    assertEquals(6,lengths.size)
                    assertTrue("All hex sides equal",lengths.max()-lengths.min()<.01f)
                }
                val centre=grid.getChildAt(45)
                fun distance(v:View)=kotlin.math.hypot((v.left+v.right-centre.left-centre.right)/2f,(v.top+v.bottom-centre.top-centre.bottom)/2f)
                val nearest=(0 until grid.childCount).filter { it!=45 }.map { distance(grid.getChildAt(it)) }.sorted().take(6)
                assertTrue("Hex centres form a regular lattice",nearest.max()-nearest.min()<1.5f)
            }
            val slider=palette.findViewWithTag<SeekBar>("map_palette_brightness")
            val neutrals=mutableSetOf<Int>()
            for(step in 0..9) {
                slider.progress=step
                val chip=grid.getChildAt(45)
                val image=Bitmap.createBitmap(chip.width,chip.height,Bitmap.Config.ARGB_8888)
                chip.draw(Canvas(image));val color=image.getPixel(chip.width/2,chip.height/2);image.recycle()
                assertEquals(Color.red(color),Color.green(color));assertEquals(Color.green(color),Color.blue(color));neutrals.add(color)
            }
            assertEquals(10,neutrals.size);assertTrue(Color.BLACK in neutrals);assertTrue(Color.WHITE in neutrals)
            assertTrue("Brightness previews do not persist a selection",chosen.isEmpty())
            val image=Bitmap.createBitmap(palette.width,palette.height,Bitmap.Config.ARGB_8888)
            image.eraseColor(themes.active.tokens.secondary);palette.draw(Canvas(image))
            java.io.File("build/reports/ui/map-color-palette.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            slider.progress=4;grid.getChildAt(45).performClick()
            assertEquals(listOf(FixedColorPalette.withBrightness(Color.WHITE,4)),chosen)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun screenshotPreviewShowsOpacityAndUsesActualNightPalette() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("line-style-render",0)
        val preview=MapLinePreview(c,prefs,MapLineKind.RECORDED)
        preview.layout(0,0,360,150)
        fun image():Bitmap=Bitmap.createBitmap(360,150,Bitmap.Config.ARGB_8888).also { preview.draw(Canvas(it)) }
        val style=MapLineStyle(Color.RED,8f,0)
        MapRouteStyles.write(prefs,MapLineKind.RECORDED,style);val clear=image().getPixel(180,60)
        MapRouteStyles.write(prefs,MapLineKind.RECORDED,style.copy(opacity=50));val middle=image().getPixel(180,60)
        assertTrue(kotlin.math.abs(Color.red(middle)-(Color.red(clear)+255)/2)<=3)
        assertTrue(kotlin.math.abs(Color.green(middle)-Color.green(clear)/2)<=3)
        assertNotEquals(clear,middle)
        MapRouteStyles.write(prefs,MapLineKind.RECORDED,style)
        prefs.edit().putBoolean("mapNight",true).commit();assertNotEquals(clear,image().getPixel(180,60))
    }
}
