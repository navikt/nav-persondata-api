package no.nav.persondataapi.rest.oppslag

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
class ArbeidsforholdController(
    private val arbeidsforholdService: ArbeidsforholdService
) {
    @Protected
    @PostMapping
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
