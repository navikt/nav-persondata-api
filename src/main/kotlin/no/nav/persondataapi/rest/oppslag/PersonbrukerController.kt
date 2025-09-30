package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.service.BrukertilgangService
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
    private val log = LoggerFactory.getLogger(javaClass)
    @PostMapping
    fun hentStatusPåBrukeroppslag(@RequestBody dto: OppslagRequestDto): ResponseEntity<Void> {
        val status = brukertilgangService.hentStatusPåBruker(dto.ident)
        log.info("Hentet status for bruker ${dto.ident}: $status")
        return ResponseEntity.status(status).build()
    }
}
