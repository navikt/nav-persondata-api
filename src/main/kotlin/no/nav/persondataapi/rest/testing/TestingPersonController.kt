package no.nav.persondataapi.rest.testing

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.PersonDataResultat
import no.nav.persondataapi.pdl.client.PdlClient

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

import org.springframework.web.bind.annotation.RestController

@RestController
class TestingPersonController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val pdlClient: PdlClient,
) {
    @PostMapping("/personsoek")
    @Protected
    fun finnBruker(@RequestBody dto: FinnBrukerRequest): PersonDataResultat {

        println("Received ${dto.fnr}")
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")

            pdlClient.hentPerson(dto.fnr)
        }
    }
}
data class FinnBrukerRequest(val fnr: String)
