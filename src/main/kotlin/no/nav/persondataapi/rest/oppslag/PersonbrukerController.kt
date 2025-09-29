package no.nav.persondataapi.rest.oppslag

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.service.BrukertilgangService
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
    @PostMapping
    fun hentStatusPåBrukeroppslag(@RequestBody dto: OppslagRequestDto): ResponseEntity<Void> {
        return runBlocking {
            val status  = brukertilgangService.hentStatusPåBruker(dto.ident)
            ResponseEntity.status(status).build();
        }
    }
}
