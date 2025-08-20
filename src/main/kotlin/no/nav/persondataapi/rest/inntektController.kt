package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.InntektDataResultat
import no.nav.persondataapi.inntekt.client.InntektClient
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class inntektController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val inntektClient: InntektClient
) {
    @GetMapping("/inntekt")
    @Protected
    fun hentInnekter(@RequestHeader("fnr") fnr: String): InntektDataResultat {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")
            val res = inntektClient.hentInntekter(fnr,token)
            res
        }
    }
}