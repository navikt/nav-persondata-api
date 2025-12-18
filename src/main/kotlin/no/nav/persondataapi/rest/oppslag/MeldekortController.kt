package no.nav.persondataapi.rest.oppslag


import no.nav.persondataapi.service.MeldekortDto
import no.nav.persondataapi.service.MeldekortResultat
import no.nav.persondataapi.service.MeldekortService
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
class MeldekortController(
    private val meldekortService: MeldekortService
) {
    @Protected
    @PostMapping
    fun hentMeldekort(
        @RequestBody dto: OppslagRequestDto,
        @RequestParam(required = false, defaultValue = "false") utvidet: Boolean
    ): ResponseEntity<OppslagResponseDto<List<MeldekortDto>>> {
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
