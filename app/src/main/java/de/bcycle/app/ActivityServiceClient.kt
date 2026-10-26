package de.bcycle.app

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class ActivityServiceFailure(val retryable:Boolean,val problem:Int):IOException()
internal data class UploadProgress(val jobId:String,val activityId:String?=null)
internal class ActivityHttp(private val cancelled:()->Boolean={false},private val http:OkHttpClient=client) {
    fun json(request:Request,codes:Set<Int> = setOf(200)):JSONObject=readJson(request,codes,false)!!
    fun optionalJson(request:Request):JSONObject?=readJson(request,setOf(200),true)
    private fun readJson(request:Request,codes:Set<Int>,missingAllowed:Boolean):JSONObject? {
        if(cancelled()) throw IOException()
        return http.newCall(request).execute().use { response ->
            if(missingAllowed && response.code==404) return@use null
            if(response.code !in codes) throw ActivityServiceFailure(response.code==408 || response.code==429 || response.code>=500,
                ServiceProblems.http(response.code)?.text ?: R.string.service_upload_failed)
            val bytes=response.body?.byteStream()?.use { it.readNBytes(1024*1024+1) } ?: throw IOException()
            if(bytes.size>1024*1024) throw ActivityServiceFailure(false,R.string.service_response_failed)
            try { JSONObject(String(bytes,Charsets.UTF_8)) } catch(_:org.json.JSONException) { throw ActivityServiceFailure(false,R.string.service_response_failed) }
        }
    }
    fun download(request:Request,file:File,maximum:Long) {
        if(cancelled()) throw IOException()
        http.newCall(request).execute().use { response ->
            if(response.code!=200) throw ActivityServiceFailure(response.code==408 || response.code==429 || response.code>=500,
                ServiceProblems.http(response.code)?.text ?: R.string.dreeve_import_download_failed)
            val body=response.body ?: throw IOException()
            if(body.contentLength()>maximum) throw IOException()
            body.byteStream().use { input -> file.outputStream().use { output ->
                val buffer=ByteArray(16384);var size=0L
                while(true) {
                    if(cancelled()) throw IOException()
                    val n=input.read(buffer);if(n<0) break
                    size+=n;if(size>maximum) throw IOException()
                    output.write(buffer,0,n)
                }
            } }
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
        fun brief(cancelled:()->Boolean)=ActivityHttp(cancelled,client.newBuilder()
            .connectTimeout(5,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS).callTimeout(15,TimeUnit.SECONDS).build())
    }
}
internal class DreeveClient(config:ActivityServiceConfig,private val key:String,cancelled:()->Boolean={false},base:HttpUrl?=null,transport:ActivityHttp?=null) {
    private val base=base ?: dreeveUrl(config.address)
    private val http=transport ?: ActivityHttp(cancelled)
    private fun request(path:String)=Request.Builder().url(base.newBuilder().addPathSegments("api/v1/$path").build())
        .header("Authorization","Bearer $key").header("Accept","application/json").header("User-Agent","B-Cycle/${AppVersion.name}")
    fun test() {
        val status=http.json(request("status").build())
        if(status.optString("app")!="dreeve") throw ActivityServiceFailure(false,R.string.service_response_failed)
        if(status.opt("canUpload")!=true) throw ActivityServiceFailure(false,R.string.dreeve_import_disabled)
    }
    fun activities(page:Int=1):DreeveActivityPage {
        require(page in 1..10000)
        val url=request("activities").build().url.newBuilder()
            .addQueryParameter("pagination[page]",page.toString()).addQueryParameter("pagination[size]","50")
            .addQueryParameter("filters[sportType]","Ride,VirtualRide,EBikeRide,MountainBikeRide,GravelRide,EMountainBikeRide").build()
        return DreeveActivityPage.decode(http.json(request("activities").url(url).build()),page)
    }
    fun allActivities(startPage:Int=1,received:(DreeveActivityPage)->Unit) {
        var page=startPage
        do { val data=activities(page);received(data);page=data.page+1 } while(page<=data.totalPages)
    }
    fun gears():Map<String,String> {
        val array=http.json(request("gear").build()).getJSONArray("gear")
        return (0 until array.length()).mapNotNull { i -> val row=array.getJSONObject(i)
            row.validId("id")?.let { it to row.getString("name").take(120) } }.toMap()
    }
    fun maintenance():List<DreeveMaintenanceTask> {
        val response=http.optionalJson(request("gear/maintenance-tasks").build())
            ?: throw ActivityServiceFailure(false,R.string.maintenance_api_unavailable)
        return DreeveMaintenanceTask.decode(response)
    }
    fun activityExists(activityId:String):Boolean {
        require(activityId.isNotBlank() && activityId.length<=512 && activityId.none(Char::isISOControl))
        val url=request("activities").build().url.newBuilder().addPathSegment(activityId).build()
        val result=http.optionalJson(request("activities").url(url).build()) ?: return false
        if(result.validId("id")!=activityId) throw ActivityServiceFailure(false,R.string.service_response_failed)
        return true
    }
    fun activityName(activityId:String):String {
        val url=request("activities").build().url.newBuilder().addPathSegment(activityId).build()
        return http.json(request("activities").url(url).build()).getString("name").trim().take(120).also { require(it.isNotBlank()) }
    }
    fun rename(activityId:String,name:String) {
        val url=request("activities").build().url.newBuilder().addPathSegment(activityId).build()
        val body=JSONObject().put("name",name).toString().toRequestBody("application/json".toMediaType())
        http.json(request("activities").url(url).patch(body).build())
    }
    fun downloadGpx(activityId:String,file:File) {
        require(activityId.isNotBlank() && activityId.length<=512 && activityId.none(Char::isISOControl))
        val url=request("activities").build().url.newBuilder().addPathSegment(activityId).addPathSegment("gpx").build()
        http.download(request("activities").url(url).header("Accept","application/gpx+xml").build(),file,GpxImport.MAX_BYTES)
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
    fun rename(activityId:String,name:String) {
        require(activityId.matches(Regex("[0-9]{1,20}")))
        http.json(request("activities/$activityId").put(FormBody.Builder().add("name",name).build()).build())
    }
    companion object { private val tokenLock=Any() }
}
