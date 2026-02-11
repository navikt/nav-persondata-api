package no.nav.persondataapi.integrasjon.aap.meldekort.client

import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.AapMaximumRespons
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.AapMaximumRequest
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.Vedtak
import no.nav.persondataapi.konfigurasjon.RetryPolicy
import no.nav.persondataapi.konfigurasjon.rootCause
import no.nav.persondataapi.metrics.AAPMetrics
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeoutException

@Component
class AapClient(
    @param:Qualifier("aapWebClient") private val webClient: WebClient,
    private val metrics: AAPMetrics,
    private val tokenService: TokenService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val operationName = "max"


    @Cacheable(
        value = ["aap"],
        key = "#personIdent + '_' + #utvidet",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    fun hentAapMax(
        personIdent: PersonIdent,
        utvidet: Boolean,
    ): AapMeldekortRespons {
        val antallÅr: Long = if (utvidet) 10 else 3
        val oboToken = tokenService.getServiceToken(SCOPE.AAP_SCOPE)
        return runCatching {
            metrics.timer(operationName).recordCallable {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                val requestBody = AapMaximumRequest(
                    personidentifikator = personIdent.value,
                    fraOgMedDato = LocalDate.now().minusYears(antallÅr).format(formatter),
                    tilOgMedDato = LocalDate.now().format(formatter),
                )

                val responseResult = webClient.post().uri("/maksimum")
                    .header("Authorization", "Bearer $oboToken")
                    .bodyValue(requestBody)
                    .exchangeToMono { response ->
                        val status = response.statusCode()
                        if (status.is2xxSuccessful) {
                            response.bodyToMono(object : ParameterizedTypeReference<AapMaximumRespons>() {})
                        } else {
                            response.bodyToMono(String::class.java).map { body ->
                                throw RuntimeException("Feil fra AAP maksimum: HTTP $status – $body")
                            }
                        }
                    }.retryWhen(RetryPolicy.reactorRetrySpec(kilde = "aap-maksimum")).block()!!

                responseResult
            }
        }.fold(onSuccess = { respons ->
            metrics.counter(operationName, DownstreamResult.SUCCESS).increment()
            AapMeldekortRespons(
                data = respons.vedtak,
                statusCode = 200,
                message = null
            )
        }, onFailure = { error ->
            val resultType = when {
                erTimeout(error) -> DownstreamResult.TIMEOUT
                error.message?.contains("ikke tilgang", ignoreCase = true) == true -> DownstreamResult.CLIENT_ERROR
                else -> DownstreamResult.UNEXPECTED
            }

            metrics.counter(operationName, resultType).increment()

            log.error("Feil ved henting av aap-max: ${error.message}", error)
            return AapMeldekortRespons(
                data = null, statusCode = 500, message = error.rootCause().message
            )
        })
    }

    private fun erTimeout(e: Throwable): Boolean = when (e.cause) {
        is TimeoutException -> true
        is ReadTimeoutException -> true
        is WriteTimeoutException -> true
        else -> false
    }
}

data class AapMeldekortRespons(
    val data: List<Vedtak>? = null,
    val statusCode: Int,
    val message: String?
)
