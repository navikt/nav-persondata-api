package no.nav.persondataapi.integrasjon.krr

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class KrrClientTest {
    private val ident = PersonIdent("12345678901")
    private val tokenService = mockk<TokenService>()
    private val contextHolder = mockk<TokenValidationContextHolder>()
    private val registry = SimpleMeterRegistry()

    private fun klient(
        svar: String,
        status: HttpStatus = HttpStatus.OK,
    ): Pair<KrrClient, ExchangeFunction> {
        val context = mockk<TokenValidationContext>()
        val token = mockk<JwtToken>()
        val claims = mockk<JwtTokenClaims>()
        every { contextHolder.getTokenValidationContext() } returns context
        every { context.firstValidToken } returns token
        every { token.jwtTokenClaims } returns claims
        every { claims.get("NAVident") } returns "Z12345"
        every { token.encodedToken } returns "syntetisk-brukertoken"
        every { tokenService.exchangeToken("syntetisk-brukertoken", SCOPE.KRR_SCOPE) } returns "syntetisk-obo-token"
        val exchange = mockk<ExchangeFunction>()
        every { exchange.exchange(any()) } answers {
            val request = firstArg<org.springframework.web.reactive.function.client.ClientRequest>()
            assertEquals("/rest/v1/personer", request.url().path)
            assertEquals("Bearer syntetisk-obo-token", request.headers().getFirst("Authorization"))
            Mono.just(
                ClientResponse
                    .create(status)
                    .header("Content-Type", "application/json")
                    .body(svar)
                    .build(),
            )
        }
        val webClient =
            WebClient
                .builder()
                .baseUrl("http://krr.test")
                .exchangeFunction(exchange)
                .build()
        return KrrClient(tokenService, contextHolder, webClient, registry) to exchange
    }

    @Test
    fun `returnerer e-post bare for riktig aktiv person med delegert token`() {
        val (client, exchange) =
            klient(
                """{"personer":{"12345678901":{"personident":"12345678901","aktiv":true,"reservert":false,"epostadresse":"syntetisk@example.com"}}}""",
            )

        assertEquals("syntetisk@example.com", client.hentEpost(ident))
        verify(exactly = 1) { exchange.exchange(any()) }
        assertEquals(1.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "funnet").count())
        val varighet = registry.find("personopplysninger_krr_epost_varighet").tags("resultat", "funnet").timer()
        assertEquals(1L, varighet?.count())
        assertTrue(varighet!!.totalTime(TimeUnit.NANOSECONDS) > 0)
        assertEquals(
            setOf("resultat"),
            varighet.id.tags
                .map { it.key }
                .toSet(),
        )
    }

    @Test
    fun `henter e-post uten reservasjonsfelt og fjerner blanke tegn rundt adressen`() {
        val (client, _) =
            klient(
                """{"personer":{"12345678901":{"personident":"12345678901","aktiv":true,"epostadresse":" syntetisk@example.com "}}}""",
            )

        assertEquals("syntetisk@example.com", client.hentEpost(ident))
    }

    @Test
    fun `skjuler reservert eller inaktiv e-post`() {
        val (reservert, _) =
            klient(
                """{"personer":{"12345678901":{"personident":"12345678901","aktiv":true,"reservert":true,"epostadresse":"syntetisk@example.com"}}}""",
            )
        assertNull(reservert.hentEpost(ident))
        val (inaktiv, _) =
            klient(
                """{"personer":{"12345678901":{"personident":"12345678901","aktiv":false,"reservert":false,"epostadresse":"syntetisk@example.com"}}}""",
            )
        assertNull(inaktiv.hentEpost(ident))
        assertEquals(2.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "ikke_funnet").count())
        assertEquals(
            2L,
            registry
                .find("personopplysninger_krr_epost_varighet")
                .tags("resultat", "ikke_funnet")
                .timer()
                ?.count(),
        )
    }

    @Test
    fun `skjuler manglende, tom eller blank e-post og tom personliste`() {
        val svar =
            listOf(
                """{"personer":{"12345678901":{"personident":"12345678901","aktiv":true}}}""",
                """{"personer":{"12345678901":{"personident":"12345678901","aktiv":true,"epostadresse":""}}}""",
                """{"personer":{"12345678901":{"personident":"12345678901","aktiv":true,"epostadresse":"   "}}}""",
                """{"personer":{}}""",
            )
        svar.forEach { assertNull(klient(it).first.hentEpost(ident)) }
        assertEquals(4.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "ikke_funnet").count())
    }

    @Test
    fun `avviser e-post for en annen person eller feil i KRR-responsen`() {
        val (feilIdent, _) =
            klient(
                """{"personer":{"12345678901":{"personident":"10987654321","aktiv":true,"epostadresse":"syntetisk@example.com"}}}""",
            )
        assertNull(feilIdent.hentEpost(ident))
        val (feil, _) =
            klient(
                """{"personer":{"12345678901":{"personident":"12345678901","aktiv":true,"epostadresse":"syntetisk@example.com"}},"feil":{"12345678901":"ikke tilgjengelig"}}""",
            )
        assertNull(feil.hentEpost(ident))
    }

    @Test
    fun `personoppslaget kan fortsette når KRR svarer med feil`() {
        val (client, _) = klient("""{"error":"unavailable"}""", HttpStatus.SERVICE_UNAVAILABLE)

        assertNull(client.hentEpost(ident))
        assertEquals(1.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "feil").count())
        assertEquals(
            1L,
            registry
                .find("personopplysninger_krr_epost_varighet")
                .tags("resultat", "feil")
                .timer()
                ?.count(),
        )
    }

    @Test
    fun `tokenutveksling og nettverkstimeout feiler uten å vise e-post`() {
        val (tokenFeil, tokenExchange) = klient("""{}""")
        every { tokenService.exchangeToken(any(), SCOPE.KRR_SCOPE) } throws IllegalStateException("syntetisk feil")
        assertNull(tokenFeil.hentEpost(ident))
        verify(exactly = 0) { tokenExchange.exchange(any()) }

        val (nettverksFeil, nettverksExchange) = klient("""{}""")
        every { nettverksExchange.exchange(any()) } returns Mono.error(TimeoutException("syntetisk tidsavbrudd"))
        assertNull(nettverksFeil.hentEpost(ident))
        assertEquals(2.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "feil").count())
        assertEquals(
            2L,
            registry
                .find("personopplysninger_krr_epost_varighet")
                .tags("resultat", "feil")
                .timer()
                ?.count(),
        )
    }

    @Test
    fun `uventet tom respons fra KRR gir ingen e-post og telles som feil`() {
        val (client, _) = klient("", HttpStatus.NO_CONTENT)

        assertNull(client.hentEpost(ident))
        assertEquals(1.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "feil").count())
    }

    @Test
    fun `204 uten body gir ingen e-post`() {
        val (client, exchange) = klient("""{}""")
        every { exchange.exchange(any()) } returns Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build())

        assertNull(client.hentEpost(ident))
        assertEquals(1.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "ikke_funnet").count())
    }

    @Test
    fun `maskin-token uten NAVident kan ikke hente KRR-e-post`() {
        val (client, exchange) = klient("""{}""")
        val token = contextHolder.getTokenValidationContext().firstValidToken!!
        every { token.jwtTokenClaims.get("NAVident") } returns null

        assertNull(client.hentEpost(ident))
        verify(exactly = 0) { tokenService.exchangeToken(any(), any()) }
        verify(exactly = 0) { exchange.exchange(any()) }
        assertEquals(1.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "uten_bruker").count())
        assertEquals(
            1L,
            registry
                .find("personopplysninger_krr_epost_varighet")
                .tags("resultat", "uten_bruker")
                .timer()
                ?.count(),
        )
    }

    @Test
    fun `NAVident med feil datatype kan ikke hente KRR-e-post`() {
        val (client, exchange) = klient("""{}""")
        val token = contextHolder.getTokenValidationContext().firstValidToken!!
        every { token.jwtTokenClaims.get("NAVident") } returns 123

        assertNull(client.hentEpost(ident))
        verify(exactly = 0) { tokenService.exchangeToken(any(), any()) }
        verify(exactly = 0) { exchange.exchange(any()) }
    }

    @Test
    fun `tom NAVident og manglende tokenkontekst kan ikke hente KRR-e-post`() {
        val (tomIdent, exchange) = klient("""{}""")
        val token = contextHolder.getTokenValidationContext().firstValidToken!!
        every { token.jwtTokenClaims.get("NAVident") } returns " "
        assertNull(tomIdent.hentEpost(ident))
        verify(exactly = 0) { exchange.exchange(any()) }

        val (manglerKontekst, _) = klient("""{}""")
        every { contextHolder.getTokenValidationContext() } throws IllegalStateException("ingen tokenkontekst")
        assertNull(manglerKontekst.hentEpost(ident))
        assertEquals(1.0, registry.counter("personopplysninger_krr_epost_oppslag", "resultat", "feil").count())
    }

    @Test
    fun `manglende token kan ikke hente KRR-e-post`() {
        val (client, exchange) = klient("""{}""")
        val context = contextHolder.getTokenValidationContext()
        every { context.firstValidToken } returns null

        assertNull(client.hentEpost(ident))
        verify(exactly = 0) { tokenService.exchangeToken(any(), any()) }
        verify(exactly = 0) { exchange.exchange(any()) }
    }
}
