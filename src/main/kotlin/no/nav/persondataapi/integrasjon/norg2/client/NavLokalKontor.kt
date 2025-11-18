package no.nav.persondataapi.integrasjon.norg2.client

import no.nav.persondataapi.rest.oppslag.Maskert


data class NavLokalKontor(
    @Maskert
    val enhetId: Long,
    @Maskert
    val navn: String,
    @Maskert
    val enhetNr: String,
    val type: String,
)