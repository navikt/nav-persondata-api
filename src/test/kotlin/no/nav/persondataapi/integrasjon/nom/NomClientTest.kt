package no.nav.persondataapi.integrasjon.nom

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.generated.nom.RessursQuery
import no.nav.persondataapi.generated.nom.ressursquery.OrgEnhet
import no.nav.persondataapi.generated.nom.ressursquery.Ressurs
import no.nav.persondataapi.generated.nom.ressursquery.RessursOrgTilknytning
import no.nav.persondataapi.metrics.DownstreamResult
import no.nav.persondataapi.metrics.NomMetrics
import no.nav.persondataapi.service.SCOPE
import no.nav.persondataapi.service.TokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NomClientTest {

    private val tokenService = mockk<TokenService>()
    private val metrics = mockk<NomMetrics>(relaxed = true)
    private val nomUrl = "http://test-nom"

    @AfterEach
    fun rydderMocker() {
        unmockkAll()
    }

    @Test
    fun `håndterNomFeil skal returnere 404 når feilkode er not_found`() {
        val nomClient = NomClient(tokenService, metrics, nomUrl)

        val feil = mockk<GraphQLClientError>()
        every { feil.message } returns "Fant ikke ressurs"
        every { feil.extensions } returns mapOf("code" to "not_found")

        val (statuskode, feilmelding) = nomClient.håndterNomFeil(listOf(feil))

        assertEquals(404, statuskode)
        assertEquals("Fant ikke ressurs", feilmelding)
    }

    @Test
    fun `håndterNomFeil skal returnere 500 når feilkode ikke er not_found`() {
        val nomClient = NomClient(tokenService, metrics, nomUrl)

        val feil = mockk<GraphQLClientError>()
        every { feil.message } returns "Noe gikk galt"
        every { feil.extensions } returns mapOf("code" to "internal_error")

        val (statuskode, feilmelding) = nomClient.håndterNomFeil(listOf(feil))

        assertEquals(500, statuskode)
        assertEquals("Noe gikk galt", feilmelding)
    }

    @Test
    fun `hentSaksbehandlerTilhørighet skal mappe data ved suksess`() = runBlocking {
        mockkConstructor(GraphQLWebClient::class)
        val nomClient = NomClient(tokenService, metrics, nomUrl)

        every { tokenService.getServiceToken(SCOPE.NOM_SCOPE) } returns "token"

        val ressurs = Ressurs(
            navident = "Z12345",
            orgTilknytninger = listOf(
                RessursOrgTilknytning(orgEnhet = OrgEnhet(navn = "Enhet A")),
                RessursOrgTilknytning(orgEnhet = OrgEnhet(navn = "Enhet B"))
            )
        )
        val respons = mockk<GraphQLClientResponse<RessursQuery.Result>>()
        every { respons.data } returns RessursQuery.Result(ressurs = ressurs)
        every { respons.errors } returns null
        coEvery { anyConstructed<GraphQLWebClient>().execute(any<RessursQuery>(), any()) } returns respons

        val resultat = nomClient.hentSaksbehandlerTilhørighet("Z12345")

        assertEquals(200, resultat.statusCode)
        assertEquals(null, resultat.errorMessage)
        assertEquals("Z12345", resultat.data?.navIdent)
        assertEquals(listOf("Enhet A", "Enhet B"), resultat.data?.organisasjoner)
        verify { tokenService.getServiceToken(SCOPE.NOM_SCOPE) }
    }

    @Test
    fun `hentSaksbehandlerTilhørighet skal returnere 404 ved not_found feil`() = runBlocking {
        mockkConstructor(GraphQLWebClient::class)
        val nomClient = NomClient(tokenService, metrics, nomUrl)

        every { tokenService.getServiceToken(SCOPE.NOM_SCOPE) } returns "token"

        val feil = mockk<GraphQLClientError>()
        every { feil.message } returns "Fant ikke ressurs"
        every { feil.extensions } returns mapOf("code" to "not_found")
        val respons = mockk<GraphQLClientResponse<RessursQuery.Result>>()
        every { respons.data } returns null
        every { respons.errors } returns listOf(feil)
        coEvery { anyConstructed<GraphQLWebClient>().execute(any<RessursQuery>(), any()) } returns respons

        val resultat = nomClient.hentSaksbehandlerTilhørighet("Z12345")

        assertEquals(404, resultat.statusCode)
        assertEquals("Fant ikke ressurs", resultat.errorMessage)
        assertEquals(null, resultat.data)
    }

    @Test
    fun `hentSaksbehandlerTilhørighet skal returnere 500 ved ukjent feil i respons`() = runBlocking {
        mockkConstructor(GraphQLWebClient::class)
        val nomClient = NomClient(tokenService, metrics, nomUrl)

        every { tokenService.getServiceToken(SCOPE.NOM_SCOPE) } returns "token"

        val feil = mockk<GraphQLClientError>()
        every { feil.message } returns "Noe gikk galt"
        every { feil.extensions } returns mapOf("code" to "internal_error")
        val respons = mockk<GraphQLClientResponse<RessursQuery.Result>>()
        every { respons.data } returns null
        every { respons.errors } returns listOf(feil)
        coEvery { anyConstructed<GraphQLWebClient>().execute(any<RessursQuery>(), any()) } returns respons

        val resultat = nomClient.hentSaksbehandlerTilhørighet("Z12345")

        assertEquals(500, resultat.statusCode)
        assertEquals("Noe gikk galt", resultat.errorMessage)
        assertEquals(null, resultat.data)
    }

    @Test
    fun `hentSaksbehandlerTilhørighet skal returnere 404 når ressurs er null`() = runBlocking {
        mockkConstructor(GraphQLWebClient::class)
        val nomClient = NomClient(tokenService, metrics, nomUrl)

        every { tokenService.getServiceToken(SCOPE.NOM_SCOPE) } returns "token"

        val respons = mockk<GraphQLClientResponse<RessursQuery.Result>>()
        every { respons.data } returns RessursQuery.Result(ressurs = null)
        every { respons.errors } returns null
        coEvery { anyConstructed<GraphQLWebClient>().execute(any<RessursQuery>(), any()) } returns respons

        val resultat = nomClient.hentSaksbehandlerTilhørighet("Z12345")

        assertEquals(404, resultat.statusCode)
        assertEquals("Fant ikke ressurs", resultat.errorMessage)
        assertEquals(null, resultat.data)
    }

    @Test
    fun `hentSaksbehandlerTilhørighet skal returnere 504 ved timeout`() = runBlocking {
        mockkConstructor(GraphQLWebClient::class)
        val nomClient = NomClient(tokenService, metrics, nomUrl)

        every { tokenService.getServiceToken(SCOPE.NOM_SCOPE) } returns "token"
        coEvery { anyConstructed<GraphQLWebClient>().execute(any<RessursQuery>(), any()) } throws TimeoutException("Timeout")

        val resultat = nomClient.hentSaksbehandlerTilhørighet("Z12345")

        assertEquals(504, resultat.statusCode)
        assertEquals("Timeout mot NOM", resultat.errorMessage)
        verify { metrics.counter("HentRessurs", DownstreamResult.TIMEOUT) }
    }

    @Test
    fun `hentSaksbehandlerTilhørighet skal returnere 500 ved uventet exception`() = runBlocking {
        mockkConstructor(GraphQLWebClient::class)
        val nomClient = NomClient(tokenService, metrics, nomUrl)

        every { tokenService.getServiceToken(SCOPE.NOM_SCOPE) } returns "token"
        coEvery { anyConstructed<GraphQLWebClient>().execute(any<RessursQuery>(), any()) } throws RuntimeException("Uventet")

        val resultat = nomClient.hentSaksbehandlerTilhørighet("Z12345")

        assertEquals(500, resultat.statusCode)
        assertEquals("Uventet", resultat.errorMessage)
        verify { metrics.counter("HentRessurs", DownstreamResult.UNEXPECTED) }
    }
}
