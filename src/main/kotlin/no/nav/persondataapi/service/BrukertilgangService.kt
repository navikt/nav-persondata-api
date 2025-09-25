package no.nav.persondataapi.service

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class BrukertilgangService(
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val tilgangService: TilgangService,
) {
    fun harBrukerTilgangTilIdent(ident: String): Boolean {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")

        val groups = token.jwtTokenClaims.get("groups") as? List<String> ?: emptyList()

        return if (tilgangService.harUtvidetTilgang(groups)) {
            true
        } else {
            val status = tilgangService.sjekkTilgang(ident, token.encodedToken)
            return status == 200
        }
    }
}
