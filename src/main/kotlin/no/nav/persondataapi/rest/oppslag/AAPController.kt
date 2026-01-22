package no.nav.persondataapi.rest.oppslag


import no.nav.persondataapi.integrasjon.aap.meldekort.client.AapClient
import no.nav.persondataapi.integrasjon.aap.meldekort.domene.Vedtak
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

        val resultat = aapClient.hentAapMax(personIdent = dto.ident,utvidet)

        when (resultat.statusCode){
            200 -> return ResponseEntity.ok(OppslagResponseDto(data = emptyList()))
            else -> return  ResponseEntity(
                OppslagResponseDto(error = "Feil i baksystem, ${resultat.statusCode}, ${resultat.data}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
}
