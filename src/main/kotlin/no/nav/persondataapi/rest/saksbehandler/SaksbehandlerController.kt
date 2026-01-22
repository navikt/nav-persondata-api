package no.nav.persondataapi.rest.saksbehandler

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
class SaksbehandlerController(
    private val saksbehandlerService: SaksbehandlerService
) {
    /**
     * Henter saksbehandlerens organisasjonstilhørighet.
     */
    @Protected
    @GetMapping
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
