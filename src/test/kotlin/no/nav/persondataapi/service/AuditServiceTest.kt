package no.nav.persondataapi.service

import no.nav.persondataapi.application
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class AuditServiceTest {
    @Test
    internal fun `skal lage cef med korrekt format`() {
        val auditLogger = AuditLogger()
        val expected ="CEF:0|watson\\oppslag-bruker|oppslag-bruker|1.0|audit:access|oppslag-bruker|INFO|end=1761217044817 duid=12345678901 suid=Z12345 request=/oppslag/personbruker flexString1Label=Decision flexString1=Permit"
        val now = 1761217044817
        val msg = auditLogger.createCefMessage(
            application = application,
            saksbehandlerIdent = "Z12345",
            "12345678901",
            operation = AuditLogger.Operation.READ,
            requestPath = "/oppslag/personbruker",
            permit = AuditLogger.Permit.PERMIT,
            endMillis = now

            )
        Assertions.assertEquals(expected, msg)
    }

}