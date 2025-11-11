package no.nav.persondataapi.service

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrukertilgangService(
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val tilgangService: TilgangService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    fun harSaksbehandlerTilgangTilPersonIdent(personIdent: PersonIdent): Boolean {
        val status = hentStatusPåBruker(personIdent)
        return status == 200
    }

    fun hentStatusPåBruker(personIdent: PersonIdent): Int {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")

        val groups = token.jwtTokenClaims.get("groups") as? List<String> ?: emptyList()

        return when (val status = tilgangService.sjekkTilgang(personIdent, token.encodedToken)) {
            403 -> {
                if (tilgangService.harUtvidetTilgang(groups)) {
                    logger.info("saksbehandler benytter  utvidet tilgang til $personIdent")
                    return 200
                }
                else status
            }
            else -> status
        }
    }
}
