package de.bcycle.app

import android.content.SharedPreferences

/** Ordered graph choices; displayModes is retained only for old preference/backup compatibility. */
internal data class GraphOverlayConfig(val slots: List<List<AnalysisChart>> = List(DashboardLayout.FIELD_COUNT) { emptyList() },
    val displayModes: List<GraphDisplayMode> = List(DashboardLayout.FIELD_COUNT) { GraphDisplayMode.WITH_DATA }) {
    fun withDisplay(slot: Int,mode: GraphDisplayMode)=copy(displayModes=displayModes.mapIndexed { i,old -> if(i==slot) mode else old })
    fun encodeModes()=displayModes.joinToString("|") { it.name }
    fun replace(slot: Int, selected: List<AnalysisChart>) = copy(slots = slots.mapIndexed { i, old ->
        if (i == slot) selected.distinct().take(2) else old
    })
    fun encode() = slots.joinToString("|") { it.joinToString(",") { chart -> chart.name } }
    companion object {
        fun key(page: Int) = if (page == 0) "dashboardGraphs" else "dashboardGraphs_$page"
        fun read(prefs: SharedPreferences, page: Int):GraphOverlayConfig {
            val legacy=GraphDisplaySettings.read(prefs)
            val modes=prefs.getString(GraphDisplaySettings.key(page),null)?.split('|').orEmpty()
            return decode(prefs.getString(key(page),null)).copy(displayModes=List(DashboardLayout.FIELD_COUNT) { i ->
                GraphDisplayMode.entries.find { it.name==modes.getOrNull(i) } ?: legacy })
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
