package no.nav.persondataapi.rest.domene

import no.nav.persondataapi.rest.oppslag.Maskert
import java.time.LocalDate
import java.time.YearMonth

data class ArbeidsgiverInformasjon(
    val løpendeArbeidsforhold: List<ArbeidsgiverData>,
    val historikk: List<ArbeidsgiverData>,
) {
    data class ArbeidsgiverData(
        @Maskert
        val arbeidsgiver: String,
        @Maskert
        val organisasjonsnummer: String,
        val ansettelsesDetaljer: List<AnsettelsesDetalj>,
        val timerMedTimeloenn: List<TimerMedTimeloennDto>,
        val id: String,
    )

    data class TimerMedTimeloennDto(
        val antall: Double,
        val startdato: String?,
        val sluttdato: String?,
        val rapporteringsmaaneder: Rapporteringsperiode?,
    )

    data class AnsettelsesDetalj(
        val type: String,
        val stillingsprosent: Double?,
        val antallTimerPrUke: Double?,
        val periode: ÅpenPeriode,
        val yrke: String? = null,
    )

    /** Ansettelsesperiode med eksakte datoer (dag-presisjon) */
    data class ÅpenPeriode(
        val fom: LocalDate,
        var tom: LocalDate?,
    )

    /** Rapporteringsperiode på månedsnivå (år-måned) */
    data class Rapporteringsperiode(
        val fom: YearMonth,
        var tom: YearMonth?,
    )
}
