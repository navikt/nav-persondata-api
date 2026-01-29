package no.nav.persondataapi.rest.oppslag



import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.persondataapi.service.MeldekortResultat
import no.nav.persondataapi.service.MeldekortService
import no.nav.persondataapi.service.domain.DagpengerMeldekortDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/oppslag/meldekort")
@Tag(name = "Meldekort", description = "Endepunkter for oppslag av dagpenge-meldekort")
class MeldekortController(
    private val meldekortService: MeldekortService
) {
    @Protected
    @PostMapping
    @Operation(
        summary = "Hent dagpenge-meldekort",
        description = "Henter meldekort for dagpenger for en person"
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
                description = "Meldekort hentet",
                content = [Content(
                    schema = Schema(implementation = OppslagResponseDto::class),
                    examples = [ExampleObject(
                        name = "Vellykket respons",
                        value = """{"data": [{"uke": "2024-W03", "status": "INNSENDT"}], "error": null}"""
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
    ): ResponseEntity<OppslagResponseDto<List<DagpengerMeldekortDto>>> {
        val resultat = meldekortService.hentDagpengeMeldekortForPerson(dto.ident, utvidet)

        return when (resultat) {
            is MeldekortResultat.Success -> {
                ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
            }

            is MeldekortResultat.IngenTilgang -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Ingen tilgang", data = null),
                    HttpStatus.FORBIDDEN
                )
            }

            is MeldekortResultat.PersonIkkeFunnet -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Person ikke funnet", data = null),
                    HttpStatus.NOT_FOUND
                )
            }

            is MeldekortResultat.FeilIBaksystem -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Feil i baksystem", data = null),
                    HttpStatus.BAD_GATEWAY
                )
            }
        }
    }
}
