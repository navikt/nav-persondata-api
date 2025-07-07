package no.nav.persondataapi.utbetaling.client

import no.nav.persondataapi.configuration.JsonUtils
import no.nav.persondataapi.domain.UtbetalingRespons
import no.nav.persondataapi.utbetaling.dto.Utbetaling
import org.springframework.stereotype.Component

@Component
class UtbetalingClient {

    fun hentUtbetalingerForAktor(fnr: String): UtbetalingRespons {
        return runCatching {
            readJsonFileToDto<List<Utbetaling>>("$fnr.json")
        }.fold(
            onSuccess = { utbetalinger ->
                UtbetalingRespons(status = true, utbetalinger = utbetalinger)
            },
            onFailure = { error ->
                println("Feil ved lesing av utbetalinger for $fnr: ${error.message}")
                UtbetalingRespons(status = false, utbetalinger = emptyList())
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
