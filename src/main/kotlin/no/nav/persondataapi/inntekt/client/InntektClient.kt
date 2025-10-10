package no.nav.persondataapi.inntekt.client

import no.nav.inntekt.generated.model.InntektshistorikkApiInn
import no.nav.inntekt.generated.model.InntektshistorikkApiUt
import no.nav.persondataapi.domain.InntektDataResultat
import no.nav.persondataapi.domain.KontrollPeriode
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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


    ) {
    private val log = LoggerFactory.getLogger(javaClass)
    // Felles tags for denne klienten


    fun hentInntekter(fnr: String, kontrollPeriode: KontrollPeriode = KontrollPeriode(LocalDate.now().minusYears(5),
        LocalDate.now())): InntektDataResultat {

        return runCatching {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM")

            val requestBody = InntektshistorikkApiInn(
                personident = fnr,
                filter= "NAVKontrollA-Inntekt",
                formaal = "NAVKontroll",
                maanedFom = kontrollPeriode.fom.format(formatter),
                maanedTom = kontrollPeriode.tom.format(formatter),
            )
            val oboToken = tokenService.getServiceToken(SCOPE.INNTEKT_SCOPE)


            val responseResult = webClient.post()
                .uri("/rest/v2/inntektshistorikk")
                .header("Authorization", "Bearer $oboToken")
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .bodyValue(requestBody)
                .exchangeToMono { response ->
                    val status = response.statusCode()
                    if (status.is2xxSuccessful) {
                        response.bodyToMono(object : ParameterizedTypeReference<InntektshistorikkApiUt>() {})
                    } else {
                        response.bodyToMono(String::class.java).map { body ->

                            throw RuntimeException("Feil fra inntektsAPI: HTTP $status – $body")
                        }
                    }
                }.block()!!
            responseResult
        }.fold(
            onSuccess = { inntekt ->
                InntektDataResultat(
                    data = inntekt,
                    statusCode = 200,
                    ""
                )
            },
            onFailure = { error ->
                log.error("Feil ved henting av utbetalinger", error)
                    InntektDataResultat (
                        data = null,
                        statusCode = 500,
                        errorMessage = "Feil ved lesing: ${error.message}"
                    )
            }
        )
    }

}
