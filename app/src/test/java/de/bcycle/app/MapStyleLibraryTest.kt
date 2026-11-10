package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class MapStyleLibraryTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val prefs get()=c.getSharedPreferences("bike",0)
    private val custom=MapStyleOverrides().update(MapStyleTarget.PRIMARY,MapObjectStyle(Color.RED,alpha=123,widthPercent=70,outline=MapOutlineStyle(Color.BLUE,88,150)))
        .copy(placeLabels=mapOf(MapPlaceClass.CITY to MapPlaceTextStyle(900,80)))
    private fun pressSave(dialog:AlertDialog) {
        val decor=dialog.window!!.decorView
        decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY))
        decor.layout(0,0,360,640)
        val button=decor.findViewWithTag<View>("map_style_save_confirm")
        assertTrue("Save must be visible to the user",button.isShown)
        val rect=android.graphics.Rect();assertTrue(button.getGlobalVisibleRect(rect));assertFalse(rect.isEmpty)
        assertTrue(button.isEnabled);button.performClick()
    }
    @Test fun builtinBrightIsOfferedAndCanBeSavedOverwrittenAndLoadedWithoutDuplicateRows() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(prefs)
            fun open():AlertDialog {
                MapStyleSaveDialog.show(a,prefs,themes) { true };shadowOf(Looper.getMainLooper()).idle()
                return ShadowAlertDialog.getLatestAlertDialog()
            }
            custom.write(prefs)
            var dialog=open();val choice=dialog.window!!.decorView.findViewWithTag<Spinner>("map_style_save_choice")
            assertEquals("Bright",choice.adapter.getItem(1));assertEquals(1,choice.selectedItemPosition)
            assertEquals("Bright",dialog.window!!.decorView.findViewWithTag<EditText>("map_style_save_name").text.toString())
            pressSave(dialog);shadowOf(Looper.getMainLooper()).idle()
            val first=MapStyleLibrary.read(prefs).single();assertEquals("Bright",first.name)
            MapStyleOverrides().write(prefs);dialog=open();pressSave(dialog);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(first.id,MapStyleLibrary.read(prefs).single().id)
            val picker=MapSourcePicker(a,themes,prefs);picker.select(MapSource.VECTOR)
            assertEquals(8,picker.adapter.count);assertEquals(7,picker.selectedItemPosition);assertEquals(first.id,picker.selectedStyleId)
            custom.write(prefs);MapStyleLibrary.load(prefs,picker.selectedStyleId)
            assertEquals(MapStyleOverrides(),MapStyleOverrides.read(prefs))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun snapshotsOverwriteByIdentityAndNameRemainIndependentOfDraftAndSurviveProfilesAndBackup() {
        custom.write(prefs)
        prefs.edit().putBoolean(MapVectorLayer.CONTOURS.key,true).putBoolean(MapVectorLayer.POIS.key,true).apply()
        MapRouteStyles.write(prefs,MapLineKind.SAVED,MapLineStyle(Color.GREEN,3f,60))
        MapRouteStyles.write(prefs,MapLineKind.CYCLEWAYS,MapLineStyle(Color.CYAN,4f,55))
        val saved=MapStyleLibrary.save(prefs,"Sommer",null)
        MapStyleOverrides().write(prefs)
        assertEquals(custom,MapStyleOverrides.parse(JSONObject(MapStyleLibrary.read(prefs).single().style)))
        val bank=BikeUiConfigurationStore(prefs);bank.ensure(listOf(0,1));bank.save(0);bank.apply(1)
        assertEquals(saved.id,MapStyleLibrary.read(prefs).single().id)
        MapStyleLibrary.load(prefs,saved.id)
        assertEquals(custom,MapStyleOverrides.read(prefs))
        assertTrue(prefs.getBoolean(MapVectorLayer.CONTOURS.key,false));assertTrue(prefs.getBoolean(MapVectorLayer.POIS.key,false))
        assertEquals(MapLineStyle(Color.CYAN,4f,55),MapRouteStyles.read(prefs,MapLineKind.CYCLEWAYS))
        assertEquals(MapLineStyle(Color.GREEN,3f,60),MapRouteStyles.read(prefs,MapLineKind.SAVED))
        val edited=custom.update(MapStyleTarget.FOREST,MapObjectStyle(Color.GREEN));edited.write(prefs)
        val overwritten=MapStyleLibrary.save(prefs,"SOMMER",null)
        assertEquals(saved.id,overwritten.id);assertEquals(1,MapStyleLibrary.read(prefs).size)
        val second=MapStyleLibrary.save(prefs,"Winter",null);assertNotEquals(saved.id,second.id)
        val backup=SettingsBackup(prefs,c.getSharedPreferences("route-export",0),ThemeManager(prefs))
        val bytes=backup.export(setOf(BackupSection.MAPS))
        prefs.edit().remove(MapStyleLibrary.KEY).remove(MapStyleLibrary.ACTIVE).apply()
        backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.MAPS)))
        assertEquals(2,MapStyleLibrary.read(prefs).size);assertEquals(second.id,prefs.getString(MapStyleLibrary.ACTIVE,null))
        MapStyleLibrary.load(prefs,saved.id);assertEquals(edited,MapStyleOverrides.read(prefs))
        bank.save(1);bank.apply(0);assertEquals(saved.id,prefs.getString(MapStyleLibrary.ACTIVE,null))
        MapStyleLibrary.load(prefs,null);assertEquals(MapStyleOverrides(),MapStyleOverrides.read(prefs));assertFalse(prefs.contains(MapStyleLibrary.ACTIVE))
        assertEquals(2,MapStyleLibrary.read(prefs).size)
        assertThrows(IllegalArgumentException::class.java) { MapStyleLibrary.save(prefs," ",null) }
        val malformed=MapStyleLibrary.json(MapStyleLibrary.read(prefs));malformed.getJSONObject(0).getJSONObject("style").put("base","other")
        assertThrows(IllegalArgumentException::class.java) { MapStyleLibrary.parse(malformed.toString()) }
        val duplicate=MapStyleLibrary.json(MapStyleLibrary.read(prefs));duplicate.put(duplicate.getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) { MapStyleLibrary.parse(duplicate.toString()) }
    }
    @Test fun fullSettingsBackupKeepsSharedLibraryAndEachBicyclesSelectedStyle() {
        val bikes=BikeProfileStore(c,prefs);val first=bikes.activeSlot;val second=bikes.add()!!.slot
        custom.write(prefs);val summer=MapStyleLibrary.save(prefs,"Sommer",null)
        bikes.select(second);MapStyleOverrides().write(prefs);val winter=MapStyleLibrary.save(prefs,"Winter",null)
        val backup=SettingsBackup(c,ThemeManager(prefs));val selected=setOf(BackupSection.MAPS,BackupSection.PROFILE)
        val bytes=backup.export(selected)
        prefs.edit().remove(MapStyleLibrary.KEY).apply();MapStyleLibrary.load(prefs,null)
        backup.apply(backup.plan(backup.decode(bytes),selected))
        assertEquals(2,MapStyleLibrary.read(prefs).size)
        assertEquals(winter.id,prefs.getString(MapStyleLibrary.ACTIVE,null))
        val restored=BikeProfileStore(c,prefs);restored.select(first)
        assertEquals(summer.id,prefs.getString(MapStyleLibrary.ACTIVE,null));assertEquals(custom,MapStyleOverrides.read(prefs))
        restored.select(second);assertEquals(winter.id,prefs.getString(MapStyleLibrary.ACTIVE,null))
        assertEquals(MapStyleOverrides(),MapStyleOverrides.read(prefs))
    }
    @Test fun editorSavesNewAndExistingStylesWithProfileGuardAndKeepsJsonExport() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        val a=host.get();val themes=ThemeManager(prefs)
        try {
            custom.write(prefs)
            var exported:String?=null
            val editor=MapStyleEditorView(a,prefs,themes,{}, { exported=it });a.setContentView(editor)
            editor.findViewWithTag<View>("map_style_save").performClick();shadowOf(Looper.getMainLooper()).idle()
            var dialog=ShadowAlertDialog.getLatestAlertDialog()
            var name=dialog.window!!.decorView.findViewWithTag<EditText>("map_style_save_name")
            name.setText("Meine Karte");pressSave(dialog);shadowOf(Looper.getMainLooper()).idle()
            val saved=MapStyleLibrary.read(prefs).single()
            MapStyleOverrides().write(prefs)
            editor.findViewWithTag<View>("map_style_save").performClick();shadowOf(Looper.getMainLooper()).idle()
            dialog=ShadowAlertDialog.getLatestAlertDialog();name=dialog.window!!.decorView.findViewWithTag("map_style_save_name")
            assertEquals("Meine Karte",name.text.toString())
            pressSave(dialog);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(saved.id,MapStyleLibrary.read(prefs).single().id)
            assertEquals(MapStyleOverrides(),MapStyleOverrides.parse(JSONObject(MapStyleLibrary.read(prefs).single().style)))
            editor.findViewWithTag<View>("map_style_export").performClick();assertNotNull(exported)
            editor.findViewWithTag<View>("map_style_save").performClick();shadowOf(Looper.getMainLooper()).idle()
            dialog=ShadowAlertDialog.getLatestAlertDialog();prefs.edit().putInt(BikeProfileStore.ACTIVE,1).apply()
            dialog.window!!.decorView.findViewWithTag<EditText>("map_style_save_name").setText("Falsches Profil")
            pressSave(dialog);assertEquals(1,MapStyleLibrary.read(prefs).size)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun actualSettingsPickerLoadsSavedStylesAndDoesNotOverwriteDraftOnReopening() {
        custom.write(prefs);prefs.edit().putBoolean(MapVectorLayer.CONTOURS.key,true).apply()
        val saved=MapStyleLibrary.save(prefs,"Meine Karte",null)
        MapStyleLibrary.load(prefs,null)
        val host=Robolectric.buildActivity(MainActivity::class.java).setup();val a=host.get()
        fun settings():AlertDialog {
            MainActivity::class.java.getDeclaredMethod("mapSettings").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle();return ShadowAlertDialog.getLatestAlertDialog()
        }
        try {
            var dialog=settings()
            var source=testDescendants(dialog.window!!.decorView).filterIsInstance<MapSourcePicker>().single()
            assertEquals("Meine Karte",source.adapter.getItem(8))
            source.setSelection(8);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(custom,MapStyleOverrides.read(prefs));assertEquals(saved.id,prefs.getString(MapStyleLibrary.ACTIVE,null))
            assertTrue(dialog.window!!.decorView.findViewWithTag<android.widget.Switch>(MapVectorLayer.CONTOURS.key).isChecked)
            val draft=custom.update(MapStyleTarget.FOREST,MapObjectStyle(Color.MAGENTA));draft.write(prefs)
            dialog.dismiss();shadowOf(Looper.getMainLooper()).idle();dialog=settings()
            source=testDescendants(dialog.window!!.decorView).filterIsInstance<MapSourcePicker>().single()
            assertEquals(8,source.selectedItemPosition);assertEquals(draft,MapStyleOverrides.read(prefs))
            source.setSelection(7);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(MapStyleOverrides(),MapStyleOverrides.read(prefs))
            source.setSelection(8);shadowOf(Looper.getMainLooper()).idle();assertEquals(custom,MapStyleOverrides.read(prefs))
            dialog.dismiss();shadowOf(Looper.getMainLooper()).idle()
            prefs.edit().remove(MapStyleLibrary.KEY).apply()
            dialog=settings();assertEquals(custom,MapStyleOverrides.read(prefs))
            dialog.dismiss()
        } finally { host.pause().stop().destroy() }
    }
}
