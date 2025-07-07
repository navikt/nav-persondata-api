package    no.nav.persondataapi.utbetaling.client
import no.nav.persondataapi.configuration.JsonUtils
import no.nav.persondataapi.domain.UtbetalingRespons
import no.nav.persondataapi.domain.UtbetalingResultat
import no.nav.persondataapi.utbetaling.dto.Utbetaling
import org.springframework.stereotype.Component

@Component
class UtbetalingClient {

    fun hentUtbetalingerForAktor(fnr: String): UtbetalingResultat {
        return runCatching {
            readJsonFileToDto<List<Utbetaling>>("$fnr.json")
        }.fold(
            onSuccess = { utbetalinger ->
                UtbetalingResultat(
                    data = UtbetalingRespons(utbetalinger = utbetalinger),
                    statusCode = 200
                )
            },
            onFailure = { error ->
                // 👇 Simulerer "ikke tilgang"-feil hvis fil ikke finnes
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

    private inline fun <reified T> readJsonFileToDto(filename: String): T {
        val json = object {}.javaClass.classLoader.getResource(filename)
            ?.readText(Charsets.UTF_8)
            ?: throw IllegalArgumentException("Finner ikke fil: $filename")

        return JsonUtils.fromJson(json)
    }
}
