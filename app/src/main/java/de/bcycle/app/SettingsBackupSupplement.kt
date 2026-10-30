package de.bcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Explicit additional public settings. No credentials, upload jobs or OS grants. */
internal class SettingsBackupSupplement(private val context:Context) {
    private fun prefs(name:String)=context.getSharedPreferences(name,0)
    private val profileKeys=setOf("slot","name","type","category","indoorType","sources","wheelMm","autoConnect","autoStartRecording","gpsEnabled","bikeEnabled","heartEnabled",
        "bikeAddress","bikeName","heartAddress","heartName","trainerAddress","trainerName","trainerEnabled","powerAddress","powerName","powerEnabled","shiftEnabled","shiftDevices","shiftBatteryWarning","indoorPowerTenths","indoorSpeedTenths","fields","metadata","speed","distance")
    private val files=mapOf(
        BackupSection.RECORDING to mapOf("bike" to setOf(RideNamingSettings.KEY),"route-export" to setOf("tree")),
        BackupSection.DESIGN to mapOf("ride-backup" to setOf("enabled","target"),"message-notifications" to setOf("enabled","messengerOnly","packages","packageOrder","position","durationSeconds")),
        BackupSection.CLOUD to mapOf("intervals-settings" to setOf("enabled","athlete","wifi"),
            "activity-service-dreeve" to setOf("enabled","wifi","address"),
            "activity-service-strava" to setOf("enabled","wifi","clientId")),
        BackupSection.PROFILE to mapOf("bike" to setOf(BikeProfileStore.KEY,BikeProfileStore.ACTIVE,"nextBikeSlot",BikeIdentities.KEY,BikeIdentities.RETAINED_TYPES,BikeUiConfigurationStore.KEY),
            HrvSettings.STORE to setOf("restSeconds","measureSeconds","showOnFirstStart","vibrateAtEnd","soundAtEnd"),HrvQuestionSettings.STORE to setOf(HrvQuestionSettings.KEY),"dreeve-bike-mappings" to setOf("entries")))
    fun export(section:BackupSection):JSONObject {
        val profiles=if(section==BackupSection.PROFILE) BikeProfileStore(context,prefs("bike"),initialize=false) else null
        return JSONObject().apply {
            files[section].orEmpty().forEach { (file,keys) ->
                val source=prefs(file).all
                put(file,JSONObject().apply { keys.forEach { key -> source[key]?.let { value ->
                    if(key==BikeProfileStore.KEY) {
                        val rows=JSONArray(profiles!!.backupJson())
                        put(key,JSONArray().apply { repeat(rows.length()) { i ->
                            val row=rows.getJSONObject(i)
                            put(JSONObject().apply { profileKeys.forEach { if(row.has(it)) put(it,row.get(it)) } })
                        } }.toString())
                    } else if(key==BikeUiConfigurationStore.KEY) put(key,profiles!!.ui.backupBank(profiles.activeSlot))
                    else if(file==HrvQuestionSettings.STORE && key==HrvQuestionSettings.KEY) put(key,HrvQuestionSettings(context).read().json().toString())
                    else put(key,if(value is Set<*>) JSONArray(value.toList()) else value)
                } } })
            }
        }.also { plan(section,it) }
    }
    fun plan(section:BackupSection,json:JSONObject):Map<String,Map<String,Any?>> {
        require(json.keys().asSequence().all { it in files[section].orEmpty() })
        return json.keys().asSequence().associateWith { file ->
            val row=json.getJSONObject(file);val allowed=files.getValue(section).getValue(file)
            require(row.keys().asSequence().all { it in allowed })
            val values=allowed.associateWith<String,Any?> { null }.toMutableMap()
            row.keys().forEach { key ->
                val value=row.get(key)
                values[key]=when(key) {
                    RideNamingSettings.KEY -> (value as String).also { RideNaming.decode(JSONObject(it)) }
                    "enabled","messengerOnly","wifi","showOnFirstStart","vibrateAtEnd","soundAtEnd" -> (value as? Boolean) ?: error("boolean")
                    "packageOrder" -> (value as String).also { text ->
                        require(text.length<=128000)
                        val items=text.split(',').filter { it.isNotEmpty() }
                        require(items.size<=500 && items.distinct().size==items.size && items.all { it.length in 1..255 && it.matches(Regex("[A-Za-z0-9_.]+")) })
                    }
                    "packages" -> (value as JSONArray).let { a ->
                        require(a.length()<=500)
                        (0 until a.length()).map { a.getString(it).also { p -> require(p.length in 1..255 && p.matches(Regex("[A-Za-z0-9_.]+"))) } }.toSet()
                    }
                    "tree" -> (value as String).also { if(it.isNotEmpty()) { val uri=android.net.Uri.parse(it);require(it.length<=8192 && uri.scheme=="content" && !uri.authority.isNullOrBlank() && uri.path.orEmpty().startsWith("/tree/")) } }
                    "target" -> (value as String).also { RideBackupDestination.valueOf(it) }
                    "entries" -> (value as String).also { text ->
                        val all=BackupJson.objectValue(text.toByteArray(Charsets.UTF_8),65536);require(all.length()<=100)
                        all.keys().forEach { account ->
                            dreeveUrl(account);val rows=all.getJSONObject(account);require(rows.length()<=10000)
                            rows.keys().forEach { gear -> require(gear.length in 1..512 && gear.none(Char::isISOControl));require(integer(rows,gear)>=-1) }
                        }
                    }
                    "position" -> (value as String).also { require(MessagePosition.entries.any { p -> p.name==it }) }
                    "durationSeconds" -> integer(row,key).also { require(it in 5..180) }
                    HrvQuestionSettings.KEY -> HrvQuestionConfig.decode(JSONObject(value as String)).json().toString()
                    "restSeconds" -> integer(row,key).also { require(it in 15..60 && it%15==0) }
                    "measureSeconds" -> integer(row,key).also { require(it in 60..300 && it%15==0) }
                    BikeProfileStore.ACTIVE,"nextBikeSlot" -> integer(row,key).also { require(it in 0 until Int.MAX_VALUE) }
                    BikeProfileStore.KEY -> (value as String).also(::validateProfiles)
                    BikeIdentities.RETAINED_TYPES -> (value as String).also { text ->
                        val types=BackupJson.objectValue(text.toByteArray(Charsets.UTF_8),65536);require(types.length()<=10000)
                        types.keys().forEach { id -> require(BikeIdentities.valid(id));BikeType.valueOf(types.getString(id)) }
                    }
                    BikeIdentities.KEY -> (value as String).also { text ->
                        val ids=BackupJson.objectValue(text.toByteArray(Charsets.UTF_8),65536);require(ids.length()<=10000)
                        val all=ids.keys().asSequence().map { slot ->
                            require(slot.toIntOrNull()?.let { it>=0 }==true)
                            ids.getString(slot).also { require(BikeIdentities.valid(it)) }
                        }.toList();require(all.distinct().size==all.size)
                    }
                    BikeUiConfigurationStore.KEY -> (value as String).also { text ->
                        val bank=BackupJson.objectValue(text.toByteArray(Charsets.UTF_8),65536);require(bank.length()<=BikeProfileStore.MAX_COUNT)
                        bank.keys().forEach { slot ->
                            require(slot.toIntOrNull()?.let { it>=0 }==true)
                            val saved=bank.getJSONObject(slot)
                            saved.keys().forEach { k ->
                                require(BikeUiConfigurationScope.designKey(k)||BikeUiConfigurationScope.mapKey(k))
                                val item=saved.getJSONObject(k);require(item.getString("type") in setOf("string","boolean","int","long","float","set"))
                                if(k==MapRouteSlots.KEY) { require(item.getString("type")=="string");MapRouteSlots.parse(JSONObject(item.getString("value"))) }
                                if(k==MapRouteStyles.key) { require(item.getString("type")=="string");MapRouteStyles.validate(JSONObject(item.getString("value"))) }
                                if(k==DashboardStatusConfig.key) { require(item.getString("type")=="string");DashboardStatusConfig.parse(item.getString("value")) }
                                if(k==RideNamingSettings.KEY) { require(item.getString("type")=="string");RideNaming.decode(JSONObject(item.getString("value"))) }
                                if((0 until DashboardPages.MAX).any { GraphOverlayConfig.windowKey(it)==k }) {
                                    require(item.getString("type")=="string")
                                    val windows=item.getString("value").split('|')
                                    require(windows.size in 8..DashboardLayout.FIELD_COUNT && windows.all { name -> GraphWindow.entries.any { it.name==name } })
                                }
                                require(item.toString().length<=65536)
                            }
                        }
                    }
                    "address" -> (value as String).also { if(it.isNotBlank()) dreeveUrl(it) }
                    "clientId" -> (value as String).also { require(it.isEmpty()||it.matches(Regex("[0-9]{1,20}"))) }
                    "athlete" -> (value as String).also { require(it.isEmpty()||it.matches(Regex("i?[0-9]{1,15}"))) }
                    else -> error("unknown setting")
                }
            }
            if(file=="bike" && values[BikeProfileStore.KEY] is String) {
                val rows=JSONArray(values.getValue(BikeProfileStore.KEY) as String)
                val active=(values[BikeProfileStore.ACTIVE] as? Int) ?: rows.getJSONObject(0).getInt("slot")
                val profile=(0 until rows.length()).map(rows::getJSONObject).single { it.getInt("slot")==active }
                values[BikeProfileStore.ACTIVE]=active
                mapOf("bikeAddress" to "address","bikeName" to "name","heartAddress" to "heartAddress","heartName" to "heartName").forEach { (from,to) ->
                    values[to]=profile.optString(from).takeIf { it.isNotBlank() }
                }
                listOf("gpsEnabled","bikeEnabled","heartEnabled").forEach { values[it]=profile.getBoolean(it) }
                val fields=profile.getJSONArray("fields")
                values["recordFields"]=(0 until fields.length()).map(fields::getString).toSet()
                values["recordMetadata"]=profile.optJSONArray("metadata")?.let { array -> (0 until array.length()).map { array.getString(it) }.toSet() } ?: FitMetadataField.defaults.map { it.name }.toSet()
                values["recordSpeed"]=profile.getString("speed");values["recordDistance"]=profile.getString("distance");values[RecordingSources.KEY]=profile.optString("sources").takeIf { it.isNotEmpty() }
                // Restore the active projection too, not just the dormant per-profile bank.
                (values[BikeUiConfigurationStore.KEY] as? String)?.let { text ->
                    JSONObject(text).optJSONObject(active.toString())?.let { saved ->
                        prefs(file).all.keys.filter { BikeUiConfigurationScope.designKey(it)||BikeUiConfigurationScope.mapKey(it) }.forEach { values[it]=null }
                        saved.keys().forEach { key ->
                        val item=saved.getJSONObject(key)
                        values[key]=when(item.getString("type")) {
                            "string"->item.getString("value");"boolean"->item.getBoolean("value");"int"->item.getInt("value")
                            "long"->item.getLong("value");"float"->item.getDouble("value").toFloat()
                            "set"->item.getJSONArray("value").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                            else->error("setting type")
                        }
                    } }
                }
            }
            if(file in setOf("intervals-settings","activity-service-dreeve","activity-service-strava")) {
                val old=prefs(file)
                val identity=when(file) { "intervals-settings"->"athlete";"activity-service-dreeve"->"address";else->"clientId" }
                val changed=(values[identity] as? String).orEmpty()!=old.getString(identity,"").orEmpty()
                if(changed) {
                    values["password"]=null
                    if(file=="activity-service-strava") {
                        values["account"]=null;values["oauthState"]=null;values["oauthAt"]=null;values["oauthClient"]=null
                    }
                }
                if(changed || !old.contains("password") || old.getBoolean("needsPassword",false))
                    values["enabled"]=false
                // A copied Strava client ID does not restore an OAuth grant.
                if(file=="activity-service-strava" && old.getString("account","").isNullOrBlank()) values["enabled"]=false
            }
            values.toMap()
        }
    }
    private fun integer(row:JSONObject,key:String):Int {
        val value=row.get(key) as? Number ?: error("integer required")
        require(value.toDouble().isFinite() && value.toDouble()==value.toInt().toDouble())
        return value.toInt()
    }
    private fun validateProfiles(text:String) {
        require(text.length<=65536)
        val rows=JSONArray(BackupJson.text(text.toByteArray(Charsets.UTF_8),65536));require(rows.length() in 1..BikeProfileStore.MAX_COUNT)
        val slots=mutableSetOf<Int>()
        repeat(rows.length()) { i ->
            val p=rows.getJSONObject(i)
            require(p.keys().asSequence().all { it in profileKeys })
            require(integer(p,"slot") in 0 until Int.MAX_VALUE && slots.add(integer(p,"slot")))
            require(p.getString("name").let { it.isNotBlank() && it.length<=60 && it.none(Char::isISOControl) })
            BikeType.valueOf(p.getString("type"));BikeCategory.valueOf(p.getString("category"))
            if(p.has("indoorType")) IndoorBikeType.valueOf(p.getString("indoorType"))
            require(integer(p,"wheelMm") in 500..4000)
            listOf("autoConnect","gpsEnabled","bikeEnabled","heartEnabled").forEach { require(p.get(it) is Boolean) }
            if(p.has("sources") && p.getString("sources").isNotEmpty()) RecordingSources.decode(p.getString("sources"))
            if(p.has("autoStartRecording")) require(p.get("autoStartRecording") is Boolean)
            if(p.has("powerEnabled")) require(p.get("powerEnabled") is Boolean)
            if(p.has("shiftBatteryWarning")) require(p.get("shiftBatteryWarning") is Boolean)
            if(p.has("shiftEnabled")) require(p.get("shiftEnabled") is Boolean)
            if(p.has("shiftDevices")) AxsDevice.decode(p.getJSONArray("shiftDevices"))
            listOf("indoorPowerTenths","indoorSpeedTenths").forEach { if(p.has(it)) require(integer(p,it) in 0..30) }
            listOf("bikeAddress","heartAddress","trainerAddress","powerAddress").forEach { if(p.has(it)) require(p.getString(it).isEmpty() || p.getString(it).matches(Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}"))) }
            listOf("bikeName","heartName","trainerName","powerName").forEach { if(p.has(it)) require(p.getString(it).let { s -> s.length<=248 && s.none(Char::isISOControl) }) }
            if(p.has("metadata")) { val selected=p.getJSONArray("metadata");require(selected.length()<=FitMetadataField.entries.size)
                val names=(0 until selected.length()).map { selected.getString(it) };require(names.distinct().size==names.size && names.all { name -> FitMetadataField.entries.any { it.name==name } }) }
            val fields=p.getJSONArray("fields");require(fields.length()<=RecordingFields.options.size)
            repeat(fields.length()) { index -> require(RecordingFields.options.any { it.id==fields.getString(index) }) }
            require(p.getString("speed") in setOf("SPEED","GPS_SPEED"));require(p.getString("distance") in setOf("BIKE_DISTANCE","GPS_DISTANCE"))
        }
    }
}
