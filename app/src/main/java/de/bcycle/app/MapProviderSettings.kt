package de.bcycle.app

import android.content.Context

/** Provider key is private to this install; never part of settings exports or diagnostics. */
internal object MapProviderSettings {
    private val keyPattern=Regex("[A-Fa-f0-9]{32}")
    fun valid(key: String)=keyPattern.matches(key)
    fun read(context: Context): String = context.getSharedPreferences("map-provider",Context.MODE_PRIVATE)
        .getString("thunderforest","").orEmpty().takeIf(::valid).orEmpty()
    fun write(context: Context,key: String) {
        require(key.isEmpty() || valid(key))
        if(read(context)==key) return
        check(context.getSharedPreferences("map-provider",Context.MODE_PRIVATE).edit().putString("thunderforest",key).commit())
    }
    fun source(context: Context,id: String?): MapSource = MapSource.decode(id).let {
        if(it==MapSource.OPENCYCLEMAP && read(context).isEmpty()) MapSource.CYCLOSM else it
    }
}
