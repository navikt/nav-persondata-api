package no.nav.persondataapi.rest.domain

import no.nav.persondataapi.aareg.client.Arbeidsforhold
import no.nav.persondataapi.ereg.client.Adresse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class OppslagBrukerRespons(
    val utrekkstidspunkt: LocalDateTime,
    val saksbehandlerIdent: String,
    val fodselsnr: String,
)

data class PersonInformasjon(
    val navn: String,
    val aktorId: String?,
    val adresse: String?,
    val familemedllemmer : String
)


data class ArbeidsgiverInformasjon(
    val lopendeArbeidsforhold : List<ArbeidsgiverData>,
    val historikk:List<ArbeidsgiverData>
)

data class ArbeidsgiverData(
    val arbeidsgiver:String,
    val organisasjonsnummer: String,
    val adresse: String,
    val ansettelsesDetaljer:List<AnsettelsesDetalj>,
)



data class AnsettelsesDetalj(
    val type: String,
    val stillingsprosent: Double?,
    val antallTimerPrUke: Double?,
    val periode: OpenPeriode,
)
data class OpenPeriode(
    val fom: YearMonth,
    val tom: YearMonth?,
)
/*
* YTELSER_OG_STONADER
* */
data class ytelserOgStonaderInformasjon(
    val stonader:List<Stonad>
)
data class Stonad(
    val stonadType: String,
    val perioder : List<PeriodeInformasjon>
)
data class PeriodeInformasjon(
    val periode: Periode,
    val beløp: Int,
    val kilde: String,
    val info: String?
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)

/*
* Utbealinger
* */

data class UtbetakingInformasjon(
    val utbetalinger: List<UtbetalingInfo>
)
data class UtbetalingInfo(
    val dato: LocalDate,
    val belop: Int,
    val beskrivelse: String
)


/*
* Lønn fra A-Meldinge
* */

data class LoensInformasjon(
    val lønnsInformasjon: List<LoensDetaljer>,
)

data class LoensDetaljer(
    val arbeidsgiver: String?,
    val periode:String,
    val arbeidsforhold:String,
    val stillingsprosent: String?,
    val lonnstype: String?,
    val timer :Int?,
    val belop: Int

)
