package no.nav.persondataapi.rest

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SecureController(
    private val contextHolder: TokenValidationContextHolder


) {

    @GetMapping("/secured")
    @Protected
    fun userInfo(): String {
        val context = contextHolder.getTokenValidationContext()
        val issuer = context.issuers.first()
        val claims = context.getClaims(issuer)

        val username = claims.getStringClaim("NAVident") // eller preferred_username
        return "Du er logget inn som $username"
    }
}