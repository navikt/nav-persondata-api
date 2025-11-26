package no.nav.persondataapi.integrasjon.pdl.client

import com.expediagroup.graphql.client.types.GraphQLClientError
import io.mockk.every
import io.mockk.mockk
import no.nav.persondataapi.metrics.PdlMetrics
import no.nav.persondataapi.service.TokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PdlClientTest {

    private val tokenService = mockk<TokenService>()
    private val metrics = mockk<PdlMetrics>(relaxed = true)
    private val pdlUrl = "http://test-pdl"

    @Test
    fun `håndterPdlFeil skal returnere 404 når error code er not_found`() {
        val pdlClient = PdlClient(tokenService, metrics, pdlUrl)

        val error = mockk<GraphQLClientError>()
        every { error.message } returns "Fant ikke person"
        every { error.extensions } returns mapOf("code" to "not_found")

        val (statusCode, errorMessage) = pdlClient.håndterPdlFeil(listOf(error))

        assertEquals(404, statusCode)
        assertEquals("Fant ikke person", errorMessage)
    }

    @Test
    fun `håndterPdlFeil skal returnere 500 når error code ikke er not_found`() {
        val pdlClient = PdlClient(tokenService, metrics, pdlUrl)

        val error = mockk<GraphQLClientError>()
        every { error.message } returns "Noe gikk galt"
        every { error.extensions } returns mapOf("code" to "internal_error")

        val (statusCode, errorMessage) = pdlClient.håndterPdlFeil(listOf(error))

        assertEquals(500, statusCode)
        assertEquals("Noe gikk galt", errorMessage)
    }
}
