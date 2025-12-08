package no.nav.persondataapi.parsing

import no.nav.inntekt.generated.model.InntektshistorikkApiUt
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.service.nyeste
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

class InntektParseResponsTest {
    @Test
    fun test(){
        val jsonString = readJsonFromFile("testrespons/InntektResponMedSlettedeHisoriskeInnslag.json")
        val inntekt: InntektshistorikkApiUt = JsonUtils.fromJson(jsonString)


        val v = inntekt.data?.filter { it.versjoner!!.size>1 }?.filter { it.maaned.contains("2025-02") }
        v?.forEach {
            val nyeste = it.versjoner.nyeste()
            println(nyeste?.inntektListe?.size ?: null)
        }
        println(v)



    }

    private fun readJsonFromFile(filename: String): String {
        val resource = ClassPathResource(filename)
        val inputStream = resource.inputStream
        return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
    }
}