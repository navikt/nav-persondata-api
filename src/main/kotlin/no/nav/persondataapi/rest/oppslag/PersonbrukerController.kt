package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.BrukertilgangService
import no.nav.persondataapi.service.PersonopplysningerService
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/personbruker")
class PersonbrukerController(
    val brukertilgangService: BrukertilgangService,
    val personopplysningerService: PersonopplysningerService,
    val tokenValidationContextHolder: TokenValidationContextHolder,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    @Protected
    @PostMapping
    fun sjekkOmBrukerEksisterer(@RequestBody dto: OppslagRequestDto): ResponseEntity<Void> {
        val token = tokenValidationContextHolder.getTokenValidationContext().firstValidToken
        val saksbehandlerIdent = token!!.jwtTokenClaims.get("NAVident").toString()
        logger.info("Saksbehandler $saksbehandlerIdent slår opp person ${dto.ident}")

        val status = brukertilgangService.hentStatusPåBruker(dto.ident)
        if (status == 403) {
            logger.info("Fant bruker ${dto.ident}, men saksbehandler har ikke tilgang til å se all informasjon")
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build()
        }
        return runBlocking {
            if (!personopplysningerService.finnesPerson(dto.ident)) {
                logger.info("Fant ikke bruker ${dto.ident}")
                ResponseEntity.notFound().build()
            } else {
                logger.info("Fant bruker ${dto.ident}")
                ResponseEntity.ok().build();
            }
        }
    }
}
