package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.util.UUID
import java.util.concurrent.ExecutorService

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class OrphanProfilesTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val prefs get()=c.getSharedPreferences("bike",0)
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+8_000_000_000
        while(!condition() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5) }
        shadowOf(Looper.getMainLooper()).idle();assertTrue(condition())
    }
    private fun stop(state:MapState) { (MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as ExecutorService).shutdownNow() }
    private fun route(id:String,origin:TrackOrigin=TrackOrigin.RECORDED)=ManagedRoute(id,"Fahrt $id",origin==TrackOrigin.RECORDED,"FIT",RouteDetails(bikeType=BikeType.EBIKE),false,origin)
    private data class Setup(val ids:List<String>,val imported:String,val oldId:String,val target:BikeProfile)
    private fun setupLibrary():Setup {
        val store=BikeProfileStore(c,prefs);val old=store.current();store.save(old.copy(name="Mein E-Bike"))
        val oldId=BikeIdentities.id(prefs,old.slot)
        val library=GpxLibrary(c.filesDir);library.load()
        val ids=(0..2).map { i ->
            val id=UUID.randomUUID().toString()
            library.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(),"Fahrt $i",
                stableId=id,bikeType=BikeType.EBIKE,origin=if(i==2) TrackOrigin.IMPORTED else TrackOrigin.RECORDED)
            RideProfileColor.assign(c,id,old.slot);id
        }
        val indoor=store.add()!!;store.select(indoor.slot);store.save(store.current().copy(name="Indoor",type=BikeType.INDOOR))
        assertTrue(store.remove(old.slot))
        val target=store.add()!!;store.select(target.slot);store.save(store.current().copy(name="Mein E-Bike"))
        return Setup(ids.take(2),ids.last(),oldId,store.current())
    }
    @Test fun groupsUseOldIdentityExcludeImportsAndKeepUnknownOrAmbiguousSuggestionsSeparate() {
        val store=BikeProfileStore(c,prefs);val first=store.current();store.save(first.copy(name="Gleicher Name"))
        RideProfileColor.assign(c,"a",first.slot);RideProfileColor.assign(c,"imported",first.slot)
        val second=store.add()!!;store.select(second.slot);store.save(store.current().copy(name="Gleicher Name"))
        RideProfileColor.assign(c,"b",second.slot)
        val third=store.add()!!;store.select(third.slot);assertTrue(store.remove(first.slot));assertTrue(store.remove(second.slot))
        store.save(store.current().copy(name="Gleicher Name"))
        val entries=listOf(route("a"),route("b"),route("unassigned"),route("imported",TrackOrigin.IMPORTED),route("unknown",TrackOrigin.UNKNOWN))
        val groups=OrphanRideProfiles.find(c,entries)
        assertEquals(3,groups.size);assertEquals(setOf("a","b","unassigned"),groups.flatMap { it.rides }.map { it.id }.toSet())
        assertEquals(2,groups.filter { it.name=="Gleicher Name" }.size)
        val group=groups.first { it.rides.single().id=="a" }
        assertEquals(third.slot,OrphanRideProfiles.suggestion(group,store.all())!!.slot)
        assertNull(OrphanRideProfiles.suggestion(group,store.all()+store.current().copy(slot=999)))
        assertNull(OrphanRideProfiles.suggestion(groups.single { it.key=="unassigned" },store.all()))
        assertNull(RideProfileColor.slot(c,"a")) // Detection never writes.
    }
    @Test fun confirmedGroupUpdatesFilteringAndBackupWithoutChangingFilesImportsNamesOrLinks() {
        val setup=setupLibrary();val library=GpxLibrary(c.filesDir)
        val hashes=setup.ids.associateWith { WebDavClient.hash(library.file(it)) }
        val link=RideServiceLink(setup.ids.first(),RideService.DREEVE,"https://dreeve.example/","activity-test",hashes.getValue(setup.ids.first()))
        RideServiceLinks.confirm(c,link)
        val state=MapState(c,{})
        try {
            await { !state.busy };val names=state.entries.associate { it.id to it.name }
            val group=state.orphanProfiles().single();assertEquals(setup.ids.toSet(),group.rides.map { it.id }.toSet())
            var result:Result<Int>?=null
            val targetId=BikeIdentities.id(prefs,setup.target.slot)
            assertFalse(state.assignOrphanProfile(group,targetId,BikeType.EBIKE,{false}) { result=it });assertNull(result)
            assertTrue(state.assignOrphanProfile(group,targetId,BikeType.EBIKE,{true}) { result=it })
            await { result!=null && !state.busy };assertEquals(2,result!!.getOrThrow())
            assertTrue(state.orphanProfiles().isEmpty())
            setup.ids.forEach { id ->
                assertEquals(setup.target.slot,RideProfileColor.slot(c,id));assertEquals(hashes[id],WebDavClient.hash(library.file(id)))
                assertEquals(targetId,RideProfileColor.backup(c,id)!!.getString("bikeId"))
            }
            assertEquals(names,state.entries.associate { it.id to it.name });assertEquals(listOf(link),RideServiceLinks.links(c,setup.ids.first()))
            assertNull(RideProfileColor.slot(c,setup.imported))
            val query=RouteQuery().apply { bikes=setOf(setup.target.slot) }
            assertEquals(setup.ids.toSet(),query.apply(state.entries,{ RideProfileColor.slot(c,it) }).map { it.id }.toSet())
            result=null
            assertTrue(state.assignOrphanProfile(group,targetId,BikeType.EBIKE,{true}) { result=it })
            await { result!=null && !state.busy };assertTrue(result!!.isFailure)
        } finally { stop(state) }
    }
    @Test fun targetRemovedBeforeConfirmationCannotAttachTheGroupToAnotherProfile() {
        val setup=setupLibrary();val state=MapState(c,{})
        try {
            await { !state.busy };val group=state.orphanProfiles().single();val targetId=BikeIdentities.id(prefs,setup.target.slot)
            val store=BikeProfileStore(c,prefs);store.select(store.all().first { it.slot!=setup.target.slot }.slot);assertTrue(store.remove(setup.target.slot))
            var result:Result<Int>?=null
            assertTrue(state.assignOrphanProfile(group,targetId,BikeType.EBIKE,{true}) { result=it })
            await { result!=null && !state.busy };assertTrue(result!!.isFailure)
            setup.ids.forEach { assertNull(RideProfileColor.slot(c,it));assertEquals(setup.oldId,RideProfileColor.backup(c,it)!!.getString("bikeId")) }
        } finally { stop(state) }
    }
    @Test fun managerOffersOncePerOpeningAndProfileButtonUsesSameFormUntilConfirmed() {
        val setup=setupLibrary();val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a);await { !runtime.mapState.busy }
            MainActivity::class.java.getDeclaredMethod("routeSettings").apply { isAccessible=true }.invoke(a)
            val manager=ShadowAlertDialog.getLatestAlertDialog()
            await { ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("orphan_profiles")!=null }
            val offer=ShadowAlertDialog.getLatestAlertDialog();assertNotSame(manager,offer)
            assertNotNull(offer.window!!.decorView.findViewWithTag<View>("orphan_ride_${setup.ids.first()}"))
            offer.dismiss();shadowOf(Looper.getMainLooper()).idle();assertTrue(manager.isShowing);assertFalse(offer.isShowing)
            assertSame(manager,ShadowAlertDialog.getLatestAlertDialog()) // Showing the parent again must not re-offer.
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle()
            val profiles=ShadowAlertDialog.getLatestAlertDialog()
            val open=profiles.window!!.decorView.findViewWithTag<View>("profile_assignment_open")
            assertEquals("bicycle_action_footer",(open.parent as View).tag)
            val footer=open.parent as android.view.ViewGroup
            assertEquals("bike_delete",footer.getChildAt(footer.indexOfChild(open)+1).tag)
            assertEquals(View.VISIBLE,open.visibility);open.performClick();shadowOf(Looper.getMainLooper()).idle()
            val screen=ShadowAlertDialog.getLatestAlertDialog();val form=screen.window!!.decorView.findViewWithTag<View>("orphan_profiles")
            assertNotNull(form);assertNull(RideProfileColor.slot(c,setup.ids.first()))
            val picker=form.findViewWithTag<Spinner>("orphan_target_${setup.oldId}")
            picker.setSelection(1);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE,form.findViewWithTag<View>("orphan_type_warning_${setup.oldId}").visibility)
            picker.setSelection(2);shadowOf(Looper.getMainLooper()).idle()
            form.findViewWithTag<View>("orphan_apply_${setup.oldId}").performClick()
            await { RideProfileColor.slot(c,setup.ids.first())==setup.target.slot && !screen.isShowing }
            assertEquals(View.GONE,open.visibility)
        } finally { host.pause().stop().destroy() }
    }
}
