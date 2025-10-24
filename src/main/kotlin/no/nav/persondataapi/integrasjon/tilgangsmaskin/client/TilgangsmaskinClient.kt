package no.nav.persondataapi.integrasjon.tilgangsmaskin.client

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TilgangsmaskinClient
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.*

@Component
class TilgangsmaskinClientImpl (
    private val tokenService: TokenService,
    @param:Qualifier("tilgangWebClient")
    private val webClient: WebClient,


    ): TilgangsmaskinClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun sjekkTilgang(personIdent: PersonIdent, saksbehandlerToken: String
            ): TilgangResultat {
            return runCatching {

                val oboToken = tokenService.exchangeToken(
                    saksbehandlerToken, SCOPE.TILGANGMASKIN_SCOPE
                )
                val responseResult = webClient.post()
                    .uri("/api/v1/komplett")
                    .header("Authorization", "Bearer $oboToken")
                    .header("Nav-Call-Id", UUID.randomUUID().toString())
                    .bodyValue(personIdent.value)
                    .exchangeToMono {
                        response ->
                        val status = response.statusCode()
                        if (status.value() == 204) {
                            Mono.just(TilgangMaskinResultat(
                                type = null,
                                title = null,
                                status = 204,
                                instance = null,
                                brukerIdent = personIdent.value,
                                navIdent = null,
                                traceId = null,
                                begrunnelse = null,
                                kanOverstyres = true
                            ))
                        }
                        else if (status.is2xxSuccessful || status.is4xxClientError) {
                            response.bodyToMono(object : ParameterizedTypeReference<TilgangMaskinResultat>() {})
                        }
                        else {
                            response.bodyToMono(String::class.java).map { body ->
                                logger.error("Feilrespons: $body")
                                throw RuntimeException("Feil fra Tilgang: HTTP $status – $body")
                            }
                        }
                    }.block()!!
                responseResult
            }.fold(
                onSuccess = { resultat ->
                    logger.debug("Tilgangskontroll: ${resultat.title} ${resultat.status} ${resultat.type} ${resultat.begrunnelse}")
                    TilgangResultat(
                        data = resultat,
                        statusCode = 200,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    logger.error("Feil ved sjekk av tilgang",error)
                    error.printStackTrace()
                    TilgangResultat (
                        data = null,
                        statusCode = 500,
                        errorMessage = "Feil ved kall: ${error.message}"
                    )
                }
            )
        }
    }

data class TilgangMaskinResultat(
    val type: String?,
    val title: String?,
    val status: Int,
    val instance: String?,
    val brukerIdent: String?,
    val navIdent: String?,
    val traceId: String?,
    val begrunnelse: String?,
    val kanOverstyres: Boolean?,
)

data class TilgangResultat(
    val data: TilgangMaskinResultat?,
    val statusCode: Int?,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)
