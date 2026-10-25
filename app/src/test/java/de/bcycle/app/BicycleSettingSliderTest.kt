package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.view.View
import android.widget.*
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
class BicycleSettingSliderTest {
    private fun layout(view:View,width:Int=360) { repeat(2) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1200,View.MeasureSpec.AT_MOST));view.layout(0,0,width,view.measuredHeight)
    } }
    @Test fun pillThumbKeepsItsValueVisibleAtBothEndsAndRowPitchMatchesRecordingDropdowns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val profile=BikeProfileStore(a,a.getSharedPreferences("bike",0)).current()
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL }
            val sources=RecordingSourcesView(a,themes,profile,true,RecordingSources.defaults(profile.recording,false),{true}) {}
            root.addView(sources)
            val sliders=listOf(10,11,100,15,180).map { maximum ->
                BicycleSettingSlider(a,themes) { "$it s" }.apply { max=maximum;progress=maximum/2 }
            }
            sliders.forEach { root.addView(settingSliderRow(a,themes,"Anzeigedauer",it)) }
            a.setContentView(root);shadowOf(android.os.Looper.getMainLooper()).idle()
            for(width in listOf(320,360,412)) {
                layout(root,width)
                val choices=testDescendants(sources).filterIsInstance<Spinner>()
                fun center(v:View):Int { val r=Rect(0,0,v.width,v.height);root.offsetDescendantRectToMyCoords(v,r);return r.centerY() }
                val dropdownPitch=center(choices[1])-center(choices[0])
                assertEquals(dropdownPitch,center(sliders[1])-center(sliders[0]))
                for(slider in sliders) {
                    assertEquals(48,slider.height);assertTrue(slider.width>slider.thumb.intrinsicWidth)
                    for(value in listOf(slider.min,slider.max)) {
                        slider.progress=value
                        val image=Bitmap.createBitmap(slider.width,slider.height,Bitmap.Config.ARGB_8888);slider.draw(Canvas(image))
                        val area=Rect(slider.thumb.bounds).apply { offset(slider.paddingLeft-slider.thumbOffset,slider.paddingTop) }
                        assertTrue("Pill fits left at $value",area.left>=0);assertTrue("Pill fits right at $value",area.right<=slider.width)
                        var foreground=0
                        for(y in area.top.coerceAtLeast(0) until area.bottom.coerceAtMost(image.height)) for(x in area.left until area.right) {
                            val pixel=image.getPixel(x,y)
                            fun distance(a:Int,b:Int)=kotlin.math.abs(Color.red(a)-Color.red(b))+kotlin.math.abs(Color.green(a)-Color.green(b))+kotlin.math.abs(Color.blue(a)-Color.blue(b))
                            if(x>=area.left+8 && x<area.right-8 && y>=area.top+4 && y<area.bottom-4 && distance(pixel,themes.active.tokens.foreground)<distance(pixel,themes.active.tokens.primary)) foreground++
                        }
                        assertTrue("Value text drawn inside thumb at width=$width max=${slider.max} value=$value area=$area",foreground>0);image.recycle()
                    }
                }
            }
            sliders.forEach { it.progress=it.max/2 }
            layout(root)
            val image=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888);image.eraseColor(themes.active.tokens.secondary);root.draw(Canvas(image))
            java.io.File("build/reports/ui/setting-slider-pills.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
        } finally { host.pause().stop().destroy() }
    }
}
