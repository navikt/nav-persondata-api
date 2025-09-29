package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.PersonDataResultat
import no.nav.persondataapi.pdl.client.PdlClient
import no.nav.persondataapi.service.TilgangsmaskinClient

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

import org.springframework.web.bind.annotation.RestController

@RestController
class TestingPersonController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val pdlClient: PdlClient,
    private val tilgangsmaskinClient: TilgangsmaskinClient
) {
    @PostMapping("/personsoek")
    @Protected
    fun finnBruker(@RequestBody dto: FinnBrukerRequest): PersonDataResultat {

        println("Received ${dto.fnr}")
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")

            val res = pdlClient.hentPersonv2(dto.fnr)
            res
        }
    }
}
data class FinnBrukerRequest(val fnr: String)
