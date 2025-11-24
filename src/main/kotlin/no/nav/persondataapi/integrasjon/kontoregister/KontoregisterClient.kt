package no.nav.persondataapi.integrasjon.kontoregister

import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import no.nav.persondataapi.integrasjon.utbetaling.dto.Utbetaling
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.KontoregisterMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.TimeoutException

@Component
class KontoregisterClient(
    private val tokenService: TokenService,
    @Qualifier("kontoregisterWebClient")
    private val webClient: WebClient,
    private val metrics: KontoregisterMetrics,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val operationName = "hent-konto-med-historikk"

    @Cacheable(value = ["kontonummer-persom"], key = "#personIdent", unless = "#result.statusCode != 200")
    fun hentKontoMedKontoHistorikk(personIdent: PersonIdent):KontoRegisterResultat  {
        return try {
            // Selve kallet måles i timeren
            val kontohistorikk: KontoRegisterRespons? = metrics
                .timer(operationName)
                .recordCallable {

                    val requestBody = RequestBody(
                        kontohaver = personIdent.value
                    )

                    val oboToken = tokenService.getServiceToken(SCOPE.KONTOREGISTER_SCOPE)

                    webClient.post()
                        .header("Authorization", "Bearer $oboToken")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(object : ParameterizedTypeReference<KontoRegisterRespons>() {})
                        .block()!! // API-kontrakt: forventer alltid en liste
                }

            // Kallet kom helt frem og tilbake uten exception → SUCCESS
            metrics.counter(operationName, DownstreamResult.SUCCESS).increment()

            KontoRegisterResultat(
                data = kontohistorikk,
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
                DownstreamResult.TIMEOUT -> KontoRegisterResultat(
                    data = null,
                    statusCode = 504,
                    errorMessage = "Timeout mot utbetalingstjenesten"
                )

                DownstreamResult.CLIENT_ERROR -> KontoRegisterResultat(
                    data = null,
                    statusCode = 401,
                    errorMessage = "Ingen tilgang til utbetalinger for $personIdent"
                )

                else -> KontoRegisterResultat(
                    data = null,
                    statusCode = 500,
                    errorMessage = "Feil ved lesing: ${e.message}"
                )
            }
        }
    }

    private fun erTimeout(e: Throwable): Boolean =
        when (e) {
            is TimeoutException -> true
            is ReadTimeoutException -> true
            is WriteTimeoutException -> true
            else -> false
        }
}

data class KontoRegisterRespons(
    val aktivKonto: KontoInfo?,
    val kontohistorikk: List<KontoHistorikkEntry>
)

data class KontoInfo(
    val kontohaver: String,
    val kontonummer: String,
    val utenlandskKontoInfo: UtenlandskKontoInfo?,
    val gyldigFom: String?,
    val opprettetAv: String?,
    val kilde: String?,
    val karInfo: KarInfo?
)

data class KontoHistorikkEntry(
    val kontohaver: String,
    val kontonummer: String,
    val utenlandskKontoInfo: UtenlandskKontoInfo?,
    val gyldigFom: String?,
    val gyldigTom: String?,
    val endretAv: String?,
    val endretAvKilde: String?,
    val opprettetAv: String?,
    val kilde: String?,
    val karInfo: KarInfo?
)

data class UtenlandskKontoInfo(
    val banknavn: String?,
    val bankkode: String?,
    val bankLandkode: String?,
    val valutakode: String?,
    val swiftBicKode: String?,
    val bankadresse1: String?,
    val bankadresse2: String?,
    val bankadresse3: String?
)

data class KarInfo(
    val kontoEiesAvPerson: String?,
    val valideringskode: String?,
    val karDatoTid: String?
)

data class KontoRegisterResultat(
    val data: KontoRegisterRespons?,
    val statusCode: Int,               // f.eks. 200, 401, 500, 504
    val errorMessage: String? = null
)

data class RequestBody(
    val kontohaver: String
)
