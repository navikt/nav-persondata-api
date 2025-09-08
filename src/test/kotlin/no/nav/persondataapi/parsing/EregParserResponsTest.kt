package no.nav.persondataapi.parsing

import no.nav.persondataapi.configuration.JsonUtils
import no.nav.persondataapi.ereg.client.EregRespons
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

class EregParserResponsTest {

    @Test
    fun kanLeseResponsFraSOKOSUtbetalingsAPI() {


        val jsonString = readJsonFromFile("testrespons/EregResponsSample2.json")
        val eregRespons: EregRespons = JsonUtils.fromJson(jsonString)
        Assertions.assertNotNull(eregRespons)
        Assertions.assertNotNull(eregRespons.navn)


        println(eregRespons)
    }

    private fun readJsonFromFile(filename: String): String {
        val resource = ClassPathResource(filename)
        val inputStream = resource.inputStream
        return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
    }
}