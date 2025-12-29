package no.nav.persondataapi.integrasjon.aareg.client

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
    val type: no.nav.persondataapi.integrasjon.aareg.client.Identtype,
    val ident: String,
    val gjeldende: Boolean?,
)

data class Identer(
    val identer: List<no.nav.persondataapi.integrasjon.aareg.client.Ident>
)

data class Kodeverksentitet(
    val kode: String,
    val beskrivelse: String,
)

data class Arbeidssted(
    val type: String, // "Underenhet,Person"
    val identer: List<no.nav.persondataapi.integrasjon.aareg.client.Ident>
)

data class Opplysningspliktig(
    val type: String, // "Hovedenhet,Person"
    val identer: List<no.nav.persondataapi.integrasjon.aareg.client.Ident>
)

data class Ansettelsesperiode(
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val sluttaarsak: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet?,
    val varsling: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet?,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.Sporingsinformasjon?
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
    val arbeidstidsordning: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet?,
    val ansettelsesform: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet?,
    val yrke: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet,
    val antallTimerPrUke: Double?,
    val avtaltStillingsprosent: Double?,
    val sisteStillingsprosentendring: LocalDate?,
    val sisteLoennsendring: LocalDate?,
    val rapporteringsmaaneder: no.nav.persondataapi.integrasjon.aareg.client.Rapporteringsmaaneder, // v1.gyldighetsperiode
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.Sporingsinformasjon?
)

data class Arbeidsforhold(
    val id: String?,
    val type: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet,
    val arbeidstaker: no.nav.persondataapi.integrasjon.aareg.client.Identer,
    val arbeidssted: no.nav.persondataapi.integrasjon.aareg.client.Arbeidssted,
    val opplysningspliktig: no.nav.persondataapi.integrasjon.aareg.client.Opplysningspliktig,
    val ansettelsesperiode: no.nav.persondataapi.integrasjon.aareg.client.Ansettelsesperiode,
    val ansettelsesdetaljer: List<no.nav.persondataapi.integrasjon.aareg.client.Ansettelsesdetaljer>,
    val permisjoner: List<no.nav.persondataapi.integrasjon.aareg.client.PermisjonPermittering>?,
    val permitteringer: List<no.nav.persondataapi.integrasjon.aareg.client.PermisjonPermittering>?,
    val timerMedTimeloenn: List<no.nav.persondataapi.integrasjon.aareg.client.TimerMedTimeloenn>?,
    val idHistorikk: List<no.nav.persondataapi.integrasjon.aareg.client.IdHistorikk>?,
    val varsler: List<no.nav.persondataapi.integrasjon.aareg.client.Varsel>?,
    val rapporteringsordning: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet,
    val navArbeidsforholdId: Int,
    val navVersjon: Int,
    val navUuid: String,
    val opprettet: LocalDateTime,
    val sistBekreftet: LocalDateTime,
    val bruksperiode: no.nav.persondataapi.integrasjon.aareg.client.Bruksperiode,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.Sporingsinformasjon?,
    var organisasjoner : List<no.nav.persondataapi.integrasjon.aareg.client.AaRegOrganisasjon> = emptyList()
)

data class Varsel(
    val entitet: no.nav.persondataapi.integrasjon.aareg.client.Entitet,
    val varsling: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet?
)

data class TimerMedTimeloenn(
    val antall: Double,
    val startdato: String?,
    val sluttdato: String?,
    val rapporteringsmaaneder: no.nav.persondataapi.integrasjon.aareg.client.Rapporteringsmaaneder?,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.Sporingsinformasjon?
)

data class PermisjonPermittering(
    val id: String?,
    val type: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val prosent: Double,
    val varsling: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet?,
    val idHistorikk: List<no.nav.persondataapi.integrasjon.aareg.client.IdHistorikk>?,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.Sporingsinformasjon?
)

data class IdHistorikk(
    val id: String?,
    val bruksperiode: no.nav.persondataapi.integrasjon.aareg.client.Bruksperiode?
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
    val bruksperiode: no.nav.persondataapi.integrasjon.aareg.client.Bruksperiode,
    val periode: no.nav.persondataapi.integrasjon.aareg.client.AaRegPeriode,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.AaRegSporingsinformasjon,
    val varslingskode: String?
)

data class AaRegAntallTimerForTimeloennet(
    val antallTimer: Double,
    val periode: no.nav.persondataapi.integrasjon.aareg.client.AaRegPeriode?,
    val rapporteringsperiode: YearMonth,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.AaRegSporingsinformasjon
)

data class AaRegArbeidsavtale(
    val antallTimerPrUke: Double?,
    val arbeidstidsordning: String?,
    val beregnetAntallTimerPrUke: Double?,
    val bruksperiode: no.nav.persondataapi.integrasjon.aareg.client.Bruksperiode,
    val gyldighetsperiode: no.nav.persondataapi.integrasjon.aareg.client.AaRegGyldighetsperiode,
    val sistLoennsendring: String?,
    val sistStillingsendring: String?,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.AaRegSporingsinformasjon,
    val stillingsprosent: Double?,
    val yrke: String,
    val fartsomraade: String?,
    val skipsregister: String?,
    val skipstype: String?

)

data class AaRegArbeidsforhold(
    val id :String,
    val ansettelsesperiode: no.nav.persondataapi.integrasjon.aareg.client.AaRegAnsettelsesperiode?,
    val antallTimerForTimeloennet: List<no.nav.persondataapi.integrasjon.aareg.client.AaRegAntallTimerForTimeloennet>?,
    val arbeidsavtaler: List<no.nav.persondataapi.integrasjon.aareg.client.AaRegArbeidsavtale>,
    val arbeidsforholdId: String?,
    val arbeidsgiver: no.nav.persondataapi.integrasjon.aareg.client.AaRegOpplysningspliktigArbeidsgiver?,
    val arbeidstaker: no.nav.persondataapi.integrasjon.aareg.client.AaRegPerson?,
    val innrapportertEtterAOrdningen: Boolean,
    val navArbeidsforholdId: Int,
    val opplysningspliktig: no.nav.persondataapi.integrasjon.aareg.client.AaRegOpplysningspliktigArbeidsgiver?,
    val permisjonPermitteringer: List<no.nav.persondataapi.integrasjon.aareg.client.AaRegPermisjonPermittering>?,
    val registrert: LocalDateTime?,
    val sistBekreftet: LocalDateTime?,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.AaRegSporingsinformasjon?,
    val type: no.nav.persondataapi.integrasjon.aareg.client.Kodeverksentitet?,
)

data class AaRegArbeidsgiverArbeidsforhold(
    val antall: Int,
    val arbeidsforhold: List<no.nav.persondataapi.integrasjon.aareg.client.AaRegArbeidsforhold>
)

enum class AaRegOpplysningspliktigArbeidsgiverType {
    Organisasjon, Person
}

data class AaRegGyldighetsperiode(
    val fom: LocalDate,
    val tom: LocalDate?
)

data class AaRegOpplysningspliktigArbeidsgiver(
    val type: no.nav.persondataapi.integrasjon.aareg.client.AaRegOpplysningspliktigArbeidsgiverType,
    val organisasjonsnummer: String?,
    val aktoerId: String?,
    val offentligIdent: String?
)

enum class AaRegOrganisasjonType {
    Organisasjon
}

data class AaRegOrganisasjon(
    val type: no.nav.persondataapi.integrasjon.aareg.client.AaRegOrganisasjonType,
    val organisasjonsnummer: String
)

data class AaRegPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?
)

data class AaRegPermisjonPermittering(
    val periode: no.nav.persondataapi.integrasjon.aareg.client.AaRegPeriode?,
    val permisjonPermitteringId: String,
    val prosent: Double?,
    val sporingsinformasjon: no.nav.persondataapi.integrasjon.aareg.client.AaRegSporingsinformasjon,
    val type: String,
    val varslingskode: String?
)

enum class AaRegPersonType {
    Person
}

data class AaRegPerson(
    val type: no.nav.persondataapi.integrasjon.aareg.client.AaRegPersonType,
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

fun List<no.nav.persondataapi.integrasjon.aareg.client.Arbeidsforhold>.hentIdenter(): List<no.nav.persondataapi.integrasjon.aareg.client.Ident> {
    val identer = mutableListOf<no.nav.persondataapi.integrasjon.aareg.client.Ident>()
    forEach {
        identer.addAll(it.arbeidssted.identer)
        identer.addAll(it.opplysningspliktig.identer)
    }
    return identer
}
