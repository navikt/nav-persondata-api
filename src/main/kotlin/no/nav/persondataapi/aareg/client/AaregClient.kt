package no.nav.persondataapi.aareg.client


import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.persondataapi.configuration.JsonUtils
import no.nav.persondataapi.domain.AaregResultat
import no.nav.persondataapi.ereg.client.EregClient

import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.springframework.beans.factory.annotation.Qualifier

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

import java.util.UUID

@Component
class AaregClient(
    private val tokenService: TokenService,
    @Qualifier("aaregWebClient")
    private val webClient: WebClient,
) {

    // Jackson for parsing etter at vi har logget rå-body

    private val arbeidsforholdListType = object : TypeReference<List<Arbeidsforhold>>() {}

    fun hentArbeidsForhold(fnr: String, token: String): AaregResultat {
        return runCatching {
            val oboToken = tokenService.getServiceToken(SCOPE.AAREG_SCOPE)

            val responsePair: Pair<Int, List<Arbeidsforhold>> = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v2/arbeidstaker/arbeidsforhold")
                        .queryParam("historikk", "true")
                        .queryParam("arbeidsforholdstatus", "AKTIV", "AVSLUTTET", "FREMTIDIG")
                        .queryParam("rapporteringsordning", "A_ORDNINGEN")
                        .build()
                }
                .header("Authorization", "Bearer $oboToken")
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .header("Nav-Personident", fnr)
                .exchangeToMono { response ->
                    val status = response.statusCode()
                    // Les body som String (kan bare leses én gang), logg, og parse
                    response.bodyToMono(String::class.java)
                        .map { raw ->
                            println("RAW body (${status.value()}): $raw") // 👈 nå ser du faktisk responsen
                            if (status.is2xxSuccessful) {
                                val parsed: List<Arbeidsforhold> =
                                    JsonUtils.fromJson(raw)
                                status.value() to parsed
                            } else {
                                throw HttpStatusException(
                                    status.value(),
                                    "Feil fra AaregAPI: HTTP $status – $raw"
                                )
                            }
                        }
                }
                .block()!!

            AaregResultat(
                data = responsePair.second,
                statusCode = responsePair.first,
                errorMessage = ""
            )
        }.getOrElse { error ->
            if (error is HttpStatusException) {
                AaregResultat(
                    data = null,
                    statusCode = error.statusCode,
                    errorMessage = error.message ?: "Feil fra Aareg"
                )
            } else {
                AaregResultat(
                    data = null,
                    statusCode = 500,
                    errorMessage = "Teknisk feil: ${error.message}"
                )
            }
        }
    }
}

// Enkel custom exception for å bære HTTP-status
class HttpStatusException(val statusCode: Int, override val message: String) : RuntimeException(message)
