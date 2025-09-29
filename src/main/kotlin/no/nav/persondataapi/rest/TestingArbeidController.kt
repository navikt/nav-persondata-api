package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.aareg.client.AaregClient
import no.nav.persondataapi.domain.AaregDataResultat
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class TestingArbeidController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val aaregClient: AaregClient
) {
    @GetMapping("/arbeid")
    @Protected
    fun hentIArbeid(@RequestHeader("fnr") fnr: String): AaregDataResultat {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")
            val res = aaregClient.hentArbeidsforhold(fnr)
            res
        }
    }
}
