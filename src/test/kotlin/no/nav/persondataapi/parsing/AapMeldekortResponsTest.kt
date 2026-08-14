package no.nav.persondataapi.parsing

import no.nav.persondataapi.integrasjon.aap.meldekort.domene.AapMaximumRespons
import no.nav.persondataapi.konfigurasjon.JsonUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets
import kotlin.test.Test

class AapMeldekortResponsTest {
    @Test
    fun kanLese2025Respons() {
        val jsonString = lesJsonFraFil("testrespons/AAPMaxRepons2025.json")
        val aapMeldekortRespons: AapMaximumRespons = JsonUtils.fromJson(jsonString)

        assertTrue(aapMeldekortRespons.vedtak.isNotEmpty())
        // dagsatsEtterUføreReduksjon (JSON, med "ø") skal mappes til
        // dagsatsEtterUforeReduksjon (Kotlin, med "o") via @JsonAlias.
        assertEquals(1022, aapMeldekortRespons.vedtak[0].dagsatsEtterUforeReduksjon)
    }

    @Test
    fun kanLese2026Respons() {
        val jsonString = lesJsonFraFil("testrespons/AAPMaxRespons2026.json")
        val aapMeldekortRespons: AapMaximumRespons = JsonUtils.fromJson(jsonString)

        assertTrue(aapMeldekortRespons.vedtak.isNotEmpty())
        assertEquals(1022, aapMeldekortRespons.vedtak[0].dagsatsEtterUforeReduksjon)
    }
}

private fun lesJsonFraFil(filename: String): String {
    val resource = ClassPathResource(filename)
    val inputStream = resource.inputStream
    return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
}
