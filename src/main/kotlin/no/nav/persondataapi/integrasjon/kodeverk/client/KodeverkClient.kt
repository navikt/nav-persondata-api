package no.nav.persondataapi.integrasjon.kodeverk.client

import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class KodeverkClient(
	private val tokenService: TokenService,
	@param:Qualifier("kodeverkWebClient")
	private val webClient: WebClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Cacheable(value = ["kodeverk-landkoder"])
	fun hentLandkoder(): List<Landkode> {
		val token = tokenService.getServiceToken(SCOPE.KODEVERK_SCOPE)

		return webClient
			.get()
			.uri("/api/v1/kodeverk/Landkoder/koder/betydninger?spraak=nb")
			.header("Authorization", "Bearer $token")
			.retrieve()
			.bodyToMono<KodeverkResponse>()
			.doOnError { ex ->
				log.error("Feil ved henting av landkoder fra kodeverk", ex)
			}.onErrorResume { _ ->
				// Returner en tom respons dersom noe feiler
				Mono.just(KodeverkResponse(emptyMap()))
			}.block() // fortsatt blocking
			?.let { response ->
				log.info("Hentet landkoder (${response.betydninger.keys.size} stk)")
				response.betydninger.entries.mapNotNull { (kode, betydninger) ->
					betydninger
						.firstOrNull()
						?.beskrivelser
						?.values
						?.firstOrNull()
						?.tekst
						?.let { Landkode(kode, it) }
				}
			} ?: emptyList()
	}
}

data class KodeverkResponse(
	val betydninger: Map<String, List<KodeverkBetydning>>,
)

data class KodeverkBetydning(
	val gyldigFra: String,
	val gyldigTil: String,
	val beskrivelser: Map<String, KodeverkBeskrivelse>,
)

data class KodeverkBeskrivelse(
	val term: String,
	val tekst: String,
)
