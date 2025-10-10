package no.nav.persondataapi.rest.domain

import java.time.YearMonth

data class ArbeidsgiverInformasjon(
    val løpendeArbeidsforhold : List<ArbeidsgiverData>,
    val historikk:List<ArbeidsgiverData>
) {
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
        val periode: ÅpenPeriode,
        val yrke: String? = null
    )
    data class ÅpenPeriode(
        val fom: YearMonth,
        val tom: YearMonth?,
    )
}
