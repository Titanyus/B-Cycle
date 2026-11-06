package de.bcycle.app

import com.garmin.fit.*
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

internal class TrainingImportFailure(val problem:Int):IllegalArgumentException()

/** Bounded workout-only readers. Activity FIT files and unsupported control structures are rejected. */
internal object TrainingFileImport {
    const val MAX_BYTES=4*1024*1024
    private fun number(text:String)=text.trim().toDouble().also { require(it.isFinite() && it>=0) }
    fun read(bytes:ByteArray,filename:String):TrainingWorkout {
        require(bytes.isNotEmpty() && bytes.size<=MAX_BYTES)
        val extension=filename.substringAfterLast('.',"").lowercase()
        val fallback=filename.substringBeforeLast('.').ifBlank { "Training" }.take(512)
        val result=when(extension) { "fit"->fit(bytes,fallback);"zwo"->zwo(bytes,fallback);"erg"->erg(bytes,fallback);else->throw TrainingImportFailure(R.string.training_format_unsupported) }
        return result.copy(id=TrainingWorkout.id(bytes),source=filename.take(512),format=extension)
    }
    private fun workout(name:String,description:String,steps:List<TrainingStep>,sport:String="Ride",tags:List<String> = emptyList())=
        TrainingWorkout("0".repeat(64),name.take(512),description.take(20000),sport,"","",steps,tags)
    private fun zwo(bytes:ByteArray,fallback:String):TrainingWorkout {
        val text=bytes.toString(Charsets.UTF_8)
        require(!text.contains('\u0000') && !Regex("<!DOCTYPE|<!ENTITY",RegexOption.IGNORE_CASE).containsMatchIn(text))
        val factory=DocumentBuilderFactory.newInstance().apply { isExpandEntityReferences=false }
        val builder=factory.newDocumentBuilder().apply { setEntityResolver { _,_ -> throw org.xml.sax.SAXException("External entities disabled") } }
        val root=builder.parse(bytes.inputStream()).documentElement
        require(root.tagName=="workout_file")
        fun meta(key:String)=root.getElementsByTagName(key).item(0)?.textContent.orEmpty()
        val container=root.getElementsByTagName("workout").item(0) ?: error("No workout")
        val steps=ArrayList<TrainingStep>()
        fun add(e:Element,duration:String,power:String,cadence:String="Cadence",end:String?=null) {
            val seconds=number(e.getAttribute(duration));require(seconds>0)
            val targets=mutableListOf<TrainingTarget>()
            if(e.hasAttribute(power)) targets+=TrainingTarget(TrainingUnit.FTP,number(e.getAttribute(power)),end?.let { number(e.getAttribute(it)) } ?: number(e.getAttribute(power)),end!=null)
            if(e.hasAttribute(cadence)) targets+=TrainingTarget(TrainingUnit.CADENCE,number(e.getAttribute(cadence)))
            steps+=TrainingStep(e.tagName,seconds,targets)
            require(steps.size<=4096)
        }
        for(i in 0 until container.childNodes.length) {
            val e=container.childNodes.item(i) as? Element ?: continue
            when(e.tagName) {
                "Warmup","Cooldown","Ramp"->add(e,"Duration","PowerLow",end="PowerHigh")
                "SteadyState"->{ require(e.hasAttribute("Power"));add(e,"Duration","Power") }
                "FreeRide"->add(e,"Duration","Power")
                "IntervalsT"->{ require(e.hasAttribute("OnPower") && e.hasAttribute("OffPower"));val repeats=e.getAttribute("Repeat").toInt();require(repeats in 1..1000)
                    repeat(repeats) { add(e,"OnDuration","OnPower",if(e.hasAttribute("Cadence")) "Cadence" else "OnCadence");if(e.getAttribute("OffDuration").toDouble()>0) add(e,"OffDuration","OffPower",if(e.hasAttribute("CadenceResting")) "CadenceResting" else "OffCadence") } }
                else->throw TrainingImportFailure(R.string.training_structure_unsupported)
            }
        }
        return workout(meta("name").ifBlank { fallback },meta("description"),steps,meta("sportType").ifBlank { "Ride" },(0 until root.getElementsByTagName("tag").length).map { (root.getElementsByTagName("tag").item(it) as Element).getAttribute("name") })
    }
    private fun erg(bytes:ByteArray,fallback:String):TrainingWorkout {
        val text=bytes.toString(Charsets.UTF_8);var data=false;var name=fallback;var description="";var percent=false
        val points=ArrayList<Pair<Double,Double>>()
        for(raw in text.lineSequence()) {
            val line=raw.trim().removePrefix("\uFEFF")
            when {
                line.equals("[COURSE DATA]",true)->data=true
                line.startsWith("[END COURSE DATA]",true)->data=false
                data && line.isNotBlank() && !line.startsWith(";")-> {
                    val cells=line.split(Regex("\\s+"));require(cells.size==2);points+=number(cells[0])*60 to number(cells[1]);require(points.size<=4097)
                }
                line.startsWith("FILE NAME=",true)->name=line.substringAfter('=').trim().ifBlank { fallback }
                line.startsWith("DESCRIPTION=",true)->description=line.substringAfter('=').trim()
                line.equals("MINUTES PERCENT",true)->percent=true
            }
        }
        require(points.size>=2 && points.first().first==0.0)
        val steps=points.zipWithNext().mapNotNull { (a,b) ->
            require(b.first>=a.first)
            if(b.first==a.first) null else TrainingStep("",b.first-a.first,listOf(TrainingTarget(if(percent) TrainingUnit.FTP else TrainingUnit.WATTS,a.second/if(percent) 100 else 1,b.second/if(percent) 100 else 1,a.second!=b.second)))
        }
        return workout(name,description,steps)
    }
    private fun fit(bytes:ByteArray,fallback:String):TrainingWorkout {
        require(Decode().checkFileIntegrity(bytes.inputStream()))
        var header:WorkoutMesg?=null;val raw=ArrayList<WorkoutStepMesg>()
        require(Decode().read(bytes.inputStream(),MesgListener { mesg -> when(mesg.num) {
            MesgNum.WORKOUT->header=WorkoutMesg(mesg)
            MesgNum.WORKOUT_STEP->{ require(raw.size<4096);raw+=WorkoutStepMesg(mesg) }
        } }))
        val metadata=header ?: throw TrainingImportFailure(R.string.training_fit_not_workout)
        val steps=ArrayList<TrainingStep>();val positions=HashMap<Int,Int>()
        fun target(type:WktStepTarget?,zone:Long?,low:Long?,high:Long?):TrainingTarget? {
            if(type==null || type==WktStepTarget.OPEN) return null
            fun values(unit:TrainingUnit,offset:Double=0.0,scale:Double=1.0):TrainingTarget? = low?.let { TrainingTarget(unit,(it-offset)/scale,((high ?: it)-offset)/scale,label=type.name) }
            return when(type) {
                WktStepTarget.POWER,WktStepTarget.POWER_3S,WktStepTarget.POWER_10S,WktStepTarget.POWER_30S,WktStepTarget.POWER_LAP->
                    if(zone!=null && zone in 1..7) TrainingTarget(TrainingUnit.POWER_ZONE,zone.toDouble()) else if(low!=null && low>=1000 && (high ?: low)>=1000) values(TrainingUnit.WATTS,1000.0) else values(TrainingUnit.FTP,scale=100.0)
                WktStepTarget.HEART_RATE,WktStepTarget.HEART_RATE_LAP->
                    if(zone!=null && zone in 1..5) TrainingTarget(TrainingUnit.HR_ZONE,zone.toDouble()) else if(low!=null && low>=100 && (high ?: low)>=100) values(TrainingUnit.HR_BPM,100.0) else values(TrainingUnit.HR_MAX_PERCENT)
                WktStepTarget.CADENCE->values(TrainingUnit.CADENCE)
                WktStepTarget.SPEED,WktStepTarget.SPEED_LAP->values(TrainingUnit.SPEED,scale=1000.0)
                else->values(TrainingUnit.OTHER)
            }
        }
        for((ordinal,s) in raw.sortedBy { it.messageIndex }.withIndex()) {
            val index=s.messageIndex ?: ordinal
            if(s.durationType==WktStepDuration.REPEAT_UNTIL_STEPS_CMPLT) {
                val start=positions[s.durationStep?.toInt()] ?: error("Invalid repeat start")
                val count=s.repeatSteps?.toInt() ?: error("Invalid repeat count");require(count in 1..1000)
                val block=steps.subList(start,steps.size).toList();require(block.isNotEmpty() && steps.size+block.size*(count-1)<=4096)
                repeat(count-1) { steps.addAll(block) };positions[index]=steps.size
            } else {
                if(s.durationType?.name?.startsWith("REPEAT_")==true) throw TrainingImportFailure(R.string.training_structure_unsupported)
                require(index !in positions);positions[index]=steps.size
                val targets=listOfNotNull(target(s.targetType,s.targetValue,s.customTargetValueLow,s.customTargetValueHigh),target(s.secondaryTargetType,s.secondaryTargetValue,s.secondaryCustomTargetValueLow,s.secondaryCustomTargetValueHigh))
                val time=if(s.durationType in listOf(WktStepDuration.TIME,WktStepDuration.TIME_ONLY,WktStepDuration.REPETITION_TIME)) s.durationTime?.toDouble() else null
                steps+=TrainingStep(s.wktStepName.orEmpty().take(512),time,targets,s.durationType?.name ?: "OPEN",if(time!=null) time else s.durationValue?.toDouble())
            }
        }
        require(metadata.numValidSteps==null || metadata.numValidSteps==raw.size)
        return workout(metadata.wktName?.ifBlank { fallback } ?: fallback,metadata.wktDescription.orEmpty(),steps,metadata.sport?.name ?: "Ride")
    }
}
