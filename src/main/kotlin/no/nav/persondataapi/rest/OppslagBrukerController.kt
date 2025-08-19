package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.rest.domain.OppslagBrukerRespons
import no.nav.persondataapi.service.OppslagService
import no.nav.persondataapi.service.ResponsMappingService
import no.nav.persondataapi.service.TokenService

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder


import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController


@RestController
class OppslagBrukerController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val oppslagService: OppslagService,
    private val tokenService: TokenService,
    private val mappingService: ResponsMappingService
) {

    @GetMapping("/oppslag-bruker")
    @Protected
    fun userInfo(@RequestHeader("fnr") fnr: String): OppslagBrukerRespons {
        return runBlocking {

            val grunnlag = oppslagService.hentGrunnlagsData(fnr)
            mappingService.mapToMOppslagBrukerResponse(grunnlag)
        }
    }
    @GetMapping("/utbetaling-token")
    @Protected
    fun utbetalingToken(@RequestHeader("fnr") fnr: String): String {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")

            val newToken = tokenService.exchangeToken(
                token,
                "api://dev-fss.okonomi.sokos-utbetaldata/.default"
            )
            println("Hentet nytt token: $newToken")
            newToken
        }
    }
}