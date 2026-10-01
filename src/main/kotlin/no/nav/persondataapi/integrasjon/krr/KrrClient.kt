package no.nav.persondataapi.integrasjon.krr

import io.micrometer.core.instrument.MeterRegistry
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.concurrent.TimeUnit

@Component
class KrrClient(
    private val tokenService: TokenService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    @param:Qualifier("krrWebClient") private val webClient: WebClient,
    private val meterRegistry: MeterRegistry,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Henter kun e-post for personen som det allerede er bekreftet tilgang til. */
    fun hentEpost(personIdent: PersonIdent): String? {
        val startet = System.nanoTime()
        var resultat = "feil"
        return try {
            // RØD SONE: Krev delegert saksbehandlerkontekst; et maskin-token skal aldri kunne hente KRR-e-post.
            val token = tokenValidationContextHolder.getTokenValidationContext().firstValidToken
            val navIdent = token?.jwtTokenClaims?.get("NAVident") as? String
            if (navIdent.isNullOrBlank()) {
                resultat = "uten_bruker"
                null
            } else {
                val oboToken = tokenService.exchangeToken(token.encodedToken, SCOPE.KRR_SCOPE)
                val svar =
                    webClient
                        .post()
                        .uri("/rest/v1/personer")
                        .header("Authorization", "Bearer $oboToken")
                        .bodyValue(mapOf("personidenter" to setOf(personIdent.value)))
                        .retrieve()
                        .bodyToMono<KrrRespons>()
                        .block()

                val kontaktinfo = svar?.personer?.get(personIdent.value)
                val epost =
                    kontaktinfo
                        ?.takeIf { !svar.feil.containsKey(personIdent.value) }
                        ?.takeIf { it.personident == personIdent.value && it.aktiv == true && it.reservert != true }
                        ?.epostadresse
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                resultat = if (epost == null) "ikke_funnet" else "funnet"
                epost
            }
        } catch (_: Exception) {
            // Ikke logg ident, token, URL med parametre eller respons fra KRR.
            logger.warn("KRR-e-post kunne ikke hentes")
            null
        } finally {
            // Fire faste utfall som etiketter. Verken ident eller kontaktdata legges i metrikker.
            meterRegistry.counter("personopplysninger_krr_epost_oppslag", "resultat", resultat).increment()
            meterRegistry
                .timer("personopplysninger_krr_epost_varighet", "resultat", resultat)
                .record(System.nanoTime() - startet, TimeUnit.NANOSECONDS)
        }
    }
}

private data class KrrRespons(
    val personer: Map<String, KrrPerson> = emptyMap(),
    val feil: Map<String, String> = emptyMap(),
)

private data class KrrPerson(
    val personident: String? = null,
    val aktiv: Boolean? = null,
    val reservert: Boolean? = null,
    val epostadresse: String? = null,
)
