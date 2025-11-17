package no.nav.persondataapi.parsing

import no.nav.persondataapi.generated.hentperson.Person
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.service.nåværendeBostedsadresse
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

class PdlParseResponsTest {

    @Test
    fun `Skal returnere OK fra PDL`() {
        val jsonString = readJsonFromFile("testrespons/PdlResponsSample.json")
        val person: Person = JsonUtils.fromJson(jsonString)
        person.nåværendeBostedsadresse()
    }

}
private fun readJsonFromFile(filename: String): String {
    val resource = ClassPathResource(filename)
    val inputStream = resource.inputStream
    return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
}