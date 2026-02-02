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
        key = "#personIdent + '_' + #utvidet",
        unless = "#result.statusCode != 200 && #result.statusCode != 404"
    )
    fun hentPensjonsgivendeInntekt(
        personIdent: PersonIdent,
        utvidet: Boolean,
    ): PensjonsgivendeInntektDataResultat {
        val inneværendeÅr = LocalDate.now().year
        val antallÅr: Long = if (utvidet) 10 else 3
        val oboToken = tokenService.getServiceToken(SCOPE.SIGRUN_SCOPE)

       /*
       * logikk for å hente ut data her
       * */

        val requestBody = SigrunPensjongivendeInntektRequest(
            inntektsaar = "2020",
            personident = personIdent.value,
            rettighetspakke = "")

        return runCatching {
            metrics.timer(operationName).recordCallable {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                val responseResult = webClient.post().uri("/api/v1/pensjonsgivendeinntektforfolketrygden")
                    .header("Authorization", "Bearer $oboToken")
                    .bodyValue(requestBody)
                    .exchangeToMono { response ->
                        val status = response.statusCode()
                        if (status.is2xxSuccessful) {
                            response.bodyToMono(String::class.java)
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
            println(respons)
            return PensjonsgivendeInntektDataResultat(
                data = PensjonsgivendeInntektRespons(emptyList()),
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

            log.error("Feil ved henting av aap-max: ${error.message}", error)
            return PensjonsgivendeInntektDataResultat(
                data = null, statusCode = 500, errorMessage = error.rootCause().message
            )
        })


        /*
    * mock respons her
    * */
        val inntekter = (1..antallÅr).map { index ->
            val år = inneværendeÅr - index
            val beløp = BigDecimal(480000 + (index * 12000))

            PensjonsgivendeInntekt(
                innteksår = år.toString(),
                skatteordning = Skatteordning("FASTLAND",år.toString(),listOf(Inntekt(InntektType.LØNN,beløp.toDouble())))
            )
        }

        return PensjonsgivendeInntektDataResultat(
            data = PensjonsgivendeInntektRespons(inntekter = inntekter),
            statusCode = 200
        )
    }
}

data class PensjonsgivendeInntektRespons(
    val inntekter: List<PensjonsgivendeInntekt>
)



data class PensjonsgivendeInntektDataResultat(
    val data: PensjonsgivendeInntektRespons?,
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
