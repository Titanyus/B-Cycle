package de.bcycle.app

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class IntervalsFailure(val retryable:Boolean,val problem:Int):IOException()
/** Activity upload only. Future workout APIs belong to separate client capabilities. */
internal class IntervalsClient(private val config:IntervalsConfig,private val key:String,
    private val cancelled:()->Boolean={false},private val base:HttpUrl="https://intervals.icu/api/v1/".toHttpUrl(),
    private val http:OkHttpClient=client) {
    private fun request(path:String)=Request.Builder().url(base.newBuilder().addPathSegment("athlete").addPathSegment(config.athlete).apply {
        if(path.isNotEmpty()) addPathSegment(path)
    }.build()).header("Authorization",Credentials.basic("API_KEY",key))
        .header("User-Agent","B-Cycle/${AppVersion.name}").header("Accept","application/json")
    private fun execute(request:Request,missingAllowed:Boolean=false):String {
        require(config.athlete.matches(Regex("i?[0-9]{1,15}")) && key.isNotEmpty())
        if(cancelled()) throw IOException()
        val result=http.newCall(request).execute().use { response ->
            if(missingAllowed && response.code==404) return@use ""
            if(!response.isSuccessful) throw IntervalsFailure(response.code==408 || response.code==429 || response.code>=500,
                ServiceProblems.http(response.code)?.let { if(it==ServiceProblems.Problem.AUTH) R.string.intervals_auth_failed else it.text } ?: R.string.intervals_upload_failed)
            val bytes=response.body?.byteStream()?.use { it.readNBytes(1024*1024+1) } ?: throw IOException()
            if(bytes.size>1024*1024) throw IntervalsFailure(false,R.string.intervals_response_failed)
            String(bytes,Charsets.UTF_8)
        }
        if(cancelled()) throw IOException()
        return result
    }
    fun test() {
        val result=JSONObject(execute(request("").get().build()))
        if(result.optString("id")!=config.athlete) throw IntervalsFailure(false,R.string.intervals_response_failed)
    }
    fun activities(oldest:java.time.LocalDate,newest:java.time.LocalDate):List<IntervalsActivity> {
        val url=request("activities").build().url.newBuilder().addQueryParameter("oldest",oldest.toString())
            .addQueryParameter("newest",newest.toString()).addQueryParameter("limit","1000")
            .addQueryParameter("fields","id,type,start_date,start_date_local,timezone,elapsed_time,distance,external_id").build()
        return IntervalsActivity.decode(execute(request("activities").url(url).get().build()))
    }
    fun upload(file:File,name:String):String=upload(file,name,null,null)
    fun upload(file:File,name:String,externalId:String?,title:String?):String {
        require(file.isFile && file.extension.equals("fit",true))
        val body=object:RequestBody() {
            override fun contentType()="application/octet-stream".toMediaType()
            override fun contentLength()=file.length()
            override fun writeTo(sink:BufferedSink) { file.inputStream().use { input ->
                val buffer=ByteArray(16384)
                while(true) { if(cancelled()) throw IOException();val n=input.read(buffer);if(n<0) break;sink.write(buffer,0,n) }
            } }
        }
        val url=request("activities").build().url.newBuilder().apply {
            externalId?.let { addQueryParameter("external_id",it) };title?.let { addQueryParameter("name",it) }
        }.build()
        val data=execute(request("activities").url(url).post(MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file",name.replace(Regex("[\\r\\n\\\"]"),"_"),body).build()).build())
        val parsed=org.json.JSONTokener(data).nextValue()
        val activity=when(parsed) {
            is JSONArray -> if(parsed.length()==1) parsed.optJSONObject(0) else null
            is JSONObject -> if(parsed.has("activities")) parsed.optJSONArray("activities")?.takeIf { it.length()==1 }?.optJSONObject(0) else parsed
            else->null
        }
        val id=activity?.optString("id")?.takeIf { it.isNotBlank() && it!="null" }
        return id ?: throw IntervalsFailure(false,R.string.intervals_response_failed)
    }
    fun activityExists(activityId:String):Boolean {
        require(activityId.isNotBlank() && activityId.length<=512 && activityId.none(Char::isISOControl))
        val url=base.newBuilder().addPathSegment("activity").addPathSegment(activityId).build()
        val text=execute(request("").url(url).get().build(),missingAllowed=true)
        if(text.isEmpty()) return false
        if(JSONObject(text).optString("id")!=activityId) throw IntervalsFailure(false,R.string.intervals_response_failed)
        return true
    }
    fun rename(activityId:String,name:String) {
        require(activityId.isNotBlank() && activityId.length<=512 && activityId.none(Char::isISOControl))
        val url=base.newBuilder().addPathSegment("activity").addPathSegment(activityId).build()
        val body=JSONObject().put("name",name).toString().toRequestBody("application/json".toMediaType())
        execute(request("").url(url).put(body).build())
    }
    companion object {
        private val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(15,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS).callTimeout(120,TimeUnit.SECONDS).build()
    }
}
