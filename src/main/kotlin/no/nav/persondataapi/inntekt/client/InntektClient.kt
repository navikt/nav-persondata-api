package no.nav.persondataapi.inntekt.client

import jakarta.servlet.Filter
import no.nav.inntekt.generated.model.InntektshistorikkApiInn
import no.nav.inntekt.generated.model.InntektshistorikkApiUt
import no.nav.persondataapi.domain.InntektResultat

import no.nav.persondataapi.service.TokenService

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

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
    fun hentInntekter(fnr: String, token:String): InntektResultat {
        return runCatching {

            val requestBody = InntektshistorikkApiInn(
                personident = fnr,
                filter= "NAVKontrollA-Inntekt",
                formaal = "kontroll",
                maanedFom = "2020-01-01",
                maanedTom = "2025-01-01",
            )
            val oboToken = tokenService.exchangeToken(
                token,
                inntekt_scope
            )
            val response: InntektshistorikkApiUt = webClient.post()
                .header("Authorization","Bearer $oboToken")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<InntektshistorikkApiUt>() {})
                .block()!! // Bruk `awaitSingle()` hvis du er i coroutine-verden

            response
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