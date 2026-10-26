package de.bcycle.app

import android.content.Context
import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.SecretKey

internal enum class ActivityService(val title:Int,val icon:BicycleIcon,val job:Int,val link:RideService) {
    DREEVE(R.string.dreeve_title,TransferSymbols.dreeve,8303,RideService.DREEVE),
    STRAVA(R.string.strava_title,TransferSymbols.strava,8304,RideService.STRAVA)
}
internal data class StravaAuthorization(val code:String,val clientId:String,val generation:Long)
internal data class ActivityServiceConfig(val enabled:Boolean=false,val wifiOnly:Boolean=true,val address:String="",val clientId:String="",val account:String="") {
    fun identity(service:ActivityService)=if(service==ActivityService.DREEVE) (if(address.isBlank()) "" else dreeveUrl(address).toString()) else account
}
internal fun dreeveUrl(address:String)= (address.trim().trimEnd('/')+"/").toHttpUrl().also {
    require(it.isHttps && it.username.isEmpty() && it.password.isEmpty() && it.query==null && it.fragment==null)
}
/** Personal API credentials only. No shared client secret is bundled with the app. */
internal class ActivityServiceSettings(c:Context,val service:ActivityService,key:(()->SecretKey)?=null) {
    private val prefs=c.getSharedPreferences("activity-service-${service.name.lowercase()}",0)
    private val secret=StoredSecret(prefs,"bcycle-${service.name.lowercase()}",key)
    fun read()=synchronized(lock) { ActivityServiceConfig(prefs.getBoolean("enabled",false),prefs.getBoolean("wifi",true),
        prefs.getString("address","")!!,prefs.getString("clientId","")!!,prefs.getString("account","")!!) }
    fun credentials()=synchronized(lock) { secret.read().takeIf { it.isNotEmpty() }?.let(::JSONObject) ?: JSONObject() }
    fun hasSecret()=prefs.contains("password")
    fun dreeveImportConfigured()=service==ActivityService.DREEVE && runCatching {
        dreeveUrl(read().address)
        credentials().optString("secret").isNotBlank()
    }.getOrDefault(false)
    fun connected()=service==ActivityService.STRAVA && read().account.isNotBlank() && credentials().optString("refresh").isNotBlank()
    fun write(config:ActivityServiceConfig,newSecret:String?=null)=synchronized(lock) {
        val old=read();val data=credentials()
        val changed=service==ActivityService.STRAVA && (old.clientId!=config.clientId || (newSecret!=null && newSecret!=data.optString("secret")))
        if(service==ActivityService.DREEVE) {
            if(config.address.isNotEmpty()) dreeveUrl(config.address)
            require(newSecret==null || newSecret.matches(Regex("drv_[a-fA-F0-9]{64}")))
        } else {
            require(config.clientId.isEmpty() || config.clientId.matches(Regex("[0-9]{1,20}")))
            require(newSecret==null || (newSecret.length in 1..256 && newSecret.none { it.isWhitespace() || it.isISOControl() }))
            if(changed) {
                data.remove("access");data.remove("refresh");data.remove("expires")
            }
        }
        if(newSecret!=null) data.put("secret",newSecret)
        val account=if(service==ActivityService.STRAVA && data.optString("refresh").isEmpty()) "" else old.account
        require(!config.enabled || if(service==ActivityService.DREEVE) config.address.isNotBlank() && data.optString("secret").isNotBlank()
            else config.clientId.isNotBlank() && account.isNotBlank())
        val edit=prefs.edit().putBoolean("enabled",config.enabled).putBoolean("wifi",config.wifiOnly)
            .putString("address",config.address.trim()).putString("clientId",config.clientId).putString("account",account)
        if(changed) edit.remove("oauthState").putLong("oauthGeneration",authorizationGeneration()+1)
        if(newSecret!=null || data.toString()!=credentials().toString()) edit.putString("password",secret.encode(data.toString()))
        check(edit.commit())
    }
    fun authorizationUrl():Uri=synchronized(lock) {
        val config=read();require(config.clientId.isNotEmpty() && credentials().optString("secret").isNotEmpty())
        val state=ByteArray(32).apply { SecureRandom().nextBytes(this) }.joinToString("") { "%02x".format(it) }
        check(prefs.edit().putLong("oauthGeneration",authorizationGeneration()+1).putString("oauthState",state).putLong("oauthAt",System.currentTimeMillis()).putString("oauthClient",config.clientId).commit())
        Uri.parse("https://www.strava.com/oauth/mobile/authorize").buildUpon()
            .appendQueryParameter("client_id",config.clientId).appendQueryParameter("redirect_uri",REDIRECT)
            .appendQueryParameter("response_type","code").appendQueryParameter("approval_prompt","force")
            .appendQueryParameter("scope","activity:write").appendQueryParameter("state",state).build()
    }
    fun authorizationGeneration()=prefs.getLong("oauthGeneration",0)
    fun consumeCallback(uri:Uri):StravaAuthorization=synchronized(lock) {
        require(uri.scheme=="bcycle" && uri.host=="localhost" && uri.path=="/strava")
        val state=prefs.getString("oauthState",null)
        val valid=state!=null && uri.getQueryParameter("state")==state && prefs.getString("oauthClient",null)==read().clientId &&
            System.currentTimeMillis()-prefs.getLong("oauthAt",0) in 0..600000
        check(prefs.edit().remove("oauthState").remove("oauthAt").remove("oauthClient").commit())
        require(valid && uri.getQueryParameter("error")==null && uri.getQueryParameter("scope").orEmpty().split(',', ' ').contains("activity:write"))
        val code=uri.getQueryParameter("code")?.takeIf { it.isNotBlank() && it.length<=2048 } ?: throw IllegalArgumentException()
        StravaAuthorization(code,read().clientId,authorizationGeneration())
    }
    fun tokens(clientId:String,clientSecret:String,response:JSONObject,initial:Boolean,expectedRefresh:String?=null,expectedGeneration:Long?=null)=synchronized(lock) {
        val config=read();val data=credentials()
        check(config.clientId==clientId && data.optString("secret")==clientSecret)
        check(initial || data.optString("refresh")==expectedRefresh)
        check(expectedGeneration==null || authorizationGeneration()==expectedGeneration)
        val access=response.getString("access_token");val refresh=response.getString("refresh_token");val expires=response.getLong("expires_at")
        require(access.isNotBlank() && refresh.isNotBlank() && expires>System.currentTimeMillis()/1000)
        val account=if(initial) response.getJSONObject("athlete").get("id").toString() else config.account
        require(account.matches(Regex("[0-9]{1,20}")))
        data.put("access",access).put("refresh",refresh).put("expires",expires)
        check(prefs.edit().putString("account",account).putString("password",secret.encode(data.toString())).commit())
    }
    companion object {
        internal val lock=Any()
        const val REDIRECT="bcycle://localhost/strava"
    }
}
