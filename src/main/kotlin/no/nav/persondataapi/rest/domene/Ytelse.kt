package no.nav.persondataapi.rest.domene

import java.math.BigDecimal
import java.time.LocalDate

data class Ytelse(
	val stonadType: String,
	val perioder: List<PeriodeInformasjon>,
) {
	data class PeriodeInformasjon(
		val periode: Periode,
		val beløp: BigDecimal,
		val kilde: String,
		val info: String?,
	)

	data class Periode(
		val fom: LocalDate,
		val tom: LocalDate,
	)
}
