package no.nav.persondataapi.rest.meg

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.rest.oppslag.OppslagResponseDto
import no.nav.persondataapi.service.MegService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/meg")
class MegController(
    private val megService: MegService
) {
    /**
     * Henter saksbehandlerens organisasjonstilhørighet.
     */
    @Protected
    @GetMapping
    fun hentMeg(): ResponseEntity<OppslagResponseDto<MegResponsDto>> {
        return runBlocking {
            val resultat = megService.hentMeg()

            if (resultat.data == null) {
                val feilmelding = resultat.errorMessage ?: "Ukjent feil"
                ResponseEntity(
                    OppslagResponseDto(error = feilmelding),
                    HttpStatus.valueOf(resultat.statusCode)
                )
            } else {
                ResponseEntity.ok(
                    OppslagResponseDto(
                        data = MegResponsDto(
                            navIdent = resultat.data.navIdent,
                            organisasjoner = resultat.data.organisasjoner
                        )
                    )
                )
            }
        }
    }

    data class MegResponsDto(
        val navIdent: String,
        val organisasjoner: List<String>
    )
}
