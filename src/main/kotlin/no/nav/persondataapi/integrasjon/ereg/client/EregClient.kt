package no.nav.persondataapi.integrasjon.ereg.client

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.persondataapi.konfigurasjon.RetryPolicy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class EregClient(
    @param:Qualifier("eregWebClient")
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper // injiseres automatisk av Spring Boot
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Cacheable(
        value = ["ereg-organisasjon"],
        key = "#orgnummer"
    )
    fun hentOrganisasjon(orgnummer: String): EregRespons {
        val rawJson: String = try {
            webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v2/organisasjon/$orgnummer")
                        .queryParam("inkluderHistorikk", "false")
                        .queryParam("inkluderHierarki", "false")
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .retryWhen(RetryPolicy.reactorRetrySpec(kilde = "ereg-organisasjon"))
                .block()!!
        } catch (ex: Exception) {
            logger.error("Klarte ikke å hente data fra Ereg for orgnummer=$orgnummer", ex)
            return fallback(orgnummer)
        }

        return try {
            objectMapper.readValue(rawJson, EregRespons::class.java)
        } catch (ex: Exception) {
            logger.error("Klarte ikke å parse Ereg-respons for orgnummer=$orgnummer. Rå JSON:\n$rawJson", ex)
            fallback(orgnummer)
        }
    }

    private fun fallback(orgnummer: String) = EregRespons(
        organisasjonsnummer = orgnummer,
        type = "",
        navn = null,
    )
}
