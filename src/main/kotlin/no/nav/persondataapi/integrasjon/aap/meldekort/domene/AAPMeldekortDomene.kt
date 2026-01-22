package no.nav.persondataapi.integrasjon.aap.meldekort.domene

import java.time.LocalDate


data class AAPMaximumRequest(
    val personidentifikator: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String,
)

data class AAPMaxRespons(
    val vedtak: List<Vedtak>
)
data class Vedtak(
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtaksdato: String, // evt. LocalDate
    val periode: Periode,
    val rettighetsType: String,

    val dagsats: Int,
    val dagsatsEtterUforeReduksjon: Int?,
    val beregningsgrunnlag: Int,

    val barnMedStonad: Int,
    val barnetillegg: Int,

    val kildesystem: String,

    val samordningsId: String?,
    val opphorsAarsak: String?,
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,

    val utbetaling: List<Utbetaling>
)
data class Utbetaling(
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val utbetalingsgrad: Int,

    val reduksjon: Reduksjon?,
    val barnetilegg: Int?,
    val barnetillegg: Int?
)
data class Periode(
    val fraOgMedDato: LocalDate, // evt. LocalDate
    val tilOgMedDato: LocalDate  // evt. LocalDate
)

data class Reduksjon(
    val annenReduksjon : String,
    val timerArbeidet : String

)