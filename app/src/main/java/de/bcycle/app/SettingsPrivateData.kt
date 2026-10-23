package de.bcycle.app

import android.content.Context
import org.json.JSONObject
import javax.crypto.SecretKey

internal data class ProtectedSettingsData(val credentials:JSONObject,val photos:Map<String,ByteArray>)

/** Plaintext only in worker memory inside a password-encrypted archive. Never in public JSON/Bundle. */
internal class SettingsPrivateData(private val context:Context,private val keys:((String)->SecretKey)?=null) {
    private val stores=linkedMapOf("route-export" to "bcycle-webdav","intervals-settings" to "bcycle-intervals",
        "activity-service-dreeve" to "bcycle-dreeve","activity-service-strava" to "bcycle-strava","map-provider" to "")
    private fun section(file:String)=if(file=="map-provider") BackupSection.MAPS else BackupSection.CLOUD
    private fun prefs(file:String)=context.getSharedPreferences(file,0)
    private fun secret(file:String)=StoredSecret(prefs(file),stores.getValue(file),keys?.let { resolve -> { resolve(stores.getValue(file)) } })
    fun export(selected:Set<BackupSection>):JSONObject = JSONObject().apply {
        stores.keys.filter { section(it) in selected }.forEach { file ->
            val source=prefs(file)
            val value=try {
                if(file=="map-provider") MapProviderSettings.read(context).takeIf { it.isNotEmpty() }
                else if(source.contains("password") && !source.getBoolean("needsPassword",false)) secret(file).read().takeIf { it.isNotEmpty() } else null
            } catch(_:Exception) { throw SettingsBackupFailure(BackupProblem.CREDENTIALS) }
            put(file,JSONObject().put("value",value ?: JSONObject.NULL).apply {
                if(file=="activity-service-strava") put("account",source.getString("account","").orEmpty())
            })
        }
        validate(this,selected)
    }
    fun validate(data:JSONObject,available:Set<BackupSection>) {
        require(data.keys().asSequence().toSet()==stores.keys.filter { section(it) in available }.toSet())
        data.keys().forEach { file ->
            val row=data.getJSONObject(file)
            require(row.keys().asSequence().all { it=="value" || file=="activity-service-strava" && it=="account" })
            if(file=="activity-service-strava") require(row.getString("account").let { it.isEmpty() || it.matches(Regex("[0-9]{1,20}")) })
            require(row.has("value"));if(row.isNull("value")) return@forEach
            val text=row.get("value") as? String ?: error("credential string")
            require(text.length in 1..16384 && text.none { it=='\u0000' })
            when(file) {
                "map-provider"->require(MapProviderSettings.valid(text))
                "intervals-settings"->require(text.length<=256 && text.none { it.isWhitespace() || it.isISOControl() })
                "activity-service-dreeve","activity-service-strava"->{
                    val values=JSONObject(text);require(values.keys().asSequence().all { it in setOf("secret","access","refresh","expires") })
                    listOf("secret","access","refresh").forEach { key -> if(values.has(key)) require(values.get(key) is String && values.getString(key).length<=4096) }
                    if(values.has("expires")) require(values.get("expires") is Number && values.getLong("expires")>=0)
                }
            }
        }
    }
    fun plan(doc:SettingsBackupDocument,selected:Set<BackupSection>,base:SettingsRestorePlan):SettingsRestorePlan {
        val protected=checkNotNull(doc.protectedData);validate(protected.credentials,doc.sections.keys)
        val route=base.route.toMutableMap();val extra=base.additional.mapValues { it.value.toMutableMap() }.toMutableMap()
        var missing=false
        protected.credentials.keys().forEach { file ->
            if(section(file) !in selected) return@forEach
            val row=protected.credentials.getJSONObject(file);val value=if(row.isNull("value")) null else row.getString("value")
            val target=if(file=="route-export") route else extra.getOrPut(file) { mutableMapOf() }
            if(file=="map-provider") { target["thunderforest"]=value;return@forEach }
            target["password"]=value?.let { secret(file).encode(it) };target["needsPassword"]=null
            val config=if(file=="route-export") doc.sections.getValue(BackupSection.CLOUD)
                else doc.sections.getValue(BackupSection.CLOUD).getJSONObject("additional").getJSONObject(file)
            val desired=config.optBoolean("enabled",false)
            var usable=!value.isNullOrEmpty()
            if(file=="activity-service-strava") {
                val account=row.optString("account")
                val credentials=value?.let(::JSONObject)
                usable=usable && account.isNotEmpty() && !credentials?.optString("refresh").isNullOrEmpty() && !credentials?.optString("secret").isNullOrEmpty()
                target["account"]=account;target["oauthState"]=null;target["oauthAt"]=null;target["oauthClient"]=null
                target["oauthGeneration"]=prefs(file).getLong("oauthGeneration",0)+1
            }
            if(file=="activity-service-dreeve") usable=usable && !value?.let(::JSONObject)?.optString("secret").isNullOrEmpty()
            target[if(file=="route-export") "webdav" else "enabled"]=desired && usable
            if(desired && !usable) { missing=true;if(file=="route-export") target["needsPassword"]=true }
        }
        val photos=if(BackupSection.PROFILE in selected) {
            val ids=doc.sections.getValue(BackupSection.PROFILE).optJSONObject("additional")?.optJSONObject("bike")?.optString(BikeIdentities.KEY)
                ?.takeIf { it.isNotEmpty() }?.let(::JSONObject)
            val profiles=doc.sections.getValue(BackupSection.PROFILE).optJSONObject("additional")?.optJSONObject("bike")?.optString(BikeProfileStore.KEY)
                ?.takeIf { it.isNotEmpty() }?.let { org.json.JSONArray(it) }
            val expected=if(ids!=null && profiles!=null) (0 until profiles.length()).map { ids.getString(profiles.getJSONObject(it).getInt("slot").toString()) }.toSet() else emptySet()
            require(protected.photos.keys.all { it in expected })
            expected.associateWith { protected.photos[it] }
        } else emptyMap()
        return base.copy(route=route,additional=extra,photos=photos,cloudNeedsPassword=if(BackupSection.CLOUD in selected) missing else base.cloudNeedsPassword)
    }
}
