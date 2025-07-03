package no.nav.persondataapi.rest


import no.nav.security.token.support.core.api.Unprotected

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/open")
class UnSecureedCpntroller {
    @GetMapping
    @Unprotected
    fun unprotectedPath() = "I am unprotected"
    }

