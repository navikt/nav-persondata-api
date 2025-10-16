package no.nav.persondataapi.service

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Service

@Service
class BrukertilgangService(
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val tilgangService: TilgangService,
) {
    fun harSaksbehandlerTilgangTilPersonIdent(personIdent: PersonIdent): Boolean {
        val status = hentStatusPåBruker(personIdent)
        return status == 200
    }

    fun hentStatusPåBruker(personIdent: PersonIdent): Int {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")

        val groups = token.jwtTokenClaims.get("groups") as? List<String> ?: emptyList()

        return when (val status = tilgangService.sjekkTilgang(personIdent, token.encodedToken)) {
            403 -> if (tilgangService.harUtvidetTilgang(groups)) 200 else status
            else -> status
        }
    }
}
