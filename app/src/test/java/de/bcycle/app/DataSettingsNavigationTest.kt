package de.bcycle.app

import android.app.AlertDialog
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Looper
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
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowContentResolver
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class DataSettingsNavigationTest {
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun result(activity:MainActivity,code:Int,status:Int,data:Intent?) {
        MainActivity::class.java.getDeclaredMethod("onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Intent::class.java)
            .apply { isAccessible=true }.invoke(activity,code,status,data)
    }
    private fun latest()=ShadowAlertDialog.getLatestAlertDialog()
    private fun click(screen:AlertDialog,tag:String)=screen.window!!.decorView.findViewWithTag<View>(tag).performClick()
    private fun open(activity:MainActivity):Pair<AlertDialog,AlertDialog> {
        views(activity.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
        val menu=latest();click(menu,"settings_DATA");return menu to latest()
    }
    private fun waitFor(test:()->Boolean) {
        val deadline=System.nanoTime()+10_000_000_000L
        while(!test()&&System.nanoTime()<deadline) { Thread.sleep(10);shadowOf(Looper.getMainLooper()).idle() }
        shadowOf(Looper.getMainLooper()).idle();assertTrue(test())
    }
    private fun measure(screen:AlertDialog,w:Int=360,h:Int=640) {
        shadowOf(Looper.getMainLooper()).idle();val root=screen.window!!.decorView
        root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h)
    }
    @Test fun seventhTileTwoSectionsAndMovedFormsPreserveConfigurationAndReturnToDataScreen() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val settings=RouteExportSettings(activity)
            settings.write(RouteExportConfig(save=false,tree="content://folder/tree/kept",server="https://cloud.example.test",user="bernd",folder="rides",wifiOnly=false))
            val before=settings.read();val (root,data)=open(activity);measure(data)
            val text=views(data.window!!.decorView).filterIsInstance<TextView>().map { it.text.toString() }
            assertTrue(text.indexOf(activity.getString(R.string.data_global))<text.indexOf(activity.getString(R.string.data_routes)))
            assertEquals(listOf("data_BACKUP","data_RESTORE","data_LOCAL","data_CLOUD"),views(data.window!!.decorView).mapNotNull { (it.tag as? String)?.takeIf { it in listOf("data_BACKUP","data_RESTORE","data_LOCAL","data_CLOUD") } })
            click(data,"data_LOCAL");val local=latest();measure(local)
            assertNotNull(local.window!!.decorView.findViewWithTag<View>("export_folder_choose"));assertNull(local.window!!.decorView.findViewWithTag<View>("export_server"))
            local.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(View.VISIBLE,data.window!!.decorView.visibility)
            click(data,"data_CLOUD");val cloud=latest();measure(cloud)
            assertNotNull(cloud.window!!.decorView.findViewWithTag<View>("export_server"));assertNull(cloud.window!!.decorView.findViewWithTag<View>("export_folder_choose"))
            assertEquals("https://cloud.example.test",cloud.window!!.decorView.findViewWithTag<EditText>("export_server").text.toString())
            assertNotNull(cloud.window!!.decorView.findViewWithTag<View>("export_password_visibility"));assertNotNull(cloud.window!!.decorView.findViewWithTag<View>("export_test"))
            cloud.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(before,settings.read())
            data.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(View.VISIBLE,root.window!!.decorView.visibility)
            click(root,"settings_ROUTES");val manager=latest();measure(manager)
            assertNull(manager.window!!.decorView.findViewWithTag<View>("route_settings"))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun backupSelectionAndSafCancelWriteNoSettingsAndConfirmationIsRequiredForRestore() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        val provider=Robolectric.buildContentProvider(SettingsProvider::class.java).create("settings-test").get()
        ShadowContentResolver.registerProviderInternal("settings-test",provider)
        try {
            val activity=controller.get();val (_,data)=open(activity);val prefs=activity.getSharedPreferences("bike",0)
            click(data,"data_BACKUP");val choice=latest();measure(choice)
            assertEquals(6,views(choice.window!!.decorView).filterIsInstance<CheckBox>().size)
            click(choice,"backup_none");assertFalse(choice.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
            click(choice,"backup_all");assertTrue(choice.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
            val original=prefs.all.toMap();choice.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(original,prefs.all)
            click(data,"data_BACKUP");val export=latest();export.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            val create=shadowOf(activity).nextStartedActivityForResult
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,create.intent.action);assertEquals(12,create.requestCode)
            assertTrue(create.intent.getStringExtra(Intent.EXTRA_TITLE)!!.startsWith("BICYCLE_Settings_"))
            result(activity,12,android.app.Activity.RESULT_CANCELED,null);assertEquals(original,prefs.all)
            export.dismiss();shadowOf(Looper.getMainLooper()).idle()
            click(data,"data_BACKUP");latest().getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(activity).nextStartedActivityForResult
            latest().dismiss();shadowOf(Looper.getMainLooper()).idle()
            val uri=Uri.parse("content://settings-test/document")
            result(activity,12,android.app.Activity.RESULT_OK,Intent().setData(uri))
            waitFor { provider.file.isFile && provider.file.length()>0 }
            val bytes=provider.file.readBytes();assertEquals(6,SettingsBackup(activity,RideRuntime.get(activity).themes).decode(bytes).sections.size)
            prefs.edit().putBoolean(ModeGlowSettings.key,true).commit()
            click(data,"data_RESTORE");val open=shadowOf(activity).nextStartedActivityForResult
            assertEquals(Intent.ACTION_OPEN_DOCUMENT,open.intent.action);assertEquals(13,open.requestCode)
            result(activity,13,android.app.Activity.RESULT_OK,Intent().setData(uri))
            waitFor { latest().isShowing && latest()!==data && latest().window!!.decorView.findViewWithTag<View>("backup_selection")!=null }
            val restore=latest();click(restore,"backup_none");assertFalse(restore.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
            (restore.window!!.decorView.findViewWithTag<View>("backup_designUi") as CheckBox).isChecked=true
            restore.getButton(AlertDialog.BUTTON_POSITIVE).performClick();val confirm=latest()
            assertTrue(prefs.getBoolean(ModeGlowSettings.key,false));confirm.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle()
            assertTrue(prefs.getBoolean(ModeGlowSettings.key,false))
            restore.getButton(AlertDialog.BUTTON_POSITIVE).performClick();latest().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            waitFor { !prefs.getBoolean(ModeGlowSettings.key,true) && !restore.isShowing }
            assertEquals(View.VISIBLE,data.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun pendingSafExportSurvivesActivityRecreationAndReturnsToNewDataScreen() {
        val provider=Robolectric.buildContentProvider(SettingsProvider::class.java).create("settings-test").get()
        ShadowContentResolver.registerProviderInternal("settings-test",provider)
        val old=Robolectric.buildActivity(MainActivity::class.java).setup()
        val state=android.os.Bundle()
        try {
            val activity=old.get();val (_,data)=open(activity)
            click(data,"data_BACKUP");val choice=latest();click(choice,"backup_none")
            (choice.window!!.decorView.findViewWithTag<View>("backup_dashboard") as CheckBox).isChecked=true
            choice.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,shadowOf(activity).nextStartedActivityForResult.intent.action)
            old.saveInstanceState(state)
        } finally { old.pause().stop().destroy() }
        assertNotNull(state.getByteArray("settingsBackupPendingExport"))
        val fresh=Robolectric.buildActivity(MainActivity::class.java).setup(state)
        try {
            val activity=fresh.get();result(activity,12,android.app.Activity.RESULT_OK,Intent().setData(Uri.parse("content://settings-test/document")))
            waitFor { provider.file.isFile && provider.file.length()>0 }
            val doc=SettingsBackup(activity,RideRuntime.get(activity).themes).decode(provider.file.readBytes())
            assertEquals(setOf(BackupSection.DASHBOARD),doc.sections.keys)
            waitFor { latest().isShowing && latest().window!!.decorView.findViewWithTag<View>("data_settings")!=null }
        } finally { fresh.pause().stop().destroy() }
    }
    @Test fun allThemesKeepStandardFullscreenHeaderFooterAndAccessibleRowsOnNarrowScreens() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val (_,data)=open(activity);val themes=RideRuntime.get(activity).themes
            for((w,h) in listOf(280 to 640,360 to 640,640 to 360)) {
                measure(data,w,h);val root=data.window!!.decorView.findViewWithTag<ViewGroup>("bicycle_fullscreen")
                assertEquals(BicycleDesign.headerHeight,root.getChildAt(0).height)
                assertEquals(h,root.findViewWithTag<View>("bicycle_fullscreen_footer").bottom)
                val geometry=views(root).map { listOf(it.left,it.top,it.width,it.height) }
                for(theme in BicycleThemes.presets) {
                    themes.activate(theme.id);measure(data,w,h);assertEquals(geometry,views(root).map { listOf(it.left,it.top,it.width,it.height) })
                    for(action in DataAction.entries) { val row=root.findViewWithTag<View>("data_${action.name}");assertTrue(row.isClickable);assertTrue(row.height>=BicycleDesign.touchSize);assertEquals(activity.getString(action.title),row.contentDescription) }
                }
            }
        } finally { controller.pause().stop().destroy() }
    }
    class SettingsProvider:android.content.ContentProvider() {
        val file get()=File(context!!.cacheDir,"settings-provider.json")
        override fun onCreate()=true
        override fun query(uri:Uri,projection:Array<out String>?,selection:String?,args:Array<out String>?,order:String?)=null
        override fun getType(uri:Uri)="application/json"
        override fun insert(uri:Uri,values:android.content.ContentValues?)=uri
        override fun delete(uri:Uri,selection:String?,args:Array<out String>?)=0
        override fun update(uri:Uri,values:android.content.ContentValues?,selection:String?,args:Array<out String>?)=0
        override fun openFile(uri:Uri,mode:String)=android.os.ParcelFileDescriptor.open(file,if(mode.contains('w')) android.os.ParcelFileDescriptor.MODE_READ_WRITE or android.os.ParcelFileDescriptor.MODE_CREATE or android.os.ParcelFileDescriptor.MODE_TRUNCATE else android.os.ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
