package no.nav.persondataapi.rest.saksbehandler

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.persondataapi.integrasjon.nom.SaksbehandlerTilhørighet
import no.nav.persondataapi.integrasjon.nom.SaksbehandlerTilhørighetResultat
import no.nav.persondataapi.service.SaksbehandlerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class SaksbehandlerControllerTest {

    private val saksbehandlerService = mockk<SaksbehandlerService>()
    private val controller = SaksbehandlerController(saksbehandlerService)

    @Test
    fun `hentSaksbehandler skal returnere saksbehandlerdata`() {
        val resultat = SaksbehandlerTilhørighetResultat(
            data = SaksbehandlerTilhørighet(
                navIdent = "Z12345",
                organisasjoner = listOf("Enhet A", "Enhet B")
            ),
            statusCode = 200,
            errorMessage = null
        )
        coEvery { saksbehandlerService.hentSaksbehandler() } returns resultat

        val response = controller.hentSaksbehandler()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Z12345", response.body?.data?.navIdent)
        assertEquals(listOf("Enhet A", "Enhet B"), response.body?.data?.organisasjoner)
        assertEquals(null, response.body?.error)
    }

    @Test
    fun `hentSaksbehandler skal returnere feilmelding ved NOM-feil`() {
        val resultat = SaksbehandlerTilhørighetResultat(
            data = null,
            statusCode = 404,
            errorMessage = "Fant ikke ressurs"
        )
        coEvery { saksbehandlerService.hentSaksbehandler() } returns resultat

        val response = controller.hentSaksbehandler()

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Fant ikke ressurs", response.body?.error)
        assertEquals(null, response.body?.data)
    }
}
