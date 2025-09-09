package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.UtbetalingResultat
import no.nav.persondataapi.utbetaling.client.UtbetalingClient
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class UtbetalingController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val utbetalingClient: UtbetalingClient
) {
    @GetMapping("/utbetalinger")
    @Protected
    fun hentUtbetalinger(@RequestHeader("fnr") fnr: String): UtbetalingResultat {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")
            val res = utbetalingClient.hentUtbetalingerForAktor(fnr,token)
            res
        }
    }
}