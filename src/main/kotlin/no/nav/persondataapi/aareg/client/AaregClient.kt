package no.nav.persondataapi.aareg.client

import no.nav.inntekt.generated.model.InntektshistorikkApiInn
import no.nav.inntekt.generated.model.InntektshistorikkApiUt
import no.nav.persondataapi.domain.AaregResultat
import no.nav.persondataapi.domain.InntektResultat
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class AaregClient(
                  private val tokenService: TokenService,
                  @Qualifier("aaregWebClient")
                  private val webClient: WebClient) {

    fun hentArbeidsForhold(fnr:String,token:String): AaregResultat {
        return runCatching {

            val oboToken = tokenService.exchangeToken(
                token, SCOPE.PDL_SCOPE
            )
            val responseResult = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v2/arbeidstaker/arbeidsforhold")
                        .queryParam("historikk", "true")
                        .queryParam("arbeidsforholdstatus", "AKTIV", "AVSLUTTET", "FREMTIDIG")
                        .queryParam("rapporteringsordning", "A_ORDNINGEN")
                        .build()
                }
                .header("Authorization", "Bearer $oboToken")
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .header("Nav-Personident", fnr)
                .exchangeToMono { response ->
                    val status = response.statusCode()
                    val headers = response.headers().asHttpHeaders()

                    println("HTTP status: $status")
                    println("Headers: $headers")

                    if (status.is2xxSuccessful) {
                        response.bodyToMono(object : ParameterizedTypeReference<List<AaRegArbeidsforhold>>() {})
                    } else {
                        response.bodyToMono(String::class.java).map { body ->
                            println("Feilrespons: $body")
                            throw RuntimeException("Feil fra AaregAPI: HTTP $status – $body")
                        }
                    }
                }.block()!!
            responseResult
        }.fold(
            onSuccess = { resultat ->
                println("inntekt er ok..fått svar!")
                AaregResultat(
                    data = resultat,
                    statusCode = 200,
                    ""
                )
            },
            onFailure = { error ->
                println("Feil ved henting av utbetalinger")
                error.printStackTrace()
                AaregResultat (
                    data = null,
                    statusCode = 500,
                    errorMessage = "Feil ved lesing: ${error.message}"
                )
            }
        )
    }
}