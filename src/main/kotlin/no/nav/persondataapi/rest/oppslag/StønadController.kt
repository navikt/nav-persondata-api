package no.nav.persondataapi.rest.oppslag

import no.nav.persondataapi.rest.domene.Stønad
import no.nav.persondataapi.service.StønadResultat
import no.nav.persondataapi.service.StønadService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/stønad")
class StønadController(
    private val stønadService: StønadService
) {
    @Protected
    @PostMapping
    fun hentStønader(@RequestBody dto: OppslagRequestDto): ResponseEntity<OppslagResponseDto<List<Stønad>>> {
        val resultat = stønadService.hentStønaderForPerson(dto.ident)

        return when (resultat) {
            is StønadResultat.Success -> {
                ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
            }
            is StønadResultat.IngenTilgang -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Ingen tilgang", data = null),
                    HttpStatus.FORBIDDEN
                )
            }
            is StønadResultat.PersonIkkeFunnet -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Person ikke funnet", data = null),
                    HttpStatus.NOT_FOUND
                )
            }
            is StønadResultat.FeilIBaksystem -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Feil i baksystem", data = null),
                    HttpStatus.BAD_GATEWAY
                )
            }
        }
    }
}
