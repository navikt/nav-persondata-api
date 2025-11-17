package no.nav.persondataapi.rest.oppslag

import no.nav.persondataapi.rest.domene.Ytelse
import no.nav.persondataapi.service.YtelseService
import no.nav.persondataapi.service.YtelserResultat
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/oppslag/stønad")
class YtelseController(
	private val ytelseService: YtelseService,
) {
	@Protected
	@PostMapping
	fun hentYtelser(
		@RequestBody dto: OppslagRequestDto,
		@RequestParam(required = false, defaultValue = "false") utvidet: Boolean,
	): ResponseEntity<OppslagResponseDto<List<Ytelse>>> {
		val resultat = ytelseService.hentYtelserForPerson(dto.ident, utvidet)

		return when (resultat) {
			is YtelserResultat.Success -> {
				ResponseEntity.ok(OppslagResponseDto(data = resultat.data))
			}
			is YtelserResultat.IngenTilgang -> {
				ResponseEntity(
					OppslagResponseDto(error = "Ingen tilgang", data = null),
					HttpStatus.FORBIDDEN,
				)
			}
			is YtelserResultat.PersonIkkeFunnet -> {
				ResponseEntity(
					OppslagResponseDto(error = "Person ikke funnet", data = null),
					HttpStatus.NOT_FOUND,
				)
			}
			is YtelserResultat.FeilIBaksystem -> {
				ResponseEntity(
					OppslagResponseDto(error = "Feil i baksystem", data = null),
					HttpStatus.BAD_GATEWAY,
				)
			}
		}
	}
}
