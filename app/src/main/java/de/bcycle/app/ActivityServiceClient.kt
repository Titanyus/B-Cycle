package de.bcycle.app

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class ActivityServiceFailure(val retryable:Boolean,val problem:Int):IOException()
internal data class UploadProgress(val jobId:String,val activityId:String?=null)
internal class ActivityHttp(private val cancelled:()->Boolean={false},private val http:OkHttpClient=client) {
    fun json(request:Request,codes:Set<Int> = setOf(200)):JSONObject {
        if(cancelled()) throw IOException()
        return http.newCall(request).execute().use { response ->
            if(response.code !in codes) throw ActivityServiceFailure(response.code==408 || response.code==429 || response.code>=500,
                if(response.code==401 || response.code==403) R.string.service_auth_failed else R.string.service_upload_failed)
            val bytes=response.body?.byteStream()?.use { it.readNBytes(1024*1024+1) } ?: throw IOException()
            if(bytes.size>1024*1024) throw ActivityServiceFailure(false,R.string.service_response_failed)
            try { JSONObject(String(bytes,Charsets.UTF_8)) } catch(_:org.json.JSONException) { throw ActivityServiceFailure(false,R.string.service_response_failed) }
        }
    }
    fun file(file:File)=object:RequestBody() {
        override fun contentType()="application/octet-stream".toMediaType()
        override fun contentLength()=file.length()
        override fun writeTo(sink:BufferedSink) { file.inputStream().use { input ->
            val buffer=ByteArray(16384)
            while(true) { if(cancelled()) throw IOException();val n=input.read(buffer);if(n<0) break;sink.write(buffer,0,n) }
        } }
    }
    companion object {
        private val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(15,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS).callTimeout(120,TimeUnit.SECONDS).build()
    }
}
internal class DreeveClient(config:ActivityServiceConfig,private val key:String,cancelled:()->Boolean={false},base:HttpUrl?=null) {
    private val base=base ?: dreeveUrl(config.address)
    private val http=ActivityHttp(cancelled)
    private fun request(path:String)=Request.Builder().url(base.newBuilder().addPathSegments("api/v1/$path").build())
        .header("Authorization","Bearer $key").header("Accept","application/json").header("User-Agent","B-Cycle/${AppVersion.name}")
    fun test() {
        val status=http.json(request("status").build())
        if(status.optString("app")!="dreeve") throw ActivityServiceFailure(false,R.string.service_response_failed)
        if(status.opt("canUpload")!=true) throw ActivityServiceFailure(false,R.string.dreeve_import_disabled)
    }
    fun step(file:File,filename:String,jobId:String):UploadProgress {
        // Recover a lost 202 response by looking up this durable, unique attempt filename first.
        val query=request("file-imports").build().url.newBuilder().addQueryParameter("filters[filename]",filename)
            .addQueryParameter("pagination[size]","100").build()
        val entries=http.json(request("file-imports").url(query).build()).optJSONArray("fileImports")
            ?: throw ActivityServiceFailure(false,R.string.service_response_failed)
        val matches=(0 until entries.length()).map { entries.getJSONObject(it) }.filter { it.optString("filename")==filename && it.optString("source")=="fitFile" }
        val ids=matches.filter { it.optString("status")=="success" }.mapNotNull { it.validId("activityId") }.distinct()
        if(ids.size>1) throw ActivityServiceFailure(false,R.string.service_response_failed)
        if(ids.size==1) return UploadProgress("accepted",ids.single())
        if(matches.any { it.optString("status")=="queued" }) return UploadProgress("accepted")
        if(matches.any { it.optString("status")=="skipped" }) throw ActivityServiceFailure(false,R.string.service_duplicate)
        if(matches.isNotEmpty()) throw ActivityServiceFailure(false,R.string.service_upload_failed)
        if(jobId.isNotEmpty()) return UploadProgress(jobId)
        val result=http.json(request("activity/upload").post(MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file",filename,http.file(file)).build()).build(),setOf(202))
        if(result.optString("status")!="queued") throw ActivityServiceFailure(false,R.string.service_response_failed)
        return UploadProgress("accepted")
    }
}
internal fun JSONObject.validId(key:String)=opt(key)?.takeIf { it!=JSONObject.NULL }?.toString()
    ?.takeIf { it.isNotBlank() && it!="null" && it.length<=512 && it.none(Char::isISOControl) }

internal class StravaClient(private val settings:ActivityServiceSettings,private val cancelled:()->Boolean={false},
    private val base:HttpUrl="https://www.strava.com/".toHttpUrl()) {
    private val http=ActivityHttp(cancelled)
    fun authorize(authorization:StravaAuthorization) {
        val config=settings.read();val secret=settings.credentials().getString("secret")
        check(config.clientId==authorization.clientId && settings.authorizationGeneration()==authorization.generation)
        val response=token(config.clientId,secret,"authorization_code","code",authorization.code)
        settings.tokens(config.clientId,secret,response,true,expectedGeneration=authorization.generation)
    }
    private fun token(clientId:String,secret:String,grant:String,key:String,value:String)=http.json(Request.Builder()
        .url(base.newBuilder().addPathSegments("oauth/token").build()).post(FormBody.Builder()
            .add("client_id",clientId).add("client_secret",secret).add("grant_type",grant).add(key,value).build()).build())
    private fun access():String=synchronized(tokenLock) {
        val config=settings.read();val credentials=settings.credentials()
        require(config.account.isNotBlank())
        if(credentials.optLong("expires")>System.currentTimeMillis()/1000+60) return@synchronized credentials.getString("access")
        val secret=credentials.getString("secret");val refresh=credentials.getString("refresh")
        val response=token(config.clientId,secret,"refresh_token","refresh_token",refresh)
        settings.tokens(config.clientId,secret,response,false,refresh)
        response.getString("access_token")
    }
    private fun request(path:String)=Request.Builder().url(base.newBuilder().addPathSegments("api/v3/$path").build())
        .header("Authorization","Bearer ${access()}").header("Accept","application/json").header("User-Agent","B-Cycle/${AppVersion.name}")
    fun test() {
        val expected=settings.read().account
        if(http.json(request("athlete").build()).validId("id")!=expected) throw ActivityServiceFailure(false,R.string.service_auth_failed)
    }
    fun step(file:File,filename:String,externalId:String,jobId:String):UploadProgress {
        val request=if(jobId.isNotEmpty()) {
            require(jobId.matches(Regex("[0-9]{1,20}")))
            request("uploads/$jobId").build()
        } else request("uploads").post(MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("data_type","fit").addFormDataPart("external_id",externalId)
            .addFormDataPart("file",filename,http.file(file)).build()).build()
        val result=http.json(request,if(jobId.isEmpty()) setOf(201) else setOf(200))
        if(result.validId("error")!=null) throw ActivityServiceFailure(false,R.string.service_upload_failed)
        val id=result.validId("id_str") ?: result.validId("id") ?: throw ActivityServiceFailure(false,R.string.service_response_failed)
        if(!id.matches(Regex("[0-9]{1,20}")) || (jobId.isNotEmpty() && jobId!=id)) throw ActivityServiceFailure(false,R.string.service_response_failed)
        val activity=result.validId("activity_id")
        if(activity!=null && !activity.matches(Regex("[0-9]{1,20}"))) throw ActivityServiceFailure(false,R.string.service_response_failed)
        return UploadProgress(id,activity)
    }
    companion object { private val tokenLock=Any() }
}
