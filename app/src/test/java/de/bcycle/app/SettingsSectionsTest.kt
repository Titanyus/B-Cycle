package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.View
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="de-rDE-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsSectionsTest {
    @Test fun newSectionsUseOneScrollHostAndNumericRowsStayInlineAcrossThemes() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            runtime.themes.activate("preset:black-ops")
            fun open(method:String):View {
                MainActivity::class.java.getDeclaredMethod(method).apply { isAccessible=true }.invoke(activity)
                shadowOf(Looper.getMainLooper()).idle()
                return ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.apply {
                    measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY))
                    layout(0,0,360,640)
                }
            }
            fun headings(root:View)=testDescendants(root).filterIsInstance<BicycleSectionHeader>().map { it.contentDescription.toString() }
            val design=open("dashboardSettings")
            assertEquals(listOf("Theme","Dashboard-Layout","Trainingshilfe","Fahrmodus-Anzeige"),headings(design))
            assertEquals(1,testDescendants(design).filterIsInstance<android.widget.ScrollView>().size)
            testBack(ShadowAlertDialog.getLatestAlertDialog())
            val data=open("dataSettings")
            assertEquals(listOf("GLOBALE DATEN","STRECKENDATEN"),headings(data))
            assertEquals(1,testDescendants(data).filterIsInstance<BicycleStickyScrollView>().size)
            testBack(ShadowAlertDialog.getLatestAlertDialog())
            val profile=showRiderProfile(activity,runtime.themes,DialogNavigation(),runtime.riderProfiles)
            shadowOf(Looper.getMainLooper()).idle()
            val root=profile.window!!.decorView
            for(width in listOf(360,320)) {
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY));root.layout(0,0,width,640)
                assertEquals(listOf("Persönliche Daten","Trainingsparameter","Herzfrequenzzonen","Leistungszonen"),headings(root))
                assertEquals(1,testDescendants(root).filterIsInstance<android.widget.ScrollView>().size)
                testDescendants(root).filterIsInstance<EditText>().forEach { edit ->
                    val row=edit.parent as android.widget.LinearLayout
                    assertEquals(3,row.childCount)
                    assertTrue(row.getChildAt(0).width>0)
                    assertEquals(BicycleDesign.formNumberWidth,edit.width)
                    assertEquals(activity.resources.getFont(R.font.bebas_neue),edit.typeface)
                    assertEquals(BicycleDesign.analysisMetricSp,edit.textSize,0f)
                    assertEquals(android.graphics.Color.TRANSPARENT,(edit.background as GradientDrawable).color!!.defaultColor)
                }
            }
            val height=root.findViewWithTag<EditText>("rider_height")
            height.setText("185");assertEquals(185,runtime.riderProfiles.read().heightCm)
            runtime.themes.activate("preset:solar-flare")
            val bitmap=Bitmap.createBitmap(116,48,Bitmap.Config.ARGB_8888)
            height.background.setBounds(0,0,116,48);height.background.draw(Canvas(bitmap))
            assertEquals(runtime.themes.active.tokens.tertiary,bitmap.getPixel(58,0));bitmap.recycle()
        } finally { controller.pause().stop().destroy() }
    }
}
