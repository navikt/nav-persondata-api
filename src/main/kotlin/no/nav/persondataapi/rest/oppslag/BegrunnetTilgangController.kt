package no.nav.persondataapi.rest.oppslag

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.persondataapi.service.BegrunnetTilgangService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/begrunnet-tilgang")
@Tag(name = "Begrunnet tilgang", description = "Endepunkter for logging av begrunnet tilgang til personopplysninger")
class BegrunnetTilgangController(
    private val begrunnetTilgangService: BegrunnetTilgangService
) {

    @Protected
    @PostMapping
    @Operation(
        summary = "Logg begrunnet tilgang",
        description = "Logger at saksbehandler har begrunnet sin tilgang til en persons opplysninger"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = [Content(
            examples = [ExampleObject(
                name = "Begrunnelse for tilgang",
                value = """{"ident": "12345678901", "begrunnelse": "Vurderer søknad om dagpenger", "mangel": "Mangler dokumentasjon"}"""
            )]
        )]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "202", description = "Begrunnelse registrert")
        ]
    )
    fun loggBegrunnetTilgang(@RequestBody dto: BegrunnelseRequestDto): ResponseEntity<Void> {
        begrunnetTilgangService.loggBegrunnetTilgang(personIdent = dto.ident, begrunnelse = dto.begrunnelse, mangel = dto.mangel)
        return ResponseEntity.accepted().build()
    }
}
