package no.nav.persondataapi.rest.oppslag

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapClient
import no.nav.persondataapi.service.AAPMeldekortResultat
import no.nav.persondataapi.service.MeldekortService
import no.nav.persondataapi.service.domain.AapMeldekortDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/oppslag/aap-meldekort")
@Tag(name = "AAP", description = "Endepunkter for oppslag av AAP-meldekort og vedtak")
class AAPController(
    private val meldekortService: MeldekortService,
    private val aapClient: AapClient
) {
    @Protected
    @PostMapping
    @Operation(
        summary = "Hent AAP-meldekort",
        description = "Henter meldekort og vedtak for arbeidsavklaringspenger (AAP) for en person"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = [Content(
            examples = [ExampleObject(
                name = "Standard oppslag",
                value = """{"ident": "12345678901"}"""
            )]
        )]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "AAP-meldekort hentet",
                content = [Content(
                    schema = Schema(implementation = OppslagResponseDto::class),
                    examples = [ExampleObject(
                        name = "Vellykket respons",
                        value = """{"data": [{"vedtaksperiode": "2024-01-01/2024-06-30", "status": "AKTIV"}], "error": null}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ingen tilgang til personen",
                content = [Content(examples = [ExampleObject(value = """{"data": null, "error": "Ingen tilgang"}""")])]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Person ikke funnet",
                content = [Content(examples = [ExampleObject(value = """{"data": null, "error": "Person ikke funnet"}""")])]
            ),
            ApiResponse(
                responseCode = "502",
                description = "Feil i baksystem",
                content = [Content(examples = [ExampleObject(value = """{"data": null, "error": "Feil i baksystem"}""")])]
            )
        ]
    )
    fun hentMeldekort(
        @RequestBody dto: OppslagRequestDto,
        @Parameter(description = "Om utvidet meldekortinformasjon skal hentes")
        @RequestParam(required = false, defaultValue = "false") utvidet: Boolean
    ): ResponseEntity<OppslagResponseDto<List<AapMeldekortDto>>> {

        return runBlocking {
        val resultat = meldekortService.hentAAPMeldekortForPerson(personIdent = dto.ident,utvidet)

        when (resultat) {
            is AAPMeldekortResultat.Success -> {
                ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
            }
            is AAPMeldekortResultat.IngenTilgang -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Ingen tilgang", data = null),
                    HttpStatus.FORBIDDEN
                )
            }
            is AAPMeldekortResultat.PersonIkkeFunnet -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Person ikke funnet", data = null),
                    HttpStatus.NOT_FOUND
                )
            }
            is AAPMeldekortResultat.FeilIBaksystem -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Feil i baksystem", data = null),
                    HttpStatus.BAD_GATEWAY
                )
            }
        }
        }
    }
}
