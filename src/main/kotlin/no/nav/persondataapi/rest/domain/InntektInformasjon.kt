package no.nav.persondataapi.rest.domain

import java.math.BigDecimal

data class InntektInformasjon(
    val lønnsinntekt : List<Lønnsdetaljer> = emptyList(),
    val næringsinntekt : List<Lønnsdetaljer> = emptyList(),
    val pensjonEllerTrygd : List<Lønnsdetaljer> = emptyList(),
    val ytelseFraOffentlige : List<Lønnsdetaljer> = emptyList(),
) {
    data class Lønnsdetaljer(
        val arbeidsgiver: String?,
        val periode:String,
        val arbeidsforhold:String,
        val stillingsprosent: String?,
        val lønnstype: String?,
        val antall : BigDecimal? = null,
        val beløp: BigDecimal?,
        val harFlereVersjoner:Boolean = false,

        )
}
