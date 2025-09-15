package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.PersonDataResultat
import no.nav.persondataapi.pdl.client.PdlClient
import no.nav.persondataapi.tilgangsmaskin.client.TilgangsmaskinClient
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class PersonController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val pdlClient: PdlClient,
    private val tilgangsmaskinClient: TilgangsmaskinClient
) {
    @GetMapping("/persondata")
    @Protected
    fun hentPerson(@RequestHeader("fnr") fnr: String): PersonDataResultat {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")
            val res = pdlClient.hentPersonv2(fnr)
            res
        }
    }
    @PostMapping("/person-søk")
    @Protected
    fun søkPerson(@RequestHeader("fnr") fnr: String): PersonDataResultat {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")

            val res = pdlClient.hentPersonv2(fnr)
            res
        }
    }
    @GetMapping("/me")
    @Protected
    fun me(): String {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            context.firstValidToken!!.encodedToken
        }
    }
}