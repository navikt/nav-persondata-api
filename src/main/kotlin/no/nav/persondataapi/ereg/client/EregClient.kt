package no.nav.persondataapi.ereg.client


import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class EregClient(
    @Qualifier("eregWebClient")
    private val webClient: WebClient,
) {

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
                error.printStackTrace()
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

