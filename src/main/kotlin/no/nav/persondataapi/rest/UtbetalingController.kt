package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.UtbetalingResultat
import no.nav.persondataapi.utbetaling.client.UtbetalingClient
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class UtbetalingController(
    private val utbetalingClient: UtbetalingClient
) {
    @GetMapping("/utbetalinger")
    @Protected
    fun hentUtbetalinger(@RequestHeader("fnr") fnr: String): UtbetalingResultat {
        return runBlocking {
            utbetalingClient.hentUtbetalingerForBruker(fnr)
        }
    }
}
