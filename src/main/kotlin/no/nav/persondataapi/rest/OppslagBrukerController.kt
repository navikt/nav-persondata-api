package no.nav.persondataapi.rest

import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.service.OppslagService

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController


@RestController
class OppslagBrukerController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val oppslagService: OppslagService
) {
    @GetMapping("/oppslag-bruker")
    @Protected
    suspend fun userInfo(@RequestHeader("fnr") fnr: String): GrunnlagsData {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val issuer = context.issuers.first()
        val claims = context.getClaims(issuer)

        val username = claims.getStringClaim("NAVident")

        println("Bruker $username gjorde oppslag på fnr: $fnr")
        val response = oppslagService.hentGrunnlagsData(fnr, username)
        return response
    }

}