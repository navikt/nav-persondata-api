package no.nav.persondataapi.integrasjon.utbetaling.client

import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import no.nav.persondataapi.integrasjon.utbetaling.dto.Utbetaling
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.UtbetalingMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.util.concurrent.TimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import no.nav.persondataapi.konfigurasjon.RetryPolicy

@Component
class UtbetalingClient(
    private val tokenService: TokenService,
    @Qualifier("utbetalingWebClient")
    private val webClient: WebClient,
    private val metrics: UtbetalingMetrics,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val operationName = "hentUtbetalinger"

    @Cacheable(
        value = ["utbetaling-bruker"],
        key = "#personIdent + '_' + #utvidet",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    fun hentUtbetalingerForBruker(personIdent: PersonIdent, utvidet: Boolean): UtbetalingResultat {
        return try {
            // Selve kallet måles i timeren
            val utbetalinger: List<Utbetaling>? = metrics
                .timer(operationName)
                .recordCallable {

                    val antallÅr: Long = if (utvidet) 10 else 3
                    val requestBody = RequestBody(
                        ident = personIdent.value,
                        rolle = "RETTIGHETSHAVER",
                        periode = Periode(LocalDate.now().minusYears(antallÅr), LocalDate.now()),
                        periodetype = "UTBETALINGSPERIODE"
                    )

                    val oboToken = tokenService.getServiceToken(SCOPE.UTBETALING_SCOPE)

                    webClient.post()
                        .header("Authorization", "Bearer $oboToken")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(object : ParameterizedTypeReference<List<Utbetaling>>() {})
                        .retryWhen(RetryPolicy.reactorRetrySpec(kilde = "UtbetalingHistorikk"))
                        .block()!! // API-kontrakt: forventer alltid en liste
                }

            // Kallet kom helt frem og tilbake uten exception → SUCCESS
            metrics.counter(operationName, DownstreamResult.SUCCESS).increment()

            UtbetalingResultat(
                data = UtbetalingRespons(utbetalinger = utbetalinger),
                statusCode = 200
            )
        } catch (e: Exception) {
            val resultType = when {
                erTimeout(e) -> DownstreamResult.TIMEOUT
                e.message?.contains("ikke tilgang", ignoreCase = true) == true ->
                    DownstreamResult.CLIENT_ERROR
                else -> DownstreamResult.UNEXPECTED
            }

            metrics.counter(operationName, resultType).increment()

            log.error("Feil ved henting av utbetalinger for $personIdent: ${e.message}", e)

            when (resultType) {
                DownstreamResult.TIMEOUT -> UtbetalingResultat(
                    data = null,
                    statusCode = 504,
                    errorMessage = "Timeout mot utbetalingstjenesten"
                )

                DownstreamResult.CLIENT_ERROR -> UtbetalingResultat(
                    data = null,
                    statusCode = 401,
                    errorMessage = "Ingen tilgang til utbetalinger for $personIdent"
                )

                else -> UtbetalingResultat(
                    data = null,
                    statusCode = 500,
                    errorMessage = "Feil ved lesing: ${e.message}"
                )
            }
        }
    }

    private fun erTimeout(e: Throwable): Boolean =
        when (e.cause) {
            is TimeoutException -> true
            is ReadTimeoutException -> true
            is WriteTimeoutException -> true
            else -> false
        }
}

data class UtbetalingRespons(
    val utbetalinger: List<Utbetaling>?
)

data class UtbetalingResultat(
    val data: UtbetalingRespons?,
    val statusCode: Int,               // f.eks. 200, 401, 500, 504
    val errorMessage: String? = null
)

data class RequestBody(
    val ident: String,
    val rolle: String,
    val periode: Periode,
    val periodetype: String
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
)
