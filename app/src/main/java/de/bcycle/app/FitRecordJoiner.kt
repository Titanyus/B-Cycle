package de.bcycle.app

import com.garmin.fit.DeveloperField
import com.garmin.fit.Field
import com.garmin.fit.Mesg
import com.garmin.fit.MesgListener

/** Some exporters split one timestamp into position and telemetry records. Hold only one record,
 * join strictly complementary fields, and never bridge timestamps, overlapping fields or events.
 * Original messages/files and the optional raw-record observer are unchanged.
 */
internal class FitRecordJoiner(private val listener:MesgListener,private val rawRecord:((Mesg)->Unit)?=null):MesgListener {
    private var pending:Mesg?=null
    private var count=0
    override fun onMesg(message:Mesg) {
        if(message.num!=20) { flush();listener.onMesg(message);return }
        require(++count<=600_000) { "FIT enthält zu viele Messpunkte" }
        rawRecord?.invoke(message)
        val before=pending
        val time=message.getFieldDoubleValue("timestamp")?.takeIf { it.isFinite() }
        val sameTime=time!=null && time==before?.getFieldDoubleValue("timestamp")
        val complementary=before!=null && sameTime &&
            message.fields.none { it.num!=253 && before.hasField(it.num) } &&
            message.developerFields.none { next -> before.developerFields.any {
                it.developerDataIndex==next.developerDataIndex && it.num==next.num
            } }
        if(complementary) {
            message.fields.filter { it.num!=253 }.forEach { before!!.addField(Field(it)) }
            message.developerFields.forEach { before!!.addDeveloperField(DeveloperField(it)) }
        } else {
            flush();pending=Mesg(message)
        }
    }
    fun flush() { val message=pending;pending=null;if(message!=null) listener.onMesg(message) }
}
