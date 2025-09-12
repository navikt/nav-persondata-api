package no.nav.persondataapi.parsing

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.persondataapi.configuration.JsonUtils
import no.nav.persondataapi.ereg.client.EregRespons
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import org.w3c.dom.Node
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class EregParserResponsTest {

    @Test
    fun kanLeseEregResponsFraProduksjon() {


        val jsonString = readJsonFromFile("testrespons/EregResponsSample2.json")
        val eregRespons: EregRespons = JsonUtils.fromJson(jsonString)
        Assertions.assertNotNull(eregRespons)
        Assertions.assertNotNull(eregRespons.navn)


        println(eregRespons)
    }
    @Test
    fun kanLeseEregResponsFraDolly() {


        val jsonString = readJsonFromFile("testrespons/EregResponsSample.json")
        val eregRespons: EregRespons = JsonUtils.fromJson(jsonString)
        Assertions.assertNotNull(eregRespons)
        Assertions.assertNotNull(eregRespons.navn)


        println(eregRespons)
    }

    @Test
    fun kanLeseKodeverk() {


        val jsonString = readJsonFromFile("testrespons/kodeverk.json")
        val v : Respons = JsonUtils.fromJson(jsonString)
       val liste =v.toSimpleMap()
        Assertions.assertEquals(liste["1340"],"SKUI")

    }
    @Test
    fun kanLeseDataNorgeFil() {


        val jsonString = readJsonFromFile("Postnummerregister-ansi.txt")

       println(jsonString)
    }


    data class Respons(
        val betydninger: Map<String, List<Betydning>>
    )

    data class Betydning(
        val gyldigFra: String,
        val gyldigTil: String,
        val beskrivelser: Map<String, Beskrivelse>
    )


    data class Beskrivelse(
        val term: String,
        val tekst: String
    )
    fun Respons.toSimpleMap(): Map<String, String> {
        return betydninger.mapValues { (_, liste) ->
            // Ta første betydning i listen (dersom flere, kan du velge logikk her)
            val første = liste.firstOrNull()
            // Hent første beskrivelse (her "nb")
            første?.beskrivelser?.values?.firstOrNull()?.tekst ?: ""
        }
    }

    private fun readJsonFromFile(filename: String): String {
        val resource = ClassPathResource(filename)
        val inputStream = resource.inputStream
        return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
    }
}