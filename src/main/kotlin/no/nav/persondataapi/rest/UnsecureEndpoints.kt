package no.nav.persondataapi.rest

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping


@RequestMapping("/open")
class UnSecureedCpntroller {
    @GetMapping
    @Unprotected
    fun unprotectedPath() = "I am unprotected"
    }

