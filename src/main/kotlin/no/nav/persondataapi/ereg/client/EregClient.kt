package no.nav.persondataapi.ereg.client


import no.nav.persondataapi.service.ResponsMappingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class EregClient(
    @Qualifier("eregWebClient")
    private val webClient: WebClient,
) {
    private val logger = LoggerFactory.getLogger(ResponsMappingService::class.java)

    fun hentOrganisasjon(orgnummer: String): EregRespons {
        return runCatching {

            val response: EregRespons = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v2/organisasjon/$orgnummer")
                        .queryParam("inkluderHistorikk", "false")
                        .queryParam("inkluderHierarki", "false")
                        .build()
                }
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<EregRespons>() {})
                .block()!! // Bruk `awaitSingle()` hvis du er i coroutine-verden

            response
        }.fold(
            onSuccess = { ereg ->

                ereg
            },
            onFailure = { error ->
                logger.error("Teknisk feil Feil ved henting av Ereg")
                logger.error(error.stackTraceToString())
                EregRespons(
                    orgnummer,
                    type = "",
                    navn = null,
                    organisasjonDetaljer = null,
                    virksomhetDetaljer = null
                )

            }
        )
    }
}

