package no.nav.persondataapi.pensjonsgivendeInntekt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Year

class `SisteXÅrServiceTest` {

    @Test
    fun UthentingavÅrSkalIkkeTaMedInneværendeÅr() {
        val detteÅret = Year.now().value
        val response = HistoriskeÅrService().hentTidligereÅrEkskludertNåværende(3)
        Assertions.assertNotNull(response)
        val forventet = listOf<Int>(detteÅret-1,detteÅret-2,detteÅret-3)
        Assertions.assertTrue(response.containsAll(forventet))
        println(response)
    }
    @Test
    fun SkalKunneHenteUt10År() {
        val detteÅret = Year.now().value
        val response = HistoriskeÅrService().hentTidligereÅrEkskludertNåværende(10)
        Assertions.assertNotNull(response)
        println(response)
        Assertions.assertTrue(response.contains(detteÅret-10))
        Assertions.assertFalse(response.contains(detteÅret))
        println(response)
    }
}