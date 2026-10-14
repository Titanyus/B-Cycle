package de.bcycle.app

import android.content.SharedPreferences

/** Ordered display-only selection, kept beside each existing dashboard page. */
internal data class GraphOverlayConfig(val slots: List<List<AnalysisChart>> = List(DashboardLayout.FIELD_COUNT) { emptyList() }) {
    fun replace(slot: Int, selected: List<AnalysisChart>) = copy(slots = slots.mapIndexed { i, old ->
        if (i == slot) selected.distinct().take(2) else old
    })
    fun encode() = slots.joinToString("|") { it.joinToString(",") { chart -> chart.name } }
    companion object {
        fun key(page: Int) = if (page == 0) "dashboardGraphs" else "dashboardGraphs_$page"
        fun read(prefs: SharedPreferences, page: Int) = decode(prefs.getString(key(page), null))
        fun decode(text: String?): GraphOverlayConfig {
            val entries = text?.split('|').orEmpty()
            return GraphOverlayConfig(List(DashboardLayout.FIELD_COUNT) { i -> entries.getOrNull(i)?.split(',')
                ?.mapNotNull { name -> AnalysisChart.entries.find { it.name == name } }?.distinct()?.take(2).orEmpty() })
        }
    }
}

/** Global display preference; graph sources and sensor values remain unchanged. */
internal enum class GraphDisplayMode { WITH_DATA, GRAPH_ONLY }
internal object GraphDisplaySettings {
    const val key="dashboardGraphDisplay"
    fun read(prefs: SharedPreferences)=GraphDisplayMode.entries.find { it.name==prefs.getString(key,null) } ?: GraphDisplayMode.WITH_DATA
}
