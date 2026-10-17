package de.bcycle.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "de-rDE-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RiderProfileViewTest {
    private fun views(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun edit(dialog: AlertDialog, name: String) = dialog.window!!.decorView.findViewWithTag<EditText>("rider_$name")
    private fun measure(dialog: AlertDialog, width: Int=360, height: Int=640) {
        shadowOf(Looper.getMainLooper()).idle()
        val decor=dialog.window!!.decorView
        decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
        decor.layout(0,0,width,height)
    }
    @Test fun sectionsCollapseIndependentlyAndBirthBorderFitsTheEntireRow() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("rider-accordion",0))
            val store=RiderProfileStore(a.getSharedPreferences("rider-accordion",0))
            val screen=showRiderProfile(a,themes,DialogNavigation(),store)
            for(width in listOf(320,360)) {
                measure(screen,width)
                val root=screen.window!!.decorView
                val birth=root.findViewWithTag<Button>("rider_birth")
                val row=birth.parent as ViewGroup
                assertEquals(0,(birth.layoutParams as ViewGroup.MarginLayoutParams).topMargin)
                assertTrue(birth.bottom<=row.height);assertTrue(birth.top>=0)
                val bitmap=android.graphics.Bitmap.createBitmap(row.width,row.height,android.graphics.Bitmap.Config.ARGB_8888)
                row.draw(android.graphics.Canvas(bitmap))
                assertTrue(android.graphics.Color.alpha(bitmap.getPixel(birth.left+birth.width/2,birth.bottom-1))>0);bitmap.recycle()
            }
            val root=screen.window!!.decorView
            val headers=views(root).filterIsInstance<BicycleSectionHeader>()
            assertEquals(4,headers.size);assertTrue(headers.all { it.expanded==true })
            edit(screen,"height").setText("182")
            headers[0].performClick();assertEquals(false,headers[0].expanded)
            assertEquals(true,headers[1].expanded)
            headers[0].performClick();assertEquals("182",edit(screen,"height").text.toString())
            assertEquals(182,store.read().heightCm)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun profileChangesPersistImmediatelyAndHeaderAndAndroidBackReturnToMenu() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            views(activity.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
            val parent=ShadowAlertDialog.getLatestAlertDialog()
            val tile=parent.window!!.decorView.findViewWithTag<View>("settings_PROFILE")
            tile.performClick();val screen=ShadowAlertDialog.getLatestAlertDialog();measure(screen)
            assertEquals(BicycleDesign.settingsMetricSp*activity.resources.displayMetrics.scaledDensity,edit(screen,"weight").textSize,.01f)
            edit(screen,"weight").setText("76,");assertEquals(RiderProfile(),runtime.riderProfiles.read())
            edit(screen,"weight").setText("76,4");edit(screen,"resting_heart_rate").setText("55");edit(screen,"maximum_heart_rate").setText("185");edit(screen,"ftp").setText("245")
            assertEquals(RiderProfile(weightKg=76.4,restingHeartRate=55,maximumHeartRate=185,ftpW=245),runtime.riderProfiles.read())
            testBack(screen);assertEquals(View.VISIBLE,parent.window!!.decorView.visibility)
            tile.performClick();val again=ShadowAlertDialog.getLatestAlertDialog();measure(again)
            edit(again,"ftp").setText("260");again.onBackPressed();shadowOf(Looper.getMainLooper()).idle()
            assertEquals(260,runtime.riderProfiles.read().ftpW);assertEquals(View.VISIBLE,parent.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun liveZonesRespondAndInvalidFieldsPreserveLastValidProfile() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val store=RiderProfileStore(activity.getSharedPreferences("bike",0))
            val screen=showRiderProfile(activity,ThemeManager(activity.getSharedPreferences("bike",0)),DialogNavigation(),store);measure(screen)
            val hint=screen.window!!.decorView.findViewWithTag<TextView>("rider_zone_hint")
            val zones=screen.window!!.decorView.findViewWithTag<View>("rider_zones")
            assertEquals(activity.getString(R.string.rider_hint_both),hint.text);assertEquals(View.GONE,zones.visibility)
            edit(screen,"resting_heart_rate").setText("55");assertEquals(activity.getString(R.string.rider_hint_maximum),hint.text)
            edit(screen,"maximum_heart_rate").setText("185")
            val first=screen.window!!.decorView.findViewWithTag<TextView>("rider_zone_value_1")
            assertEquals((BicycleDesign.settingsMetricSp*activity.resources.displayMetrics.scaledDensity).toInt(),first.autoSizeMaxTextSize)
            assertEquals("120–132",first.text);assertEquals(View.VISIBLE,zones.visibility)
            edit(screen,"maximum_heart_rate").setText("195");assertEquals("125–138",first.text)
            edit(screen,"weight").setText("bad");assertEquals(View.VISIBLE,zones.visibility)
            edit(screen,"maximum_heart_rate").setText("55");assertEquals(View.GONE,zones.visibility);assertEquals(activity.getString(R.string.rider_hint_order),hint.text)
            edit(screen,"maximum_heart_rate").setText("185x");assertEquals(activity.getString(R.string.rider_hint_invalid),hint.text)
            edit(screen,"maximum_heart_rate").setText("185");edit(screen,"resting_heart_rate").setText("")
            assertEquals(activity.getString(R.string.rider_hint_resting),hint.text)
            assertEquals(RiderProfile(restingHeartRate=55,maximumHeartRate=195),store.read());screen.dismiss()
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun birthPickerLimitsFutureDatesSupportsClearAndDisplaysAgeAndLocale() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val store=RiderProfileStore(activity.getSharedPreferences("bike",0))
            val screen=showRiderProfile(activity,ThemeManager(activity.getSharedPreferences("bike",0)),DialogNavigation(),store);measure(screen)
            val birth=screen.window!!.decorView.findViewWithTag<Button>("rider_birth")
            val age=screen.window!!.decorView.findViewWithTag<TextView>("rider_age")
            assertEquals(View.GONE,age.visibility)
            birth.performClick();val pickerScreen=ShadowAlertDialog.getLatestAlertDialog()
            val picker=pickerScreen.window!!.decorView.findViewWithTag<DatePicker>("rider_birth_picker")
            assertEquals(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),picker.maxDate)
            picker.updateDate(1972,9,13);testBack(pickerScreen)
            val date=LocalDate.of(1972,10,13)
            assertEquals(RiderProfileView.formatBirthDate(date,Locale.GERMANY),birth.text)
            assertEquals(View.VISIBLE,age.visibility);assertTrue(age.text.toString().contains(RiderProfile(birthDate=date).ageYears().toString()))
            assertEquals(date,store.read().birthDate)
            birth.performClick();val cancelled=ShadowAlertDialog.getLatestAlertDialog()
            cancelled.window!!.decorView.findViewWithTag<DatePicker>("rider_birth_picker").updateDate(1980,0,1)
            testBack(cancelled)
            assertEquals(RiderProfileView.formatBirthDate(LocalDate.of(1980,1,1),Locale.GERMANY),birth.text)
            screen.window!!.decorView.findViewWithTag<View>("rider_birth_clear").performClick();assertEquals(View.GONE,age.visibility)
            testBack(screen);assertNull(store.read().birthDate)
            assertNotEquals(RiderProfileView.formatBirthDate(date,Locale.GERMANY),RiderProfileView.formatBirthDate(date,Locale.US))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun everyThemeKeepsFixedHeaderFooterScrollAndTypographyOnSmallDisplays() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val prefs=activity.getSharedPreferences("bike",0)
            val store=RiderProfileStore(prefs);assertTrue(store.write(RiderProfile(heightCm=182,weightKg=76.4,restingHeartRate=55,maximumHeartRate=185,ftpW=245)))
            val themes=ThemeManager(prefs);val screen=showRiderProfile(activity,themes,DialogNavigation(),store)
            for((width,height) in listOf(280 to 640,360 to 640,480 to 800)) {
                measure(screen,width,height)
                val root=screen.window!!.decorView.findViewWithTag<ViewGroup>("bicycle_fullscreen")
                val header=root.getChildAt(0);assertNull(root.findViewWithTag<View>("bicycle_fullscreen_footer"))
                val scroll=root.findViewWithTag<RiderProfileView>("rider_profile")
                assertEquals(BicycleDesign.headerHeight,header.height);assertEquals(height,scroll.bottom)
                assertEquals(header.bottom,scroll.top);assertEquals(height,scroll.bottom)
                assertEquals(views(root).filterIsInstance<ScrollView>().joinToString { "${it.javaClass.simpleName}:${it.tag}:shown=${it.isShown}" },1,views(root).count { it is ScrollView && it.isShown && it.tag != "bicycle_fullscreen_footer" })
                scroll.fullScroll(View.FOCUS_DOWN);measure(screen,width,height)
                assertTrue(scroll.scrollY>0);val top=header.top;val bottom=scroll.bottom
                val geometry=views(root).map { listOf(it.left,it.top,it.width,it.height) }
                for(theme in BicycleThemes.presets) {
                    assertTrue(themes.activate(theme.id));shadowOf(Looper.getMainLooper()).idle();measure(screen,width,height)
                    assertEquals(geometry,views(root).map { listOf(it.left,it.top,it.width,it.height) })
                    assertEquals(top,header.top);assertEquals(bottom,scroll.bottom)
                    assertTrue(views(root).filterIsInstance<TextView>().all { it.currentTextColor == theme.tokens.foreground || it.tag=="rider_unit" || !it.isEnabled })
                    views(root).filterIsInstance<AnalysisValueRow>().forEach { row ->
                        val value=row.getChildAt(0) as TextView;val unit=row.getChildAt(1) as TextView
                        assertEquals(BicycleDesign.dashboardUnitColor,unit.currentTextColor)
                        assertEquals((value.top+value.bottom)/2f,(unit.top+unit.bottom)/2f,1f)
                        assertTrue(value.textSize>unit.textSize)
                    }
                }
                views(root).filterIsInstance<EditText>().forEach { field ->
                    assertTrue(field.height>=BicycleDesign.touchSize);assertTrue(field.contentDescription.isNotEmpty())
                    assertTrue(views(root).filterIsInstance<TextView>().any { it.labelFor==field.id })
                }
                assertNotNull(root.findViewWithTag<View>("dialog_back"))
            }
            screen.dismiss()
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun allOptionalFieldsCanRemainEmptyAndNewLabelsAreResourceBacked() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val prefs=activity.getSharedPreferences("bike",0);val store=RiderProfileStore(prefs)
            val screen=showRiderProfile(activity,ThemeManager(prefs),DialogNavigation(),store);measure(screen)
            val resources=listOf(R.string.rider_profile,R.string.rider_personal,R.string.rider_training,R.string.rider_zones,R.string.rider_karvonen,
                R.string.rider_birth,R.string.rider_height,R.string.rider_weight,R.string.rider_resting,R.string.rider_maximum,R.string.rider_ftp,R.string.rider_hint_both)
            val visible=views(screen.window!!.decorView).filterIsInstance<TextView>().map { it.text.toString() }
            resources.forEach { assertTrue(activity.getString(it),activity.getString(it) in visible) }
            testBack(screen);assertEquals(RiderProfile(),store.read());assertFalse(screen.isShowing)
            assertFalse(prefs.all.keys.any { it.contains("zone",true) || it.contains("age",true) })
        } finally { controller.pause().stop().destroy() }
    }
}
