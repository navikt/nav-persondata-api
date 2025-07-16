package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.PersonDataResultat
import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.pdl.PdlClient
import no.nav.persondataapi.service.OppslagService
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
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
class PersonController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val pdlClient: PdlClient
) {
    @GetMapping("/persondata")
    @Protected
    fun hentPerson(@RequestHeader("fnr") fnr: String): PersonDataResultat {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")
            val res = pdlClient.hentPersonv2(fnr,token)
            res
        }
    }
}