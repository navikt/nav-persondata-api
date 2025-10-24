package no.nav.persondataapi.service

// Loggers
import no.nav.persondataapi.application
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.AuditLogger.Operation
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class AuditService (val auditLogger: AuditLogger)
{

    val logger = LoggerFactory.getLogger(javaClass)
    val audit = LoggerFactory.getLogger("audit")
    var clock = Clock.systemDefaultZone()
    fun auditLookupGranted(ident: PersonIdent,saksbehandlerIdent: String) {

        val msg: String
        try {
            msg = auditLogger.createCefMessage(
                       application = application,
                       saksbehandlerIdent = saksbehandlerIdent,
                       fnr = ident.value,
                       operation = Operation.READ,
                       requestPath = "/oppslag/personbruker",
                       permit = AuditLogger.Permit.PERMIT,
                       endMillis = Instant.now(clock).toEpochMilli()
                       )
            MDC.put("team", "team holmes")
            audit.info(msg)
            MDC.clear()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }

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
