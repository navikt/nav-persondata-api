package no.nav.persondataapi.rest.oppslag

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.rest.domene.ArbeidsgiverInformasjon
import no.nav.persondataapi.service.ArbeidsforholdResultat
import no.nav.persondataapi.service.ArbeidsforholdService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/arbeidsforhold")
@Tag(name = "Arbeidsforhold", description = "Endepunkter for oppslag av arbeidsforhold fra Aa-registeret")
class ArbeidsforholdController(
    private val arbeidsforholdService: ArbeidsforholdService
) {
    @Protected
    @PostMapping
    @Operation(
        summary = "Hent arbeidsforhold",
        description = "Henter arbeidsforhold og arbeidsgivere for en person fra Aa-registeret"
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
                description = "Arbeidsforhold hentet",
                content = [Content(
                    schema = Schema(implementation = OppslagResponseDto::class),
                    examples = [ExampleObject(
                        name = "Vellykket respons",
                        value = """{"data": {"arbeidsgivere": [{"navn": "NAV", "orgnummer": "889640782"}]}, "error": null}"""
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
    fun hentArbeidsforhold(@RequestBody dto: OppslagRequestDto): ResponseEntity<OppslagResponseDto<ArbeidsgiverInformasjon>> {
        return runBlocking {
            val resultat = arbeidsforholdService.hentArbeidsforholdForPerson(personIdent = dto.ident)

            when (resultat) {
                is ArbeidsforholdResultat.Success -> {
                    ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
                }
                is ArbeidsforholdResultat.IngenTilgang -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Ingen tilgang", data = null),
                        HttpStatus.FORBIDDEN
                    )
                }
                is ArbeidsforholdResultat.PersonIkkeFunnet -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Person ikke funnet", data = null),
                        HttpStatus.NOT_FOUND
                    )
                }
                is ArbeidsforholdResultat.FeilIBaksystem -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Feil i baksystem", data = null),
                        HttpStatus.BAD_GATEWAY
                    )
                }
            }
        }
    }
}
