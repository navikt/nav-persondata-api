package no.nav.persondataapi.integrasjon.modiacontextholder.client

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class ModiaContextHolderClient(
	private val tokenService: TokenService,
	private val tokenValidationContextHolder: TokenValidationContextHolder,
	@Qualifier("modiaContextHolderWebClient")
	private val webClient: WebClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun settModiakontekst(personIdent: PersonIdent) {
		val token =
			tokenValidationContextHolder.getTokenValidationContext().firstValidToken
				?: throw IllegalStateException("Fant ikke gyldig token")
		val oboToken = tokenService.exchangeToken(token.encodedToken, SCOPE.MODIA_CONTEXT_HOLDER_SCOPE)

		val requestBody =
			mapOf(
				"eventType" to "NY_AKTIV_BRUKER",
				"verdi" to personIdent.value,
			)

		webClient
			.post()
			.uri("/api/context")
			.header("Authorization", "Bearer $oboToken")
			.bodyValue(requestBody)
			.retrieve()
			.bodyToMono<ModiaContextHolderResponse>()
			.doOnError { ex ->
				log.error("Feil ved oppdatering av modiakontekst", ex)
			}.doOnSuccess { _ ->
				log.info("Modiakontekst satt for bruker ${personIdent.value}")
			}.block() // fortsatt blocking
	}
}

data class ModiaContextHolderResponse(
	val aktivBruker: String?,
	val aktivGruppeId: String?,
	val aktivEnhet: String?,
)
