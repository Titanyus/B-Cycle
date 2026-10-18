package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.LocalDate
import java.net.URI

enum class BackupSection(val id: String, val title: Int) {
    DESIGN("designUi", R.string.backup_design), DASHBOARD("dashboard", R.string.backup_dashboard),
    RECORDING("recordingFit", R.string.backup_recording), MAPS("mapsNavigation", R.string.backup_maps),
    PROFILE("riderProfile", R.string.rider_profile), CLOUD("cloudSettings", R.string.backup_cloud)
}
enum class BackupProblem { FORMAT, VERSION, INVALID, TOO_LARGE, SELECTION, IO, ROLLBACK }
class SettingsBackupFailure(val problem: BackupProblem) : Exception(problem.name)
internal data class SettingsBackupDocument(val sections: Map<BackupSection, JSONObject>)
internal data class SettingsRestorePlan(val bike: Map<String, Any?>, val route: Map<String, Any?>, val cloudNeedsPassword: Boolean)

/** Explicit public schema; never serialize preferences.all, files, pairing or credentials. */
internal class SettingsBackup(private val bike: SharedPreferences, private val route: SharedPreferences,
    private val themes: ThemeManager, private val today: () -> LocalDate = LocalDate::now) {
    constructor(context: Context, themes: ThemeManager) : this(context.getSharedPreferences("bike", 0), context.getSharedPreferences("route-export", 0), themes)
    companion object { const val MAX_BYTES = 262144; const val FORMAT = "bicycle-settings"; const val VERSION = 1 }
    private fun fail(reason: BackupProblem = BackupProblem.INVALID): Nothing = throw SettingsBackupFailure(reason)
    private fun check(ok: Boolean) { if (!ok) fail() }
    private fun field(row: JSONObject, name: String, limit: Int = 2048): String {
        val v = row.opt(name); check(v is String && v.length <= limit && !v.any(Char::isISOControl)); return v as String
    }
    private fun integer(row: JSONObject, name: String): Int {
        val v = row.opt(name); check(v is Number && v.toDouble().isFinite() && v.toDouble() == v.toInt().toDouble()); return (v as Number).toInt()
    }
    private fun boolean(row: JSONObject, name: String): Boolean { val v = row.opt(name); check(v is Boolean); return v as Boolean }
    private fun obj(row: JSONObject, name: String) = row.opt(name) as? JSONObject ?: fail()
    private fun array(row: JSONObject, name: String, limit: Int): JSONArray = (row.opt(name) as? JSONArray)?.also { check(it.length() <= limit) } ?: fail()
    private fun strings(a: JSONArray): List<String> = (0 until a.length()).map { (a.opt(it) as? String)?.takeIf { it.length <= 80 } ?: fail() }
    private fun jsonArray(values: Iterable<*>) = JSONArray().apply { values.forEach(::put) }
    private fun themeJson(t: CustomTheme) = JSONObject().put("id", t.id).put("name", t.name).apply {
        listOf("primary" to t.tokens.primary, "secondary" to t.tokens.secondary, "tertiary" to t.tokens.tertiary,
            "foreground" to t.tokens.foreground, "accent" to t.tokens.accent).forEach { (k,v) -> put(k, "#%06X".format(java.util.Locale.ROOT, v and 0xffffff)) }
    }
    private fun profileJson(p: RiderProfile) = JSONObject().put("birthDate", p.birthDate?.toString() ?: JSONObject.NULL)
        .put("heightCm", p.heightCm ?: JSONObject.NULL).put("weightKg", p.weightKg ?: JSONObject.NULL)
        .put("restingHeartRate", p.restingHeartRate ?: JSONObject.NULL).put("maximumHeartRate", p.maximumHeartRate ?: JSONObject.NULL).put("ftpW", p.ftpW ?: JSONObject.NULL).put("heartRateMethod",p.heartRateMethod.name).put("thresholdHeartRate",p.thresholdHeartRate ?: JSONObject.NULL)
    private fun dashboardIds():List<Int> = runCatching {
        val saved=JSONArray(bike.getString(DashboardPages.KEY,null))
        (0 until saved.length()).map { saved.getJSONObject(it).getInt("id") }.also { ids ->
            require(ids.isNotEmpty() && ids.size<=DashboardPages.MAX && ids.distinct().size==ids.size && ids.all { it in 0 until DashboardPages.MAX })
        }
    }.getOrDefault((0..3).toList())
    fun export(selected: Set<BackupSection>): ByteArray {
        if (selected.isEmpty()) fail(BackupProblem.SELECTION)
        val sections = JSONObject()
        selected.forEach { section -> sections.put(section.id, when(section) {
            BackupSection.DESIGN -> JSONObject().put("activeThemeId", themes.active.id).put("customThemes", jsonArray(themes.customThemes.map(::themeJson)))
                .put("settingsLight",themes.settingsLight)
                .put("modeGlow", ModeGlowSettings.enabled(bike))
                .put("modeGlowPlacement",ModeGlowSettings.placement(bike).name)
                .put("dashboardFullscreen",DashboardFullscreen.enabled(bike))

            BackupSection.DASHBOARD -> JSONObject().put("pages", JSONArray().apply { dashboardIds().forEach { i ->
                put(JSONObject().put("index", i).put("fields", jsonArray(DashboardLayout.decode(bike.getString(if(i==0) "dashboard" else "dashboard_$i", null)).fields.map { it.id }))
                    .put("graphs", JSONArray().apply { GraphOverlayConfig.read(bike,i).slots.forEach { put(jsonArray(it.map { chart -> chart.name })) } })
                    .put("graphDisplayModes",jsonArray(GraphOverlayConfig.read(bike,i).displayModes.map { it.name }))
                    .put("zoneGlowSources",jsonArray(ZoneGlowConfig.read(bike,i).sources.map { it.name }))
                    .put("dataVisible",JSONArray(DashboardDataVisibility.read(bike,i).visible))
                    .put("trendIndicators",JSONArray(DashboardTrendIndicators.read(bike,i).enabled))
                    .put("trainingHelp",JSONObject(DashboardTrainingHelp.read(bike,i).encode()))
                    .put("layout", DashboardTemplate.decode(bike.getString("dashboardTemplate_$i", DashboardTemplate.entries[i].name)).name))
            } }).put("catalog",bike.getString(DashboardPages.KEY,null) ?: JSONObject.NULL).put("activePage",bike.getInt("dashboardPage",0)).put("mapFields", JSONArray().apply { repeat(3) { put(MapFieldSelection.read(bike,it)?.id ?: JSONObject.NULL) } })
            BackupSection.RECORDING -> RecordingConfig.decode(bike.getStringSet("recordFields", null), bike.getString("recordSpeed",null), bike.getString("recordDistance",null)).let {
                JSONObject().put("fields", jsonArray(it.fields.sorted())).put("speedSource",it.speedSource).put("distanceSource",it.distanceSource).put("localSaveEnabled",route.getBoolean("save",true)) }
            BackupSection.MAPS -> JSONObject().put("source",MapSource.decode(bike.getString("mapSource",null)).name).put("night",bike.getBoolean("mapNight",false))
                .put("follow",bike.getBoolean("mapFollow",true)).put("heading",bike.getBoolean("mapHeading",true)).put("oval",bike.getBoolean(MapOvalAnchor.preference,false))
                .put("showTrack",bike.getBoolean("mapTrackVisible",true)).put("showGpx",bike.getBoolean("mapGpxVisible",true))
                .put("elevationCorrection",bike.getBoolean(FitElevationCorrection.preference,false))
                .put("routeStyles",runCatching { MapRouteStyles.validate(JSONObject(bike.getString(MapRouteStyles.key,"{}")!!)) }.getOrDefault(JSONObject()))
            BackupSection.PROFILE -> profileJson(RiderProfileStore(bike,today).read())
            BackupSection.CLOUD -> JSONObject().put("server",route.getString("server","")).put("user",route.getString("user","")).put("folder",route.getString("folder","B-Cycle"))
                .put("enabled",route.getBoolean("webdav",false)).put("wifiOnly",route.getBoolean("wifi",true))
        }) }
        val bytes = JSONObject().put("format",FORMAT).put("schemaVersion",VERSION).put("createdAt",Instant.now().toString())
            .put("appVersion",AppVersion.name).put("sections",sections).toString(2).toByteArray(Charsets.UTF_8)
        if(bytes.size>MAX_BYTES) fail(BackupProblem.TOO_LARGE)
        decode(bytes) // Refuse to export invalid configurations, including credentials embedded in URLs.
        return bytes
    }
    fun read(input: InputStream): SettingsBackupDocument = input.use { decode(it.readNBytes(MAX_BYTES + 1)) }
    fun write(output: OutputStream, bytes: ByteArray) { output.use { it.write(bytes); it.flush() } }
    fun decode(bytes: ByteArray): SettingsBackupDocument {
        if(bytes.size>MAX_BYTES) fail(BackupProblem.TOO_LARGE)
        try {
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            var depth=0;var quoted=false;var escaped=false
            text.forEach { c ->
                if(quoted) { if(escaped) escaped=false else if(c=='\\') escaped=true else if(c=='"') quoted=false }
                else if(c=='"') quoted=true else if(c=='{'||c=='[') { depth++;check(depth<=32) } else if(c=='}'||c==']') depth--
            }
            check(depth==0 && !quoted)
            val tokener=org.json.JSONTokener(text)
            val json=tokener.nextValue() as? JSONObject ?: fail()
            check(tokener.nextClean()==0.toChar())
            if(json.opt("format")!=FORMAT) fail(BackupProblem.FORMAT)
            if(integer(json,"schemaVersion")!=VERSION) fail(BackupProblem.VERSION)
            Instant.parse(field(json,"createdAt",80));field(json,"appVersion",80)
            val source=obj(json,"sections")
            val known=BackupSection.entries.filter { source.has(it.id) }.associateWith { obj(source,it.id) }
            if(known.isEmpty()) fail(BackupProblem.SELECTION)
            val doc=SettingsBackupDocument(known);plan(doc,known.keys);return doc
        } catch(e:SettingsBackupFailure) { throw e } catch(_:Exception) { fail() }
    }
    fun plan(doc: SettingsBackupDocument, selected: Set<BackupSection>): SettingsRestorePlan {
        if(selected.isEmpty() || !doc.sections.keys.containsAll(selected)) fail(BackupProblem.SELECTION)
        val b=linkedMapOf<String,Any?>();val r=linkedMapOf<String,Any?>();var needsPassword=false
        try { selected.forEach { section -> val row=doc.sections.getValue(section)
            when(section) {
                BackupSection.DESIGN -> {
                    var custom=themes.customThemes
                    if(row.has("customThemes")) {
                        val a=array(row,"customThemes",ThemeManager.maximumCustomThemes)
                        custom=(0 until a.length()).map { i ->
                            val t=a.opt(i) as? JSONObject ?: fail()
                            fun color(k:String):Int { val s=field(t,k,7);check(s.matches(Regex("#[0-9a-fA-F]{6}")));return (0xff000000L or s.drop(1).toLong(16)).toInt() }
                            val id=field(t,"id",80);val foreground=color("foreground")
                            CustomTheme(id,field(t,"name",60),ThemeTokens(color("primary"),color("secondary"),color("tertiary"),foreground,
                                if(t.has("accent")) color("accent") else themes.customThemes.find { it.id==id }?.tokens?.accent ?: foreground)).also { check(ThemeValidator.validate(it).valid) }
                        }
                        check(custom.map { it.id }.distinct().size==custom.size)
                        b[ThemeManager.customsKey]=JSONObject().put("version",2).put("themes",jsonArray(custom.map { themeJson(it).put("kind","custom") })).toString()
                    }
                    val active=if(row.has("activeThemeId")) field(row,"activeThemeId",80) else themes.active.id
                    check(BicycleThemes.findPreset(active)!=null || custom.any { it.id==active })
                    if(row.has("activeThemeId")) b[ThemeManager.activeKey]=BicycleThemes.findPreset(active)?.id ?: active
                    if(row.has("settingsLight")) b[ThemeManager.settingsLightKey]=boolean(row,"settingsLight")
                    if(row.has("modeGlow")) b[ModeGlowSettings.key]=boolean(row,"modeGlow")
                    if(row.has("modeGlowPlacement")) {
                        val placement=row.opt("modeGlowPlacement") as? String ?: fail()
                        check(ModeGlowPlacement.entries.any { it.name==placement })
                        b[ModeGlowSettings.placementKey]=placement
                    }
                    if(row.has("dashboardFullscreen")) b[DashboardFullscreen.key]=boolean(row,"dashboardFullscreen")
                    if(row.has("graphDisplay")) {
                        val mode=row.opt("graphDisplay") as? String ?: fail()
                        check(GraphDisplayMode.entries.any { it.name==mode })
                        b[GraphDisplaySettings.key]=mode
                    }
                }
                BackupSection.DASHBOARD -> {
                    if(row.has("pages")) {
                        val a=array(row,"pages",DashboardPages.MAX);val indices=mutableSetOf<Int>()
                        repeat(a.length()) { i -> val page=a.opt(i) as? JSONObject ?: fail();val index=integer(page,"index");check(index in 0 until DashboardPages.MAX && indices.add(index))
                            if(page.has("fields")) {
                                val fields=strings(array(page,"fields",DashboardLayout.FIELD_COUNT))
                                check(fields.size in 7..DashboardLayout.FIELD_COUNT && fields.all { DashboardField.fromId(it)!=null })
                                val key=if(index==0) "dashboard" else "dashboard_$index"
                                // Older backups contain seven slots; keep the new slot's current choice.
                                val current=DashboardLayout.decode(bike.getString(key,null)).fields
                                b[key]=(fields + current.drop(fields.size).map { it.id }).joinToString("|")
                            }
                            if(page.has("graphs")) {
                                val graphs=array(page,"graphs",DashboardLayout.FIELD_COUNT);check(graphs.length()==DashboardLayout.FIELD_COUNT)
                                val slots=(0 until graphs.length()).map { slot ->
                                    val values=graphs.opt(slot) as? JSONArray ?: fail()
                                    check(values.length()<=2)
                                    val names=strings(values);check(names.distinct().size==names.size && names.all { name -> AnalysisChart.entries.any { it.name==name } })
                                    names.map { AnalysisChart.valueOf(it) }
                                }
                                b[GraphOverlayConfig.key(index)]=GraphOverlayConfig(slots).encode()
                            }
                            if(page.has("trainingHelp")) {
                                val helper=DashboardTrainingHelp.decode(obj(page,"trainingHelp").toString()) ?: fail()
                                b[DashboardTrainingHelp.key(index)]=helper.encode()
                            } else b[DashboardTrainingHelp.key(index)]=null
                            if(page.has("trendIndicators")) {
                                val values=array(page,"trendIndicators",DashboardLayout.FIELD_COUNT)
                                check(values.length()==DashboardLayout.FIELD_COUNT)
                                val flags=(0 until values.length()).map { slot -> values.opt(slot) as? Boolean ?: fail() }
                                b[DashboardTrendIndicators.key(index)]=DashboardTrendIndicators(flags).encode()
                            } else b[DashboardTrendIndicators.key(index)]=null // Old backups keep the former always-visible behavior.
                            if(page.has("dataVisible")) {
                                val values=array(page,"dataVisible",DashboardLayout.FIELD_COUNT)
                                check(values.length()==DashboardLayout.FIELD_COUNT)
                                val flags=(0 until values.length()).map { slot -> values.opt(slot) as? Boolean ?: fail() }
                                b[DashboardDataVisibility.key(index)]=DashboardDataVisibility(flags).encode()
                            }
                            if(page.has("zoneGlowSources")) {
                                val sources=strings(array(page,"zoneGlowSources",DashboardLayout.FIELD_COUNT))
                                check(sources.size==DashboardLayout.FIELD_COUNT && sources.all { name -> ZoneGlowSource.entries.any { it.name==name } })
                                b[ZoneGlowConfig.key(index)]=sources.joinToString("|")
                            }
                            if(page.has("graphDisplayModes")) {
                                val modes=strings(array(page,"graphDisplayModes",DashboardLayout.FIELD_COUNT))
                                check(modes.size==DashboardLayout.FIELD_COUNT && modes.all { name -> GraphDisplayMode.entries.any { it.name==name } })
                                b[GraphDisplaySettings.key(index)]=modes.joinToString("|")
                            }
                            if(page.has("layout")) { val layout=field(page,"layout",30);check(DashboardTemplate.entries.any { it.name==layout });b["dashboardTemplate_$index"]=layout }
                        }
                    }
                    if(row.has("catalog")) {
                        if(row.isNull("catalog")) b[DashboardPages.KEY]=null else {
                            val encoded=field(row,"catalog",2048)
                            val catalog=runCatching { JSONArray(encoded) }.getOrElse { fail() }
                            check(catalog.length() in 1..DashboardPages.MAX)
                            val ids=mutableSetOf<Int>()
                            repeat(catalog.length()) { i ->
                                val page=catalog.optJSONObject(i) ?: fail()
                                val id=integer(page,"id");val name=field(page,"name",60)
                                check(id in 0 until DashboardPages.MAX && ids.add(id) && name.trim().isNotEmpty())
                            }
                            val pages=array(row,"pages",DashboardPages.MAX)
                            check(ids==(0 until pages.length()).map { integer(pages.getJSONObject(it),"index") }.toSet())
                            b[DashboardPages.KEY]=encoded
                        }
                    } else if(row.has("pages")) b[DashboardPages.KEY]=null
                    if(row.has("activePage")) {
                        val active=integer(row,"activePage");check(active in 0 until DashboardPages.MAX)
                        b["dashboardPage"]=active
                    }
                    if(row.has("mapFields")) { val a=array(row,"mapFields",3);check(a.length()==3);repeat(3) { i ->
                        b[MapFieldSelection.key(i)] = if(a.isNull(i)) null else (a.opt(i) as? String)?.also { check(DashboardField.fromId(it)!=null) } ?: fail()
                    } }
                }
                BackupSection.RECORDING -> {
                    if(row.has("fields")) { val fields=strings(array(row,"fields",RecordField.options.size));check(fields.distinct().size==fields.size && fields.all { id -> RecordField.options.any { it.id==id } });b["recordFields"]=fields.toSet() }
                    if(row.has("speedSource")) b["recordSpeed"]=field(row,"speedSource",30).also { check(it in listOf("SPEED","GPS_SPEED")) }
                    if(row.has("distanceSource")) b["recordDistance"]=field(row,"distanceSource",30).also { check(it in listOf("BIKE_DISTANCE","GPS_DISTANCE")) }
                    if(row.has("localSaveEnabled")) r["save"]=boolean(row,"localSaveEnabled")
                }
                BackupSection.MAPS -> {
                    if(row.has("routeStyles")) b[MapRouteStyles.key]=MapRouteStyles.validate(row.getJSONObject("routeStyles")).toString()
                    if(row.has("source")) b["mapSource"]=field(row,"source",30).also { v -> check(MapSource.entries.any { it.name==v }) }
                    mapOf("night" to "mapNight","follow" to "mapFollow","heading" to "mapHeading","oval" to MapOvalAnchor.preference,"showTrack" to "mapTrackVisible","showGpx" to "mapGpxVisible","elevationCorrection" to FitElevationCorrection.preference).forEach { (public,key) -> if(row.has(public)) b[key]=boolean(row,public) }
                }
                BackupSection.PROFILE -> {
                    val old=RiderProfileStore(bike,today).read()
                    fun number(k:String,previous:Int?):Int? = if(!row.has(k)) previous else if(row.isNull(k)) null else integer(row,k)
                    val profile=old.copy(birthDate=if(!row.has("birthDate")) old.birthDate else if(row.isNull("birthDate")) null else LocalDate.parse(field(row,"birthDate",20)),
                        heightCm=number("heightCm",old.heightCm),weightKg=if(!row.has("weightKg")) old.weightKg else if(row.isNull("weightKg")) null else (row.opt("weightKg") as? Number)?.toDouble() ?: fail(),
                        restingHeartRate=number("restingHeartRate",old.restingHeartRate),maximumHeartRate=number("maximumHeartRate",old.maximumHeartRate),ftpW=number("ftpW",old.ftpW),
                        heartRateMethod=if(row.has("heartRateMethod")) HeartRateMethod.valueOf(field(row,"heartRateMethod",30)) else old.heartRateMethod,
                        thresholdHeartRate=number("thresholdHeartRate",old.thresholdHeartRate))
                    check(RiderProfileValidation.validate(profile,today()).isEmpty())
                    if(listOf("birthDate","heightCm","weightKg","restingHeartRate","maximumHeartRate","ftpW","heartRateMethod","thresholdHeartRate").any(row::has))
                        b[RiderProfileStore.key]=profileJson(profile).put("version",1).toString()
                }
                BackupSection.CLOUD -> {
                    listOf("server","user","folder").forEach { key -> if(row.has(key)) r[key]=field(row,key) }
                    if(row.has("wifiOnly")) r["wifi"]=boolean(row,"wifiOnly")
                    val server=(r["server"] as? String) ?: route.getString("server","")!!
                    val user=(r["user"] as? String) ?: route.getString("user","")!!
                    val folder=(r["folder"] as? String) ?: route.getString("folder","B-Cycle")!!
                    if(server.isNotEmpty()) { val uri=URI(server);check(uri.scheme.equals("https",true) && !uri.host.isNullOrBlank() && uri.rawUserInfo==null && uri.rawQuery==null && uri.rawFragment==null) }
                    check(user !in setOf(".","..") && user.none { it=='/' || it=='\\' || it==':' || it.isISOControl() })
                    check(folder.split('/').none { it=="." || it==".." || it.contains('\\') })
                    val changed=server!=route.getString("server","") || user!=route.getString("user","")
                    if(row.has("enabled")) { val enabled=boolean(row,"enabled")
                        if(enabled) WebDavClient.directory(RouteExportConfig(server=server,user=user,folder=folder))
                        needsPassword=changed || (enabled && (!route.contains("password") || route.getBoolean("needsPassword",false)));r["webdav"]=enabled && !needsPassword
                    } else if(changed) { r["webdav"]=false;needsPassword=true }
                    if(needsPassword) r["needsPassword"]=true
                }
            }
        } } catch(e:SettingsBackupFailure) { throw e } catch(_:Exception) { fail() }
        return SettingsRestorePlan(b.toMap(),r.toMap(),needsPassword)
    }
    /** Prepare/validate all sections first. Each preferences file commits atomically; failures roll back changed keys only. */
    fun apply(plan: SettingsRestorePlan) {
        val oldBike=plan.bike.keys.associateWith { bike.all[it] };val oldRoute=plan.route.keys.associateWith { route.all[it] }
        fun write(p:SharedPreferences, values:Map<String,Any?>):Boolean {
            val e=p.edit();values.forEach { (k,v) -> when(v) { null->e.remove(k);is Boolean->e.putBoolean(k,v);is String->e.putString(k,v);is Int->e.putInt(k,v);is Float->e.putFloat(k,v);is Long->e.putLong(k,v);is Set<*>->e.putStringSet(k,v.filterIsInstance<String>().toSet());else->fail() } };return e.commit()
        }
        try { if(!write(bike,plan.bike)||!write(route,plan.route)) fail(BackupProblem.IO) }
        catch(_:Exception) { val restoredB=runCatching { write(bike,oldBike) }.getOrDefault(false);val restoredR=runCatching { write(route,oldRoute) }.getOrDefault(false)
            fail(if(restoredB && restoredR) BackupProblem.IO else BackupProblem.ROLLBACK)
        }
    }
}
