package no.nav.persondataapi.inntekt.client


import no.nav.inntekt.generated.model.InntektshistorikkApiInn
import no.nav.inntekt.generated.model.InntektshistorikkApiUt
import no.nav.persondataapi.domain.InntektResultat
import no.nav.persondataapi.domain.KontrollPeriode

import no.nav.persondataapi.service.TokenService

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID


@Component
class InntektClient(
    private val tokenService: TokenService,
    @Qualifier("inntektWebClient")
    private val webClient: WebClient,
    @Value("\${INNTEKT_URL}")
    private val inntekt_url: String,
    @Value("\${INNTEKT_SCOPE}")
    private val inntekt_scope: String,

    ) {
    fun hentInntekter(fnr: String, token:String, kontrollPeriode: KontrollPeriode = KontrollPeriode(LocalDate.now().minusYears(5),
        LocalDate.now())): InntektResultat {
        return runCatching {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM")

            val requestBody = InntektshistorikkApiInn(
                personident = fnr,
                filter= "NAVKontrollA-Inntekt",
                formaal = "NAVKontroll",
                maanedFom = kontrollPeriode.fom.format(formatter),
                maanedTom = kontrollPeriode.tom.format(formatter),
            )
            val oboToken = tokenService.exchangeToken(
                token,
                inntekt_scope
            )
            val responseResult = webClient.post()
                .uri("/rest/v2/inntektshistorikk")
                .header("Authorization", "Bearer $oboToken")
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .bodyValue(requestBody)
                .exchangeToMono { response ->
                    val status = response.statusCode()
                    val headers = response.headers().asHttpHeaders()

                    println("HTTP status: $status")
                    println("Headers: $headers")

                    if (status.is2xxSuccessful) {
                        response.bodyToMono(object : ParameterizedTypeReference<InntektshistorikkApiUt>() {})
                    } else {
                        response.bodyToMono(String::class.java).map { body ->
                            println("Feilrespons: $body")
                            throw RuntimeException("Feil fra inntektsAPI: HTTP $status – $body")
                        }
                    }
                }.block()!!
            responseResult
        }.fold(
            onSuccess = { inntekt ->
                println("inntekt er ok..fått svar!")
                InntektResultat(
                    data = inntekt,
                    statusCode = 200,
                    ""
                )
            },
            onFailure = { error ->
                println("Feil ved henting av utbetalinger")
                    error.printStackTrace()
                    InntektResultat (
                        data = null,
                        statusCode = 500,
                        errorMessage = "Feil ved lesing: ${error.message}"
                    )
            }
        )
    }
}