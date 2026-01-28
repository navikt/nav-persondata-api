package no.nav.persondataapi.rest.oppslag


import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapClient
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.Vedtak
import no.nav.persondataapi.service.AAPMeldekortResultat
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
@RequestMapping("/oppslag/aap-meldekort")
class AAPController(
    private val meldekortService: MeldekortService,
    private val aapClient: AapClient
) {
    @Protected
    @PostMapping
    fun hentMeldekort(
        @RequestBody dto: OppslagRequestDto,
        @RequestParam(required = false, defaultValue = "false") utvidet: Boolean
    ): ResponseEntity<OppslagResponseDto<List<Vedtak>>> {

        return runBlocking {
        val resultat = meldekortService.hentAAPMeldekortForPerson(personIdent = dto.ident,utvidet)

        when (resultat) {
            is AAPMeldekortResultat.Success -> {
                ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
            }
            is AAPMeldekortResultat.IngenTilgang -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Ingen tilgang", data = null),
                    HttpStatus.FORBIDDEN
                )
            }
            is AAPMeldekortResultat.PersonIkkeFunnet -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Person ikke funnet", data = null),
                    HttpStatus.NOT_FOUND
                )
            }
            is AAPMeldekortResultat.FeilIBaksystem -> {
                ResponseEntity(
                    OppslagResponseDto(error = "Feil i baksystem", data = null),
                    HttpStatus.BAD_GATEWAY
                )
            }
        }
        }
    }
}
