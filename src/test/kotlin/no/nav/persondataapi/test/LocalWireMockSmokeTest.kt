package no.nav.persondataapi.test

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.integrasjon.aareg.client.AaregClient
import no.nav.persondataapi.integrasjon.inntekt.client.InntektClient
import no.nav.persondataapi.integrasjon.kodeverk.client.KodeverkClient
import no.nav.persondataapi.integrasjon.modiacontextholder.client.ModiaContextHolderClient
import no.nav.persondataapi.integrasjon.nom.NomClient
import no.nav.persondataapi.integrasjon.pdl.client.PdlClient
import no.nav.persondataapi.integrasjon.tilgangsmaskin.client.TilgangsmaskinClientImpl
import no.nav.persondataapi.integrasjon.utbetaling.client.UtbetalingClient
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@ActiveProfiles("local")
class LocalWireMockSmokeTest {
    companion object {
        val wiremock: WireMockServer =
            WireMockServer(
                WireMockConfiguration.wireMockConfig().dynamicPort().usingFilesUnderDirectory("src/test/resources"),
            ).apply {
                start()
                stubFor(
                    WireMock.get(WireMock.urlPathEqualTo("/azuread/.well-known/openid-configuration"))
                        .willReturn(
                            WireMock.okJson(
                                """{"issuer":"http://localhost:${port()}/azuread","jwks_uri":"http://localhost:${port()}/azuread/jwks","token_endpoint":"http://localhost:${port()}/azuread/token","authorization_endpoint":"http://localhost:${port()}/azuread/authorize","response_types_supported":["code"],"subject_types_supported":["public"],"id_token_signing_alg_values_supported":["RS256"]}""",
                            ),
                        ),
                )
                stubFor(
                    WireMock.get(WireMock.urlPathEqualTo("/azuread/jwks"))
                        .willReturn(WireMock.okJson("""{"keys":[]}""")),
                )
            }

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            val port = wiremock.port()
            registry.add("WIREMOCK_PORT") { port.toString() }
            registry.add("AZURE_APP_WELL_KNOWN_URL") { "http://localhost:$port/azuread/.well-known/openid-configuration" }
            registry.add("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT") { "http://localhost:$port/oauth2/token" }
            registry.add("NAIS_TOKEN_EXCHANGE_ENDPOINT") { "http://localhost:$port/api/v1/token/exchange" }
            registry.add("PDL_URL") { "http://localhost:$port/pdl/graphql" }
            registry.add("NOM_URL") { "http://localhost:$port/nom/graphql" }
            registry.add("KODEVERK_URL") { "http://localhost:$port" }
            registry.add("INNTEKT_URL") { "http://localhost:$port" }
            registry.add("UTBETALING_URL") { "http://localhost:$port" }
            registry.add("AAREG_URL") { "http://localhost:$port" }
            registry.add("TILGANGMASKIN_URL") { "http://localhost:$port" }
            registry.add("MODIA_CONTEXT_HOLDER_URL") { "http://localhost:$port" }
        }

        @JvmStatic
        @AfterAll
        fun stop() {
            wiremock.stop()
        }
    }

    @Autowired
    lateinit var kodeverkClient: KodeverkClient

    @Autowired
    lateinit var inntektClient: InntektClient

    @Autowired
    lateinit var utbetalingClient: UtbetalingClient

    @Autowired
    lateinit var tilgangClient: TilgangsmaskinClientImpl

    @Autowired
    lateinit var aaregClient: AaregClient

    @Autowired
    lateinit var modiaClient: ModiaContextHolderClient

    @Autowired
    lateinit var pdlClient: PdlClient

    @Autowired
    lateinit var nomClient: NomClient

    @MockitoBean
    lateinit var tokenValidationContextHolder: TokenValidationContextHolder

    @BeforeEach
    fun settOppBrukertoken() {
        val context = mock(TokenValidationContext::class.java)
        val token = mock(JwtToken::class.java)
        given(token.encodedToken).willReturn("test-bruker-token")
        given(context.firstValidToken).willReturn(token)
        given(tokenValidationContextHolder.getTokenValidationContext()).willReturn(context)
    }

    @Test
    fun `full flow triggers downstream calls and wiremock records them`() {
        // Trigger synchronous/blocking clients
        val landkoder = kodeverkClient.hentLandkoder()
        val inntekter = inntektClient.hentInntekter(PersonIdent("12345678901"))
        val utbetalinger = utbetalingClient.hentUtbetalingerForBruker(PersonIdent("12345678901"), false)
        val tilgang = tilgangClient.sjekkTilgang(PersonIdent("12345678901"), "dummy-token")
        val aareg = aaregClient.hentArbeidsforhold(PersonIdent("12345678901"))
        modiaClient.settModiakontekst(PersonIdent("12345678901"))

        // Trigger suspend functions
        runBlocking {
            val pdl = pdlClient.hentPerson(PersonIdent("12345678901"))
            val nom = nomClient.hentSaksbehandlerTilhørighet("Z12345")
            // simple asserts to ensure responses were mapped
            assertTrue(pdl.statusCode == 200 || pdl.statusCode == 404) {
                "PDL statusCode was ${pdl.statusCode}, errorMessage: ${pdl.errorMessage}"
            }
            assertTrue(nom.statusCode == 200 || nom.statusCode == 404) {
                "NOM statusCode was ${nom.statusCode}, errorMessage: ${nom.errorMessage}"
            }
        }

        // Basic local assertions
        assertTrue(landkoder.isNotEmpty())

        // Verify WireMock received expected requests
        val port = wiremock.port()
        WireMock.configureFor("localhost", port)
        WireMock.verify(
            WireMock.getRequestedFor(
                WireMock
                    .urlPathEqualTo("/api/v1/kodeverk/Landkoder/koder/betydninger"),
            ),
        )
        WireMock.verify(
            WireMock.postRequestedFor(
                WireMock
                    .urlPathEqualTo("/rest/v2/inntektshistorikk"),
            ),
        )
        WireMock.verify(
            WireMock.postRequestedFor(
                WireMock
                    .urlPathEqualTo("/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern"),
            ),
        )
        WireMock.verify(
            WireMock.getRequestedFor(
                WireMock
                    .urlPathEqualTo("/v2/arbeidstaker/arbeidsforhold"),
            ),
        )
        WireMock.verify(
            WireMock.postRequestedFor(
                WireMock
                    .urlPathEqualTo("/api/v1/komplett"),
            ),
        )
        WireMock.verify(
            WireMock.postRequestedFor(
                WireMock
                    .urlPathEqualTo("/pdl/graphql"),
            ),
        )
        WireMock.verify(
            WireMock.postRequestedFor(
                WireMock
                    .urlPathEqualTo("/nom/graphql"),
            ),
        )
        WireMock.verify(
            WireMock.postRequestedFor(
                WireMock
                    .urlPathEqualTo("/api/context"),
            ),
        )
        // Token endpoints — flere klienter kaller token-endepunktene, verifiser minst ett kall
        WireMock.verify(
            WireMock.moreThanOrExactly(1),
            WireMock.postRequestedFor(
                WireMock
                    .urlPathEqualTo("/oauth2/token"),
            ),
        )
        WireMock.verify(
            WireMock.moreThanOrExactly(1),
            WireMock.postRequestedFor(
                WireMock
                    .urlPathEqualTo("/api/v1/token/exchange"),
            ),
        )
    }
}
