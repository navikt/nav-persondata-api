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
        return hentTilgangsvurdering(personIdent).status == 200
    }

    fun hentTilgangsvurdering(personIdent: PersonIdent): BrukertilgangVurdering {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")

        val groups = token.jwtTokenClaims.get("groups") as? List<String> ?: emptyList()

        val resultat = tilgangService.hentTilgangsresultat(personIdent, token.encodedToken)
        val beregnetStatus = tilgangService.beregnStatus(resultat)
        val status = if (beregnetStatus != 200) {
                if (tilgangService.harUtvidetTilgang(groups)) {
                    logger.info("Saksbehandler har utvidet tilgang til $personIdent")
                    200
                }
                else beregnetStatus
        } else beregnetStatus

        return BrukertilgangVurdering(
            status = status,
            tilgang = resultat.data?.title ?: "UKJENT"
        )
    }
}

data class BrukertilgangVurdering(
    val status: Int,
    val tilgang: String,
)
