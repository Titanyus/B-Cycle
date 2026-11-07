package de.bcycle.app

import com.garmin.fit.*
import java.io.File

/** Export-only feedback overlay. Immutable originals and private analysis sources stay untouched. */
internal object RideAnnotationFiles {
    fun fit(source:File,destination:File,value:RideAnnotation) {
        require(source.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
        val encoder=FileEncoder(destination,Fit.ProtocolVersion.V2_0)
        try {
            require(source.inputStream().buffered().use { Decode().read(it,MesgListener { message ->
                encoder.write(if(message.num==18) SessionMesg(message).apply { value.rpe?.let { setWorkoutRpe((it*10).toShort()) } } else message)
            }) })
            if(value.note.isNotEmpty()) {
                val bytes=("B|CYCLE note\n"+value.note).toByteArray(Charsets.UTF_8)
                bytes.asList().chunked(200).forEachIndexed { part,chunk ->
                    val memo=MemoGlobMesg().apply { setPartIndex(part.toLong());setMesgNum(18);setParentIndex(0)
                        chunk.forEachIndexed { index,byte -> setFieldValue(MemoGlobMesg.MemoFieldNum,index,byte.toInt() and 255) }
                    }
                    // Use an exact definition for the final short chunk, without invalid-byte padding.
                    encoder.write(MesgDefinition(memo));encoder.write(memo)
                }
            }
        } finally { encoder.close() }
        require(destination.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
    }
    fun current(context:android.content.Context,id:String):File {
        val gpx=File(context.filesDir,"gpx-library/$id.gpx")
        val json=File(context.filesDir,"dreeve-summaries/$id.json")
        val source=when { gpx.isFile->gpx;json.isFile->json;else->RouteTransferPayload.file(context,id) }
        return exported(context,id,source)
    }
    fun exported(context:android.content.Context,id:String,source:File):File {
        val value=RideAnnotations.read(context,id)
        if(value.empty) return source
        val key=TrainingWorkout.id((WebDavClient.hash(source)+value.json()).toByteArray())
        val target=File(context.cacheDir,"ride-feedback/$key/$id.${source.extension}")
        if(target.isFile) return target
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
        val temporary=File.createTempFile("feedback-",".${source.extension}",target.parentFile)
        try {
            when(source.extension.lowercase()) {
                "fit"->fit(source,temporary,value)
                "json"->{ val json=org.json.JSONObject(source.readText());json.put("bcycleFeedback",value.json());temporary.writeText(json.toString()) }
                "gpx"->gpx(source,temporary,value)
                else->return source
            }
            check(temporary.renameTo(target))
            // Versions are regenerable. Keep only the latest few per ride, never grow with every edit.
            File(context.cacheDir,"ride-feedback").listFiles().orEmpty().map { File(it,"$id.${source.extension}") }
                .filter { it.isFile && it!=target }.sortedByDescending { it.lastModified() }.drop(2).forEach { it.delete();it.parentFile?.delete() }
        } finally { temporary.delete() }
        return target
    }
    private fun gpx(source:File,target:File,value:RideAnnotation) {
        val parser=android.util.Xml.newPullParser().apply { setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES,true) }
        source.inputStream().use { input -> target.outputStream().use { output ->
            parser.setInput(input,"UTF-8")
            val writer=android.util.Xml.newSerializer().apply { setOutput(output,"UTF-8");startDocument("UTF-8",true) }
            val ns="https://bcycle.app/feedback/1"
            var extended=false
            fun feedback() {
                writer.setPrefix("bcycle",ns);writer.startTag(ns,"feedback")
                writer.startTag(ns,"note").text(value.note).endTag(ns,"note")
                value.rpe?.let { writer.startTag(ns,"rpe").text(it.toString()).endTag(ns,"rpe") }
                writer.endTag(ns,"feedback")
            }
            var event=parser.eventType
            while(event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when(event) {
                    org.xmlpull.v1.XmlPullParser.DOCDECL->error("DTD not supported")
                    org.xmlpull.v1.XmlPullParser.START_TAG->{
                        for(i in parser.getNamespaceCount(parser.depth-1) until parser.getNamespaceCount(parser.depth)) writer.setPrefix(parser.getNamespacePrefix(i).orEmpty(),parser.getNamespaceUri(i))
                        writer.startTag(parser.namespace,parser.name)
                        for(i in 0 until parser.attributeCount) writer.attribute(parser.getAttributeNamespace(i),parser.getAttributeName(i),parser.getAttributeValue(i))
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG->{
                        if(parser.depth==2 && parser.name=="extensions") { feedback();extended=true }
                        if(parser.depth==1 && !extended) { writer.startTag(parser.namespace,"extensions");feedback();writer.endTag(parser.namespace,"extensions") }
                        writer.endTag(parser.namespace,parser.name)
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT,org.xmlpull.v1.XmlPullParser.CDSECT,org.xmlpull.v1.XmlPullParser.ENTITY_REF->writer.text(parser.text ?: error("Unknown XML entity"))
                }
                event=parser.nextToken()
            }
            writer.endDocument();writer.flush()
        } }
    }
}
