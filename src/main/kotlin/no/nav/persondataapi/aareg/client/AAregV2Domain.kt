package no.nav.persondataapi.aareg.client

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

enum class Identtype {
    AKTORID, FOLKEREGISTERIDENT, ORGANISASJONSNUMMER
}

enum class Entitet {
    Arbeidsforhold, Ansettelsesperiode, Permisjon, Permittering
}

data class Ident(
    val type: Identtype,
    val ident: String,
    val gjeldende: Boolean?,
)

data class Identer(
    val identer: List<Ident>
)

data class Kodeverksentitet(
    val kode: String,
    val beskrivelse: String,
)

data class Arbeidssted(
    val type: String, // "Underenhet,Person"
    val identer: List<Ident>
)

data class Opplysningspliktig(
    val type: String, // "Hovedenhet,Person"
    val identer: List<Ident>
)

data class Ansettelsesperiode(
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val sluttaarsak: Kodeverksentitet?,
    val varsling: Kodeverksentitet?,
    val sporingsinformasjon: Sporingsinformasjon?
)

data class Rapporteringsmaaneder(
    val fra: YearMonth,
    val til: YearMonth?,
)

data class Bruksperiode(
    val fom: String?, // LocalDateTime
    val tom: String?, // LocalDateTime
)

data class Ansettelsesdetaljer(
    val type: String, // "Ordinaer,Maritim,Forenklet,Frilanser"
    val arbeidstidsordning: Kodeverksentitet?,
    val ansettelsesform: Kodeverksentitet?,
    val yrke: Kodeverksentitet,
    val antallTimerPrUke: Double?,
    val avtaltStillingsprosent: Double?,
    val sisteStillingsprosentendring: LocalDate?,
    val sisteLoennsendring: LocalDate?,
    val rapporteringsmaaneder: Rapporteringsmaaneder, // v1.gyldighetsperiode
    val sporingsinformasjon: Sporingsinformasjon?
)

data class Arbeidsforhold(
    val id: String?,
    val type: Kodeverksentitet,
    val arbeidstaker: Identer,
    val arbeidssted: Arbeidssted,
    val opplysningspliktig: Opplysningspliktig,
    val ansettelsesperiode: Ansettelsesperiode,
    val ansettelsesdetaljer: List<Ansettelsesdetaljer>,
    val permisjoner: List<PermisjonPermittering>?,
    val permitteringer: List<PermisjonPermittering>?,
    val timerMedTimeloenn: List<TimerMedTimeloenn>?,
    val utenlandsopphold: List<Utenlandsopphold>?,
    val idHistorikk: List<IdHistorikk>?,
    val varsler: List<Varsel>?,
    val rapporteringsordning: Kodeverksentitet,
    val navArbeidsforholdId: Int,
    val navVersjon: Int,
    val navUuid: String,
    val opprettet: LocalDateTime,
    val sistBekreftet: LocalDateTime,
    val bruksperiode: Bruksperiode,
    val sporingsinformasjon: Sporingsinformasjon?,
    var organisasjoner : List<AaRegOrganisasjon> = emptyList()
)

data class Varsel(
    val entitet: Entitet,
    val varsling: Kodeverksentitet?
)

data class Utenlandsopphold(
    val land: Kodeverksentitet?,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val rapporteringsmaaneder: Rapporteringsmaaneder?,
    val sporingsinformasjon: Sporingsinformasjon?
)

data class TimerMedTimeloenn(
    val antall: Double,
    val startdato: String?,
    val sluttdato: String?,
    val rapporteringsmaaneder: Rapporteringsmaaneder?,
    val sporingsinformasjon: Sporingsinformasjon?
)

data class PermisjonPermittering(
    val id: String?,
    val type: Kodeverksentitet,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val prosent: Double,
    val varsling: Kodeverksentitet?,
    val idHistorikk: List<IdHistorikk>?,
    val sporingsinformasjon: Sporingsinformasjon?
)

data class IdHistorikk(
    val id: String?,
    val bruksperiode: Bruksperiode?
)

data class Sporingsinformasjon(
    val opprettetTidspunkt: LocalDateTime,
    val opprettetAv: String,
    val opprettetKilde: String,
    val opprettetKildereferanse: String,
    val endretTidspunkt: LocalDateTime,
    val endretAv: String,
    val endretKilde: String,
    val endretKildereferanse: String
)
data class AaRegAnsettelsesperiode(
    val bruksperiode: Bruksperiode,
    val periode: AaRegPeriode,
    val sporingsinformasjon: AaRegSporingsinformasjon,
    val varslingskode: String?
)

data class AaRegAntallTimerForTimeloennet(
    val antallTimer: Double,
    val periode: AaRegPeriode?,
    val rapporteringsperiode: YearMonth,
    val sporingsinformasjon: AaRegSporingsinformasjon
)

data class AaRegArbeidsavtale(
    val antallTimerPrUke: Double?,
    val arbeidstidsordning: String?,
    val beregnetAntallTimerPrUke: Double?,
    val bruksperiode: Bruksperiode,
    val gyldighetsperiode: AaRegGyldighetsperiode,
    val sistLoennsendring: String?,
    val sistStillingsendring: String?,
    val sporingsinformasjon: AaRegSporingsinformasjon,
    val stillingsprosent: Double?,
    val yrke: String,
    val fartsomraade: String?,
    val skipsregister: String?,
    val skipstype: String?

)

data class AaRegArbeidsforhold(
    val id :String,
    val ansettelsesperiode: AaRegAnsettelsesperiode?,
    val antallTimerForTimeloennet: List<AaRegAntallTimerForTimeloennet>?,
    val arbeidsavtaler: List<AaRegArbeidsavtale>,
    val arbeidsforholdId: String?,
    val arbeidsgiver: AaRegOpplysningspliktigArbeidsgiver?,
    val arbeidstaker: AaRegPerson?,
    val innrapportertEtterAOrdningen: Boolean,
    val navArbeidsforholdId: Int,
    val opplysningspliktig: AaRegOpplysningspliktigArbeidsgiver?,
    val permisjonPermitteringer: List<AaRegPermisjonPermittering>?,
    val registrert: LocalDateTime?,
    val sistBekreftet: LocalDateTime?,
    val sporingsinformasjon: AaRegSporingsinformasjon?,
    val type: Kodeverksentitet?,
    val utenlandsopphold: List<AaRegUtenlandsopphold>?
)

data class AaRegArbeidsgiverArbeidsforhold(
    val antall: Int,
    val arbeidsforhold: List<AaRegArbeidsforhold>
)

enum class AaRegOpplysningspliktigArbeidsgiverType {
    Organisasjon, Person
}

data class AaRegGyldighetsperiode(
    val fom: LocalDate,
    val tom: LocalDate?
)

data class AaRegOpplysningspliktigArbeidsgiver(
    val type: AaRegOpplysningspliktigArbeidsgiverType,
    val organisasjonsnummer: String?,
    val aktoerId: String?,
    val offentligIdent: String?
)

enum class AaRegOrganisasjonType {
    Organisasjon
}

data class AaRegOrganisasjon(
    val type: AaRegOrganisasjonType,
    val organisasjonsnummer: String
)

data class AaRegPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?
)

data class AaRegPermisjonPermittering(
    val periode: AaRegPeriode?,
    val permisjonPermitteringId: String,
    val prosent: Double?,
    val sporingsinformasjon: AaRegSporingsinformasjon,
    val type: String,
    val varslingskode: String?
)

enum class AaRegPersonType {
    Person
}

data class AaRegPerson(
    val type: AaRegPersonType,
    val aktoerId: String,
    val offentligIdent: String
)

data class AaRegSporingsinformasjon(
    val endretAv: String?,
    val endretKilde: String?,
    val endretKildeReferanse: String?,
    val endretTidspunkt: LocalDateTime?,
    val opprettetAv: String,
    val opprettetKilde: String?,
    val opprettetKildereferanse: String?,
    val opprettetTidspunkt: LocalDateTime?
)

data class AaRegTjenestefeilResponse(
    val melding: String
)

data class AaRegUtenlandsopphold(
    val landkode: String,
    val periode: AaRegPeriode?,
    val rapporteringsperiode: YearMonth,
    val sporingsinformasjon: AaRegSporingsinformasjon
)

fun List<Arbeidsforhold>.hentIdenter(): List<Ident> {
    val identer = mutableListOf<Ident>()
    forEach {
        identer.addAll(it.arbeidssted.identer)
        identer.addAll(it.opplysningspliktig.identer)
    }
    return identer
}