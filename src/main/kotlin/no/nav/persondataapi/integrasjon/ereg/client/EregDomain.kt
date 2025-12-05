package no.nav.persondataapi.integrasjon.ereg.client

import java.time.LocalDate
import java.time.LocalDateTime

data class EregRespons(
    val organisasjonsnummer: String,
    val type: String?,
    val navn: Navn?,
)

data class Navn(
    val sammensattnavn: String,
    val navnelinje1: String,
    val navnelinje2: String? = null,
    val navnelinje3: String? = null,
    val navnelinje4: String? = null,
    val navnelinje5: String? = null,
    val bruksperiode: PeriodeTid,
    val gyldighetsperiode: PeriodeDato
)

/** Perioder */
data class PeriodeTid(
    val fom: LocalDateTime,
    val tom: LocalDateTime? = null
)

data class PeriodeDato(
    val fom: LocalDate,
    val tom: LocalDate? = null
)
