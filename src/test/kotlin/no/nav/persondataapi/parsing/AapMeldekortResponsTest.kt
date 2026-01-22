package no.nav.persondataapi.parsing

import no.nav.persondataapi.integrasjon.aap.meldekort.domene.AAPMaxRespons
import no.nav.persondataapi.konfigurasjon.JsonUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets
import kotlin.test.Test

class AapMeldekortResponsTest {

    @Test
    fun KanLese2025Respons(){
        val jsonString = lesJsonFraFil("testrespons/AAPMaxRepons2025.json")
        val aapMeldekortRespons: AAPMaxRespons = JsonUtils.fromJson(jsonString)
        println()
    }
    @Test
    fun KanLese2026Respons(){
        val jsonString = lesJsonFraFil("testrespons/AAPMaxRespons2026.json")
        val aapMeldekortRespons: AAPMaxRespons = JsonUtils.fromJson(jsonString)
        println()
    }

}
private fun lesJsonFraFil(filename: String): String {
    val resource = ClassPathResource(filename)
    val inputStream = resource.inputStream
    return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
}