package no.nav.persondataapi.integrasjon.aareg.client


import com.fasterxml.jackson.core.type.TypeReference
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.rest.domene.PersonIdent

import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

import java.util.UUID

@Component
class AaregClient(
    private val tokenService: TokenService,
    @param:Qualifier("aaregWebClient")
    private val webClient: WebClient,
) {

    // Jackson for parsing etter at vi har logget rå-body
    private val log = LoggerFactory.getLogger(javaClass)
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")

    @Cacheable(value = ["aareg-arbeidsforhold"], key = "#personIdent")
    fun hentArbeidsforhold(personIdent: PersonIdent): AaregDataResultat {
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
                .header("Nav-Personident", personIdent.value)
                .exchangeToMono { response ->
                    val status = response.statusCode()
                    // Les body som String (kan bare leses én gang), logg, og parse
                    response.bodyToMono(String::class.java)
                        .map { raw ->
                            if (status.is2xxSuccessful) {
                                try {
                                    val parsed: List<Arbeidsforhold> =
                                        JsonUtils.fromJson(raw)
                                    status.value() to parsed
                                }
                                catch (ex:Exception) {
                                    log.error("Klarte ikke å parse Aareg-respons. se Team Logs for full trace", ex)
                                    log.error(teamLogsMarker,"Klarte ikke å parse Aareg-respons. Rådata: $raw", ex)
                                    throw RuntimeException("Feil ved parsing av Aareg-respons", ex)
                                }

                            } else {
                                throw HttpStatusException(
                                    status.value(),
                                    "Feil fra AaregAPI: HTTP $status – $raw"
                                )
                            }
                        }
                }
                .block()!!

            AaregDataResultat(
                data = responsePair.second,
                statusCode = responsePair.first,
                errorMessage = ""
            )
        }.getOrElse { error ->
            if (error is HttpStatusException) {
                AaregDataResultat(
                    data = emptyList(),
                    statusCode = error.statusCode,
                    errorMessage = error.message ?: "Feil fra Aareg"
                )
            } else {
                AaregDataResultat(
                    data = emptyList(),
                    statusCode = 500,
                    errorMessage = "Teknisk feil: ${error.message}"
                )
            }
        }
    }
}

data class AaregDataResultat(
    val data: List<Arbeidsforhold> = emptyList(),
    val statusCode: Int?,               // f.eks. 200, 401, 500
    val errorMessage: String? = null
)

// Enkel custom exception for å bære HTTP-status
class HttpStatusException(val statusCode: Int, override val message: String) : RuntimeException(message)
