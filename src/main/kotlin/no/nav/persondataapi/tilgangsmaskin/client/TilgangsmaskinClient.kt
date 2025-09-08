package no.nav.persondataapi.tilgangsmaskin.client

import no.nav.persondataapi.domain.TilgangMaskinResultat
import no.nav.persondataapi.domain.TilgangResultat
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Component
class TilgangsmaskinClient (
    private val tokenService: TokenService,
    @Qualifier("tilgangWebClient")
    private val webClient: WebClient,


    ) {
        fun sjekkTilgang(fnr: String, userToken: String
            ): TilgangResultat {
            return runCatching {

                val oboToken = tokenService.exchangeToken(
                    userToken, SCOPE.TILGANGMASKIN_SCOPE
                )
                val responseResult = webClient.post()
                    .uri("/api/v1/komplett")
                    .header("Authorization", "Bearer $oboToken")
                    .header("Nav-Call-Id", UUID.randomUUID().toString())
                    .bodyValue(fnr)
                    .exchangeToMono { response ->
                        val status = response.statusCode()
                        val headers = response.headers().asHttpHeaders()
                        if (status.is2xxSuccessful) {
                            response.bodyToMono(object : ParameterizedTypeReference<TilgangMaskinResultat>() {})
                        }
                        if (status.is4xxClientError){
                            response.bodyToMono(object : ParameterizedTypeReference<TilgangMaskinResultat>() {})
                        }
                        else {
                            response.bodyToMono(String::class.java).map { body ->
                                println("Feilrespons: $body")
                                throw RuntimeException("Feil fra inntektsAPI: HTTP $status – $body")
                            }
                        }
                    }.block()!!
                responseResult
            }.fold(
                onSuccess = { resultat ->
                    println("inntekt er ok..fått svar!")
                    TilgangResultat(
                        data = resultat,
                        statusCode = 200,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    println("Feil ved sjekk av tilgang")
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