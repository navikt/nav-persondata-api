package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.service.TilgangService
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TilgangsmaskinController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val tilgangService: TilgangService
) {
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")
    private val log = LoggerFactory.getLogger(javaClass)
    @PostMapping("/tilgang")
    @Protected
    fun sjekkTilgang(@RequestBody dto: OppslagBrukerRequest): ResponseEntity<Void> = runBlocking {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")

        val groups = token.jwtTokenClaims.get("groups") as? List<String> ?: emptyList()

        return@runBlocking if (tilgangService.harUtvidetTilgang(groups)) {

            ResponseEntity.ok().build() // 204
        } else {
            val status = tilgangService.sjekkTilgang(dto.fnr, token.encodedToken)
            ResponseEntity.status(status).build()
        }
    }

}
