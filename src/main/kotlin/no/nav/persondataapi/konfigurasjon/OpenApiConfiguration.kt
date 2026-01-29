package no.nav.persondataapi.konfigurasjon

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
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
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("NAV Persondata API")
                .description(
                    """
                    API for oppslag av persondata i NAV.
                    
                    Dette APIet tilbyr endepunkter for å hente:
                    - Personopplysninger (navn, adresse, kontaktinfo)
                    - Inntektshistorikk
                    - Arbeidsforhold
                    - Ytelser og utbetalinger
                    - AAP-data
                    - Meldekort
                    
                    Alle endepunkter krever gyldig Azure AD-token med riktige tilganger.
                    """.trimIndent()
                )
                .version("1.0.0")
                .contact(
                    Contact()
                        .name("Team Holmes")
                        .email("holmes@nav.no")
                )
        )
        .addServersItem(
            Server()
                .url("/")
                .description("Nåværende miljø")
        )
}
