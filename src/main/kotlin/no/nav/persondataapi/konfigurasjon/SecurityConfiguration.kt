package no.nav.persondataapi.konfigurasjon

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

/**
 * Sikkerhetskonfigurasjon for token-validering.
 *
 * Swagger UI og OpenAPI-dokumentasjon er unntatt fra autentisering
 * ved å ignorere klasser i org.springdoc- og org.springframework-pakkene
 *
 */
@EnableJwtTokenValidation(
    ignore = [
        "org.springdoc",
        "org.springframework"
    ]
)
@Configuration
class SecurityConfiguration
