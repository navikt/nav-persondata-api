package no.nav.persondataapi.pensjonsgivendeInntekt

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.rest.oppslag.OppslagRequestDto
import no.nav.persondataapi.rest.oppslag.OppslagResponseDto
import no.nav.persondataapi.service.domain.pensjonsgivendeinntekt.PensjonsgivendeInntekt
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/oppslag/pensjonsgivende-inntekt")
@Tag(name = "PensjonsgivendeInntekt", description = "Endepunkter for oppslag av pensjonsgivende inntekt")
class PensjonsgivendeInntektController(
    private val pensjonsgivendeInntektService: PensjonsgivendeInntektService,
) {
    @Protected
    @PostMapping
    @Operation(
        summary = "Hent pensjonsgivende inntekt",
        description = "Henter pensjonsgivende inntekt for en person"
    )
    @RequestBody(
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
                description = "Pensjonsgivende inntekt hentet",
                content = [Content(
                    schema = Schema(implementation = OppslagResponseDto::class),
                    examples = [ExampleObject(
                        name = "Vellykket respons",
                        value = """{"data": {"inntekter": [{"år": 2024, "beløp": 520000, "kilde": "FiktivKilde"}]}, "error": null}"""
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
    fun hentPensjonsgivendeInntekt(
        @org.springframework.web.bind.annotation.RequestBody dto: OppslagRequestDto,
        @Parameter(description = "Om utvidet inntektsinformasjon skal hentes")
        @RequestParam(required = false, defaultValue = "false") utvidet: Boolean
    ): ResponseEntity<OppslagResponseDto<List<PensjonsgivendeInntekt>>> {
        return runBlocking {
            val resultat = pensjonsgivendeInntektService.hentPensjonsgivendeInntektForPerson(
                personIdent = dto.ident,
                utvidet = utvidet
            )

            when (resultat) {
                is PensjonsgivendeInntektResultat.Success -> {
                    ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
                }

                is PensjonsgivendeInntektResultat.IngenTilgang -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Ingen tilgang", data = null),
                        HttpStatus.FORBIDDEN
                    )
                }

                is PensjonsgivendeInntektResultat.PersonIkkeFunnet -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Person ikke funnet", data = null),
                        HttpStatus.NOT_FOUND
                    )
                }

                is PensjonsgivendeInntektResultat.FeilIBaksystem -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Feil i baksystem", data = null),
                        HttpStatus.BAD_GATEWAY
                    )
                }
            }
        }
    }
}