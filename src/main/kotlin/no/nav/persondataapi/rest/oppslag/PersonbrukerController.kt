package no.nav.persondataapi.rest.oppslag

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.integrasjon.modiacontextholder.client.ModiaContextHolderClient
import no.nav.persondataapi.service.RevisjonsloggService
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.service.PersonopplysningerService
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/personbruker")
@Tag(name = "Personbruker", description = "Endepunkter for å sjekke eksistens og tilgang til en person")
class PersonbrukerController(
    val brukertilgangService: BrukertilgangService,
    val personopplysningerService: PersonopplysningerService,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val auditService: RevisjonsloggService,
    val modiaContextHolderClient: ModiaContextHolderClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Protected
    @PostMapping
    @Operation(
        summary = "Sjekk eksistens og tilgang",
        description = "Sjekker om en person eksisterer og om saksbehandler har tilgang til å se informasjon om personen"
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
                description = "Person funnet og saksbehandler har tilgang",
                content = [Content(
                    schema = Schema(implementation = PersonbrukerResponseDto::class),
                    examples = [ExampleObject(
                        name = "Full tilgang",
                        value = """{"tilgang": "FULL", "harUtvidetTilgang": true}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "206",
                description = "Person funnet, men saksbehandler har begrenset tilgang",
                content = [Content(
                    schema = Schema(implementation = PersonbrukerResponseDto::class),
                    examples = [ExampleObject(
                        name = "Begrenset tilgang",
                        value = """{"tilgang": "BEGRENSET", "harUtvidetTilgang": false}"""
                    )]
                )]
            ),
            ApiResponse(responseCode = "404", description = "Person ikke funnet")
        ]
    )
    fun hentEksistensOgTilgang(@RequestBody dto: OppslagRequestDto): ResponseEntity<PersonbrukerResponseDto> {
        val token = tokenValidationContextHolder.getTokenValidationContext().firstValidToken
        val saksbehandlerIdent = token!!.jwtTokenClaims.get("NAVident").toString()
        logger.info("Saksbehandler $saksbehandlerIdent slår opp person ${dto.ident}")

        val tilgangsvurdering = brukertilgangService.hentTilgangsvurdering(dto.ident)
        if (tilgangsvurdering.status == 403) {
            logger.info("Fant bruker ${dto.ident}, men saksbehandler har ikke tilgang til å se all informasjon")
            return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .body(PersonbrukerResponseDto(tilgang = tilgangsvurdering.tilgang, harUtvidetTilgang = tilgangsvurdering.harUtvidetTilgang))
        }
        return runBlocking {
            if (!personopplysningerService.finnesPerson(dto.ident)) {
                logger.info("Fant ikke bruker ${dto.ident}")
                ResponseEntity.notFound().build()
            } else {
                logger.info("Fant bruker ${dto.ident}")
                auditService.tilgangOppslagGodkjent(dto.ident,saksbehandlerIdent)
                try {
                    modiaContextHolderClient.settModiakontekst(dto.ident)
                } catch (e: Exception) {
                    logger.error("Feil ved oppdatering av modiakontekst for bruker ${dto.ident}", e)
                }
                ResponseEntity.ok(PersonbrukerResponseDto(tilgang = tilgangsvurdering.tilgang, harUtvidetTilgang = tilgangsvurdering.harUtvidetTilgang))
            }
        }
    }

    data class PersonbrukerResponseDto(
        val tilgang: String,
        val harUtvidetTilgang: Boolean
    )
}
