package no.nav.persondataapi.integrasjon.utbetaling.client
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService

import no.nav.persondataapi.integrasjon.utbetaling.dto.Utbetaling
import no.nav.persondataapi.rest.domene.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

@Component
class UtbetalingClient(
    private val tokenService: TokenService,
    @param:Qualifier("utbetalingWebClient")
    private val webClient: WebClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Cacheable(value = ["utbetaling-bruker"], key = "#personIdent")
    fun hentUtbetalingerForBruker(personIdent: PersonIdent, utvidet: Boolean): UtbetalingResultat {
        return runCatching {
            val antallÅr: Long = if (utvidet) 10 else 3
            val requestBody = RequestBody(
                ident = personIdent.value,
                rolle = "RETTIGHETSHAVER",
                periode = Periode(LocalDate.now().minusYears(antallÅr), LocalDate.now()),
                periodetype = "UTBETALINGSPERIODE"
            )
            val oboToken = tokenService.getServiceToken(
                SCOPE.UTBETALING_SCOPE
            )
            val response: List<Utbetaling> = webClient.post()
                .header("Authorization","Bearer $oboToken")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<Utbetaling>>() {})
                .block()!! // Bruk `awaitSingle()` hvis du er i coroutine-verden

            response
        }.fold(
            onSuccess = { utbetalinger ->
                UtbetalingResultat(
                    data = UtbetalingRespons(utbetalinger = utbetalinger),
                    statusCode = 200
                )
            },
            onFailure = { error ->
                log.error("Feil ved henting av utbetalinger", error)
                if (error.message?.contains("ikke tilgang") == true) {
                    UtbetalingResultat(
                        data = null,
                        statusCode = 401,
                        errorMessage = "Ingen tilgang til utbetalinger for $personIdent"
                    )
                } else {
                    UtbetalingResultat(
                        data = null,
                        statusCode = 500,
                        errorMessage = "Feil ved lesing: ${error.message}"
                    )
                }
            }
        )
    }
}

data class UtbetalingRespons(val utbetalinger: List<no.nav.persondataapi.integrasjon.utbetaling.dto.Utbetaling>)

data class UtbetalingResultat(
    val data: UtbetalingRespons?,
    val statusCode: Int,               // f.eks. 200, 401, 500
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
