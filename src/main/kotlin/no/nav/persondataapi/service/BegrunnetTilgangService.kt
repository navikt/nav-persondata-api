package no.nav.persondataapi.service

import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrunnetTilgangService {
	private val logger = LoggerFactory.getLogger(javaClass)

	fun loggBegrunnetTilgang(
		personIdent: PersonIdent,
		begrunnelse: String,
		mangel: String,
	) {
		logger.info("Begrunnet tilgang registrert")
		logger.info(
			teamLogsMarker,
			"Begrunnet tilgang registrert; ident={} melding={} mangel={}",
			personIdent.value,
			begrunnelse,
			mangel,
		)
	}
}
