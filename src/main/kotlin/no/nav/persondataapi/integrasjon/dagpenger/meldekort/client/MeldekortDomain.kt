package no.nav.persondataapi.integrasjon.dagpenger.meldekort.client

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime


data class MeldekortRequest(
    val personIdent : String,
    val fraOgMedDato:String,
    val tilOgMedDato:String,
)


data class Meldekort(
    val id: String,                     // "1915708190" → ikke UUID
    val ident: String,
    val status: MeldekortStatus,
    val type: MeldekortType,

    val periode: Periode,
    val dager: List<Dag>,

    val kanSendes: Boolean,
    val kanEndres: Boolean,
    val kanSendesFra: LocalDate,
    val sisteFristForTrekk: LocalDate?,

    val opprettetAv: String,
    val migrert: Boolean,               // Nytt felt i faktisk API

    val kilde: Kilde,

    val innsendtTidspunkt: LocalDateTime?,
    val registrertArbeidssoker: Boolean?,
    val meldedato: LocalDate?
)


enum class MeldekortStatus {
    Innsendt,
    TilUtfylling
}

enum class MeldekortType {
    Ordinaert,
    Etterregistrert
}

data class Periode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)

data class Dag(
    val dato: LocalDate,
    val aktiviteter: List<Aktivitet>,
    val dagIndex: Int
)

data class Aktivitet(
    val id: String,                   // Ikke alltid UUID-format
    val type: AktivitetType,
    val timer: String?,               // Kan mangle!
    val dato: LocalDate? = null       // Kan mangle (viktig!)
)
 fun Aktivitet.timerAsDouble(): Double? {
    if (this.timer != null) {
        val duration = Duration.parse(this.timer)
        val hoursDecimal = duration.toMinutes().toDouble() / 60.0
        return hoursDecimal
    }
    else{
        return null
    }
}

enum class AktivitetType {
    Arbeid,
    Fravaer,
    Syk,
    Utdanning
}

data class Kilde(
    val rolle: String,
    val ident: String
)