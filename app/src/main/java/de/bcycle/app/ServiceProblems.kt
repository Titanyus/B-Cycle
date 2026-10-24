package de.bcycle.app

/** Stable, credential-free diagnostics shared by connection checks and durable transfer receipts. */
internal object ServiceProblems {
    enum class Problem(val key:String,val text:Int) {
        UNREACHABLE("unreachable",R.string.service_unreachable),TIMEOUT("timeout",R.string.service_timeout),
        TLS("tls",R.string.service_tls_failed),AUTH("auth",R.string.service_auth_failed),
        SERVER("server",R.string.service_server_failed),RATE("rate",R.string.service_rate_limited),
        RESPONSE("response",R.string.service_response_failed),DUPLICATE("duplicate",R.string.service_duplicate)
    }
    fun resource(key:String)=Problem.entries.firstOrNull { it.key==key }?.text
    fun of(error:Throwable?):Problem? {
        if(error==null) return null
        val known=when(error) { is ActivityServiceFailure->error.problem;is IntervalsFailure->error.problem;is DavFailure->error.problem;else->null }
        (if(known==R.string.intervals_auth_failed) Problem.AUTH else Problem.entries.firstOrNull { it.text==known })?.let { return it }
        return when(error) {
            is javax.net.ssl.SSLException,is java.security.cert.CertificateException->Problem.TLS
            is java.net.SocketTimeoutException->Problem.TIMEOUT
            is java.net.UnknownHostException,is java.net.ConnectException,is java.net.NoRouteToHostException,is java.net.SocketException->Problem.UNREACHABLE
            else->if(error.cause!==error) of(error.cause) else null
        }
    }
    fun http(code:Int)=when(code) {401,403->Problem.AUTH;408,504->Problem.TIMEOUT;429->Problem.RATE;in 500..599->Problem.SERVER;else->null}
    fun global(problem:Problem)=problem !in setOf(Problem.RESPONSE,Problem.DUPLICATE)
    fun text(error:Throwable?,fallback:Int)=of(error)?.text ?: when(error) {
        is ActivityServiceFailure->error.problem;is IntervalsFailure->error.problem;else->fallback
    }
}
