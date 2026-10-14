package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiConventionsTest {
    private fun views(v: View)=testDescendants(v)
    private fun latest(): AlertDialog { shadowOf(Looper.getMainLooper()).idle();return ShadowAlertDialog.getLatestAlertDialog() }
    private fun click(v: View,tag: String) { views(v).single { it.tag==tag }.performClick() }
    private fun layout(v: View) {
        v.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2130,View.MeasureSpec.EXACTLY));v.layout(0,0,1080,2130)
    }
    @Test fun menuUsesDoubleHeightRowsAdvancedIsSeparateAndEmptyFooterHasNoSpace() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();views(activity.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
            val menu=latest();val decor=menu.window!!.decorView;layout(decor)
            val entries=views(decor).filter { it.tag?.toString()?.startsWith("settings_")==true && it.isClickable }
            assertEquals(7,entries.size)
            entries.forEach {
                assertEquals(BicycleDesign.dp(activity,BicycleDesign.touchSize*2),it.minimumHeight)
                val text=views(it).filterIsInstance<TextView>().single()
                assertEquals(BicycleDesign.bodySp*2*activity.resources.displayMetrics.density*activity.resources.configuration.fontScale,text.textSize,.1f)
                val icon=views(it).filterIsInstance<ImageView>().first()
                assertEquals(BicycleDesign.dp(activity,BicycleDesign.iconSize*2),icon.width)
            }
            assertFalse(views(decor).any { it.tag=="bicycle_fullscreen_footer" })
            val title=views(decor).single { it.tag=="bicycle_dialog_title" }
            assertEquals(decor.width/2f,(title.left+title.right)/2f,1f)
            click(decor,"settings_ADVANCED");val advanced=latest();assertNotSame(menu,advanced)
            assertEquals(View.GONE,menu.window!!.decorView.visibility)
            assertFalse(views(advanced.window!!.decorView).any { it.tag=="settings_advanced_content" })
            val advancedText=views(advanced.window!!.decorView).filterIsInstance<SettingsMenuView>().single().let { views(it).filterIsInstance<TextView>().first() }
            assertEquals(BicycleDesign.bodySp*activity.resources.displayMetrics.density*activity.resources.configuration.fontScale,advancedText.textSize,.1f)
            testBack(advanced);assertTrue(menu.isShowing);assertEquals(View.VISIBLE,menu.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun profileAndDateChangesPersistWithoutSaveAndInvalidNumberPreservesLastValue() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            views(activity.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
            click(latest().window!!.decorView,"settings_PROFILE");val profile=latest()
            val body=views(profile.window!!.decorView).filterIsInstance<RiderProfileView>().single()
            val headings=views(body).filterIsInstance<TextView>().filter { it.isAccessibilityHeading }
            assertTrue(headings.isNotEmpty());headings.forEach { assertEquals(900,it.typeface.weight);assertEquals(BicycleDesign.titleSp*activity.resources.displayMetrics.density*activity.resources.configuration.fontScale,it.textSize,.1f) }
            val themes=runtime.themes
            val field=FieldPickerView(activity,LiveData(),GpsValues(),null,PulseValues(),DashboardField("SPEED"),false,themes) {}
            val data=DataSettingsView(activity,themes) {}
            for(view in listOf(field,data,GraphOverlayPicker(activity,themes,emptyList()))) {
                val sections=views(view).filterIsInstance<TextView>().filter { it.isAccessibilityHeading }
                assertTrue(sections.isNotEmpty());sections.forEach {
                    assertEquals(900,it.typeface.weight)
                    assertEquals(BicycleDesign.titleSp*activity.resources.displayMetrics.density*activity.resources.configuration.fontScale,it.textSize,.1f)
                }
            }
            val height=views(body).filterIsInstance<EditText>().single { it.tag=="rider_height" }
            height.setText("180");assertEquals(180,runtime.riderProfiles.read().heightCm)
            height.setText("9999");assertEquals(180,runtime.riderProfiles.read().heightCm)
            height.setText("181");assertEquals(181,runtime.riderProfiles.read().heightCm)
            click(body,"rider_birth");val date=latest()
            val picker=views(date.window!!.decorView).filterIsInstance<DatePicker>().single()
            val year=picker.findViewById<TextView>(picker.resources.getIdentifier("date_picker_header_year","id","android"))
            val day=picker.findViewById<TextView>(picker.resources.getIdentifier("date_picker_header_date","id","android"))
            assertEquals(18f*(picker.resources.displayMetrics.density*picker.resources.configuration.fontScale),year.textSize,.1f)
            assertEquals(28f*(picker.resources.displayMetrics.density*picker.resources.configuration.fontScale),day.textSize,.1f)
            picker.updateDate(1990,6,13);assertEquals(java.time.LocalDate.of(1990,7,13),runtime.riderProfiles.read().birthDate)
            testBack(date);testBack(profile);assertEquals(181,runtime.riderProfiles.read().heightCm)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun mapIconsShowCurrentStateAndZoomDoesNotOverlapRightValue() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val root=activity.findViewById<ViewGroup>(android.R.id.content)
            val runtime=RideRuntime.get(activity);runtime.preferences.edit().putString(MapFieldSelection.key(2),"BATTERY").commit()
            val panel=views(root).filterIsInstance<MapPanel>().single();panel.refreshSettings();layout(root)
            fun matches(button: Button,icon: BicycleIcon) {
                val actual=button.compoundDrawables[0]!!
                val expected=BicycleIcons.drawable(activity,icon,android.graphics.Color.WHITE)
                fun draw(d: android.graphics.drawable.Drawable): Bitmap {
                    val b=Bitmap.createBitmap(72,72,Bitmap.Config.ARGB_8888);d.setBounds(0,0,72,72);d.draw(Canvas(b));return b
                }
                assertTrue(icon.name,draw(actual).sameAs(draw(expected)))
            }
            val night=views(panel).single { it.tag=="map_night" } as Button
            matches(night,BicycleIcon.Sun);night.performClick();matches(night,BicycleIcon.Moon)
            val direction=views(panel).filterIsInstance<Button>().single { it.contentDescription?.toString().orEmpty().startsWith("Fahrtrichtung oben") }
            matches(direction,BicycleIcon.HeadingUp);direction.performClick();matches(direction,BicycleIcon.Compass)
            val graphic=direction.compoundDrawables[0];panel.refreshSettings();layout(root);assertSame(graphic,direction.compoundDrawables[0])
            panel.setEditing(true);layout(root)
            val values=views(panel).single { it.tag=="map_data_fields" }
            val zoom=views(panel).single { it.tag=="map_zoom_controls" }
            assertEquals(panel.height,values.bottom);assertTrue(zoom.bottom<=values.top)
            val finish=views(panel).single { it.tag=="dashboard_editor_finish" }
            assertTrue(finish.paddingLeft>=BicycleDesign.dp(activity,BicycleDesign.horizontalPadding))
        } finally { controller.pause().stop().destroy() }
    }
}
