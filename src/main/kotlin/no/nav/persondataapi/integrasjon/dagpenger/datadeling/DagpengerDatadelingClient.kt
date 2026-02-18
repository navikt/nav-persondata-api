package no.nav.persondataapi.integrasjon.dagpenger.datadeling

import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Meldekort
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.MeldekortRequest
import no.nav.persondataapi.konfigurasjon.RetryPolicy
import no.nav.persondataapi.konfigurasjon.rootCause
import no.nav.persondataapi.metrics.DPDatadelingMetrics
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.responstracing.erTraceLoggingAktvert
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import no.nav.persondataapi.tracelogging.traceLoggHvisAktivert
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeoutException

@Component
class DagpengerDatadelingClient(
    @param:Qualifier("dpDatadelingClient") private val webClient: WebClient,
    private val metrics: DPDatadelingMetrics,
    private val tokenService: TokenService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val operationName = "meldekort"


    @Cacheable(
        value = ["meldekort"],
        key = "#personIdent + '_' + #utvidet",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    fun hentDagpengeMeldekort(
        personIdent: PersonIdent,
        utvidet: Boolean,
    ): DagpengerMeldekortRespons {
        val antallÅr: Long = if (utvidet) 10 else 3
        val oboToken = tokenService.getServiceToken(SCOPE.DP_DATADELING_SCOPE)
        return runCatching {
            metrics.timer(operationName).recordCallable {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                val requestBody = MeldekortRequest(
                    personIdent = personIdent.value,
                    fraOgMedDato = LocalDate.now().minusYears(antallÅr).format(formatter),
                    tilOgMedDato = LocalDate.now().format(formatter),
                )
                traceLoggHvisAktivert(
                    logger = log,
                    kilde = "Dagpenger - request",
                    personIdent=personIdent,
                    unit = requestBody
                )
                

                val responseResult = webClient.post().uri("/dagpenger/datadeling/v1/meldekort")
                    .header("Authorization", "Bearer $oboToken")
                    .bodyValue(requestBody)
                    .exchangeToMono { response ->
                        val status = response.statusCode()
                        if (status.is2xxSuccessful) {
                            response.bodyToMono(object : ParameterizedTypeReference<List<Meldekort>>() {})
                        } else {
                            response.bodyToMono(String::class.java).map { body ->
                                throw RuntimeException("Feil fra DPDatadeling: HTTP $status – $body")
                            }
                        }
                    }.retryWhen(RetryPolicy.reactorRetrySpec(kilde = "meldekort")).block()!!

                responseResult
            }
        }.fold(onSuccess = { inntekt ->
            metrics.counter(operationName, DownstreamResult.SUCCESS).increment()
            DagpengerMeldekortRespons(
                data = inntekt,
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

            log.error("Feil ved henting av dagpenger-meldekort: ${error.message}", error)
            return DagpengerMeldekortRespons(
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

data class DagpengerMeldekortRespons(
    val data: List<Meldekort>? = null,
    val statusCode: Int,
    val message: String?
)
