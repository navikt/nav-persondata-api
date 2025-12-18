package no.nav.persondataapi.parsing

import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Meldekort
import no.nav.persondataapi.konfigurasjon.JsonUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

class MeldekortParseResponsTest {

    @Test
    fun `Skal parse respons fra meldekort`() {
        val jsonString = lesJsonFraFil("testrespons/DagpengerMeldekortSample.json")
        val meldekort: List<Meldekort> = JsonUtils.fromJson(jsonString)
        Assertions.assertNotNull(meldekort)
    }

    private fun lesJsonFraFil(filnavn: String): String {
        val ressurs = ClassPathResource(filnavn)
        return StreamUtils.copyToString(ressurs.inputStream, StandardCharsets.UTF_8)
    }
}
