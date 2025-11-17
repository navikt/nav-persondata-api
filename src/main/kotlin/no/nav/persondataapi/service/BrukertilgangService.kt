package no.nav.persondataapi.service

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Service

@Service
class BrukertilgangService(
	val tokenValidationContextHolder: TokenValidationContextHolder,
	val tilgangService: TilgangService,
) {
	fun harSaksbehandlerTilgangTilPersonIdent(personIdent: PersonIdent): Boolean = hentTilgangsvurdering(personIdent).status == 200

	fun hentTilgangsvurdering(personIdent: PersonIdent): BrukertilgangVurdering {
		val context = tokenValidationContextHolder.getTokenValidationContext()
		val token = context.firstValidToken ?: throw IllegalStateException("Fant ikke gyldig token")

		val groups = token.jwtTokenClaims.get("groups") as? List<String> ?: emptyList()

		val resultat = tilgangService.hentTilgangsresultat(personIdent, token.encodedToken)
		val beregnetStatus = tilgangService.beregnStatus(resultat)
		val harUtvidetTilgang = tilgangService.harUtvidetTilgang(groups)

		return BrukertilgangVurdering(
			status = if (harUtvidetTilgang) 200 else beregnetStatus,
			tilgang = resultat.data?.title ?: "OK",
			harUtvidetTilgang = harUtvidetTilgang,
		)
	}
}

data class BrukertilgangVurdering(
	val status: Int,
	val tilgang: String,
	val harUtvidetTilgang: Boolean,
)
