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
            assertEquals(listOf("data_BACKUP","data_RESTORE","data_IMPORT","data_LOCAL","data_CLOUD","data_INTERVALS","data_DREEVE","data_STRAVA"),views(data.window!!.decorView).mapNotNull { (it.tag as? String)?.takeIf { it in listOf("data_BACKUP","data_RESTORE","data_IMPORT","data_LOCAL","data_CLOUD","data_INTERVALS","data_DREEVE","data_STRAVA") } })
            click(data,"data_LOCAL");val local=latest();measure(local)
            assertSame(data,local)
            val localBody=local.window!!.decorView.findViewWithTag<View>("data_details_LOCAL")
            assertNotNull(localBody.findViewWithTag<View>("export_folder_choose"));assertNull(localBody.findViewWithTag<View>("export_server"))
            click(data,"data_LOCAL");shadowOf(Looper.getMainLooper()).idle();assertEquals(View.VISIBLE,data.window!!.decorView.visibility)
            click(data,"data_CLOUD");val cloud=data;measure(cloud)
            val cloudBody=cloud.window!!.decorView.findViewWithTag<View>("data_details_CLOUD")
            assertNotNull(cloudBody.findViewWithTag<View>("export_server"));assertNull(cloudBody.findViewWithTag<View>("export_folder_choose"))
            assertEquals("https://cloud.example.test",cloud.window!!.decorView.findViewWithTag<EditText>("export_server").text.toString())
            assertNotNull(cloud.window!!.decorView.findViewWithTag<View>("export_password_visibility"));assertNotNull(cloud.window!!.decorView.findViewWithTag<View>("export_test"))
            click(data,"data_CLOUD");shadowOf(Looper.getMainLooper()).idle();assertEquals(before,settings.read())
            testBack(data);shadowOf(Looper.getMainLooper()).idle();assertEquals(View.VISIBLE,root.window!!.decorView.visibility)
            click(root,"settings_ROUTES");val manager=latest();measure(manager)
            assertNull(manager.window!!.decorView.findViewWithTag<View>("route_settings"))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun routeImportAccordionUsesExistingDocumentPickersWithoutLeavingDataPage() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val (_,screen)=open(a)
            click(screen,"data_expand_IMPORT");measure(screen)
            assertEquals(View.VISIBLE,screen.window!!.decorView.findViewWithTag<View>("data_details_IMPORT").visibility)
            for((index,code) in listOf(8,9,15).withIndex()) {
                click(screen,"data_import_$index")
                val request=shadowOf(a).nextStartedActivityForResult
                assertEquals(code,request.requestCode)
                assertEquals(Intent.ACTION_OPEN_DOCUMENT,request.intent.action)
                val mime=request.intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!!.toList()
                assertTrue(if(index==2) "application/zip" in mime else "application/vnd.ant.fit" in mime)
                if(index==0) assertTrue("application/gpx+xml" in mime)
                assertSame(screen,latest());assertTrue(screen.isShowing)
            }
            click(screen,"data_expand_IMPORT")
            assertEquals(View.GONE,screen.window!!.decorView.findViewWithTag<View>("data_details_IMPORT").visibility)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun intervalsExpandsInlineWithoutEnablingUploadsOrOpeningAnotherDialog() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val (_,data)=open(activity)
            click(data,"data_INTERVALS");val intervals=latest();measure(intervals)
            assertNotNull(intervals.window!!.decorView.findViewWithTag<View>("intervals_athlete"))
            assertNotNull(intervals.window!!.decorView.findViewWithTag<View>("intervals_key"))
            assertNotNull(intervals.window!!.decorView.findViewWithTag<View>("intervals_test"))
            assertFalse(IntervalsSettings(activity).read().enabled)
            assertSame(data,intervals)
            click(data,"data_INTERVALS");shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.GONE,data.window!!.decorView.findViewWithTag<View>("data_details_INTERVALS").visibility)
            assertEquals(View.VISIBLE,data.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun dreeveAndStravaUseSeparateNativeFormsAndPreserveDraftsOnAppSwitch() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val (_,data)=open(activity)
            click(data,"data_DREEVE");val dreeve=latest();measure(dreeve)
            assertSame(data,dreeve)
            val address=dreeve.window!!.decorView.findViewWithTag<EditText>("service_address")
            address.setText("https://my.dreeve.example.org")
            waitFor { ActivityServiceSettings(activity,ActivityService.DREEVE).read().address==address.text.toString() }
            assertNotNull(dreeve.window!!.decorView.findViewWithTag<View>("service_secret"))
            controller.pause().stop();controller.start().resume()
            assertTrue(dreeve.isShowing);assertEquals("https://my.dreeve.example.org",address.text.toString())
            click(data,"data_DREEVE");shadowOf(Looper.getMainLooper()).idle()
            click(data,"data_STRAVA");val strava=latest();measure(strava)
            assertSame(data,strava)
            val clientId=strava.window!!.decorView.findViewWithTag<EditText>("service_client_id")
            clientId.setText("123456")
            waitFor { ActivityServiceSettings(activity,ActivityService.STRAVA).read().clientId=="123456" }
            controller.pause().stop();controller.start().resume()
            assertTrue(strava.isShowing);assertEquals("123456",clientId.text.toString())
            controller.newIntent(Intent(Intent.ACTION_VIEW,Uri.parse("bcycle://localhost/strava?state=invalid&code=untrusted&scope=activity:write")))
            assertFalse(ActivityServiceSettings(activity,ActivityService.STRAVA).connected())
            assertTrue(strava.isShowing)
            click(data,"data_STRAVA");shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE,data.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun serviceSwitchAndAccordionWorkIndependentlyAndKeepTheOriginalPersistence() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            a.getSharedPreferences("intervals-settings",0).edit().putBoolean("enabled",true).putString("athlete","42").commit()
            val (_,data)=open(a);measure(data)
            val root=data.window!!.decorView
            val header=root.findViewWithTag<ViewGroup>("data_INTERVALS")
            val toggle=root.findViewWithTag<Switch>("intervals_enabled")
            val body=root.findViewWithTag<View>("data_details_INTERVALS")
            assertSame(header,toggle.parent);assertTrue(toggle.isChecked);assertEquals(View.GONE,body.visibility)
            assertEquals(header.height,toggle.height)
            assertEquals(0,(toggle.layoutParams as ViewGroup.MarginLayoutParams).topMargin)
            root.findViewWithTag<View>("data_expand_INTERVALS").performClick()
            assertEquals(View.VISIBLE,body.visibility);assertTrue(toggle.isChecked)
            toggle.isChecked=false
            waitFor { !IntervalsSettings(a).read().enabled }
            assertEquals(View.VISIBLE,body.visibility)
            root.findViewWithTag<View>("data_expand_INTERVALS").performClick()
            assertEquals(View.GONE,body.visibility);assertFalse(toggle.isChecked)
            assertSame(data,latest());assertFalse(ActivityServiceSettings(a,ActivityService.DREEVE).read().enabled)
            assertFalse(ActivityServiceSettings(a,ActivityService.STRAVA).read().enabled)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun folderPickerReturnsToSameFormAndUpdatesItsSelectedFolder() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val (_,data)=open(activity)
            click(data,"data_LOCAL");val local=latest();measure(local)
            click(local,"export_folder_choose")
            assertEquals(10,shadowOf(activity).nextStartedActivityForResult.requestCode)
            controller.pause().stop();controller.start().resume()
            val uri=Uri.parse("content://bcycle.test.folder/tree/new-folder")
            result(activity,10,android.app.Activity.RESULT_OK,Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(local.isShowing)
            assertSame(local,latest())
            assertEquals(uri.toString(),RouteExportSettings(activity).read().tree)
            val content=views(local.window!!.decorView.findViewWithTag<View>("data_details_LOCAL")).filterIsInstance<RouteExportView>().single()
            assertEquals(uri.toString(),content.tree)
            assertTrue(content.folderButton.text.toString().contains("new-folder"))
            click(data,"data_LOCAL");shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE,data.window!!.decorView.visibility)
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
            click(choice,"backup_none");assertFalse(choice.window!!.decorView.findViewWithTag<Button>("backup_execute").isEnabled)
            click(choice,"backup_all");assertTrue(choice.window!!.decorView.findViewWithTag<Button>("backup_execute").isEnabled)
            val original=prefs.all.toMap();click(data,"data_BACKUP");assertEquals(original,prefs.all)
            click(data,"data_BACKUP");val export=latest();export.window!!.decorView.findViewWithTag<Button>("backup_execute").performClick()
            val create=shadowOf(activity).nextStartedActivityForResult
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,create.intent.action);assertEquals(12,create.requestCode)
            assertTrue(create.intent.getStringExtra(Intent.EXTRA_TITLE)!!.startsWith("BICYCLE_Settings_"))
            result(activity,12,android.app.Activity.RESULT_CANCELED,null);assertEquals(original,prefs.all)
            click(data,"backup_execute");shadowOf(activity).nextStartedActivityForResult
            shadowOf(Looper.getMainLooper()).idle()
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
            assertTrue(prefs.getBoolean(ModeGlowSettings.key,false));testBack(confirm);shadowOf(Looper.getMainLooper()).idle()
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
            choice.window!!.decorView.findViewWithTag<Button>("backup_execute").performClick()
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
            for((w,h) in listOf(280 to 640,360 to 640,480 to 800)) {
                measure(data,w,h);val root=data.window!!.decorView.findViewWithTag<ViewGroup>("bicycle_fullscreen")
                assertEquals(BicycleDesign.headerHeight,root.getChildAt(0).height)
                assertNull(root.findViewWithTag<View>("bicycle_fullscreen_footer"))
                assertEquals(h,root.getChildAt(root.childCount-1).bottom)
                val geometry=views(root).map { listOf(it.left,it.top,it.width,it.height) }
                for(theme in BicycleThemes.presets) {
                    themes.activate(theme.id);measure(data,w,h);assertEquals(geometry,views(root).map { listOf(it.left,it.top,it.width,it.height) })
                    for(action in DataAction.entries) { val row=root.findViewWithTag<View>("data_${action.name}");assertTrue(row.isClickable);assertTrue(row.height>=BicycleDesign.touchSize);assertEquals(activity.getString(action.title),row.contentDescription) }
                }
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun sharedInlineOwnerRunsEveryCleanupOnce() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val navigation=DialogNavigation();val calls=mutableListOf<Int>()
            val screen=BicycleDialogBuilder(a,ThemeManager(a.getSharedPreferences("cleanup",0)),navigation).setTitle("Dienste").create()
            navigation.onDismiss(screen) { calls+=1 };navigation.onDismiss(screen) { calls+=2 }
            screen.show();screen.dismiss();shadowOf(Looper.getMainLooper()).idle()
            assertEquals(listOf(1,2),calls)
            screen.dismiss();shadowOf(Looper.getMainLooper()).idle();assertEquals(listOf(1,2),calls)
        } finally { host.pause().stop().destroy() }
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
