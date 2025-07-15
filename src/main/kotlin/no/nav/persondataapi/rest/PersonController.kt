package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.pdl.PdlClient
import no.nav.persondataapi.service.OppslagService
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
    private val tokenService: TokenService,
    private val pdlClient: PdlClient
) {
    @GetMapping("/pdl-token")
    @Protected
    fun pdlToken(@RequestHeader("fnr") fnr: String): String {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")

            val newToken = tokenService.exchangeToken(
                token,
                "api://dev-fss.pdl.pdl-api/.default"
            )
            val res = pdlClient.hentPerson(fnr,newToken)
            println(res)
            println("Hentet nytt token: $newToken")
            newToken
        }
    }
}