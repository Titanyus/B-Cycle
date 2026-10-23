package de.bcycle.app

import org.json.JSONObject

/** Optional standard FIT bike_profile fields, separate from measurement/developer fields. */
enum class FitMetadataField { BIKE_NAME, SPORT, SUB_SPORT, PROFILE_ID, PROFILE_INDEX, RECORDER;
    companion object {
        val defaults=entries.toSet()
        fun decode(names:Set<String>?):Set<FitMetadataField> = names?.mapNotNull { name -> entries.find { it.name==name } }?.toSet() ?: defaults
    }
}

/** Snapshot at recording start, never the currently active profile during deferred encoding. */
data class FitMetadata(val name:String,val type:BikeType,val index:Int,val fields:Set<FitMetadataField> = FitMetadataField.defaults) {
    // Garmin's id is uint8 (255 invalid); message_index uses the low 12 bits for an unflagged index.
    val profileId:Int? get()=index.takeIf { it in 0..254 }
    val profileIndex:Int? get()=index.takeIf { it in 0..4095 }
    companion object {
        fun fromSnapshot(json:String):FitMetadata {
            val row=JSONObject(json);val selected=row.optJSONArray("fitMetadata")
            return FitMetadata(row.getString("name").take(60),BikeType.valueOf(row.getString("type")),row.getInt("slot"),
                FitMetadataField.decode(selected?.let { array -> (0 until array.length()).map { array.getString(it) }.toSet() }))
        }
    }
}
