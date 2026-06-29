package no.nav.persondataapi.integrasjon.pdl.client

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.metrics.PdlMetrics
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class PdlClientTest {
    private val tokenService = mockk<TokenService>()
    private val metrics = mockk<PdlMetrics>(relaxed = true)

    private lateinit var wireMock: WireMockServer

    @BeforeEach
    fun startWireMock() {
        wireMock = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMock.start()
        every { tokenService.getServiceToken(SCOPE.PDL_SCOPE) } returns "test-token"
    }

    @AfterEach
    fun stopWireMock() {
        wireMock.stop()
    }

    private fun pdlGraphQLClient(baseUrl: String = "http://localhost:${wireMock.port()}/graphql") =
        GraphQLWebClient(url = baseUrl, builder = WebClient.builder().mutate())

    @Test
    fun `håndterPdlFeil skal returnere 404 når error code er not_found`() {
        val pdlClient = PdlClient(tokenService, metrics, pdlGraphQLClient())

        val error = mockk<GraphQLClientError>()
        every { error.message } returns "Fant ikke person"
        every { error.extensions } returns mapOf("code" to "not_found")

        val (statusCode, errorMessage) = pdlClient.håndterPdlFeil(listOf(error))

        assertEquals(404, statusCode)
        assertEquals("Fant ikke person", errorMessage)
    }

    @Test
    fun `håndterPdlFeil skal returnere 500 når error code ikke er not_found`() {
        val pdlClient = PdlClient(tokenService, metrics, pdlGraphQLClient())

        val error = mockk<GraphQLClientError>()
        every { error.message } returns "Noe gikk galt"
        every { error.extensions } returns mapOf("code" to "internal_error")

        val (statusCode, errorMessage) = pdlClient.håndterPdlFeil(listOf(error))

        assertEquals(500, statusCode)
        assertEquals("Noe gikk galt", errorMessage)
    }

    @Test
    fun `hentPersonBolk skal returnere navn for alle identer ved OK-respons`() =
        runBlocking {
            wireMock.stubFor(
                post(urlPathEqualTo("/graphql"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                  "data": {
                                    "hentPersonBolk": [
                                      {
                                        "ident": "11111111111",
                                        "code": "ok",
                                        "person": {
                                          "navn": [{ "fornavn": "Barn", "mellomnavn": null, "etternavn": "Testesen" }],
                                          "foedselsdato": [{ "foedselsdato": "2015-03-10" }],
                                          "adressebeskyttelse": []
                                        }
                                      }
                                    ]
                                  }
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val pdlClient = PdlClient(tokenService, metrics, pdlGraphQLClient())
            val resultat = pdlClient.hentPersonBolk(listOf(PersonIdent("11111111111")))

            assertEquals(200, resultat.statusCode)
            assertEquals(1, resultat.data.size)
            assertEquals("ok", resultat.data[0].code)
            assertEquals(
                "Barn",
                resultat.data[0]
                    .person
                    ?.navn
                    ?.firstOrNull()
                    ?.fornavn,
            )
        }

    @Test
    fun `hentPersonBolk skal returnere 502 når PDL returnerer null data`() =
        runBlocking {
            wireMock.stubFor(
                post(urlPathEqualTo("/graphql"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""{ "data": null }"""),
                    ),
            )

            val pdlClient = PdlClient(tokenService, metrics, pdlGraphQLClient())
            val resultat = pdlClient.hentPersonBolk(listOf(PersonIdent("11111111111")))

            assertEquals(502, resultat.statusCode)
            assertTrue(resultat.data.isEmpty())
        }

    @Test
    fun `hentPersonBolk skal returnere 404 når PDL returnerer not_found-feil`() =
        runBlocking {
            wireMock.stubFor(
                post(urlPathEqualTo("/graphql"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                  "errors": [
                                    {
                                      "message": "Fant ikke person",
                                      "extensions": { "code": "not_found" }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val pdlClient = PdlClient(tokenService, metrics, pdlGraphQLClient())
            val resultat = pdlClient.hentPersonBolk(listOf(PersonIdent("11111111111")))

            assertEquals(404, resultat.statusCode)
            assertTrue(resultat.data.isEmpty())
        }

    @Test
    fun `hentPersonBolk skal returnere 500 ved uventet feil`() =
        runBlocking {
            val pdlClient = PdlClient(tokenService, metrics, pdlGraphQLClient("http://localhost:1/graphql"))
            val resultat = pdlClient.hentPersonBolk(listOf(PersonIdent("11111111111")))

            assertEquals(500, resultat.statusCode)
            assertTrue(resultat.data.isEmpty())
        }
}

