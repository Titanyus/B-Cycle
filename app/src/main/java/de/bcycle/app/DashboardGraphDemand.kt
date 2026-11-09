package de.bcycle.app

/** One bounded snapshot, keyed by all actual graph choices and template capabilities. */
internal class DashboardGraphDemand {
    private var template:DashboardTemplate?=null
    private var config:GraphOverlayConfig?=null
    private var demand:Map<GraphWindow,Set<AnalysisChart>> = emptyMap()
    fun get(nextTemplate:DashboardTemplate,nextConfig:GraphOverlayConfig):Map<GraphWindow,Set<AnalysisChart>> {
        if(template==nextTemplate && config==nextConfig) return demand
        template=nextTemplate;config=nextConfig
        demand=buildMap<GraphWindow,Set<AnalysisChart>> {
            nextConfig.slots.indices.forEach { index ->
                if(nextTemplate.supportsGraph(index) && nextConfig.slots[index].isNotEmpty()) {
                    val window=nextConfig.windows[index]
                    put(window,get(window).orEmpty()+nextConfig.slots[index])
                }
            }
        }
        return demand
    }
}
