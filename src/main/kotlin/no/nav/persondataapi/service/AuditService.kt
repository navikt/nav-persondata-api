package no.nav.persondataapi.service

// Loggers
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.AuditLogger.Operation
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime.now

private const val application = "oppslag-bruker"
@Service
class AuditService (val auditLogger: AuditLogger)
{

    val audit = LoggerFactory.getLogger("audit")
    var clock = Clock.systemDefaultZone()
    fun auditLookupGranted(ident: PersonIdent,saksbehandlerIdent: String) {

        val msg = auditLogger.createCefMessage(
            application = application,
            saksbehandlerIdent = saksbehandlerIdent,
            fnr = ident.value,
            operation = Operation.READ,
            requestPath = "/oppslag/personbruker",
            permit = AuditLogger.Permit.PERMIT,
            endMillis = Instant.now(clock).toEpochMilli()
            )
        MDC.put("team", "team holmes");
        audit.info(msg)
        MDC.clear();
    }
    fun auditLookupDenied(ident: PersonIdent,saksbehandlerIdent: String) {

        val msg = AuditLogger().createCefMessage(
            application = application,
            saksbehandlerIdent = saksbehandlerIdent,
            fnr = ident.value,
            operation = Operation.READ,
            requestPath = "/oppslag/personbruker",
            permit = AuditLogger.Permit.DENY,
            endMillis = Instant.now(clock).toEpochMilli()

        )
        MDC.put("team", "team holmes");
        audit.info(msg)
        MDC.clear();
    }



}
@Component
class AuditLogger {
    fun createCefMessage(
        application: String,
        saksbehandlerIdent: String,
        fnr: String?,
        operation: Operation,
        requestPath: String,
        permit: Permit,
        endMillis: Long
    ): String {
        val subject = fnr?.padStart(11, '0')
        val duidStr = subject?.let { " duid=$it" } ?: ""
        return "CEF:0|$application|Sporingslogg|1.0|${operation.logString}|Sporingslogg|INFO|" +
                "end=$endMillis$duidStr suid=$saksbehandlerIdent request=$requestPath " +
                "flexString1Label=Decision flexString1=${permit.logString}"
    }

    enum class Operation(val logString: String) {
        READ("audit:access"), WRITE("audit:update"), UNKNOWN("audit:unknown")
    }
    enum class Permit(val logString: String) {
        PERMIT("Permit"), DENY("Deny")
    }
}