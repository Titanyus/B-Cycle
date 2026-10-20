package de.bcycle.app

import org.json.JSONObject
import org.json.JSONTokener
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal object BackupJson {
    fun text(bytes:ByteArray,max:Int):String {
        require(bytes.size<=max)
        val text=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        var depth=0;var quoted=false;var escaped=false
        text.forEach { c ->
            if(quoted) { if(escaped) escaped=false else if(c=='\\') escaped=true else if(c=='"') quoted=false }
            else if(c=='"') quoted=true else if(c=='{'||c=='[') { depth++;require(depth<=32) }
            else if(c=='}'||c==']') { depth--;require(depth>=0) }
        }
        require(depth==0 && !quoted)
        return text
    }
    fun objectValue(bytes:ByteArray,max:Int):JSONObject {
        val tokener=JSONTokener(text(bytes,max));val row=tokener.nextValue() as? JSONObject ?: error("JSON object required")
        require(tokener.nextClean()==0.toChar());return row
    }
}
