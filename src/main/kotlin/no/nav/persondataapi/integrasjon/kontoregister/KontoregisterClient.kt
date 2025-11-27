package no.nav.persondataapi.integrasjon.kontoregister

import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException

import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.KontoregisterMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable

import org.springframework.http.ResponseEntity
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

    @Cacheable(
        value = ["kontonummer-persom"],
        key = "#personIdent",
        unless = "#result.statusCode != 200"
    )
    fun hentKontoMedKontoHistorikk(personIdent: PersonIdent): KontoRegisterResultat {

        return try {
            val resultat: ResponseEntity<KontoRegisterRespons>? = metrics
                .timer(operationName)
                .recordCallable { hentFraKontoregister(personIdent) }

            val status = resultat?.statusCode?.value()
            val body = resultat?.body

            when (status) {
                200 -> {
                    metrics.counter(operationName, DownstreamResult.SUCCESS).increment()
                    KontoRegisterResultat(data = body, statusCode = 200)
                }
                204 -> {
                    metrics.counter(operationName, DownstreamResult.SUCCESS).increment()
                    KontoRegisterResultat(data = body, statusCode = 204)
                }

                404 -> {
                    metrics.counter(operationName, DownstreamResult.CLIENT_ERROR).increment()
                    KontoRegisterResultat(
                        data = null,
                        statusCode = 404,
                        errorMessage = "Kontohistorikk ikke funnet for ${personIdent.value}"
                    )
                }

                401, 403 -> {
                    metrics.counter(operationName, DownstreamResult.CLIENT_ERROR).increment()
                    KontoRegisterResultat(
                        data = null,
                        statusCode = 401,
                        errorMessage = "Ingen tilgang til kontoregister for ${personIdent.value}"
                    )
                }

                else -> {
                    metrics.counter(operationName, DownstreamResult.UNEXPECTED).increment()
                    KontoRegisterResultat(
                        data = null,
                        statusCode = 500,
                        errorMessage = "Uventet statuskode $status fra kontoregister"
                    )
                }
            }

        } catch (e: Exception) {

            val resultType = when {
                erTimeout(e) -> DownstreamResult.TIMEOUT
                e.message?.contains("ikke tilgang", ignoreCase = true) == true ->
                    DownstreamResult.CLIENT_ERROR
                else -> DownstreamResult.UNEXPECTED
            }

            metrics.counter(operationName, resultType).increment()
            log.error("Feil ved kontoregister-kall for $personIdent: ${e.message}", e)

            when (resultType) {
                DownstreamResult.TIMEOUT ->
                    KontoRegisterResultat(null, 504, "Timeout mot kontoregister")

                DownstreamResult.CLIENT_ERROR ->
                    KontoRegisterResultat(null, 401, "Ingen tilgang til kontoregister")

                else ->
                    KontoRegisterResultat(null, 500, e.message)
            }
        }
    }

    /**
     * Utfører selve WebClient-kallet, men returnerer ResponseEntity
     * med både body OG statuskode.
     */
    private fun hentFraKontoregister(personIdent: PersonIdent): ResponseEntity<KontoRegisterRespons> {
        val token = tokenService.getServiceToken(SCOPE.KONTOREGISTER_SCOPE)

        val requestBody = RequestBody(kontohaver = personIdent.value)

        return webClient.post()
            .header("Authorization", "Bearer $token")
            .bodyValue(requestBody)
            .exchangeToMono { response ->

                val status = response.statusCode()

                if (status.is2xxSuccessful) {
                    response.bodyToMono(KontoRegisterRespons::class.java)
                        .defaultIfEmpty(KontoRegisterRespons(null,emptyList()))
                        .map { body -> ResponseEntity.status(status).body(body) }
                } else {
                    response.bodyToMono(KontoRegisterRespons::class.java)
                        .defaultIfEmpty(KontoRegisterRespons(null,emptyList()))
                        .map { body -> ResponseEntity.status(status).body(body) }
                }
            }
            .block() ?: ResponseEntity.status(500).body(null)
    }

    private fun erTimeout(e: Throwable): Boolean =
        when (e) {
            is TimeoutException,
            is ReadTimeoutException,
            is WriteTimeoutException,
            is java.net.ConnectException,
            is io.netty.channel.ConnectTimeoutException -> true
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
