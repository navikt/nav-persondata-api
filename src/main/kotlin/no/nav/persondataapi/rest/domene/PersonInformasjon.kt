package no.nav.persondataapi.rest.domene

import no.nav.persondataapi.rest.oppslag.Maskert

data class PersonInformasjon(
    val aktørId: String?,
    val familemedlemmer : Map<String,String> = emptyMap<String, String>(),
    val statsborgerskap: List<String> = emptyList(),
    val navn: Navn,
    val adresse: Bostedsadresse? =null,
    val sivilstand: String? = null,
    val alder: Int,
    val adresseBeskyttelse: Skjerming = Skjerming.UGRADERT,
    val fødselsdato: String,
    val dødsdato: String? = null,
) {
    data class Navn(
        @Maskert
        val fornavn: String,
        @Maskert
        val mellomnavn: String?,
        @Maskert
        val etternavn: String,
    )

    data class Bostedsadresse(
        val norskAdresse: NorskAdresse?,
        val utenlandskAdresse: UtenlandskAdresse?
    )

    data class NorskAdresse(
        @Maskert
        val adressenavn: String?,
        @Maskert("*")
        val husnummer: String?,
        @Maskert("*")
        val husbokstav: String?,

        @Maskert("****")
        val postnummer: String?,
        @Maskert
        val kommunenummer:String?,
        @Maskert
        val poststed: String?,
    )

    data class UtenlandskAdresse(
        @Maskert
        val adressenavnNummer: String?,
        @Maskert("*")
        val bygningEtasjeLeilighet: String?,
        @Maskert
        val postboksNummerNavn: String?,
        @Maskert("*****")
        val postkode: String?,
        @Maskert
        val bySted: String?,
        @Maskert
        val regionDistriktOmråde: String?,
        @Maskert
        val landkode: String
    )
    enum class Skjerming {
        UGRADERT,
        FORTROLIG,
        STRENGT_FORTROLIG,
        STRENGT_FORTROLIG_UTLAND
    }

}
