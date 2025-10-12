package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.rest.domene.InntektInformasjon
import no.nav.persondataapi.service.InntektResultat
import no.nav.persondataapi.service.InntektService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/inntekt")
class InntektController(
    private val inntektService: InntektService
) {
    @Protected
    @PostMapping
    fun hentInntekter(@RequestBody dto: OppslagRequestDto): ResponseEntity<OppslagResponseDto<InntektInformasjon>> {
        return runBlocking {
            val resultat = inntektService.hentInntekterForPerson(dto.ident.value)

            when (resultat) {
                is InntektResultat.Success -> {
                    ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
                }
                is InntektResultat.IngenTilgang -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Ingen tilgang", data = null),
                        HttpStatus.FORBIDDEN
                    )
                }
                is InntektResultat.PersonIkkeFunnet -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Person ikke funnet", data = null),
                        HttpStatus.NOT_FOUND
                    )
                }
                is InntektResultat.FeilIBaksystem -> {
                    ResponseEntity(
                        OppslagResponseDto(error = "Feil i baksystem", data = null),
                        HttpStatus.BAD_GATEWAY
                    )
                }
            }
        }
    }
}
