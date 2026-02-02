package no.nav.persondataapi.pensjonsgivendeInntekt

import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapMeldekortRespons
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.AapMaximumRequest
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.AapMaximumRespons
import no.nav.persondataapi.konfigurasjon.RetryPolicy
import no.nav.persondataapi.konfigurasjon.rootCause
import no.nav.persondataapi.metrics.DPDatadelingMetrics
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.SigrunMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.Inntekt
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.InntektType
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.PensjonsgivendeInntekt
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.Skatteordning
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeoutException

@Component
class PensjonsgivendeInntektClient (
    @param:Qualifier("sigrunClient") private val webClient: WebClient,
    private val metrics: SigrunMetrics,
    private val tokenService: TokenService,
){

    private val log = LoggerFactory.getLogger(javaClass)
    private val operationName = "pensjonsgivendeinntektforfolketrygden"


    @Cacheable(
        value = ["pensjonsgivende-inntekt"],
        key = "#personIdent + '_' + #inntektsaar",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    fun hentPensjonsgivendeInntekt(
        personIdent: PersonIdent,
        utvidet: Boolean,
        inntektsaar: Int = 2020
    ): PensjonsgivendeInntektDataResultat {
        val oboToken = tokenService.getServiceToken(SCOPE.SIGRUN_SCOPE)

       /*
       * logikk for å hente ut data her
       * */

        val requestBody = SigrunPensjongivendeInntektRequest(
            inntektsaar = inntektsaar.toString(),
            personident = personIdent.value,
            rettighetspakke = "navkontroll")

        return runCatching {
            metrics.timer(operationName).recordCallable {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                val responseResult = webClient.post().uri("/api/v1/pensjonsgivendeinntektforfolketrygden")
                    .header("Authorization", "Bearer $oboToken")
                    .bodyValue(requestBody)
                    .exchangeToMono { response ->
                        when {
                            response.statusCode().is2xxSuccessful -> {

                                response.bodyToMono(SigrunPensjonsgivendeInntektResponse::class.java)
                                    .map {
                                        println(it)
                                        PensjonsgivendeInntektDataResultat(
                                            data = it,
                                            statusCode = 200
                                        )
                                    }
                            }

                            response.statusCode().value() == 404 -> {
                                Mono.just(
                                    PensjonsgivendeInntektDataResultat(
                                        data = SigrunPensjonsgivendeInntektResponse(inntektsaar = inntektsaar.toString(),emptyList()),
                                        statusCode = 404,
                                        errorMessage = null
                                    )
                                )
                            }

                            else -> {
                                response.bodyToMono(String::class.java).flatMap { body ->
                                    Mono.error(
                                        RuntimeException(
                                            "Feil fra Sigrun: HTTP ${response.statusCode()} – $body"
                                        )
                                    )
                                }
                            }
                        }
                    }
                    .retryWhen(RetryPolicy.reactorRetrySpec(kilde = "sigrun"))
                    .block()!!

                responseResult
            }
        }.fold(onSuccess = { respons ->
            metrics.counter(operationName, DownstreamResult.SUCCESS).increment()
            return PensjonsgivendeInntektDataResultat(
                data = respons.data,
                statusCode = 200,
                errorMessage = null
            )

        }, onFailure = { error ->
            val resultType = when {
                erTimeout(error) -> DownstreamResult.TIMEOUT
                error.message?.contains("ikke tilgang", ignoreCase = true) == true -> DownstreamResult.CLIENT_ERROR
                else -> DownstreamResult.UNEXPECTED
            }

            metrics.counter(operationName, resultType).increment()

            log.error("Feil ved henting av sigrun: ${error.message}", error)
            return PensjonsgivendeInntektDataResultat(
                data = null, statusCode = 500, errorMessage = error.rootCause().message
            )
        })

}

data class PensjonsgivendeInntektRespons(
    val inntekter: List<PensjonsgivendeInntekt>
)



data class PensjonsgivendeInntektDataResultat(
    val data: SigrunPensjonsgivendeInntektResponse?,
    val statusCode: Int,
    val errorMessage: String? = null,
)

data class SigrunPensjongivendeInntektRequest(
    val inntektsaar:String,
    val personident: String,
    val rettighetspakke : String
)
private fun erTimeout(e: Throwable): Boolean = when (e.cause) {
    is TimeoutException -> true
    is ReadTimeoutException -> true
    is WriteTimeoutException -> true
    else -> false
    }
}
