package no.nav.persondataapi.rest.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class OppslagBrukerRespons(
    val utrekkstidspunkt: LocalDateTime,
    val saksbehandlerIdent: String,
    val fødselsnummer: String,
    val personInformasjon: PersonInformasjon?,
    val arbeidsgiverInformasjon: ArbeidsgiverInformasjon?,
    val inntektInformasjon: InntektInformasjon?,
    val stønader: List<Stonad> = emptyList()
)

data class PersonInformasjon(
    val aktørId: String?,
    val familemedlemmer : Map<String,String> = emptyMap<String, String>(),
    val statsborgerskap: List<String> = emptyList(),
    val navn: Navn,
    val adresse: Bostedsadresse? =null,
    val sivilstand: String? = null,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

data class Bostedsadresse(
    val norskAdresse: NorskAdresse?,
    val utenlandskAdresse: UtenlandskAdresse?
)

data class NorskAdresse(
    val adressenavn: String?,
    val husnummer: String?,
    val husbokstav: String?,

    val postnummer: String?,
    val kommunenummer:String?,
    val poststed: String?,
)

data class UtenlandskAdresse(
    val adressenavnNummer: String?,
    val bygningEtasjeLeilighet: String?,
    val postboksNummerNavn: String?,
    val postkode: String?,
    val bySted: String?,
    val regionDistriktOmråde: String?,
    val landkode: String
)


data class InntektInformasjon(
    val loennsinntekt : List<LoensDetaljer> = emptyList(),
    val naringsInntekt : List<LoensDetaljer> = emptyList(),
    val PensjonEllerTrygd : List<LoensDetaljer> = emptyList(),
    val YtelseFraOffentlige : List<LoensDetaljer> = emptyList(),
)

data class ArbeidsgiverInformasjon(
    val løpendeArbeidsforhold : List<ArbeidsgiverData>,
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
    val yrke: String? = null
)
data class OpenPeriode(
    val fom: YearMonth,
    val tom: YearMonth?,
)
/*
* YTELSER_OG_STONADER
* */
data class Stonad(
    val stonadType: String,
    val perioder : List<PeriodeInformasjon>
)
data class PeriodeInformasjon(
    val periode: Periode,
    val beløp: BigDecimal,
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
* Lønn fra A-Meldingen
* */
data class LoensDetaljer(
    val arbeidsgiver: String?,
    val periode:String,
    val arbeidsforhold:String,
    val stillingsprosent: String?,
    val lonnstype: String?,
    val antall : BigDecimal? = null,
    val belop: BigDecimal?,
    val harFlereVersjoner:Boolean = false,

    )
