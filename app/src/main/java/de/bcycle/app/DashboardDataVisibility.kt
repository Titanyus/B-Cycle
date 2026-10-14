package de.bcycle.app

/** Main-value visibility is independent of graphs, glow and the selected field's stable ID. */
internal data class DashboardDataVisibility(val visible: List<Boolean> = List(DashboardLayout.FIELD_COUNT) { true }) {
    fun replace(slot: Int, enabled: Boolean) = copy(visible=visible.mapIndexed { i,old -> if(i==slot) enabled else old })
    fun encode() = visible.joinToString("|") { if(it) "1" else "0" }
    companion object {
        fun key(page: Int) = "dashboardDataVisible_$page"
        fun read(prefs: android.content.SharedPreferences,page: Int): DashboardDataVisibility {
            val encoded=prefs.getString(key(page),null)
            if(encoded!=null) {
                val values=encoded.split('|')
                return DashboardDataVisibility(List(DashboardLayout.FIELD_COUNT) { values.getOrNull(it)!="0" })
            }
            // Preserve the former graph-only display only where a graph was actually visible.
            val graphs=GraphOverlayConfig.read(prefs,page)
            val template=DashboardTemplate.decode(prefs.getString("dashboardTemplate_$page",DashboardTemplate.entries[page].name))
            return DashboardDataVisibility(List(DashboardLayout.FIELD_COUNT) { slot ->
                !(template.supportsGraph(slot) && graphs.slots[slot].isNotEmpty() && graphs.displayModes[slot]==GraphDisplayMode.GRAPH_ONLY)
            })
        }
    }
}
