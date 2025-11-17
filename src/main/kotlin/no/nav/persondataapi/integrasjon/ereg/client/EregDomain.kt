package no.nav.persondataapi.integrasjon.ereg.client

import java.time.LocalDate
import java.time.LocalDateTime

data class EregRespons(
	val organisasjonsnummer: String,
	val type: String?,
	val navn: Navn?,
	val organisasjonDetaljer: OrganisasjonDetaljer?,
	val virksomhetDetaljer: VirksomhetDetaljer?,
)

data class Navn(
	val sammensattnavn: String,
	val navnelinje1: String,
	val navnelinje2: String? = null,
	val navnelinje3: String? = null,
	val navnelinje4: String? = null,
	val navnelinje5: String? = null,
	val bruksperiode: PeriodeTid,
	val gyldighetsperiode: PeriodeDato,
)

/** Perioder */
data class PeriodeTid(
	val fom: LocalDateTime,
	val tom: LocalDateTime? = null,
)

data class PeriodeDato(
	val fom: LocalDate,
	val tom: LocalDate? = null,
)

/** Detaljer */
data class OrganisasjonDetaljer(
	val registreringsdato: LocalDateTime,
	val enhetstyper: List<EnhetstypeDetalj>,
	val navn: List<Navn>,
	val naeringer: List<Naering> = emptyList(),
	val forretningsadresser: List<Adresse> = emptyList(),
	val postadresser: List<Adresse> = emptyList(),
	val epostadresser: List<EpostAdresse> = emptyList(),
	val internettadresser: List<InternettAdresse> = emptyList(),
	val ansatte: List<Ansatte> = emptyList(),
	val navSpesifikkInformasjon: NavSpesifikkInformasjon,
	val sistEndret: LocalDate,
)

data class EnhetstypeDetalj(
	val enhetstype: String,
	val bruksperiode: PeriodeTid,
	val gyldighetsperiode: PeriodeDato,
)

data class Naering(
	val naeringskode: String,
	val hjelpeenhet: Boolean,
	val bruksperiode: PeriodeTid,
	val gyldighetsperiode: PeriodeDato,
)

/** Adresse gjenbrukes for både forretnings- og postadresse */
data class Adresse(
	val type: String,
	val adresselinje1: String,
	val adresselinje2: String? = null,
	val adresselinje3: String? = null,
	val postnummer: String,
	val poststed: String? = null,
	val landkode: String? = null,
	val kommunenummer: String? = null,
	val bruksperiode: PeriodeTid,
	val gyldighetsperiode: PeriodeDato,
)

data class EpostAdresse(
	val adresse: String,
	val bruksperiode: PeriodeTid,
	val gyldighetsperiode: PeriodeDato,
)

data class InternettAdresse(
	val adresse: String,
	val bruksperiode: PeriodeTid,
	val gyldighetsperiode: PeriodeDato,
)

data class Ansatte(
	val antall: Int,
	val bruksperiode: PeriodeTid,
	val gyldighetsperiode: PeriodeDato,
)

data class NavSpesifikkInformasjon(
	val erIA: Boolean,
	val bruksperiode: PeriodeTid,
	val gyldighetsperiode: PeriodeDato,
)

data class VirksomhetDetaljer(
	val enhetstype: String,
	val oppstartsdato: LocalDate? = null,
)
