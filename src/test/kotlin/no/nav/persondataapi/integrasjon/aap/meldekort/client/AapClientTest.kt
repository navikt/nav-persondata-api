package no.nav.persondataapi.integrasjon.aap.meldekort.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.metrics.AAPMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal

private const val IDENT = "12345678901"

/**
 * WireMock-baserte tester for [AapClient.hentArbeidstimer] — verifiserer
 * faktisk HTTP-oppførsel (request-body, headers, feilhåndtering) mot det
 * dedikerte `/holmes/arbeidstimer`-endepunktet i aap-api-intern. Bruker en
 * ekte [AAPMetrics]-instans (ikke mockk) fordi [AapClient] pakker selve
 * nettverkskallet i `metrics.timer(...).recordCallable { ... }` — en
 * relaxed-mocket [io.micrometer.core.instrument.Timer] ville ikke faktisk
 * kalt lambdaen, og dermed aldri utført det ekte HTTP-kallet som testes her.
 */
class AapClientTest {
    private val tokenService = mockk<TokenService>()
    private val metrics = AAPMetrics(SimpleMeterRegistry())

    private lateinit var wireMock: WireMockServer

    @BeforeEach
    fun startWireMock() {
        wireMock = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMock.start()
        every { tokenService.getServiceToken(SCOPE.AAP_SCOPE) } returns "test-token"
    }

    @AfterEach
    fun stopWireMock() {
        wireMock.stop()
    }

    private fun aapClient(baseUrl: String = "http://localhost:${wireMock.port()}") =
        AapClient(
            webClient =
                WebClient
                    .builder()
                    .baseUrl(baseUrl)
                    .codecs { configurer ->
                        configurer.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(JsonUtils.mapper))
                        configurer.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(JsonUtils.mapper))
                    }.build(),
            metrics = metrics,
            tokenService = tokenService,
        )

    @Test
    fun `hentArbeidstimer skal mappe respons korrekt ved OK`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/holmes/arbeidstimer"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "personIdent": "$IDENT",
                              "meldeperioder": [
                                {
                                  "periodeFom": "2026-08-01",
                                  "periodeTom": "2026-08-14",
                                  "timerArbeid": [
                                    { "periodeFom": "2026-08-01", "periodeTom": "2026-08-14", "timerArbeidet": 20.0 }
                                  ]
                                }
                              ]
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val resultat = aapClient().hentArbeidstimer(PersonIdent(IDENT), utvidet = false)

        assertEquals(IDENT, resultat?.personIdent)
        assertEquals(1, resultat?.meldeperioder?.size)
        assertEquals(
            BigDecimal("20.0"),
            resultat
                ?.meldeperioder
                ?.first()
                ?.timerArbeid
                ?.first()
                ?.timerArbeidet,
        )
    }

    @Test
    fun `hentArbeidstimer sender personidentifikator og Authorization-header i forespørselen`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/holmes/arbeidstimer"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "personIdent": "$IDENT", "meldeperioder": [] }"""),
                ),
        )

        aapClient().hentArbeidstimer(PersonIdent(IDENT), utvidet = false)

        wireMock.verify(
            postRequestedFor(urlPathEqualTo("/holmes/arbeidstimer"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .withRequestBody(matchingJsonPath("$.personidentifikator", equalTo(IDENT))),
        )
    }

    @Test
    fun `hentArbeidstimer returnerer null (ikke krasj) ved 500-feil`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/holmes/arbeidstimer"))
                .willReturn(aResponse().withStatus(500).withBody("Feil på serveren")),
        )

        val resultat = aapClient().hentArbeidstimer(PersonIdent(IDENT), utvidet = false)

        assertNull(resultat)
    }

    @Test
    fun `hentArbeidstimer returnerer null (ikke krasj) ved 403 Ingen tilgang`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/holmes/arbeidstimer"))
                .willReturn(aResponse().withStatus(403).withBody("Ingen tilgang")),
        )

        val resultat = aapClient().hentArbeidstimer(PersonIdent(IDENT), utvidet = false)

        assertNull(resultat)
    }

    @Test
    fun `hentArbeidstimer returnerer null når nedstrøms-tjenesten er utilgjengelig`() {
        val resultat =
            aapClient(baseUrl = "http://localhost:1").hentArbeidstimer(PersonIdent(IDENT), utvidet = false)

        assertNull(resultat)
    }

    @Test
    fun `hentArbeidstimer håndterer flere meldeperioder med flere timerArbeid-segmenter`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/holmes/arbeidstimer"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "personIdent": "$IDENT",
                              "meldeperioder": [
                                {
                                  "periodeFom": "2026-08-01",
                                  "periodeTom": "2026-08-14",
                                  "timerArbeid": [
                                    { "periodeFom": "2026-08-01", "periodeTom": "2026-08-07", "timerArbeidet": 5.0 },
                                    { "periodeFom": "2026-08-08", "periodeTom": "2026-08-14", "timerArbeidet": 7.5 }
                                  ]
                                },
                                {
                                  "periodeFom": "2026-08-15",
                                  "periodeTom": "2026-08-28",
                                  "timerArbeid": [
                                    { "periodeFom": "2026-08-15", "periodeTom": "2026-08-28", "timerArbeidet": 0.0 }
                                  ]
                                }
                              ]
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val resultat = aapClient().hentArbeidstimer(PersonIdent(IDENT), utvidet = true)!!
        val alleSegmenter = resultat.alleTimerArbeidSegmenter()

        assertEquals(3, alleSegmenter.size)
        assertEquals(BigDecimal("12.5"), alleSegmenter.take(2).sumOf { it.timerArbeidet })
    }
}
