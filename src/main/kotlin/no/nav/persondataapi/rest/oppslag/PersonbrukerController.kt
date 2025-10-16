package no.nav.persondataapi.rest.oppslag

import no.nav.persondataapi.service.BrukertilgangService
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/personbruker")
class PersonbrukerController(
    val brukertilgangService: BrukertilgangService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    @Protected
    @PostMapping
    fun hentStatusPåBrukeroppslag(@RequestBody dto: OppslagRequestDto): ResponseEntity<Void> {
        val status = brukertilgangService.hentStatusPåBruker(dto.ident.value)
        if (status == 404) {
            logger.info("Fant ikke bruker ${dto.ident}")
            return ResponseEntity.notFound().build()
        }
        logger.info("Fant bruker ${dto.ident}")
        return ResponseEntity.ok().build();
    }
}
