package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
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
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteDetailActionsTest {
    private fun views(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun measure(v: View) { v.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(2100, View.MeasureSpec.EXACTLY)); v.layout(0, 0, 1080, 2100) }
    private fun waitFor(condition: () -> Boolean) {
        val end = System.nanoTime() + 10_000_000_000
        while (!condition() && System.nanoTime() < end) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
        assertTrue(condition())
    }
    @Test fun fiveFixedFooterActionsRenameMapAndConfirmedDeletionReturnToSameManager() {
        val context = RuntimeEnvironment.getApplication(); val library = GpxLibrary(context.filesDir)
        library.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(), "First.gpx")
        val id = library.entries.single().id
        library.add("<gpx><rte><rtept lat='51' lon='8'/><rtept lat='51.01' lon='8.01'/></rte></gpx>".byteInputStream(), "Other.gpx")
        val provider = TestDownloads(java.io.File(context.cacheDir, "action-downloads"))
        provider.attachInfo(context, android.content.pm.ProviderInfo().apply { authority = "media" })
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("media", provider)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val state = RideRuntime.get(activity).mapState
            views(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            views(ShadowAlertDialog.getLatestAlertDialog().window!!.decorView).single { it.tag == "settings_ROUTES" }.performClick()
            val manager = ShadowAlertDialog.getLatestAlertDialog(); waitFor { !state.busy }; measure(manager.window!!.decorView)
            val list = manager.window!!.decorView.findViewWithTag<ListView>("route_list")
            val index = (0 until list.adapter.count).single { (list.adapter.getItem(it) as ManagedRoute).id == id }
            val card = list.adapter.getView(index, null, list)
            assertNull(card.findViewWithTag<View>("route_more_$id")); assertNotNull(card.findViewWithTag<View>("route_eye_$id"))
            assertNull(card.findViewWithTag<View>("route_cloud_$id")); assertNull(card.findViewWithTag<View>("route_local_$id"))
            card.performClick(); shadowOf(Looper.getMainLooper()).idle()
            val detail = ShadowAlertDialog.getLatestAlertDialog()
            var activeDetail = detail
            waitFor { detail.window!!.decorView.findViewWithTag<View>("detail_map").isEnabled }
            measure(detail.window!!.decorView)
            val footer = detail.window!!.decorView.findViewWithTag<ViewGroup>("bicycle_fullscreen_footer")
            var activeFooter = footer
            assertEquals(5, footer.childCount)
            assertEquals(listOf("detail_map", "detail_elevation", "detail_cloud", "detail_save", "detail_delete"), (0 until footer.childCount).map { footer.getChildAt(it).tag })
            assertTrue((0 until footer.childCount).all { footer.getChildAt(it) is BicycleFooterAction })
            assertEquals(listOf("Anzeigen", "Höhe korrigieren", "Cloudupload", "Speichern", "Löschen"),
                (0 until footer.childCount).map { (footer.getChildAt(it) as android.widget.LinearLayout).getChildAt(1) as TextView }.map { it.text.toString() })
            assertFalse(detail.window!!.decorView.findViewWithTag<View>("detail_elevation").isEnabled)
            assertNull(detail.window!!.decorView.findViewWithTag<View>("analysis_rename"))
            assertTrue(detail.window!!.decorView.findViewWithTag<View>("analysis_title").isClickable)
            detail.window!!.decorView.findViewWithTag<View>("settings_help").performClick()
            val help=ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(help.window!!.decorView.findViewWithTag<View>("help_body_${R.string.help_route_title_body}"))
            testBack(help);shadowOf(Looper.getMainLooper()).idle()
            assertTrue(detail.isShowing)
            assertFalse(detail.window!!.decorView.findViewWithTag<View>("detail_cloud").isEnabled)
            assertNull(detail.window!!.decorView.findViewWithTag<View>("analysis_actions"))
            assertFalse(views(detail.window!!.decorView).filterIsInstance<TextView>().any { it.isShown && it.text.toString() == activity.getString(R.string.analysis_close) })
            footer.getChildAt(0).performClick(); shadowOf(Looper.getMainLooper()).idle()
            val map = ShadowAlertDialog.getLatestAlertDialog(); measure(map.window!!.decorView)
            assertTrue(views(map.window!!.decorView).filterIsInstance<RouteAnalysisMap>().single().height > 600)
            testBack(map); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, detail.window!!.decorView.visibility)
            fun matches(button: BicycleFooterAction, kind: BicycleIcon): Boolean {
                fun pixels(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
                    val bitmap = android.graphics.Bitmap.createBitmap(72, 72, android.graphics.Bitmap.Config.ARGB_8888)
                    drawable.setBounds(0, 0, 72, 72); drawable.draw(android.graphics.Canvas(bitmap)); return bitmap
                }
                val actual = pixels(button.image.drawable)
                val expected = pixels(BicycleIcons.drawable(activity, kind, RideRuntime.get(activity).themes.active.tokens.foreground))
                val same = actual.sameAs(expected); actual.recycle(); expected.recycle(); return same
            }
            val saveButton = footer.getChildAt(3) as BicycleFooterAction
            assertTrue(matches(saveButton, BicycleIcon.Save))
            saveButton.performClick(); saveButton.performClick()
            waitFor { !state.busy && RouteLocalSaves.completed(activity, id, state.entries.single { it.id == id }.fileStamp) }
            assertEquals(1, provider.writes)
            assertTrue(matches(saveButton, BicycleIcon.SaveConfirmed))
            assertTrue(matches(footer.getChildAt(2) as BicycleFooterAction, BicycleIcon.CloudNotUploaded))
            // Returning and reopening must show the durable save receipt, not reset it.
            testBack(detail)
            shadowOf(Looper.getMainLooper()).idle()
            card.performClick(); shadowOf(Looper.getMainLooper()).idle()
            val reopened = ShadowAlertDialog.getLatestAlertDialog()
            waitFor { reopened.window!!.decorView.findViewWithTag<View>("detail_map").isEnabled }
            val restored = reopened.window!!.decorView.findViewWithTag<BicycleFooterAction>("detail_save")
            assertTrue(matches(restored, BicycleIcon.SaveConfirmed))
            testBack(reopened)
            shadowOf(Looper.getMainLooper()).idle()
            card.performClick(); shadowOf(Looper.getMainLooper()).idle()
            activeDetail = ShadowAlertDialog.getLatestAlertDialog()
            waitFor { activeDetail.window!!.decorView.findViewWithTag<View>("detail_map").isEnabled }
            activeFooter = activeDetail.window!!.decorView.findViewWithTag<ViewGroup>("bicycle_fullscreen_footer")
            activeDetail.window!!.decorView.findViewWithTag<View>("analysis_title").performClick(); shadowOf(Looper.getMainLooper()).idle()
            val rename = ShadowAlertDialog.getLatestAlertDialog(); val input = rename.window!!.decorView.findViewWithTag<EditText>("route_rename")
            assertEquals("First", input.text.toString()); input.setText("Renamed")
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(250)); waitFor { !state.busy && state.entries.first { it.id == id }.name == "Renamed" }
            testBack(rename)
            assertEquals("Renamed", activeDetail.window!!.decorView.findViewWithTag<TextView>("analysis_title").text.toString())
            activeFooter.getChildAt(4).performClick(); shadowOf(Looper.getMainLooper()).idle()
            val cancel = ShadowAlertDialog.getLatestAlertDialog(); testBack(cancel); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, activeDetail.window!!.decorView.visibility); assertTrue(state.entries.any { it.id == id })
            activeFooter.getChildAt(4).performClick(); shadowOf(Looper.getMainLooper()).idle()
            val delete = ShadowAlertDialog.getLatestAlertDialog(); delete.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            waitFor { !state.busy && state.entries.none { it.id == id } && manager.window!!.decorView.visibility == View.VISIBLE }
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            assertEquals(1, state.entries.size); assertEquals("Other", state.entries.single().name)
            assertEquals(1, list.adapter.count)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun cloudReceiptUpdatesFooterAfterSuccessAndAfterReopeningAndStaysIndependentOfSave() {
        val context = RuntimeEnvironment.getApplication()
        RouteExportSettings(context).write(RouteExportConfig(save = false, webdav = true, server = "https://cloud.example.org", user = "rider", wifiOnly = false))
        val journal = java.io.File(context.cacheDir, "detail-cloud.ride")
        val start = 1_791_626_400_000L
        RideJournal(journal, start, RecordingConfig(setOf("HEART_RATE"))).use { writer ->
            writer.append(RideSample(start, 0, values = mapOf("HEART_RATE" to 120.0)))
            writer.append(RideSample(start + 1000, 1000, values = mapOf("HEART_RATE" to 121.0)))
        }
        val encoded = java.io.File(context.cacheDir, "detail-cloud.fit"); FitWriter.encode(journal, encoded)
        val lib = FitLibrary(context.filesDir)
        val entry = encoded.inputStream().use { lib.add(it, "Cloud ride.fit", true) }!!
        val original = lib.file(entry.id)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val state = RideRuntime.get(activity).mapState
            waitFor { !state.busy }
            fun open(): AlertDialog {
                val route = state.entries.single { it.id == entry.id }
                MainActivity::class.java.getDeclaredMethod("routeAnalysis", ManagedRoute::class.java).apply { isAccessible = true }.invoke(activity, route)
                shadowOf(Looper.getMainLooper()).idle()
                return ShadowAlertDialog.getLatestAlertDialog().also { screen ->
                    waitFor { screen.window!!.decorView.findViewWithTag<View>("analysis_date") != null }
                }
            }
            fun refresh() { MainActivity::class.java.getDeclaredMethod("refreshRouteRows").apply { isAccessible = true }.invoke(activity) }
            val first = open()
            val cloud = first.window!!.decorView.findViewWithTag<BicycleFooterAction>("detail_cloud")
            assertEquals(activity.getString(R.string.route_action_cloud), cloud.contentDescription)
            cloud.performClick(); cloud.performClick(); waitFor { !state.busy }
            assertNotEquals(UploadState.UPLOADED, RouteUploads.state(activity, entry.id).state)
            assertEquals(activity.getString(R.string.route_action_cloud), cloud.contentDescription)
            var transfers = 0
            RouteUploads.run(activity, { false }) { _, file, _, _ -> assertEquals(original, file); transfers++ }
            assertEquals(1, transfers); refresh()
            assertEquals(activity.getString(R.string.route_cloud_confirmed), cloud.contentDescription)
            assertEquals(activity.getString(R.string.route_action_save), first.window!!.decorView.findViewWithTag<View>("detail_save").contentDescription)
            testBack(first); shadowOf(Looper.getMainLooper()).idle()
            val reopened = open(); refresh()
            assertEquals(activity.getString(R.string.route_cloud_confirmed), reopened.window!!.decorView.findViewWithTag<View>("detail_cloud").contentDescription)
            assertEquals(activity.getString(R.string.route_action_save), reopened.window!!.decorView.findViewWithTag<View>("detail_save").contentDescription)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun closingChildAndParentBeforeTheirCallbacksDoesNotResurrectTheDeletedDetails() {
        val controller = Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val activity = controller.get(); val themes = ThemeManager(activity.getSharedPreferences("bike", 0)); val navigation = DialogNavigation()
            val manager = BicycleDialogBuilder(activity, themes, navigation).setTitle("Manager").create().apply { show() }
            shadowOf(Looper.getMainLooper()).idle()
            val parent = BicycleDialogBuilder(activity, themes, navigation).setTitle("Details").create().apply { show() }
            shadowOf(Looper.getMainLooper()).idle()
            val child = BicycleDialogBuilder(activity, themes, navigation, false).setTitle("Confirmation").create().apply { show() }
            shadowOf(Looper.getMainLooper()).idle()
            child.dismiss(); parent.dismiss()
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(child.isShowing); assertFalse(parent.isShowing)
            assertTrue(manager.isShowing); assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            manager.dismiss(); shadowOf(Looper.getMainLooper()).idle(); assertTrue(navigation.empty)
        } finally { controller.pause().stop().destroy() }
    }
    private class TestDownloads(private val root: java.io.File) : android.content.ContentProvider() {
        var writes = 0
        private lateinit var file: java.io.File
        override fun onCreate() = true
        override fun query(uri: android.net.Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?): android.database.Cursor? = null
        override fun getType(uri: android.net.Uri) = "application/gpx+xml"
        override fun insert(uri: android.net.Uri, values: android.content.ContentValues?): android.net.Uri {
            root.mkdirs(); writes++; file = java.io.File(root, "export-$writes.gpx")
            return android.net.Uri.parse("content://media/downloads/$writes")
        }
        override fun openFile(uri: android.net.Uri, mode: String) = android.os.ParcelFileDescriptor.open(file,
            android.os.ParcelFileDescriptor.MODE_CREATE or android.os.ParcelFileDescriptor.MODE_TRUNCATE or android.os.ParcelFileDescriptor.MODE_WRITE_ONLY)
        override fun update(uri: android.net.Uri, values: android.content.ContentValues?, selection: String?, args: Array<out String>?) = 1
        override fun delete(uri: android.net.Uri, selection: String?, args: Array<out String>?) = 1
    }
}
