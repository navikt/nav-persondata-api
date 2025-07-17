package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.InntektResultat
import no.nav.persondataapi.domain.PersonDataResultat
import no.nav.persondataapi.domain.UtbetalingResultat
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.inntekt.client.InntektClient
import no.nav.persondataapi.pdl.PdlClient
import no.nav.persondataapi.service.OppslagService
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import no.nav.persondataapi.utbetaling.client.UtbetalingClient
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class inntektController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val inntektClient: InntektClient
) {
    @GetMapping("/inntekt")
    @Protected
    fun hentInnekter(@RequestHeader("fnr") fnr: String): InntektResultat {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")
            val res = inntektClient.hentInntekter(fnr,token)
            res
        }
    }
}