package no.nav.persondataapi.rest.oppslag

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.responstracing.LOGG_HEADER
import no.nav.persondataapi.rest.domene.PersonInformasjon
import no.nav.persondataapi.service.PersonopplysningerResultat
import no.nav.persondataapi.service.PersonopplysningerService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/personopplysninger")
@Tag(name = "Personopplysninger", description = "Endepunkter for oppslag av personopplysninger")
class PersonopplysningerController(
    private val personopplysningerService: PersonopplysningerService
) {
    @Protected
    @PostMapping
    @Operation(
        summary = "Hent personopplysninger",
        description = "Henter personopplysninger for en person basert på fødselsnummer eller annen identifikator"
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
                description = "Personopplysninger hentet",
                content = [Content(
                    schema = Schema(implementation = OppslagResponseDto::class),
                    examples = [ExampleObject(
                        name = "Vellykket respons",
                        value = """{"data": {"navn": "Ola Nordmann", "fodselsdato": "1990-01-15"}, "error": null}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ingen tilgang til personen",
                content = [Content(
                    examples = [ExampleObject(value = """{"data": null, "error": "Ingen tilgang"}""")]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Person ikke funnet",
                content = [Content(
                    examples = [ExampleObject(value = """{"data": null, "error": "Person ikke funnet"}""")]
                )]
            ),
            ApiResponse(
                responseCode = "502",
                description = "Feil i baksystem",
                content = [Content(
                    examples = [ExampleObject(value = """{"data": null, "error": "Feil i baksystem"}""")]
                )]
            )
        ]
    )
    fun hentPersonopplysninger(
        @RequestBody dto: OppslagRequestDto,
        @RequestHeader(name = LOGG_HEADER, required = false) traceHeader: String?
    ): ResponseEntity<OppslagResponseDto<PersonInformasjon>> {
        return runBlocking {
            val logResponsAktivert = traceHeader?.toBoolean() ?: false
            val resultat = personopplysningerService.hentPersonopplysningerForPerson(dto.ident, logResponsAktivert)

            when (resultat) {
                is PersonopplysningerResultat.Success -> {
                    ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
                }
                is PersonopplysningerResultat.IngenTilgang -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Ingen tilgang", data = null),
                        HttpStatus.FORBIDDEN
                    )
                }
                is PersonopplysningerResultat.PersonIkkeFunnet -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Person ikke funnet", data = null),
                        HttpStatus.NOT_FOUND
                    )
                }
                is PersonopplysningerResultat.FeilIBaksystem -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Feil i baksystem", data = null),
                        HttpStatus.BAD_GATEWAY
                    )
                }
            }
        }
    }

}
