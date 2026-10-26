package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.widget.TextView
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="de-rDE-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DreeveMaintenanceTest {
    private fun fixture()=javaClass.getResource("/dreeve-maintenance.json")!!.readText()
    @Test fun documentedSnapshotIsReadOnlyAuthenticatedAndKeepsServerDueStatusAndSharedGear() {
        MockWebServer().use { server ->
            server.start();server.enqueue(MockResponse().setBody(fixture()))
            val tasks=DreeveClient(ActivityServiceConfig(),"private-test-key",base=server.url("/sub/")).maintenance()
            val request=server.takeRequest()
            assertEquals("GET",request.method);assertEquals("/sub/api/v1/gear/maintenance-tasks",request.path)
            assertEquals("Bearer private-test-key",request.getHeader("Authorization"));assertEquals(0,request.bodySize)
            assertEquals(6,tasks.size);assertTrue(tasks.first().overdue);assertEquals(-370.4,tasks.first().remaining!!,0.0)
            assertNull(tasks.first().lastPerformed)
            val shared=tasks[2];assertTrue(shared.due);assertFalse(shared.overdue)
            assertEquals(listOf("gear-bike-a","gear-bike-b"),shared.gearIds)
            assertEquals("gearComponent-chain",tasks.first().componentId)
            assertEquals("days",tasks[3].unit);assertTrue(tasks.last().gearIds.isEmpty())
        }
    }
    @Test fun emptyDisabledOlderApiAuthMalformedAndCancellationRemainDistinct() {
        MockWebServer().use { server ->
            server.start();val client=DreeveClient(ActivityServiceConfig(),"test",base=server.url("/"))
            server.enqueue(MockResponse().setBody("{\"maintenanceTasks\":[],\"pagination\":{\"total\":0}}"))
            assertTrue(client.maintenance().isEmpty())
            server.enqueue(MockResponse().setResponseCode(404))
            assertEquals(R.string.maintenance_api_unavailable,assertThrows(ActivityServiceFailure::class.java) { client.maintenance() }.problem)
            server.enqueue(MockResponse().setResponseCode(401))
            assertEquals(R.string.service_auth_failed,assertThrows(ActivityServiceFailure::class.java) { client.maintenance() }.problem)
            server.enqueue(MockResponse().setBody(JSONObject(fixture()).apply { getJSONArray("maintenanceTasks").getJSONObject(0).getJSONObject("interval").put("value",-1) }.toString()))
            assertEquals(R.string.service_response_failed,assertThrows(ActivityServiceFailure::class.java) { client.maintenance() }.problem)
            assertThrows(IOException::class.java) { DreeveClient(ActivityServiceConfig(),"test",{true},server.url("/")).maintenance() }
            assertEquals(4,server.requestCount)
        }
    }
    @Test fun viewShowsUpcomingOverdueSharedBikesAndRetryWithoutInventedLocalAssignment() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));var reload=0
            val view=DreeveMaintenanceView(a,themes) { reload++ };a.setContentView(view)
            view.loading();assertFalse(view.findViewWithTag<View>("maintenance_refresh").isEnabled)
            view.loaded(DreeveMaintenanceTask.decode(JSONObject(fixture())),mapOf("gear-bike-a" to "E-Bike","gear-bike-b" to "Indoor"))
            val text=testDescendants(view).filterIsInstance<TextView>().joinToString("\n") { it.text }
            assertTrue(text.contains("E-Bike"));assertTrue(text.contains("Indoor"));assertTrue(text.contains("9,6 km verbleibend"));assertTrue(text.contains("370,4 km überfällig"))
            assertTrue(testDescendants(view).any { it.stateDescription==a.getString(R.string.maintenance_overdue) })
            assertEquals(3,testDescendants(view).filterIsInstance<BicycleSectionHeader>().size)
            val actions=testDescendants(view).filter { it.tag?.toString()?.startsWith("maintenance_action_")==true }
            assertEquals(7,actions.size);assertTrue(actions.all { !it.isEnabled })
            val toggle=view.findViewWithTag<View>("maintenance_component_gear-bike-a_id:gearComponent-chain")
            toggle.performClick()
            view.loading();view.loaded(DreeveMaintenanceTask.decode(JSONObject(fixture())),mapOf("gear-bike-a" to "E-Bike","gear-bike-b" to "Indoor"))
            assertEquals(View.GONE,view.findViewWithTag<View>("maintenance_component_body_gear-bike-a_id:gearComponent-chain").visibility)
            view.loading();view.failed(R.string.service_unreachable)
            assertEquals(a.getString(R.string.service_unreachable),view.findViewWithTag<TextView>("maintenance_status").text.toString())
            view.findViewWithTag<View>("maintenance_refresh").performClick();assertEquals(1,reload)
            view.loaded(emptyList(),emptyMap());assertEquals(a.getString(R.string.maintenance_empty),view.findViewWithTag<TextView>("maintenance_status").text.toString())
        } finally { host.pause().stop().destroy() }
    }
    @Test fun realSettingsEntryShowsMissingConfigurationAndClosesCleanly() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            MainActivity::class.java.getDeclaredMethod("showMenu",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,false)
            val menu=ShadowAlertDialog.getLatestAlertDialog()
            menu.window!!.decorView.findViewWithTag<View>("settings_MAINTENANCE").performClick()
            val screen=ShadowAlertDialog.getLatestAlertDialog()
            val status=screen.window!!.decorView.findViewWithTag<TextView>("maintenance_status")
            val deadline=System.nanoTime()+5_000_000_000
            while(status.text.toString()!=a.getString(R.string.dreeve_import_config_missing) && System.nanoTime()<deadline) {
                shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5)
            }
            assertEquals(a.getString(R.string.dreeve_import_config_missing),status.text.toString())
            assertNotNull(screen.window!!.decorView.findViewWithTag<View>("settings_help"))
            screen.dismiss();shadowOf(Looper.getMainLooper()).idle();assertTrue(menu.isShowing)
        } finally { host.pause().stop().destroy() }
    }
}
