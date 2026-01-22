package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.nom.NomClient
import no.nav.persondataapi.integrasjon.nom.SaksbehandlerTilhørighetResultat
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Service

@Service
class SaksbehandlerService(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val nomClient: NomClient,
) {
    /**
     * Henter organisasjonstilhørighet for innlogget saksbehandler.
     */
    suspend fun hentSaksbehandler(): SaksbehandlerTilhørighetResultat {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")
        val navIdent = token.jwtTokenClaims.get("NAVident").toString()

        return nomClient.hentSaksbehandlerTilhørighet(navIdent)
    }
}
