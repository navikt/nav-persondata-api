package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.service.OppslagService
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
    private val tokenService: TokenService
) {

    @GetMapping("/oppslag-bruker")
    @Protected
    fun userInfo(@RequestHeader("fnr") fnr: String): GrunnlagsData {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val issuer = context.issuers.first()
            val claims = context.getClaims(issuer)
            val username = claims.getStringClaim("NAVident")

            println("Bruker $username gjorde oppslag på fnr: $fnr")

            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")

            val newToken = tokenService.exchangeToken(
                token,
                "api://dev-fss.okonomi.sokos-utbetaldata/.default)"
            )

            println("Hentet nytt token: $newToken")

            oppslagService.hentGrunnlagsData(fnr, username)
        }
    }
}