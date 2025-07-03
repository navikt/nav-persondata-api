package no.nav.persondataapi.rest


import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/secured")

class SecureedCpntroller {
    @GetMapping
    @Protected
    fun unprotectedPath() = "I am protected"
}
