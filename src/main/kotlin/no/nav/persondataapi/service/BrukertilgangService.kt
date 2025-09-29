package no.nav.persondataapi.service

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Service

@Service
class BrukertilgangService(
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val tilgangService: TilgangService,
) {
    fun harSaksbehandlerTilgangTilPersonIdent(ident: String): Boolean {
        val status = hentStatusPåBruker(ident)
        return status == 200
    }

    fun hentStatusPåBruker(ident: String): Int {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")

        val groups = token.jwtTokenClaims.get("groups") as? List<String> ?: emptyList()

        return when (val status = tilgangService.sjekkTilgang(ident, token.encodedToken)) {
            403 -> if (tilgangService.harUtvidetTilgang(groups)) 200 else status
            else -> status
        }
    }
}
