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
import no.nav.persondataapi.rest.domene.tilV1Format
import no.nav.persondataapi.service.PersonopplysningerResultat
import no.nav.persondataapi.service.PersonopplysningerService
import no.nav.persondataapi.unleash.FeatureToggleService
import no.nav.persondataapi.unleash.Toggle
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
    private val personopplysningerService: PersonopplysningerService,
    private val featureToggleService: FeatureToggleService,
) {
    @Protected
    @PostMapping
    @Operation(
        summary = "Hent personopplysninger",
        description = "Henter personopplysninger for en person basert på fødselsnummer eller annen identifikator",
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = [
            Content(
                examples = [
                    ExampleObject(
                        name = "Standard oppslag",
                        value = """{"ident": "12345678901"}""",
                    ),
                ],
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Personopplysninger hentet",
                content = [
                    Content(
                        schema = Schema(implementation = OppslagResponseDto::class),
                        examples = [
                            ExampleObject(
                                name = "Vellykket respons",
                                value =
                                    """{"data": {"navn": "Ola Nordmann"}, "error": null}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ingen tilgang til personen",
                content = [
                    Content(
                        examples = [ExampleObject(value = """{"data": null, "error": "Ingen tilgang"}""")],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Person ikke funnet",
                content = [
                    Content(
                        examples = [ExampleObject(value = """{"data": null, "error": "Person ikke funnet"}""")],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "502",
                description = "Feil i baksystem",
                content = [
                    Content(
                        examples = [ExampleObject(value = """{"data": null, "error": "Feil i baksystem"}""")],
                    ),
                ],
            ),
        ],
    )
    fun hentPersonopplysninger(
        @RequestBody dto: OppslagRequestDto,
        @RequestHeader(name = LOGG_HEADER, required = false) traceHeader: String?,
    ): ResponseEntity<OppslagResponseDto<Any>> =
        runBlocking {
            val logResponsAktivert = traceHeader?.toBoolean() ?: false
            val resultat = personopplysningerService.hentPersonopplysningerForPerson(dto.ident, logResponsAktivert)

            when (resultat) {
                is PersonopplysningerResultat.Success -> {
                    val nyStruktur = featureToggleService.isEnabled(Toggle.WATSON_SOK_V_1_2)
                    val data: Any = if (nyStruktur) resultat.data else resultat.data.tilV1Format()
                    ResponseEntity.ok(OppslagResponseDto(data = data))
                }

                is PersonopplysningerResultat.IngenTilgang -> {
                    ResponseEntity<OppslagResponseDto<Any>>(
                        OppslagResponseDto(error = "Ingen tilgang", data = null),
                        HttpStatus.FORBIDDEN,
                    )
                }

                is PersonopplysningerResultat.PersonIkkeFunnet -> {
                    ResponseEntity<OppslagResponseDto<Any>>(
                        OppslagResponseDto(error = "Person ikke funnet", data = null),
                        HttpStatus.NOT_FOUND,
                    )
                }

                is PersonopplysningerResultat.FeilIBaksystem -> {
                    ResponseEntity<OppslagResponseDto<Any>>(
                        OppslagResponseDto(error = "Feil i baksystem", data = null),
                        HttpStatus.BAD_GATEWAY,
                    )
                }
            }
        }
}
