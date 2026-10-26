package de.bcycle.app

import org.json.JSONObject

/** Never choose a file by filename alone or attach another activity returned by an old server. */
internal data class DreeveOriginalFits(val ids:List<String>,val totalPages:Int,val activityIds:Set<String> = emptySet()) {
    companion object {
        fun decode(json:JSONObject,requested:Int,activityId:String?=null):DreeveOriginalFits=try {
            val pagination=json.getJSONObject("pagination")
            val page=pagination.getInt("page");val size=pagination.getInt("size");val pages=pagination.getInt("totalPages")
            val rows=json.getJSONArray("fileImports")
            require(page==requested && size in 1..100 && pages in 0..10000 && page<=maxOf(pages,1) && rows.length()<=size)
            val activityIds=mutableSetOf<String>()
            val ids=(0 until rows.length()).mapNotNull { i ->
                val row=rows.getJSONObject(i)
                if(row.has("hasFile")) require(row.get("hasFile") is Boolean)
                if(activityId!=null && row.optString("activityId")!=activityId || row.optString("source")!="fitFile" || row.optString("status")!="success" || row.opt("hasFile")!=true) null
                else {
                    val owner=row.validId("activityId") ?: return@mapNotNull null
                    activityIds.add(owner)
                    row.validId("id") ?: throw IllegalArgumentException()
                }
            }
            require(ids.distinct().size==ids.size)
            val legacy=rows.length()>0 && (0 until rows.length()).none { rows.getJSONObject(it).has("hasFile") }
            DreeveOriginalFits(ids,if(legacy) 0 else pages,activityIds)
        } catch(_:Exception) { throw ActivityServiceFailure(false,R.string.service_response_failed) }
    }
}
