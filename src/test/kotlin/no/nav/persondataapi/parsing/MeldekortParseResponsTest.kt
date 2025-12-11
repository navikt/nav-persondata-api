package no.nav.persondataapi.parsing

import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.integrasjon.dagpenger.meldekort.client.Meldekort
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.service.nåværendeBostedsadresse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

class MeldekortParseResponsTest {

    @Test
    fun `Skal returnere OK fra PDL`() {
        val jsonString = readJsonFromFile("testrespons/DagpengerMeldekortSample.json")
        val meldekort: List<Meldekort> = JsonUtils.fromJson(jsonString)
        Assertions.assertNotNull(meldekort)

    }

}
private fun readJsonFromFile(filename: String): String {
    val resource = ClassPathResource(filename)
    val inputStream = resource.inputStream
    return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
}