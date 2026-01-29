package no.nav.persondataapi.konfigurasjon

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

/**
 * Sikkerhetskonfigurasjon for token-validering.
 *
 * Swagger UI og OpenAPI-dokumentasjon er unntatt fra autentisering
 * via ignorePathPatterns slik at utviklere kan se API-dokumentasjonen.
 */
@EnableJwtTokenValidation(
    ignore = [
        "org.springdoc",
        "org.springframework"
    ]
)
@Configuration
class SecurityConfiguration
