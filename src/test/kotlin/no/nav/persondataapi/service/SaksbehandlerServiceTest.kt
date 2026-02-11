package no.nav.persondataapi.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.integrasjon.nom.NomClient
import no.nav.persondataapi.integrasjon.nom.SaksbehandlerTilhørighet
import no.nav.persondataapi.integrasjon.nom.SaksbehandlerTilhørighetResultat
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SaksbehandlerServiceTest {

    private val tokenValidationContextHolder = mockk<TokenValidationContextHolder>()
    private val nomClient = mockk<NomClient>()
    private val saksbehandlerService = SaksbehandlerService(tokenValidationContextHolder, nomClient)

    @Test
    fun `hentSaksbehandler skal bruke NAVident og returnere resultat fra NOM`() = runBlocking {
        val context = mockk<TokenValidationContext>()
        val token = mockk<JwtToken>()
        val claims = mockk<JwtTokenClaims>()
        val forventet = SaksbehandlerTilhørighetResultat(
            data = SaksbehandlerTilhørighet(
                navIdent = "Z12345",
                organisasjoner = listOf("Enhet A")
            ),
            statusCode = 200,
            errorMessage = null
        )

        every { tokenValidationContextHolder.getTokenValidationContext() } returns context
        every { context.firstValidToken } returns token
        every { token.jwtTokenClaims } returns claims
        every { claims.get("NAVident") } returns "Z12345"
        coEvery { nomClient.hentSaksbehandlerTilhørighet("Z12345") } returns forventet

        val resultat = saksbehandlerService.hentSaksbehandler()

        assertEquals(forventet, resultat)
    }

    @Test
    fun `hentSaksbehandler skal kaste feil når token mangler`() {
        val context = mockk<TokenValidationContext>()

        every { tokenValidationContextHolder.getTokenValidationContext() } returns context
        every { context.firstValidToken } returns null

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                saksbehandlerService.hentSaksbehandler()
            }
        }

        assertEquals("Fant ikke gyldig token", exception.message)
    }
}
