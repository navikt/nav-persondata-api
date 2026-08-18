package no.nav.persondataapi.konfigurasjon

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Konfigurasjon for OpenAPI/Swagger UI.
 *
 * Denne klassen definerer metadata for API-dokumentasjonen,
 * inkludert tittel, beskrivelse og kontaktinformasjon.
 */
@Configuration
class OpenApiConfiguration {
    @Value("\${application.name:nav-persondata-api}")
    private lateinit var applicationName: String

    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Nav Persondata API")
                    .description(
                        """
                        API for oppslag av persondata i Nav.
                        
                        Dette APIet tilbyr endepunkter for å hente:
                        - Personopplysninger (navn, adresse, kontaktinfo)
                        - Inntektshistorikk
                        - Arbeidsforhold
                        - Ytelser og utbetalinger
                        - AAP-data
                        - Meldekort
                        
                        ## Autentisering
                        Alle endepunkter krever gyldig Azure AD-token med riktige tilganger.
                        
                        Trykk «Authorize» og lim inn et rått JWT-token (uten "Bearer "-prefiks).
                        I dev/local kan et M2M-token hentes fra:
                        `https://azure-token-generator.intern.dev.nav.no/api/m2m?aud=<cluster>:<team>:<app>`
                        """.trimIndent(),
                    ).version("1.0.0")
                    .contact(
                        Contact()
                            .name("Team Holmes"),
                    ),
            ).addServersItem(
                Server()
                    .url("/")
                    .description("Nåværende miljø"),
            ).components(
                Components()
                    .addSecuritySchemes(
                        "azure-ad",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description(
                                "Azure AD autentisering. Lim inn et gyldig JWT-token (M2M eller OBO) uten \"Bearer \"-prefiks.",
                            ),
                    ),
            ).addSecurityItem(SecurityRequirement().addList("azure-ad"))
}
