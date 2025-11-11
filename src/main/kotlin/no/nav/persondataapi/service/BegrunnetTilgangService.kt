package no.nav.persondataapi.service

import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrunnetTilgangService(
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun loggBegrunnetTilgang(personIdent: PersonIdent, begrunnelse: String) {
        logger.info(
            teamLogsMarker,
            "Begrunnet tilgang registrert; ident={} melding={}",
            personIdent.value,
            begrunnelse
        )
    }
}
