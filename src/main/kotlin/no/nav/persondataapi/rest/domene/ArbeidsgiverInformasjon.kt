package no.nav.persondataapi.rest.domene

import no.nav.persondataapi.rest.oppslag.Maskert
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import java.util.Locale

data class ArbeidsgiverInformasjon(
    val løpendeArbeidsforhold : List<ArbeidsgiverData>,
    val historikk:List<ArbeidsgiverData>
) {
    data class ArbeidsgiverData(
        @Maskert
        val arbeidsgiver:String,
        @Maskert
        val organisasjonsnummer: String,
        @Maskert
        val adresse: String,
        val ansettelsesDetaljer:List<AnsettelsesDetalj>,
        val id:String,
    )

    data class AnsettelsesDetalj(
        val type: String,
        val stillingsprosent: Double?,
        val antallTimerPrUke: Double?,
        val periode: ÅpenPeriode,
        val yrke: String? = null
    )
    data class ÅpenPeriode(
        val fom: YearMonth,
        val tom: YearMonth?,
    )
}
