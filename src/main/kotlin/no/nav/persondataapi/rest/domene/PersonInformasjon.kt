package no.nav.persondataapi.rest.domene

import no.nav.persondataapi.rest.oppslag.Maskert

data class PersonInformasjon(
    val aktørId: String?,
    val familemedlemmer: List<Familiemedlem> = emptyList(),
    val statsborgerskap: List<String> = emptyList(),
    val navn: Navn,
    val adresse: Bostedsadresse? = null,
    val adresseHistorikk: List<HistoriskAdresse> = emptyList(),
    val telefonnummer: List<Telefonnummer> = emptyList(),
    val sivilstand: String? = null,
    val alder: Int,
    val adressebeskyttelse: Skjerming = Skjerming.UGRADERT,
    val fødselsdato: String,
    val dødsdato: String? = null,
    val navKontor: NavKontor? = null,
    val historiskeIdenter: List<HistoriskIdent> = emptyList(),
) {
    data class Familiemedlem(
        val ident: String,
        val rolle: String,
        val fornavn: String? = null,
        val mellomnavn: String? = null,
        val etternavn: String? = null,
        val fødselsdato: String? = null,
        val adressebeskyttelse: Skjerming = Skjerming.UGRADERT,
    )

    data class Navn(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
    )

    data class Bostedsadresse(
        val norskAdresse: NorskAdresse?,
        val utenlandskAdresse: UtenlandskAdresse?,
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
        val kommunenummer: String?,
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
        val landkode: String,
    )

    data class NavKontor(
        @Maskert
        val enhetId: Long,
        @Maskert
        val navn: String,
        @Maskert
        val enhetNr: String,
        val type: String,
    )

    data class Telefonnummer(
        @Maskert
        val landskode: String,
        @Maskert("*")
        val nummer: String,
        val prioritet: Int,
    )

    data class HistoriskAdresse(
        val adresse: Bostedsadresse,
        val gyldigFraOgMed: String?,
        val gyldigTilOgMed: String?,
    )

    enum class Skjerming {
        UGRADERT,
        FORTROLIG,
        STRENGT_FORTROLIG,
        STRENGT_FORTROLIG_UTLAND,
    }
}

/**
 * En folkeregisteridentifikator (FNR eller D-nummer), med informasjon om den er historisk.
 *
 * [personIdent] maskeres bevisst ikke, selv om resten av responsen maskeres for en
 * saksbehandler uten full tilgang. Fødselsnummer regnes ikke som geoidentifiserende og
 * er nødvendig for konsumenter (f.eks. watson-admin-api) til korrelasjon og videre
 * tilgangskontroll — kun geoidentifiserende informasjon (adresse, poststed, kommunenummer,
 * NAV-kontor) og kontaktinfo skal maskeres, se øvrige @Maskert-felter i [PersonInformasjon].
 */
data class HistoriskIdent(
    val personIdent: String,
    val type: String,
    val historisk: Boolean,
)
