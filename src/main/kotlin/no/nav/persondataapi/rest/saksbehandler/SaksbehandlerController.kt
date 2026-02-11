package no.nav.persondataapi.rest.saksbehandler

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.rest.oppslag.OppslagResponseDto
import no.nav.persondataapi.service.SaksbehandlerService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/saksbehandler")
@Tag(name = "Saksbehandler", description = "Endepunkter for informasjon om innlogget saksbehandler")
class SaksbehandlerController(
    private val saksbehandlerService: SaksbehandlerService
) {
    @Protected
    @GetMapping
    @Operation(
        summary = "Hent saksbehandlerinformasjon",
        description = "Henter informasjon om innlogget saksbehandler, inkludert organisasjonstilhørighet"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Saksbehandlerinformasjon hentet",
                content = [Content(
                    schema = Schema(implementation = OppslagResponseDto::class),
                    examples = [ExampleObject(
                        name = "Vellykket respons",
                        value = """{"data": {"navIdent": "Z123456", "organisasjoner": ["NAV Arbeid og ytelser", "NAV Kontaktsenter"]}, "error": null}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke autentisert",
                content = [Content(examples = [ExampleObject(value = """{"data": null, "error": "Ikke autentisert"}""")])]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Intern feil",
                content = [Content(examples = [ExampleObject(value = """{"data": null, "error": "Intern feil"}""")])]
            )
        ]
    )
    fun hentSaksbehandler(): ResponseEntity<OppslagResponseDto<SaksbehandlerResponsDto>> {
        return runBlocking {
            val resultat = saksbehandlerService.hentSaksbehandler()

            if (resultat.data == null) {
                val feilmelding = resultat.errorMessage ?: "Ukjent feil"
                ResponseEntity(
                    OppslagResponseDto(error = feilmelding),
                    HttpStatus.valueOf(resultat.statusCode)
                )
            } else {
                ResponseEntity.ok(
                    OppslagResponseDto(
                        data = SaksbehandlerResponsDto(
                            navIdent = resultat.data.navIdent,
                            organisasjoner = resultat.data.organisasjoner
                        )
                    )
                )
            }
        }
    }

    data class SaksbehandlerResponsDto(
        val navIdent: String,
        val organisasjoner: List<String>
    )
}
