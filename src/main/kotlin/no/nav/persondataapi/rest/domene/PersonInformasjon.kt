package no.nav.persondataapi.rest.domene

data class PersonInformasjon(
    val aktørId: String?,
    val familemedlemmer : Map<String,String> = emptyMap<String, String>(),
    val statsborgerskap: List<String> = emptyList(),
    val navn: Navn,
    val adresse: Bostedsadresse? =null,
    val sivilstand: String? = null,
    val alder: Int,
) {
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
}
