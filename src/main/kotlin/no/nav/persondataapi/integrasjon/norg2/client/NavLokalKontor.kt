package no.nav.persondataapi.integrasjon.norg2.client


data class NavLokalKontor(
    val enhetId: Long,
    val navn: String,
    val enhetNr: String,
    val type: String,
)