package no.nav.persondataapi.service

import no.nav.persondataapi.integrasjon.nom.NomClient
import no.nav.persondataapi.integrasjon.nom.SaksbehandlerTilhørighetResultat
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SaksbehandlerService(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val nomClient: NomClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Henter organisasjonstilhørighet for innlogget saksbehandler.
     */
    suspend fun hentSaksbehandler(): SaksbehandlerTilhørighetResultat {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")
        val navIdent = token.jwtTokenClaims.get("NAVident").toString()

        val resultat = nomClient.hentSaksbehandlerTilhørighet(navIdent)

        if (resultat.errorMessage == null && resultat.data != null) {
            logger.info("Saksbehandler $navIdent i ${resultat.data.organisasjoner.joinToString(", ")} lastet applikasjonen")
        } else {
            logger.warn("Feil ved henting av organisasjonstilhørighet for ${navIdent}: ${resultat.errorMessage}")
        }
        return resultat
    }
}
