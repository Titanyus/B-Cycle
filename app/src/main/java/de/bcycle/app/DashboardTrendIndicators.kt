package de.bcycle.app

/** Presentation only: the existing trend history/calculation continues even while hidden. */
internal data class DashboardTrendIndicators(val enabled:List<Boolean> = List(DashboardLayout.FIELD_COUNT) { true }) {
    fun replace(slot:Int,show:Boolean)=copy(enabled=enabled.mapIndexed { i,old -> if(i==slot) show else old })
    fun encode()=enabled.joinToString("|") { if(it) "1" else "0" }
    companion object {
        fun key(page:Int)="dashboardTrendIndicators_$page"
        fun read(prefs:android.content.SharedPreferences,page:Int):DashboardTrendIndicators {
            val values=prefs.getString(key(page),null)?.split('|').orEmpty()
            return DashboardTrendIndicators(List(DashboardLayout.FIELD_COUNT) { values.getOrNull(it)!="0" })
        }
    }
}
