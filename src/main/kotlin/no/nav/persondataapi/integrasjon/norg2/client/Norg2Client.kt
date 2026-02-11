package no.nav.persondataapi.integrasjon.norg2.client

import tools.jackson.databind.json.JsonMapper
import no.nav.persondataapi.konfigurasjon.RetryPolicy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class Norg2Client(
    @param:Qualifier("norg2WebClient")
    private val webClient: WebClient,
    private val jsonMapper: JsonMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Cacheable(
        value = ["norg2-lokalKontor"],
        unless = "#result.enhetId == -1"
    )
    fun hentLokalNavKontor(lokalKontor:String): NavLokalKontor {

        val rawJson = try {
            webClient.get()

                .uri("/api/v1/enhet/navkontor/$lokalKontor")
                .retrieve()
                .bodyToMono(String::class.java)
                .retryWhen(RetryPolicy.reactorRetrySpec(kilde = "Norg2"))
                .block()!!
        }
        catch (ex: Exception) {
        logger.error("Teknisk feil ved kall mot Norg2",ex)
        return fallback()
    }

    return try {
        jsonMapper.readValue(rawJson, NavLokalKontor::class.java)
    } catch (ex: Exception) {
        logger.error("Klarte ikke å parse Norg2-respons for enhetsnummer=$lokalKontor. Rå JSON:\n$rawJson", ex)
        fallback()
    }
}
    fun fallback() = NavLokalKontor(
        enhetId = -1,
        navn = "Ukjent",
        enhetNr = "Ukjent",
        type = "Ukjent",
    )
}
