package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
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
class PersonopplysningerController(
    private val personopplysningerService: PersonopplysningerService
) {
    @Protected
    @PostMapping
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

    companion object {
        private const val LOGG_HEADER = "logg"
    }
}
