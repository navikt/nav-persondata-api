package no.nav.persondataapi.integrasjon.krr.client

import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import no.nav.persondataapi.konfigurasjon.RetryPolicy
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.KrrMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.UUID
import java.util.concurrent.TimeoutException

@Component
class KrrClient(
    private val tokenService: TokenService,
    @param:Qualifier("krrWebClient")
    private val webClient: WebClient,
    private val metrics: KrrMetrics,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val operationName = "hentKontaktinformasjon"

    fun hentKontaktinformasjon(personIdent: PersonIdent): KrrDataResultat =
        runCatching {
            val serviceToken = tokenService.getServiceToken(SCOPE.KRR_SCOPE)

            webClient
                .get()
                .uri("/rest/v1/person")
                .header("Authorization", "Bearer $serviceToken")
                .header("Nav-Personident", personIdent.value)
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { response ->
                    response.bodyToMono(String::class.java).map { body ->
                        WebClientResponseException(
                            response.statusCode().value(),
                            "Feil fra KRR: $body",
                            null,
                            null,
                            null,
                        )
                    }
                }.bodyToMono(KrrPerson::class.java)
                .retryWhen(RetryPolicy.reactorRetrySpec(kilde = "KRR"))
                .block()
        }.fold(
            onSuccess = { person ->
                metrics.counter(operationName, DownstreamResult.SUCCESS).increment()
                KrrDataResultat(epost = person?.epostadresse)
            },
            onFailure = { error ->
                val resultType =
                    when {
                        erTimeout(error) -> {
                            DownstreamResult.TIMEOUT
                        }

                        error is WebClientResponseException && error.statusCode.value() == 404 -> {
                            DownstreamResult.CLIENT_ERROR
                        }

                        else -> {
                            DownstreamResult.UNEXPECTED
                        }
                    }
                metrics.counter(operationName, resultType).increment()
                log.warn("Feil ved henting av kontaktinformasjon fra KRR: ${error.message}")
                KrrDataResultat(epost = null)
            },
        )

    private fun erTimeout(e: Throwable): Boolean =
        when (e.cause) {
            is TimeoutException -> true
            is ReadTimeoutException -> true
            is WriteTimeoutException -> true
            else -> false
        }
}

data class KrrPerson(
    val epostadresse: String? = null,
    val mobiltelefonnummer: String? = null,
    val reservert: Boolean = false,
    val kanVarsles: Boolean = false,
)

data class KrrDataResultat(
    val epost: String?,
)
