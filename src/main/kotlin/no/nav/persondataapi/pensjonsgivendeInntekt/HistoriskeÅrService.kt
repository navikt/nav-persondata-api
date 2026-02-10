package no.nav.persondataapi.pensjonsgivendeInntekt

import java.time.Year

class `HistoriskeÅrService` {

    fun hentTidligereÅrEkskludertNåværende(antallår : Int):List<Int>{
        val `årListe` = (0 until antallår).map { Year.now().minusYears(1).value - it }
        return `årListe`
    }


}