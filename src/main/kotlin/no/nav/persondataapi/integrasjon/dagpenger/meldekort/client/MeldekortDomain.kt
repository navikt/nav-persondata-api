package no.nav.persondataapi.integrasjon.dagpenger.meldekort.client

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class MeldekortRequest(
    val personIdent : String,
    val fraOgMedDato:String,
    val tilOgMedDato:String,
)


data class Meldekort(
    val id: UUID,
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
    val originalMeldekortId: UUID?,
    val kilde: Kilde,
    val innsendtTidspunkt: LocalDateTime?,
    val meldedato: LocalDate?,
    val registrertArbeidssoker: Boolean?,
    val begrunnelse: String
)

enum class MeldekortStatus {
    TilUtfylling,
    Innsendt
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
    val type: String, // Alltid "dag" i datasettet
    val dagIndex: Int,
    val dato: LocalDate,
    val aktiviteter: List<Aktivitet>
)

data class Aktivitet(
    val id: UUID,
    val type: AktivitetType,  // Nå ENUM
    val dato: LocalDate,
    val timer: String         // ISO-8601 "PT2H", "PT7H30M", eller tom streng
)

enum class AktivitetType {
    Arbeid,
    Syk,
    Utdanning,
    Fravaer
}

data class Kilde(
    val rolle: String,  // "Bruker" eller "Saksbehandler"
    val ident: String
)
