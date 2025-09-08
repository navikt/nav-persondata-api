package no.nav.persondataapi.ereg.client

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.persondataapi.service.ResponsMappingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class EregClient(
    @Qualifier("eregWebClient")
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper // injiseres automatisk av Spring Boot
) {
    private val logger = LoggerFactory.getLogger(ResponsMappingService::class.java)

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
        organisasjonDetaljer = null,
        virksomhetDetaljer = null
    )
}
