package de.bcycle.app

import android.util.AtomicFile
import java.io.File

/** Private complete analysis data; exported FIT selection remains independent. */
internal object RideAnalysisFiles {
    fun file(root:File,id:String):File {
        require(id.matches(Regex("(recorded|imported)-[a-f0-9]{64}|[a-f0-9-]{36}")))
        return File(File(root,"ride-analysis"),"$id.fit")
    }
    fun retain(root:File,id:String,journal:File,metadata:FitMetadata?=null) {
        val target=file(root,id);target.parentFile!!.mkdirs()
        val temporary=File.createTempFile("analysis-",".fit",target.parentFile)
        try {
            FitWriter.encode(journal,temporary,fullData=true,metadata=metadata)
            val atomic=AtomicFile(target);val output=atomic.startWrite()
            try { temporary.inputStream().use { it.copyTo(output) };atomic.finishWrite(output) }
            catch(e:Exception) { atomic.failWrite(output);throw e }
        } finally { temporary.delete() }
    }
}
