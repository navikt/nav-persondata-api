package no.nav.persondataapi.kodeverk.client

import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class KodeverkClient(
    private val tokenService: TokenService,
    @Qualifier("kodeverkWebClient")
    private val webClient: WebClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun hentLandkoder(): List<Landkode> {
        val token = tokenService.getServiceToken(SCOPE.KODEVERK_SCOPE);
        val response = webClient.get()
            .uri("/api/v1/kodeverk/Landkoder/koder/betydninger?spraak=nb")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono<KodeverkResponse>()
            .block()

        if (response == null) {
            log.error("Kunne ikke hente landkoder fra kodeverk");
            return emptyList()
        }

        log.info("Hentet landkoder (${response.betydninger.keys.size} stk)")
        return response.betydninger.entries.mapNotNull { (kode, betydninger) ->
            val beskrivelse = betydninger.firstOrNull()?.beskrivelser?.get(Språk.NB)?.tekst
            if (beskrivelse != null) {
                Landkode(kode, beskrivelse)
            } else {
                null
            }
        }
    }

}

data class KodeverkResponse(
    val betydninger: Map<String, List<KodeverkBetydning>>
)

data class KodeverkBetydning(
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelser: Map<Språk, KodeverkBeskrivelse>
)

data class KodeverkBeskrivelse(
    val term: String,
    val tekst: String
)

enum class Språk(val kode: String) {
    NB("nb"),
}
