package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class ServiceProblemsTest {
    @Test fun transportFailuresAreDistinctFromAuthenticationProtocolAndLocalFileErrors() {
        assertEquals(ServiceProblems.Problem.UNREACHABLE,ServiceProblems.of(java.net.ConnectException("secret host")))
        assertEquals(ServiceProblems.Problem.UNREACHABLE,ServiceProblems.of(java.net.UnknownHostException("secret host")))
        assertEquals(ServiceProblems.Problem.TIMEOUT,ServiceProblems.of(java.net.SocketTimeoutException()))
        assertEquals(ServiceProblems.Problem.TLS,ServiceProblems.of(javax.net.ssl.SSLHandshakeException("certificate")))
        assertEquals(ServiceProblems.Problem.AUTH,ServiceProblems.of(IntervalsFailure(false,R.string.intervals_auth_failed)))
        assertEquals(ServiceProblems.Problem.AUTH,ServiceProblems.of(DavFailure(false,"private",R.string.service_auth_failed)))
        assertEquals(ServiceProblems.Problem.SERVER,ServiceProblems.of(ActivityServiceFailure(true,R.string.service_server_failed)))
        assertNull(ServiceProblems.of(java.io.FileNotFoundException("local file")))
        assertNull(ServiceProblems.of(java.io.IOException("unspecified failure")))
        assertEquals(R.string.service_timeout,ServiceProblems.text(java.io.IOException(java.net.SocketTimeoutException()),R.string.service_upload_failed))
    }
    @Test fun httpDiagnosticsAndPersistedKeysRoundTripWithoutRawServerData() {
        for((code,problem) in listOf(401 to ServiceProblems.Problem.AUTH,403 to ServiceProblems.Problem.AUTH,408 to ServiceProblems.Problem.TIMEOUT,504 to ServiceProblems.Problem.TIMEOUT,429 to ServiceProblems.Problem.RATE,503 to ServiceProblems.Problem.SERVER)) {
            assertEquals(problem,ServiceProblems.http(code));assertEquals(problem.text,ServiceProblems.resource(problem.key))
        }
        assertNull(ServiceProblems.http(404));assertNull(ServiceProblems.http(409))
    }
}
