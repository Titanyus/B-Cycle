package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun ComposeSettingsMenu(advanced:Boolean,selected:(SettingsAction)->Unit) {
    val context=LocalContext.current
    val actions=if(advanced) SettingsAction.entries.filter { it.ordinal>=SettingsAction.RECOVERY.ordinal } else
        listOf(SettingsAction.SENSORS,SettingsAction.PROFILE,SettingsAction.MAP,SettingsAction.DASHBOARDS,SettingsAction.ROUTES,SettingsAction.DATA,SettingsAction.ADVANCED)
    LazyColumn(Modifier.fillMaxSize().testTag("settings_list")) { items(actions,key={ it.name }) { action ->
        BicycleMenuRow(action.label(context),action.icon,!advanced,"settings_${action.name}") { selected(action) };BicycleDivider()
    } }
}
@Composable internal fun ComposeDataSettings(selected:(DataAction)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        listOf(R.string.data_global to listOf(DataAction.BACKUP,DataAction.RESTORE),R.string.data_routes to listOf(DataAction.LOCAL,DataAction.CLOUD)).forEach { (title,actions) ->
            Box(Modifier.padding(horizontal=16.dp,vertical=8.dp)) { BicycleSection(stringResource(title)) }
            actions.forEach { BicycleMenuRow(stringResource(it.title),it.icon.resource,tag="data_${it.name}") { selected(it) } }
            BicycleDivider()
        }
    }
}
@Composable internal fun ComposeBackupSelection(available:Set<BackupSection>,selected:Set<BackupSection>,changed:(Set<BackupSection>)->Unit) {
    BicycleBody {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { BicycleAction(stringResource(R.string.backup_all),tag="backup_all") { changed(available) } }
            Box(Modifier.weight(1f)) { BicycleAction(stringResource(R.string.backup_none),tag="backup_none") { changed(emptySet()) } }
        }
        BackupSection.entries.filter { it in available }.forEach { section ->
            BicycleCheck(stringResource(section.title),section in selected,"backup_${section.id}") { if(it) changed(selected+section) else changed(selected-section) }
        }
    }
}
@Composable internal fun ComposeDesignSettings(prefs:SharedPreferences,themes:ThemeManager,currentPage:Int,refresh:()->Unit,editor:()->Unit) {
    preferenceRevision(prefs)
    var page by rememberSaveable { mutableIntStateOf(currentPage) }
    BicycleBody {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val scale=androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
            val columns=(maxWidth.value/(BicycleDesign.themeTileMinimumWidth*scale+BicycleDesign.smallGap)).toInt().coerceIn(1,BicycleDesign.themeColumns)
            Column {
                (themes.presets.map { it as PresetTheme? }+null).chunked(columns).forEach { row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(BicycleDesign.smallGap.dp)) {
                        row.forEach { theme ->
                            val active=theme!=null && themes.active.id==theme.id
                            val foreground=Color(LocalBicycleColors.current.foreground)
                            Column(Modifier.weight(1f).height((BicycleDesign.themeTileHeight*scale).dp).testTag(theme?.id ?: "theme_reserved")
                                .border(if(active) BicycleDesign.themeSelectedBorder.dp else 1.dp,foreground.copy(alpha=if(active) 1f else .33f),androidx.compose.foundation.shape.RoundedCornerShape(BicycleDesign.inputRadius.dp))
                                .clickable(enabled=theme!=null) { themes.activate(theme!!.id) }.padding(BicycleDesign.verticalPadding.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                androidx.compose.foundation.text.BasicText(theme?.name ?: stringResource(R.string.compose_theme_reserved),Modifier.fillMaxWidth().height((BicycleDesign.themeNameHeight*scale).dp),maxLines=2,
                                    style=androidx.compose.ui.text.TextStyle(color=foreground,fontSize=BicycleDesign.themeNameSp.sp,fontFamily=androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.bebas_neue)),textAlign=androidx.compose.ui.text.style.TextAlign.Center))
                                if(theme==null) Box(Modifier.weight(1f).fillMaxWidth()) { Box(Modifier.align(Alignment.Center)) { BicycleIconImage(BicycleIcon.AddSensor) } }
                                else listOf(theme.tokens.primary,theme.tokens.secondary,theme.tokens.tertiary).forEach { color -> Box(Modifier.fillMaxWidth().weight(1f).background(Color(color))) }
                            }
                        }
                        repeat(columns-row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(BicycleDesign.smallGap.dp))
                }
            }
        }
        BicycleDivider();BicycleSection(stringResource(R.string.compose_dashboard_page))
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { repeat(4) { index ->
            Box(Modifier.weight(1f)) { BicycleCheck((index+1).toString(),page==index,"preset_page_$index",radio=true) { page=index } }
        } }
        val chosen=DashboardTemplate.decode(prefs.getString("dashboardTemplate_$page",DashboardTemplate.entries[page].name))
        DashboardTemplate.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { row.forEach { template ->
                Column(Modifier.weight(1f).height(BicycleDesign.presetTileHeight.dp).testTag("preset_${template.name}")
                    .border(if(chosen==template) 2.dp else 1.dp,Color(LocalBicycleColors.current.foreground),androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
                    .clickable { prefs.edit().putString("dashboardTemplate_$page",template.name).apply();refresh() }.padding(6.dp)) {
                    val foreground=Color(LocalBicycleColors.current.foreground)
                    Canvas(Modifier.fillMaxWidth().weight(1f)) {
                        val weight=template.rows.indices.sumOf { template.rowWeight(it).toDouble() }.toFloat()
                        var y=0f
                        template.rows.forEachIndexed { i,slots ->
                            val h=size.height*template.rowWeight(i)/weight
                            slots.indices.forEach { x -> drawRect(foreground,androidx.compose.ui.geometry.Offset(x*size.width/slots.size+2,y+2),androidx.compose.ui.geometry.Size(size.width/slots.size-4,h-4),style=Stroke(1.dp.toPx())) };y+=h
                        }
                    }
                    BicycleText(stringResource(R.string.dashboard_preset_values,template.slots.size))
                }
            } }
        }
        BicycleDivider();BicycleSection(stringResource(R.string.compose_glow))
        BicycleCheck(stringResource(R.string.compose_glow),ModeGlowSettings.enabled(prefs),"mode_glow_enabled") { ModeGlowSettings.save(prefs,it);refresh() }
        BicycleDivider();BicycleAction(stringResource(R.string.dashboard_editor_open),BicycleIcon.EditDashboard,tag="dashboard_editor_open",action=editor)
    }
}
@Composable internal fun ComposeMapSettings(prefs:SharedPreferences,refresh:()->Unit,gps:()->Unit,legend:()->Unit) {
    preferenceRevision(prefs)
    BicycleBody {
        BicycleSection(stringResource(R.string.compose_map_source))
        BicycleDropdown(stringResource(R.string.compose_map_source),MapSource.entries.map { it.title },MapSource.decode(prefs.getString("mapSource",null)).ordinal,"map_source") {
            prefs.edit().putString("mapSource",MapSource.entries[it].name).apply();refresh()
        }
        listOf("mapFollow" to R.string.compose_map_follow,"mapHeading" to R.string.compose_map_heading,"mapGpxVisible" to R.string.compose_map_gpx,"mapTrackVisible" to R.string.compose_map_track).forEach { (key,label) ->
            BicycleCheck(stringResource(label),prefs.getBoolean(key,true),key) { prefs.edit().putBoolean(key,it).apply();refresh() }
        }
        BicycleDivider();BicycleAction(stringResource(R.string.compose_gps_enable),BicycleIcon.Map,action=gps)
        BicycleAction(stringResource(R.string.compose_map_legend),BicycleIcon.Info,action=legend)
    }
}
@Composable internal fun ComposeSensors(runtime:RideRuntime,choose:(String)->Unit,toggle:(String)->Unit) {
    runtimeRevision(runtime);preferenceRevision(runtime.preferences)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context=LocalContext.current
    val current=runtime.config
    LazyColumn(Modifier.fillMaxSize().testTag("sensor_list"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(Triple("gps",stringResource(R.string.compose_gps),runtime.gpsStatus),Triple("bike",context.getString(R.string.compose_bike_name,if(runtime.bikeAddress==null) context.getString(R.string.compose_no_bike) else runtime.bikeName),runtime.bikeStatus),Triple("heart",context.getString(R.string.compose_heart_name,if(runtime.heartAddress==null) context.getString(R.string.compose_no_heart) else runtime.heartName),runtime.heartStatus)).forEach { (kind,title,status) ->
                BicycleSection(title);BicycleText(status,Modifier.testTag("${kind}_state"))
                if(kind!="gps") BicycleAction(context.getString(if(kind=="bike") R.string.compose_bike_choose else R.string.compose_sensor_choose),BicycleIcon.Bluetooth) { choose(kind) }
                val enabled=when(kind) { "gps"->runtime.gpsEnabled;"bike"->runtime.bikeEnabled && runtime.bikeAddress!=null;else->runtime.heartEnabled && runtime.heartAddress!=null }
                BicycleAction(context.getString(if(enabled) R.string.compose_disconnect else R.string.compose_connect),if(enabled) BicycleIcon.Connection else BicycleIcon.Disconnect,tag="sensor_toggle_$kind") { toggle(kind) }
                Spacer(Modifier.height(8.dp));BicycleDivider()
            }
            BicycleAction(context.getString(R.string.compose_sensor_add),BicycleIcon.AddSensor) { choose("add") }
            Spacer(Modifier.height(8.dp));BicycleDivider()
            BicycleAction(context.getString(R.string.compose_recording),BicycleIcon.Expand,tag="sensor_recording_toggle") { expanded=!expanded }
        } }
        if(expanded) {
            item { Column {
                BicycleSection(context.getString(R.string.compose_fit_speed))
                BicycleDropdown(context.getString(R.string.compose_fit_speed),listOf("E-Bike","GPS"),if(current.speedSource=="SPEED") 0 else 1,"record_speed") { runtime.saveConfig(runtime.config.copy(speedSource=if(it==0) "SPEED" else "GPS_SPEED")) }
                Spacer(Modifier.height(8.dp));BicycleSection(context.getString(R.string.compose_fit_distance))
                BicycleDropdown(context.getString(R.string.compose_fit_distance),listOf("E-Bike","GPS"),if(current.distanceSource=="BIKE_DISTANCE") 0 else 1,"record_distance") { runtime.saveConfig(runtime.config.copy(distanceSource=if(it==0) "BIKE_DISTANCE" else "GPS_DISTANCE")) }
            } }
            RecordField.options.groupBy { it.source }.forEach { (source,fields) ->
                item { Column { BicycleDivider();BicycleSection(source) } }
                items(fields,key={ it.id }) { field ->
                    BicycleCheck(field.titleResource?.let(context::getString) ?: field.title,field.id in current.fields,"record_${field.id}") {
                        val latest=runtime.config;runtime.saveConfig(latest.copy(fields=if(it) latest.fields+field.id else latest.fields-field.id))
                    }
                }
            }
        }
    }
}
@Composable internal fun ComposeFieldPicker(data:LiveData,gps:GpsValues,distance:Long?,pulse:PulseValues,selected:DashboardField?,allowEmpty:Boolean,
    graphs:List<AnalysisChart>?,display:GraphDisplayMode,choose:(DashboardField?)->Unit,graphChanged:(List<AnalysisChart>,GraphDisplayMode)->Unit) {
    val context=LocalContext.current
    val groups=remember(data.assistNames,selected) { DashboardField.groups(data,selected).groupBy { it.source } }
    val sources=groups.keys.toList()
    var source by rememberSaveable { mutableIntStateOf(sources.indexOf(selected?.source).coerceAtLeast(0)) }
    var graphChoice by remember { mutableStateOf(graphs.orEmpty()) }
    var graphMode by remember { mutableStateOf(display) }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=8.dp)) {
        BicycleDropdown(context.getString(R.string.compose_field_source),sources,source,"field_source",icon={ when(it) { "GPS"->BicycleIcon.GpsSource;"Pulsmesser"->BicycleIcon.Heart;"Wetter"->BicycleIcon.WeatherSource;else->BicycleIcon.TrackRecorded } },action={ source=it })
        if(allowEmpty) BicycleCheck(context.getString(R.string.compose_field_empty),selected==null,"map_field_empty",radio=true) { choose(null) }
        val scroll=remember(source) { androidx.compose.foundation.lazy.LazyListState() }
        LazyColumn(Modifier.weight(1f).testTag("field_list"),state=scroll,verticalArrangement=Arrangement.spacedBy(4.dp)) {
            groups.getValue(sources[source]).forEach { group ->
                item { BicycleSection(group.title) }
                items(group.fields,key={ it.id }) { field ->
                    val value=field.value(data,gps,distance,pulse)
                    BicycleCheck((if(field.id=="RIDER_POWER") context.getString(R.string.compose_rider_power) else field.title(data,gps))+if(value.unit.isEmpty()) "" else " · ${value.unit}",selected==field,field.id,radio=true) { choose(field) }
                }
            }
            if(graphs!=null) {
                item { Column { BicycleDivider();BicycleSection(context.getString(R.string.graph_overlay)) } }
                item { BicycleCheck(context.getString(R.string.graph_none),graphChoice.isEmpty(),"graph_none") { graphChoice=emptyList();graphChanged(graphChoice,graphMode) } }
                items(AnalysisChart.entries,key={ "graph_${it.name}" }) { chart ->
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Canvas(Modifier.size(14.dp)) { drawCircle(Color(chart.color(android.graphics.Color.BLACK))) }
                        Box(Modifier.weight(1f)) {
                            BicycleCheck(context.getString(if(chart==AnalysisChart.SPEED) R.string.graph_speed else if(chart==AnalysisChart.HEART) R.string.graph_heart else chart.label),chart in graphChoice,"graph_${chart.name}",chart in graphChoice || graphChoice.size<2) {
                                graphChoice=if(it) graphChoice+chart else graphChoice-chart;graphChanged(graphChoice,graphMode)
                            }
                        }
                    }
                }
                item { Column { BicycleSection(context.getString(R.string.graph_display_title))
                    BicycleDropdown(context.getString(R.string.graph_display_title),listOf(context.getString(R.string.graph_display_with_data),context.getString(R.string.graph_display_only)),graphMode.ordinal,"graph_display_mode") {
                        graphMode=GraphDisplayMode.entries[it];graphChanged(graphChoice,graphMode)
                    }
                } }
            }
        }
    }
}
