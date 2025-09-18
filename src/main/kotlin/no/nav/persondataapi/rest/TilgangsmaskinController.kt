package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.domain.TilgangResultat
import no.nav.persondataapi.tilgangsmaskin.client.TilgangsmaskinClient
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class TilgangsmaskinController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val tilgangsmaskinClient: TilgangsmaskinClient
) {
    @PostMapping("/tilgang")
    @Protected
    fun sjekkTilgang(@RequestBody dto: OppslagBrukerRequest): ResponseEntity<Void> {
        return runBlocking {
            val context = tokenValidationContextHolder.getTokenValidationContext()
            val token = context.firstValidToken?.encodedToken
                ?: throw IllegalStateException("Fant ikke gyldig token")

            //TODO: Sjekk rolle tilganger til bruker og sjekk om bruker har utvidet tilgang eller ikke.
            // Dersom bruker har utvidet tilgang, returner status 204
            val res = tilgangsmaskinClient.sjekkTilgang(dto.fnr,token)
            if (res.statusCode!=null) {
                ResponseEntity.status(res.data!!.status).build()
            }
            else{
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

        }
    }
}