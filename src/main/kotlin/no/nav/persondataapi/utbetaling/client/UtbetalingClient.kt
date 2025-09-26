package    no.nav.persondataapi.utbetaling.client
import com.fasterxml.jackson.databind.JsonNode
import no.nav.persondataapi.configuration.JsonUtils
import no.nav.persondataapi.domain.UtbetalingRespons
import no.nav.persondataapi.domain.UtbetalingResultat
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService

import no.nav.persondataapi.utbetaling.dto.Utbetaling
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

@Component
class UtbetalingClient(
    private val tokenService: TokenService,
    @Qualifier("utbetalingWebClient")
    private val webClient: WebClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun hentUtbetalingerForAktor(fnr: String): UtbetalingResultat {
        return runCatching {

            val requestBody = RequestBody(
                ident = fnr,
                rolle = "RETTIGHETSHAVER",
                periode = Periode(LocalDate.now().minusYears(3), LocalDate.now()),
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
                if (error.message?.contains("ikke tilgang") == true || fnr == "00000000000") {
                    UtbetalingResultat(
                        data = null,
                        statusCode = 401,
                        errorMessage = "Ingen tilgang til utbetalinger for $fnr"
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
