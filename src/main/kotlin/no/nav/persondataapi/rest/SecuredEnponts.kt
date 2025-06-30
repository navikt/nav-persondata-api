package no.nav.persondataapi.rest

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/me")
class WhoamiController {

    @GetMapping
    suspend fun whoami(@AuthenticationPrincipal principal: Jwt): Map<String, Any?> {
        return mapOf(
            "NAVident" to principal.getClaimAsString("NAVident"),
            "azp_name" to principal.getClaimAsString("azp_name"),
            "oid" to principal.getClaimAsString("oid"),
            "sub" to principal.subject,
            "aud" to principal.audience,
            "roles" to principal.getClaimAsStringList("roles")
        )
    }
}
