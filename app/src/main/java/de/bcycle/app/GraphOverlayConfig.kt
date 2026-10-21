package de.bcycle.app

import android.content.SharedPreferences

/** Ordered graph choices; displayModes is retained only for old preference/backup compatibility. */
internal data class GraphOverlayConfig(val slots: List<List<AnalysisChart>> = List(DashboardLayout.FIELD_COUNT) { emptyList() },
    val displayModes: List<GraphDisplayMode> = List(DashboardLayout.FIELD_COUNT) { GraphDisplayMode.WITH_DATA },
    val windows:List<GraphWindow> = List(DashboardLayout.FIELD_COUNT) { GraphWindow.FULL }) {
    fun withDisplay(slot: Int,mode: GraphDisplayMode)=copy(displayModes=displayModes.mapIndexed { i,old -> if(i==slot) mode else old })
    fun withWindow(slot:Int,value:GraphWindow)=copy(windows=windows.mapIndexed { i,old -> if(i==slot) value else old })
    fun encodeWindows()=windows.joinToString("|") { it.name }
    fun encodeModes()=displayModes.joinToString("|") { it.name }
    fun replace(slot: Int, selected: List<AnalysisChart>) = copy(slots = slots.mapIndexed { i, old ->
        if (i == slot) selected.distinct().take(2) else old
    })
    fun encode() = slots.joinToString("|") { it.joinToString(",") { chart -> chart.name } }
    companion object {
        fun windowKey(page:Int)="dashboardGraphWindows_$page"
        fun key(page: Int) = if (page == 0) "dashboardGraphs" else "dashboardGraphs_$page"
        fun read(prefs: SharedPreferences, page: Int):GraphOverlayConfig {
            val legacy=GraphDisplaySettings.read(prefs)
            val modes=prefs.getString(GraphDisplaySettings.key(page),null)?.split('|').orEmpty()
            val windows=prefs.getString(windowKey(page),null)?.split('|').orEmpty()
            return decode(prefs.getString(key(page),null)).copy(displayModes=List(DashboardLayout.FIELD_COUNT) { i ->
                GraphDisplayMode.entries.find { it.name==modes.getOrNull(i) } ?: legacy },
                windows=List(DashboardLayout.FIELD_COUNT) { i -> GraphWindow.entries.find { it.name==windows.getOrNull(i) } ?: GraphWindow.FULL })
        }
        fun decode(text: String?): GraphOverlayConfig {
            val entries = text?.split('|').orEmpty()
            return GraphOverlayConfig(List(DashboardLayout.FIELD_COUNT) { i -> entries.getOrNull(i)?.split(',')
                ?.mapNotNull { name -> AnalysisChart.entries.find { it.name == name } }?.distinct()?.take(2).orEmpty() })
        }
    }
}

/** Internal graph brightness states and legacy visibility migration; no graph-mode selector UI. */
internal enum class GraphDisplayMode { WITH_DATA, GRAPH_ONLY }
internal object GraphDisplaySettings {
    const val key="dashboardGraphDisplay"
    fun key(page: Int)="${key}_$page"
    fun read(prefs: SharedPreferences)=GraphDisplayMode.entries.find { it.name==prefs.getString(key,null) } ?: GraphDisplayMode.WITH_DATA
}

internal enum class GraphWindow(val milliseconds:Long,val label:Int) {
    FULL(0,R.string.graph_window_full),SECONDS_30(30000,R.string.graph_window_30),
    SECONDS_60(60000,R.string.graph_window_60),SECONDS_120(120000,R.string.graph_window_120)
}
/** Configured capabilities, independent of live reception or temporarily disconnected devices. */
internal object GraphAvailability {
    fun forProfile(p:BikeProfile):Set<AnalysisChart> = buildSet {
        val trainer=p.type==BikeType.INDOOR && p.trainerEnabled && p.trainerAddress!=null
        val bike=p.type==BikeType.EBIKE && p.bikeEnabled && p.bikeAddress!=null
        val gps=p.type!=BikeType.INDOOR && p.gpsEnabled
        if(trainer || if(p.recording.speedSource=="GPS_SPEED") gps else bike) add(AnalysisChart.SPEED)
        if(trainer || bike) { add(AnalysisChart.CADENCE);add(AnalysisChart.POWER) }
        if(p.heartEnabled && p.heartAddress!=null || trainer) add(AnalysisChart.HEART)
        if(gps) add(AnalysisChart.ELEVATION)
    }
}
