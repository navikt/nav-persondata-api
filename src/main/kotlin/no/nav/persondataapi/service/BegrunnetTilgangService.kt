package no.nav.persondataapi.service

import no.nav.persondataapi.konfigurasjon.teamLogsMarker
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrunnetTilgangService(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun loggBegrunnetTilgang(personIdent: PersonIdent, begrunnelse: String, mangel: String) {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")

        val saksbehandlerIdent = token.jwtTokenClaims.get("NAVIdent") as String? ?: "ukjent"

        logger.info("Begrunnet tilgang registrert")
        logger.info(
            teamLogsMarker,
            "Begrunnet tilgang registrert; saksbehandler={} personIdent={} melding={} mangel={}",
            saksbehandlerIdent,
            personIdent.value,
            begrunnelse,
            mangel,
        )
    }
}
