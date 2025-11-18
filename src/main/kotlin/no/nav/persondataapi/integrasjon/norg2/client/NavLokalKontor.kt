package no.nav.persondataapi.integrasjon.norg2.client

import no.nav.persondataapi.rest.oppslag.Maskert


data class NavLokalKontor(
    val enhetId: Long,
    val navn: String,
    val enhetNr: String,
    val type: String,
)