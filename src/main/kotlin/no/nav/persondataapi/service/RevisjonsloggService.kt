package no.nav.persondataapi.service

// Loggers
import no.nav.persondataapi.application
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.RevisjonsLogger.Operasjon
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class RevisjonsloggService(
	val auditLogger: RevisjonsLogger,
) {
	val logger = LoggerFactory.getLogger(javaClass)
	val revisjonslogg = LoggerFactory.getLogger("audit")
	var clock = Clock.systemDefaultZone()

	fun tilgangOppslagGodkjent(
		ident: PersonIdent,
		saksbehandlerIdent: String,
	) {
		val msg: String
		try {
			msg =
				auditLogger.createCefMessage(
					application = application,
					saksbehandlerIdent = saksbehandlerIdent,
					personIdent = ident.value,
					operasjon = Operasjon.READ,
					requestPath = "/oppslag/personbruker",
					adgang = RevisjonsLogger.Adgang.PERMIT,
					endMillis = Instant.now(clock).toEpochMilli(),
				)
			revisjonslogg.info(msg)
		} catch (e: Exception) {
			logger.error(e.message, e)
		}
	}
}

@Component
class RevisjonsLogger {
    /*
     * Lager eb CEF kompatibel streng som vi skal sende til ArchSite for audit logging.
     * */
	fun createCefMessage(
		application: String,
		saksbehandlerIdent: String,
		personIdent: String?,
		operasjon: Operasjon,
		requestPath: String,
		adgang: Adgang,
		endMillis: Long,
	): String {
		val subject = personIdent?.padStart(11, '0')
		val duidStr = subject?.let { " duid=$it" } ?: ""
		return "CEF:0|$application|oppslag-bruker|1.0|${operasjon.logString}|oppslag-bruker|INFO|" +
			"end=$endMillis$duidStr suid=$saksbehandlerIdent request=$requestPath " +
			"flexString1Label=Decision flexString1=${adgang.logString}"
	}

	enum class Operasjon(
		val logString: String,
	) {
		READ("audit:access"),
		WRITE("audit:update"),
		UNKNOWN("audit:unknown"),
	}

	enum class Adgang(
		val logString: String,
	) {
		PERMIT("Permit"),
		DENY("Deny"),
	}
}
