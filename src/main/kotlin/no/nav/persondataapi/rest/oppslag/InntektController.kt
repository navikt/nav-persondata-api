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
import no.nav.persondataapi.rest.domene.InntektInformasjon
import no.nav.persondataapi.service.InntektResultat
import no.nav.persondataapi.service.InntektService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/oppslag/inntekt")
@Tag(name = "Inntekt", description = "Endepunkter for oppslag av inntektshistorikk")
class InntektController(
    private val inntektService: InntektService
) {
    @Protected
    @PostMapping
    @Operation(
        summary = "Hent inntektshistorikk",
        description = "Henter inntektshistorikk for en person fra A-ordningen"
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
                description = "Inntektshistorikk hentet",
                content = [Content(
                    schema = Schema(implementation = OppslagResponseDto::class),
                    examples = [ExampleObject(
                        name = "Vellykket respons",
                        value = """{"data": {"inntekter": [{"beloep": 50000, "periode": "2024-01"}]}, "error": null}"""
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
    fun hentInntekter(
        @RequestBody dto: OppslagRequestDto,
        @Parameter(description = "Om utvidet inntektsinformasjon skal hentes")
        @RequestParam(required = false, defaultValue = "false") utvidet: Boolean
    ): ResponseEntity<OppslagResponseDto<InntektInformasjon>> {
        return runBlocking {
            val resultat = inntektService.hentInntekterForPerson(personIdent = dto.ident, utvidet = utvidet)

            when (resultat) {
                is InntektResultat.Success -> {
                    ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
                }
                is InntektResultat.IngenTilgang -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Ingen tilgang", data = null),
                        HttpStatus.FORBIDDEN
                    )
                }
                is InntektResultat.PersonIkkeFunnet -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Person ikke funnet", data = null),
                        HttpStatus.NOT_FOUND
                    )
                }
                is InntektResultat.FeilIBaksystem -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Feil i baksystem", data = null),
                        HttpStatus.BAD_GATEWAY
                    )
                }
            }
        }
    }
}
