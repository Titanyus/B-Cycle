package de.bcycle.app

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="de-rDE-w360dp-h800dp-mdpi")
class RouteCollectionsTest {
    private val now=Instant.parse("2026-11-01T12:00:00Z").toEpochMilli()
    private fun route(id:String,kind:RouteKind=RouteKind.RECORDED,speed:Double=20.0,date:Long=now,virtual:Boolean=false,distance:Double=40000.0)=
        ManagedRoute(id,id,kind==RouteKind.RECORDED,"GPX",RouteDetails(date,distance,movingMs=(distance*3600/speed).toLong(),virtualRide=virtual),true,
            if(kind==RouteKind.RECORDED) TrackOrigin.RECORDED else TrackOrigin.IMPORTED,locallySaved=true)
    private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun layout(v:View,width:Int=360) { v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));v.layout(0,0,width,800) }
    @Test fun lockedCollectionsSurviveResetAndRememberIndependentFiltersAndSorts() {
        val context=RuntimeEnvironment.getApplication();val preferences=context.getSharedPreferences("queries",0)
        val entries=listOf(route("ride"),route("plan",RouteKind.IMPORTED))
        val rides=RouteQuery(RouteKind.RECORDED);val plans=RouteQuery(RouteKind.IMPORTED)
        rides.kinds=setOf(RouteKind.IMPORTED)
        assertEquals(listOf("ride"),rides.apply(entries,{null}).map { it.id })
        rides.clearFilters();assertEquals(0,rides.activeCount)
        assertEquals(listOf("ride"),rides.apply(entries,{null}).map { it.id })
        rides.distance=RouteRange(5.0,50.0);rides.sort=RouteSort.DISTANCE;rides.descending=false;rides.save(preferences)
        plans.displayedOnly=true;plans.sort=RouteSort.MOVING;plans.save(preferences)
        val a=RouteQuery(RouteKind.RECORDED).apply { restore(preferences) }
        val b=RouteQuery(RouteKind.IMPORTED).apply { restore(preferences) }
        assertEquals(RouteRange(5.0,50.0),a.distance);assertFalse(a.descending);assertEquals(RouteSort.DISTANCE,a.sort)
        assertTrue(b.displayedOnly);assertEquals(RouteSort.MOVING,b.sort);assertFalse(b.distance.active)
        assertEquals(listOf("plan"),b.apply(entries,{null},displayedIds=setOf("ride","plan")).map { it.id })
        b.clearFilters();assertEquals(listOf("plan"),b.apply(entries,{null}).map { it.id })
        b.estimateKmh=20.0;assertEquals(7200000.0,b.value(RouteSort.MOVING,entries.last())!!,.01)
        val settings=context.getSharedPreferences("bike",0)
        a.save(settings);b.save(settings)
        val backup=SettingsBackupSupplement(context)
        val saved=backup.export(BackupSection.MAPS)
        val restored=backup.plan(BackupSection.MAPS,saved).getValue("bike")
        assertEquals(settings.getString("routeQuery.RECORDED.v1",null),restored["routeQuery.RECORDED.v1"])
        assertEquals(settings.getString("routeQuery.IMPORTED.v1",null),restored["routeQuery.IMPORTED.v1"])
    }
    @Test fun paceUsesArithmeticRideAveragesOver28DaysExcludesVirtualAndIsStoredBetweenChanges() {
        val preferences=RuntimeEnvironment.getApplication().getSharedPreferences("pace",0)
        val pace=RecentRidePace(preferences)
        assertNull(pace.value)
        val rides=listOf(route("short",speed=10.0,distance=1000.0),route("long",speed=30.0,distance=90000.0),
            route("virtual",speed=100.0,virtual=true),route("plan",RouteKind.IMPORTED,speed=90.0),
            route("old",speed=80.0,date=now-29L*86400000),route("future",date=now+86400000),
            route("indoor",speed=100.0).let { it.copy(details=it.details.copy(bikeType=BikeType.INDOOR)) })
        assertEquals(20.0,pace.update(rides,now)!!,.0001)
        assertEquals(20.0,RecentRidePace(preferences).value!!,.0001)
        // No periodic or per-render re-evaluation: retain the saved value until ride inputs change.
        assertEquals(20.0,pace.update(rides,now+60L*86400000)!!,.0001)
        assertEquals(40.0,pace.update(rides+route("new",speed=40.0,date=now+60L*86400000),now+60L*86400000)!!,.0001)
        assertEquals(7200000L,RecentRidePace.duration(40000.0,20.0));assertNull(RecentRidePace.duration(40000.0,null))
    }
    @Test fun gpxDateComesFromMetadataThenTrackTimeOtherwiseAddedDateAndSurvivesReload() {
        val root=java.nio.file.Files.createTempDirectory("template-dates").toFile()
        try {
            val library=GpxLibrary(root).apply { load() }
            val geometry="<trk><trkseg><trkpt lat='50' lon='8'><time>2026-10-20T10:00:00Z</time></trkpt><trkpt lat='50.01' lon='8.01'/></trkseg></trk>"
            library.add("<gpx><metadata><time>2026-10-15T09:00:00Z</time></metadata>$geometry</gpx>".byteInputStream(),"Dated",select=false)
            library.add("<gpx>$geometry</gpx>".byteInputStream(),"Track date",select=false)
            val before=System.currentTimeMillis()
            library.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(),"Undated",select=false)
            val dated=library.entries.first();val undated=library.entries.last()
            assertEquals(Instant.parse("2026-10-15T09:00:00Z").toEpochMilli(),dated.details.dateMs);assertTrue(dated.details.dateFromFile)
            assertEquals(Instant.parse("2026-10-20T10:00:00Z").toEpochMilli(),library.entries[1].details.dateMs)
            assertFalse(undated.details.dateFromFile);assertTrue(undated.details.dateMs>=before)
            val reopened=GpxLibrary(root).apply { load() };assertEquals(library.entries,reopened.entries)
            val json=org.json.JSONObject(File(root,"gpx-library/index.json").readText()).apply { remove("metadataVersion") }
            File(root,"gpx-library/index.json").writeText(json.toString())
            val upgraded=GpxLibrary(root).apply { load() };assertEquals(dated.id,upgraded.entries.first().id)
            assertFalse(upgraded.entries.last().details.dateFromFile)
        } finally { root.deleteRecursively() }
    }
    @Test fun fitVirtualActivityMetadataSurvivesIndexReloadAndProfileTypeOverride() {
        val root=java.nio.file.Files.createTempDirectory("virtual-fit").toFile()
        try {
            val file=File(root,"virtual.fit");val encoder=com.garmin.fit.FileEncoder(file,com.garmin.fit.Fit.ProtocolVersion.V2_0)
            try {
                encoder.write(com.garmin.fit.SportMesg().apply { setSport(com.garmin.fit.Sport.CYCLING);setSubSport(com.garmin.fit.SubSport.VIRTUAL_ACTIVITY) })
                for(i in 0..2) encoder.write(com.garmin.fit.RecordMesg().apply { setTimestamp(com.garmin.fit.DateTime(java.util.Date(now+i*1000)));setSpeed(10f);setDistance(i*10f) })
            } finally { encoder.close() }
            assertTrue(FitRead.read(file).details.virtualRide)
            val library=FitLibrary(root).apply { load() }
            val entry=file.inputStream().use { library.add(it,"Virtual",true) }!!
            assertTrue(entry.details.virtualRide)
            val reopened=FitLibrary(root).apply { load() }
            val stored=reopened.entries.single()
            assertTrue(stored.details.virtualRide)
            assertTrue(stored.details.copy(bikeType=BikeType.EBIKE).virtualRide)
        } finally { root.deleteRecursively() }
    }
    @Test fun templateRowsHaveCreatedDateDistanceAndEstimatedTimeWithSharedGlobalMapSlots() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val ride=route("ride");val plans=listOf(route("plan",RouteKind.IMPORTED),route("other",RouteKind.IMPORTED))
            val view=RouteManagerView(c,{},themes,scope=RouteKind.IMPORTED)
            c.setContentView(view);view.submit(listOf(ride)+plans,null,false,numbers=mapOf("ride" to 1,"plan" to 2,"other" to 3),shown=setOf("ride","plan"),estimateKmh=20.0);layout(view)
            val list=view.findViewWithTag<ListView>("route_list")
            assertFalse((0 until list.count).map { list.adapter.getItem(it) }.filterIsInstance<ManagedRoute>().any { it.id=="ride" })
            val position=(0 until list.count).first { (list.adapter.getItem(it) as? ManagedRoute)?.id=="plan" }
            val row=list.adapter.getView(position,null,list);layout(row,344)
            assertNull(row.findViewWithTag<TextView>("route_average_plan"))
            assertEquals("2:00",row.findViewWithTag<TextView>("route_moving_plan").text.toString())
            assertTrue(row.findViewWithTag<TextView>("route_date_plan").text.contains(" "))
            assertTrue(all(row).filterIsInstance<TextView>().any { it.text==c.getString(R.string.route_created_at) })
            val eye=row.findViewWithTag<android.widget.ImageButton>("route_eye_plan")
            assertTrue(eye.contentDescription.toString().contains("2"))
            val group=(0 until list.count).map { list.adapter.getItem(it) }.filterIsInstance<RouteListGroup>().first()
            assertTrue(group.templates);assertNull(group.movingMs);assertNull(group.averageKmh)
            val header=list.adapter.getView(0,null,list);layout(header,344)
            assertEquals(View.GONE,(header.findViewWithTag<TextView>("route_group_moving_0").parent.parent as View).visibility)
            view.submit(listOf(ride)+plans,null,false,estimateKmh=null)
            val missing=list.adapter.getView(position,row,list)
            assertEquals("--",missing.findViewWithTag<TextView>("route_moving_plan").text.toString())
        } finally { host.pause().stop().destroy() }
    }
    @Test fun settingsExposeTwoCollectionsAndTheirPopupsHaveNoKindSelector() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val menu=SettingsMenuView(c,themes) {}
            assertNotNull(menu.findViewWithTag<View>("settings_ROUTES"));assertNotNull(menu.findViewWithTag<View>("settings_ROUTE_TEMPLATES"))
            val advanced=SettingsMenuView(c,themes,advanced=true) {};assertNull(advanced.findViewWithTag<View>("settings_ROUTE_TEMPLATES"))
            for(kind in RouteKind.entries) {
                val query=RouteQuery(kind)
                val popup=RouteQueryPopup(c,themes,DialogNavigation(),query) {}
                popup.filters {}
                assertNull(popup.contentView.findViewWithTag<View>("route_query_group_${R.string.route_filter_kind}"))
                if(query.templates) assertNull(popup.contentView.findViewWithTag<View>("route_query_group_${R.string.route_filter_speed}"))
            }
        } finally { host.pause().stop().destroy() }
    }
}
