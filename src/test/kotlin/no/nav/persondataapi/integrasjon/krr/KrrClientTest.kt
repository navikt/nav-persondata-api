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
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

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
    }

    @Test
    fun `maskin-token uten NAVident kan ikke hente KRR-e-post`() {
        val (client, exchange) = klient("""{}""")
        val token = contextHolder.getTokenValidationContext().firstValidToken!!
        every { token.jwtTokenClaims.get("NAVident") } returns null

        assertNull(client.hentEpost(ident))
        verify(exactly = 0) { tokenService.exchangeToken(any(), any()) }
        verify(exactly = 0) { exchange.exchange(any()) }
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
