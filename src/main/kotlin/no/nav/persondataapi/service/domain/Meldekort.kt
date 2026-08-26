package no.nav.persondataapi.service.domain

import no.nav.persondataapi.service.Tema
import java.time.LocalDate
import java.time.LocalDateTime

data class AapMeldekortDto(
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtakPeriode: ÅpenPeriode,
    val rettighetsType: String,
    val kide: String,
    val tema: Tema,
    val perioder: List<AapMeldekortPeriode> = emptyList(),
    val vedtaktypeNavn: String?,
)

data class AapMeldekortPeriode(
    val fraOgMed: LocalDate,
    // Nullable: Kelvin lar tilOgMedDato være null for å indikere en ÅPEN
    // periode (løper fra fraOgMed og videre uten kjent sluttdato), samme
    // konvensjon som ÅpenPeriode og timerMedTimeloenn.sluttdato ellers i
    // kodebasen. IKKE tolk null som en éndags-periode.
    val tilOgMed: LocalDate?,
    val arbeidetTimer: Double?,
    val annenReduksjon: Float?,
    val utbetalingsgrad: Int?,
    // Dag-for-dag-nedbryting av arbeidetTimer, tilsvarende DagpengerMeldekortDag
    // for dagpenger. Utledet fra /holmes/arbeidstimer sine (RLE-komprimerte)
    // segmenter — se MeldekortService.beregnArbeidPerDagFraHolmesSegmenter.
    // Tom liste hvis Holmes-endepunktet ikke har overlappende data for perioden.
    val arbeidPerDag: List<AapArbeidPerDag> = emptyList(),
)

data class AapArbeidPerDag(
    val dag: LocalDate,
    val timerArbeidet: Double,
)

data class DagpengerMeldekortDto(
    val dager: List<DagpengerMeldekortDag>,
    val id: String,
    val periode: PeriodeDto,
    val opprettetAv: String,
    val migrert: Boolean,
    val kilde: KildeDto,
    val innsendtTidspunkt: LocalDateTime?,
    val registrertArbeidssoker: Boolean?,
    val meldedato: LocalDate?,
)

data class ÅpenPeriode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
)

data class PeriodeDto(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
)

data class KildeDto(
    val rolle: String,
    val ident: String,
)

data class DagpengerMeldekortDag(
    val dato: LocalDate,
    val aktiviteter: List<AktivitetDto>,
    val dagIndex: Int,
)

data class AktivitetDto(
    val id: String,
    val type: AktivitetTypeDto,
    val timer: Double?,
    val dato: LocalDate?,
)

enum class AktivitetTypeDto {
    Arbeid,
    Fravaer,
    Syk,
    Utdanning,
    Annet,
}
